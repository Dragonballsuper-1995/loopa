package com.example.repository

import com.example.db.MediaItemDao
import com.example.db.MediaItemEntity
import com.example.model.JikanAnime
import com.example.model.TmdbMovie
import com.example.network.NetworkModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import retrofit2.HttpException
import okhttp3.MediaType.Companion.toMediaType

object ApiCache {
    private val cache = mutableMapOf<String, CacheEntry>()
    private const val EXPIRATION_TIME = 5 * 60 * 1000L // 5 minutes

    data class CacheEntry(val data: Any, val timestamp: Long)

    fun <T> get(key: String): T? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() - entry.timestamp > EXPIRATION_TIME) {
            cache.remove(key)
            return null
        }
        @Suppress("UNCHECKED_CAST")
        return entry.data as T
    }

    fun put(key: String, data: Any) {
        cache[key] = CacheEntry(data, System.currentTimeMillis())
    }
}

class MediaRepository(private val mediaItemDao: MediaItemDao) {
    private val tmdbApi = NetworkModule.tmdbApi
    private val jikanApi = NetworkModule.jikanApi
    
    private val _isRateLimited = MutableStateFlow(false)
    val isRateLimited = _isRateLimited.asStateFlow()

    suspend fun <T> retryWithBackoff(
        times: Int = 3,
        initialDelay: Long = 1000,
        maxDelay: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        var currentAttempt = 0
        while (currentAttempt < times) {
            try {
                val result = block()
                if (currentAttempt > 0) _isRateLimited.value = false
                return result
            } catch (e: HttpException) {
                if (e.code() == 429) {
                    currentAttempt++
                    if (currentAttempt >= times) {
                        _isRateLimited.value = false
                        throw e
                    }
                    _isRateLimited.value = true
                    kotlinx.coroutines.delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
                } else {
                    throw e
                }
            } catch (e: Exception) {
                throw e
            }
        }
        throw Exception("Max retries reached")
    }

    suspend fun getSimilarTitles(proxyUrl: String, tmdbApiKey: String, titles: String): List<TmdbMovie> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val prompt = "Based on the user's watched and saved movies/TV shows: [$titles], suggest 5 highly relevant movie or TV show titles they should watch next. Return ONLY a JSON array of strings containing the titles. Do not include markdown."
        val jsonBody = org.json.JSONObject().apply {
            put("prompt", prompt)
        }
        val reqBody = okhttp3.RequestBody.create("application/json".toMediaType(), jsonBody.toString())
        val req = okhttp3.Request.Builder()
            .url(proxyUrl)
            .post(reqBody)
            .build()
            
        val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { NetworkModule.okHttpClient.newCall(req).execute() }
        if (!response.isSuccessful) throw Exception("AI Proxy failed: ${response.code}")
        
        val textResponse = response.body?.string() ?: "[]"
        
        val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
        val listAdapter = moshi.adapter<List<String>>(
            com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
        )
        
        val parsedTitles = try {
            val cleanText = textResponse.trim().removePrefix("```json").removeSuffix("```").trim()
            listAdapter.fromJson(cleanText) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        
        if (parsedTitles.isEmpty()) {
            throw Exception("Failed to parse recommendations")
        }
        
        parsedTitles.mapIndexed { index, title ->
            async {
                kotlinx.coroutines.delay(index * 500L)
                try {
                    val cacheKey = "search_tmdb_$title"
                    val cachedPoster = ApiCache.get<String>(cacheKey)
                    
                    if (cachedPoster != null) {
                        // For simplicity in this endpoint we're not recreating the full object from cache,
                        // so we'll just fetch again or assume cache missed if we need the full object.
                        // Actually, we can just cache the full TmdbMovie object if we want.
                    }
                    
                    retryWithBackoff { tmdbApi.searchMulti(tmdbApiKey, title) }.results.firstOrNull { it.posterPath != null }
                } catch (e: Exception) {
                    null
                }
            }
        }.awaitAll().filterNotNull()
    }

    val allMediaItems: Flow<List<MediaItemEntity>> = mediaItemDao.getAllMediaItems()

    suspend fun insertMediaItem(item: MediaItemEntity) {
        mediaItemDao.insertMediaItem(item)
    }

    suspend fun deleteMediaItem(id: Int, mediaType: String) {
        mediaItemDao.deleteMediaItem(id, mediaType)
    }

    suspend fun syncWithRemote() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val user = NetworkModule.supabase.auth.currentUserOrNull() ?: throw Exception("User not logged in")
        val localItems = mediaItemDao.getAllMediaItemsSync()
        
        // Fetch remote items
        val remoteItems = retryWithBackoff {
            NetworkModule.supabase.postgrest["media_items"]
                .select()
                .decodeList<com.example.model.RemoteMediaItem>()
        }
        
        // For a simple sync: we merge them by inserting missing local items to remote,
        // and inserting missing remote items to local. 
        // In a real app we'd use timestamps, but this is a simple naive sync.
        
