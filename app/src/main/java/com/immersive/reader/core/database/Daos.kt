package com.immersive.reader.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY COALESCE(lastOpenedAt, addedAt) DESC")
    fun observeBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    suspend fun getById(bookId: String): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookEntity)

    @Update
    suspend fun update(book: BookEntity)

    @Delete
    suspend fun delete(book: BookEntity)
}

@Dao
interface ReadingSessionDao {
    @Query("SELECT * FROM reading_sessions WHERE status = 'ACTIVE' ORDER BY startEpochMillis DESC LIMIT 1")
    suspend fun getActive(): ReadingSessionEntity?

    @Query("SELECT * FROM reading_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: String): ReadingSessionEntity?

    @Query("SELECT * FROM reading_sessions ORDER BY startEpochMillis DESC")
    fun observeAll(): Flow<List<ReadingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ReadingSessionEntity)

    @Update
    suspend fun update(session: ReadingSessionEntity)

    @Query("SELECT COALESCE(SUM(accumulatedReadingMillis), 0) FROM reading_sessions WHERE status != 'INTERRUPTED'")
    fun observeTotalReadingMillis(): Flow<Long>

    @Query("SELECT COUNT(*) FROM reading_sessions")
    fun observeSessionCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reading_sessions WHERE status = 'COMPLETED'")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reading_sessions WHERE status IN ('EMERGENCY_EXIT', 'INTERRUPTED')")
    fun observeInterruptedCount(): Flow<Int>
}
