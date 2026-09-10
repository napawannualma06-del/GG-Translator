package com.example.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApiService {
    @GET("v1beta/models")
    suspend fun listModels(
        @Query("key") apiKey: String
    ): GeminiListModelsResponse

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContentWithModel(
        @retrofit2.http.Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse

    @POST("v1beta/models/gemini-3.6-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiGenerateResponse
}
