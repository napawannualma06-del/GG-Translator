package com.example.data.model

enum class TranslationProvider(val id: String, val displayName: String, val modelName: String) {
    GEMINI("gemini", "Google Gemini 3.6 Flash", "gemini-3.6-flash"),
    DEEPSEEK("deepseek", "DeepSeek Chat (V3)", "deepseek-chat")
}
