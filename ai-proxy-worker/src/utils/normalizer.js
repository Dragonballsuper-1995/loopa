const TMDB_IMG_W154 = 'https://image.tmdb.org/t/p/w154';
const TMDB_IMG_W500 = 'https://image.tmdb.org/t/p/w500';
const TMDB_IMG_ORIG = 'https://image.tmdb.org/t/p/original';

/**
 * Normalizes item for instant fast search (micro-payload < 2KB for list).
 * Uses w154 thumbnail images (~15KB) for crisp, readable visuals.
 */
export function normalizeForSearch(item, provider, forceType = null) {
  if (!item) return null;

  if (provider === 'tmdb') {
    const genreIds = item.genre_ids || (item.genres || []).map(g => g.id || g);
    const isAnimation = Array.isArray(genreIds) && genreIds.includes(16);
    const isJapanese = (item.origin_country && item.origin_country.includes('JP')) || item.original_language === 'ja';
    const isTV = item.media_type === 'tv' || item.first_air_date || item.name;
    const isAnime = isAnimation && (isJapanese || forceType === 'anime') && isTV;

    const mediaType = forceType === 'movie' ? 'movie'
      : forceType === 'anime' ? 'anime'
      : isAnime ? 'anime'
      : forceType === 'tv' ? 'tv'
      : (item.media_type === 'movie' ? 'movie'
      :  item.media_type === 'tv'    ? 'tv'
      :  item.first_air_date         ? 'tv'
      :  item.release_date           ? 'movie'
      :  item.name                   ? 'tv' : 'movie');

    const posterPath = item.poster_path;
    const posterUrl = posterPath ? `${TMDB_IMG_W154}${posterPath}` : null;
    const year = (item.release_date || item.first_air_date || '').substring(0, 4);

    return {
      id: item.id,
      mediaType,
      provider: 'tmdb',
      title: item.title || item.name || 'Unknown',
      posterUrl,
      year,
      score: item.vote_average ? +item.vote_average.toFixed(1) : null
    };
  }

  if (provider === 'anilist') {
    const title = item.title?.english || item.title?.romaji || item.title?.userPreferred || 'Unknown';
    const posterUrl = item.coverImage?.large || item.coverImage?.medium || null;
    const year = item.startDate?.year ? String(item.startDate.year) : '';
    const score = item.meanScore ? +(item.meanScore / 10).toFixed(1) : null;

    return {
      id: item.id,
      mediaType: 'anime',
      provider: 'anilist',
      title,
      posterUrl,
      year,
      score
    };
  }

  if (provider === 'kitsu') {
    const attr = item.attributes || {};
    const title = attr.canonicalTitle || attr.titles?.en || attr.titles?.en_jp || 'Unknown';
    const posterUrl = attr.posterImage?.small || attr.posterImage?.medium || attr.posterImage?.large || null;
    const year = attr.startDate ? attr.startDate.substring(0, 4) : '';
    const score = attr.averageRating ? +(parseFloat(attr.averageRating) / 10).toFixed(1) : null;

    return {
      id: Number(item.id),
      mediaType: 'anime',
      provider: 'kitsu',
      title,
      posterUrl,
      year,
      score
    };
  }

  if (provider === 'jikan') {
    const title = item.title_english || item.title || 'Unknown';
    const posterUrl = item.images?.jpg?.image_url || item.images?.jpg?.large_image_url || null;
    const year = item.year ? String(item.year) : (item.aired?.from || '').substring(0, 4);
    const score = item.score ? +item.score.toFixed(1) : null;

    return {
      id: item.mal_id,
      mediaType: 'anime',
      provider: 'jikan',
      title,
      posterUrl,
      year,
      score
    };
  }

  return null;
}

/**
 * Normalizes full metadata for lazy detail hydration when user opens details view.
 */
