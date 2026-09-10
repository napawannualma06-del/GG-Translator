package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface DeepSeekApiService {
    @GET("models")
    suspend fun listModels(
        @Header("Authorization") authorization: String
    ): DeepSeekListModelsResponse

    @POST("chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body request: DeepSeekChatRequest
    ): DeepSeekChatResponse
}

@JsonClass(generateAdapter = true)
data class DeepSeekListModelsResponse(
    @Json(name = "data")
    val data: List<DeepSeekModelInfo>? = null
)

@JsonClass(generateAdapter = true)
data class DeepSeekModelInfo(
    @Json(name = "id")
    val id: String
)

@JsonClass(generateAdapter = true)
data class DeepSeekChatRequest(
    @Json(name = "model")
    val model: String = "deepseek-chat",
    @Json(name = "messages")
    val messages: List<DeepSeekChatMessage>,
    @Json(name = "response_format")
    val responseFormat: DeepSeekResponseFormat? = DeepSeekResponseFormat(type = "json_object"),
    @Json(name = "temperature")
    val temperature: Float = 0.3f
)

@JsonClass(generateAdapter = true)
data class DeepSeekChatMessage(
    @Json(name = "role")
    val role: String,
    @Json(name = "content")
    val content: String
)

@JsonClass(generateAdapter = true)
data class DeepSeekResponseFormat(
    @Json(name = "type")
    val type: String = "json_object"
)

@JsonClass(generateAdapter = true)
data class DeepSeekChatResponse(
    @Json(name = "id")
    val id: String?,
    @Json(name = "choices")
    val choices: List<DeepSeekChoice>?
)

@JsonClass(generateAdapter = true)
data class DeepSeekChoice(
    @Json(name = "index")
    val index: Int?,
    @Json(name = "message")
    val message: DeepSeekChatMessage?
)
