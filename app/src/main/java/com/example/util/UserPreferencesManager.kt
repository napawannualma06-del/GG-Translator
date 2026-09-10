package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.data.model.AutoTranslateSpeed
import com.example.data.model.TranslationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserPreferencesManager {
    private const val PREF_NAME = "game_translator_user_prefs"
    private const val KEY_GEMINI_KEY = "gemini_api_key"
    private const val KEY_DEEPSEEK_KEY = "deepseek_api_key"
    private const val KEY_SELECTED_PROVIDER = "selected_provider"
    private const val KEY_GAME_TITLE = "custom_game_title"
    private const val KEY_GAME_ERA = "custom_game_era"
    private const val KEY_AUTO_TRANSLATE = "is_auto_translate_enabled"
    private const val KEY_AUTO_TRANSLATE_SPEED = "auto_translate_speed_mode"
    private const val KEY_SELECTED_GEMINI_MODEL = "selected_gemini_model"
    private const val KEY_SELECTED_DEEPSEEK_MODEL = "selected_deepseek_model"

    private var prefs: SharedPreferences? = null

    private val _isAutoTranslateEnabled = MutableStateFlow(false)
    val isAutoTranslateEnabled: StateFlow<Boolean> = _isAutoTranslateEnabled.asStateFlow()

    private val _autoTranslateSpeed = MutableStateFlow(AutoTranslateSpeed.FAST)
    val autoTranslateSpeed: StateFlow<AutoTranslateSpeed> = _autoTranslateSpeed.asStateFlow()

    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _deepseekApiKey = MutableStateFlow("")
    val deepseekApiKey: StateFlow<String> = _deepseekApiKey.asStateFlow()

    private val _selectedProvider = MutableStateFlow(TranslationProvider.GEMINI)
    val selectedProvider: StateFlow<TranslationProvider> = _selectedProvider.asStateFlow()

    private val _selectedGeminiModel = MutableStateFlow("gemini-3.6-flash")
    val selectedGeminiModel: StateFlow<String> = _selectedGeminiModel.asStateFlow()

    private val _selectedDeepseekModel = MutableStateFlow("deepseek-chat")
    val selectedDeepseekModel: StateFlow<String> = _selectedDeepseekModel.asStateFlow()

    private val _gameTitle = MutableStateFlow("Elden Ring")
    val gameTitle: StateFlow<String> = _gameTitle.asStateFlow()

    private val _gameEra = MutableStateFlow(com.example.data.model.GameEra.RETRO_PIXEL_GBA)
    val gameEra: StateFlow<com.example.data.model.GameEra> = _gameEra.asStateFlow()

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs = sp

        val savedGemini = sp.getString(KEY_GEMINI_KEY, "") ?: ""
        val initialGemini = savedGemini.ifBlank {
            val buildKey = BuildConfig.GEMINI_API_KEY
            if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
        }
        _geminiApiKey.value = initialGemini

        val savedDeepseek = sp.getString(KEY_DEEPSEEK_KEY, "") ?: ""
        _deepseekApiKey.value = savedDeepseek

        val savedProvider = sp.getString(KEY_SELECTED_PROVIDER, TranslationProvider.GEMINI.id)
        _selectedProvider.value = if (savedProvider == TranslationProvider.DEEPSEEK.id) {
            TranslationProvider.DEEPSEEK
        } else {
            TranslationProvider.GEMINI
        }

        val savedGeminiModel = sp.getString(KEY_SELECTED_GEMINI_MODEL, "gemini-3.6-flash") ?: "gemini-3.6-flash"
        _selectedGeminiModel.value = savedGeminiModel

        val savedDeepseekModel = sp.getString(KEY_SELECTED_DEEPSEEK_MODEL, "deepseek-chat") ?: "deepseek-chat"
        _selectedDeepseekModel.value = savedDeepseekModel

        val savedTitle = sp.getString(KEY_GAME_TITLE, "Elden Ring") ?: "Elden Ring"
        _gameTitle.value = savedTitle

        val savedEraName = sp.getString(KEY_GAME_ERA, com.example.data.model.GameEra.RETRO_PIXEL_GBA.name)
        _gameEra.value = try {
            com.example.data.model.GameEra.valueOf(savedEraName ?: "RETRO_PIXEL_GBA")
        } catch (e: Exception) {
            com.example.data.model.GameEra.RETRO_PIXEL_GBA
        }

        _isAutoTranslateEnabled.value = sp.getBoolean(KEY_AUTO_TRANSLATE, false)

        val savedSpeedId = sp.getString(KEY_AUTO_TRANSLATE_SPEED, AutoTranslateSpeed.FAST.id)
        _autoTranslateSpeed.value = AutoTranslateSpeed.fromId(savedSpeedId)
    }

    fun setAutoTranslateEnabled(enabled: Boolean) {
        _isAutoTranslateEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_AUTO_TRANSLATE, enabled)?.apply()
    }

    fun setAutoTranslateSpeed(speed: AutoTranslateSpeed) {
        _autoTranslateSpeed.value = speed
        prefs?.edit()?.putString(KEY_AUTO_TRANSLATE_SPEED, speed.id)?.apply()
    }

    fun setGeminiApiKey(key: String) {
        _geminiApiKey.value = key
        prefs?.edit()?.putString(KEY_GEMINI_KEY, key)?.apply()
    }

    fun setDeepseekApiKey(key: String) {
        _deepseekApiKey.value = key
        prefs?.edit()?.putString(KEY_DEEPSEEK_KEY, key)?.apply()
    }

    fun setSelectedProvider(provider: TranslationProvider) {
        _selectedProvider.value = provider
        prefs?.edit()?.putString(KEY_SELECTED_PROVIDER, provider.id)?.apply()
    }

    fun setSelectedGeminiModel(model: String) {
        _selectedGeminiModel.value = model
        prefs?.edit()?.putString(KEY_SELECTED_GEMINI_MODEL, model)?.apply()
    }

    fun setSelectedDeepseekModel(model: String) {
        _selectedDeepseekModel.value = model
        prefs?.edit()?.putString(KEY_SELECTED_DEEPSEEK_MODEL, model)?.apply()
    }

    fun setGameTitle(title: String) {
        _gameTitle.value = title
        prefs?.edit()?.putString(KEY_GAME_TITLE, title)?.apply()
    }

    fun setGameEra(era: com.example.data.model.GameEra) {
        _gameEra.value = era
        prefs?.edit()?.putString(KEY_GAME_ERA, era.name)?.apply()
    }

    fun getEffectiveGeminiKey(): String {
        val custom = _geminiApiKey.value.trim()
        if (custom.isNotBlank()) return custom
        val buildKey = BuildConfig.GEMINI_API_KEY
        return if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") buildKey else ""
    }

    fun getEffectiveDeepseekKey(): String {
        return _deepseekApiKey.value.trim()
    }
}
