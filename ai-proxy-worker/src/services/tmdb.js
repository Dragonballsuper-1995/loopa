export async function searchTmdb(query, env) {
  const apiKey = env.TMDB_API_KEY || env.TMDB_KEY;
  if (!apiKey) return [];

  const url = new URL('https://api.themoviedb.org/3/search/multi');
  url.searchParams.set('api_key', apiKey);
  url.searchParams.set('query', query);
  url.searchParams.set('language', 'en-US');
  url.searchParams.set('page', '1');

  try {
    const res = await fetch(url.toString(), {
      headers: { 'Accept': 'application/json' }
    });
    if (!res.ok) return [];
    const data = await res.json();
    return data.results || [];
  } catch (err) {
    console.error('[TMDB Service] Search error:', err.message);
    return [];
  }
}

export async function getTmdbDetails(id, type = 'movie', env) {
  const apiKey = env.TMDB_API_KEY || env.TMDB_KEY;
  if (!apiKey) return null;

  const endpoint = type === 'tv' || type === 'anime' ? `/3/tv/${id}` : `/3/movie/${id}`;
  const url = new URL(`https://api.themoviedb.org${endpoint}`);
  url.searchParams.set('api_key', apiKey);
  url.searchParams.set('language', 'en-US');

  try {
    const res = await fetch(url.toString(), {
      headers: { 'Accept': 'application/json' }
    });
    if (!res.ok) return null;
    return await res.json();
  } catch (err) {
    console.error('[TMDB Service] Details error:', err.message);
    return null;
  }
}
