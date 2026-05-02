package com.example.chatapp.ui.screens.download

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.data.model.ModelOption
import com.example.chatapp.domain.usecase.DownloadState
import com.example.chatapp.R
import com.example.chatapp.ui.components.DockTab
import com.example.chatapp.ui.components.FloatingBottomDock
import com.example.chatapp.ui.theme.DarkBackground
import com.example.chatapp.ui.theme.ErrorRed
import com.example.chatapp.ui.theme.PrimaryGreen
import kotlin.math.max
import java.util.Locale

@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel,
    onUseModel: () -> Unit,
    onOpenChat: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null
) {
    val state by viewModel.downloadState.collectAsStateWithLifecycle()
    val activeModelId by viewModel.activeModelId.collectAsStateWithLifecycle()
    val lastTouchedModelId by viewModel.lastTouchedModelId.collectAsStateWithLifecycle()
    val selectedModelId by viewModel.selectedModelId.collectAsStateWithLifecycle()
    val huggingFaceToken by viewModel.huggingFaceToken.collectAsStateWithLifecycle()
    val downloadedModelIds by viewModel.downloadedModelIds.collectAsStateWithLifecycle()
    val openSelectedModel by viewModel.openSelectedModel.collectAsStateWithLifecycle()
    var selectedUseCase by remember { mutableStateOf("All") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val activityManager = remember(context) {
        context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    }
    val memoryInfo = remember(activityManager) {
        android.app.ActivityManager.MemoryInfo().apply { activityManager.getMemoryInfo(this) }
    }
    val availableRamGb = (memoryInfo.availMem / (1024f * 1024f * 1024f))
    val availableStorageGb = remember(context) {
        val statFs = android.os.StatFs(context.filesDir.absolutePath)
        statFs.availableBytes / (1024f * 1024f * 1024f)
    }

    val useCaseFilteredModels = remember(selectedUseCase) {
        if (selectedUseCase == "All") {
            viewModel.models()
        } else {
            viewModel.models().filter { model -> model.useCases.contains(selectedUseCase) }
        }
    }

    val recommendedModelId = remember(selectedUseCase, availableRamGb, availableStorageGb) {
        recommendModel(
            models = useCaseFilteredModels,
            selectedUseCase = selectedUseCase,
            availableRamGb = availableRamGb,
            availableStorageGb = availableStorageGb
        )?.id
    }

    val filteredModels = remember(useCaseFilteredModels, recommendedModelId) {
        useCaseFilteredModels.sortedWith(
            compareByDescending<ModelOption> { it.id == recommendedModelId }
                .thenBy { it.sizeMb }
        )
    }

    LaunchedEffect(openSelectedModel) {
        if (openSelectedModel) {
            onUseModel()
            viewModel.onModelOpened()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidthDp = maxWidth.value.toInt()
        val horizontalPad = when {
            maxWidth >= 900.dp -> 32.dp
            maxWidth >= 600.dp -> 24.dp
            maxWidth >= 360.dp -> 18.dp
            else -> 12.dp
        }
        val maxContentWidth = if (maxWidth >= 900.dp) 1040.dp else 860.dp

        Scaffold(
            containerColor = DarkBackground,
            bottomBar = {
                FloatingBottomDock(
                    selectedTab = DockTab.MODELS,
                    modifier = Modifier.imePadding(),
                    onChatClick = onOpenChat,
                    onProfileClick = onProfileClick
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = maxContentWidth),
                    contentPadding = PaddingValues(
                        horizontal = horizontalPad,
                        vertical = 20.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(PrimaryGreen.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.innoailogomain),
                            contentDescription = "App logo",
                            modifier = Modifier.size(24.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Column {
                        Text(
                            text = "Models",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${ModelCatalog.all.size} models available",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF888888)
                        )
                    }
                }
            }

            // Use-case filter chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FamilyChip("All", selectedUseCase == "All") { selectedUseCase = "All" }
                    ModelCatalog.useCaseFilters.forEach { useCase ->
                        FamilyChip(useCase, selectedUseCase == useCase) { selectedUseCase = useCase }
                    }
                }
            }

            item {
                val recommendedName = recommendedModelId?.let { ModelCatalog.fromId(it).displayName } ?: "No exact match"
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1F2B1F)
                ) {
                    Text(
                        text = "Recommended: $recommendedName",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF87E398)
                    )
                }
            }

            // Model cards
            items(filteredModels, key = { it.id }) { model ->
                val localState =
                    if (activeModelId == model.id || lastTouchedModelId == model.id) state else null

                ModelCard(
                    model = model,
                    isSelected = selectedModelId == model.id,
                    isDownloaded = downloadedModelIds.contains(model.id),
                    isActive = activeModelId == model.id,
                    downloadState = localState,
                    isRecommended = model.id == recommendedModelId,
                    deviceFit = evaluateDeviceFit(model, availableRamGb, availableStorageGb),
                    compact = screenWidthDp < 360,
                    huggingFaceToken = huggingFaceToken,
                    onUseModel = { viewModel.useModel(model.id) },
                    onTokenChange = viewModel::updateHuggingFaceToken,
                    onRetry = viewModel::retryActiveModel,
                    onDeleteModel = { viewModel.deleteModel(model.id) }
                )
            }

                    item {
                        Text(
                            text = "Models stay local on your device and run fully on-device after download.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) PrimaryGreen else Color(0xFF2A2A2A)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else Color(0xFF999999)
        )
    }
}

