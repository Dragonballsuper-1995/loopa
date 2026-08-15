/**
 * Loopa Web — Supabase Layer
 * Wraps auth, real-time channels, offline sync, and the shared `media_items` table.
 *
 * Table schema (media_items):
 *   id INTEGER, user_id UUID, title TEXT, image_url TEXT, date TEXT,
 *   score DOUBLE, list_name TEXT, media_type TEXT,
 *   current_season INT, current_episode INT,
 *   total_episodes INT, total_seasons INT,
 *   progress_string TEXT, user_rating INT, personal_notes TEXT,
 *   updated_at TIMESTAMPTZ DEFAULT now()
 *   PRIMARY KEY (id, user_id, media_type)
 *
 * watched_episodes table schema:
 *   id BIGSERIAL, media_id INTEGER, user_id UUID, media_type TEXT,
 *   season_number INT, episode_number INT,
 *   watched_at TIMESTAMPTZ DEFAULT now(),
 *   updated_at TIMESTAMPTZ DEFAULT now()
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
        if (window.IDBStore) {
            window.IDBStore.clear('sync_queue').then(() => {
                window.IDBStore.putBulk('sync_queue', q);
            }).catch(() => {});
        }
    },
    enqueue(operation) {
        const q = this.getQueue();
        const opWithTs = { ...operation, timestamp: Date.now() };
        q.push(opWithTs);
        this.saveQueue(q);
        this.attemptSync();
    },
    async attemptSync() {
        if (!navigator.onLine || this._isSyncing) return;
        this._isSyncing = true;

        try {
            let q = this.getQueue();
            if (q.length === 0) return;

            // Auto-collapse sequential EPISODE_ADD into BULK to minimize network requests
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
                        // Last-Write-Wins conflict resolution
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
                    console.error("[OfflineSync] Failed for operation", op, e);

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
    /** Fetch all rows for a user. Offline-first read via IndexedDB with fallback. */
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
                    if (window.IDBStore) {
                        await window.IDBStore.saveWatchlistForUser(userId, data);
                    }
                    return data;
                }
            } catch (e) {
                console.warn('[SBList] Network fetch failed, falling back to local storage', e);
            }
        }

        // Offline or Network Failure: read from IndexedDB first, then localStorage
        if (window.IDBStore) {
            try {
                const idbData = await window.IDBStore.getWatchlistForUser(userId);
                if (idbData && idbData.length > 0) return idbData;
            } catch (e) {
                console.warn('[SBList] IDB read failed:', e);
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
            updated_at:       new Date().toISOString(),
        };

        const localData = JSON.parse(localStorage.getItem(`loopa_wl_${userId}`) || '[]');
        const idx = localData.findIndex(i => i.id === row.id && i.media_type === row.media_type);
        if (idx >= 0) localData[idx] = row;
        else localData.unshift(row);
        localStorage.setItem(`loopa_wl_${userId}`, JSON.stringify(localData));

        if (window.IDBStore) {
            const key = `${userId}_${row.id}_${row.media_type}`;
            window.IDBStore.put('watchlist', { ...row, _key: key }).catch(() => {});
        }

        OfflineSync.enqueue({ type: 'ADD', data: row });
        return [row];
    },

    /** Update selected fields on an existing row */
    async update(userId, id, mediaType, updates) {
        const localData = JSON.parse(localStorage.getItem(`loopa_wl_${userId}`) || '[]');
        const idx = localData.findIndex(i => i.id === id && i.media_type === mediaType);
        const updatedRow = idx >= 0 ? { ...localData[idx], ...updates, updated_at: new Date().toISOString() } : null;
        if (idx >= 0 && updatedRow) {
            localData[idx] = updatedRow;
            localStorage.setItem(`loopa_wl_${userId}`, JSON.stringify(localData));
            if (window.IDBStore) {
                const key = `${userId}_${id}_${mediaType}`;
                window.IDBStore.put('watchlist', { ...updatedRow, _key: key }).catch(() => {});
            }
        }
        OfflineSync.enqueue({
            type: 'UPDATE',
            data: { ...updates, updated_at: new Date().toISOString() },
            keys: { id, user_id: userId, media_type: mediaType }
        });
        return updatedRow;
    },

    /** Delete a row */
    async remove(userId, id, mediaType) {
        const localData = JSON.parse(localStorage.getItem(`loopa_wl_${userId}`) || '[]');
        const filtered = localData.filter(i => !(i.id === id && i.media_type === mediaType));
        localStorage.setItem(`loopa_wl_${userId}`, JSON.stringify(filtered));

        if (window.IDBStore) {
            const key = `${userId}_${id}_${mediaType}`;
            window.IDBStore.delete('watchlist', key).catch(() => {});
        }

        OfflineSync.enqueue({ type: 'REMOVE', keys: { id, user_id: userId, media_type: mediaType } });
    },

    /** Returns the row if present, null otherwise */
    async find(userId, id, mediaType) {
        if (window.IDBStore) {
            try {
                const key = `${userId}_${id}_${mediaType}`;
                const row = await window.IDBStore.get('watchlist', key);
                if (row) return row;
            } catch {}
        }
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
                (payload) => {
                    const row = payload.new;
                    if (row) {
                        const key = `${userId}_${row.id}_${row.media_type}`;
                        if (window.IDBStore) window.IDBStore.put('watchlist', { ...row, _key: key });
                        const localData = JSON.parse(localStorage.getItem(`loopa_wl_${userId}`) || '[]');
                        const idx = localData.findIndex(i => i.id === row.id && i.media_type === row.media_type);
                        if (idx >= 0) localData[idx] = row;
                        else localData.unshift(row);
                        localStorage.setItem(`loopa_wl_${userId}`, JSON.stringify(localData));
                        if (onInsert) onInsert(row);
                    }
                }
            )
            .on(
                'postgres_changes',
                { event: 'UPDATE', schema: 'public', table: CONFIG.DB_TABLE, filter: `user_id=eq.${userId}` },
                (payload) => {
                    const row = payload.new;
                    if (row) {
                        const key = `${userId}_${row.id}_${row.media_type}`;
                        if (window.IDBStore) window.IDBStore.put('watchlist', { ...row, _key: key });
                        const localData = JSON.parse(localStorage.getItem(`loopa_wl_${userId}`) || '[]');
                        const idx = localData.findIndex(i => i.id === row.id && i.media_type === row.media_type);
                        if (idx >= 0) localData[idx] = row;
                        else localData.unshift(row);
                        localStorage.setItem(`loopa_wl_${userId}`, JSON.stringify(localData));
                        if (onUpdate) onUpdate(row);
                    }
                }
            )
            .on(
                'postgres_changes',
                { event: 'DELETE', schema: 'public', table: CONFIG.DB_TABLE, filter: `user_id=eq.${userId}` },
                (payload) => {
                    const old = payload.old;
                    if (old) {
                        const key = `${userId}_${old.id}_${old.media_type}`;
                        if (window.IDBStore) window.IDBStore.delete('watchlist', key);
                        const localData = JSON.parse(localStorage.getItem(`loopa_wl_${userId}`) || '[]');
                        const filtered = localData.filter(i => !(i.id === old.id && i.media_type === old.media_type));
                        localStorage.setItem(`loopa_wl_${userId}`, JSON.stringify(filtered));
                        if (onDelete) onDelete(old);
                    }
                }
            )
            .subscribe();

        return this._subscription;
    },
};

