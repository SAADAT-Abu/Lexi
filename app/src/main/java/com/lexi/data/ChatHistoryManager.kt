package com.lexi.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lexi.model.ChatSession
import com.lexi.model.ChatMessage
import com.lexi.ui.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ChatHistoryManager(private val context: Context) {
    private val gson = Gson()
    private val CHAT_SESSIONS_KEY = stringPreferencesKey("chat_sessions")
    
    fun getAllSessions(): Flow<List<ChatSession>> {
        return context.dataStore.data.map { preferences ->
            val sessionsJson = preferences[CHAT_SESSIONS_KEY] ?: ""
            if (sessionsJson.isBlank()) {
                emptyList()
            } else {
                try {
                    val type = object : TypeToken<List<ChatSession>>() {}.type
                    gson.fromJson<List<ChatSession>>(sessionsJson, type)
                        .sortedByDescending { it.updatedAt }
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }
    
    suspend fun createSession(
        title: String,
        modelId: String,
        modelName: String,
        initialMessage: ChatMessage? = null
    ): ChatSession {
        val newSession = ChatSession(
            title = title,
            modelId = modelId,
            modelName = modelName,
            messages = initialMessage?.let { listOf(it) } ?: emptyList()
        )
        
        val sessions = getAllSessions().first().toMutableList()
        sessions.add(0, newSession)
        saveSessions(sessions)
        
        return newSession
    }
    
    suspend fun updateSession(sessionId: String, messages: List<ChatMessage>) {
        val sessions = getAllSessions().first().toMutableList()
        val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
        
        if (sessionIndex != -1) {
            val updatedSession = sessions[sessionIndex].copy(
                messages = messages,
                updatedAt = System.currentTimeMillis()
            )
            sessions[sessionIndex] = updatedSession
            
            // Move to top
            sessions.removeAt(sessionIndex)
            sessions.add(0, updatedSession)
            
            saveSessions(sessions)
        }
    }
    
    suspend fun deleteSession(sessionId: String) {
        val sessions = getAllSessions().first().toMutableList()
        sessions.removeAll { it.id == sessionId }
        saveSessions(sessions)
    }
    
    suspend fun getSession(sessionId: String): ChatSession? {
        return getAllSessions().first().find { it.id == sessionId }
    }
    
    private suspend fun saveSessions(sessions: List<ChatSession>) {
        context.dataStore.edit { preferences ->
            preferences[CHAT_SESSIONS_KEY] = gson.toJson(sessions)
        }
    }
}