export function normalizeForDetails(item, provider, forceType = null) {
  if (!item) return null;

  if (provider === 'tmdb') {
    const genreIds = item.genre_ids || (item.genres || []).map(g => g.id || g);
    const isAnimation = Array.isArray(genreIds) && genreIds.includes(16);
    const isJapanese = (item.origin_country && item.origin_country.includes('JP')) || item.original_language === 'ja';
    const isTV = item.media_type === 'tv' || item.first_air_date || item.name;
    const isAnime = isAnimation && (isJapanese || forceType === 'anime') && isTV;

    const mediaType = forceType === 'movie' ? 'movie'
      : forceType === 'anime' ? 'anime'
      : isAnime ? 'anime'
      : forceType === 'tv' ? 'tv'
      : (item.media_type === 'movie' ? 'movie'
      :  item.media_type === 'tv'    ? 'tv'
      :  item.first_air_date         ? 'tv'
      :  item.release_date           ? 'movie'
      :  item.name                   ? 'tv' : 'movie');

    return {
      id: item.id,
      provider: 'tmdb',
      mediaType,
      title: item.title || item.name || 'Unknown',
      posterUrl: item.poster_path ? `${TMDB_IMG_W500}${item.poster_path}` : null,
      backdropUrl: item.backdrop_path ? `${TMDB_IMG_ORIG}${item.backdrop_path}` : null,
      year: (item.release_date || item.first_air_date || '').substring(0, 4),
      score: item.vote_average ? +item.vote_average.toFixed(1) : null,
      synopsis: item.overview || 'No synopsis available.',
      genres: (item.genres || []).map(g => g.name || g),
      totalEpisodes: item.number_of_episodes || 0,
      totalSeasons: item.number_of_seasons || 0,
      status: item.status || (mediaType === 'movie' ? 'Released' : 'Airing'),
      tagline: item.tagline || null,
      runtime: item.runtime ? `${item.runtime} min` : null,
    };
  }

  if (provider === 'anilist') {
    const currentEps = item.episodes || (item.nextAiringEpisode ? item.nextAiringEpisode.episode - 1 : 0);
    const rawStatus = item.status || '';
    const status = rawStatus === 'RELEASING' ? 'Airing'
      : rawStatus === 'FINISHED' ? 'Finished'
      : rawStatus === 'NOT_YET_RELEASED' ? 'Upcoming'
      : (rawStatus || 'Finished');

    return {
      id: item.id,
      provider: 'anilist',
      mediaType: 'anime',
      title: item.title?.english || item.title?.romaji || item.title?.userPreferred || 'Unknown',
      posterUrl: item.coverImage?.extraLarge || item.coverImage?.large || null,
      backdropUrl: item.bannerImage || null,
      year: item.startDate?.year ? String(item.startDate.year) : '',
      score: item.meanScore ? +(item.meanScore / 10).toFixed(1) : null,
      synopsis: (item.description || 'No synopsis available.').replace(/<[^>]*>?/gm, ''),
      genres: item.genres || [],
      totalEpisodes: currentEps,
      totalSeasons: 1,
      status,
    };
  }

  if (provider === 'kitsu') {
    const attr = item.attributes || {};
    return {
      id: Number(item.id),
      provider: 'kitsu',
      mediaType: 'anime',
      title: attr.canonicalTitle || attr.titles?.en || attr.titles?.en_jp || 'Unknown',
      posterUrl: attr.posterImage?.large || attr.posterImage?.original || attr.posterImage?.small || null,
      backdropUrl: attr.coverImage?.large || attr.coverImage?.original || null,
      year: attr.startDate ? attr.startDate.substring(0, 4) : '',
      score: attr.averageRating ? +(parseFloat(attr.averageRating) / 10).toFixed(1) : null,
      synopsis: attr.synopsis || 'No synopsis available.',
      genres: ['Anime'],
      totalEpisodes: attr.episodeCount || 0,
      totalSeasons: 1,
      status: attr.status === 'current' ? 'Airing' : attr.status === 'finished' ? 'Finished' : 'Unknown',
    };
  }

  if (provider === 'jikan') {
    const rawStatus = item.status || '';
    const status = rawStatus.includes('Currently Airing') ? 'Airing'
      : rawStatus.includes('Finished') ? 'Finished'
      : rawStatus.includes('Not yet aired') ? 'Upcoming'
      : (rawStatus || 'Finished');

    return {
      id: item.mal_id,
      provider: 'jikan',
      mediaType: 'anime',
      title: item.title_english || item.title || 'Unknown',
      posterUrl: item.images?.jpg?.large_image_url || item.images?.jpg?.image_url || null,
      backdropUrl: null,
      year: item.year ? String(item.year) : (item.aired?.from || '').substring(0, 4),
      score: item.score ? +item.score.toFixed(1) : null,
      synopsis: item.synopsis || 'No synopsis available.',
      genres: (item.genres || []).map(g => g.name || g),
      totalEpisodes: item.episodes || 0,
      totalSeasons: 1,
      status,
    };
  }

  return null;
}
