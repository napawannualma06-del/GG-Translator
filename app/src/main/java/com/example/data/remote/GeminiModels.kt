package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    @Json(name = "contents")
    val contents: List<GeminiContent>,
    @Json(name = "systemInstruction")
    val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig")
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts")
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text")
    val text: String? = null,
    @Json(name = "inlineData")
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @Json(name = "mimeType")
    val mimeType: String,
    @Json(name = "data")
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "responseMimeType")
    val responseMimeType: String? = "application/json",
    @Json(name = "temperature")
    val temperature: Float? = 0.2f,
    @Json(name = "maxOutputTokens")
    val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateResponse(
    @Json(name = "candidates")
    val candidates: List<GeminiCandidate>? = null,
    @Json(name = "usageMetadata")
    val usageMetadata: GeminiUsageMetadata? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content")
    val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiUsageMetadata(
    @Json(name = "promptTokenCount")
    val promptTokenCount: Int? = null,
    @Json(name = "candidatesTokenCount")
    val candidatesTokenCount: Int? = null,
    @Json(name = "totalTokenCount")
    val totalTokenCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiListModelsResponse(
    @Json(name = "models")
    val models: List<GeminiModelInfo>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiModelInfo(
    @Json(name = "name")
    val name: String,
    @Json(name = "displayName")
    val displayName: String? = null,
    @Json(name = "description")
    val description: String? = null,
    @Json(name = "supportedGenerationMethods")
    val supportedGenerationMethods: List<String>? = null
)


