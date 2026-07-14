package com.example.chatapp.ui.screens.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chatapp.data.local.ConversationSummary
import com.example.chatapp.data.model.MessageRole
import com.example.chatapp.domain.AssistantResponseCleaner
import com.example.chatapp.ui.components.BrandLockup
import com.example.chatapp.ui.components.AppLogo
import com.example.chatapp.ui.components.ChatBubble
import com.example.chatapp.ui.components.MessageInput
import com.example.chatapp.ui.components.TypingIndicator
import com.example.chatapp.ui.theme.PrimaryGreen
import com.example.chatapp.ui.theme.chipBackground
import com.example.chatapp.ui.theme.isDark
import com.example.chatapp.ui.theme.subtleBorder
import com.example.chatapp.ui.theme.textHint
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    initialPrompt: String? = null,
    initialConversationId: Long? = null,
    onOpenSettings: () -> Unit,
    onOpenModels: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generationTokensPerSecond by viewModel.generationTokensPerSecond.collectAsStateWithLifecycle()
    val timeToFirstTokenMillis by viewModel.timeToFirstTokenMillis.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val conversationHistory by viewModel.conversationHistory.collectAsStateWithLifecycle()
    val selectedModelName by viewModel.selectedModelName.collectAsStateWithLifecycle()
    val contextWindowPercent by viewModel.contextWindowPercent.collectAsStateWithLifecycle()
    val isModelDownloaded by viewModel.isModelDownloaded.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    var hasProcessedInitialPrompt by remember { mutableStateOf(false) }
    var suggestedPrompt by remember { mutableStateOf<String?>(null) }

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) true
            else visibleItems.last().index >= layoutInfo.totalItemsCount - 1
        }
    }

    LaunchedEffect(isModelDownloaded) { viewModel.initEngine() }

    LaunchedEffect(initialConversationId) { viewModel.openConversation(initialConversationId) }

    LaunchedEffect(isLoading, errorMessage, initialPrompt) {
        if (!isLoading && errorMessage == null && initialPrompt != null && !hasProcessedInitialPrompt) {
            hasProcessedInitialPrompt = true
            viewModel.sendMessage(initialPrompt)
        }
    }

    LaunchedEffect(messages.size, isGenerating) {
        if (isAtBottom && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    DismissibleNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DismissibleDrawerSheet(
                modifier = Modifier.widthIn(max = 320.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                ChatHistoryDrawer(
                    historyItems = conversationHistory,
                    activeConversationId = activeConversationId,
                    userName = userName,
                    onNewChat = {
                        viewModel.startNewConversation()
                        scope.launch { drawerState.close() }
                    },
                    onOpenConversation = { id ->
                        viewModel.openConversation(id)
                        scope.launch { drawerState.close() }
                    },
                    onDeleteConversation = viewModel::deleteConversation,
                    onRenameConversation = viewModel::renameConversation
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                ChatTopBar(
                    isReady = !isLoading && viewModel.isEngineReady(),
                    modelName = selectedModelName,
                    onOpenHistory = { scope.launch { if (drawerState.isOpen) drawerState.close() else drawerState.open() } },
                    onOpenSettings = onOpenSettings,
                    onOpenModels = onOpenModels,
                    contextWindowPercent = contextWindowPercent
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                ChatBottomInputArea(
                    isGenerating = isGenerating,
                    isLoading = isLoading,
                    isModelReady = isModelDownloaded,
                    onOpenModels = onOpenModels,
                    tokensPerSec = generationTokensPerSecond,
                    ttft = timeToFirstTokenMillis,
                    suggestedText = suggestedPrompt,
                    onSuggestionConsumed = { suggestedPrompt = null },
                    onSend = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.sendMessage(it)
                    },
                    onStop = viewModel::stopGeneration
                )
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (messages.isEmpty()) {
                    EmptyChatState(
                        modifier = Modifier.fillMaxSize(),
                        compact = false,
                        onSuggestionClick = { suggestedPrompt = it },
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val showTyping = msg.role == MessageRole.AI && msg.isStreaming && AssistantResponseCleaner.clean(msg.content).isBlank()
                            if (showTyping) TypingIndicator()
                            else ChatBubble(message = msg, onRetry = { viewModel.retryMessage(it.id) })
                        }
                    }
                }

                AnimatedVisibility(
                    visible = !isAtBottom,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    FloatingActionButton(
                        onClick = { scope.launch { listState.animateScrollToItem(messages.lastIndex) } },
                        containerColor = PrimaryGreen,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBottomInputArea(
    isGenerating: Boolean,
    isLoading: Boolean,
    isModelReady: Boolean,
    onOpenModels: (() -> Unit)?,
    tokensPerSec: Float?,
    ttft: Long?,
    suggestedText: String?,
    onSuggestionConsumed: () -> Unit,
    onSend: (String) -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).imePadding()
    ) {
        AnimatedVisibility(visible = isGenerating && (tokensPerSec != null || ttft != null)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Center) {
                Surface(shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.chipBackground) {
                    Text(
                        text = buildString {
                            tokensPerSec?.let { append("${String.format(Locale.US, "%.1f", it)} tok/s") }
                            ttft?.let { if (isNotEmpty()) append(" • "); append("${it}ms TTFT") }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryGreen.copy(alpha = 0.7f)
                    )
                }
            }
        }

        AnimatedVisibility(visible = !isModelReady) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = MaterialTheme.colorScheme.chipBackground,
                    onClick = { onOpenModels?.invoke() }
                ) {
                    Text(
                        text = "Download a model to start chatting  →",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryGreen
                    )
                }
            }
        }

        Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
            MessageInput(
                isGenerating = isGenerating,
                enabled = !isLoading && isModelReady,
                suggestedText = suggestedText,
                onSuggestionConsumed = onSuggestionConsumed,
                onSend = onSend,
                onStop = onStop
            )
        }
    }
}

