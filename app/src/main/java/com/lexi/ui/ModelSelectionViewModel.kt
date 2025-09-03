package com.lexi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexi.api.ApiClient
import com.lexi.api.OpenRouterClient
import com.lexi.model.Model
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ModelSelectionUiState(
    val models: List<Model> = emptyList(),
    val isLoading: Boolean = false,
    val error: String = ""
)

class ModelSelectionViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ModelSelectionUiState())
    val uiState: StateFlow<ModelSelectionUiState> = _uiState.asStateFlow()
    
    fun loadModels() {
        android.util.Log.d("ModelViewModel", "loadModels() called")
        viewModelScope.launch {
            android.util.Log.d("ModelViewModel", "Starting model loading...")
            _uiState.value = _uiState.value.copy(isLoading = true, error = "")
            
            // Add a small delay to ensure API key is set
            kotlinx.coroutines.delay(100)
            
            try {
                val apiKey = OpenRouterClient.getApiKey()
                android.util.Log.d("ModelViewModel", "API key length: ${apiKey.length}")
                if (apiKey.isEmpty()) {
                    android.util.Log.e("ModelViewModel", "No API key found!")
                    _uiState.value = _uiState.value.copy(
                        error = "No API key found. Please restart the app.",
                        isLoading = false
                    )
                    return@launch
                }
                
                android.util.Log.d("ModelViewModel", "Making API request to get models...")
                val response = ApiClient.openRouterApi.getModels()
                android.util.Log.d("ModelViewModel", "API response: code=${response.code()}, successful=${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    android.util.Log.d("ModelViewModel", "Response body: $responseBody")
                    val models = responseBody?.data ?: emptyList()
                    android.util.Log.d("ModelViewModel", "Total models found: ${models.size}")
                    
                    val freeModels = models.filter { model ->
                        model.pricing?.prompt == "0" && model.pricing.completion == "0"
                    }
                    android.util.Log.d("ModelViewModel", "Free models found: ${freeModels.size}")
                    
                    if (freeModels.isEmpty()) {
                        // If no free models found, show all models
                        val limitedModels = models.take(10)
                        android.util.Log.d("ModelViewModel", "No free models, showing first ${limitedModels.size} models")
                        _uiState.value = _uiState.value.copy(
                            models = limitedModels,
                            isLoading = false
                        )
                    } else {
                        android.util.Log.d("ModelViewModel", "Showing ${freeModels.size} free models")
                        _uiState.value = _uiState.value.copy(
                            models = freeModels,
                            isLoading = false
                        )
                    }
                    android.util.Log.d("ModelViewModel", "Model loading completed successfully")
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("ModelViewModel", "API error: $errorBody")
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load models: HTTP ${response.code()} - ${response.message()}",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ModelViewModel", "Exception during model loading", e)
                e.printStackTrace() // Log the full stack trace
                _uiState.value = _uiState.value.copy(
                    error = "Network error: ${e.message ?: "Please check your connection and API key"}",
                    isLoading = false
                )
            }
        }
    }
}