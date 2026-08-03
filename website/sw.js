const CACHE_NAME = 'loopa-cache-v5';

// Static assets to cache immediately on install
const PRECACHE_URLS = [
    './',
    './index.html',
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

    // Skip cross-origin requests, except for fonts or TMDB if we want to cache them
    // For now, we use Network-First for API calls and Stale-While-Revalidate for static assets.
    
    if (url.origin === location.origin) {
        // Stale-While-Revalidate for local assets
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
    } else {
        // Network-First for API and external resources
        event.respondWith(
            fetch(event.request)
                .then(response => {
                    // Only cache successful GET requests over HTTP/HTTPS (ignore chrome-extension://)
                    if (event.request.method === 'GET' && response.ok && url.protocol.startsWith('http')) {
                        const responseClone = response.clone();
                        caches.open(CACHE_NAME).then(cache => {
                            cache.put(event.request, responseClone);
                        });
                    }
                    return response;
                })
                .catch(() => {
                    // If offline, try to return cached response
                    return caches.match(event.request);
                })
        );
    }
});
