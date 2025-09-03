# Lexi App - Current Debugging Status

## 📱 Project Overview
**Lexi** is an Android app for chatting with AI models via OpenRouter API, built with Kotlin and Jetpack Compose.

---

## 🚨 Current Issues

### **Primary Problem**: Network Connectivity Failure
- **Network Test shows "Network failed: null"** - Indicates network security/permission issue
- App **cannot make HTTP/HTTPS requests** - All network calls failing silently
- **Network permissions not visible** (normal for Android 14, but INTERNET permission granted automatically)

### **Root Cause Identified**:
- **Android 14 network security restrictions** likely blocking network requests
- **Cleartext traffic policies** may be preventing HTTP requests
- **SSL/TLS certificate issues** may be blocking HTTPS requests
- **Device or network-level restrictions** preventing outbound connections

---

## 🔧 Debugging Progress

### ✅ **Completed Fixes**:

1. **Network Permissions Setup**:
   - Added `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` permissions
   - Configured network security config for HTTPS
   - Enabled cleartext traffic for debugging

2. **OpenRouter API Authentication**:
   - Correct Bearer token format: `Authorization: Bearer <API_KEY>`
   - Added required headers: `HTTP-Referer` and `X-Title`
   - Added `Content-Type: application/json` header

3. **Enhanced Error Handling**:
   - Extensive logging throughout API validation process
   - Detailed HTTP response code and message logging
   - Exception stack trace printing
   - User-friendly error messages

4. **API Key Validation Logic**:
   - Forces actual network requests to OpenRouter `/models` endpoint
   - Validates response contains actual model data
   - Prevents progression without valid API key

5. **Enhanced Diagnostic Tools**:
   - **NetworkTestScreen**: Tests basic internet connectivity using `httpbin.org`
   - **Detailed Exception Handling**: Shows SecurityException, DNS, Connection, SSL errors
   - **Network State Check**: Tests active network and internet capability
   - **HTTP vs HTTPS Testing**: Isolates SSL/TLS issues from general network problems
   - **Comprehensive logging**: Shows every step of network requests

---

## 📂 Project Structure

```
/home/saadat/Documents/Lexi/
├── app/
│   ├── build.gradle                    # Dependencies & build config
│   └── src/main/
│       ├── AndroidManifest.xml         # Permissions & app config
│       ├── java/com/lexi/
│       │   ├── MainActivity.kt          # Main activity with navigation
│       │   ├── SimpleMainActivity.kt   # Test activity (minimal)
│       │   ├── api/
│       │   │   ├── ApiClient.kt         # Retrofit client with auth
│       │   │   └── OpenRouterApi.kt     # API interface definitions
│       │   ├── model/
│       │   │   └── Models.kt            # Data classes for API responses
│       │   ├── navigation/
│       │   │   └── Screen.kt            # Navigation routes
│       │   └── ui/
│       │       ├── NetworkTestScreen.kt # Network connectivity test
│       │       ├── ApiKeyScreen.kt      # API key input with validation
│       │       ├── ModelSelectionScreen.kt # Model selection UI
│       │       ├── ChatScreen.kt        # Chat interface
│       │       ├── TestChatScreen.kt    # Simple test screen
│       │       ├── ErrorScreen.kt       # Error display component
│       │       └── theme/               # Material3 theme files
│       └── res/
│           ├── values/
│           │   ├── strings.xml          # App strings
│           │   ├── themes.xml           # Material theme
│           │   └── colors.xml           # Color definitions
│           └── xml/
│               └── network_security_config.xml # HTTPS config
├── build.gradle                        # Project-level build config
├── settings.gradle                     # Gradle settings
└── README.md                          # Original project description
```

---

## 🧪 Current App Flow (Diagnostic Version)

1. **NetworkTestScreen** (First screen)
   - Tests basic HTTP connectivity to `httpbin.org`
   - Uses simple `HttpURLConnection` 
   - Shows "✅ Network works!" or network error

2. **ApiKeyScreen** 
   - Enhanced with extensive logging
   - Makes real API request to OpenRouter `/models`
   - Validates response contains model data
   - Shows detailed HTTP codes and errors

3. **ModelSelectionScreen**
   - Loads free models from OpenRouter
   - Enhanced error handling and logging

4. **ChatScreen** 
   - Chat interface with OpenRouter integration
   - Proper API request/response handling

---

## 🔍 Key Technical Details

### **Network Configuration**:
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<application android:usesCleartextTraffic="true"
             android:networkSecurityConfig="@xml/network_security_config">
```

### **API Authentication**:
```kotlin
// ApiClient.kt
.header("Authorization", "Bearer $apiKey")
.header("HTTP-Referer", "https://github.com/lexi-app")
.header("X-Title", "Lexi")
.header("Content-Type", "application/json")
```

### **OpenRouter Endpoints**:
- **Base URL**: `https://openrouter.ai/api/v1/`
- **Models**: `GET /models` - List available models
- **Chat**: `POST /chat/completions` - Send chat messages

---

## 📋 Next Steps for Debugging

### **Current Test Results**:
- ❌ **"Test Network" button shows "Network failed: null"** 
- 🔍 **Indicates network security/permission issue on Android 14**

### **Next Testing Priorities**:

1. **Enhanced Network Diagnostics**:
   - Test **"Test Network State & HTTP"** button (new enhanced version)
   - Check for specific error types: SecurityException, DNS, Connection, SSL
   - Verify if HTTP works vs HTTPS (SSL issue isolation)

2. **Log Analysis** (Check Android logs):
   - Look for "NetworkTest" logs showing specific failure points
   - Check for SecurityException, UnknownHostException, ConnectException
   - Identify if it's SSL/TLS, DNS, or permission related

3. **Network Policy Investigation**:
   - Test on different network (mobile data vs WiFi)
   - Check if corporate/school network is blocking requests
   - Verify if Android 14 network security policies are interfering

### **Potential Issues to Investigate**:
- Android 14 network security restrictions
- Corporate/WiFi network blocking HTTPS requests
- Device-specific network policy restrictions
- OpenRouter API endpoint changes or rate limiting
- Manifest merge issues dropping permissions

---

## 🛠 Development Environment

- **Target SDK**: 34 (Android 14)
- **Min SDK**: 24
- **Kotlin Version**: 1.9.20
- **Compose BOM**: 2023.10.01
- **Gradle Version**: 8.4

---

## 📝 Known Working Components

- ✅ **App Installation**: APK builds and installs successfully
- ✅ **UI Framework**: Jetpack Compose and Material3 working
- ✅ **Navigation**: Navigation Compose working between screens
- ✅ **Theme**: Material Design theme loads correctly
- ✅ **Permissions**: INTERNET permission automatically granted (normal on Android 14)
- ✅ **Build System**: Gradle builds without errors

---

## ❌ Outstanding Issues

- 🔴 **Network connectivity completely blocked** - "Network failed: null" error
- 🔴 **Android 14 network security restrictions** - Likely root cause
- 🔴 **All HTTP/HTTPS requests failing** - No network access from app
- ⚠️  **SSL/TLS or cleartext traffic policies** - Need to identify specific cause
- ⚠️  **Device or network-level restrictions** - May need alternative approach

## 🎯 **Next Session Focus**

1. **Test enhanced network diagnostics** (new buttons with detailed error reporting)
2. **Analyze Android logs** for specific exception types
3. **Investigate Android 14 network security policies** and workarounds
4. **Test alternative network approaches** if current method blocked

---

*Last updated: September 2, 2025*
*Status: Network connectivity root cause identified, enhanced diagnostics ready*
*Next session: Detailed network failure analysis and Android 14 compatibility fixes*