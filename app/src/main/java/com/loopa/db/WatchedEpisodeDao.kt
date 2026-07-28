package com.loopa.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedEpisodeDao {
    @Query("SELECT * FROM watched_episodes WHERE mediaId = :mediaId AND mediaType = :mediaType")
    fun getWatchedEpisodesForMedia(mediaId: Int, mediaType: String): Flow<List<WatchedEpisodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchedEpisode(episode: WatchedEpisodeEntity)

    @Query("DELETE FROM watched_episodes WHERE mediaId = :mediaId AND mediaType = :mediaType AND seasonNumber = :seasonNumber AND episodeNumber = :episodeNumber")
    suspend fun deleteWatchedEpisode(mediaId: Int, mediaType: String, seasonNumber: Int, episodeNumber: Int)

    @Query("DELETE FROM watched_episodes WHERE mediaId = :mediaId AND mediaType = :mediaType")
    suspend fun deleteAllForMedia(mediaId: Int, mediaType: String)

    @Query("SELECT * FROM watched_episodes")
    suspend fun getAllWatchedEpisodesSync(): List<WatchedEpisodeEntity>

    @Query("DELETE FROM watched_episodes")
    suspend fun deleteAllWatchedEpisodes()
}
