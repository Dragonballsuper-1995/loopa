const memoryCache = new Map();
const MEMORY_TTL_MS = 10 * 60 * 1000; // 10 minutes in memory

export async function getCachedResponse(request) {
  const cacheKey = request.url;
  
  // 1. Check in-memory Map
  const memEntry = memoryCache.get(cacheKey);
  if (memEntry) {
    if (Date.now() - memEntry.timestamp < MEMORY_TTL_MS) {
      return memEntry.response.clone();
    }
    memoryCache.delete(cacheKey);
  }

  // 2. Check Cloudflare Cache API (caches.default) if available
  try {
    if (typeof caches !== 'undefined' && caches.default) {
      const cache = caches.default;
      const cached = await cache.match(request);
      if (cached) {
        return cached;
      }
    }
  } catch (err) {
    console.warn('[Cache] Cloudflare Cache API read error:', err.message);
  }

  return null;
}

export async function setCachedResponse(request, response, ctx, ttlSeconds = 86400) {
  const cacheKey = request.url;
  const clonedForMemory = response.clone();
  const clonedForCfCache = response.clone();

  // 1. Save to in-memory Map
  memoryCache.set(cacheKey, {
    response: clonedForMemory,
    timestamp: Date.now()
  });

  // Prune memoryCache if too large
  if (memoryCache.size > 500) {
    const firstKey = memoryCache.keys().next().value;
    memoryCache.delete(firstKey);
  }

  // 2. Save to Cloudflare Cache API (caches.default)
  try {
    if (typeof caches !== 'undefined' && caches.default) {
      const cache = caches.default;
      // Set Cache-Control headers for Cloudflare Cache API
      const headers = new Headers(clonedForCfCache.headers);
      headers.set('Cache-Control', `public, max-age=${ttlSeconds}, s-maxage=${ttlSeconds}`);
      
      const responseToCache = new Response(clonedForCfCache.body, {
        status: clonedForCfCache.status,
        statusText: clonedForCfCache.statusText,
        headers
      });

      if (ctx && typeof ctx.waitUntil === 'function') {
        ctx.waitUntil(cache.put(request, responseToCache));
      } else {
        await cache.put(request, responseToCache);
      }
    }
  } catch (err) {
    console.warn('[Cache] Cloudflare Cache API write error:', err.message);
  }
}
