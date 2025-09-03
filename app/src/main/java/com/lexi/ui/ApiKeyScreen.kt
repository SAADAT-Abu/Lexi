package com.lexi.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lexi.R
import com.lexi.api.ApiClient
import com.lexi.api.OpenRouterClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiKeyScreen(
    onApiKeySaved: () -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val API_KEY = stringPreferencesKey("api_key")
    
    LaunchedEffect(Unit) {
        val preferences = context.dataStore.data.first()
        val savedApiKey = preferences[API_KEY] ?: ""
        if (savedApiKey.isNotEmpty()) {
            apiKey = savedApiKey
            OpenRouterClient.setApiKey(savedApiKey)
            onApiKeySaved()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Lexi",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Text(
            text = stringResource(R.string.enter_api_key),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp),
            textAlign = TextAlign.Center
        )
        
        OutlinedTextField(
            value = apiKey,
            onValueChange = { 
                apiKey = it
                error = ""
            },
            label = { Text(stringResource(R.string.api_key_hint)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = !isLoading,
            isError = error.isNotEmpty()
        )
        
        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        Button(
            onClick = {
                if (apiKey.isEmpty()) {
                    error = context.getString(R.string.error_api_key_required)
                    return@Button
                }
                
                scope.launch {
                    isLoading = true
                    error = ""
                    try {
                        android.util.Log.d("ApiKeyScreen", "Starting API key validation...")
                        
                        // Set the API key
                        OpenRouterClient.setApiKey(apiKey)
                        android.util.Log.d("ApiKeyScreen", "API key set, making request to validate...")
                        
                        // Make the actual API request
                        val response = ApiClient.openRouterApi.getModels()
                        
                        android.util.Log.d("ApiKeyScreen", "Response received: ${response.code()}")
                        android.util.Log.d("ApiKeyScreen", "Response message: ${response.message()}")
                        android.util.Log.d("ApiKeyScreen", "Response successful: ${response.isSuccessful}")
                        
                        if (response.isSuccessful) {
                            val body = response.body()
                            android.util.Log.d("ApiKeyScreen", "Response body: $body")
                            
                            val models = body?.data
                            android.util.Log.d("ApiKeyScreen", "Found ${models?.size ?: 0} models")
                            
                            if (models != null && models.isNotEmpty()) {
                                // API key is valid, save it
                                context.dataStore.edit { preferences ->
                                    preferences[API_KEY] = apiKey
                                }
                                android.util.Log.d("ApiKeyScreen", "API key validated and saved!")
                                onApiKeySaved()
                            } else {
                                error = "API key valid but no models found"
                            }
                        } else {
                            val errorBody = response.errorBody()?.string()
                            android.util.Log.e("ApiKeyScreen", "API error: $errorBody")
                            error = "Invalid API key - HTTP ${response.code()}: ${response.message()}"
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ApiKeyScreen", "Exception during API validation", e)
                        error = "Failed to validate API key: ${e.javaClass.simpleName} - ${e.message}"
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(stringResource(R.string.save))
        }
    }
}