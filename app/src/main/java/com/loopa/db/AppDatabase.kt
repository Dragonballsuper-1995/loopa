package com.loopa.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MediaItemEntity::class, WatchedEpisodeEntity::class, PendingOpEntity::class],
    version = 7,          // bumped from 6
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun watchedEpisodeDao(): WatchedEpisodeDao
    abstract fun pendingOpDao(): PendingOpDao

    companion object {
        /**
         * Migration 5 → 6
         * 1. Adds `updatedAt TEXT` column to media_items (nullable, default null).
         * 2. Rebuilds watched_episodes to change `watchedAt` from INTEGER (Unix ms)
         *    to TEXT (ISO 8601), converting existing timestamps via datetime().
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {

                // ── 1. Add updatedAt to media_items ───────────────────────────
                database.execSQL(
                    "ALTER TABLE media_items ADD COLUMN updatedAt TEXT DEFAULT NULL"
                )

                // ── 2. Rebuild watched_episodes (type change requires table rebuild)
                database.execSQL("""
                    CREATE TABLE watched_episodes_new (
                        mediaId       INTEGER NOT NULL,
                        mediaType     TEXT    NOT NULL,
                        seasonNumber  INTEGER NOT NULL,
                        episodeNumber INTEGER NOT NULL,
                        watchedAt     TEXT    NOT NULL DEFAULT '',
                        PRIMARY KEY (mediaId, mediaType, seasonNumber, episodeNumber),
                        FOREIGN KEY (mediaId, mediaType)
                            REFERENCES media_items(id, mediaType)
                            ON DELETE CASCADE
                    )
                """.trimIndent())

                // Convert existing Unix-ms timestamps to ISO 8601 strings.
                // datetime(watchedAt/1000,'unixepoch') → "2026-07-22 06:00:00"
                // Append 'T' and 'Z' to form a valid ISO 8601 UTC string.
                database.execSQL("""
                    INSERT INTO watched_episodes_new
                        (mediaId, mediaType, seasonNumber, episodeNumber, watchedAt)
                    SELECT
                        mediaId, mediaType, seasonNumber, episodeNumber,
                        REPLACE(datetime(watchedAt / 1000, 'unixepoch'), ' ', 'T') || 'Z'
                    FROM watched_episodes
                """.trimIndent())

                database.execSQL("DROP TABLE watched_episodes")
                database.execSQL("ALTER TABLE watched_episodes_new RENAME TO watched_episodes")
                database.execSQL(
                    "CREATE INDEX index_watched_episodes_mediaId_mediaType " +
                    "ON watched_episodes(mediaId, mediaType)"
                )

                // ── 3. Create pending_ops queue table ─────────────────────────
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS pending_ops (
                        localId    INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        opType     TEXT    NOT NULL,
                        payload    TEXT    NOT NULL,
                        enqueuedAt TEXT    NOT NULL DEFAULT ''
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration 6 → 7
         * Adds runtime, genres, and directorStudio columns for Stats Dashboard.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE media_items ADD COLUMN runtime INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE media_items ADD COLUMN genres TEXT DEFAULT NULL")
                database.execSQL("ALTER TABLE media_items ADD COLUMN directorStudio TEXT DEFAULT NULL")
            }
        }
    }
}
