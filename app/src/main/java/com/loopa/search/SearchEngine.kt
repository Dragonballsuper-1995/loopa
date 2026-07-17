package com.loopa.search

import com.loopa.model.TmdbMovie
import com.loopa.db.MediaItemEntity
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class TrieNode {
    val children = ConcurrentHashMap<Char, TrieNode>()
    val titles = ConcurrentHashMap.newKeySet<String>() // Stores full lowercase titles matching this prefix
}

class Trie {
    private val root = TrieNode()

    fun insert(word: String, title: String) {
        var node = root
        for (char in word) {
            node = node.children.computeIfAbsent(char) { TrieNode() }
            node.titles.add(title)
        }
    }

    fun search(prefix: String): List<String> {
        var node = root
        for (char in prefix) {
            node = node.children[char] ?: return emptyList()
        }
        return node.titles.toList()
    }
}

object SearchEngine {
    private val trie = Trie()
    private val mediaMap = ConcurrentHashMap<String, TmdbMovie>() // lowercase title -> original TmdbMovie
    private val queryCorrectionCache = ConcurrentHashMap<String, String>() // raw query -> corrected query
    private val stopwords = setOf("the", "and", "for", "with", "a", "an", "of", "in", "to", "is", "on", "at")

    fun clearIndex() {
        // Since we can't easily recreate a private val reference directly or clear it, we recreate node links
        // We can just wipe out the Trie by creating a new Trie instance or clearing the maps
        // Let's make Trie class mutable or instantiate a new one. We can do:
        synchronized(this) {
            // Re-instantiate Trie is fine if we make it a var. Let's make it a var!
            _trie = Trie()
            mediaMap.clear()
        }
    }

    private var _trie = Trie()

    fun indexMediaItem(item: TmdbMovie) {
        val title = item.title ?: item.name ?: return
        val fullTitle = title.lowercase(Locale.getDefault()).trim()
        if (fullTitle.isEmpty()) return

        val existing = mediaMap[fullTitle]
        if (existing == null || (item.voteAverage ?: 0.0) > (existing.voteAverage ?: 0.0)) {
            mediaMap[fullTitle] = item
        }

        // 1. Index full title as a single phrase
        val cleanTitle = fullTitle.replace(Regex("[^a-z0-9\\s]"), "").replace(Regex("\\s+"), " ").trim()
        if (cleanTitle.isNotEmpty()) {
            _trie.insert(cleanTitle, fullTitle)
        }

        // 2. Index individual words
        val words = cleanTitle.split(" ")
        for (word in words) {
            if (word.length >= 3 && !stopwords.contains(word)) {
                _trie.insert(word, fullTitle)
            }
        }
    }

    fun indexMediaItems(items: List<TmdbMovie>) {
        for (item in items) {
            indexMediaItem(item)
        }
    }

    fun indexMediaItemEntity(entity: MediaItemEntity) {
        val movie = TmdbMovie(
            id = entity.id,
            title = entity.title,
            name = null,
            overview = entity.personalNotes,
            posterPath = entity.imageUrl?.replace("https://image.tmdb.org/t/p/w500", "")?.replace("https://loopa-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w500", ""),
            backdropPath = null,
            voteAverage = entity.score,
            releaseDate = entity.date,
            firstAirDate = null,
            mediaType = entity.mediaType,
            popularity = 0.0,
            genreIds = null
        )
        indexMediaItem(movie)
    }

    fun indexMediaItemEntities(entities: List<MediaItemEntity>) {
        for (entity in entities) {
            indexMediaItemEntity(entity)
        }
    }

    private fun levenshteinDistance(str1: String, str2: String): Int {
        val dp = Array(str2.length + 1) { IntArray(str1.length + 1) }
        for (i in 0..str1.length) dp[0][i] = i
        for (j in 0..str2.length) dp[j][0] = j
        for (j in 1..str2.length) {
            for (i in 1..str1.length) {
                val indicator = if (str1[i - 1] == str2[j - 1]) 0 else 1
                dp[j][i] = minOf(
                    dp[j][i - 1] + 1, // deletion
                    dp[j - 1][i] + 1, // insertion
                    dp[j - 1][i - 1] + indicator // substitution
                )
            }
        }
        return dp[str2.length][str1.length]
    }

