package com.loopa.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a Supabase mutation that could not be delivered because the device
 * was offline (or the network call threw an exception).
 *
 * Ops are flushed in FIFO order (ascending localId) when connectivity returns,
 * using Last-Write-Wins via [enqueuedAt] vs the remote row's updated_at.
 *
 * Supported [opType] values:
 *   "UPSERT_MEDIA"   — insert or update a media_items row
 *   "DELETE_MEDIA"   — delete a media_items row
 *   "UPSERT_EPISODE" — insert or update a watched_episodes row
 *   "DELETE_EPISODE" — delete a watched_episodes row
 */
@Entity(tableName = "pending_ops")
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,

    /** Operation discriminator — see supported values above. */
    val opType: String,

    /**
     * JSON-serialised payload of the operation.
     * For MEDIA ops: serialised RemoteMediaItem.
     * For EPISODE ops: serialised RemoteWatchedEpisode.
     */
    val payload: String,

    /**
     * ISO 8601 UTC timestamp of when this op was enqueued locally.
     * Used for Last-Write-Wins: if the remote row's updated_at is NEWER than
     * this value, the UPSERT is skipped to avoid clobbering a more recent write.
     */
    val enqueuedAt: String = java.time.Instant.now().toString()
)
