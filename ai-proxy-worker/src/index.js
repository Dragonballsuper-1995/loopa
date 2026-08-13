import { getCorsHeaders, handleOptions, authorizeClient } from './utils/cors.js';
import { handleFastSearch } from './routes/search.js';
import { handleMediaDetails } from './routes/details.js';
import { handleAiRecommendations } from './routes/recommendations.js';

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const isTmdbImage = url.pathname.startsWith('/tmdb/t/p/') || url.pathname.startsWith('/api/image');

    // 1. Handle OPTIONS preflight requests for CORS
    if (request.method === 'OPTIONS') {
      return handleOptions(request);
    }

    // 2. Client Authorization check (bypassed for public image endpoints)
    if (!isTmdbImage) {
      if (!authorizeClient(request, env)) {
        return new Response(JSON.stringify({ error: 'Unauthorized Client Request' }), {
          status: 403,
          headers: { 'Content-Type': 'application/json', ...getCorsHeaders(request) },
        });
      }
    }

    // 3. API Router
    // ── Instant Fast Search Endpoint ──────────────────────────────────────────
    if (url.pathname === '/api/search/fast' || url.pathname === '/api/search') {
      if (request.method !== 'GET') {
        return new Response(JSON.stringify({ error: 'Method Not Allowed' }), {
          status: 405,
          headers: { 'Content-Type': 'application/json', ...getCorsHeaders(request) },
        });
      }
      return handleFastSearch(request, env, ctx);
    }

    // ── Lazy Detail Hydration Endpoint ───────────────────────────────────────
    if (url.pathname === '/api/media/details' || url.pathname === '/api/details') {
      if (request.method !== 'GET') {
        return new Response(JSON.stringify({ error: 'Method Not Allowed' }), {
          status: 405,
          headers: { 'Content-Type': 'application/json', ...getCorsHeaders(request) },
        });
      }
      return handleMediaDetails(request, env, ctx);
    }

    // ── Unified AI Recommendations Endpoint ─────────────────────────────────
    if (url.pathname === '/api/recommendations') {
      return handleAiRecommendations(request, env);
    }

    // ── Secure Proxy TMDB Requests & Images ──────────────────────────────────
    if (url.pathname.startsWith('/tmdb/')) {
      if (request.method !== 'GET') {
        return new Response(JSON.stringify({ error: 'Method Not Allowed' }), {
          status: 405,
          headers: { 'Content-Type': 'application/json', ...getCorsHeaders(request) },
        });
      }

      const subpath = url.pathname.replace(/^\/tmdb\//, '');
      const isImage = subpath.startsWith('t/p/');
      const targetHost = isImage ? 'https://image.tmdb.org/' : 'https://api.themoviedb.org/';
      const targetUrl = new URL(targetHost + subpath);
      
      // Copy search parameters
      url.searchParams.forEach((value, key) => {
        targetUrl.searchParams.set(key, value);
      });
      
      // Inject TMDB API Key from environment secrets (images don't require an API key)
      if (!isImage) {
        targetUrl.searchParams.set('api_key', env.TMDB_API_KEY || env.TMDB_KEY);
      }

      try {
        const headers = {};
        if (!isImage) {
          headers['Accept'] = 'application/json';
        }
        const tmdbRes = await fetch(targetUrl.toString(), {
          method: 'GET',
          headers: headers
        });

        const data = await tmdbRes.arrayBuffer();
        const corsHeaders = getCorsHeaders(request, isImage);

        return new Response(data, {
          status: tmdbRes.status,
          headers: {
            'Content-Type': tmdbRes.headers.get('Content-Type') || (isImage ? 'image/jpeg' : 'application/json'),
            'Cache-Control': isImage ? 'public, max-age=604800, immutable' : 'public, max-age=3600',
            ...corsHeaders
          }
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: 'Failed to proxy TMDB request', details: err.message }), {
          status: 502,
          headers: { 'Content-Type': 'application/json', ...getCorsHeaders(request) }
        });
      }
    }

    // ── Root Endpoint (Legacy AI Recommendation Proxy for backward compatibility) ─
    if (url.pathname === '/' || url.pathname === '') {
      return handleAiRecommendations(request, env);
    }

    // ── 404 Route Not Found ─────────────────────────────────────────────────
    return new Response(JSON.stringify({ error: 'Endpoint Not Found' }), {
      status: 404,
      headers: { 'Content-Type': 'application/json', ...getCorsHeaders(request) }
    });
  },
};
