export async function searchJikan(query) {
  const url = new URL('https://api.jikan.moe/v4/anime');
  url.searchParams.set('q', query);
  url.searchParams.set('sfw', 'true');
  url.searchParams.set('limit', '10');

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 2000);

  try {
    const res = await fetch(url.toString(), { signal: controller.signal });
    clearTimeout(timeoutId);
    if (!res.ok) return [];
    const json = await res.json();
    return json.data || [];
  } catch (err) {
    clearTimeout(timeoutId);
    console.warn('[Jikan Service] Search error:', err.message);
    return [];
  }
}

export async function getJikanDetails(id) {
  const url = `https://api.jikan.moe/v4/anime/${id}`;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 2500);

  try {
    const res = await fetch(url, { signal: controller.signal });
    clearTimeout(timeoutId);
    if (!res.ok) return null;
    const json = await res.json();
    return json.data || null;
  } catch (err) {
    clearTimeout(timeoutId);
    console.warn('[Jikan Service] Details error:', err.message);
    return null;
  }
}
