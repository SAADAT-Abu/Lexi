package com.lexi.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun NetworkTestScreen(
    onNext: () -> Unit
) {
    var testResult by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Network Test",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (testResult.isNotEmpty()) {
            Text(
                text = testResult,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    testResult = "Testing network connection..."
                    
                    try {
                        val result = withContext(Dispatchers.IO) {
                            android.util.Log.d("NetworkTest", "Starting network test...")
                            
                            // Simple HTTP test
                            val url = URL("https://httpbin.org/get")
                            android.util.Log.d("NetworkTest", "Created URL: $url")
                            
                            val connection = url.openConnection() as HttpURLConnection
                            android.util.Log.d("NetworkTest", "Got connection: $connection")
                            
                            connection.requestMethod = "GET"
                            connection.connectTimeout = 5000
                            connection.readTimeout = 5000
                            android.util.Log.d("NetworkTest", "Set connection properties")
                            
                            android.util.Log.d("NetworkTest", "Attempting to connect...")
                            val responseCode = connection.responseCode
                            android.util.Log.d("NetworkTest", "Got response code: $responseCode")
                            
                            connection.disconnect()
                            "✅ Network works! HTTP $responseCode"
                        }
                        testResult = result
                    } catch (e: SecurityException) {
                        android.util.Log.e("NetworkTest", "Security exception", e)
                        testResult = "❌ Security error: ${e.message} - Check network permissions"
                    } catch (e: java.net.UnknownHostException) {
                        android.util.Log.e("NetworkTest", "Unknown host", e)
                        testResult = "❌ DNS failed: Cannot resolve httpbin.org - Check internet connection"
                    } catch (e: java.net.ConnectException) {
                        android.util.Log.e("NetworkTest", "Connection failed", e)
                        testResult = "❌ Connection failed: ${e.message} - Network blocked?"
                    } catch (e: javax.net.ssl.SSLException) {
                        android.util.Log.e("NetworkTest", "SSL error", e)
                        testResult = "❌ SSL error: ${e.message} - Certificate issue"
                    } catch (e: Exception) {
                        android.util.Log.e("NetworkTest", "Network test failed", e)
                        testResult = "❌ Network failed: ${e.javaClass.simpleName} - ${e.message ?: "Unknown error"}"
                        e.printStackTrace()
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Test Network")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    testResult = "Testing multiple approaches..."
                    
                    // Test 1: Check network state
                    try {
                        val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                        val network = connectivityManager.activeNetwork
                        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                        
                        android.util.Log.d("NetworkTest", "Network state: $network")
                        android.util.Log.d("NetworkTest", "Network capabilities: $networkCapabilities")
                        
                        if (network == null) {
                            testResult = "❌ No active network connection"
                            isLoading = false
                            return@launch
                        }
                        
                        if (networkCapabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
                            testResult = "🔍 Network available, testing connectivity..."
                        } else {
                            testResult = "❌ Network has no internet capability"
                            isLoading = false
                            return@launch
                        }
                        
                    } catch (e: Exception) {
                        android.util.Log.e("NetworkTest", "Network state check failed", e)
                        testResult = "❌ Network state check failed: ${e.message}"
                        isLoading = false
                        return@launch
                    }
                    
                    // Test 2: Simple HTTP request
                    try {
                        val result = withContext(Dispatchers.IO) {
                            val url = URL("https://httpbin.org/get")
                            android.util.Log.d("NetworkTest", "Trying HTTPS: $url")
                            
                            val connection = url.openConnection() as HttpURLConnection
                            connection.requestMethod = "GET"
                            connection.connectTimeout = 10000
                            connection.readTimeout = 10000
                            
                            val responseCode = connection.responseCode
                            android.util.Log.d("NetworkTest", "HTTP response: $responseCode")
                            
                            connection.disconnect()
                            "✅ HTTPS works! Code: $responseCode"
                        }
                        testResult = result
                    } catch (e: Exception) {
                        android.util.Log.e("NetworkTest", "HTTP test failed", e)
                        testResult = "❌ HTTPS failed: ${e.javaClass.simpleName} - ${e.message}"
                    }
                    
                    isLoading = false
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test Network State & HTTPS")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue to API Key")
        }
    }
}