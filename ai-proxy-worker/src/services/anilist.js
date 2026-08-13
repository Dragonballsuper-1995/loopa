export async function searchAniList(query) {
  const gql = `
  query ($search: String) {
    Page(page: 1, perPage: 10) {
      media(search: $search, type: ANIME, isAdult: false) {
        id
        title { english romaji userPreferred }
        coverImage { extraLarge large medium }
        bannerImage
        startDate { year }
        meanScore
        description
        genres
        episodes
        status
      }
    }
  }
  `;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 4000);

  try {
    const res = await fetch('https://graphql.anilist.co', {
      method: 'POST',
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({ query: gql, variables: { search: query } })
    });
    clearTimeout(timeoutId);
    if (!res.ok) return [];
    const json = await res.json();
    return json.data?.Page?.media || [];
  } catch (err) {
    clearTimeout(timeoutId);
    console.warn('[AniList Service] Search error:', err.message);
    return [];
  }
}

export async function getAniListDetails(id) {
  const gql = `
  query ($id: Int) {
    Media(id: $id, type: ANIME) {
      id
      title { english romaji userPreferred }
      coverImage { extraLarge large }
      bannerImage
      startDate { year }
      meanScore
      description
      genres
      episodes
      status
      nextAiringEpisode { episode }
    }
  }
  `;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 3000);

  try {
    const res = await fetch('https://graphql.anilist.co', {
      method: 'POST',
      signal: controller.signal,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: JSON.stringify({ query: gql, variables: { id: Number(id) } })
    });
    clearTimeout(timeoutId);
    if (!res.ok) return null;
    const json = await res.json();
    return json.data?.Media || null;
  } catch (err) {
    clearTimeout(timeoutId);
    console.warn('[AniList Service] Details error:', err.message);
    return null;
  }
}