        val localMap = localItems.associateBy { "${it.id}_${it.mediaType}" }
        val remoteMap = remoteItems.associateBy { "${it.id}_${it.mediaType}" }
        
        val toUpload = localItems.filter { "${it.id}_${it.mediaType}" !in remoteMap }.map {
            com.example.model.RemoteMediaItem(
                id = it.id,
                userId = user.id,
                title = it.title,
                imageUrl = it.imageUrl,
                date = it.date,
                score = it.score,
                listName = it.listName,
                mediaType = it.mediaType,
                currentSeason = it.currentSeason,
                currentEpisode = it.currentEpisode,
                totalEpisodes = it.totalEpisodes,
                totalSeasons = it.totalSeasons,
                progressString = it.progressString,
                userRating = it.userRating,
                personalNotes = it.personalNotes
            )
        }
        
        val toDownload = remoteItems.filter { "${it.id}_${it.mediaType}" !in localMap }.map {
            com.example.db.MediaItemEntity(
                id = it.id,
                title = it.title,
                imageUrl = it.imageUrl,
                date = it.date,
                score = it.score,
                listName = it.listName,
                mediaType = it.mediaType,
                currentSeason = it.currentSeason,
                currentEpisode = it.currentEpisode,
                totalEpisodes = it.totalEpisodes,
                totalSeasons = it.totalSeasons,
                progressString = it.progressString,
                userRating = it.userRating,
                personalNotes = it.personalNotes
            )
        }
        
        // Upload to Supabase
        if (toUpload.isNotEmpty()) {
            retryWithBackoff {
                NetworkModule.supabase.postgrest["media_items"].upsert(toUpload)
            }
        }
        