@Composable
private fun ModelCard(
    model: ModelOption,
    isSelected: Boolean,
    isDownloaded: Boolean,
    isActive: Boolean,
    downloadState: DownloadState?,
    isRecommended: Boolean,
    deviceFit: DeviceFit,
    compact: Boolean,
    huggingFaceToken: String,
    onUseModel: () -> Unit,
    onTokenChange: (String) -> Unit,
    onRetry: () -> Unit,
    onDeleteModel: () -> Unit
) {
    val progress = (downloadState as? DownloadState.Downloading)?.progress ?: 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(450),
        label = "model_progress_${model.id}"
    )

    var showDownloadDialog by remember { mutableStateOf(false) }
    var showHuggingFaceDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    if (showDownloadDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val actMan = remember { context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager }
        val memInfo = remember { android.app.ActivityManager.MemoryInfo().apply { actMan.getMemoryInfo(this) } }
        val availRamMb = memInfo.availMem / (1024 * 1024)

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCCCCCC),
            title = { Text("Model Specifications") },
            text = {
                val accessText = if (model.requiresHuggingFaceAccess) {
                    "\n\nThis model also needs Hugging Face license access. Accept the license on Hugging Face and add a read token in Settings before downloading."
                } else {
                    ""
                }
                Text("Your device has ${String.format(java.util.Locale.US, "%.1f", availRamMb / 1024f)} GB of free RAM. This model requires approximately ${String.format(java.util.Locale.US, "%.1f", model.sizeMb / 1000f)} GB.\n\nLarge models may run slowly or crash if your device does not have enough memory.$accessText\n\nDo you want to proceed with downloading?")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { 
                    showDownloadDialog = false
                    onUseModel() 
                }) {
                    Text("Download", color = com.example.chatapp.ui.theme.PrimaryGreen)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDownloadDialog = false }) {
                    Text("Cancel", color = Color(0xFF888888))
                }
            }
        )
    }

    if (showHuggingFaceDialog) {
        HuggingFaceAccessDialog(
            model = model,
            token = huggingFaceToken,
            onTokenChange = onTokenChange,
            onOpenLicense = {
                model.licenseUrl?.let(uriHandler::openUri)
            },
            onOpenTokenPage = {
                uriHandler.openUri("https://huggingface.co/settings/tokens")
            },
            onDismiss = { showHuggingFaceDialog = false },
            onDownload = {
                showHuggingFaceDialog = false
                onUseModel()
            }
        )
    }

    if (showDeleteDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCCCCCC),
            title = { Text("Delete model?") },
            text = {
                Text("This will completely remove ${model.displayName} from your device storage.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteModel()
                    }
                ) {
                    Text("Delete", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Color(0xFF888888))
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 14.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top row: icon + name + badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrimaryGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = model.family,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF888888)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (isRecommended) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryGreen.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "Recommended",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryGreen
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                // Size badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF2A2A2A)
                ) {
                    Text(
                        text = model.sizeLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFCCCCCC)
                    )
                }
                if (isDownloaded) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryGreen.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Ready",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryGreen
                        )
                    }
                }
                if (model.requiresHuggingFaceAccess) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF3A321F)
                    ) {
                        Text(
                            text = "HF token",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFFFD27A)
                        )
                    }
                }
            }

            // Best-for labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                model.useCases.take(3).forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFF2A2A2A)
                    ) {
                        Text(
                            text = "Best for $tag",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCCCCCC)
                        )
                    }
                }
            }

            // Description
            if (model.description.isNotBlank()) {
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888888),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FriendlyMetric("Speed", speedLabelFor(model), Modifier.weight(1f))
                FriendlyMetric("Quality", qualityLabelFor(model), Modifier.weight(1f))
                FriendlyMetric("Storage", storageLabelFor(model), Modifier.weight(1f))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = deviceFit.backgroundColor
            ) {
                Text(
                    text = "Device fit: ${deviceFit.message}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = deviceFit.textColor
                )
            }

            // Context & Quantization info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Context",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = model.contextLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCCCCCC)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Quantization",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = model.quantizationLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCCCCCC)
                    )
                }
            }

            // Progress section
            if (isActive || isDownloaded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { if (isDownloaded) 1f else animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = PrimaryGreen,
                        trackColor = Color(0xFF333333)
                    )
                    if (downloadState is DownloadState.Error) {
                        Text(
                            text = downloadState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (isActive && downloadState is DownloadState.Downloading) {
                        Text(
                            text = "${formatMb(downloadState.downloadedMB)} MB of ${formatMb(downloadState.totalMB)} MB",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF888888)
                        )
                    }
                }
            }

            // Action button
            Button(
                onClick = {
                    if (downloadState is DownloadState.Error && model.requiresHuggingFaceAccess) showHuggingFaceDialog = true
                    else if (downloadState is DownloadState.Error) onRetry()
                    else if (!isDownloaded && model.requiresHuggingFaceAccess) showHuggingFaceDialog = true
                    else if (!isDownloaded) showDownloadDialog = true
                    else onUseModel()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isActive,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDownloaded) PrimaryGreen else Color(0xFF2A2A2A),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF2A2A2A).copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = if (isDownloaded) Icons.Rounded.PlayArrow else Icons.Rounded.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        downloadState is DownloadState.Error -> "Retry Download"
                        isActive -> "Downloading..."
                        isDownloaded -> "Use Model"
                        else -> "Download (${model.sizeLabel})"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (isDownloaded) {
                TextButton(
                    onClick = { showDeleteDialog = true },
                    enabled = !isActive,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete Model",
                        color = ErrorRed,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun HuggingFaceAccessDialog(
    model: ModelOption,
    token: String,
    onTokenChange: (String) -> Unit,
    onOpenLicense: () -> Unit,
    onOpenTokenPage: () -> Unit,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFCCCCCC),
        title = { Text("Hugging Face access") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${model.displayName} is protected by its creator. You only need to do this once.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "1. Open the license page and accept access.\n2. Create a free Read token.\n3. Paste the token here.\n4. Tap Download.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBDBDBD)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onOpenLicense, enabled = model.licenseUrl != null) {
                        Text("Open license", color = PrimaryGreen)
                    }
                    TextButton(onClick = onOpenTokenPage) {
                        Text("Create token", color = PrimaryGreen)
                    }
                }
                OutlinedTextField(
                    value = token,
                    onValueChange = onTokenChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Paste hf_ token") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDownload,
                enabled = token.trim().startsWith("hf_")
            ) {
                Text("Download", color = PrimaryGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF888888))
            }
        }
    )
}

