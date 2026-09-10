package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_history")
data class TranslationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val gameTitle: String = "General Game",
    val era: String = "FANTASY_MEDIEVAL",
    val speaker: String? = null,
    val sourceEnglish: String,
    val translatedThai: String,
    val category: String = "DIALOGUE",
    val contextNote: String? = null,
    val isFavorite: Boolean = false
)
