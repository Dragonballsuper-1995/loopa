/**
 * Loopa Web — IndexedDB Storage Engine (IDBStore)
 * High-performance, asynchronous offline-first database.
 * Replaces synchronous 5MB localStorage limits with high-capacity IndexedDB storage
 * and automatic migration for existing watchlist & episode records.
 */

const IDBStore = {
    DB_NAME: 'LoopaDB',
    DB_VERSION: 1,
    _db: null,
    _initPromise: null,

    /**
     * Initializes the IndexedDB database and performs legacy localStorage migration.
     */
    async init() {
        if (this._db) return this._db;
        if (this._initPromise) return this._initPromise;

        this._initPromise = new Promise((resolve, reject) => {
            if (!window.indexedDB) {
                console.warn('[IDBStore] IndexedDB not supported in this environment, falling back to localStorage');
                resolve(null);
                return;
            }

            const request = indexedDB.open(this.DB_NAME, this.DB_VERSION);

            request.onupgradeneeded = (event) => {
                const db = event.target.result;

                // Watchlist Store: indexed by composite key (id + '_' + media_type)
                if (!db.objectStoreNames.contains('watchlist')) {
                    const wlStore = db.createObjectStore('watchlist', { keyPath: '_key' });
                    wlStore.createIndex('user_id', 'user_id', { unique: false });
                    wlStore.createIndex('list_name', 'list_name', { unique: false });
                    wlStore.createIndex('media_type', 'media_type', { unique: false });
                    wlStore.createIndex('updated_at', 'updated_at', { unique: false });
                }

                // Watched Episodes Store: indexed by composite key
                if (!db.objectStoreNames.contains('watched_episodes')) {
                    const epStore = db.createObjectStore('watched_episodes', { keyPath: '_key' });
                    epStore.createIndex('user_media', ['user_id', 'media_id', 'media_type'], { unique: false });
                    epStore.createIndex('watched_at', 'watched_at', { unique: false });
                }

                // Offline Sync Queue Store
                if (!db.objectStoreNames.contains('sync_queue')) {
                    db.createObjectStore('sync_queue', { keyPath: 'timestamp' });
                }

                // Generic Key-Value Store
                if (!db.objectStoreNames.contains('keyval')) {
                    db.createObjectStore('keyval', { keyPath: 'key' });
                }
            };

            request.onsuccess = async (event) => {
                this._db = event.target.result;
                try {
                    await this._migrateFromLocalStorage();
                } catch (e) {
                    console.warn('[IDBStore] Migration error:', e);
                }
                resolve(this._db);
            };

            request.onerror = (event) => {
                console.error('[IDBStore] Open error:', event.target.error);
                resolve(null); // Fall back to localStorage gracefully
            };
        });

        return this._initPromise;
    },

    /**
     * Migrates legacy localStorage records into IndexedDB seamlessly without data loss.
     */
    async _migrateFromLocalStorage() {
        if (!this._db) return;

        // 1. Migrate Watchlists
        for (let i = 0; i < localStorage.length; i++) {
            const k = localStorage.key(i);
            if (k && k.startsWith('loopa_wl_')) {
                const userId = k.replace('loopa_wl_', '');
                try {
                    const items = JSON.parse(localStorage.getItem(k) || '[]');
                    if (Array.isArray(items) && items.length > 0) {
                        for (const item of items) {
                            const key = `${userId}_${item.id}_${item.media_type}`;
                            await this.put('watchlist', { ...item, user_id: userId, _key: key });
                        }
                    }
                } catch (e) {
                    console.warn(`[IDBStore] Failed migrating watchlist key ${k}:`, e);
                }
            }
        }

        // 2. Migrate Episodes
        for (let i = 0; i < localStorage.length; i++) {
            const k = localStorage.key(i);
            if (k && k.startsWith('loopa_episodes_')) {
                try {
                    const episodes = JSON.parse(localStorage.getItem(k) || '[]');
                    if (Array.isArray(episodes) && episodes.length > 0) {
                        for (const ep of episodes) {
                            const key = `${ep.user_id}_${ep.media_id}_${ep.media_type}_${ep.season_number}_${ep.episode_number}`;
                            await this.put('watched_episodes', { ...ep, _key: key });
                        }
                    }
                } catch (e) {
                    console.warn(`[IDBStore] Failed migrating episodes key ${k}:`, e);
                }
            }
        }
    },

    // ── Generic IDB Operations with LocalStorage Fallback ─────────────────────

    async get(storeName, key) {
        await this.init();
        if (!this._db) {
            return JSON.parse(localStorage.getItem(`idb_fb_${storeName}_${key}`) || 'null');
        }

        return new Promise((resolve) => {
            const tx = this._db.transaction(storeName, 'readonly');
            const store = tx.objectStore(storeName);
            const req = store.get(key);
            req.onsuccess = () => resolve(req.result || null);
            req.onerror = () => resolve(null);
        });
    },

    async getAll(storeName) {
        await this.init();
        if (!this._db) {
            const results = [];
            const prefix = `idb_fb_${storeName}_`;
            for (let i = 0; i < localStorage.length; i++) {
                const k = localStorage.key(i);
                if (k && k.startsWith(prefix)) {
                    results.push(JSON.parse(localStorage.getItem(k)));
                }
            }
            return results;
        }

        return new Promise((resolve) => {
            const tx = this._db.transaction(storeName, 'readonly');
            const store = tx.objectStore(storeName);
            const req = store.getAll();
            req.onsuccess = () => resolve(req.result || []);
            req.onerror = () => resolve([]);
        });
    },

    async put(storeName, item) {
        await this.init();
        if (!this._db) {
            const key = item._key || item.key || item.timestamp;
            localStorage.setItem(`idb_fb_${storeName}_${key}`, JSON.stringify(item));
            return;
        }

        return new Promise((resolve, reject) => {
            const tx = this._db.transaction(storeName, 'readwrite');
            const store = tx.objectStore(storeName);
            const req = store.put(item);
            req.onsuccess = () => resolve(req.result);
            req.onerror = (e) => reject(e.target.error);
        });
    },

    async putBulk(storeName, items) {
        if (!items || items.length === 0) return;
        await this.init();
        if (!this._db) {
            items.forEach(item => {
                const key = item._key || item.key || item.timestamp;
                localStorage.setItem(`idb_fb_${storeName}_${key}`, JSON.stringify(item));
            });
            return;
        }

        return new Promise((resolve, reject) => {
            const tx = this._db.transaction(storeName, 'readwrite');
            const store = tx.objectStore(storeName);
            items.forEach(item => store.put(item));
            tx.oncomplete = () => resolve();
            tx.onerror = (e) => reject(e.target.error);
        });
    },

    async delete(storeName, key) {
        await this.init();
        if (!this._db) {
            localStorage.removeItem(`idb_fb_${storeName}_${key}`);
            return;
        }

        return new Promise((resolve) => {
            const tx = this._db.transaction(storeName, 'readwrite');
            const store = tx.objectStore(storeName);
            const req = store.delete(key);
            req.onsuccess = () => resolve(true);
            req.onerror = () => resolve(false);
        });
    },

    async clear(storeName) {
        await this.init();
        if (!this._db) {
            const prefix = `idb_fb_${storeName}_`;
            const keysToRemove = [];
            for (let i = 0; i < localStorage.length; i++) {
                const k = localStorage.key(i);
                if (k && k.startsWith(prefix)) keysToRemove.push(k);
            }
            keysToRemove.forEach(k => localStorage.removeItem(k));
            return;
        }

        return new Promise((resolve) => {
            const tx = this._db.transaction(storeName, 'readwrite');
            const store = tx.objectStore(storeName);
            const req = store.clear();
            req.onsuccess = () => resolve(true);
            req.onerror = () => resolve(false);
        });
    },

    // ── Specialized Watchlist & Episode Query Helpers ─────────────────────────

    async getWatchlistForUser(userId) {
        const all = await this.getAll('watchlist');
        return all.filter(item => item.user_id === userId);
    },

    async saveWatchlistForUser(userId, items) {
        if (!Array.isArray(items)) return;
        const prepared = items.map(item => ({
            ...item,
            user_id: userId,
            _key: `${userId}_${item.id}_${item.media_type}`
        }));
        await this.putBulk('watchlist', prepared);
    },

    async getEpisodesForMedia(userId, mediaId, mediaType) {
        const all = await this.getAll('watched_episodes');
        return all.filter(e =>
            e.user_id === userId &&
            parseInt(e.media_id, 10) === parseInt(mediaId, 10) &&
            e.media_type === mediaType
        );
    }
};

// Initialize IDBStore eagerly in background
if (typeof window !== 'undefined') {
    window.IDBStore = IDBStore;
    IDBStore.init().catch(() => {});
}
