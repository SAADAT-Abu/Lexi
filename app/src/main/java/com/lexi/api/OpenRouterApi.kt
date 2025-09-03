package com.lexi.api

import com.lexi.model.ChatRequest
import com.lexi.model.ChatResponse
import com.lexi.model.ModelsResponse
import retrofit2.Response
import retrofit2.http.*

interface OpenRouterApi {
    @GET("models")
    suspend fun getModels(): Response<ModelsResponse>
    
    @POST("chat/completions")
    suspend fun sendMessage(
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

object OpenRouterClient {
    private var apiKey: String = ""
    
    fun setApiKey(key: String) {
        apiKey = key
    }
    
    fun getApiKey(): String = apiKey
}