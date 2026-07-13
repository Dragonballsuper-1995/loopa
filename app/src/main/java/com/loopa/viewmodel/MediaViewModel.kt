package com.loopa.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.loopa.app.BuildConfig
import com.loopa.db.DatabaseProvider
import com.loopa.db.MediaItemEntity
import com.loopa.model.TmdbMovie
import com.loopa.repository.MediaRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.loopa.network.NetworkModule
import com.loopa.network.GenerateContentRequest
import com.loopa.network.Content
import com.loopa.network.Part
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

sealed class MediaUiState {
    object Loading : MediaUiState()
    data class Success(val trending: List<TmdbMovie>) : MediaUiState()
    data class Error(val message: String) : MediaUiState()
    object InsufficientData : MediaUiState()
}

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

    // ── In-app Toast Event Channel ────────────────────────────────────────────
    private val _toastEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    fun showToast(message: String) {
        _toastEvent.tryEmit(message)
    }

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "System") ?: "System")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()
    
    private val _lastSyncTime = MutableStateFlow(prefs.getLong("last_sync_time", 0L))
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()
    
    private val _aiRecommendations = MutableStateFlow<List<com.loopa.model.AiRecommendationResult>>(emptyList())
    val aiRecommendations: StateFlow<List<com.loopa.model.AiRecommendationResult>> = _aiRecommendations.asStateFlow()
    
    private val _isLoadingAiRecs = MutableStateFlow(false)
    val isLoadingAiRecs: StateFlow<Boolean> = _isLoadingAiRecs.asStateFlow()
    
    private val _aiRecsError = MutableStateFlow<String?>(null)
    val aiRecsError: StateFlow<String?> = _aiRecsError.asStateFlow()
    
    private val _chatHistory = MutableStateFlow<List<com.loopa.model.ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<com.loopa.model.ChatMessage>> = _chatHistory.asStateFlow()

    fun fetchAiRecommendations() {
        if (_chatHistory.value.isEmpty()) {
            _chatHistory.value = listOf(
                com.loopa.model.ChatMessage(
                    role = "model",
                    text = "Hello! I have access to your watchlist. What kind of movie, show, or anime are you in the mood for today?",
                    recommendations = emptyList()
                )
            )
        }
    }

    fun clearAiChat() {
        _chatHistory.value = listOf(
            com.loopa.model.ChatMessage(
                role = "model",
                text = "Hello! I have access to your watchlist. What kind of movie, show, or anime are you in the mood for today?"
            )
        )
    }

    fun sendAiChatMessage(userMessage: String) {
        if (userMessage.isBlank() || _isLoadingAiRecs.value) return
        
        viewModelScope.launch {
            val proxyUrl = BuildConfig.AI_PROXY_URL
            if (proxyUrl.isEmpty() || proxyUrl == "YOUR_CLOUDFLARE_WORKER_URL") {
                _aiRecsError.value = "AI Proxy URL is missing. Add it in .env"
                return@launch
            }
            
            // Add user message immediately
            _chatHistory.value = _chatHistory.value + com.loopa.model.ChatMessage(role = "user", text = userMessage)
            
            _aiRecsError.value = null
            _isLoadingAiRecs.value = true
            try {
                val history = savedMediaItems.value
                val likedTitles = prefs.getStringSet("liked_titles", emptySet()) ?: emptySet()
                val dislikedTitles = prefs.getStringSet("disliked_titles", emptySet()) ?: emptySet()
                
                val recs = repository.getDiscoverRecommendations(
                    proxyUrl, history, likedTitles, dislikedTitles, _chatHistory.value
                )
                
                val modelResponseText = if (recs.isNotEmpty()) "Here are some recommendations based on what you asked:" else "I couldn't find any good matches for that. Could you try asking in a different way?"
                
                _chatHistory.value = _chatHistory.value + com.loopa.model.ChatMessage(
                    role = "model",
                    text = modelResponseText,
                    recommendations = recs
                )
            } catch (e: Exception) {
                _chatHistory.value = _chatHistory.value + com.loopa.model.ChatMessage(
                    role = "model",
                    text = "Sorry, I encountered an error: ${e.message}"
                )
            } finally {
                _isLoadingAiRecs.value = false
            }
        }
    }

    fun dislikeRecommendation(title: String) {
        val currentDisliked = prefs.getStringSet("disliked_titles", emptySet()) ?: emptySet()
        val newDisliked = currentDisliked + title
        prefs.edit().putStringSet("disliked_titles", newDisliked).apply()
        
        // Remove from current recommendations list immediately so it vanishes from UI
        _chatHistory.value = _chatHistory.value.map { msg ->
            msg.copy(recommendations = msg.recommendations.filter { it.title != title })
        }
        showToast("Removed from recommendations")
    }

    fun likeRecommendation(title: String) {
        val currentLiked = prefs.getStringSet("liked_titles", emptySet()) ?: emptySet()
        val newLiked = currentLiked + title
        prefs.edit().putStringSet("liked_titles", newLiked).apply()
        showToast("Logged to recommendation preferences")
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    private val mediaItemDao = DatabaseProvider.getDatabase(application).mediaItemDao()
    private val repository = MediaRepository(mediaItemDao)

    val savedMediaItems: StateFlow<List<MediaItemEntity>> = repository.allMediaItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
    val isRateLimited: StateFlow<Boolean> = repository.isRateLimited

    private val _uiState = MutableStateFlow<MediaUiState>(MediaUiState.Loading)
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    private val _popularMovies = MutableStateFlow<List<TmdbMovie>>(emptyList())
    val popularMovies: StateFlow<List<TmdbMovie>> = _popularMovies.asStateFlow()

    private val _popularTv = MutableStateFlow<List<TmdbMovie>>(emptyList())
    val popularTv: StateFlow<List<TmdbMovie>> = _popularTv.asStateFlow()

    private val _topAnime = MutableStateFlow<List<com.loopa.model.JikanAnime>>(emptyList())
    val topAnime: StateFlow<List<com.loopa.model.JikanAnime>> = _topAnime.asStateFlow()

    private val _searchState = MutableStateFlow<MediaUiState>(MediaUiState.Success(emptyList()))
    val searchState: StateFlow<MediaUiState> = _searchState.asStateFlow()

    private val _recommendationState = MutableStateFlow<MediaUiState>(MediaUiState.Loading)
    val recommendationState: StateFlow<MediaUiState> = _recommendationState.asStateFlow()

    init {
        fetchHomeData()
        fetchAiRecommendations()
        viewModelScope.launch {
            repository.observeRealtimeChanges()
        }
    }

    fun addMediaItem(id: Int, title: String, imageUrl: String?, date: String?, score: Double?, listName: String, mediaType: String) {
        viewModelScope.launch {
            repository.insertMediaItem(
                MediaItemEntity(
                    id = id,
                    title = title,
                    imageUrl = imageUrl,
                    date = date,
                    score = score,
                    listName = listName,
                    mediaType = mediaType,
                    currentEpisode = 0,
                    currentSeason = 1,
                    totalEpisodes = 0,
                    totalSeasons = 0
                )
            )
        }
    }

    fun updateMediaItem(item: MediaItemEntity) {
        viewModelScope.launch {
            repository.insertMediaItem(item)
        }
    }

    fun removeMediaItem(id: Int, mediaType: String) {
        viewModelScope.launch {
            repository.deleteMediaItem(id, mediaType)
        }
    }

    suspend fun syncData() {
        repository.syncWithRemote()
        val currentTime = System.currentTimeMillis()
        _lastSyncTime.value = currentTime
        prefs.edit().putLong("last_sync_time", currentTime).apply()
    }

    fun getFilteredLocalItems(items: List<MediaItemEntity>, tabIndex: Int, query: String): List<MediaItemEntity> {
        val typeFiltered = when (tabIndex) {
            1 -> items.filter { it.mediaType == "movie" }
            2 -> items.filter { it.mediaType == "tv" }
            3 -> items.filter { it.mediaType == "anime" }
            else -> items
        }
        if (query.isBlank()) return typeFiltered
        
        return typeFiltered.filter { fuzzyMatch(query, it.title) }
    }
    
    private fun fuzzyMatch(query: String, text: String): Boolean {
        if (query.isBlank()) return true
        val q = query.lowercase().trim()
        val t = text.lowercase().trim()
        if (t.contains(q)) return true
        
        // Levenshtein distance
        val m = q.length
        val n = t.length
        if (Math.abs(m - n) > 3) return false
        
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (q[i - 1] == t[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        val maxErrors = minOf(2, q.length / 2)
        
        // Check if query is close to ANY substring of text word by word
        val words = t.split(" ")
        for (word in words) {
            val wm = q.length
            val wn = word.length
            if (Math.abs(wm - wn) > 3) continue
            val wdp = Array(wm + 1) { IntArray(wn + 1) }
            for (i in 0..wm) wdp[i][0] = i
            for (j in 0..wn) wdp[0][j] = j
            for (i in 1..wm) {
                for (j in 1..wn) {
                    val cost = if (q[i - 1] == word[j - 1]) 0 else 1
                    wdp[i][j] = minOf(wdp[i - 1][j] + 1, wdp[i][j - 1] + 1, wdp[i - 1][j - 1] + cost)
                }
            }
            if (wdp[wm][wn] <= maxErrors) return true
        }
        
        return dp[m][n] <= maxErrors
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isNotBlank()) kotlinx.coroutines.delay(500)
            
            if (query.isBlank()) {
                _searchState.value = MediaUiState.Loading
                val apiKey = BuildConfig.TMDB_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_TMDB_API_KEY") {
                    _searchState.value = MediaUiState.Error("Missing TMDB API Key. Add it in AI Studio Secrets.")
                    return@launch
                }
                repository.getTrendingMovies(apiKey)
                    .catch { e -> _searchState.value = MediaUiState.Error(e.localizedMessage ?: "Unknown error") }
                    .collect { movies -> _searchState.value = MediaUiState.Success(movies) }
                return@launch
            }
            
            _searchState.value = MediaUiState.Loading
            
            val apiKey = BuildConfig.TMDB_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_TMDB_API_KEY") {
                _searchState.value = MediaUiState.Error("Missing TMDB API Key. Add it in AI Studio Secrets.")
                return@launch
            }
            
            var actualQuery = query
            val proxyUrl = BuildConfig.AI_PROXY_URL
            if (proxyUrl.isNotEmpty() && proxyUrl != "YOUR_CLOUDFLARE_WORKER_URL" && query.length >= 3) {
                try {
                    val prompt = "Correct any typos in this movie/TV show search query: '$query'. Return ONLY the corrected title, nothing else. If it's correct, return it as is."
                    val jsonBody = org.json.JSONObject().apply {
                        put("prompt", prompt)
                    }
                    val reqBody = okhttp3.RequestBody.create("application/json".toMediaType(), jsonBody.toString())
                    val req = okhttp3.Request.Builder()
                        .url(proxyUrl)
                        .post(reqBody)
                        .build()
                    val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.loopa.network.NetworkModule.okHttpClient.newCall(req).execute() }
                    val corrected = response.body?.string()?.trim()
                    if (!corrected.isNullOrEmpty() && corrected != "[]") {
                        // AI proxy returns JSON array if it falls back to recommendation mode? No, we sent a plain string.
                        // Actually, our proxy just returns text. If it's a JSON array, it's weird.
                        actualQuery = corrected
                    }
                } catch (e: Exception) {
                    // ignore and fallback to original
                }
            }

            repository.searchMedia(apiKey, actualQuery)
                .catch { e ->
                    _searchState.value = MediaUiState.Error(e.localizedMessage ?: "Unknown error occurred")
                }
                .collect { movies ->
                    _searchState.value = MediaUiState.Success(movies)
                }
        }
    }

    fun fetchHomeData() {
        viewModelScope.launch {
            val apiKey = BuildConfig.TMDB_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_TMDB_API_KEY") {
                _uiState.value = MediaUiState.Error("Missing TMDB API Key. Add it in AI Studio Secrets.")
                return@launch
            }

            _uiState.value = MediaUiState.Loading
            
            // Trending
            launch {
                repository.getTrendingMovies(apiKey)
                    .catch { e -> _uiState.value = MediaUiState.Error(e.localizedMessage ?: "Unknown error occurred") }
                    .collect { movies -> _uiState.value = MediaUiState.Success(movies) }
            }

            // Popular Movies
            launch {
                repository.getPopularMovies(apiKey)
                    .catch { /* ignore */ }
                    .collect { movies -> _popularMovies.value = movies }
            }

            // Popular TV
            launch {
                repository.getPopularTv(apiKey)
                    .catch { /* ignore */ }
                    .collect { tv -> _popularTv.value = tv }
            }

            // Top Anime
            launch {
                repository.getTopAnime()
                    .catch { /* ignore */ }
                    .collect { anime -> _topAnime.value = anime }
            }
        }
    }

    fun fetchRecommendations(savedItems: List<MediaItemEntity>) {
        if (savedItems.size < 10) {
            _recommendationState.value = MediaUiState.InsufficientData
            return
        }

        val proxyUrl = BuildConfig.AI_PROXY_URL
        val tmdbApiKey = BuildConfig.TMDB_API_KEY
        
        // Return immediately if empty to avoid continuous loading state
        if (proxyUrl.isEmpty() || proxyUrl == "YOUR_CLOUDFLARE_WORKER_URL") {
            _aiRecsError.value = "AI Proxy URL is missing. Add it in .env"
            return
        }

        viewModelScope.launch {
            _recommendationState.value = MediaUiState.Loading
            try {
                val titles = savedItems.joinToString(", ") { it.title }
                val results = repository.getSimilarTitles(proxyUrl, tmdbApiKey, titles)
                _recommendationState.value = MediaUiState.Success(results)
            } catch (e: Exception) {
                _recommendationState.value = MediaUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }
}
