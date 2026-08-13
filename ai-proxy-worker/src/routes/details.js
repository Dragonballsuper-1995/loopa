import { getCorsHeaders } from '../utils/cors.js';
import { normalizeForDetails } from '../utils/normalizer.js';
import { getCachedResponse, setCachedResponse } from '../utils/cache.js';
import { getTmdbDetails } from '../services/tmdb.js';
import { getAniListDetails } from '../services/anilist.js';
import { getKitsuDetails } from '../services/kitsu.js';
import { getJikanDetails } from '../services/jikan.js';

export async function handleMediaDetails(request, env, ctx) {
  const corsHeaders = getCorsHeaders(request);
  const url = new URL(request.url);
  
  const id = url.searchParams.get('id');
  const type = url.searchParams.get('type') || url.searchParams.get('mediaType') || 'movie';
  const provider = url.searchParams.get('provider') || 'tmdb';

  if (!id) {
    return new Response(JSON.stringify({ error: 'Missing id parameter' }), {
      status: 400,
      headers: { 'Content-Type': 'application/json', ...corsHeaders }
    });
  }

  // 1. Check Edge Cache
  const cached = await getCachedResponse(request);
  if (cached) {
    const cachedData = await cached.text();
    return new Response(cachedData, {
      status: 200,
      headers: { 'Content-Type': 'application/json', ...corsHeaders, 'X-Cache': 'HIT' }
    });
  }

  // 2. Fetch full metadata from specified provider
  let rawItem = null;
  if (provider === 'anilist') {
    rawItem = await getAniListDetails(id);
  } else if (provider === 'kitsu') {
    rawItem = await getKitsuDetails(id);
  } else if (provider === 'jikan') {
    rawItem = await getJikanDetails(id);
  } else {
    rawItem = await getTmdbDetails(id, type, env);
  }

  if (!rawItem) {
    return new Response(JSON.stringify({ error: 'Media details not found' }), {
      status: 404,
      headers: { 'Content-Type': 'application/json', ...corsHeaders }
    });
  }

  const normalized = normalizeForDetails(rawItem, provider, type);
  const jsonString = JSON.stringify(normalized);

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
