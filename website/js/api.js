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

    // ── Private fetch helpers ────────────────────────────────────────────────

    async _tmdb(endpoint, params = {}) {
        const url = new URL(`${CONFIG.TMDB_BASE}${endpoint}`);
        url.searchParams.set('language', 'en-US');
        Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, v));
        const res = await fetch(url.toString(), {
            headers: {
                'X-Loopa-Client-Key': CONFIG.CLIENT_KEY
            }
        });
        if (!res.ok) throw new Error(`TMDB ${res.status} on ${endpoint}`);
        return res.json();
    },

    async _jikan(endpoint, params = {}) {
        const url = new URL(`${CONFIG.JIKAN_BASE}${endpoint}`);
        Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, v));
        // Jikan rate limit: simple retry after 1 s on 429
        let res = await fetch(url.toString());
        if (res.status === 429) {
            await new Promise(r => setTimeout(r, 1000));
            res = await fetch(url.toString());
        }
        if (!res.ok) throw new Error(`Jikan ${res.status} on ${endpoint}`);
        return res.json();
    },

    // ── Normalise ─────────────────────────────────────────────────────────────

    _normTMDB(item, forceType = null) {
        const mediaType = forceType
            || (item.media_type === 'movie' ? 'movie'
            :   item.media_type === 'tv'    ? 'tv'
            :   item.first_air_date          ? 'tv'
            :   item.release_date            ? 'movie'
            :   item.name                    ? 'tv' : 'movie');

        return {
            id:           item.id,
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

    _normJikan(anime) {
        return {
            id:           anime.mal_id,
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

    // ── Dashboard / Discovery rows ────────────────────────────────────────────

    async fetchTrending() {
        const d = await this._tmdb('/trending/all/week');
        return (d.results || []).filter(i => i.poster_path).slice(0, 20).map(i => this._normTMDB(i));
    },

    async fetchPopularMovies() {
        const d = await this._tmdb('/movie/popular');
        return (d.results || []).filter(i => i.poster_path).slice(0, 20)
            .map(i => this._normTMDB(i, 'movie'));
    },

    async fetchPopularTV() {
        const d = await this._tmdb('/tv/popular');
        return (d.results || []).filter(i => i.poster_path).slice(0, 20)
            .map(i => this._normTMDB(i, 'tv'));
    },

    async fetchTopAnime() {
        const d = await this._jikan('/top/anime', { limit: 20 });
        return (d.data || []).filter(a => a.images?.jpg?.large_image_url).slice(0, 20)
            .map(a => this._normJikan(a));
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
        const d = await this._jikan('/anime', { q: query, limit: 12, sfw: true });
        return (d.data || []).filter(a => a.images?.jpg?.large_image_url).slice(0, 12)
            .map(a => this._normJikan(a));
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

    async fetchAnimeDetails(id) {
        const d = await this._jikan(`/anime/${id}/full`);
        return this._normJikan(d.data || {});
    },

    async fetchDetails(id, mediaType) {
        try {
            if (mediaType === 'movie') return await this.fetchMovieDetails(id);
            if (mediaType === 'tv')    return await this.fetchTVDetails(id);
            if (mediaType === 'anime') return await this.fetchAnimeDetails(id);
        } catch (e) {
            console.warn('fetchDetails failed:', e.message);
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

        prompt += `\nBased on the conversation above (especially the User's last message), provide exactly 4 recommendations they have NOT watched yet. Return ONLY a raw JSON array (no markdown, no code blocks). Each element:
{"title":"<title>","type":"movie"|"tv"|"anime","reason":"<max 12 words>"}`;

        try {
            console.log('Attempting AI recommendation via Cloudflare Proxy...');
            const res = await fetch(CONFIG.AI_PROXY_URL, {
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
};
