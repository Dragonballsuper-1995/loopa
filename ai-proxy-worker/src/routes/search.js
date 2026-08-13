import { getCorsHeaders } from '../utils/cors.js';
import { normalizeForSearch } from '../utils/normalizer.js';
import { getCachedResponse, setCachedResponse } from '../utils/cache.js';
import { searchTmdb } from '../services/tmdb.js';
import { searchAniList } from '../services/anilist.js';
import { searchKitsu } from '../services/kitsu.js';
import { searchJikan } from '../services/jikan.js';

function withTimeout(promise, ms, fallbackValue = []) {
  let timeoutId;
  const timeoutPromise = new Promise((resolve) => {
    timeoutId = setTimeout(() => resolve(fallbackValue), ms);
  });
  return Promise.race([
    promise.then(res => { clearTimeout(timeoutId); return res; }),
    timeoutPromise
  ]);
}

export async function handleFastSearch(request, env, ctx) {
  const corsHeaders = getCorsHeaders(request);
  const url = new URL(request.url);
  const query = url.searchParams.get('q') || url.searchParams.get('query') || '';

  if (!query.trim()) {
    return new Response(JSON.stringify([]), {
      status: 200,
      headers: { 'Content-Type': 'application/json', ...corsHeaders }
    });
  }

  // 1. Check Edge Cache
  const cached = await getCachedResponse(request);
  if (cached) {
    // Return cached response with updated CORS header for current origin
    const cachedData = await cached.text();
    return new Response(cachedData, {
      status: 200,
      headers: { 'Content-Type': 'application/json', ...corsHeaders, 'X-Cache': 'HIT' }
    });
  }

  // 2. Parallel multi-provider search at the Edge with 200ms timeout cap on secondary providers
  const [tmdbResults, aniListResults, kitsuResults, jikanResults] = await Promise.allSettled([
    searchTmdb(query, env),
    searchAniList(query),
    withTimeout(searchKitsu(query), 200, []),
    withTimeout(searchJikan(query), 200, [])
  ]);

  const tmdbList = tmdbResults.status === 'fulfilled' ? tmdbResults.value : [];
  const aniList = aniListResults.status === 'fulfilled' ? aniListResults.value : [];
  const kitsuList = kitsuResults.status === 'fulfilled' ? kitsuResults.value : [];
  const jikanList = jikanResults.status === 'fulfilled' ? jikanResults.value : [];

  const seen = new Set();
  const mergedResults = [];

  // Normalize TMDB
  for (const item of tmdbList) {
    const norm = normalizeForSearch(item, 'tmdb');
    if (norm && norm.posterUrl) {
      const key = `${norm.id}_${norm.mediaType}`;
      if (!seen.has(key)) {
        seen.add(key);
        mergedResults.push(norm);
      }
    }
  }

  // Normalize AniList
  for (const item of aniList) {
    const norm = normalizeForSearch(item, 'anilist');
    if (norm && norm.posterUrl) {
      const key = `${norm.id}_${norm.mediaType}`;
      if (!seen.has(key)) {
        seen.add(key);
        mergedResults.push(norm);
      }
    }
  }

  // Normalize Kitsu
  for (const item of kitsuList) {
    const norm = normalizeForSearch(item, 'kitsu');
    if (norm && norm.posterUrl) {
      const key = `${norm.id}_${norm.mediaType}`;
      if (!seen.has(key)) {
        seen.add(key);
        mergedResults.push(norm);
      }
    }
  }

  // Normalize Jikan fallback if few results
  if (mergedResults.length < 10) {
    for (const item of jikanList) {
      const norm = normalizeForSearch(item, 'jikan');
      if (norm && norm.posterUrl) {
        const key = `${norm.id}_${norm.mediaType}`;
        if (!seen.has(key)) {
          seen.add(key);
          mergedResults.push(norm);
        }
      }
    }
  }

  // Limit fast search output to top 15 results for ultra-lightweight micro-payload (< 2KB)
  const finalResults = mergedResults.slice(0, 15);
  const jsonString = JSON.stringify(finalResults);

  const response = new Response(jsonString, {
    status: 200,
    headers: {
      'Content-Type': 'application/json',
      'X-Cache': 'MISS',
      'Cache-Control': 'public, max-age=86400, s-maxage=86400',
      ...corsHeaders
    }
  });

  // Save to Edge Cache
  await setCachedResponse(request, response, ctx, 86400);

  return response;
}
