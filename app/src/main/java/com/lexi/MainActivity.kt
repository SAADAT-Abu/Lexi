package com.lexi

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.net.URLEncoder
import java.net.URLDecoder
import com.lexi.navigation.Screen
import com.lexi.ui.ApiKeyScreen
import com.lexi.ui.ChatScreen
import com.lexi.ui.ModelSelectionScreen
import com.lexi.ui.WelcomeScreen
import com.lexi.ui.UserSetupScreen
import com.lexi.ui.SettingsScreen
import com.lexi.ui.dataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.compose.runtime.*
import com.lexi.ui.theme.LexiTheme
import com.lexi.api.OpenRouterClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LexiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    
                    // Determine start destination based on first run
                    var startDestination by remember { mutableStateOf<String?>(null) }
                    
                    LaunchedEffect(Unit) {
                        val preferences = dataStore.data.first()
                        val isFirstRun = preferences[stringPreferencesKey("is_first_run")] != "false"
                        val apiKey = preferences[stringPreferencesKey("api_key")] ?: ""
                        val hasApiKey = apiKey.isNotEmpty()
                        val hasDefaultModel = preferences[stringPreferencesKey("default_model")]?.isNotEmpty() == true
                        
                        // Load API key into OpenRouterClient if it exists
                        if (hasApiKey) {
                            OpenRouterClient.setApiKey(apiKey)
                            Log.d("MainActivity", "API key loaded into OpenRouterClient")
                        }
                        
                        startDestination = when {
                            isFirstRun -> Screen.Welcome.route
                            !hasApiKey -> Screen.UserSetup.route
                            !hasDefaultModel -> Screen.ModelSelection.route
                            else -> {
                                val model = preferences[stringPreferencesKey("default_model")]
                                if (!model.isNullOrEmpty()) {
                                    try {
                                        Screen.Chat.route + "/" + URLEncoder.encode(model, "UTF-8")
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Error encoding model ID: $model", e)
                                        Screen.ModelSelection.route
                                    }
                                } else {
                                    Screen.ModelSelection.route
                                }
                            }
                        }
                    }
                    
                    startDestination?.let { destination ->
                        NavHost(
                            navController = navController,
                            startDestination = destination
                        ) {
                        composable(Screen.Welcome.route) {
                            WelcomeScreen(
                                onGetStarted = {
                                    navController.navigate(Screen.UserSetup.route) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        composable(Screen.UserSetup.route) {
                            UserSetupScreen(
                                onSetupComplete = {
                                    navController.navigate(Screen.ModelSelection.route) {
                                        popUpTo(Screen.UserSetup.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        composable(Screen.ApiKey.route) {
                            ApiKeyScreen(
                                onApiKeySaved = {
                                    navController.navigate(Screen.ModelSelection.route) {
                                        popUpTo(Screen.ApiKey.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        composable(Screen.ModelSelection.route) {
                            ModelSelectionScreen(
                                onModelSelected = { modelId ->
                                    try {
                                        val encodedModelId = URLEncoder.encode(modelId, "UTF-8")
                                        navController.navigate("${Screen.Chat.route}/$encodedModelId")
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Navigation error", e)
                                        e.printStackTrace()
                                    }
                                }
                            )
                        }
                        
                        composable(
                            route = "${Screen.Chat.route}/{modelId}",
                            arguments = listOf(androidx.navigation.navArgument("modelId") { type = androidx.navigation.NavType.StringType })
                        ) { backStackEntry ->
                            val encodedModelId = backStackEntry.arguments?.getString("modelId") ?: ""
                            val modelId = try {
                                URLDecoder.decode(encodedModelId, "UTF-8")
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error decoding model ID: $encodedModelId", e)
                                encodedModelId
                            }
                            ChatScreen(
                                modelId = modelId,
                                onBackPressed = {
                                    navController.popBackStack()
                                },
                                onNavigateToSettings = {
                                    navController.navigate(Screen.Settings.route)
                                },
                                onNavigateToSession = { sessionModelId, sessionId ->
                                    try {
                                        val encodedModelId = URLEncoder.encode(sessionModelId, "UTF-8")
                                        val encodedSessionId = URLEncoder.encode(sessionId, "UTF-8")
                                        navController.navigate("${Screen.Chat.route}/$encodedModelId/$encodedSessionId")
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Session navigation error", e)
                                    }
                                },
                                onNewChat = { newChatModelId ->
                                    try {
                                        val encodedModelId = URLEncoder.encode(newChatModelId, "UTF-8")
                                        navController.navigate("${Screen.Chat.route}/$encodedModelId") {
                                            popUpTo("${Screen.Chat.route}/$encodedModelId") { inclusive = true }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "New chat navigation error", e)
                                    }
                                }
                            )
                        }
                        
                        composable(
                            route = "${Screen.Chat.route}/{modelId}/{sessionId}",
                            arguments = listOf(
                                androidx.navigation.navArgument("modelId") { type = androidx.navigation.NavType.StringType },
                                androidx.navigation.navArgument("sessionId") { type = androidx.navigation.NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val encodedModelId = backStackEntry.arguments?.getString("modelId") ?: ""
                            val encodedSessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                            val modelId = try {
                                URLDecoder.decode(encodedModelId, "UTF-8")
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error decoding model ID: $encodedModelId", e)
                                encodedModelId
                            }
                            val sessionId = try {
                                URLDecoder.decode(encodedSessionId, "UTF-8")
                            } catch (e: Exception) {
                                Log.e("MainActivity", "Error decoding session ID: $encodedSessionId", e)
                                null
                            }
                            ChatScreen(
                                modelId = modelId,
                                sessionId = sessionId,
                                onBackPressed = {
                                    navController.popBackStack()
                                },
                                onNavigateToSettings = {
                                    navController.navigate(Screen.Settings.route)
                                },
                                onNavigateToSession = { sessionModelId, sessionIdParam ->
                                    try {
                                        val encodedModel = URLEncoder.encode(sessionModelId, "UTF-8")
                                        val encodedSession = URLEncoder.encode(sessionIdParam, "UTF-8")
                                        navController.navigate("${Screen.Chat.route}/$encodedModel/$encodedSession")
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Session navigation error", e)
                                    }
                                },
                                onNewChat = { newChatModelId ->
                                    try {
                                        val encodedModelId = URLEncoder.encode(newChatModelId, "UTF-8")
                                        navController.navigate("${Screen.Chat.route}/$encodedModelId") {
                                            popUpTo("${Screen.Chat.route}/$encodedModelId") { inclusive = true }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "New chat navigation error", e)
                                    }
                                }
                            )
                        }
                        
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                onBackPressed = {
                                    navController.popBackStack()
                                },
                                onChangeModel = {
                                    navController.navigate(Screen.ModelSelection.route)
                                }
                            )
                        }
                        }
                    }
                }
            }
        }
    }
}