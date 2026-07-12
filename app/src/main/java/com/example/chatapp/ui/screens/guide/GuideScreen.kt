package com.example.chatapp.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chatapp.ui.theme.PrimaryGreen
import com.example.chatapp.ui.theme.subtleBorder
import com.example.chatapp.ui.components.AppLogo

@Composable
fun GuideScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = PrimaryGreen)
                }
                AppLogo(modifier = Modifier.size(26.dp), contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Guide", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("GET STARTED", style = MaterialTheme.typography.labelMedium, color = PrimaryGreen)
            Text("Use InnoAI entirely on your device", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Download a local model once, then chat privately without sending conversations to a cloud service.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            GuideStep(Icons.Rounded.Download, "1", "Choose a model", "Open Models, choose a model suitable for your device, and wait for the download to finish.")
            GuideStep(Icons.Rounded.AutoAwesome, "2", "Load the model", "Tap Use Model. InnoAI will prepare the local engine before opening your chat.")
            GuideStep(Icons.Rounded.ChatBubbleOutline, "3", "Start chatting", "Type a message and tap the amber arrow. Use the stop button whenever you want to end generation.")
            GuideStep(Icons.Rounded.Settings, "4", "Tune responses", "Settings lets you change the model, compute backend, temperature, maximum tokens, and history tools.")
            GuideStep(Icons.Rounded.Lock, "5", "Your data stays local", "Chats, attachments, preferences, and downloaded model files remain stored on this device.")
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GuideStep(icon: ImageVector, number: String, title: String, description: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.subtleBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(42.dp).background(PrimaryGreen.copy(alpha = 0.13f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("STEP $number", style = MaterialTheme.typography.labelSmall, color = PrimaryGreen)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
