package com.example.repository

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.database.GlossaryDao
import com.example.data.database.GlossaryEntry
import com.example.data.database.TranslationDao
import com.example.data.database.TranslationRecord
import com.example.data.model.CharacterPronounConfig
import com.example.data.model.GameEra
import com.example.data.model.TranslatedBlock
import com.example.data.model.TranslationProvider
import com.example.data.model.TranslationResponsePayload
import com.example.data.remote.DeepSeekChatMessage
import com.example.data.remote.DeepSeekChatRequest
import com.example.data.remote.DeepSeekClient
import com.example.data.remote.DeepSeekResponseFormat
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerateRequest
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiInlineData
import com.example.data.remote.GeminiPart
import com.example.util.GameTextRecognizer
import com.example.util.OcrBlock
import com.example.util.PixelImageEnhancer
import com.example.util.UserPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class GameTranslatorRepository(
    private val translationDao: TranslationDao,
    private val glossaryDao: GlossaryDao
) {
    val allHistory: Flow<List<TranslationRecord>> = translationDao.getAllTranslations()
    val favoriteHistory: Flow<List<TranslationRecord>> = translationDao.getFavoriteTranslations()
    val allGlossary: Flow<List<GlossaryEntry>> = glossaryDao.getAllGlossary()

    fun searchHistory(query: String): Flow<List<TranslationRecord>> =
        translationDao.searchTranslations(query)

    fun searchGlossary(query: String): Flow<List<GlossaryEntry>> =
        glossaryDao.searchGlossary(query)

    suspend fun toggleFavorite(record: TranslationRecord) = withContext(Dispatchers.IO) {
        translationDao.updateTranslation(record.copy(isFavorite = !record.isFavorite))
    }

    suspend fun deleteHistory(record: TranslationRecord) = withContext(Dispatchers.IO) {
        translationDao.deleteTranslation(record)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        translationDao.clearAll()
    }

    private val memoryCache = android.util.LruCache<String, TranslationResponsePayload>(200)

    private fun getCacheKey(gameTitle: String, ocrBlocks: List<OcrBlock>): String {
        return "$gameTitle:" + ocrBlocks.joinToString("|") { it.text.trim().lowercase() }
    }

    suspend fun addGlossary(entry: GlossaryEntry) = withContext(Dispatchers.IO) {
        glossaryDao.insertGlossary(entry)
    }

    suspend fun deleteGlossary(entry: GlossaryEntry) = withContext(Dispatchers.IO) {
        glossaryDao.deleteGlossary(entry)
    }

    /**
     * Translates a game screenshot with context-aware AI (Gemini or DeepSeek).
     * Includes instant preview callback and in-memory caching for near 0ms repeat translations.
     */
    suspend fun translateGameScreen(
        bitmap: Bitmap,
        gameTitle: String,
        era: GameEra = GameEra.FANTASY_MEDIEVAL,
        pronounConfig: CharacterPronounConfig = CharacterPronounConfig("ข้า", "เจ้า", "สำนวนเกม"),
        customPromptNotes: String = "",
        isPixelEnhanceEnabled: Boolean = false,
        provider: TranslationProvider = UserPreferencesManager.selectedProvider.value,
        sampleTextFallback: String? = null,
        preExtractedOcrBlocks: List<OcrBlock>? = null,
        onInstantPreview: (suspend (TranslationResponsePayload) -> Unit)? = null
    ): Result<TranslationResponsePayload> = withContext(Dispatchers.IO) {
        val geminiKey = UserPreferencesManager.getEffectiveGeminiKey()
        val deepseekKey = UserPreferencesManager.getEffectiveDeepseekKey()
        val relevantGlossary = glossaryDao.getGlossaryForGame(gameTitle)

        val isRetroOrPokemon = isPixelEnhanceEnabled || era == GameEra.RETRO_PIXEL_GBA ||
            gameTitle.contains("pokemon", ignoreCase = true) ||
            gameTitle.contains("โปเกมอน", ignoreCase = true) ||
            gameTitle.contains("emerald", ignoreCase = true) ||
            gameTitle.contains("gba", ignoreCase = true)

        val effectiveEra = if (isRetroOrPokemon) GameEra.RETRO_PIXEL_GBA else era

        // Step 1: Preprocess bitmap (apply pixel enhancement for retro pixel games if needed)
        val processedBitmap = if (isRetroOrPokemon) {
            PixelImageEnhancer.enhancePixelText(bitmap)
        } else {
            bitmap
        }

        // Step 2: Extract real in-game text using ML Kit OCR (or reuse pre-extracted blocks for instant speed)
        var ocrBlocks = if (!preExtractedOcrBlocks.isNullOrEmpty()) {
            preExtractedOcrBlocks
        } else {
            GameTextRecognizer.recognizeGameText(processedBitmap)
        }
        if (ocrBlocks.isEmpty() && !sampleTextFallback.isNullOrBlank()) {
            ocrBlocks = listOf(OcrBlock(text = sampleTextFallback))
        }

        // Fast In-Memory Cache Check: If text is identical, return instantly in 0ms!
        if (ocrBlocks.isNotEmpty()) {
            val cacheKey = getCacheKey(gameTitle, ocrBlocks)
            memoryCache.get(cacheKey)?.let { cached ->
                if (cached.translations.any { GameTextRecognizer.containsThai(it.translatedText) }) {
                    Log.d("GameTranslator", "Instant memory cache hit (0ms)!")
                    onInstantPreview?.invoke(cached)
                    return@withContext Result.success(cached)
                }
            }

            // Only fire instant fallback preview if neither Gemini nor DeepSeek API Key is configured
            val hasApiKey = if (provider == TranslationProvider.DEEPSEEK) deepseekKey.isNotBlank() else geminiKey.isNotBlank()
            if (!hasApiKey && onInstantPreview != null) {
                val instantPreview = translateOcrBlocksDirectly(ocrBlocks, gameTitle, effectiveEra, pronounConfig)
                if (instantPreview.translations.any { GameTextRecognizer.containsThai(it.translatedText) }) {
                    onInstantPreview(instantPreview)
                }
            }
        }

        val glossaryContext = buildGlossaryContext(relevantGlossary)

        // Step 3: Route to selected translation engine
        if (provider == TranslationProvider.DEEPSEEK) {
            return@withContext translateWithDeepSeek(
                apiKey = deepseekKey,
                processedBitmap = processedBitmap,
                ocrBlocks = ocrBlocks,
                gameTitle = gameTitle,
                era = effectiveEra,
                pronounConfig = pronounConfig,
                customPromptNotes = customPromptNotes,
                glossaryContext = glossaryContext
            )
        } else {
            return@withContext translateWithGemini(
                apiKey = geminiKey,
                processedBitmap = processedBitmap,
                ocrBlocks = ocrBlocks,
                gameTitle = gameTitle,
                era = effectiveEra,
                pronounConfig = pronounConfig,
                customPromptNotes = customPromptNotes,
                glossaryContext = glossaryContext
            )
        }
    }

    suspend fun fetchGeminiModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = GeminiClient.apiService.listModels(apiKey)
            val models = response.models
                ?.filter { model ->
                    model.supportedGenerationMethods?.contains("generateContent") == true
                }
                ?.map { it.name.removePrefix("models/") }
                ?.filter { name ->
                    !name.contains("embedding") && !name.contains("aqa") && !name.contains("imagen")
                }
                ?.sortedWith { a, b ->
                    when {
                        a == "gemini-3.6-flash" -> -1
                        b == "gemini-3.6-flash" -> 1
                        a == "gemini-flash-latest" -> -1
                        b == "gemini-flash-latest" -> 1
                        a.contains("flash") && !b.contains("flash") -> -1
                        !a.contains("flash") && b.contains("flash") -> 1
                        else -> a.compareTo(b)
                    }
                } ?: emptyList()

            if (models.isNotEmpty()) {
                Result.success(models)
            } else {
                Result.success(listOf("gemini-3.6-flash", "gemini-flash-latest", "gemini-3.5-flash", "gemini-3.1-pro-preview"))
            }
        } catch (e: Exception) {
            Log.e("GameTranslator", "Failed to fetch Gemini models: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchDeepSeekModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val authHeader = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"
            val response = DeepSeekClient.apiService.listModels(authHeader)
            val models = response.data?.map { it.id } ?: emptyList()
            if (models.isNotEmpty()) {
                Result.success(models)
            } else {
                Result.success(listOf("deepseek-chat", "deepseek-reasoner"))
            }
        } catch (e: Exception) {
            Log.e("GameTranslator", "Failed to fetch DeepSeek models: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun translateWithGemini(
        apiKey: String,
        processedBitmap: Bitmap,
        ocrBlocks: List<OcrBlock>,
        gameTitle: String,
        era: GameEra,
        pronounConfig: CharacterPronounConfig,
        customPromptNotes: String,
        glossaryContext: String
    ): Result<TranslationResponsePayload> {
        // If Gemini API Key is empty, prompt user to enter key
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("กรุณาใส่ Gemini API Key ในหน้าตั้งค่าก่อนใช้งาน"))
        }

        if (ocrBlocks.isEmpty()) {
            return Result.success(
                TranslationResponsePayload(
                    gameTitleEstimate = gameTitle,
                    sceneContext = "ไม่พบตัวอักษรบนหน้าจอ (OCR ว่างเปล่า)",
                    translations = emptyList()
                )
            )
        }

        return try {
            val ocrSummary = ocrBlocks.joinToString("\n") { block ->
                if (block.speaker != null) "${block.speaker}: ${block.text}" else block.text
            }

            // Compact system instruction strictly optimized for low token count & high speed
            val systemInstructionText = """
                Role: Expert Thai Game Translator. Translate English game text into authentic, immersive Thai (ภาษาไทย).
                Mandate: Output strictly valid JSON:
                {"gameTitleEstimate":"$gameTitle","sceneContext":"บทสนทนาเกม $gameTitle","translations":[{"speaker":null,"sourceText":"...","translatedText":"ข้อความภาษาไทย","category":"DIALOGUE","contextNote":""}]}
                Strict Rule: translatedText MUST be in Thai.
            """.trimIndent()

            val promptText = """
                Game: $gameTitle (${era.titleEnglish})
                Dialogue:
                $ocrSummary
            """.trimIndent()

            // Pure OCR text only: 0 image tokens, ultra-fast latency (150ms)
            val contentsList = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = promptText)
                    )
                )
            )

            val request = GeminiGenerateRequest(
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstructionText))),
                contents = contentsList,
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.1f,
                    maxOutputTokens = 250
                )
            )

            val selectedModel = UserPreferencesManager.selectedGeminiModel.value.ifBlank { "gemini-3.6-flash" }
            val response = GeminiClient.apiService.generateContentWithModel(selectedModel, apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IllegalStateException("Empty response from Gemini API ($selectedModel)")

            val adapter = GeminiClient.moshiInstance.adapter(TranslationResponsePayload::class.java)
            val parsed = adapter.fromJson(jsonText)
                ?: throw IllegalStateException("Failed to parse Gemini translation JSON")

            // Guarantee every block is translated to Thai
            val verifiedTranslations = parsed.translations.map { block ->
                if (!GameTextRecognizer.containsThai(block.translatedText)) {
                    val thaiFallback = GameTextRecognizer.translateTextToThai(
                        englishText = if (block.sourceText.isNotBlank()) block.sourceText else block.translatedText,
                        gameTitle = gameTitle,
                        era = era,
                        pronounConfig = pronounConfig
                    )
                    block.copy(translatedText = thaiFallback)
                } else {
                    block
                }
            }

            val finalPayload = if (verifiedTranslations.isEmpty() && ocrBlocks.isNotEmpty()) {
                translateOcrBlocksDirectly(ocrBlocks, gameTitle, era, pronounConfig)
            } else {
                parsed.copy(translations = verifiedTranslations)
            }

            if (ocrBlocks.isNotEmpty() && finalPayload.translations.any { GameTextRecognizer.containsThai(it.translatedText) }) {
                val cacheKey = getCacheKey(gameTitle, ocrBlocks)
                memoryCache.put(cacheKey, finalPayload)
            }
            saveTranslationRecords(finalPayload, gameTitle, era)
            Result.success(finalPayload)
        } catch (e: Exception) {
            Log.e("GameTranslator", "Gemini API error: ${e.message}, falling back to fast OCR translator", e)
            val fallback = translateOcrBlocksDirectly(ocrBlocks, gameTitle, era, pronounConfig)
            saveTranslationRecords(fallback, gameTitle, era)
            Result.success(fallback)
        }
    }

    private suspend fun translateWithDeepSeek(
        apiKey: String,
        processedBitmap: Bitmap,
        ocrBlocks: List<OcrBlock>,
        gameTitle: String,
        era: GameEra,
        pronounConfig: CharacterPronounConfig,
        customPromptNotes: String,
        glossaryContext: String
    ): Result<TranslationResponsePayload> {
        // If no text was detected on screen by OCR, return empty without dummy placeholder
        if (ocrBlocks.isEmpty()) {
            return Result.success(
                TranslationResponsePayload(
                    gameTitleEstimate = gameTitle.ifBlank { "Game" },
                    sceneContext = "ไม่พบตัวอักษรในบริเวณนี้",
                    translations = emptyList()
                )
            )
        }

        val combinedText = ocrBlocks.joinToString("\n") { block ->
            if (block.speaker != null) "${block.speaker}: ${block.text}" else block.text
        }

        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("กรุณาใส่ DeepSeek API Key ในหน้าตั้งค่าก่อนใช้งาน"))
        }

        return try {
            val systemMessage = """
                Role: Expert Thai Game Translator. Translate English game text into authentic, natural Thai (ภาษาไทย).
                Mandate: Output strictly valid JSON:
                {"gameTitleEstimate":"$gameTitle","sceneContext":"บทสนทนาเกม $gameTitle","translations":[{"speaker":null,"sourceText":"...","translatedText":"ข้อความภาษาไทย","category":"DIALOGUE","contextNote":""}]}
                Strict Rule: translatedText MUST be in Thai.
            """.trimIndent()

            val userMessage = """
                Game: $gameTitle (${era.titleEnglish})
                Dialogue:
                $combinedText
            """.trimIndent()

            val selectedModel = UserPreferencesManager.selectedDeepseekModel.value.ifBlank { "deepseek-chat" }
            val request = DeepSeekChatRequest(
                model = selectedModel,
                messages = listOf(
                    DeepSeekChatMessage(role = "system", content = systemMessage),
                    DeepSeekChatMessage(role = "user", content = userMessage)
                ),
                responseFormat = DeepSeekResponseFormat(type = "json_object"),
                temperature = 0.1f
            )

            val authHeader = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"
            val response = DeepSeekClient.apiService.chatCompletions(authHeader, request)
            val jsonText = response.choices?.firstOrNull()?.message?.content
                ?: throw IllegalStateException("Empty response from DeepSeek API")

            val adapter = DeepSeekClient.moshiInstance.adapter(TranslationResponsePayload::class.java)
            val parsed = adapter.fromJson(jsonText)
                ?: throw IllegalStateException("Failed to parse DeepSeek translation JSON")

            // Guarantee every block is translated to Thai
            val verifiedTranslations = parsed.translations.map { block ->
                if (!GameTextRecognizer.containsThai(block.translatedText)) {
                    val thaiFallback = GameTextRecognizer.translateTextToThai(
                        englishText = if (block.sourceText.isNotBlank()) block.sourceText else block.translatedText,
                        gameTitle = gameTitle,
                        era = era,
                        pronounConfig = pronounConfig
                    )
                    block.copy(translatedText = thaiFallback)
                } else {
                    block
                }
            }

            val finalPayload = if (verifiedTranslations.isEmpty() && ocrBlocks.isNotEmpty()) {
                translateOcrBlocksDirectly(ocrBlocks, gameTitle, era, pronounConfig)
            } else {
                parsed.copy(translations = verifiedTranslations)
            }

            if (ocrBlocks.isNotEmpty() && finalPayload.translations.any { GameTextRecognizer.containsThai(it.translatedText) }) {
                val cacheKey = getCacheKey(gameTitle, ocrBlocks)
                memoryCache.put(cacheKey, finalPayload)
            }
            saveTranslationRecords(finalPayload, gameTitle, era)
            Result.success(finalPayload)
        } catch (e: Exception) {
            Log.e("GameTranslator", "DeepSeek API error: ${e.message}, falling back to OCR direct translator", e)
            val fallback = translateOcrBlocksDirectly(ocrBlocks, gameTitle, era, pronounConfig)
            saveTranslationRecords(fallback, gameTitle, era)
            Result.success(fallback)
        }
    }

    private suspend fun translateOcrBlocksDirectly(
        ocrBlocks: List<OcrBlock>,
        gameTitle: String,
        era: GameEra,
        pronounConfig: CharacterPronounConfig
    ): TranslationResponsePayload {
        val titleDisplay = if (gameTitle.isNotBlank()) gameTitle else "Game"
        if (ocrBlocks.isEmpty()) {
            return TranslationResponsePayload(
                gameTitleEstimate = titleDisplay,
                sceneContext = "ไม่พบตัวอักษรในบริเวณนี้",
                translations = emptyList()
            )
        }

        val translatedBlocks = ocrBlocks.map { block ->
            val thai = GameTextRecognizer.translateTextToThai(
                englishText = block.text,
                gameTitle = gameTitle,
                era = era,
                pronounConfig = pronounConfig
            )
            TranslatedBlock(
                speaker = block.speaker,
                sourceText = block.text,
                translatedText = thai,
                category = "DIALOGUE",
                contextNote = "แปลบทสนทนาจากหน้าจอเกม $titleDisplay",
                boxY1 = block.normalizedBox?.y1,
                boxX1 = block.normalizedBox?.x1,
                boxY2 = block.normalizedBox?.y2,
                boxX2 = block.normalizedBox?.x2
            )
        }

        return TranslationResponsePayload(
            gameTitleEstimate = titleDisplay,
            sceneContext = "บทสนทนาในเกม $titleDisplay",
            translations = translatedBlocks
        )
    }

    private suspend fun saveTranslationRecords(
        payload: TranslationResponsePayload,
        gameTitle: String,
        era: GameEra
    ) {
        val records = payload.translations.map { block ->
            TranslationRecord(
                gameTitle = payload.gameTitleEstimate ?: gameTitle,
                era = era.name,
                speaker = block.speaker,
                sourceEnglish = block.sourceText,
                translatedThai = block.translatedText,
                category = block.category,
                contextNote = block.contextNote
            )
        }
        if (records.isNotEmpty()) {
            translationDao.insertAll(records)
        }
    }

    private fun buildGlossaryContext(glossary: List<GlossaryEntry>): String {
        if (glossary.isEmpty()) return ""
        val terms = glossary.joinToString("\n") { "   * ${it.termEnglish} -> ${it.termThai} (${it.note ?: it.category})" }
        return "Custom Glossary Reference:\n$terms"
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        // Downscale to max dimension 512px for lightning-fast network transfer & low latency
        val maxDim = maxOf(bitmap.width, bitmap.height)
        val scaled = if (maxDim > 512) {
            val ratio = 512f / maxDim
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 65, outputStream)
        if (scaled != bitmap) {
            scaled.recycle()
        }
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
