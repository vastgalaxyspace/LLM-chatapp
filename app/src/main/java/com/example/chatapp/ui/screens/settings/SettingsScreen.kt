package com.example.chatapp.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chatapp.data.model.ModelCatalog
import com.example.chatapp.ui.theme.DarkBackground
import com.example.chatapp.ui.theme.ErrorRed
import com.example.chatapp.ui.theme.PrimaryGreen
import java.util.Locale

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenModels: (() -> Unit)? = null,
    onOpenChat: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val selectedBackend by viewModel.selectedBackend.collectAsStateWithLifecycle()
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val temperature by viewModel.temperature.collectAsStateWithLifecycle()
    val maxTokens by viewModel.maxTokens.collectAsStateWithLifecycle()
    val huggingFaceToken by viewModel.huggingFaceToken.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteModelDialog by remember { mutableStateOf(false) }
    val currentModel = ModelCatalog.fromId(selectedModel)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPad = when {
            maxWidth >= 900.dp -> 32.dp
            maxWidth >= 600.dp -> 24.dp
            maxWidth >= 360.dp -> 18.dp
            else -> 12.dp
        }
        val maxContentWidth = if (maxWidth >= 900.dp) 920.dp else 760.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DarkBackground,
            topBar = { SettingsTopBar(onNavigateBack) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = maxContentWidth)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = horizontalPad, vertical = 12.dp)
                ) {
            // MODEL section
            SectionLabel("MODEL")
            SettingsCard {
                ModelHeader(currentModel.displayName, currentModel.sizeLabel)
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MetricBlock("CONTEXT", currentModel.contextLabel, Modifier.weight(1f))
                    MetricBlock("QUANTIZE", currentModel.quantizationLabel, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                StatusReadyPill()
                Spacer(modifier = Modifier.height(14.dp))
                ModelSelector(selectedModel, viewModel::updateModel)
                Spacer(modifier = Modifier.height(10.dp))
                HuggingFaceTokenField(
                    value = huggingFaceToken,
                    onValueChange = viewModel::updateHuggingFaceToken
                )
                Spacer(modifier = Modifier.height(10.dp))
                DestructiveRow("Delete Downloaded Model") { showDeleteModelDialog = true }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PERFORMANCE section
            SectionLabel("LLM CONFIGURATION")
            SettingsCard {
                Text("Compute Backend", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF888888))
                Spacer(modifier = Modifier.height(10.dp))
                SegmentedSelector(listOf("CPU", "GPU"), selectedBackend, viewModel::updateBackend)
                Spacer(modifier = Modifier.height(16.dp))

                SliderBlock("Temperature", String.format(Locale.US, "%.1f", temperature), "Precise", "Creative") {
                    Slider(
                        value = temperature,
                        onValueChange = viewModel::updateTemperature,
                        valueRange = 0.1f..1.5f,
                        colors = sliderColors()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                SliderBlock("Max Tokens", maxTokens.toString(), "128", "2048") {
                    Slider(
                        value = maxTokens.toFloat(),
                        onValueChange = { viewModel.updateMaxTokens(it.toInt()) },
                        valueRange = 128f..2048f,
                        colors = sliderColors()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PRIVACY section
            SectionLabel("PRIVACY")
            SettingsCard {
                DestructiveRow("Clear Chat History") { showClearDialog = true }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "PRIVATE  |  ON-DEVICE  |  SECURE",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF444444),
                textAlign = TextAlign.Center
            )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showClearDialog) {
        DestructiveBottomDialog(
            title = "Clear Chat History?",
            description = "This will delete all messages from this device permanently.",
            confirmText = "Clear History",
            onDismiss = { showClearDialog = false },
            onConfirm = {
                viewModel.clearHistory()
                showClearDialog = false
            }
        )
    }

    if (showDeleteModelDialog) {
        DestructiveBottomDialog(
            title = "Delete Downloaded Model?",
            description = "Delete ${currentModel.displayName} from this device. You can download it again later.",
            confirmText = "Delete Model",
            onDismiss = { showDeleteModelDialog = false },
            onConfirm = {
                viewModel.deleteSelectedModel()
                showDeleteModelDialog = false
                onNavigateBack()
            }
        )
    }
}

@Composable
private fun HuggingFaceTokenField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Hugging Face Token",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF888888)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text("Required for gated Gemma models")
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
        )
        Text(
            text = "For Gemma 3 downloads, accept the model license on Hugging Face first, then paste a read token here.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF777777)
        )
    }
}

@Composable
private fun SettingsTopBar(onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141414))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = PrimaryGreen)
        }
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(PrimaryGreen, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryGreen,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E1E1E)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun ModelHeader(name: String, sizeLabel: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(PrimaryGreen.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = PrimaryGreen.copy(alpha = 0.12f)
        ) {
            Text(
                sizeLabel,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryGreen
            )
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF666666))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}

@Composable
private fun StatusReadyPill() {
    Row(
        modifier = Modifier
            .background(PrimaryGreen.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(7.dp).background(PrimaryGreen, CircleShape))
        Text("Status Ready", style = MaterialTheme.typography.labelMedium, color = PrimaryGreen)
    }
}

@Composable
private fun ModelSelector(selectedModel: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Select Model",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF888888)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModelCatalog.all.forEach { model ->
                Surface(
                    modifier = Modifier
                        .clickable { onSelect(model.id) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedModel == model.id) PrimaryGreen else Color(0xFF242424)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            model.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedModel == model.id) Color.White else Color(0xFF888888),
                            maxLines = 1
                        )
                        Text(
                            model.sizeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedModel == model.id) Color.White.copy(alpha = 0.7f) else Color(0xFF666666)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentedSelector(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF242424), RoundedCornerShape(14.dp))
            .padding(3.dp)
    ) {
        options.forEach { option ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(option) },
                shape = RoundedCornerShape(12.dp),
                color = if (option == selected) PrimaryGreen else Color.Transparent
            ) {
                Box(modifier = Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(
                        option,
                        color = if (option == selected) Color.White else Color(0xFF888888),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun SliderBlock(title: String, valueText: String, rangeStart: String, rangeEnd: String, slider: @Composable () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF888888))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PrimaryGreen.copy(alpha = 0.12f)
            ) {
                Text(
                    valueText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryGreen
                )
            }
        }
        slider()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(rangeStart, style = MaterialTheme.typography.labelSmall, color = Color(0xFF555555))
            Text(rangeEnd, style = MaterialTheme.typography.labelSmall, color = Color(0xFF555555))
        }
    }
}

@Composable
private fun DestructiveRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Delete, contentDescription = null, tint = ErrorRed)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = ErrorRed)
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color(0xFF444444))
    }
}

@Composable
private fun DestructiveBottomDialog(
    title: String,
    description: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E1E)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(ErrorRed.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF888888), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onConfirm),
                    shape = RoundedCornerShape(14.dp),
                    color = ErrorRed
                ) {
                    Box(modifier = Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        Text(confirmText, color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDismiss),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333))
                ) {
                    Box(modifier = Modifier.padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        Text("Cancel", color = Color(0xFFBBBBBB), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = PrimaryGreen,
    activeTrackColor = PrimaryGreen,
    inactiveTrackColor = Color(0xFF333333)
)
