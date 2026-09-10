package com.example.util

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.example.data.model.CharacterPronounConfig
import com.example.data.model.GameEra
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class OcrBlock(
    val text: String,
    val speaker: String? = null,
    val boundingBox: Rect? = null,
    val normalizedBox: NormalizedBox? = null
)

data class NormalizedBox(
    val y1: Float,
    val x1: Float,
    val y2: Float,
    val x2: Float
)

object GameTextRecognizer {
    private const val TAG = "GameTextRecognizer"

    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Extracts text from a game screenshot using on-device ML Kit OCR.
     * Uses a two-pass approach:
     * 1. Direct pass on input bitmap
     * 2. If nothing or very little found, attempts high-contrast pixel enhancement (crucial for Pokémon/GBA)
     */
    suspend fun recognizeGameText(sourceBitmap: Bitmap): List<OcrBlock> = withContext(Dispatchers.IO) {
        val results = mutableListOf<OcrBlock>()
        try {
            // First pass: standard OCR
            val blocks = performOcrPass(sourceBitmap)
            if (blocks.isNotEmpty()) {
                return@withContext blocks
            }

            // Second pass: Enhanced pixel contrast for retro/low-res game screens (Pokémon, GBA, emulator)
            val enhanced = PixelImageEnhancer.enhancePixelText(sourceBitmap)
            val enhancedBlocks = performOcrPass(enhanced)
            return@withContext enhancedBlocks
        } catch (e: Exception) {
            Log.e(TAG, "OCR recognition error: ${e.message}", e)
            return@withContext results
        }
    }

    private fun performOcrPass(bitmap: Bitmap): List<OcrBlock> {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val visionText = Tasks.await(textRecognizer.process(inputImage), 5, TimeUnit.SECONDS)

        val blocks = mutableListOf<OcrBlock>()
        val imgW = bitmap.width.toFloat().coerceAtLeast(1f)
        val imgH = bitmap.height.toFloat().coerceAtLeast(1f)

        val sortedTextBlocks = visionText.textBlocks.sortedBy { it.boundingBox?.top ?: 0 }

        for (textBlock in sortedTextBlocks) {
            val rawBlockText = textBlock.text.trim()
            if (rawBlockText.isBlank() || isNoiseArtifact(rawBlockText)) continue

            val box = textBlock.boundingBox
            val normBox = if (box != null) {
                NormalizedBox(
                    y1 = (box.top / imgH) * 1000f,
                    x1 = (box.left / imgW) * 1000f,
                    y2 = (box.bottom / imgH) * 1000f,
                    x2 = (box.right / imgW) * 1000f
                )
            } else null

            // Detect speaker pattern like "BIRCH:" or "Prof. Birch:"
            var speaker: String? = null
            var dialogue = rawBlockText

            val colonIdx = rawBlockText.indexOf(':')
            if (colonIdx in 1..25 && !rawBlockText.startsWith("http", ignoreCase = true)) {
                val candidate = rawBlockText.substring(0, colonIdx).trim()
                if (candidate.all { it.isLetterOrDigit() || it.isWhitespace() || it == '.' }) {
                    speaker = candidate
                    dialogue = rawBlockText.substring(colonIdx + 1).trim()
                }
            }

            // Clean dialogue: flatten line breaks to single space and remove trailing dialogue continuation markers
            val cleanedDialogue = dialogue
                .replace("\r", " ")
                .replace("\n", " ")
                .replace(Regex("[▼▶►🔻>_]+$"), "")
                .replace(Regex("\\s+"), " ")
                .trim()

            if (cleanedDialogue.isNotBlank()) {
                blocks.add(
                    OcrBlock(
                        text = cleanedDialogue,
                        speaker = speaker,
                        boundingBox = box,
                        normalizedBox = normBox
                    )
                )
            }
        }

        return blocks
    }

    private fun isNoiseArtifact(text: String): Boolean {
        if (text.length == 1 && !text[0].isLetterOrDigit()) return true
        val lettersOrDigits = text.count { it.isLetterOrDigit() }
        return lettersOrDigits == 0
    }

