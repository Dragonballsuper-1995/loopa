package com.loopa.network

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object NetworkModule {
    val supabase by lazy {
        val url = com.loopa.app.BuildConfig.SUPABASE_URL
        val key = com.loopa.app.BuildConfig.SUPABASE_KEY
        createSupabaseClient(
            supabaseUrl = if (url == "MY_SUPABASE_URL" || url.isEmpty()) "https://placeholder.supabase.co" else url,
            supabaseKey = if (key == "MY_SUPABASE_KEY" || key.isEmpty()) "placeholder" else key
        ) {
            install(Auth) {
                scheme = "app"
                host = "supabase.com"
            }
            install(Postgrest)
            install(Realtime)
        }
    }
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun createOkHttpClient(cache: Cache? = null): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("X-Loopa-Client-Key", com.loopa.app.BuildConfig.LOOPA_CLIENT_KEY)
                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (com.loopa.app.BuildConfig.DEBUG)
                    HttpLoggingInterceptor.Level.BODY
                else
                    HttpLoggingInterceptor.Level.NONE
            })

        if (cache != null) {
            builder.cache(cache)
        }
        return builder.build()
    }

    @Volatile
    var okHttpClient: OkHttpClient = createOkHttpClient()
        private set

    fun initialize(context: Context) {
        try {
            val cacheDir = File(context.cacheDir, "loopa_http_cache")
            val cache = Cache(cacheDir, 30L * 1024 * 1024) // 30 MB
            okHttpClient = createOkHttpClient(cache)
        } catch (_: Exception) {}
        prewarmConnections()
    }

    private val callFactory = okhttp3.Call.Factory { request ->
        okHttpClient.newCall(request)
    }

    val tmdbApi: TmdbApiService by lazy {
        val proxyBase = com.loopa.app.BuildConfig.AI_PROXY_URL.trimEnd('/') + "/tmdb/"
        Retrofit.Builder()
            .baseUrl(proxyBase)
            .callFactory(callFactory)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TmdbApiService::class.java)
    }

    val kitsuApi: KitsuApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://kitsu.io/api/edge/")
            .callFactory(callFactory)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(KitsuApiService::class.java)
    }

    val anilistApi: AniListApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://graphql.anilist.co/")
            .callFactory(callFactory)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AniListApiService::class.java)
    }

    val jikanApi: JikanApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.jikan.moe/v4/")
            .callFactory(callFactory)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(JikanApiService::class.java)
    }

    val loopaApi: LoopaApiService by lazy {
        val proxyBase = com.loopa.app.BuildConfig.AI_PROXY_URL.trimEnd('/') + "/"
        Retrofit.Builder()
            .baseUrl(proxyBase)
            .callFactory(callFactory)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LoopaApiService::class.java)
    }

    fun prewarmConnections() {
        try {
            val proxyBase = com.loopa.app.BuildConfig.AI_PROXY_URL
            if (proxyBase.isNotEmpty() && proxyBase != "YOUR_CLOUDFLARE_WORKER_URL") {
                val req = okhttp3.Request.Builder()
                    .url(proxyBase)
                    .head()
                    .build()
                okHttpClient.newCall(req).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) { response.close() }
                })
            }
        } catch (_: Exception) {}
    }
}
