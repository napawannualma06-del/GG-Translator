package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TranslatedBlock(
    @Json(name = "speaker")
    val speaker: String? = null,
    @Json(name = "sourceText")
    val sourceText: String = "",
    @Json(name = "translatedText")
    val translatedText: String = "",
    @Json(name = "category")
    val category: String = "DIALOGUE", // DIALOGUE, QUEST, LORE, UI, SKILL
    @Json(name = "contextNote")
    val contextNote: String? = null,
    @Json(name = "boxY1")
    val boxY1: Float? = null, // 0..1000 normalized coordinate
    @Json(name = "boxX1")
    val boxX1: Float? = null,
    @Json(name = "boxY2")
    val boxY2: Float? = null,
    @Json(name = "boxX2")
    val boxX2: Float? = null
)

@JsonClass(generateAdapter = true)
data class TranslationResponsePayload(
    @Json(name = "gameTitleEstimate")
    val gameTitleEstimate: String? = "Unknown Game",
    @Json(name = "sceneContext")
    val sceneContext: String? = null,
    @Json(name = "translations")
    val translations: List<TranslatedBlock> = emptyList()
)
