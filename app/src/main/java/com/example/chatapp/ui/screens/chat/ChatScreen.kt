package com.example.chatapp.ui.screens.chat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chatapp.data.local.ConversationSummary
import com.example.chatapp.data.model.MessageRole
import com.example.chatapp.domain.AssistantResponseCleaner
import com.example.chatapp.ui.components.BrandLockup
import com.example.chatapp.ui.components.ChatBubble
import com.example.chatapp.ui.components.MessageInput
import com.example.chatapp.ui.components.TypingIndicator
import com.example.chatapp.ui.theme.DarkBackground
import com.example.chatapp.ui.theme.PrimaryGreen
import kotlinx.coroutines.launch
import androidx.compose.material3.rememberDrawerState
import androidx.core.content.ContextCompat
import java.io.File

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
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    var hasProcessedInitialPrompt by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingTarget by remember { mutableStateOf<File?>(null) }
    var recordingStartedAt by remember { mutableStateOf(0L) }
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            messages.isEmpty() || lastVisible >= messages.lastIndex - 1
        }
    }

    fun stopRecording(save: Boolean) {
        val activeRecorder = recorder ?: return
        runCatching {
            activeRecorder.stop()
        }
        activeRecorder.release()
        recorder = null
        isRecording = false

        val target = recordingTarget
        recordingTarget = null
        if (save && target != null) {
            val durationMillis = SystemClock.elapsedRealtime() - recordingStartedAt
            viewModel.attachRecordedAudio(target, durationMillis)
        } else {
            target?.delete()
        }
    }

    fun startRecording() {
        val target = viewModel.createAudioTarget()
        val mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(target.absolutePath)
            prepare()
            start()
        }
        recordingTarget = target
        recordingStartedAt = SystemClock.elapsedRealtime()
        recorder = mediaRecorder
        isRecording = true
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.attachImage(it) }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            runCatching { startRecording() }
                .onFailure {
                    scope.launch {
                        snackbarHostState.showSnackbar("Couldn't start recording. Please try again.")
                    }
                }
        } else {
            scope.launch { snackbarHostState.showSnackbar("Microphone permission is needed to record audio.") }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopRecording(save = false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initEngine()
    }

    LaunchedEffect(initialConversationId) {
        viewModel.openConversation(initialConversationId)
    }

    LaunchedEffect(isLoading, errorMessage, initialPrompt, hasProcessedInitialPrompt) {
        if (!isLoading && errorMessage == null && initialPrompt != null && !hasProcessedInitialPrompt) {
            hasProcessedInitialPrompt = true
            viewModel.sendMessage(initialPrompt)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && isAtBottom) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    DismissibleNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DismissibleDrawerSheet(
                modifier = Modifier.widthIn(min = 260.dp, max = 340.dp),
                drawerContainerColor = Color(0xFF141414)
            ) {
                ChatHistoryDrawer(
                    historyItems = conversationHistory,
                    activeConversationId = activeConversationId,
                    onNewChat = {
                        viewModel.startNewConversation()
                        scope.launch { drawerState.close() }
                    },
                    onOpenConversation = { conversationId ->
                        viewModel.openConversation(conversationId)
                        scope.launch { drawerState.close() }
                    },
                    onDeleteConversation = viewModel::deleteConversation
                )
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenWidth = maxWidth
            val horizontalPad = when {
                screenWidth >= 900.dp -> 36.dp
                screenWidth >= 600.dp -> 24.dp
                screenWidth >= 400.dp -> 16.dp
                screenWidth >= 360.dp -> 14.dp
                else -> 8.dp
            }
            val isCompact = screenWidth < 360.dp
            val isLargeLayout = screenWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = DarkBackground,
                contentWindowInsets = WindowInsets(0),
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(horizontal = horizontalPad, vertical = 8.dp)
                    ) { data ->
                        Snackbar(
                            containerColor = Color(0xFF2A2A2A),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = data.visuals.message,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                },
                topBar = {
                    ChatTopBar(
                        isReady = !isLoading && viewModel.isEngineReady(),
                        modelName = selectedModelName,
                        onOpenHistory = {
                            scope.launch {
                                if (drawerState.isOpen) drawerState.close() else drawerState.open()
                            }
                        },
                        onOpenSettings = onOpenSettings,
                        onOpenModels = onOpenModels
                    )
                },
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .imePadding()
                            .background(Color.Transparent)
                    ) {
                        if (isGenerating && (generationTokensPerSecond != null || timeToFirstTokenMillis != null)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = horizontalPad, vertical = 2.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = Color(0xFF1E1E1E)
                                ) {
                                    Text(
                                        text = buildString {
                                            generationTokensPerSecond?.let {
                                                append("${String.format(java.util.Locale.US, "%.1f", it)} tok/s")
                                            }
                                            timeToFirstTokenMillis?.let {
                                                if (isNotEmpty()) append(" • ")
                                                append("TTFT ${it}ms")
                                            }
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF9ADFA8)
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = horizontalPad,
                                    vertical = if (isCompact) 6.dp else 8.dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            MessageInput(
                                modifier = Modifier.widthIn(max = if (isLargeLayout) 920.dp else 640.dp),
                                isGenerating = isGenerating,
                                isRecording = isRecording,
                                enabled = !isLoading,
                                onSend = { text ->
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    keyboardController?.hide()
                                    viewModel.sendMessage(text)
                                },
                                onStop = viewModel::stopGeneration,
                                onAttachImage = {
                                    imagePicker.launch("image/*")
                                },
                                onToggleRecording = {
                                    if (isRecording) {
                                        stopRecording(save = true)
                                    } else {
                                        when (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)) {
                                            PackageManager.PERMISSION_GRANTED -> {
                                                runCatching { startRecording() }
                                                    .onFailure {
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar("Couldn't start recording. Please try again.")
                                                        }
                                                    }
                                            }
                                            else -> audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                        .padding(paddingValues)
                ) {
                    if (messages.isEmpty()) {
                        EmptyChatState(
                            modifier = Modifier.fillMaxSize(),
                            compact = isCompact,
                            isLargeLayout = isLargeLayout
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = if (isLargeLayout) 920.dp else 760.dp)
                                    .padding(horizontal = horizontalPad),
                                contentPadding = PaddingValues(top = 10.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 14.dp)
                            ) {
                                items(messages, key = { it.id }) { message ->
                                    val isWaitingForVisibleAiText = message.role == MessageRole.AI &&
                                        message.isStreaming &&
                                        AssistantResponseCleaner.clean(message.content).isBlank()
                                    AnimatedVisibility(
                                        visible = true,
                                        enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn()
                                    ) {
                                        if (isWaitingForVisibleAiText) {
                                            TypingIndicator()
                                        } else {
                                            ChatBubble(
                                                message = message,
                                                onRetry = { viewModel.retryMessage(it.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        AnimatedVisibility(
                            visible = !isAtBottom,
                            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = horizontalPad, bottom = 18.dp)
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    scope.launch {
                                        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
                                    }
                                },
                                containerColor = PrimaryGreen,
                                contentColor = Color.White,
                                shape = CircleShape,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Scroll to latest"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatTopBar(
    isReady: Boolean,
    modelName: String,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenModels: (() -> Unit)?
) {
    val statusDot = if (isReady) PrimaryGreen else Color(0xFFEF5350)
    val statusGlow = if (isReady) PrimaryGreen.copy(alpha = 0.3f) else Color(0xFFEF5350).copy(alpha = 0.3f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141414))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenHistory) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = "Open history",
                    tint = Color(0xFFAAAAAA)
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Brand icon with subtle glow ring
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(PrimaryGreen.copy(alpha = 0.1f), CircleShape)
                        .border(1.5.dp, PrimaryGreen.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Hub,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "InnoAI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(statusGlow, CircleShape)
                                .padding(1.dp)
                                .background(statusDot, CircleShape)
                        )
                        Text(
                            text = if (isReady) "$modelName ready" else "Model unavailable",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isReady) Color(0xFF999999) else Color(0xFFEF5350).copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onOpenModels?.invoke() }) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Open models",
                        tint = Color(0xFFAAAAAA)
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Settings",
                        tint = Color(0xFFAAAAAA)
                    )
                }
            }
        }

        // Subtle divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF333333),
                            Color(0xFF333333),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun ChatHistoryDrawer(
    historyItems: List<ConversationSummary>,
    activeConversationId: Long?,
    onNewChat: () -> Unit,
    onOpenConversation: (Long) -> Unit,
    onDeleteConversation: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141414))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 18.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNewChat),
            shape = RoundedCornerShape(14.dp),
            color = PrimaryGreen
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.EditNote,
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = "New Chat",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.size(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.History,
                contentDescription = null,
                tint = Color(0xFF888888),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Recent chats",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF888888)
            )
        }

        Spacer(modifier = Modifier.size(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (historyItems.isEmpty()) {
                Text(
                    text = "No chat history yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF777777)
                )
            }

            historyItems.forEach { item ->
                val selected = item.id == activeConversationId
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenConversation(item.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) PrimaryGreen.copy(alpha = 0.15f) else Color(0xFF1E1E1E)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = if (selected) PrimaryGreen else Color(0xFF9A9A9A),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) PrimaryGreen else Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onDeleteConversation(item.id) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete chat",
                                    tint = Color(0xFF8A8A8A),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatState(
    modifier: Modifier = Modifier,
    compact: Boolean,
    isLargeLayout: Boolean = false
) {
    val horizontalPadding = when {
        isLargeLayout -> 40.dp
        compact -> 12.dp
        else -> 20.dp
    }

    BoxWithConstraints(modifier = modifier) {
        val contentMaxWidth = when {
            maxWidth >= 900.dp -> 520.dp
            maxWidth >= 600.dp -> 460.dp
            else -> maxWidth
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(if (compact) 16.dp else 28.dp))

            // Use BrandLockup for a premium branded header
            BrandLockup(
                title = "InnoAI",
                subtitle = "Private offline assistant",
                large = !compact
            )

            Spacer(modifier = Modifier.height(if (compact) 12.dp else 20.dp))

            Column(
                modifier = Modifier
                    .widthIn(max = contentMaxWidth)
                    .padding(horizontal = horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EmptyHintRow("Text", "Ask questions and keep chats stored locally.")
                EmptyHintRow("Images", "Attach pictures from your device for local chat context.")
                EmptyHintRow("Audio", "Record voice notes saved privately on this device.")
            }
        }
    }
}

@Composable
private fun EmptyHintRow(
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF888888)
            )
        }
    }
}