@Composable
private fun ChatTopBar(
    isReady: Boolean,
    modelName: String,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenModels: (() -> Unit)?,
    contextWindowPercent: Float
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainerLow,
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onOpenHistory) {
                    Icon(Icons.Rounded.Menu, contentDescription = "Chat history", tint = colors.onSurfaceVariant)
                }

                Row(Modifier.weight(1f).padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(32.dp).background(PrimaryGreen.copy(0.12f), RoundedCornerShape(9.dp)).border(1.dp, PrimaryGreen.copy(0.28f), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                        AppLogo(modifier = Modifier.size(20.dp), contentDescription = null)
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text("InnoAI", style = MaterialTheme.typography.titleMedium, color = colors.onSurface, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(6.dp).background(if (isReady) PrimaryGreen else colors.error, CircleShape))
                            Text(
                                text = if (isReady) " $modelName ready" else " Engine loading...",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(onClick = { onOpenModels?.invoke() }) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = "Models", tint = colors.onSurfaceVariant)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Settings", tint = colors.onSurfaceVariant)
                }
            }

            if (contextWindowPercent > 0.8f) {
                LinearProgressIndicator(
                    progress = { contextWindowPercent },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = if (contextWindowPercent > 0.95f) MaterialTheme.colorScheme.error else Color(0xFFF59E0B),
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@Composable
private fun ChatHistoryDrawer(
    historyItems: List<ConversationSummary>,
    activeConversationId: Long?,
    userName: String,
    onNewChat: () -> Unit,
    onOpenConversation: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onRenameConversation: (Long, String) -> Unit
) {
    var renamingItem by remember { mutableStateOf<ConversationSummary?>(null) }
    var renameTitle by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(
            onClick = onNewChat,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Rounded.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("New Chat", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))
        Text("Recent History", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(historyItems) { item ->
                val isSelected = item.id == activeConversationId
                Surface(
                    onClick = { onOpenConversation(item.id) },
                    modifier = Modifier.fillMaxWidth().pointerInput(item.id) {
                        detectTapGestures(onLongPress = { renamingItem = item; renameTitle = item.title })
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) PrimaryGreen.copy(0.15f) else MaterialTheme.colorScheme.chipBackground
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ChatBubbleOutline, null, tint = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Text(item.title, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { onDeleteConversation(item.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete conversation", tint = MaterialTheme.colorScheme.textHint, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.size(30.dp).background(PrimaryGreen.copy(alpha = 0.14f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "I",
                    color = PrimaryGreen,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Text(userName.ifBlank { "InnoAI User" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (renamingItem != null) {
        AlertDialog(
            onDismissRequest = { renamingItem = null },
            title = { Text("Rename Chat") },
            text = { OutlinedTextField(value = renameTitle, onValueChange = { renameTitle = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { onRenameConversation(renamingItem!!.id, renameTitle); renamingItem = null }) { Text("Save", color = PrimaryGreen) } }
        )
    }
}

@Composable
private fun EmptyChatState(
    modifier: Modifier,
    compact: Boolean,
    onSuggestionClick: (String) -> Unit,
) {
    Column(modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BrandLockup(title = "InnoAI", subtitle = "Your Private Offline Assistant", large = !compact)
        Spacer(Modifier.height(32.dp))
        Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SuggestionChip(label = "Explain Quantum Physics", onClick = { onSuggestionClick("Explain Quantum Physics") })
            SuggestionChip(label = "Write Kotlin code", onClick = { onSuggestionClick("Write Kotlin code") })
        }
    }
}

@Composable
private fun SuggestionChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.chipBackground,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.subtleBorder)
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
