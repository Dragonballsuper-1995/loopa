package com.loopa

import com.loopa.data.DataPortabilityManager
import com.loopa.db.MediaItemEntity
import com.loopa.model.TmdbMovie
import com.loopa.search.SearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchAndPortabilityUnitTest {

    @Before
    fun setup() {
        SearchEngine.clearIndex()
    }

    @Test
    fun testSearchEnginePrefixAndFuzzy() {
        val inception = TmdbMovie(
            id = 27205,
            title = "Inception",
            name = "Inception",
            overview = "Mind heist",
            posterPath = "/inception.jpg",
            backdropPath = null,
            voteAverage = 8.8,
            releaseDate = "2010-07-16",
            firstAirDate = null,
            mediaType = "movie",
            popularity = 50.0,
            genreIds = listOf(28, 878)
        )

        val interstellar = TmdbMovie(
            id = 157336,
            title = "Interstellar",
            name = "Interstellar",
            overview = "Space exploration",
            posterPath = "/interstellar.jpg",
            backdropPath = null,
            voteAverage = 8.6,
            releaseDate = "2014-11-07",
            firstAirDate = null,
            mediaType = "movie",
            popularity = 60.0,
            genreIds = listOf(18, 878)
        )

        SearchEngine.indexMediaItems(listOf(inception, interstellar))

        // Prefix match
        val prefixMatches = SearchEngine.getSuggestions("Incep", 5)
        assertEquals(1, prefixMatches.size)
        assertEquals("Inception", prefixMatches[0].title)

        // Fuzzy match
        val fuzzyMatches = SearchEngine.getSuggestions("Interstelar", 5)
        assertEquals(1, fuzzyMatches.size)
        assertEquals("Interstellar", fuzzyMatches[0].title)
    }

    @Test
    fun testDataPortabilityExportAndParse() {
        val items = listOf(
            MediaItemEntity(
                id = 101,
                title = "Blade Runner 2049",
                imageUrl = "/bladerunner.jpg",
                date = "2017",
                score = 8.5,
                listName = "Watched",
                mediaType = "movie",
                currentSeason = 1,
                currentEpisode = 0,
                totalEpisodes = 0,
                totalSeasons = 0,
                progressString = null,
                userRating = 10,
                personalNotes = "Masterpiece",
                runtime = 164,
                genres = "Sci-Fi, Drama",
                directorStudio = "Denis Villeneuve",
                updatedAt = "2026-08-15T00:00:00Z"
            )
        )

        // 1. JSON Round-trip
        val jsonString = DataPortabilityManager.exportToJSON(items)
        assertTrue(jsonString.contains("Blade Runner 2049"))
        assertTrue(jsonString.contains("Denis Villeneuve"))

        val parsedCandidates = DataPortabilityManager.parseContent(jsonString)
        assertEquals(1, parsedCandidates.size)
        assertEquals("Blade Runner 2049", parsedCandidates[0].title)
        assertEquals(10, parsedCandidates[0].userRating)

        // 2. CSV Round-trip
        val csvString = DataPortabilityManager.exportToCSV(items)
        assertTrue(csvString.contains("Blade Runner 2049"))
        assertTrue(csvString.contains("Denis Villeneuve"))

        val parsedCsvCandidates = DataPortabilityManager.parseContent(csvString, "loopa_export.csv")
        assertEquals(1, parsedCsvCandidates.size)
        assertEquals("Blade Runner 2049", parsedCsvCandidates[0].title)
    }

    @Test
    fun testLetterboxdAndImdbParsing() {
        val letterboxdCsv = """
Date,Name,Year,Letterboxd URI,Rating
2026-01-01,Dune: Part Two,2024,https://boxd.it/test,4.5
        """.trimIndent()

        val lbCandidates = DataPortabilityManager.parseContent(letterboxdCsv, "watched.csv")
        assertEquals(1, lbCandidates.size)
        assertEquals("Dune: Part Two", lbCandidates[0].title)
        assertEquals("2024", lbCandidates[0].date)
        assertEquals(9, lbCandidates[0].userRating) // 4.5 * 2 = 9

        val imdbCsv = """
Position,Const,Created,Modified,Description,Title,URL,Title Type,IMDb Rating,Runtime (mins),Year,Genres,Num Votes,Release Date,Directors,Your Rating,Date Rated
1,tt0816692,2026-01-01,2026-01-01,,Interstellar,https://imdb.com/title/tt0816692/,movie,8.7,169,2014,"Adventure, Drama, Sci-Fi",2000000,2014-11-05,Christopher Nolan,10,2026-01-01
        """.trimIndent()

        val imdbCandidates = DataPortabilityManager.parseContent(imdbCsv, "ratings.csv")
        assertEquals(1, imdbCandidates.size)
        assertEquals("Interstellar", imdbCandidates[0].title)
        assertEquals("movie", imdbCandidates[0].mediaType)
        assertEquals(10, imdbCandidates[0].userRating)
    }

    @Test
    fun testMalXmlParsing() {
        val malXml = """
<?xml version="1.0" encoding="UTF-8" ?>
<myanimelist>
    <anime>
        <series_animedb_id>5114</series_animedb_id>
        <series_title><![CDATA[Fullmetal Alchemist: Brotherhood]]></series_title>
        <my_watched_episodes>64</my_watched_episodes>
        <my_score>10</my_score>
        <my_status>2</my_status>
    </anime>
</myanimelist>
        """.trimIndent()

        val malCandidates = DataPortabilityManager.parseContent(malXml)
        assertEquals(1, malCandidates.size)
        assertEquals("Fullmetal Alchemist: Brotherhood", malCandidates[0].title)
        assertEquals(5114, malCandidates[0].id)
        assertEquals(64, malCandidates[0].currentEpisode)
        assertEquals(10, malCandidates[0].userRating)
        assertEquals("anime", malCandidates[0].mediaType)
    }
}
