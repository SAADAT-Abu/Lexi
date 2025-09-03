package com.lexi.model

import com.google.gson.annotations.SerializedName

data class Model(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("pricing") val pricing: Pricing?,
    @SerializedName("top_provider") val topProvider: Provider?,
    @SerializedName("context_length") val contextLength: Int?
)

data class Pricing(
    @SerializedName("prompt") val prompt: String?,
    @SerializedName("completion") val completion: String?
)

data class Provider(
    @SerializedName("name") val name: String?
)

data class ModelsResponse(
    @SerializedName("data") val data: List<Model>
)

data class ChatMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class ChatRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<ChatMessage>,
    @SerializedName("stream") val stream: Boolean = false
)

data class ChatChoice(
    @SerializedName("message") val message: ChatMessage,
    @SerializedName("finish_reason") val finishReason: String?
)

data class ChatResponse(
    @SerializedName("choices") val choices: List<ChatChoice>,
    @SerializedName("usage") val usage: Usage?
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int?,
    @SerializedName("completion_tokens") val completionTokens: Int?,
    @SerializedName("total_tokens") val totalTokens: Int?
)