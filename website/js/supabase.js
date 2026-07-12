/**
 * Miru Web — Supabase Layer
 * Wraps auth and the shared `media_items` table used by the Android app.
 *
 * Table schema (created by Android app):
 *   id INTEGER, user_id UUID, title TEXT, image_url TEXT, date TEXT,
 *   score DOUBLE, list_name TEXT, media_type TEXT,
 *   current_season INT, current_episode INT,
 *   total_episodes INT, total_seasons INT,
 *   progress_string TEXT, user_rating INT, personal_notes TEXT
 *   PRIMARY KEY (id, user_id, media_type)
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

// ── Watchlist CRUD ────────────────────────────────────────────────────────────

const SBList = {
    /** Fetch all rows for a user, newest first */
    async getAll(userId) {
        const { data, error } = await getDB()
            .from(CONFIG.DB_TABLE)
            .select('*')
            .eq('user_id', userId)
            .order('id', { ascending: false });
        if (error) throw error;
        return data || [];
    },

    /**
     * Upsert a media item into the list.
     * Uses composite PK (id, user_id, media_type) to avoid duplicates.
     */
    async add(userId, mediaItem, listName = 'To Watch') {
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
        };

        const { data, error } = await getDB()
            .from(CONFIG.DB_TABLE)
            .upsert(row);
        if (error) throw error;
        return data;
    },

    /** Update selected fields on an existing row */
    async update(userId, id, mediaType, updates) {
        const { data, error } = await getDB()
            .from(CONFIG.DB_TABLE)
            .update(updates)
            .eq('id',         id)
            .eq('user_id',    userId)
            .eq('media_type', mediaType);
        if (error) throw error;
        return data;
    },

    /** Delete a row */
    async remove(userId, id, mediaType) {
        const { error } = await getDB()
            .from(CONFIG.DB_TABLE)
            .delete()
            .eq('id',         id)
            .eq('user_id',    userId)
            .eq('media_type', mediaType);
        if (error) throw error;
    },

    /** Returns the row if present, null otherwise */
    async find(userId, id, mediaType) {
        const { data, error } = await getDB()
            .from(CONFIG.DB_TABLE)
            .select('*')
            .eq('id',         id)
            .eq('user_id',    userId)
            .eq('media_type', mediaType)
            .maybeSingle();
        if (error) throw error;
        return data;
    },

    /** Subscribe to real-time changes for a specific user's watchlist */
    subscribeToChanges(userId, onInsert, onUpdate, onDelete) {
        return getDB()
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
    },
};
