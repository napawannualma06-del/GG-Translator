package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GlossaryDao {
    @Query("SELECT * FROM game_glossary ORDER BY termEnglish ASC")
    fun getAllGlossary(): Flow<List<GlossaryEntry>>

    @Query("SELECT * FROM game_glossary WHERE gameTitle = :gameTitle OR gameTitle = 'All Games' ORDER BY termEnglish ASC")
    suspend fun getGlossaryForGame(gameTitle: String): List<GlossaryEntry>

    @Query("SELECT * FROM game_glossary WHERE termEnglish LIKE '%' || :query || '%' OR termThai LIKE '%' || :query || '%' ORDER BY termEnglish ASC")
    fun searchGlossary(query: String): Flow<List<GlossaryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlossary(entry: GlossaryEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<GlossaryEntry>)

    @Update
    suspend fun updateGlossary(entry: GlossaryEntry)

    @Delete
    suspend fun deleteGlossary(entry: GlossaryEntry)
}
