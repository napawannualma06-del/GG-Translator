package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_glossary")
data class GlossaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameTitle: String = "All Games",
    val termEnglish: String,
    val termThai: String,
    val category: String = "CHARACTER", // CHARACTER, ITEM, SKILL, LOCATION, FACTION
    val note: String? = null
)
