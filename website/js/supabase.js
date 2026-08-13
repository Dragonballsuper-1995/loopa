/**
 * Loopa Web — Supabase Layer
 * Wraps auth and the shared `media_items` table used by the Android app.
 *
 * Table schema (media_items):
 *   id INTEGER, user_id UUID, title TEXT, image_url TEXT, date TEXT,
 *   score DOUBLE, list_name TEXT, media_type TEXT,
 *   current_season INT, current_episode INT,
 *   total_episodes INT, total_seasons INT,
 *   progress_string TEXT, user_rating INT, personal_notes TEXT,
 *   updated_at TIMESTAMPTZ DEFAULT now()   ← added in SQL migration
 *   PRIMARY KEY (id, user_id, media_type)
 *
 * watched_episodes table schema:
 *   id BIGSERIAL, media_id INTEGER, user_id UUID, media_type TEXT,
 *   season_number INT, episode_number INT,
 *   watched_at TIMESTAMPTZ DEFAULT now(),
 *   updated_at TIMESTAMPTZ DEFAULT now()   ← added in SQL migration
 *   UNIQUE (user_id, media_id, media_type, season_number, episode_number)
 */

// Supabase JS v2 UMD exposes window.supabase
let _db = null;

function getDB() {
    if (!_db) {
        const { createClient } = window.supabase;
        _db = createClient(CONFIG.SUPABASE_URL, CONFIG.SUPABASE_KEY);
    }
    return _db;
}

// ── Auth ──────────────────────────────────────────────────────────────────────

const SBAuth = {
    async signIn(email, password) {
        const { data, error } = await getDB().auth.signInWithPassword({ email, password });
        if (error) throw error;
        return data.user;
    },

    async signUp(email, password) {
        const { data, error } = await getDB().auth.signUp({ email, password });
        if (error) throw error;
        return data.user;
    },

    async signOut() {
        const { error } = await getDB().auth.signOut();
        if (error) throw error;
    },

    async getUser() {
        const { data: { user } } = await getDB().auth.getUser();
        return user;
    },

    async getSession() {
        const { data: { session } } = await getDB().auth.getSession();
        return session;
    },

    onAuthStateChange(callback) {
        return getDB().auth.onAuthStateChange(callback);
    },
};

// ── Offline Sync Manager ──────────────────────────────────────────────────────

