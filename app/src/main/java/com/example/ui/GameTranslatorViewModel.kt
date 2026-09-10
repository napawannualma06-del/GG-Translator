package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GameTranslatorApp
import com.example.data.database.GlossaryEntry
import com.example.data.database.TranslationRecord
import com.example.data.model.CharacterPronounConfig
import com.example.data.model.GameEra
import com.example.data.model.TranslationProvider
import com.example.data.model.TranslationResponsePayload
import com.example.service.GameOverlayService
import com.example.util.UserPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GamePresetScene(
    val id: String,
    val title: String,
    val era: GameEra,
    val sampleTextEnglish: String,
    val speaker: String,
    val category: String,
    val hintPrompt: String
)

class GameTranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as GameTranslatorApp).repository

    val historyList: StateFlow<List<TranslationRecord>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteList: StateFlow<List<TranslationRecord>> = repository.favoriteHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val glossaryList: StateFlow<List<GlossaryEntry>> = repository.allGlossary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // API Keys and Provider from Preferences
    val geminiApiKey: StateFlow<String> = UserPreferencesManager.geminiApiKey
    val deepseekApiKey: StateFlow<String> = UserPreferencesManager.deepseekApiKey
    val selectedProvider: StateFlow<TranslationProvider> = UserPreferencesManager.selectedProvider
    val selectedGeminiModel: StateFlow<String> = UserPreferencesManager.selectedGeminiModel
    val selectedDeepseekModel: StateFlow<String> = UserPreferencesManager.selectedDeepseekModel

    private val _geminiModelsList = MutableStateFlow<List<String>>(
        listOf("gemini-3.6-flash", "gemini-flash-latest", "gemini-3.5-flash", "gemini-3.1-pro-preview")
    )
    val geminiModelsList: StateFlow<List<String>> = _geminiModelsList.asStateFlow()

    private val _deepseekModelsList = MutableStateFlow<List<String>>(
        listOf("deepseek-chat", "deepseek-reasoner")
    )
    val deepseekModelsList: StateFlow<List<String>> = _deepseekModelsList.asStateFlow()

    private val _isFetchingModels = MutableStateFlow(false)
    val isFetchingModels: StateFlow<Boolean> = _isFetchingModels.asStateFlow()

    private val _fetchModelMessage = MutableStateFlow<String?>(null)
    val fetchModelMessage: StateFlow<String?> = _fetchModelMessage.asStateFlow()

    fun setSelectedGeminiModel(model: String) {
        UserPreferencesManager.setSelectedGeminiModel(model)
    }

    fun setSelectedDeepseekModel(model: String) {
        UserPreferencesManager.setSelectedDeepseekModel(model)
    }

    fun fetchModelsForProvider(provider: TranslationProvider) {
        viewModelScope.launch {
            _isFetchingModels.value = true
            _fetchModelMessage.value = null
            if (provider == TranslationProvider.GEMINI) {
                val key = UserPreferencesManager.getEffectiveGeminiKey()
                if (key.isBlank()) {
                    _fetchModelMessage.value = "กรุณาใส่ Gemini API Key ก่อนดึงโมเดล"
                    _isFetchingModels.value = false
                    return@launch
                }
                val result = repository.fetchGeminiModels(key)
                result.onSuccess { models ->
                    if (models.isNotEmpty()) {
                        _geminiModelsList.value = models
                        _fetchModelMessage.value = "ดึงสำเร็จ (${models.size} รุ่น)"
                        if (selectedGeminiModel.value !in models) {
                            UserPreferencesManager.setSelectedGeminiModel(models.first())
                        }
                    }
                }.onFailure { e ->
                    _fetchModelMessage.value = "ดึงโมเดลไม่สำเร็จ: ${e.message}"
                }
            } else {
                val key = UserPreferencesManager.getEffectiveDeepseekKey()
                if (key.isBlank()) {
                    _fetchModelMessage.value = "กรุณาใส่ DeepSeek API Key ก่อนดึงโมเดล"
                    _isFetchingModels.value = false
                    return@launch
                }
                val result = repository.fetchDeepSeekModels(key)
                result.onSuccess { models ->
                    if (models.isNotEmpty()) {
                        _deepseekModelsList.value = models
                        _fetchModelMessage.value = "ดึงสำเร็จ (${models.size} รุ่น)"
                        if (selectedDeepseekModel.value !in models) {
                            UserPreferencesManager.setSelectedDeepseekModel(models.first())
                        }
                    }
                }.onFailure { e ->
                    _fetchModelMessage.value = "ดึงโมเดลไม่สำเร็จ: ${e.message}"
                }
            }
            _isFetchingModels.value = false
        }
    }

    // Game Title & Context State
    val gameTitle: StateFlow<String> = UserPreferencesManager.gameTitle

    private val _currentEra = MutableStateFlow(GameEra.FANTASY_MEDIEVAL)
    val currentEra: StateFlow<GameEra> = _currentEra.asStateFlow()

    private val _pronounConfig = MutableStateFlow(
        CharacterPronounConfig(
            protagonistSelf = "ข้า",
            protagonistToOther = "เจ้า",
            toneStyle = "สำนวนเกมแฟนตาซี ดุดัน ทรงพลัง"
        )
    )
    val pronounConfig: StateFlow<CharacterPronounConfig> = _pronounConfig.asStateFlow()

    private val _customPromptNotes = MutableStateFlow("แปลบทสนทนาและคำอธิบายในเกมให้สละสลวยตามบรรยากาศของเกม")
    val customPromptNotes: StateFlow<String> = _customPromptNotes.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _lastResult = MutableStateFlow<TranslationResponsePayload?>(null)
    val lastResult: StateFlow<TranslationResponsePayload?> = _lastResult.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val isOverlayRunning: StateFlow<Boolean> = GameOverlayService.serviceRunningState
    val isAutoTranslateEnabled: StateFlow<Boolean> = UserPreferencesManager.isAutoTranslateEnabled
    val autoTranslateSpeed: StateFlow<com.example.data.model.AutoTranslateSpeed> = UserPreferencesManager.autoTranslateSpeed

    private val _overlayOpacity = MutableStateFlow(0.85f)
    val overlayOpacity: StateFlow<Float> = _overlayOpacity.asStateFlow()

    private val _overlayFontSize = MutableStateFlow(15)
    val overlayFontSize: StateFlow<Int> = _overlayFontSize.asStateFlow()

    private val _isPixelEnhanceEnabled = MutableStateFlow(true)
    val isPixelEnhanceEnabled: StateFlow<Boolean> = _isPixelEnhanceEnabled.asStateFlow()

    // Interactive In-App Simulation & Presets
    val presetScenes = listOf(
        GamePresetScene(
            id = "souls_boss",
            title = "Elden Ring",
            era = GameEra.FANTASY_MEDIEVAL,
            sampleTextEnglish = "Foul Tarnished, in search of the Elden Ring. Emboldened by the flame of ambition! Someone must extinguish thy flame.",
            speaker = "Margit, the Fell Omen",
            category = "DIALOGUE",
            hintPrompt = "บอส Dark Fantasy สำนวนอังกฤษโบราณ (Thou/Thy)"
        ),
        GamePresetScene(
            id = "gba_pokemon",
            title = "Pokémon Emerald",
            era = GameEra.RETRO_PIXEL_GBA,
            sampleTextEnglish = "Hi! Welcome to the world of POKÉMON! My name is BIRCH. But everyone calls me the POKÉMON PROFESSOR.",
            speaker = "Prof. Birch",
            category = "DIALOGUE",
            hintPrompt = "ฟอนต์พิกเซลบิตแมป สไตล์ Game Boy Advance"
        ),
        GamePresetScene(
            id = "cyberpunk_quest",
            title = "Cyberpunk 2077",
            era = GameEra.CYBERPUNK_SCIFI,
            sampleTextEnglish = "Wake the fuck up, Samurai! We've got a city to burn. Arasaka netrunners are tracing the biochip.",
            speaker = "Johnny Silverhand",
            category = "DIALOGUE",
            hintPrompt = "สแลงสตรีท สไตล์ไซเบอร์พังก์ดิบเถื่อน"
        ),
        GamePresetScene(
            id = "anime_genshin",
            title = "Genshin Impact",
            era = GameEra.ANIME_JRPG,
            sampleTextEnglish = "Hey Traveler! The Anemo Archon said the Stormterror is raging again at Dvalin's lair!",
            speaker = "Paimon",
            category = "DIALOGUE",
            hintPrompt = "สดใส น่ารัก เป็นมิตร เพื่อนร่วมผจญภัย"
        ),
        GamePresetScene(
            id = "cod_tactical",
            title = "Call of Duty: Warzone",
            era = GameEra.MODERN_MILITARY,
            sampleTextEnglish = "Bravo Six, going dark. UAV overhead, multiple hostiles moving toward the extraction LZ.",
            speaker = "Captain Price",
            category = "QUEST",
            hintPrompt = "ยุทธวิธีทางทหาร กระชับ ชัดเจน รวดเร็ว"
        )
    )

    private val _selectedPresetIndex = MutableStateFlow(0)
    val selectedPresetIndex: StateFlow<Int> = _selectedPresetIndex.asStateFlow()

    private val _customBitmap = MutableStateFlow<Bitmap?>(null)
    val customBitmap: StateFlow<Bitmap?> = _customBitmap.asStateFlow()

    init {
        // Initial test translation
        translateCurrentScreen()
    }

    fun selectPreset(index: Int) {
        if (index in presetScenes.indices) {
            _selectedPresetIndex.value = index
            val scene = presetScenes[index]
            _currentEra.value = scene.era
            UserPreferencesManager.setGameTitle(scene.title)
            translatePresetScene(scene)
        }
    }

    fun setGeminiApiKey(key: String) {
        UserPreferencesManager.setGeminiApiKey(key)
    }

    fun setDeepseekApiKey(key: String) {
        UserPreferencesManager.setDeepseekApiKey(key)
    }

    fun setSelectedProvider(provider: TranslationProvider) {
        UserPreferencesManager.setSelectedProvider(provider)
        translateCurrentScreen()
    }

    fun setGameTitle(title: String) {
        UserPreferencesManager.setGameTitle(title)
    }

    fun setPixelEnhanceEnabled(enabled: Boolean) {
        _isPixelEnhanceEnabled.value = enabled
        translateCurrentScreen()
    }

    fun setAutoTranslateEnabled(enabled: Boolean) {
        UserPreferencesManager.setAutoTranslateEnabled(enabled)
    }

    fun setAutoTranslateSpeed(speed: com.example.data.model.AutoTranslateSpeed) {
        UserPreferencesManager.setAutoTranslateSpeed(speed)
    }

    fun updatePronouns(self: String, toOther: String, tone: String) {
        _pronounConfig.value = CharacterPronounConfig(self, toOther, tone)
    }

    fun setCustomPromptNotes(notes: String) {
        _customPromptNotes.value = notes
    }

    fun setOverlayOpacity(opacity: Float) {
        _overlayOpacity.value = opacity
    }

    fun setOverlayFontSize(size: Int) {
        _overlayFontSize.value = size
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun loadCustomImage(context: Context, uri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            _customBitmap.value = bitmap
            translateCustomBitmap(bitmap)
        } catch (e: Exception) {
            _errorMessage.value = "ไม่สามารถโหลดรูปภาพ: ${e.message}"
        }
    }

    fun translateCurrentScreen() {
        val custom = _customBitmap.value
        if (custom != null) {
            translateCustomBitmap(custom)
        } else {
            val currentScene = presetScenes[_selectedPresetIndex.value]
            translatePresetScene(currentScene)
        }
    }

    private fun translatePresetScene(scene: GamePresetScene) {
        viewModelScope.launch {
            _isTranslating.value = true
            _errorMessage.value = null

            val bitmap = createSyntheticGameFrame(scene)

            val result = repository.translateGameScreen(
                bitmap = bitmap,
                gameTitle = gameTitle.value.ifBlank { scene.title },
                era = scene.era,
                pronounConfig = _pronounConfig.value,
                customPromptNotes = _customPromptNotes.value,
                isPixelEnhanceEnabled = _isPixelEnhanceEnabled.value,
                provider = selectedProvider.value,
                sampleTextFallback = scene.sampleTextEnglish
            )

            result.onSuccess { payload ->
                _lastResult.value = payload
            }.onFailure { e ->
                _errorMessage.value = "การแปลล้มเหลว: ${e.message}"
            }
            _isTranslating.value = false
        }
    }

    private fun translateCustomBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _isTranslating.value = true
            _errorMessage.value = null

            val currentTitle = gameTitle.value.ifBlank { "Screen Game" }
            val result = repository.translateGameScreen(
                bitmap = bitmap,
                gameTitle = currentTitle,
                era = _currentEra.value,
                pronounConfig = _pronounConfig.value,
                customPromptNotes = _customPromptNotes.value,
                isPixelEnhanceEnabled = _isPixelEnhanceEnabled.value,
                provider = selectedProvider.value
            )

            result.onSuccess { payload ->
                _lastResult.value = payload
            }.onFailure { e ->
                _errorMessage.value = "การแปลล้มเหลว: ${e.message}"
            }
            _isTranslating.value = false
        }
    }

    val isScreenCaptureReady = com.example.util.ScreenCaptureManager.isReady
    val hasTargetFrame = com.example.util.TargetFrameManager.targetFrame

    fun startOverlayService(context: Context) {
        try {
            val serviceIntent = Intent(context, GameOverlayService::class.java).apply {
                action = GameOverlayService.ACTION_START
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            _errorMessage.value = "เกิดข้อผิดพลาดในการเปิดใช้งานปุ่มลอย: ${e.message}"
        }
    }

    fun toggleOverlayService(context: Context, onRequestScreenCapture: (() -> Unit)? = null) {
        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

        if (!hasPermission) {
            _errorMessage.value = "กรุณาเปิดสิทธิ์ 'แสดงทับแอพอื่น' (Display over other apps) ในการตั้งค่า"
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                _errorMessage.value = "ไม่สามารถเปิดหน้าตั้งค่าการอนุญาตได้: ${e.message}"
            }
            return
        }

        try {
            if (GameOverlayService.isServiceRunning) {
                val serviceIntent = Intent(context, GameOverlayService::class.java).apply {
                    action = GameOverlayService.ACTION_STOP
                }
                context.stopService(serviceIntent)
            } else {
                if (!com.example.util.ScreenCaptureManager.hasProjection && onRequestScreenCapture != null) {
                    onRequestScreenCapture()
                } else {
                    startOverlayService(context)
                }
            }
        } catch (e: Exception) {
            _errorMessage.value = "เกิดข้อผิดพลาดในการเปิดใช้งานปุ่มลอย: ${e.message}"
        }
    }

    // Room Database Operations
    fun toggleFavorite(record: TranslationRecord) {
        viewModelScope.launch {
            repository.toggleFavorite(record)
        }
    }

    fun deleteHistory(record: TranslationRecord) {
        viewModelScope.launch {
            repository.deleteHistory(record)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun addGlossary(termEnglish: String, termThai: String, gameTitle: String, category: String, note: String) {
        viewModelScope.launch {
            val entry = GlossaryEntry(
                termEnglish = termEnglish.trim(),
                termThai = termThai.trim(),
                gameTitle = gameTitle.trim(),
                category = category,
                note = note.ifBlank { null }
            )
            repository.addGlossary(entry)
        }
    }

    fun addGlossaryEntry(termEnglish: String, termThai: String, category: String, note: String?) {
        viewModelScope.launch {
            val entry = GlossaryEntry(
                termEnglish = termEnglish.trim(),
                termThai = termThai.trim(),
                gameTitle = gameTitle.value.ifBlank { "General" },
                category = category,
                note = note
            )
            repository.addGlossary(entry)
        }
    }

    fun deleteGlossary(entry: GlossaryEntry) {
        viewModelScope.launch {
            repository.deleteGlossary(entry)
        }
    }

    fun deleteGlossaryEntry(entry: GlossaryEntry) {
        deleteGlossary(entry)
    }

    /**
     * Synthesizes an illustrative game screenshot bitmap for in-app demonstration
     */
    private fun createSyntheticGameFrame(scene: GamePresetScene): Bitmap {
        val width = 800
        val height = 450
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            color = when (scene.era) {
                GameEra.RETRO_PIXEL_GBA -> Color.rgb(24, 72, 40)
                GameEra.FANTASY_MEDIEVAL -> Color.rgb(20, 16, 24)
                GameEra.CYBERPUNK_SCIFI -> Color.rgb(15, 23, 42)
                GameEra.ANIME_JRPG -> Color.rgb(30, 41, 59)
                GameEra.MODERN_MILITARY -> Color.rgb(35, 45, 35)
                GameEra.WUXIA_CHINESE -> Color.rgb(40, 25, 25)
                GameEra.SURVIVAL_HORROR -> Color.rgb(10, 10, 15)
            }
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw title HUD
        val hudPaint = Paint().apply {
            color = Color.rgb(220, 220, 240)
            textSize = 28f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("🎮 ${scene.title}", 40f, 55f, hudPaint)

        // Draw subtitle / quest tracker
        val questPaint = Paint().apply {
            color = Color.rgb(250, 204, 21)
            textSize = 20f
            isAntiAlias = true
        }
        canvas.drawText("QUEST: Main Scenario Dialogue Active", 40f, 90f, questPaint)

        // Draw dialogue box
        val boxPaint = Paint().apply {
            color = Color.argb(220, 15, 23, 42)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = when (scene.era) {
                GameEra.RETRO_PIXEL_GBA -> Color.rgb(74, 222, 128)
                GameEra.FANTASY_MEDIEVAL -> Color.rgb(251, 191, 36)
                GameEra.CYBERPUNK_SCIFI -> Color.rgb(6, 182, 212)
                else -> Color.rgb(129, 140, 248)
            }
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val rect = RectF(40f, 270f, (width - 40).toFloat(), (height - 30).toFloat())
        canvas.drawRoundRect(rect, 16f, 16f, boxPaint)
        canvas.drawRoundRect(rect, 16f, 16f, borderPaint)

        // Speaker name
        val speakerPaint = Paint().apply {
            color = borderPaint.color
            textSize = 24f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(scene.speaker, 65f, 312f, speakerPaint)

        // Dialogue text
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            isAntiAlias = true
        }

        val words = scene.sampleTextEnglish.split(" ")
        val lines = mutableListOf<String>()
        var curLine = StringBuilder()
        for (w in words) {
            if (curLine.length + w.length > 55) {
                lines.add(curLine.toString())
                curLine = StringBuilder(w).append(" ")
            } else {
                curLine.append(w).append(" ")
            }
        }
        if (curLine.isNotEmpty()) lines.add(curLine.toString())

        var yOffset = 350f
        for (line in lines.take(3)) {
            canvas.drawText(line, 65f, yOffset, textPaint)
            yOffset += 30f
        }

        return bitmap
    }
}