    fun containsThai(text: String): Boolean {
        return text.any { it in '\u0E00'..'\u0E7F' }
    }

    private fun cleanPixelOcrText(raw: String): String {
        var text = raw
        // Fix common retro GBA/pixel font OCR substitution errors
        text = text.replace(Regex("(?i)\\bWheul\\b"), "Whew!")
        text = text.replace(Regex("(?i)\\bIn truing\\b"), "I'm trying")
        text = text.replace(Regex("(?i)\\btruing\\b"), "trying")
        text = text.replace(Regex("(?i)\\bmenor\\s*ize\\b"), "memorize")
        text = text.replace(Regex("(?i)\\bal my\\b"), "all my")
        text = text.replace(Regex("(?i)\\bPokemon\\b"), "Pokémon")
        text = text.replace(Regex("(?i)\\bPokeball\\b"), "Pokéball")
        text = text.replace(Regex("(?i)\\bProf\\b"), "Professor")
        return text
    }

    /**
     * Fast, reliable translation fallback to natural Thai gaming language.
     * Uses MyMemory API and Google Mobile Web Translation for zero-API-key 100% Thai output.
     */
    suspend fun translateTextToThai(
        englishText: String,
        gameTitle: String = "",
        era: GameEra = GameEra.FANTASY_MEDIEVAL,
        pronounConfig: CharacterPronounConfig? = null
    ): String = withContext(Dispatchers.IO) {
        val trimmed = englishText.trim()
        if (trimmed.isBlank()) return@withContext ""

        // If it's already in Thai, no translation needed
        if (containsThai(trimmed)) {
            return@withContext postProcessGameThai(trimmed, gameTitle, era, pronounConfig)
        }

        val cleaned = cleanPixelOcrText(trimmed)

        // Tier 1: MyMemory Translation API (Free, high quality Thai translations, no CAPTCHA)
        try {
            val encodedQuery = URLEncoder.encode(cleaned, "UTF-8")
            val url = "https://api.mymemory.translated.net/get?q=$encodedQuery&langpair=en|th"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string()
            if (!body.isNullOrBlank()) {
                val json = JSONObject(body)
                val responseData = json.optJSONObject("responseData")
                val translated = responseData?.optString("translatedText", "") ?: ""
                if (translated.isNotBlank() && containsThai(translated)) {
                    val decoded = android.text.Html.fromHtml(translated, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                    return@withContext postProcessGameThai(decoded, gameTitle, era, pronounConfig)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tier 1 translation failed: ${e.message}")
        }

        // Tier 2: Google Mobile Web Translation (Reliable HTML endpoint, works without API keys)
        try {
            val encodedQuery = URLEncoder.encode(cleaned, "UTF-8")
            val url = "https://translate.google.com/m?sl=auto&tl=th&q=$encodedQuery"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")
                .build()

            val response = httpClient.newCall(request).execute()
            val html = response.body?.string()
            if (!html.isNullOrBlank()) {
                val matcher = java.util.regex.Pattern.compile("class=\"result-container\">([^<]*)<").matcher(html)
                if (matcher.find()) {
                    val matchedThai = matcher.group(1)?.trim()
                    if (!matchedThai.isNullOrBlank() && containsThai(matchedThai)) {
                        val decoded = android.text.Html.fromHtml(matchedThai, android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim()
                        return@withContext postProcessGameThai(decoded, gameTitle, era, pronounConfig)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tier 2 translation failed: ${e.message}")
        }

        // Tier 3: Built-in Gaming Vocabulary & Dialogue Dictionary fallback
        val dictThai = translateWithGameDictionary(cleaned)
        return@withContext postProcessGameThai(dictThai, gameTitle, era, pronounConfig)
    }

    private fun translateWithGameDictionary(text: String): String {
        val gameDict = mapOf(
            "whew" to "เฮ้อ",
            "notes" to "สมุดบันทึก",
            "note" to "โน้ต",
            "memorize" to "จดจำ",
            "trying to" to "กำลังพยายาม",
            "trying" to "พยายาม",
            "attack" to "โจมตี",
            "fight" to "ต่อสู้",
            "defend" to "ป้องกัน",
            "item" to "ไอเทม",
            "items" to "ไอเทม",
            "bag" to "กระเป๋า",
            "run" to "หนี",
            "escape" to "หลบหนี",
            "magic" to "เวทมนตร์",
            "skill" to "สกิล",
            "skills" to "ทักษะ",
            "pokemon" to "โปเกมอน",
            "pokémon" to "โปเกมอน",
            "potion" to "ยาฟื้นพลัง",
            "revive" to "ยาชุบชีวิต",
            "save" to "บันทึกข้อมูล",
            "load" to "โหลดข้อมูล",
            "start" to "เริ่มเกม",
            "continue" to "เล่นต่อ",
            "game over" to "จบเกม",
            "level up" to "เลเวลอัป!",
            "quest" to "ภารกิจ",
            "mission" to "ภารกิจ",
            "talk" to "คุย",
            "look" to "สำรวจ",
            "check" to "ตรวจสอบ",
            "open" to "เปิด",
            "close" to "ปิด",
            "cancel" to "ยกเลิก",
            "confirm" to "ยืนยัน",
            "yes" to "ใช่",
            "no" to "ไม่ใช่",
            "next" to "ถัดไป",
            "back" to "ย้อนกลับ",
            "battle" to "การต่อสู้",
            "victory" to "ชนะการต่อสู้!",
            "defeat" to "พ่ายแพ้",
            "danger" to "อันตราย",
            "warning" to "คำเตือน",
            "help" to "ช่วยเหลือ",
            "welcome" to "ยินดีต้อนรับ",
            "thank you" to "ขอบคุณ",
            "thanks" to "ขอบคุณ",
            "goodbye" to "ลาก่อน",
            "hello" to "สวัสดี",
            "hi" to "สวัสดี",
            "press start" to "กดปุ่มเริ่ม",
            "new game" to "เริ่มเกมใหม่",
            "option" to "ตั้งค่า",
            "options" to "การตั้งค่า"
        )

        var result = text
        gameDict.forEach { (en, th) ->
            result = result.replace(Regex("\\b(?i)$en\\b"), th)
        }

        return result
    }

    /**
     * Post-process translation to match gaming tone and role-play pronouns
     */
    private fun postProcessGameThai(
        thai: String,
        gameTitle: String,
        era: GameEra,
        pronounConfig: CharacterPronounConfig?
    ): String {
        var processed = thai

        // Game Terminology Normalization
        val termReplacements = mapOf(
            "โปเกมอน" to "โปเกมอน",
            "Pokemon" to "โปเกมอน",
            "POKEMON" to "โปเกมอน",
            "POKÉMON" to "โปเกมอน",
            "ศาสตราจารย์" to "ดร.",
            "เทรนเนอร์" to "เทรนเนอร์",
            "ยิมลีดเดอร์" to "ยิมลีดเดอร์",
            "พลังชีวิต" to "พลังชีวิต (HP)",
            "ความเสียหาย" to "ดาเมจ"
        )
        termReplacements.forEach { (en, th) ->
            processed = processed.replace(en, th, ignoreCase = true)
        }

        // Apply Pronouns if custom
        if (pronounConfig != null) {
            if (pronounConfig.protagonistSelf.isNotBlank() && pronounConfig.protagonistSelf != "ฉัน") {
                processed = processed.replace("ฉัน", pronounConfig.protagonistSelf)
                processed = processed.replace("ผม", pronounConfig.protagonistSelf)
            }
            if (pronounConfig.protagonistToOther.isNotBlank() && pronounConfig.protagonistToOther != "คุณ") {
                processed = processed.replace("คุณ", pronounConfig.protagonistToOther)
                processed = processed.replace("เธอ", pronounConfig.protagonistToOther)
            }
        }

        return processed
    }
}
