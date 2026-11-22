package com.rajeshwari.netsleuth.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticDao {
    @Insert suspend fun insert(record: DiagnosticRecord)
    @Query("SELECT * FROM diagnostic_records ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<DiagnosticRecord>>
    @Query("DELETE FROM diagnostic_records")
    suspend fun clearAll()
}
