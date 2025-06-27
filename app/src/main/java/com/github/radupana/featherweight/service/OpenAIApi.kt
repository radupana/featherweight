package com.github.radupana.featherweight.service

import retrofit2.Response
import retrofit2.http.*

interface OpenAIApi {
    @POST("v1/chat/completions")
    @Headers("Content-Type: application/json")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIRequest
    ): Response<OpenAIResponse>
}