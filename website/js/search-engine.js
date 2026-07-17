/**
 * Loopa Search Engine
 * Implements a prefix Trie and Levenshtein fuzzy string matching for client-side search indexing,
 * caching watchlist items, trending/popular items, and Gemini query corrections.
 */

class TrieNode {
    constructor() {
        this.children = {};
        this.titles = new Set(); // Stores full lowercase titles matching this prefix
    }
}

class Trie {
    constructor() {
        this.root = new TrieNode();
    }

    insert(word, title) {
        let node = this.root;
        for (const char of word) {
            if (!node.children[char]) {
                node.children[char] = new TrieNode();
            }
            node = node.children[char];
            node.titles.add(title);
        }
    }

    search(prefix) {
        let node = this.root;
        for (const char of prefix) {
            if (!node.children[char]) return [];
            node = node.children[char];
        }
        return Array.from(node.titles);
    }
}

const LoopaSearchEngine = {
    trie: new Trie(),
    mediaMap: new Map(), // lowercase title -> original MediaItem
    queryCorrectionCache: new Map(), // raw query -> corrected query
    stopwords: new Set(['the', 'and', 'for', 'with', 'a', 'an', 'of', 'in', 'to', 'is', 'on', 'at']),

    /**
     * Clear the search engine index and media map.
     */
    clearIndex() {
        this.trie = new Trie();
        this.mediaMap.clear();
    },

    /**
     * Index a single MediaItem.
     * @param {Object} item - Normalized MediaItem
     */
    indexMediaItem(item) {
        if (!item || !item.title) return;
        const fullTitle = item.title.toLowerCase().trim();
        
        // Map lowercase title to original item (avoid overwriting higher-scored items if possible)
        const existing = this.mediaMap.get(fullTitle);
        if (!existing || (item.score && (!existing.score || item.score > existing.score))) {
            this.mediaMap.set(fullTitle, item);
        }

        // 1. Index full title as a single phrase
        const cleanTitle = fullTitle.replace(/[^a-z0-9\s]/g, '').replace(/\s+/g, ' ').trim();
        if (cleanTitle) {
            this.trie.insert(cleanTitle, fullTitle);
        }

        // 2. Index individual words to allow sub-word matching (e.g. "japan" matching "Doraemon Japan")
        const words = cleanTitle.split(' ');
        words.forEach(word => {
            if (word.length >= 3 && !this.stopwords.has(word)) {
                this.trie.insert(word, fullTitle);
            }
        });
    },

    /**
     * Index a list of MediaItems.
     * @param {Array} items - Array of Normalized MediaItems
     */
    indexMediaItems(items) {
        if (!Array.isArray(items)) return;
        items.forEach(item => this.indexMediaItem(item));
        console.log(`[SearchEngine] Indexed ${items.length} items. Total unique targets: ${this.mediaMap.size}`);
    },

    /**
     * Calculate Levenshtein Distance between two strings.
     */
    levenshteinDistance(str1, str2) {
        const track = Array(str2.length + 1).fill(null).map(() => Array(str1.length + 1).fill(null));
        for (let i = 0; i <= str1.length; i += 1) track[0][i] = i;
        for (let j = 0; j <= str2.length; j += 1) track[j][0] = j;
        for (let j = 1; j <= str2.length; j += 1) {
            for (let i = 1; i <= str1.length; i += 1) {
                const indicator = str1[i - 1] === str2[j - 1] ? 0 : 1;
                track[j][i] = Math.min(
                    track[j][i - 1] + 1, // deletion
                    track[j - 1][i] + 1, // insertion
                    track[j - 1][i - 1] + indicator // substitution
                );
            }
        }
        return track[str2.length][str1.length];
    },

    /**
     * Query autocomplete suggestions.
     * Combines Trie prefix matches, sub-word prefix matches, and falls back to Levenshtein fuzzy match.
     * @param {string} rawQuery
     * @param {number} limit
     * @returns {Array<Object>} List of matched MediaItems
     */
    getSuggestions(rawQuery, limit = 5) {
        const query = rawQuery.toLowerCase().trim().replace(/[^a-z0-9\s]/g, '').replace(/\s+/g, ' ');
        if (!query) return [];

        // 1. Attempt prefix search
        const matchedTitles = this.trie.search(query);
        let results = [];
        const seen = new Set();

        matchedTitles.forEach(t => {
            if (seen.size >= limit) return;
            const item = this.mediaMap.get(t);
            if (item && !seen.has(item.id + '_' + item.mediaType)) {
                results.push(item);
                seen.add(item.id + '_' + item.mediaType);
            }
        });

        // 2. If results are less than limit, try individual word prefix matches
        if (results.length < limit) {
            const queryWords = query.split(' ');
            if (queryWords.length > 1) {
                // Find items matching all query words as prefixes
                for (const [title, item] of this.mediaMap.entries()) {
                    if (seen.size >= limit) break;
                    if (seen.has(item.id + '_' + item.mediaType)) continue;

                    const titleWords = title.split(/\s+/);
                    const allWordsMatch = queryWords.every(qWord => 
                        titleWords.some(tWord => tWord.startsWith(qWord))
                    );

                    if (allWordsMatch) {
                        results.push(item);
                        seen.add(item.id + '_' + item.mediaType);
                    }
                }
            }
        }

        // 3. Fallback: Fuzzy matching using Levenshtein distance on words
        if (results.length < limit) {
            const fuzzyCandidates = [];
            const maxErrors = Math.min(2, Math.floor(query.length / 2));

            for (const [title, item] of this.mediaMap.entries()) {
                if (seen.has(item.id + '_' + item.mediaType)) continue;

                // Simple check: is Levenshtein distance of the entire string close?
                const dist = this.levenshteinDistance(query, title);
                if (dist <= maxErrors) {
                    fuzzyCandidates.push({ item, score: dist, type: 'full' });
                    continue;
                }

                // Check Levenshtein distance of individual words
                const titleWords = title.replace(/[^a-z0-9\s]/g, '').split(/\s+/);
                const queryWords = query.split(' ');
                let matchFound = false;

                for (const qWord of queryWords) {
                    if (qWord.length < 3) continue;
                    for (const tWord of titleWords) {
                        if (tWord.length < 3) continue;
                        const wordDist = this.levenshteinDistance(qWord, tWord);
                        const wordMaxErrors = Math.min(1, Math.floor(qWord.length / 3));
                        if (wordDist <= wordMaxErrors) {
                            fuzzyCandidates.push({ item, score: wordDist, type: 'word' });
                            matchFound = true;
                            break;
                        }
                    }
                    if (matchFound) break;
                }
            }

            // Sort fuzzy matches: lowest distance first, full phrase match preferred over word matches
            fuzzyCandidates.sort((a, b) => {
                if (a.type !== b.type) return a.type === 'full' ? -1 : 1;
                return a.score - b.score;
            });

            fuzzyCandidates.forEach(cand => {
                if (seen.size >= limit) return;
                results.push(cand.item);
                seen.add(cand.item.id + '_' + cand.item.mediaType);
            });
        }

        return results;
    },

    /**
     * Cache a corrected query mapping.
     */
    cacheCorrection(query, corrected) {
        if (!query || !corrected) return;
        this.queryCorrectionCache.set(query.toLowerCase().trim(), corrected.trim());
    },

    /**
     * Retrieve cached correction.
     */
    getCachedCorrection(query) {
        if (!query) return null;
        return this.queryCorrectionCache.get(query.toLowerCase().trim()) || null;
    }
};

window.LoopaSearchEngine = LoopaSearchEngine;
