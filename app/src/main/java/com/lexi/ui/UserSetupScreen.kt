package com.lexi.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lexi.api.OpenRouterClient
import com.lexi.api.ApiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSetupScreen(
    onSetupComplete: () -> Unit
) {
    var userName by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    val USER_NAME = stringPreferencesKey("user_name")
    val API_KEY = stringPreferencesKey("api_key")
    val IS_FIRST_RUN = stringPreferencesKey("is_first_run")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Setup Title
        Text(
            text = "Setup Your Profile",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Let's personalize your Lexi experience",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Name Input
        OutlinedTextField(
            value = userName,
            onValueChange = { 
                userName = it
                error = ""
            },
            label = { Text("Your Name") },
            placeholder = { Text("Enter your name") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Name"
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // API Key Input
        OutlinedTextField(
            value = apiKey,
            onValueChange = { 
                apiKey = it
                error = ""
            },
            label = { Text("OpenRouter API Key") },
            placeholder = { Text("sk-or-...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "API Key"
                )
            },
            trailingIcon = {
                IconButton(onClick = { showApiKey = !showApiKey }) {
                    Icon(
                        imageVector = if (showApiKey) Icons.Default.Lock else Icons.Default.Lock,
                        contentDescription = if (showApiKey) "Hide API Key" else "Show API Key"
                    )
                }
            },
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // API Key Help Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "💡 Need help getting your API Key?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Visit openrouter.ai/keys and create a free account. Your API key should start with 'sk-or-'",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        
        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Continue Button
        Button(
            onClick = {
                if (userName.isBlank()) {
                    error = "Please enter your name"
                    return@Button
                }
                if (apiKey.isBlank()) {
                    error = "Please enter your OpenRouter API key"
                    return@Button
                }
                if (!apiKey.startsWith("sk-or-")) {
                    error = "Invalid API key format. OpenRouter API keys start with 'sk-or-'"
                    return@Button
                }
                
                scope.launch {
                    isLoading = true
                    error = ""
                    try {
                        // Validate API key by making a test request
                        OpenRouterClient.setApiKey(apiKey)
                        val response = ApiClient.openRouterApi.getModels()
                        
                        if (response.isSuccessful && response.body()?.data?.isNotEmpty() == true) {
                            // Save user data
                            context.dataStore.edit { preferences ->
                                preferences[USER_NAME] = userName
                                preferences[API_KEY] = apiKey
                                preferences[IS_FIRST_RUN] = "false"
                            }
                            onSetupComplete()
                        } else {
                            error = "Invalid API key. Please check your key and try again."
                        }
                    } catch (e: Exception) {
                        error = "Failed to validate API key: ${e.message ?: "Please check your internet connection and try again."}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && userName.isNotBlank() && apiKey.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Validating...")
            } else {
                Text(
                    text = "Continue to Model Selection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}