const OfflineSync = {
    _isSyncing: false,

    getQueue() {
        return JSON.parse(localStorage.getItem('loopa_sync_queue') || '[]');
    },
    saveQueue(q) {
        localStorage.setItem('loopa_sync_queue', JSON.stringify(q));
    },
    enqueue(operation) {
        const q = this.getQueue();
        q.push({ ...operation, timestamp: Date.now() });
        this.saveQueue(q);
        this.attemptSync();
    },
    async attemptSync() {
        if (!navigator.onLine || this._isSyncing) return;
        this._isSyncing = true;

        try {
            let q = this.getQueue();
            if (q.length === 0) return;
            
            // ── Auto-collapse legacy sequential EPISODE_ADD into BULK to prevent network spam ──
            const bulkData = [];
            const optimizedQ = [];
            for (const op of q) {
                if (op.type === 'EPISODE_ADD') {
                    bulkData.push(op.data);
                } else {
                    optimizedQ.push(op);
                }
            }
            if (bulkData.length > 0) {
                optimizedQ.push({ type: 'EPISODE_ADD_BULK', data: bulkData, timestamp: Date.now() });
            }
            q = optimizedQ;

            const processedTimestamps = new Set();
            const failedOps = [];

            for (const op of q) {
                try {
                    if (op.type === 'ADD') {
                        // Only apply ADD if server row doesn't already exist.
                        const { data: existing } = await getDB()
                            .from(CONFIG.DB_TABLE)
                            .select('updated_at')
                            .eq('id', op.data.id)
                            .eq('user_id', op.data.user_id)
                            .eq('media_type', op.data.media_type)
                            .maybeSingle();

                        if (!existing) {
                            await getDB().from(CONFIG.DB_TABLE).upsert(op.data);
                        } else {
                            console.log(`[OfflineSync] Skipping ADD for ${op.data.id}/${op.data.media_type} — row already exists`);
                        }
                    } else if (op.type === 'UPDATE') {
                        // ── Last-Write-Wins: only apply if our local write is newer than remote ──
                        const { data: remoteRow } = await getDB()
                            .from(CONFIG.DB_TABLE)
                            .select('updated_at')
                            .eq('id', op.keys.id)
                            .eq('user_id', op.keys.user_id)
                            .eq('media_type', op.keys.media_type)
                            .single();

                        const remoteTs = remoteRow?.updated_at ? new Date(remoteRow.updated_at).getTime() : 0;
                        const localTs  = op.timestamp || 0;

                        if (localTs >= remoteTs) {
                            await getDB()
                                .from(CONFIG.DB_TABLE)
                                .update(op.data)
                                .eq('id', op.keys.id)
                                .eq('user_id', op.keys.user_id)
                                .eq('media_type', op.keys.media_type);
                        }
                    } else if (op.type === 'REMOVE') {
                        await getDB().from(CONFIG.DB_TABLE).delete().eq('id', op.keys.id).eq('user_id', op.keys.user_id).eq('media_type', op.keys.media_type);
                    } else if (op.type === 'EPISODE_ADD') {
                        await getDB().from('watched_episodes').upsert(op.data, {
                            onConflict: 'user_id,media_id,media_type,season_number,episode_number'
                        });
                    } else if (op.type === 'EPISODE_ADD_BULK') {
                        await getDB().from('watched_episodes').upsert(op.data, {
                            onConflict: 'user_id,media_id,media_type,season_number,episode_number'
                        });
                    } else if (op.type === 'EPISODE_REMOVE') {
                        await getDB().from('watched_episodes').delete().match(op.keys);
                    } else if (op.type === 'EPISODE_REMOVE_ALL') {
                        await getDB().from('watched_episodes').delete().eq('user_id', op.keys.user_id).eq('media_id', op.keys.media_id);
                    }
                    processedTimestamps.add(op.timestamp);
                } catch (e) {
                    console.error("Sync failed for operation", op, e);

                    // If ADD failed because it already exists on server, treat as success
                    if (op.type === 'ADD') {
                        try {
                            const { data: checkRow } = await getDB()
                                .from(CONFIG.DB_TABLE)
                                .select('id')
                                .eq('id', op.data.id)
                                .eq('user_id', op.data.user_id)
                                .eq('media_type', op.data.media_type)
                                .maybeSingle();
                            if (checkRow) {
                                processedTimestamps.add(op.timestamp);
                                continue;
                            }
                        } catch {}
                    }
                    failedOps.push(op);
                }
            }

            // Remove processed ops from current queue safely
            const currentQ = this.getQueue();
            const remaining = currentQ.filter(item => !processedTimestamps.has(item.timestamp));
            this.saveQueue(remaining);

            if (remaining.length === 0) {
                localStorage.setItem('lastSyncTime', Date.now());
                if (window.App && window.App._updateSyncTime) window.App._updateSyncTime();
            }
        } finally {
            this._isSyncing = false;
        }
    }
};

window.addEventListener('online', () => OfflineSync.attemptSync());

// ── Watchlist CRUD ────────────────────────────────────────────────────────────

