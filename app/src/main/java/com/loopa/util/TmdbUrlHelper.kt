package com.loopa.util

/**
 * Centralized URL helper for TMDB poster, backdrop, and profile image proxy URLs.
 * Eliminates hardcoded image URLs across the Android codebase.
 */
object TmdbUrlHelper {
    const val PROXY_BASE_URL = "https://loopa-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/"
    const val TMDB_BASE_URL = "https://image.tmdb.org/t/p/"

    private val PREFIXES = listOf(
        "https://loopa-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/original",
        "https://loopa-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w1280",
        "https://loopa-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w780",
        "https://loopa-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w500",
        "https://loopa-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w342",
        "https://loopa-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w185",
        "https://loopa-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w154",
        "https://loopa-tmdb-proxy.sujalsanjay-chhajed2023.workers.dev/t/p/w92",
        "https://image.tmdb.org/t/p/original",
        "https://image.tmdb.org/t/p/w1280",
        "https://image.tmdb.org/t/p/w780",
        "https://image.tmdb.org/t/p/w500",
        "https://image.tmdb.org/t/p/w342",
        "https://image.tmdb.org/t/p/w185",
        "https://image.tmdb.org/t/p/w154",
        "https://image.tmdb.org/t/p/w92"
    )

    /**
     * Extracts the relative image path if the given rawPath already starts with a TMDB or proxy base URL.
     */
    fun cleanPath(rawPath: String?): String? {
        if (rawPath.isNullOrBlank()) return null
        val path = rawPath.trim()
        for (prefix in PREFIXES) {
            if (path.startsWith(prefix)) {
                return path.removePrefix(prefix)
            }
        }
        return path
    }

    /**
     * Builds a proxied TMDB poster URL.
     * Supported sizes: w92, w154, w185, w342, w500, w780, original. Default is "w342".
     */
    fun posterUrl(path: String?, size: String = "w342"): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) {
            val cleaned = cleanPath(path)
            if (cleaned != null && cleaned.startsWith("/")) {
                return "$PROXY_BASE_URL$size$cleaned"
            }
            return path
        }
        val formattedPath = if (path.startsWith("/")) path else "/$path"
        return "$PROXY_BASE_URL$size$formattedPath"
    }

    /**
     * Builds a proxied TMDB backdrop URL.
     * Supported sizes: w300, w500, w780, w1280, original. Default is "w500".
     */
    fun backdropUrl(path: String?, size: String = "w500"): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) {
            val cleaned = cleanPath(path)
            if (cleaned != null && cleaned.startsWith("/")) {
                return "$PROXY_BASE_URL$size$cleaned"
            }
            return path
        }
        val formattedPath = if (path.startsWith("/")) path else "/$path"
        return "$PROXY_BASE_URL$size$formattedPath"
    }

    /**
     * Builds a proxied TMDB profile or small image URL.
     * Supported sizes: w45, w185, h632, original. Default is "w185".
     */
    fun profileUrl(path: String?, size: String = "w185"): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) {
            val cleaned = cleanPath(path)
            if (cleaned != null && cleaned.startsWith("/")) {
                return "$PROXY_BASE_URL$size$cleaned"
            }
            return path
        }
        val formattedPath = if (path.startsWith("/")) path else "/$path"
        return "$PROXY_BASE_URL$size$formattedPath"
    }

    /**
     * Returns the best available image URL given optional backdrop and poster paths.
     */
    fun getImageUrl(backdropPath: String?, posterPath: String?, preferBackdrop: Boolean = false): String? {
        return if (preferBackdrop) {
            backdropPath?.let { backdropUrl(it) } ?: posterPath?.let { posterUrl(it) }
        } else {
            posterPath?.let { posterUrl(it) } ?: backdropPath?.let { backdropUrl(it) }
        }
    }
}
