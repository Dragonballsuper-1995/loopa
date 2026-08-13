export async function searchKitsu(query) {
  const url = new URL('https://kitsu.io/api/edge/anime');
  url.searchParams.set('filter[text]', query);
  url.searchParams.set('page[limit]', '10');

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 2500);

  try {
    const res = await fetch(url.toString(), {
      signal: controller.signal,
      headers: { 'Accept': 'application/vnd.api+json' }
    });
    clearTimeout(timeoutId);
    if (!res.ok) return [];
    const json = await res.json();
    return json.data || [];
  } catch (err) {
    clearTimeout(timeoutId);
    console.warn('[Kitsu Service] Search error:', err.message);
    return [];
  }
}

export async function getKitsuDetails(id) {
  const url = `https://kitsu.io/api/edge/anime/${id}`;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 3000);

  try {
    const res = await fetch(url, {
      signal: controller.signal,
      headers: { 'Accept': 'application/vnd.api+json' }
    });
    clearTimeout(timeoutId);
    if (!res.ok) return null;
    const json = await res.json();
    return json.data || null;
  } catch (err) {
    clearTimeout(timeoutId);
    console.warn('[Kitsu Service] Details error:', err.message);
    return null;
  }
}