        // Save to Room DB
        if (toDownload.isNotEmpty()) {
            toDownload.forEach { mediaItemDao.insertMediaItem(it) }
        }
    }

    suspend fun observeRealtimeChanges() {
        try {
            val user = NetworkModule.supabase.auth.currentUserOrNull() ?: return
            val channel = NetworkModule.supabase.channel("watchlist_changes")
            
            // Subscribing to any change on the table
            val changeFlow = channel.postgresChangeFlow<io.github.jan.supabase.realtime.PostgresAction>(schema = "public") {
                table = "media_items"
                filter("user_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, user.id)
            }
            
            val realtime = NetworkModule.supabase.pluginManager.getPlugin(io.github.jan.supabase.realtime.Realtime)
            realtime.connect()
            channel.subscribe()
            
            changeFlow.collect {
                // Whenever a change happens remotely, sync with remote to update local DB
                syncWithRemote()
            }
        } catch (e: Exception) {
            // Ignore if realtime fails to connect or subscribe
        }
    }

    fun getTrendingMovies(apiKey: String): Flow<List<TmdbMovie>> = flow {
        val cacheKey = "trending_movies"
        val cached = ApiCache.get<List<TmdbMovie>>(cacheKey)
        if (cached != null) {
            emit(cached)
            return@flow
        }
        val response = retryWithBackoff { tmdbApi.getTrendingAll(apiKey) }
        ApiCache.put(cacheKey, response.results)
        emit(response.results)
    }

    fun getPopularMovies(apiKey: String): Flow<List<TmdbMovie>> = flow {
        val cacheKey = "popular_movies"
        val cached = ApiCache.get<List<TmdbMovie>>(cacheKey)
        if (cached != null) {
            emit(cached)
            return@flow
        }
        val response = retryWithBackoff { tmdbApi.getPopularMovies(apiKey) }
        ApiCache.put(cacheKey, response.results)
        emit(response.results)
    }

    fun getPopularTv(apiKey: String): Flow<List<TmdbMovie>> = flow {
        val cacheKey = "popular_tv"
        val cached = ApiCache.get<List<TmdbMovie>>(cacheKey)
        if (cached != null) {
            emit(cached)
            return@flow
        }
        val response = retryWithBackoff { tmdbApi.getPopularTv(apiKey) }
        ApiCache.put(cacheKey, response.results)
        emit(response.results)
    }

    fun searchMedia(apiKey: String, query: String): Flow<List<TmdbMovie>> = flow {
        val cacheKey = "search_$query"
        val cached = ApiCache.get<List<TmdbMovie>>(cacheKey)
        if (cached != null) {
            emit(cached)
            return@flow
        }
        val response = retryWithBackoff { tmdbApi.searchMulti(apiKey, query) }
        ApiCache.put(cacheKey, response.results)
        emit(response.results)
    }

    fun getTopAnime(): Flow<List<JikanAnime>> = flow {
        val cacheKey = "top_anime"
        val cached = ApiCache.get<List<JikanAnime>>(cacheKey)
        if (cached != null) {
            emit(cached)
            return@flow
        }
        val response = retryWithBackoff { jikanApi.getTopAnime() }
        ApiCache.put(cacheKey, response.data)
        emit(response.data)
    }

    suspend fun getDiscoverRecommendations(
        apiKey: String,
        history: List<MediaItemEntity>,
        likedTitles: Set<String> = emptySet(),
        dislikedTitles: Set<String> = emptySet(),
        chatHistory: List<com.example.model.ChatMessage> = emptyList()
    ): List<com.example.model.AiRecommendationResult> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val isColdStart = history.size < 3
        
        val dislikedString = if (dislikedTitles.isNotEmpty()) {
            "\nThe user DISLIKED or was NOT interested in these targets (DO NOT recommend them):\n" + dislikedTitles.joinToString("\n") { "- $it" }
        } else ""

        val historyListString = if (isColdStart) "" else history.joinToString("\n") { item ->
            "- [${item.mediaType}] ${item.title} (Status: ${item.listName}, Rating: ${item.userRating}/10)"
        }
        val likedString = if (likedTitles.isNotEmpty()) {
            "\nThe user specifically LIKED these recommended targets:\n" + likedTitles.joinToString("\n") { "- $it" }
        } else ""

        val chatContext = if (chatHistory.isNotEmpty()) {
            "\nHere is the recent conversation history between you and the user:\n" +
            chatHistory.joinToString("\n") { "${if (it.role == "user") "User" else "Assistant"}: ${it.text}" } +
            "\n\nBased on the conversation above (especially the User's last message), provide exactly 4 recommendations."
        } else {
            "\nProvide exactly 4 recommendations."
        }

        val prompt = """
            You are a conversational AI Recommendation Engine. 
            ${if (isColdStart) "The user is new and hasn't tracked much content yet." else "The user has tracked the following media history:\n$historyListString"}
            $likedString
            $dislikedString
            $chatContext
            
            Provide custom, engaging reasoning (short summary) for each recommendation.
            Respond STRICTLY with a valid JSON array matching this schema:
            [{"title": "Title", "mediaType": "Movie/TV/Anime", "genre": "Genre", "releaseYear": "YYYY", "imageUrl": "Valid Poster Image URL", "reasoning": "Reasoning"}]
        """.trimIndent()
        
        var jsonText = "[]"
        try {
            android.util.Log.d("MediaRepository", "Requesting AI recommendations from Proxy...")
            val proxyUrl = com.example.app.BuildConfig.AI_PROXY_URL
            val jsonBody = org.json.JSONObject().apply {
                put("prompt", prompt)
            }
            val reqBody = okhttp3.RequestBody.create("application/json".toMediaType(), jsonBody.toString())
            val req = okhttp3.Request.Builder()
                .url(proxyUrl)
                .post(reqBody)
                .build()
                
            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { NetworkModule.okHttpClient.newCall(req).execute() }
            if (!response.isSuccessful) throw Exception("AI Proxy failed: ${response.code}")
            
            jsonText = response.body?.string() ?: "[]"
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "Failed to get AI recommendations: ${e.message}")
        }
            
            // Strip markdown block if present
            jsonText = jsonText.trim()
            if (jsonText.startsWith("```")) {
                jsonText = jsonText.substringBeforeLast("```").trim()
                if (jsonText.startsWith("```json", ignoreCase = true)) {
                    jsonText = jsonText.removePrefix("```json").trim()
                } else {
                    jsonText = jsonText.removePrefix("```").trim()
                }
            }
            
            if (jsonText.startsWith("{")) {
                try {
                    val obj = org.json.JSONObject(jsonText)
                    val keys = obj.keys()
                    if (keys.hasNext()) {
                        val firstKey = keys.next()
                        val arr = obj.optJSONArray(firstKey)
                        if (arr != null) {
                            jsonText = arr.toString()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MediaRepository", "Failed to parse JSON object wrapper: ${e.message}")
                }
            }
            
            val moshi = com.squareup.moshi.Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, com.example.model.AiRecommendationResult::class.java)
            val adapter = moshi.adapter<List<com.example.model.AiRecommendationResult>>(listType)
            
            val recs = try {
                adapter.fromJson(jsonText) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            kotlinx.coroutines.coroutineScope {
                recs.map { rec ->
                    async {
                        try {
                            val cacheKey = "search_tmdb_${rec.title}"
                            val cachedPoster = ApiCache.get<String>(cacheKey)
                            
                            val posterPath = if (cachedPoster != null) {
                                if (cachedPoster.isEmpty()) null else cachedPoster
                            } else {
                                val searchRes = retryWithBackoff { tmdbApi.searchMulti(com.example.app.BuildConfig.TMDB_API_KEY, rec.title) }
                                val path = searchRes.results.firstOrNull { it.posterPath != null }?.posterPath
                                ApiCache.put(cacheKey, path ?: "")
                                path
                            }

                            if (posterPath != null && posterPath.isNotEmpty()) {
                                rec.copy(imageUrl = "https://tsugi-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w500$posterPath")
                            } else {
                                rec
                            }
                        } catch (e: Exception) {
                            rec
                        }
                    }
                }.awaitAll()
            }
        }
    }
