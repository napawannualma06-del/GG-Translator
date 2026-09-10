package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {
    @Query("SELECT * FROM translation_history ORDER BY timestamp DESC")
    fun getAllTranslations(): Flow<List<TranslationRecord>>

    @Query("SELECT * FROM translation_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteTranslations(): Flow<List<TranslationRecord>>

    @Query("SELECT * FROM translation_history WHERE sourceEnglish LIKE '%' || :query || '%' OR translatedThai LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchTranslations(query: String): Flow<List<TranslationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(record: TranslationRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<TranslationRecord>)

    @Update
    suspend fun updateTranslation(record: TranslationRecord)

    @Delete
    suspend fun deleteTranslation(record: TranslationRecord)

    @Query("DELETE FROM translation_history")
    suspend fun clearAll()
}
