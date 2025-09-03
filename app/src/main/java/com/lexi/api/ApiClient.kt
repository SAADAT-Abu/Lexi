package com.lexi.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://openrouter.ai/api/v1/"
    
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val apiKey = OpenRouterClient.getApiKey()
        
        if (apiKey.isEmpty()) {
            throw IllegalStateException("OpenRouter API key is not set")
        }
        
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://github.com/lexi-app")
            .header("X-Title", "Lexi")
            .header("Content-Type", "application/json")
            .build()
        
        chain.proceed(newRequest)
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val openRouterApi: OpenRouterApi = retrofit.create(OpenRouterApi::class.java)
}