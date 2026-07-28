package com.loopa.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for the offline write queue.
 * All operations are suspend functions — call from a coroutine / Dispatchers.IO.
 */
@Dao
interface PendingOpDao {

    /** Enqueue a new operation. Returns the auto-generated localId. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(op: PendingOpEntity): Long

    /** Fetch all pending ops in FIFO order (oldest first). */
    @Query("SELECT * FROM pending_ops ORDER BY localId ASC")
    suspend fun getAll(): List<PendingOpEntity>

    /** Remove a successfully-flushed op by its primary key. */
    @Query("DELETE FROM pending_ops WHERE localId = :localId")
    suspend fun deleteById(localId: Long)

    /** How many ops are still waiting to be flushed. */
    @Query("SELECT COUNT(*) FROM pending_ops")
    suspend fun count(): Int

    /** Clear the entire queue — used only in tests or after a full remote sync. */
    @Query("DELETE FROM pending_ops")
    suspend fun clearAll()
}
