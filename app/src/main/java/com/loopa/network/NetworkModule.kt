package com.loopa.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

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

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val tmdbApi: TmdbApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://loopa-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TmdbApiService::class.java)
    }

    val jikanApi: JikanApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.jikan.moe/v4/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(JikanApiService::class.java)
    }

    val geminiApi: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient.newBuilder().readTimeout(60, java.util.concurrent.TimeUnit.SECONDS).build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}
