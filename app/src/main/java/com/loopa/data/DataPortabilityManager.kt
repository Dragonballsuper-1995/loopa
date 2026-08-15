package com.loopa.data

import com.loopa.db.MediaItemEntity
import com.loopa.network.NetworkModule
import com.loopa.repository.MediaRepository
import com.loopa.util.TmdbUrlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ImportCandidate(
    val id: Int? = null,
    val title: String,
    val imageUrl: String? = null,
    val date: String? = null,
    val score: Double? = null,
    val listName: String = "Watched",
    val mediaType: String = "movie",
    val currentSeason: Int = 1,
    val currentEpisode: Int = 0,
    val totalEpisodes: Int = 0,
    val totalSeasons: Int = 0,
    val progressString: String? = null,
    val userRating: Int? = null,
    val personalNotes: String? = null,
    val runtime: Int? = null,
    val genres: String? = null,
    val directorStudio: String? = null
)

data class ImportSummary(
    val total: Int,
    val imported: Int,
    val failed: Int
)

object DataPortabilityManager {

    /**
     * Converts a list of MediaItemEntity records into Loopa Universal JSON.
     */
    fun exportToJSON(items: List<MediaItemEntity>): String {
        val root = JSONObject()
        root.put("version", "2.0.0")
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
        root.put("source", "Loopa Android")
        root.put("totalItems", items.size)

        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("imageUrl", item.imageUrl ?: JSONObject.NULL)
            obj.put("date", item.date ?: JSONObject.NULL)
            obj.put("score", item.score ?: JSONObject.NULL)
            obj.put("listName", item.listName)
            obj.put("mediaType", item.mediaType)
            obj.put("currentSeason", item.currentSeason)
            obj.put("currentEpisode", item.currentEpisode)
            obj.put("totalEpisodes", item.totalEpisodes)
            obj.put("totalSeasons", item.totalSeasons)
            obj.put("progressString", item.progressString ?: JSONObject.NULL)
            obj.put("userRating", item.userRating ?: JSONObject.NULL)
            obj.put("personalNotes", item.personalNotes ?: JSONObject.NULL)
            obj.put("runtime", item.runtime ?: JSONObject.NULL)
            obj.put("genres", item.genres ?: JSONObject.NULL)
            obj.put("directorStudio", item.directorStudio ?: JSONObject.NULL)
            obj.put("updatedAt", item.updatedAt ?: JSONObject.NULL)
            array.put(obj)
        }
        root.put("watchlist", array)
        return root.toString(2)
    }

    /**
     * Converts a list of MediaItemEntity records into RFC 4180 standard CSV.
     */
    fun exportToCSV(items: List<MediaItemEntity>): String {
        val sb = StringBuilder()
        val headers = listOf(
            "Title", "Year", "MediaType", "ListName", "Score", "UserRating",
            "CurrentSeason", "CurrentEpisode", "TotalEpisodes", "TotalSeasons",
            "Genres", "DirectorStudio", "PersonalNotes", "DateAdded"
        )
        sb.append(headers.joinToString(",")).append("\r\n")

        for (i in items) {
            val row = listOf(
                escapeCSV(i.title),
                escapeCSV(i.date ?: ""),
                escapeCSV(i.mediaType),
                escapeCSV(i.listName),
                i.score?.toString() ?: "",
                i.userRating?.toString() ?: "",
                i.currentSeason.toString(),
                i.currentEpisode.toString(),
                i.totalEpisodes.toString(),
                i.totalSeasons.toString(),
                escapeCSV(i.genres ?: ""),
                escapeCSV(i.directorStudio ?: ""),
                escapeCSV(i.personalNotes ?: ""),
                escapeCSV(i.updatedAt ?: "")
            )
            sb.append(row.joinToString(",")).append("\r\n")
        }
        return sb.toString()
    }

    /**
     * Parses uploaded text content from JSON, CSV, or XML into normalized import candidates.
     */
    fun parseContent(content: String, fileName: String? = null): List<ImportCandidate> {
        val trimmed = content.trim()
        val lowerName = fileName?.lowercase(Locale.US) ?: ""

        // 1. JSON (Loopa / Trakt / Generic)
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return runCatching { parseJSON(trimmed) }.getOrDefault(emptyList())
        }

        // 2. MAL XML
        if (trimmed.startsWith("<?xml") || trimmed.contains("<myanimelist>") || trimmed.contains("<anime>")) {
            return runCatching { parseMalXML(trimmed) }.getOrDefault(emptyList())
        }

        // 3. CSV (Letterboxd, IMDb, Loopa, Generic)
        return runCatching { parseCSV(trimmed, lowerName) }.getOrDefault(emptyList())
    }

    /**
     * Enriches candidates missing IDs/images, creates MediaItemEntity objects, and saves via repository.
     */
    suspend fun enrichAndImport(
        candidates: List<ImportCandidate>,
        repository: MediaRepository,
        onProgress: (Int, Int, String) -> Unit
    ): ImportSummary = withContext(Dispatchers.IO) {
        if (candidates.isEmpty()) return@withContext ImportSummary(0, 0, 0)

        var imported = 0
        var failed = 0
        val total = candidates.size
        val entities = mutableListOf<MediaItemEntity>()

        for ((index, cand) in candidates.withIndex()) {
            onProgress(index + 1, total, cand.title)

            try {
                val enriched = if (cand.id != null && !cand.imageUrl.isNullOrEmpty()) {
                    cand
                } else {
                    enrichCandidate(cand)
                }

                if (enriched?.id != null) {
                    entities.add(
                        MediaItemEntity(
                            id = enriched.id,
                            title = enriched.title,
                            imageUrl = enriched.imageUrl,
                            date = enriched.date,
                            score = enriched.score,
                            listName = enriched.listName,
                            mediaType = enriched.mediaType,
                            currentSeason = enriched.currentSeason,
                            currentEpisode = enriched.currentEpisode,
                            totalEpisodes = enriched.totalEpisodes,
                            totalSeasons = enriched.totalSeasons,
                            progressString = enriched.progressString,
                            userRating = enriched.userRating,
                            personalNotes = enriched.personalNotes,
                            runtime = enriched.runtime,
                            genres = enriched.genres,
                            directorStudio = enriched.directorStudio,
                            updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                        )
                    )
                    imported++
                } else {
                    failed++
                }
            } catch (e: Exception) {
                android.util.Log.w("DataPortabilityManager", "Error importing ${cand.title}: ${e.message}")
                failed++
            }
        }

        if (entities.isNotEmpty()) {
            repository.insertMediaItems(entities)
        }

        ImportSummary(total = total, imported = imported, failed = failed)
    }

    // ── Internal Format Parsers ───────────────────────────────────────────────

    private fun parseJSON(jsonStr: String): List<ImportCandidate> {
        val candidates = mutableListOf<ImportCandidate>()
        if (jsonStr.startsWith("[")) {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                parseSingleJSONItem(obj)?.let { candidates.add(it) }
            }
        } else {
            val root = JSONObject(jsonStr)
            val array = root.optJSONArray("watchlist")
                ?: root.optJSONArray("items")
                ?: root.optJSONArray("movies")
                ?: JSONArray()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                parseSingleJSONItem(obj)?.let { candidates.add(it) }
            }
        }
        return candidates
    }

    private fun parseSingleJSONItem(obj: JSONObject): ImportCandidate? {
        // Trakt support
        val traktMovie = obj.optJSONObject("movie")
        if (traktMovie != null) {
            val title = traktMovie.optString("title")
            val year = traktMovie.optString("year").takeIf { it.isNotEmpty() }
            val ids = traktMovie.optJSONObject("ids")
            val tmdbId = ids?.optInt("tmdb")?.takeIf { it > 0 }
            val rating = obj.optInt("rating").takeIf { it > 0 }
            return ImportCandidate(
                id = tmdbId,
                title = title,
                date = year,
                mediaType = "movie",
                listName = "Watched",
                userRating = rating
            )
        }
        val traktShow = obj.optJSONObject("show")
        if (traktShow != null) {
            val title = traktShow.optString("title")
            val year = traktShow.optString("year").takeIf { it.isNotEmpty() }
            val ids = traktShow.optJSONObject("ids")
            val tmdbId = ids?.optInt("tmdb")?.takeIf { it > 0 }
            val rating = obj.optInt("rating").takeIf { it > 0 }
            return ImportCandidate(
                id = tmdbId,
                title = title,
                date = year,
                mediaType = "tv",
                listName = "Watched",
                userRating = rating
            )
        }

        // Loopa Universal
        val title = obj.optString("title").ifEmpty { obj.optString("name") }
        if (title.isEmpty()) return null

        val id = obj.optInt("id").takeIf { it > 0 }
        val imageUrl = obj.optString("imageUrl").ifEmpty { obj.optString("image_url").ifEmpty { obj.optString("posterUrl") } }.takeIf { it.isNotEmpty() }
        val date = obj.optString("date").ifEmpty { obj.optString("year") }.takeIf { it.isNotEmpty() }
        val score = if (obj.has("score") && !obj.isNull("score")) obj.optDouble("score") else null
        val listName = obj.optString("listName").ifEmpty { obj.optString("list_name", "Watched") }
        val mediaType = obj.optString("mediaType").ifEmpty { obj.optString("media_type", "movie") }
        val currentSeason = obj.optInt("currentSeason", obj.optInt("current_season", 1))
        val currentEpisode = obj.optInt("currentEpisode", obj.optInt("current_episode", 0))
        val totalEpisodes = obj.optInt("totalEpisodes", obj.optInt("total_episodes", 0))
        val totalSeasons = obj.optInt("totalSeasons", obj.optInt("total_seasons", 0))
        val userRating = if (obj.has("userRating") && !obj.isNull("userRating")) obj.optInt("userRating") else (if (obj.has("user_rating") && !obj.isNull("user_rating")) obj.optInt("user_rating") else null)
        val personalNotes = obj.optString("personalNotes").ifEmpty { obj.optString("personal_notes") }.takeIf { it.isNotEmpty() }
        val genres = obj.optString("genres").takeIf { it.isNotEmpty() }
        val directorStudio = obj.optString("directorStudio").ifEmpty { obj.optString("director_studio") }.takeIf { it.isNotEmpty() }

        return ImportCandidate(
            id = id,
            title = title,
            imageUrl = imageUrl,
            date = date,
            score = score,
            listName = listName,
            mediaType = mediaType,
            currentSeason = currentSeason,
            currentEpisode = currentEpisode,
            totalEpisodes = totalEpisodes,
            totalSeasons = totalSeasons,
            userRating = userRating,
            personalNotes = personalNotes,
            genres = genres,
            directorStudio = directorStudio
        )
    }

    private fun parseMalXML(xml: String): List<ImportCandidate> {
        val candidates = mutableListOf<ImportCandidate>()
        val blocks = xml.split(Regex("(?i)<anime>")).drop(1)

        for (block in blocks) {
            val titleMatch = Regex("(?i)<series_title>(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?<\\/series_title>").find(block)
            val idMatch = Regex("(?i)<series_animedb_id>(\\d+)<\\/series_animedb_id>").find(block)
            val watchedEpMatch = Regex("(?i)<my_watched_episodes>(\\d+)<\\/my_watched_episodes>").find(block)
            val scoreMatch = Regex("(?i)<my_score>(\\d+)<\\/my_score>").find(block)
            val statusMatch = Regex("(?i)<my_status>(\\d+)<\\/my_status>").find(block)

            if (titleMatch != null) {
                val title = titleMatch.groupValues[1].trim()
                val malId = idMatch?.groupValues?.get(1)?.toIntOrNull()
                val watchedEp = watchedEpMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val userScore = scoreMatch?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it > 0 }
                val statusCode = statusMatch?.groupValues?.get(1)?.toIntOrNull() ?: 2

                candidates.add(
                    ImportCandidate(
                        id = malId,
                        title = title,
                        mediaType = "anime",
                        listName = if (statusCode == 1) "Watching" else "Watched",
                        currentEpisode = watchedEp,
                        userRating = userScore
                    )
                )
            }
        }
        return candidates
    }

    private fun parseCSV(csvText: String, fileName: String): List<ImportCandidate> {
        val lines = splitCSVLines(csvText)
        if (lines.size < 2) return emptyList()

        val headers = parseCSVRow(lines[0]).map { it.trim().lowercase(Locale.US) }
        val isLetterboxd = headers.contains("letterboxd uri") || (headers.contains("name") && headers.contains("year"))
        val isIMDb = headers.contains("const") && headers.contains("title type")
        val isDefaultWatching = fileName.contains("watchlist") || fileName.contains("planning")

        val candidates = mutableListOf<ImportCandidate>()

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            val values = parseCSVRow(line)

            if (isLetterboxd) {
                val nameIdx = if (headers.indexOf("name") >= 0) headers.indexOf("name") else headers.indexOf("title")
                val yearIdx = headers.indexOf("year")
                val ratingIdx = headers.indexOf("rating")

                if (nameIdx >= 0 && nameIdx < values.size && values[nameIdx].isNotEmpty()) {
                    val title = values[nameIdx].trim()
                    val year = if (yearIdx >= 0 && yearIdx < values.size) values[yearIdx].trim().takeIf { it.isNotEmpty() } else null
                    val rawRating = if (ratingIdx >= 0 && ratingIdx < values.size) values[ratingIdx].toDoubleOrNull() else null
                    val userRating = rawRating?.let { Math.round(it * 2).toInt() }

                    candidates.add(
                        ImportCandidate(
                            title = title,
                            date = year,
                            mediaType = "movie",
                            listName = if (isDefaultWatching) "Watching" else "Watched",
                            userRating = userRating
                        )
                    )
                }
            } else if (isIMDb) {
                val titleIdx = headers.indexOf("title")
                val typeIdx = headers.indexOf("title type")
                val yearIdx = headers.indexOf("year")
                val ratingIdx = headers.indexOf("your rating")
                val genreIdx = headers.indexOf("genres")
                val directorIdx = headers.indexOf("directors")

                if (titleIdx >= 0 && titleIdx < values.size && values[titleIdx].isNotEmpty()) {
                    val title = values[titleIdx].trim()
                    val rawType = if (typeIdx >= 0 && typeIdx < values.size) values[typeIdx].trim().lowercase(Locale.US) else ""
                    val mediaType = if (rawType.contains("tv") || rawType.contains("series")) "tv" else "movie"
                    val year = if (yearIdx >= 0 && yearIdx < values.size) values[yearIdx].trim().takeIf { it.isNotEmpty() } else null
                    val userRating = if (ratingIdx >= 0 && ratingIdx < values.size) values[ratingIdx].toIntOrNull() else null
                    val genres = if (genreIdx >= 0 && genreIdx < values.size) values[genreIdx].trim().takeIf { it.isNotEmpty() } else null
                    val director = if (directorIdx >= 0 && directorIdx < values.size) values[directorIdx].trim().takeIf { it.isNotEmpty() } else null

                    candidates.add(
                        ImportCandidate(
                            title = title,
                            date = year,
                            mediaType = mediaType,
                            listName = if (isDefaultWatching) "Watching" else "Watched",
                            userRating = userRating,
                            genres = genres,
                            directorStudio = director
                        )
                    )
                }
            } else {
                // Loopa or Generic CSV
                val titleIdx = if (headers.indexOf("title") >= 0) headers.indexOf("title") else headers.indexOf("name")
                val yearIdx = if (headers.indexOf("year") >= 0) headers.indexOf("year") else headers.indexOf("date")
                val typeIdx = if (headers.indexOf("mediatype") >= 0) headers.indexOf("mediatype") else headers.indexOf("type")
                val listIdx = if (headers.indexOf("listname") >= 0) headers.indexOf("listname") else headers.indexOf("status")
                val scoreIdx = headers.indexOf("score")
                val ratingIdx = if (headers.indexOf("userrating") >= 0) headers.indexOf("userrating") else headers.indexOf("rating")
                val notesIdx = if (headers.indexOf("personalnotes") >= 0) headers.indexOf("personalnotes") else headers.indexOf("notes")

                if (titleIdx >= 0 && titleIdx < values.size && values[titleIdx].isNotEmpty()) {
                    val title = values[titleIdx].trim()
                    candidates.add(
                        ImportCandidate(
                            title = title,
                            date = if (yearIdx >= 0 && yearIdx < values.size) values[yearIdx].trim().takeIf { it.isNotEmpty() } else null,
                            mediaType = if (typeIdx >= 0 && typeIdx < values.size) values[typeIdx].trim().lowercase(Locale.US) else "movie",
                            listName = if (listIdx >= 0 && listIdx < values.size && values[listIdx].isNotEmpty()) values[listIdx].trim() else (if (isDefaultWatching) "Watching" else "Watched"),
                            score = if (scoreIdx >= 0 && scoreIdx < values.size) values[scoreIdx].toDoubleOrNull() else null,
                            userRating = if (ratingIdx >= 0 && ratingIdx < values.size) values[ratingIdx].toIntOrNull() else null,
                            personalNotes = if (notesIdx >= 0 && notesIdx < values.size) values[notesIdx].trim().takeIf { it.isNotEmpty() } else null
                        )
                    )
                }
            }
        }

        return candidates
    }

    private suspend fun enrichCandidate(candidate: ImportCandidate): ImportCandidate? {
        val title = candidate.title
        val mediaType = candidate.mediaType

        if (mediaType == "anime") {
            try {
                val res = NetworkModule.jikanApi.searchAnime(title)
                val best = res.data.firstOrNull()
                if (best != null) {
                    return candidate.copy(
                        id = best.malId,
                        imageUrl = best.images?.jpg?.largeImageUrl ?: best.images?.jpg?.imageUrl,
                        score = best.score
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("DataPortabilityManager", "Jikan search error: ${e.message}")
            }
        }

        try {
            val res = NetworkModule.tmdbApi.searchMulti(
                apiKey = com.loopa.app.BuildConfig.TMDB_API_KEY,
                query = title
            )
            val best = res.results.firstOrNull { it.mediaType == mediaType } ?: res.results.firstOrNull()
            if (best != null) {
                return candidate.copy(
                    id = best.id,
                    title = best.title ?: best.name ?: title,
                    mediaType = best.mediaType ?: mediaType,
                    imageUrl = TmdbUrlHelper.posterUrl(best.posterPath),
                    score = best.voteAverage,
                    date = best.releaseDate ?: best.firstAirDate ?: candidate.date
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("DataPortabilityManager", "TMDB search error: ${e.message}")
        }

        // Fallback with synthetic ID
        return candidate.copy(
            id = candidate.id ?: (100000 + (Math.random() * 800000).toInt())
        )
    }

    private fun splitCSVLines(text: String): List<String> {
        val lines = mutableListOf<String>()
        var cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '"') inQuotes = !inQuotes
            if ((c == '\n' || c == '\r') && !inQuotes) {
                val str = cur.toString().trim()
                if (str.isNotEmpty()) lines.add(str)
                cur = StringBuilder()
                if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
            } else {
                cur.append(c)
            }
            i++
        }
        val last = cur.toString().trim()
        if (last.isNotEmpty()) lines.add(last)
        return lines
    }

    private fun parseCSVRow(row: String): List<String> {
        val values = mutableListOf<String>()
        var cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < row.length) {
            val c = row[i]
            if (c == '"') {
                if (inQuotes && i + 1 < row.length && row[i + 1] == '"') {
                    cur.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                values.add(cur.toString())
                cur = StringBuilder()
            } else {
                cur.append(c)
            }
            i++
        }
        values.add(cur.toString())
        return values
    }

    private fun escapeCSV(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