const SBList = {
    /** Fetch all rows for a user, newest first. Offline-first read. */
    async getAll(userId) {
        if (navigator.onLine) {
            try {
                OfflineSync.attemptSync(); // Non-blocking sync
                const { data, error } = await getDB()
                    .from(CONFIG.DB_TABLE)
                    .select('*')
                    .eq('user_id', userId)
                    .order('id', { ascending: false });
                if (!error && data) {
                    localStorage.setItem(`loopa_wl_${userId}`, JSON.stringify(data));
                    return data;
                }
            } catch (e) {
                console.warn('Network fetch failed, falling back to local storage', e);
            }
        }
        return JSON.parse(localStorage.getItem(`loopa_wl_${userId}`) || '[]');
    },

    /**
     * Upsert a media item into the list.
     * Uses composite PK (id, user_id, media_type) to avoid duplicates.
     */
    async add(userId, mediaItem, listName = 'Watching') {
        const row = {
            id:               mediaItem.id,
            user_id:          userId,
            title:            mediaItem.title,
            image_url:        mediaItem.posterUrl || null,
            date:             mediaItem.year       || null,
            score:            mediaItem.score      || null,
            list_name:        listName,
            media_type:       mediaItem.mediaType,
            current_season:   1,
            current_episode:  0,
            total_episodes:   mediaItem.totalEpisodes || 0,
            total_seasons:    mediaItem.totalSeasons  || 0,
            progress_string:  null,
            user_rating:      null,
            personal_notes:   null,
            runtime:          mediaItem.runtime ? parseInt(String(mediaItem.runtime).replace(/\D/g, '')) || null : null,
            genres:           mediaItem.genres ? mediaItem.genres.join(',') : (mediaItem.genre || null),
            director_studio:  mediaItem.directorStudio || mediaItem.studio || mediaItem.director || null,
            // Timestamp for Last-Write-Wins conflict resolution (mirrors Android updatedAt)
            updated_at:       new Date().toISOString(),
        };

        const localData = JSON.parse(localStorage.getItem(`loopa_wl_${userId}`) || '[]');
        const idx = localData.findIndex(i => i.id === row.id && i.media_type === row.media_type);
        if (idx >= 0) localData[idx] = row;
        else localData.unshift(row);
        localStorage.setItem(`loopa_wl_${userId}`, JSON.stringify(localData));

        OfflineSync.enqueue({ type: 'ADD', data: row });
        return [row];
    },

    /** Update selected fields on an existing row */
    async update(userId, id, mediaType, updates) {
        const localData = JSON.parse(localStorage.getItem(`loopa_wl_${userId}`) || '[]');
        const idx = localData.findIndex(i => i.id === id && i.media_type === mediaType);
        if (idx >= 0) {
            localData[idx] = { ...localData[idx], ...updates };
            localStorage.setItem(`loopa_wl_${userId}`, JSON.stringify(localData));
        }
        OfflineSync.enqueue({
            type: 'UPDATE',
            // Stamp updated_at now so LWW comparison in attemptSync uses the mutation time,
            // not the flush time. op.timestamp (set by enqueue) is the wall-clock fallback.
            data: { ...updates, updated_at: new Date().toISOString() },
            keys: { id, user_id: userId, media_type: mediaType }
        });
        return localData[idx] || null;
    },

    /** Delete a row */
    async remove(userId, id, mediaType) {
        const localData = JSON.parse(localStorage.getItem(`loopa_wl_${userId}`) || '[]');
        const filtered = localData.filter(i => !(i.id === id && i.media_type === mediaType));
        localStorage.setItem(`loopa_wl_${userId}`, JSON.stringify(filtered));
        OfflineSync.enqueue({ type: 'REMOVE', keys: { id, user_id: userId, media_type: mediaType } });
    },

    /** Returns the row if present, null otherwise */
    async find(userId, id, mediaType) {
        const localData = JSON.parse(localStorage.getItem(`loopa_wl_${userId}`) || '[]');
        return localData.find(i => i.id === id && i.media_type === mediaType) || null;
    },

    /** Subscribe to real-time changes for a specific user's watchlist */
    subscribeToChanges(userId, onInsert, onUpdate, onDelete) {
        if (this._subscription) {
            getDB().removeChannel(this._subscription);
        }
        
        this._subscription = getDB()
            .channel('watchlist_changes')
            .on(
                'postgres_changes',
                { event: 'INSERT', schema: 'public', table: CONFIG.DB_TABLE, filter: `user_id=eq.${userId}` },
                (payload) => { if (onInsert) onInsert(payload.new); }
            )
            .on(
                'postgres_changes',
                { event: 'UPDATE', schema: 'public', table: CONFIG.DB_TABLE, filter: `user_id=eq.${userId}` },
                (payload) => { if (onUpdate) onUpdate(payload.new); }
            )
            .on(
                'postgres_changes',
                { event: 'DELETE', schema: 'public', table: CONFIG.DB_TABLE, filter: `user_id=eq.${userId}` },
                (payload) => { if (onDelete) onDelete(payload.old); }
            )
            .subscribe();
            
        return this._subscription;
    },
};

