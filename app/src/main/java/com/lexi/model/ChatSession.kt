package com.lexi.model

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

data class ChatSession(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("title") val title: String,
    @SerializedName("modelId") val modelId: String,
    @SerializedName("modelName") val modelName: String,
    @SerializedName("messages") val messages: List<ChatMessage> = emptyList(),
    @SerializedName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt") val updatedAt: Long = System.currentTimeMillis()
) {
    fun getFormattedDate(): String {
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return formatter.format(Date(updatedAt))
    }
    
    fun getFormattedTime(): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        return formatter.format(Date(updatedAt))
    }
    
    fun getPreview(): String {
        return messages.lastOrNull()?.content?.take(50)?.plus("...") ?: "New conversation"
    }
}