// ── Watched Episodes CRUD ─────────────────────────────────────────────────────

const SBWatchedEpisodes = {
    async getForMedia(userId, mediaId, mediaType) {
        if (navigator.onLine) {
            try {
                const { data, error } = await getDB()
                    .from('watched_episodes')
                    .select('*')
                    .eq('user_id', userId)
                    .eq('media_id', mediaId)
                    .eq('media_type', mediaType);
                if (!error && data) {
                    const localData = JSON.parse(localStorage.getItem(`loopa_episodes_${userId}_${mediaId}`) || '[]');
                    const map = new Map();
                    data.forEach(item => map.set(`${item.season_number}_${item.episode_number}`, item));
                    localData.forEach(item => map.set(`${item.season_number}_${item.episode_number}`, item));
                    const merged = Array.from(map.values());
                    localStorage.setItem(`loopa_episodes_${userId}_${mediaId}`, JSON.stringify(merged));
                    if (window.IDBStore) {
                        const prepared = merged.map(e => ({
                            ...e,
                            _key: `${userId}_${mediaId}_${mediaType}_${e.season_number}_${e.episode_number}`
                        }));
                        window.IDBStore.putBulk('watched_episodes', prepared);
                    }
                    return merged;
                }
            } catch (e) {
                console.warn('[SBWatchedEpisodes] Network fetch failed for episodes', e);
            }
        }

        if (window.IDBStore) {
            try {
                const idbEpisodes = await window.IDBStore.getEpisodesForMedia(userId, mediaId, mediaType);
                if (idbEpisodes && idbEpisodes.length > 0) return idbEpisodes;
            } catch {}
        }

        return JSON.parse(localStorage.getItem(`loopa_episodes_${userId}_${mediaId}`) || '[]');
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
            if (window.IDBStore) {
                const key = `${userId}_${mediaId}_${mediaType}_${seasonNumber}_${episodeNumber}`;
                window.IDBStore.put('watched_episodes', { ...row, _key: key }).catch(() => {});
            }
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
            if (window.IDBStore) {
                const prepared = newRows.map(r => ({
                    ...r,
                    _key: `${userId}_${mediaId}_${mediaType}_${r.season_number}_${r.episode_number}`
                }));
                window.IDBStore.putBulk('watched_episodes', prepared);
            }
            OfflineSync.enqueue({ type: 'EPISODE_ADD_BULK', data: newRows });
        }
    },

    async remove(userId, mediaId, mediaType, seasonNumber, episodeNumber) {
        const localData = JSON.parse(localStorage.getItem(`loopa_episodes_${userId}_${mediaId}`) || '[]');
        const filtered = localData.filter(e => !(e.season_number === seasonNumber && e.episode_number === episodeNumber));
        localStorage.setItem(`loopa_episodes_${userId}_${mediaId}`, JSON.stringify(filtered));

        if (window.IDBStore) {
            const key = `${userId}_${mediaId}_${mediaType}_${seasonNumber}_${episodeNumber}`;
            window.IDBStore.delete('watched_episodes', key).catch(() => {});
        }

        OfflineSync.enqueue({
            type: 'EPISODE_REMOVE',
            keys: { media_id: mediaId, user_id: userId, season_number: seasonNumber, episode_number: episodeNumber }
        });
    },

    async removeAll(userId, mediaId, mediaType) {
        localStorage.setItem(`loopa_episodes_${userId}_${mediaId}`, '[]');
        if (window.IDBStore) {
            window.IDBStore.getEpisodesForMedia(userId, mediaId, mediaType).then(episodes => {
                episodes.forEach(ep => {
                    const key = `${userId}_${mediaId}_${mediaType}_${ep.season_number}_${ep.episode_number}`;
                    window.IDBStore.delete('watched_episodes', key);
                });
            }).catch(() => {});
        }
        OfflineSync.enqueue({
            type: 'EPISODE_REMOVE_ALL',
            keys: { media_id: mediaId, user_id: userId }
        });
    }
};