// ── Watched Episodes CRUD ─────────────────────────────────────────────────────

const SBWatchedEpisodes = {
    async getForMedia(userId, mediaId, mediaType) {
        const localData = JSON.parse(localStorage.getItem(`loopa_episodes_${userId}_${mediaId}`) || '[]');
        if (navigator.onLine) {
            try {
                const { data, error } = await getDB()
                    .from('watched_episodes')
                    .select('*')
                    .eq('user_id', userId)
                    .eq('media_id', mediaId)
                    .eq('media_type', mediaType);
                if (!error && data) {
                    const map = new Map();
                    data.forEach(item => map.set(`${item.season_number}_${item.episode_number}`, item));
                    localData.forEach(item => map.set(`${item.season_number}_${item.episode_number}`, item));
                    const merged = Array.from(map.values());
                    localStorage.setItem(`loopa_episodes_${userId}_${mediaId}`, JSON.stringify(merged));
                    return merged;
                }
            } catch (e) {
                console.warn('Network fetch failed for episodes', e);
            }
        }
        return localData;
    },

    async add(userId, mediaId, mediaType, seasonNumber, episodeNumber) {
        const row = {
            media_id: mediaId,
            user_id: userId,
            media_type: mediaType,
            season_number: seasonNumber,
            episode_number: episodeNumber,
            watched_at: new Date().toISOString()
        };
        const localData = JSON.parse(localStorage.getItem(`loopa_episodes_${userId}_${mediaId}`) || '[]');
        if (!localData.some(e => e.season_number === seasonNumber && e.episode_number === episodeNumber)) {
            localData.push(row);
            localStorage.setItem(`loopa_episodes_${userId}_${mediaId}`, JSON.stringify(localData));
        }
        OfflineSync.enqueue({ type: 'EPISODE_ADD', data: row });
    },

    async addBulk(userId, mediaId, mediaType, episodesArray) {
        if (!episodesArray || episodesArray.length === 0) return;
        const now = new Date().toISOString();
        const rows = episodesArray.map(ep => ({
            media_id: mediaId,
            user_id: userId,
            media_type: mediaType,
            season_number: ep.season_number,
            episode_number: ep.episode_number,
            watched_at: now
        }));
        
        const localData = JSON.parse(localStorage.getItem(`loopa_episodes_${userId}_${mediaId}`) || '[]');
        const existingSet = new Set(localData.map(e => `${e.season_number}_${e.episode_number}`));
        
        const newRows = [];
        for (const row of rows) {
            if (!existingSet.has(`${row.season_number}_${row.episode_number}`)) {
                localData.push(row);
                newRows.push(row);
            }
        }
        
        if (newRows.length > 0) {
            localStorage.setItem(`loopa_episodes_${userId}_${mediaId}`, JSON.stringify(localData));
            OfflineSync.enqueue({ type: 'EPISODE_ADD_BULK', data: newRows });
        }
    },

    async remove(userId, mediaId, mediaType, seasonNumber, episodeNumber) {
        const localData = JSON.parse(localStorage.getItem(`loopa_episodes_${userId}_${mediaId}`) || '[]');
        const filtered = localData.filter(e => !(e.season_number === seasonNumber && e.episode_number === episodeNumber));
        localStorage.setItem(`loopa_episodes_${userId}_${mediaId}`, JSON.stringify(filtered));
        OfflineSync.enqueue({ 
            type: 'EPISODE_REMOVE', 
            keys: { media_id: mediaId, user_id: userId, season_number: seasonNumber, episode_number: episodeNumber } 
        });
    },

    async removeAll(userId, mediaId, mediaType) {
        localStorage.setItem(`loopa_episodes_${userId}_${mediaId}`, '[]');
        OfflineSync.enqueue({
            type: 'EPISODE_REMOVE_ALL',
            keys: { media_id: mediaId, user_id: userId }
        });
    }
};
