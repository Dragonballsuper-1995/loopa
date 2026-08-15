/**
 * Loopa Web — Data Portability Suite (Portability Engine)
 * 1-Click Universal Watchlist Import & Export.
 * Supports Loopa JSON/CSV, Letterboxd CSV, IMDb CSV, MyAnimeList XML/JSON, and Trakt.tv JSON.
 */

const Portability = {
    /**
     * Converts a full watchlist to Loopa Universal JSON.
     */
    exportJSON(items) {
        const payload = {
            version: "2.0.0",
            exportedAt: new Date().toISOString(),
            source: "Loopa Web",
            totalItems: items.length,
            watchlist: items.map(item => ({
                id: item.id,
                title: item.title,
                mediaType: item.media_type,
                listName: item.list_name || 'Watching',
                imageUrl: item.image_url || null,
                date: item.date || null,
                score: item.score || null,
                userRating: item.user_rating || null,
                currentSeason: item.current_season || 1,
                currentEpisode: item.current_episode || 0,
                totalEpisodes: item.total_episodes || 0,
                totalSeasons: item.total_seasons || 0,
                progressString: item.progress_string || null,
                personalNotes: item.personal_notes || null,
                runtime: item.runtime || null,
                genres: item.genres || null,
                directorStudio: item.director_studio || null,
                updatedAt: item.updated_at || new Date().toISOString()
            }))
        };
        const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
        this._downloadBlob(blob, `loopa_watchlist_${this._getDateStamp()}.json`);
    },

    /**
     * Converts a full watchlist to RFC 4180 standard CSV.
     */
    exportCSV(items) {
        const headers = [
            'Title', 'Year', 'MediaType', 'ListName', 'Score', 'UserRating',
            'CurrentSeason', 'CurrentEpisode', 'TotalEpisodes', 'TotalSeasons',
            'Genres', 'DirectorStudio', 'PersonalNotes', 'DateAdded'
        ];

        const rows = items.map(i => [
            this._escapeCSV(i.title || ''),
            this._escapeCSV(i.date || ''),
            this._escapeCSV(i.media_type || 'movie'),
            this._escapeCSV(i.list_name || 'Watching'),
            i.score != null ? i.score : '',
            i.user_rating != null ? i.user_rating : '',
            i.current_season || 1,
            i.current_episode || 0,
            i.total_episodes || 0,
            i.total_seasons || 0,
            this._escapeCSV(i.genres || ''),
            this._escapeCSV(i.director_studio || ''),
            this._escapeCSV(i.personal_notes || ''),
            this._escapeCSV(i.updated_at || new Date().toISOString())
        ]);

        const csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\r\n');
        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        this._downloadBlob(blob, `loopa_watchlist_${this._getDateStamp()}.csv`);
    },

    /**
     * Parses uploaded text content from JSON, CSV, or XML into normalized import candidates.
     */
    parseContent(content, fileName = '') {
        const trimmed = content.trim();
        const lowerName = fileName.toLowerCase();

        // 1. JSON Detection (Loopa or Trakt or generic)
        if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
            try {
                const parsed = JSON.parse(trimmed);
                return this._parseJSON(parsed);
            } catch (e) {
                console.warn('[Portability] Failed parsing JSON:', e);
            }
        }

        // 2. MAL XML Detection
        if (trimmed.startsWith('<?xml') || trimmed.includes('<myanimelist>') || trimmed.includes('<anime>')) {
            return this._parseMalXML(trimmed);
        }

        // 3. CSV Detection (Letterboxd, IMDb, Loopa, Generic)
        return this._parseCSV(trimmed, lowerName);
    },

    /**
     * Enriches parsed candidates with TMDB/Jikan metadata if missing IDs, and saves to database.
     */
    async importWatchlist(candidates, userId, onProgress) {
        if (!Array.isArray(candidates) || candidates.length === 0) {
            return { total: 0, imported: 0, failed: 0 };
        }

        let imported = 0;
        let failed = 0;
        const total = candidates.length;
        const finalRows = [];

        for (let i = 0; i < total; i++) {
            const cand = candidates[i];
            if (onProgress) {
                onProgress(i + 1, total, cand.title || 'Unknown');
            }

            try {
                let enriched = cand;

                // If candidate lacks TMDB/MAL ID or poster, query TMDB or Jikan API
                if (!cand.id || !cand.imageUrl) {
                    enriched = await this._enrichCandidate(cand);
                }

                if (enriched && enriched.id) {
                    const row = {
                        id: enriched.id,
                        user_id: userId,
                        title: enriched.title,
                        image_url: enriched.imageUrl || null,
                        date: enriched.date || null,
                        score: enriched.score || null,
                        list_name: enriched.listName || 'Watched',
                        media_type: enriched.mediaType || 'movie',
                        current_season: enriched.currentSeason || 1,
                        current_episode: enriched.currentEpisode || 0,
                        total_episodes: enriched.totalEpisodes || 0,
                        total_seasons: enriched.totalSeasons || 0,
                        progress_string: enriched.progressString || null,
                        user_rating: enriched.userRating || null,
                        personal_notes: enriched.personalNotes || null,
                        runtime: enriched.runtime || null,
                        genres: enriched.genres || null,
                        director_studio: enriched.directorStudio || null,
                        updated_at: new Date().toISOString()
                    };

                    finalRows.push(row);
                    imported++;
                } else {
                    failed++;
                }
            } catch (e) {
                console.warn(`[Portability] Failed importing ${cand.title}:`, e);
                failed++;
            }

            // Small yield to keep UI responsive
            if (i % 5 === 0) {
                await new Promise(r => setTimeout(r, 10));
            }
        }

        if (finalRows.length > 0) {
            // Save to IDBStore and LocalStorage
            if (window.IDBStore) {
                await window.IDBStore.saveWatchlistForUser(userId, finalRows);
            }
            const existingLocal = JSON.parse(localStorage.getItem(`loopa_wl_${userId}`) || '[]');
            const map = new Map();
            existingLocal.forEach(item => map.set(`${item.id}_${item.media_type}`, item));
            finalRows.forEach(item => map.set(`${item.id}_${item.media_type}`, item));
            const merged = Array.from(map.values());
            localStorage.setItem(`loopa_wl_${userId}`, JSON.stringify(merged));

            // Queue bulk ADD operations for cloud sync
            finalRows.forEach(row => {
                OfflineSync.enqueue({ type: 'ADD', data: row });
            });
        }

        return { total, imported, failed, items: finalRows };
    },

    // ── Internal Parsers & Enrichers ──────────────────────────────────────────

    _parseJSON(data) {
        const rawList = Array.isArray(data) ? data : (data.watchlist || data.items || data.movies || []);
        return rawList.map(item => {
            // Trakt format support
            if (item.movie) {
                return {
                    id: item.movie.ids?.tmdb || null,
                    title: item.movie.title,
                    date: item.movie.year ? String(item.movie.year) : null,
                    mediaType: 'movie',
                    listName: 'Watched',
                    userRating: item.rating ? item.rating : null
                };
            }
            if (item.show) {
                return {
                    id: item.show.ids?.tmdb || null,
                    title: item.show.title,
                    date: item.show.year ? String(item.show.year) : null,
                    mediaType: 'tv',
                    listName: 'Watched',
                    userRating: item.rating ? item.rating : null
                };
            }

            // Loopa format
            return {
                id: item.id || null,
                title: item.title || item.name || '',
                imageUrl: item.imageUrl || item.image_url || item.posterUrl || null,
                date: item.date || (item.year ? String(item.year) : null),
                score: item.score != null ? parseFloat(item.score) : null,
                listName: item.listName || item.list_name || 'Watched',
                mediaType: item.mediaType || item.media_type || 'movie',
                currentSeason: item.currentSeason || item.current_season || 1,
                currentEpisode: item.currentEpisode || item.current_episode || 0,
                totalEpisodes: item.totalEpisodes || item.total_episodes || 0,
                totalSeasons: item.totalSeasons || item.total_seasons || 0,
                userRating: item.userRating || item.user_rating || null,
                personalNotes: item.personalNotes || item.personal_notes || null,
                genres: item.genres || null,
                directorStudio: item.directorStudio || item.director_studio || null
            };
        }).filter(i => i.title.length > 0);
    },

    _parseMalXML(xml) {
        const candidates = [];
        const animeBlocks = xml.split(/<anime>/i).slice(1);

        for (const block of animeBlocks) {
            const titleMatch = block.match(/<series_title><!\[CDATA\[(.*?)\]\]><\/series_title>/i) || block.match(/<series_title>(.*?)<\/series_title>/i);
            const idMatch = block.match(/<series_animedb_id>(.*?)<\/series_animedb_id>/i);
            const watchedEpMatch = block.match(/<my_watched_episodes>(.*?)<\/my_watched_episodes>/i);
            const scoreMatch = block.match(/<my_score>(.*?)<\/my_score>/i);
            const statusMatch = block.match(/<my_status>(.*?)<\/my_status>/i); // 1: watching, 2: completed, 6: plan to watch

            if (titleMatch && titleMatch[1]) {
                const title = titleMatch[1].trim();
                const malId = idMatch ? parseInt(idMatch[1], 10) : null;
                const watchedEp = watchedEpMatch ? parseInt(watchedEpMatch[1], 10) : 0;
                const userScore = scoreMatch ? parseInt(scoreMatch[1], 10) : null;
                const statusCode = statusMatch ? parseInt(statusMatch[1], 10) : 2;

                candidates.push({
                    id: malId,
                    title: title,
                    mediaType: 'anime',
                    listName: statusCode === 1 ? 'Watching' : 'Watched',
                    currentEpisode: watchedEp,
                    userRating: userScore && userScore > 0 ? userScore : null
                });
            }
        }
        return candidates;
    },

    _parseCSV(csvText, fileName = '') {
        const lines = this._splitCSVLines(csvText);
        if (lines.length < 2) return [];

        const headerLine = lines[0];
        const headers = this._parseCSVRow(headerLine).map(h => h.trim().toLowerCase());

        const isLetterboxd = headers.includes('letterboxd uri') || (headers.includes('name') && headers.includes('year'));
        const isIMDb = headers.includes('const') && headers.includes('title type');
        const isDefaultWatching = fileName.includes('watchlist') || fileName.includes('to_watch') || fileName.includes('planning');

        const results = [];

        for (let i = 1; i < lines.length; i++) {
            const line = lines[i].trim();
            if (!line) continue;
            const values = this._parseCSVRow(line);

            if (isLetterboxd) {
                // Letterboxd: Name, Year, Rating (0.5 - 5.0) or Date, Name, Year
                const nameIdx = headers.indexOf('name') >= 0 ? headers.indexOf('name') : headers.indexOf('title');
                const yearIdx = headers.indexOf('year');
                const ratingIdx = headers.indexOf('rating');

                if (nameIdx >= 0 && values[nameIdx]) {
                    const title = values[nameIdx].trim();
                    const year = yearIdx >= 0 ? values[yearIdx]?.trim() : null;
                    const ratingRaw = ratingIdx >= 0 ? parseFloat(values[ratingIdx]) : null;
                    const userRating = ratingRaw ? Math.round(ratingRaw * 2) : null; // Convert 5 stars to 10 scale

                    results.push({
                        title,
                        date: year,
                        mediaType: 'movie',
                        listName: isDefaultWatching ? 'Watching' : 'Watched',
                        userRating
                    });
                }
            } else if (isIMDb) {
                // IMDb: Title, Title Type, Year, Your Rating, Genres, Directors
                const titleIdx = headers.indexOf('title');
                const typeIdx = headers.indexOf('title type');
                const yearIdx = headers.indexOf('year');
                const ratingIdx = headers.indexOf('your rating');
                const genreIdx = headers.indexOf('genres');
                const directorIdx = headers.indexOf('directors');

                if (titleIdx >= 0 && values[titleIdx]) {
                    const title = values[titleIdx].trim();
                    const rawType = typeIdx >= 0 ? values[typeIdx]?.trim().toLowerCase() : '';
                    const mediaType = (rawType.includes('tv') || rawType.includes('series')) ? 'tv' : 'movie';
                    const year = yearIdx >= 0 ? values[yearIdx]?.trim() : null;
                    const userRating = ratingIdx >= 0 && values[ratingIdx] ? parseInt(values[ratingIdx], 10) : null;
                    const genres = genreIdx >= 0 ? values[genreIdx]?.trim() : null;
                    const director = directorIdx >= 0 ? values[directorIdx]?.trim() : null;

                    results.push({
                        title,
                        date: year,
                        mediaType,
                        listName: isDefaultWatching ? 'Watching' : 'Watched',
                        userRating,
                        genres,
                        directorStudio: director
                    });
                }
            } else {
                // Loopa or Generic CSV
                const titleIdx = headers.indexOf('title') >= 0 ? headers.indexOf('title') : headers.indexOf('name');
                const yearIdx = headers.indexOf('year') >= 0 ? headers.indexOf('year') : headers.indexOf('date');
                const typeIdx = headers.indexOf('mediatype') >= 0 ? headers.indexOf('mediatype') : headers.indexOf('type');
                const listIdx = headers.indexOf('listname') >= 0 ? headers.indexOf('listname') : headers.indexOf('status');
                const scoreIdx = headers.indexOf('score');
                const ratingIdx = headers.indexOf('userrating') >= 0 ? headers.indexOf('userrating') : headers.indexOf('rating');
                const notesIdx = headers.indexOf('personalnotes') >= 0 ? headers.indexOf('personalnotes') : headers.indexOf('notes');

                if (titleIdx >= 0 && values[titleIdx]) {
                    const title = values[titleIdx].trim();
                    results.push({
                        title,
                        date: yearIdx >= 0 ? values[yearIdx]?.trim() : null,
                        mediaType: typeIdx >= 0 ? values[typeIdx]?.trim().toLowerCase() : 'movie',
                        listName: listIdx >= 0 && values[listIdx] ? values[listIdx].trim() : (isDefaultWatching ? 'Watching' : 'Watched'),
                        score: scoreIdx >= 0 && values[scoreIdx] ? parseFloat(values[scoreIdx]) : null,
                        userRating: ratingIdx >= 0 && values[ratingIdx] ? parseInt(values[ratingIdx], 10) : null,
                        personalNotes: notesIdx >= 0 ? values[notesIdx]?.trim() : null
                    });
                }
            }
        }

        return results;
    },

    async _enrichCandidate(candidate) {
        const title = candidate.title;
        const mediaType = candidate.mediaType || 'movie';

        // Anime enrichment via Jikan / SearchEngine
        if (mediaType === 'anime') {
            try {
                const searchRes = await API.searchAnime(title);
                if (searchRes && searchRes.length > 0) {
                    const best = searchRes[0];
                    return {
                        ...candidate,
                        id: best.id,
                        imageUrl: best.posterUrl || candidate.imageUrl,
                        score: best.score || candidate.score,
                        date: best.year || candidate.date,
                        totalEpisodes: best.totalEpisodes || candidate.totalEpisodes || 0
                    };
                }
            } catch {}
        }

        // Movie / TV enrichment via TMDB Proxy
        try {
            const searchRes = await API.searchMulti(title);
            if (searchRes && searchRes.length > 0) {
                // Find matching type if available, else first result
                const best = searchRes.find(r => r.mediaType === mediaType) || searchRes[0];
                return {
                    ...candidate,
                    id: best.id,
                    title: best.title,
                    mediaType: best.mediaType,
                    imageUrl: best.posterUrl || candidate.imageUrl,
                    score: best.score || candidate.score,
                    date: best.year || candidate.date
                };
            }
        } catch (e) {
            console.warn(`[Portability] TMDB enrichment error for ${title}:`, e);
        }

        // Fallback synthetic ID if network match not found
        return {
            ...candidate,
            id: candidate.id || Math.floor(Math.random() * 800000) + 100000
        };
    },

    _splitCSVLines(text) {
        const lines = [];
        let cur = '';
        let inQuotes = false;
        for (let i = 0; i < text.length; i++) {
            const char = text[i];
            if (char === '"') inQuotes = !inQuotes;
            if ((char === '\n' || char === '\r') && !inQuotes) {
                if (cur.trim()) lines.push(cur);
                cur = '';
                if (char === '\r' && text[i + 1] === '\n') i++;
            } else {
                cur += char;
            }
        }
        if (cur.trim()) lines.push(cur);
        return lines;
    },

    _parseCSVRow(row) {
        const values = [];
        let cur = '';
        let inQuotes = false;
        for (let i = 0; i < row.length; i++) {
            const char = row[i];
            if (char === '"') {
                if (inQuotes && row[i + 1] === '"') {
                    cur += '"';
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (char === ',' && !inQuotes) {
                values.push(cur);
                cur = '';
            } else {
                cur += char;
            }
        }
        values.push(cur);
        return values;
    },

    _escapeCSV(val) {
        const str = String(val == null ? '' : val);
        if (str.includes(',') || str.includes('"') || str.includes('\n') || str.includes('\r')) {
            return `"${str.replace(/"/g, '""')}"`;
        }
        return str;
    },

    _downloadBlob(blob, filename) {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    },

    _getDateStamp() {
        const d = new Date();
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    }
};

if (typeof window !== 'undefined') {
    window.Portability = Portability;
}
