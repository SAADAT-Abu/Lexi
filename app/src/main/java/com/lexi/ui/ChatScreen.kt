package com.lexi.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.lexi.data.ChatHistoryManager
import com.lexi.model.ChatSession
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lexi.R
import com.lexi.model.ChatMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modelId: String,
    sessionId: String? = null,
    onBackPressed: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToSession: (String, String) -> Unit = { _, _ -> },
    onNewChat: (String) -> Unit = { _ -> },
    viewModel: ChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    var showModelInfo by remember { mutableStateOf(false) }
    var enableWebSearch by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val chatHistoryManager = remember { ChatHistoryManager(context) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val chatSessions by chatHistoryManager.getAllSessions().collectAsState(initial = emptyList())
    var currentSessionId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(modelId) {
        android.util.Log.d("ChatScreen", "Setting model: $modelId")
        try {
            viewModel.setModel(modelId)
        } catch (e: Exception) {
            android.util.Log.e("ChatScreen", "Error setting model", e)
            e.printStackTrace()
        }
    }
    
    // Create or load session
    LaunchedEffect(modelId, sessionId) {
        if (sessionId != null) {
            // Load existing session
            val session = chatHistoryManager.getSession(sessionId)
            if (session != null) {
                currentSessionId = sessionId
                viewModel.loadMessages(session.messages)
            }
        } else if (currentSessionId == null) {
            // Create new session when first message is sent
            // We'll do this in the send message handler
        }
    }
    
    // Update session when messages change
    LaunchedEffect(uiState.messages) {
        if (currentSessionId != null && uiState.messages.isNotEmpty()) {
            chatHistoryManager.updateSession(currentSessionId!!, uiState.messages)
        }
    }
    
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                ChatHistoryDrawerContent(
                    sessions = chatSessions,
                    onSessionSelected = { session ->
                        scope.launch {
                            drawerState.close()
                        }
                        onNavigateToSession(session.modelId, session.id)
                    },
                    onNewChat = {
                        scope.launch {
                            drawerState.close()
                        }
                        onNewChat(modelId)
                    },
                    onSettingsClicked = {
                        scope.launch {
                            drawerState.close()
                        }
                        onNavigateToSettings()
                    },
                    onDeleteSession = { sessionId ->
                        scope.launch {
                            chatHistoryManager.deleteSession(sessionId)
                        }
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        TopAppBar(
            title = { Text(stringResource(R.string.chat_title)) },
            navigationIcon = {
                IconButton(
                    onClick = {
                        scope.launch {
                            drawerState.open()
                        }
                    }
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            },
            actions = {
                IconButton(onClick = { showModelInfo = true }) {
                    Icon(Icons.Default.Info, contentDescription = "Model Info")
                }
            }
        )
        
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.messages) { message ->
                ChatMessageBubble(message = message)
            }
            
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        
        if (uiState.error.isNotEmpty()) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Web search toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Web Search",
                        modifier = Modifier.size(16.dp),
                        tint = if (enableWebSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Web Search",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enableWebSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enableWebSearch,
                    onCheckedChange = { enableWebSearch = it }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Message input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.message_hint)) },
                    enabled = !uiState.isLoading,
                    maxLines = 4
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                FloatingActionButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            scope.launch {
                                // Create session if this is the first message
                                if (currentSessionId == null) {
                                    val session = chatHistoryManager.createSession(
                                        title = messageText.take(30) + if (messageText.length > 30) "..." else "",
                                        modelId = modelId,
                                        modelName = modelId.substringAfter("/")
                                    )
                                    currentSessionId = session.id
                                }
                            }
                            // TODO: Use enableWebSearch flag to modify request
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Send, contentDescription = stringResource(R.string.send))
                }
            }
        }
    }
    
    // Model Info Dialog
    if (showModelInfo) {
        ModelInfoDialog(
            modelId = modelId,
            onDismiss = { showModelInfo = false }
        )
    }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatMessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .combinedClickable(
                    onLongClick = {
                        clipboardManager.setText(AnnotatedString(message.content))
                        // Show a toast notification
                        android.widget.Toast.makeText(
                            context,
                            "Message copied to clipboard",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onClick = { /* Optional: handle regular click if needed */ }
                ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ) {
            MessageContent(
                message = message,
                isUser = isUser,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun MessageContent(
    message: ChatMessage,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    val content = message.content
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    // Check if message contains code blocks
    val codeBlockRegex = "```([\\s\\S]*?)```".toRegex()
    val inlineCodeRegex = "`([^`]+)`".toRegex()
    
    if (codeBlockRegex.containsMatchIn(content) || inlineCodeRegex.containsMatchIn(content)) {
        // Parse and render message with code blocks
        ParsedMessage(
            content = content,
            textColor = textColor,
            isUser = isUser,
            modifier = modifier
        )
    } else {
        // Regular text message
        Text(
            text = content,
            modifier = modifier,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ParsedMessage(
    content: String,
    textColor: Color,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val parts = parseMessageContent(content)
        
        parts.forEach { part ->
            when (part.type) {
                MessagePartType.TEXT -> {
                    if (part.content.isNotBlank()) {
                        Text(
                            text = part.content,
                            color = textColor,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                MessagePartType.CODE_BLOCK -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    CodeBlock(
                        code = part.content,
                        language = part.language ?: "text",
                        isUser = isUser
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                MessagePartType.INLINE_CODE -> {
                    Text(
                        text = part.content,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CodeBlock(
    code: String,
    language: String,
    isUser: Boolean
) {
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = {
                    clipboardManager.setText(AnnotatedString(code))
                    android.widget.Toast.makeText(
                        context,
                        "Code copied to clipboard",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                },
                onClick = { }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isUser) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            }
        )
    ) {
        Column {
            // Language header
            if (language.isNotBlank() && language != "text") {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = language.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Code content
            Text(
                text = code,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(12.dp),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                color = if (isUser) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

// Data classes and parsing logic
data class MessagePart(
    val content: String,
    val type: MessagePartType,
    val language: String? = null
)

enum class MessagePartType {
    TEXT,
    CODE_BLOCK,
    INLINE_CODE
}

fun parseMessageContent(content: String): List<MessagePart> {
    val parts = mutableListOf<MessagePart>()
    val codeBlockRegex = "```(\\w*)\\n([\\s\\S]*?)```".toRegex()
    val inlineCodeRegex = "`([^`]+)`".toRegex()
    
    var lastIndex = 0
    
    // Find code blocks first
    codeBlockRegex.findAll(content).forEach { match ->
        // Add text before code block
        if (match.range.first > lastIndex) {
            val textBefore = content.substring(lastIndex, match.range.first)
            if (textBefore.isNotBlank()) {
                parts.add(MessagePart(textBefore, MessagePartType.TEXT))
            }
        }
        
        // Add code block
        val language = match.groupValues[1].ifBlank { "text" }
        val code = match.groupValues[2].trim()
        parts.add(MessagePart(code, MessagePartType.CODE_BLOCK, language))
        
        lastIndex = match.range.last + 1
    }
    
    // Add remaining text
    if (lastIndex < content.length) {
        val remainingText = content.substring(lastIndex)
        if (remainingText.isNotBlank()) {
            parts.add(MessagePart(remainingText, MessagePartType.TEXT))
        }
    }
    
    // If no code blocks found, treat as regular text
    if (parts.isEmpty()) {
        parts.add(MessagePart(content, MessagePartType.TEXT))
    }
    
    return parts
}

@Composable
fun ModelInfoDialog(
    modelId: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Know the Model",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                // Model Name
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Current Model",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = modelId.split("/").lastOrNull()?.replace("-", " ")?.replaceFirstChar { 
                                if (it.isLowerCase()) it.titlecase() else it.toString() 
                            } ?: modelId,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = modelId,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Model Features
                val features = when {
                    modelId.contains("claude", ignoreCase = true) -> listOf(
                        "💭 Advanced reasoning capabilities",
                        "📝 Excellent at writing and analysis",
                        "🔍 Good at following complex instructions",
                        "🌐 Web search not supported"
                    )
                    modelId.contains("gpt", ignoreCase = true) -> listOf(
                        "🧠 General-purpose conversational AI",
                        "💻 Good at coding and problem-solving", 
                        "🎨 Creative writing capabilities",
                        "🌐 Web search may be supported"
                    )
                    modelId.contains("llama", ignoreCase = true) -> listOf(
                        "🦙 Open-source model",
                        "💬 Good conversational abilities",
                        "⚡ Fast response times",
                        "🌐 Web search not supported"
                    )
                    modelId.contains("gemma", ignoreCase = true) -> listOf(
                        "💎 Google's lightweight model",
                        "📊 Good at structured tasks",
                        "🔧 Efficient and fast",
                        "🌐 Web search not supported"
                    )
                    else -> listOf(
                        "🤖 AI language model",
                        "💬 Conversational capabilities",
                        "📝 Text generation and analysis",
                        "🌐 Web search support varies"
                    )
                }
                
                features.forEach { feature ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

@Composable
fun ChatHistoryDrawerContent(
    sessions: List<ChatSession>,
    onSessionSelected: (ChatSession) -> Unit,
    onNewChat: () -> Unit,
    onSettingsClicked: () -> Unit,
    onDeleteSession: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Lexi",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Your AI Powerhouse",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // New Chat Button
        Button(
            onClick = onNewChat,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "New Chat"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("New Chat")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Chat History
        if (sessions.isNotEmpty()) {
            Text(
                text = "Chat History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions) { session ->
                    ChatSessionCard(
                        session = session,
                        onClick = { onSessionSelected(session) },
                        onDelete = { onDeleteSession(session.id) }
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "💬",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No conversations yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Start a new chat to begin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Settings Button
        Divider()
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onSettingsClicked,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Settings")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ChatSessionCard(
    session: ChatSession,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = session.getPreview(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = session.modelName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Chat",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Chat") },
            text = { Text("Are you sure you want to delete this conversation?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
