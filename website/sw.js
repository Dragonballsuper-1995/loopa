const CACHE_NAME = 'loopa-cache-v7';

// Static assets to cache immediately on install
const PRECACHE_URLS = [
    './',
    './index.html',
    './css/styles.css',
    './css/output.css?v=21',
    './js/config.js?v=4',
    './js/api.js?v=9',
    './js/search-engine.js?v=2',
    './js/ui.js?v=11',
    './js/app.js?v=19',
    './assets/logo.svg',
    './assets/favicon.svg'
];

self.addEventListener('install', event => {
    self.skipWaiting();
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then(cache => cache.addAll(PRECACHE_URLS))
    );
});

self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(cacheNames => {
            return Promise.all(
                cacheNames.map(cacheName => {
                    if (cacheName !== CACHE_NAME) {
                        return caches.delete(cacheName);
                    }
                })
            );
        }).then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', event => {
    const url = new URL(event.request.url);

    // 1. NEVER intercept dynamic backend API requests, AI recommendations, or realtime DB calls.
    // Allow the browser to execute them natively with direct HTTP/2 socket streaming & abort handling.
    if (
        event.request.method !== 'GET' ||
        url.pathname.startsWith('/api/') ||
        url.hostname.includes('workers.dev') ||
        url.hostname.includes('supabase.co') ||
        url.hostname.includes('themoviedb.org') ||
        url.hostname.includes('anilist.co') ||
        url.hostname.includes('kitsu.io') ||
        url.hostname.includes('jikan.moe') ||
        url.protocol === 'chrome-extension:'
    ) {
        return;
    }

    // 2. Local Static Asset Caching (Stale-While-Revalidate)
    if (url.origin === location.origin) {
        event.respondWith(
            caches.match(event.request).then(cachedResponse => {
                const fetchPromise = fetch(event.request).then(networkResponse => {
                    if (networkResponse.ok && url.protocol.startsWith('http')) {
                        const cloneForCache = networkResponse.clone();
                        caches.open(CACHE_NAME).then(cache => {
                            cache.put(event.request, cloneForCache);
                        });
                    }
                    return networkResponse;
                }).catch(() => {
                    // Ignore fetch errors if offline
                });
                return cachedResponse || fetchPromise;
            })
        );
    }
});