    fun getSuggestions(rawQuery: String, limit: Int = 5): List<TmdbMovie> {
        val query = rawQuery.lowercase(Locale.getDefault()).trim().replace(Regex("[^a-z0-9\\s]"), "").replace(Regex("\\s+"), " ")
        if (query.isEmpty()) return emptyList()

        val matchedTitles = _trie.search(query)
        val results = CopyOnWriteArrayList<TmdbMovie>()
        val seen = ConcurrentHashMap.newKeySet<String>()

        for (t in matchedTitles) {
            if (seen.size >= limit) break
            val item = mediaMap[t]
            if (item != null) {
                val key = "${item.id}_${item.mediaType ?: "movie"}"
                if (seen.add(key)) {
                    results.add(item)
                }
            }
        }

        // 2. If results < limit, try individual word prefix matches
        if (results.size < limit) {
            val queryWords = query.split(" ")
            if (queryWords.size > 1) {
                for ((title, item) in mediaMap.entries) {
                    if (seen.size >= limit) break
                    val key = "${item.id}_${item.mediaType ?: "movie"}"
                    if (seen.contains(key)) continue

                    val titleWords = title.split(Regex("\\s+"))
                    val allWordsMatch = queryWords.all { qWord ->
                        titleWords.any { tWord -> tWord.startsWith(qWord) }
                    }

                    if (allWordsMatch) {
                        if (seen.add(key)) {
                            results.add(item)
                        }
                    }
                }
            }
        }

        // 3. Levenshtein fuzzy match
        if (results.size < limit) {
            val fuzzyCandidates = CopyOnWriteArrayList<FuzzyCandidate>()
            val maxErrors = minOf(2, query.length / 2)

            for ((title, item) in mediaMap.entries) {
                val key = "${item.id}_${item.mediaType ?: "movie"}"
                if (seen.contains(key)) continue

                val dist = levenshteinDistance(query, title)
                if (dist <= maxErrors) {
                    fuzzyCandidates.add(FuzzyCandidate(item, dist, "full"))
                    continue
                }

                val titleWords = title.replace(Regex("[^a-z0-9\\s]"), "").split(Regex("\\s+"))
                val queryWords = query.split(" ")
                var matchFound = false

                for (qWord in queryWords) {
                    if (qWord.length < 3) continue
                    for (tWord in titleWords) {
                        if (tWord.length < 3) continue
                        val wordDist = levenshteinDistance(qWord, tWord)
                        val wordMaxErrors = minOf(1, qWord.length / 3)
                        if (wordDist <= wordMaxErrors) {
                            fuzzyCandidates.add(FuzzyCandidate(item, wordDist, "word"))
                            matchFound = true
                            break
                        }
                    }
                    if (matchFound) break
                }
            }

            // Sort candidates
            val sorted = fuzzyCandidates.sortedWith(compareBy<FuzzyCandidate> { it.type != "full" }.thenBy { it.score })
            for (cand in sorted) {
                if (seen.size >= limit) break
                val key = "${cand.item.id}_${cand.item.mediaType ?: "movie"}"
                if (seen.add(key)) {
                    results.add(cand.item)
                }
            }
        }

        return results.toList()
    }

    fun cacheCorrection(query: String, corrected: String) {
        val q = query.lowercase(Locale.getDefault()).trim()
        val c = corrected.trim()
        if (q.isNotEmpty() && c.isNotEmpty()) {
            queryCorrectionCache[q] = c
        }
    }

    fun getCachedCorrection(query: String): String? {
        val q = query.lowercase(Locale.getDefault()).trim()
        return queryCorrectionCache[q]
    }

    private data class FuzzyCandidate(val item: TmdbMovie, val score: Int, val type: String)
}
