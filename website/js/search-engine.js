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
     * Calculate Levenshtein Distance between two strings using 2-row O(min(N,M)) memory.
     */
    levenshteinDistance(s1, s2) {
        if (s1 === s2) return 0;
        if (s1.length === 0) return s2.length;
        if (s2.length === 0) return s1.length;
        if (s1.length > s2.length) {
            const temp = s1; s1 = s2; s2 = temp;
        }
        let prev = new Array(s1.length + 1);
        let curr = new Array(s1.length + 1);
        for (let i = 0; i <= s1.length; i++) prev[i] = i;
        for (let j = 1; j <= s2.length; j++) {
            curr[0] = j;
            const c2 = s2.charCodeAt(j - 1);
            for (let i = 1; i <= s1.length; i++) {
                const cost = s1.charCodeAt(i - 1) === c2 ? 0 : 1;
                curr[i] = Math.min(
                    prev[i] + 1,       // deletion
                    curr[i - 1] + 1,   // insertion
                    prev[i - 1] + cost // substitution
                );
            }
            const swap = prev; prev = curr; curr = swap;
        }
        return prev[s1.length];
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

        // If prefix search already satisfied the limit, return immediately (zero wasted cycles)
        if (results.length >= limit) return results;

        // 2. If results are less than limit, try individual word prefix matches
        const queryWords = query.split(' ');
        if (queryWords.length > 1) {
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

        // 3. Fallback: Fuzzy matching ONLY if zero prefix results were found and query is at least 3 chars
        if (results.length === 0 && query.length >= 3) {
            const fuzzyCandidates = [];
            const maxErrors = Math.min(2, Math.floor(query.length / 3));

            for (const [title, item] of this.mediaMap.entries()) {
                if (seen.has(item.id + '_' + item.mediaType)) continue;

                // Length pre-check: mathematical lower bound for Levenshtein
                if (Math.abs(query.length - title.length) <= maxErrors) {
                    const dist = this.levenshteinDistance(query, title);
                    if (dist <= maxErrors) {
                        fuzzyCandidates.push({ item, score: dist, type: 'full' });
                        continue;
                    }
                }

                // Check Levenshtein distance on words
                const titleWords = title.replace(/[^a-z0-9\s]/g, '').split(/\s+/);
                let matchFound = false;

                for (const qWord of queryWords) {
                    if (qWord.length < 3) continue;
                    for (const tWord of titleWords) {
                        if (tWord.length < 3) continue;
                        if (Math.abs(qWord.length - tWord.length) > 1) continue;
                        const wordDist = this.levenshteinDistance(qWord, tWord);
                        if (wordDist <= 1) {
                            fuzzyCandidates.push({ item, score: wordDist, type: 'word' });
                            matchFound = true;
                            break;
                        }
                    }
                    if (matchFound) break;
                }
            }

            // Sort fuzzy matches: lowest distance first, full phrase match preferred
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
     * Alias for getSuggestions — allows LoopaSearchEngine.search(query, limit) calls.
     */
    search(query, limit = 5) {
        return this.getSuggestions(query, limit);
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
