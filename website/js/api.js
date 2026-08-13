/**
 * Fetches from TMDB (Movies & TV) and Jikan (Anime).
 * All public functions return normalised MediaItem objects.
 *
 * @typedef {Object} MediaItem
 * @property {number}   id
 * @property {string}   mediaType   'movie' | 'tv' | 'anime'
 * @property {string}   title
 * @property {string|null} posterUrl
 * @property {string|null} backdropUrl
 * @property {string}   year
 * @property {number|null} score     out of 10
 * @property {string}   synopsis
 * @property {string[]} genres
 * @property {number}   totalEpisodes
 * @property {number}   totalSeasons
 * @property {string}   status
 */

const API = {

    // ── Response Cache (30-minute TTL) ───────────────────────────────────────
    _cache: new Map(),
    _CACHE_TTL_MS: 30 * 60 * 1000, // 30 minutes

    _cacheGet(key) {
        const entry = this._cache.get(key);
        if (!entry) return null;
        if (Date.now() - entry.ts > this._CACHE_TTL_MS) {
            this._cache.delete(key);
            return null;
        }
        return entry.data;
    },

    _cacheSet(key, data) {
        this._cache.set(key, { data, ts: Date.now() });
    },

    clearCache() {
        this._cache.clear();
    },

    // ── Private fetch helpers ────────────────────────────────────────────────

    async _tmdb(endpoint, params = {}) {
        const cacheKey = `tmdb:${endpoint}:${JSON.stringify(params)}`;
        const cached = this._cacheGet(cacheKey);
        if (cached) return cached;

        const url = new URL(`${CONFIG.TMDB_BASE}${endpoint}`);
        url.searchParams.set('language', 'en-US');
        Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, v));
        const res = await fetch(url.toString(), {
            headers: {
                'X-Loopa-Client-Key': CONFIG.CLIENT_KEY
            }
        });
        if (!res.ok) throw new Error(`TMDB ${res.status} on ${endpoint}`);
        const data = await res.json();
        this._cacheSet(cacheKey, data);
        return data;
    },

    async _jikan(endpoint, params = {}) {
        const cacheKey = `jikan:${endpoint}:${JSON.stringify(params)}`;
        const cached = this._cacheGet(cacheKey);
        if (cached) return cached;

        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 2000); // Strict 2s timeout

        try {
            const url = new URL(`${CONFIG.JIKAN_BASE}${endpoint}`);
            Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, v));
            let res = await fetch(url.toString(), { signal: controller.signal });
            clearTimeout(timeoutId);
            if (!res.ok) throw new Error(`Jikan ${res.status} on ${endpoint}`);
            const data = await res.json();
            this._cacheSet(cacheKey, data);
            return data;
        } catch (e) {
            clearTimeout(timeoutId);
            throw e;
        }
    },

    async _kitsu(endpoint, params = {}) {
        const cacheKey = `kitsu:${endpoint}:${JSON.stringify(params)}`;
        const cached = this._cacheGet(cacheKey);
        if (cached) return cached;

        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 4500);

        try {
            const url = new URL(`https://kitsu.io/api/edge${endpoint}`);
            Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, v));
            const res = await fetch(url.toString(), {
                signal: controller.signal,
                headers: {
                    'Accept': 'application/vnd.api+json'
                }
            });
            clearTimeout(timeoutId);
            if (!res.ok) throw new Error(`Kitsu ${res.status}`);
            const data = await res.json();
            this._cacheSet(cacheKey, data);
            return data;
        } catch (e) {
            clearTimeout(timeoutId);
            if (e.name === 'AbortError') {
                return { data: [] }; // Silent abort fallback
            }
            throw e;
        }
    },

    async _anilist(query, variables = {}) {
        const cacheKey = `anilist:${query.trim().substring(0, 60)}:${JSON.stringify(variables)}`;
        const cached = this._cacheGet(cacheKey);
        if (cached) return cached;

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
                body: JSON.stringify({ query, variables })
            });
            clearTimeout(timeoutId);
            if (!res.ok) throw new Error(`AniList ${res.status}`);
            const json = await res.json();
            this._cacheSet(cacheKey, json.data);
            return json.data;
        } catch (e) {
            clearTimeout(timeoutId);
            throw e;
        }
    },

    // ── Normalise ─────────────────────────────────────────────────────────────

    _normTMDB(item, forceType = null) {
        // Detect anime: TMDB genre_ids/genres include Animation (16) + Japanese origin/language
        const genreIds = item.genre_ids || (item.genres || []).map(g => g.id);
        const isAnimation = genreIds.includes(16);
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
            id:           item.id,
            provider:     'tmdb',
            mediaType,
            title:        item.title || item.name || 'Unknown',
            posterUrl:    item.poster_path   ? `${CONFIG.TMDB_IMG_500}${item.poster_path}`   : null,
            backdropUrl:  item.backdrop_path ? `${CONFIG.TMDB_IMG_ORIG}${item.backdrop_path}` : null,
            year:         (item.release_date || item.first_air_date || '').substring(0, 4),
            score:        item.vote_average  ? +item.vote_average.toFixed(1) : null,
            synopsis:     item.overview || 'No synopsis available.',
            genres:       (item.genres || []).map(g => g.name),
            totalEpisodes: item.number_of_episodes || 0,
            totalSeasons:  item.number_of_seasons  || 0,
            status:        item.status || (mediaType === 'movie' ? 'Released' : 'Airing'),
            tagline:       item.tagline || null,
            runtime:       item.runtime ? `${item.runtime} min` : null,
        };
    },

    _normKitsu(item) {
        const attr = item.attributes || {};
        return {
            id:           item.id,
            provider:     'kitsu',
            mediaType:    'anime',
            title:        attr.canonicalTitle || attr.titles?.en || attr.titles?.en_jp || 'Unknown',
            posterUrl:    attr.posterImage?.large || attr.posterImage?.original || attr.posterImage?.small || null,
            backdropUrl:  attr.coverImage?.large || attr.coverImage?.original || null,
            year:         attr.startDate ? attr.startDate.substring(0, 4) : '',
            score:        attr.averageRating ? +(parseFloat(attr.averageRating) / 10).toFixed(1) : null,
            synopsis:     attr.synopsis || 'No synopsis available.',
            genres:       ['Anime'],
            totalEpisodes: attr.episodeCount || 0,
            totalSeasons:  1,
            status:        attr.status === 'current' ? 'Airing' : attr.status === 'finished' ? 'Finished' : 'Unknown',
        };
    },

    _normJikan(anime) {
        return {
            id:           anime.mal_id,
            provider:     'jikan',
            mediaType:    'anime',
            title:        anime.title_english || anime.title || 'Unknown',
            posterUrl:    anime.images?.jpg?.large_image_url || anime.images?.jpg?.image_url || null,
            backdropUrl:  null,
            year:         anime.year ? String(anime.year) : (anime.aired?.from || '').substring(0, 4),
            score:        anime.score ? +anime.score.toFixed(1) : null,
            synopsis:     anime.synopsis || 'No synopsis available.',
            genres:       (anime.genres || []).map(g => g.name),
            totalEpisodes: anime.episodes || 0,
            totalSeasons:  1,
            status:        anime.status || 'Unknown',
        };
    },

    _normAniList(anime) {
        const currentEps = anime.episodes || (anime.nextAiringEpisode ? anime.nextAiringEpisode.episode - 1 : 0);
        return {
            id:           anime.id,
            provider:     'anilist',
            mediaType:    'anime',
            title:        anime.title?.english || anime.title?.romaji || anime.title?.userPreferred || 'Unknown',
            posterUrl:    anime.coverImage?.extraLarge || anime.coverImage?.large || null,
            backdropUrl:  anime.bannerImage || null,
            year:         anime.startDate?.year ? String(anime.startDate.year) : '',
            score:        anime.meanScore ? +(anime.meanScore / 10).toFixed(1) : null,
            synopsis:     (anime.description || 'No synopsis available.').replace(/<[^>]*>?/gm, ''),
            genres:       anime.genres || [],
            totalEpisodes: currentEps,
            totalSeasons:  1,
            status:        anime.status === 'RELEASING' ? 'Airing' : (anime.status || 'Finished'),
        };
    },

    // ── Dashboard / Discovery rows ────────────────────────────────────────────

    async fetchTrending() {
        const d = await this._tmdb('/trending/all/week');
        return (d.results || []).filter(i => i.poster_path).slice(0, 20).map(i => this._normTMDB(i));
    },

    async fetchTrendingByRegion(region = 'IN') {
        const d = await this._tmdb('/movie/popular', { region });
        return (d.results || []).filter(i => i.poster_path).slice(0, 20).map(i => this._normTMDB(i, 'movie'));
    },

    async fetchPopularMovies() {
        const d = await this._tmdb('/movie/popular');
        return (d.results || []).filter(i => i.poster_path).slice(0, 20)
            .map(i => this._normTMDB(i, 'movie'));
    },

    async fetchTopRatedMovies() {
        const d = await this._tmdb('/movie/top_rated');
        return (d.results || []).filter(i => i.poster_path).slice(0, 20)
            .map(i => this._normTMDB(i, 'movie'));
    },

    async fetchUpcomingMovies() {
        const d = await this._tmdb('/movie/upcoming');
        return (d.results || []).filter(i => i.poster_path).slice(0, 20)
            .map(i => this._normTMDB(i, 'movie'));
    },

    async fetchPopularTV() {
        const d = await this._tmdb('/tv/popular');
        return (d.results || []).filter(i => i.poster_path).slice(0, 20)
            .map(i => this._normTMDB(i, 'tv'));
    },

    async fetchTopRatedTV() {
        const d = await this._tmdb('/tv/top_rated');
        return (d.results || []).filter(i => i.poster_path).slice(0, 20)
            .map(i => this._normTMDB(i, 'tv'));
    },

    async fetchAiringTodayTV() {
        const d = await this._tmdb('/tv/airing_today');
        return (d.results || []).filter(i => i.poster_path).slice(0, 20)
            .map(i => this._normTMDB(i, 'tv'));
    },

    async fetchTopAnime() {
        try {
            const gql = `
            query {
              Page(page: 1, perPage: 20) {
                media(type: ANIME, sort: SCORE_DESC, isAdult: false) {
                  id title { english romaji userPreferred } coverImage { extraLarge large } bannerImage startDate { year } meanScore description genres episodes status
                }
              }
            }
            `;
            const data = await this._anilist(gql);
            const list = data?.Page?.media || [];
            if (list.length > 0) return list.map(a => this._normAniList(a));
        } catch (e) {
            console.warn('[Top Anime] AniList failed:', e.message);
        }

        try {
            const d = await this._jikan('/top/anime', { limit: 20 });
            return (d.data || []).filter(a => a.images?.jpg?.large_image_url).slice(0, 20)
                .map(a => this._normJikan(a));
        } catch { return []; }
    },

    async fetchUpcomingAnime() {
        try {
            const gql = `
            query {
              Page(page: 1, perPage: 20) {
                media(type: ANIME, status: NOT_YET_RELEASED, sort: POPULARITY_DESC, isAdult: false) {
                  id title { english romaji userPreferred } coverImage { extraLarge large } bannerImage startDate { year } meanScore description genres episodes status
                }
              }
            }
            `;
            const data = await this._anilist(gql);
            const list = data?.Page?.media || [];
            if (list.length > 0) return list.map(a => this._normAniList(a));
        } catch (e) {
            console.warn('[Upcoming Anime] AniList failed:', e.message);
        }

        try {
            const d = await this._jikan('/seasons/upcoming', { limit: 20 });
            return (d.data || []).filter(a => a.images?.jpg?.large_image_url).slice(0, 20)
                .map(a => this._normJikan(a));
        } catch { return []; }
    },

    // ── Search ────────────────────────────────────────────────────────────────

    async searchMovies(query) {
        const d = await this._tmdb('/search/movie', { query });
        return (d.results || []).filter(i => i.poster_path).slice(0, 12)
            .map(i => this._normTMDB(i, 'movie'));
    },

    async searchTV(query) {
        const d = await this._tmdb('/search/tv', { query });
        return (d.results || []).filter(i => i.poster_path).slice(0, 12)
            .map(i => this._normTMDB(i, 'tv'));
    },

    async searchAnime(query) {
        // 1. Kitsu API (Fastest, rate-limit free, exact episode counts)
        try {
            const d = await this._kitsu('/anime', { 'filter[text]': query, 'page[limit]': 12 });
            const list = (d.data || []).map(a => this._normKitsu(a));
            if (list.length > 0) return list;
        } catch (e) {
            console.warn('[Anime Search] Kitsu failed:', e.message);
        }

        // 2. AniList GraphQL
        try {
            const gql = `
            query ($search: String) {
              Page(page: 1, perPage: 12) {
                media(search: $search, type: ANIME, isAdult: false) {
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
                }
              }
            }
            `;
            const data = await this._anilist(gql, { search: query });
            const list = data?.Page?.media || [];
            if (list.length > 0) {
                return list.map(a => this._normAniList(a));
            }
        } catch (e) {
            console.warn('[Anime Search] AniList failed:', e.message);
        }

        // 3. Jikan fallback (with 2s timeout)
        try {
            const d = await this._jikan('/anime', { q: query, limit: 12, sfw: true });
            const results = (d.data || []).filter(a => a.images?.jpg?.large_image_url).slice(0, 12)
                .map(a => this._normJikan(a));
            if (results.length > 0) return results;
        } catch (e) {
            console.warn('[Anime Search] Jikan failed:', e.message);
        }

        // 4. TMDB fallback (Japanese Animation only)
        try {
            const d = await this._tmdb('/search/tv', { query });
            return (d.results || [])
                .filter(i => i.poster_path && (
                    (i.genre_ids && i.genre_ids.includes(16)) || 
                    (i.origin_country && i.origin_country.includes('JP')) || 
                    i.original_language === 'ja'
                ))
                .slice(0, 12)
                .map(i => this._normTMDB(i, 'anime'));
        } catch (e) {
            console.warn('[Anime Search] TMDB fallback failed:', e.message);
            return [];
        }
    },

    async searchFast(query) {
        try {
            const url = new URL(CONFIG.SEARCH_FAST_URL || `${CONFIG.AI_PROXY_URL}/api/search/fast`);
            url.searchParams.set('q', query);
            if (CONFIG.CLIENT_KEY) {
                url.searchParams.set('k', CONFIG.CLIENT_KEY);
            }
            const res = await fetch(url.toString());
            if (res.ok) {
                const data = await res.json();
                if (Array.isArray(data)) return data;
            }
        } catch (e) {
            console.warn('[searchFast] Edge search failed, falling back to searchAll:', e.message);
        }
        return this.searchAll(query);
    },

    async searchAll(query) {
        const [movies, tv, anime] = await Promise.allSettled([
            this.searchMovies(query),
            this.searchTV(query),
            this.searchAnime(query),
        ]);
        return [
            ...(movies.status === 'fulfilled' ? movies.value : []),
            ...(tv.status    === 'fulfilled' ? tv.value    : []),
            ...(anime.status === 'fulfilled' ? anime.value : []),
        ];
    },

    // ── Details ───────────────────────────────────────────────────────────────

    async fetchMovieDetails(id) {
        const d = await this._tmdb(`/movie/${id}`);
        return this._normTMDB(d, 'movie');
    },

    async fetchTVDetails(id) {
        const d = await this._tmdb(`/tv/${id}`);
        return this._normTMDB(d, 'tv');
    },

    async fetchTVSeasonDetails(id, seasonNumber) {
        return await this._tmdb(`/tv/${id}/season/${seasonNumber}`);
    },

    async fetchAnimeDetails(id) {
        try {
            const gql = `
            query ($id: Int) {
              Media(id: $id, type: ANIME) {
                id title { english romaji userPreferred } coverImage { extraLarge large } bannerImage startDate { year } meanScore description genres episodes nextAiringEpisode { episode } status
              }
            }
            `;
            const data = await this._anilist(gql, { id: parseInt(id) });
            if (data?.Media) {
                const norm = this._normAniList(data.Media);
                if (norm.totalEpisodes > 0) return norm;
                // If episodes is still 0 (e.g., One Piece ongoing), search TMDB TV details for exact episode count
                try {
                    const tmdbSearch = await this._tmdb('/search/tv', { query: norm.title });
                    if (tmdbSearch.results && tmdbSearch.results[0]) {
                        const tvDetails = await this.fetchTVDetails(tmdbSearch.results[0].id);
                        if (tvDetails && tvDetails.totalEpisodes > 0) {
                            norm.totalEpisodes = tvDetails.totalEpisodes;
                            norm.totalSeasons = tvDetails.totalSeasons || 1;
                        }
                    }
                } catch {}
                return norm;
            }
        } catch {}

        try {
            const d = await this._jikan(`/anime/${id}/full`);
            return this._normJikan(d.data || {});
        } catch {}

        return null;
    },

    async fetchDetails(id, mediaType, provider = null) {
        // 1. Try unified Edge API lazy detail hydration endpoint
        try {
            const url = new URL(CONFIG.MEDIA_DETAILS_URL || `${CONFIG.AI_PROXY_URL}/api/media/details`);
            url.searchParams.set('id', id);
            if (mediaType) url.searchParams.set('type', mediaType);
            if (provider) url.searchParams.set('provider', provider);
            if (CONFIG.CLIENT_KEY) {
                url.searchParams.set('k', CONFIG.CLIENT_KEY);
            }

            const res = await fetch(url.toString());
            if (res.ok) {
                const data = await res.json();
                if (data && data.id) return data;
            }
        } catch (e) {
            console.warn('[fetchDetails] Edge details failed, falling back:', e.message);
        }

        // 2. Direct provider fallback
        try {
            if (provider === 'tmdb') {
                if (mediaType === 'movie') return await this.fetchMovieDetails(id);
                return await this.fetchTVDetails(id);
            }
            if (provider === 'anilist' || provider === 'jikan') {
                return await this.fetchAnimeDetails(id);
            }
            if (mediaType === 'movie') return await this.fetchMovieDetails(id);
            if (mediaType === 'anime') {
                const ani = await this.fetchAnimeDetails(id);
                if (ani) return ani;
                return await this.fetchTVDetails(id);
            }
            if (mediaType === 'tv') return await this.fetchTVDetails(id);
        } catch (e) {
            console.warn('fetchDetails fallback failed:', e.message);
        }
        return null;
    },

    // ── Gemini AI Recommendations ─────────────────────────────────────────────

    async getAIRecommendations(watchedItems, likedItems = [], dislikedItems = [], chatHistory = []) {
        const context = watchedItems
            .slice(0, 50)
            .map(i => `${i.title} (${i.media_type}, score: ${i.user_rating || i.score || '?'}/10)`)
            .join(', ');

        let prompt = `You are a conversational media recommendation engine. The user has watched: ${context}.\n`;
        if (likedItems.length > 0) {
            prompt += `The user specifically LIKED these recommended targets:\n${likedItems.map(t => `- ${t}`).join('\n')}\n`;
        }
        if (dislikedItems.length > 0) {
            prompt += `The user DISLIKED or was NOT interested in these targets (DO NOT recommend them):\n${dislikedItems.map(t => `- ${t}`).join('\n')}\n`;
        }

        prompt += `\nHere is the recent conversation history between you and the user:\n`;
        chatHistory.forEach(msg => {
            prompt += `${msg.role === 'user' ? 'User' : 'Assistant'}: ${msg.content}\n`;
        });

        // Step 3.3 — Unified schema matching Android AiRecommendationResult.kt
        prompt += `\nBased on the conversation above (especially the User's last message), provide exactly 4 recommendations they have NOT watched yet. Return ONLY a raw JSON array (no markdown, no code blocks). Each element:
{"title":"<title>","mediaType":"movie"|"tv"|"anime","genre":"<primary genre>","releaseYear":"<YYYY>","reasoning":"<max 15 words>"}`;

        try {
            console.log('Attempting AI recommendation via Cloudflare Proxy...');
            const targetUrl = CONFIG.RECOMMENDATIONS_URL || CONFIG.AI_PROXY_URL;
            const res = await fetch(targetUrl, {
                method: 'POST',
                headers: { 
                    'Content-Type': 'application/json',
                    'X-Loopa-Client-Key': CONFIG.CLIENT_KEY
                },
                body: JSON.stringify({ prompt }),
            });
            
            if (!res.ok) throw new Error(`Proxy Error: ${res.status}`);
            
            let text = await res.text();
            text = text.replace(/```json?/g, '').replace(/```/g, '').trim();
            
            try {
                const parsed = JSON.parse(text);
                return Array.isArray(parsed) ? parsed : (parsed.recommendations || parsed.titles || Object.values(parsed)[0] || []);
            } catch (e) {
                console.error('Failed to parse proxy response:', text);
                throw e;
            }
        } catch (error) {
            console.error('AI Proxy failed:', error.message);
            throw new Error('AI Recommendations are currently unavailable. Please try again later.');
        }
    },

    // ── Phase 3C & W4: Similar Content (Movies, TV & Anime) ───────────────────
    async fetchSimilar(id, mediaType) {
        if (!id) return [];
        
        if (mediaType === 'anime') {
            // 1. Try AniList GraphQL recommendations using AniList ID
            try {
                const gql = `
                query ($id: Int) {
                  Media (id: $id, type: ANIME) {
                    recommendations (perPage: 8) {
                      nodes {
                        mediaRecommendation {
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
                        }
                      }
                    }
                  }
                }
                `;
                const data = await this._anilist(gql, { id: parseInt(id) });
                const recs = data?.Media?.recommendations?.nodes || [];
                const list = recs.map(n => n.mediaRecommendation).filter(Boolean);
                if (list.length > 0) return list.map(a => this._normAniList(a));
            } catch (e) {
                console.warn('[Anime Recommendations] AniList failed:', e.message);
            }

            // 2. Kitsu / TMDB fallback
            try {
                const d = await this._kitsu('/trending/anime', { 'page[limit]': 8 });
                return (d.data || []).map(a => this._normKitsu(a));
            } catch {
                return [];
            }
        }

        const tmdbType = mediaType === 'tv' ? 'tv' : 'movie';
        try {
            const data = await this._tmdb(`/${tmdbType}/${id}/similar`);
            return (data.results || []).slice(0, 8).map(r => this._normTMDB(r, tmdbType));
        } catch {
            return [];
        }
    },
};
