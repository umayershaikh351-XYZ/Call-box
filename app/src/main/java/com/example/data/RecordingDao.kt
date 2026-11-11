package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.Recording
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Recording>>

    @Query("SELECT * FROM recordings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int = 5): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE fileName LIKE '%' || :query || '%' OR appLabel LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE appPackage = :appPackage ORDER BY timestamp DESC")
    fun filterByApp(appPackage: String): Flow<List<Recording>>

    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Recording?

    @Query("SELECT COUNT(*) FROM recordings")
    fun getCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM recordings")
    fun getTotalSizeBytes(): Flow<Long>

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM recordings")
    fun getTotalDurationMs(): Flow<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: Recording): Long

    @Update
    suspend fun update(recording: Recording)

    @Delete
    suspend fun delete(recording: Recording)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recordings")
    suspend fun deleteAll()
}