private fun formatMb(value: Float): String = String.format(Locale.US, "%.0f", value)

private data class DeviceFit(
    val message: String,
    val textColor: Color,
    val backgroundColor: Color
)

@Composable
private fun FriendlyMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF252525)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A8A8A)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

private fun speedLabelFor(model: ModelOption): String = when {
    model.sizeMb <= 700f -> "Fast"
    model.sizeMb <= 1800f -> "Medium"
    else -> "Slow"
}

private fun qualityLabelFor(model: ModelOption): String = when {
    model.sizeMb >= 3000f -> "Best"
    model.sizeMb >= 1200f -> "Better"
    else -> "Good"
}

private fun storageLabelFor(model: ModelOption): String = when {
    model.sizeMb <= 700f -> "Low"
    model.sizeMb <= 1800f -> "Medium"
    else -> "High"
}

private fun evaluateDeviceFit(
    model: ModelOption,
    availableRamGb: Float,
    availableStorageGb: Float
): DeviceFit {
    val requiresRamGb = model.sizeMb / 1000f
    val storageNeedsGb = (model.sizeMb / 1000f) * 1.2f

    return when {
        availableRamGb < requiresRamGb * 0.8f || availableStorageGb < storageNeedsGb -> {
            DeviceFit(
                message = "May be slow or fail on this device",
                textColor = Color(0xFFFF9E9E),
                backgroundColor = Color(0xFF3A1F1F)
            )
        }
        availableRamGb < requiresRamGb * 1.2f -> {
            DeviceFit(
                message = "Should work, but choose smaller model for speed",
                textColor = Color(0xFFFFD27A),
                backgroundColor = Color(0xFF3A321F)
            )
        }
        else -> {
            DeviceFit(
                message = "Works well on your device",
                textColor = PrimaryGreen,
                backgroundColor = PrimaryGreen.copy(alpha = 0.14f)
            )
        }
    }
}

private fun recommendModel(
    models: List<ModelOption>,
    selectedUseCase: String,
    availableRamGb: Float,
    availableStorageGb: Float
): ModelOption? {
    if (models.isEmpty()) return null

    val preferredUseCase = when (selectedUseCase) {
        "All" -> "Text"
        else -> selectedUseCase
    }
    val useCasePool = models.filter { preferredUseCase in it.useCases }.ifEmpty { models }
    val accessPool = useCasePool.filterNot { it.requiresHuggingFaceAccess }.ifEmpty { useCasePool }

    val viable = accessPool.filter { model ->
        val sizeGb = model.sizeMb / 1000f
        sizeGb <= availableStorageGb * 0.75f
    }
    val pool = if (viable.isNotEmpty()) viable else accessPool

    return pool.minByOrNull { model ->
        val sizeGb = model.sizeMb / 1000f
        val sizePenalty = max(0f, sizeGb - (availableRamGb * 0.8f))
        val versatilityBonus = when {
            selectedUseCase == "All" && "Text" in model.useCases -> -0.8f
            "Image" in model.useCases -> -0.6f
            "Code" in model.useCases -> -0.3f
            else -> 0f
        }
        sizePenalty + (model.sizeMb / 2000f) + versatilityBonus
    }
}
