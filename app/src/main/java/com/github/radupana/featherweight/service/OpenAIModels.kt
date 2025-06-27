package com.github.radupana.featherweight.service

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    @SerialName("max_tokens") val maxTokens: Int,
    val temperature: Double,
    @SerialName("response_format") val responseFormat: ResponseFormat? = null
)

@Serializable
data class OpenAIMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

@Serializable
data class ResponseFormat(
    val type: String = "json_object"
)

@Serializable
data class OpenAIResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage?
)

@Serializable
data class Choice(
    val index: Int,
    val message: OpenAIMessage,
    @SerialName("finish_reason") val finishReason: String?
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int
)

@Serializable
data class OpenAIError(
    val error: ErrorDetails
)

@Serializable
data class ErrorDetails(
    val message: String,
    val type: String,
    val code: String?
)