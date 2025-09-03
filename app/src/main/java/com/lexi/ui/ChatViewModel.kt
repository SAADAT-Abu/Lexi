package com.lexi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexi.api.ApiClient
import com.lexi.model.ChatMessage
import com.lexi.model.ChatRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String = ""
)

class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private var currentModelId = ""
    
    fun setModel(modelId: String) {
        currentModelId = modelId
    }
    
    fun loadMessages(messages: List<ChatMessage>) {
        _uiState.value = _uiState.value.copy(
            messages = messages,
            isLoading = false,
            error = ""
        )
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            messages = emptyList(),
            isLoading = false,
            error = ""
        )
    }
    
    fun sendMessage(content: String) {
        val userMessage = ChatMessage(role = "user", content = content)
        
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            isLoading = true,
            error = ""
        )
        
        viewModelScope.launch {
            try {
                val allMessages = _uiState.value.messages
                val request = ChatRequest(
                    model = currentModelId,
                    messages = allMessages
                )
                
                val response = ApiClient.openRouterApi.sendMessage(request)
                
                if (response.isSuccessful) {
                    val chatResponse = response.body()
                    val assistantMessage = chatResponse?.choices?.firstOrNull()?.message
                    
                    if (assistantMessage != null) {
                        _uiState.value = _uiState.value.copy(
                            messages = _uiState.value.messages + assistantMessage,
                            isLoading = false
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "No response from AI",
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to send message: ${response.message()}",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace() // Log the full stack trace
                _uiState.value = _uiState.value.copy(
                    error = "Network error: ${e.message ?: "Please check your connection and API key"}",
                    isLoading = false
                )
            }
        }
    }
}