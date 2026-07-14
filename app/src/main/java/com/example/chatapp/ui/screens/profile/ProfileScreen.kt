package com.example.chatapp.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.chatapp.ui.components.DockTab
import com.example.chatapp.ui.components.FloatingBottomDock
import com.example.chatapp.ui.theme.DarkBackground
import com.example.chatapp.ui.theme.PrimaryGreen

@Composable
fun ProfileScreen(
    onChatClick: (() -> Unit)? = null,
    onModelsClick: (() -> Unit)? = null,
    onGuideClick: (() -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profileStats = viewModel.stats.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val displayName = userName.ifBlank { "InnoAI User" }
    val avatarInitial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "I"

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalPad = when {
            maxWidth >= 900.dp -> 32.dp
            maxWidth >= 600.dp -> 24.dp
            maxWidth >= 360.dp -> 18.dp
            else -> 12.dp
        }
        val maxContentWidth = if (maxWidth >= 900.dp) 900.dp else 760.dp

        Scaffold(
            containerColor = DarkBackground,
            bottomBar = {
                FloatingBottomDock(
                    selectedTab = DockTab.PROFILE,
                    onChatClick = onChatClick,
                    onModelsClick = onModelsClick
                )
            }
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
                        .padding(horizontal = horizontalPad, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatarInitial,
                    style = MaterialTheme.typography.displayMedium,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "On-device user",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF888888)
            )

            Spacer(modifier = Modifier.height(24.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            val activityManager = remember(context) {
                context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            }
            val memoryInfo = remember(activityManager) {
                android.app.ActivityManager.MemoryInfo().apply { activityManager.getMemoryInfo(this) }
            }

            val totalRamGB = String.format(java.util.Locale.US, "%.1f GB", memoryInfo.totalMem / (1024.0 * 1024 * 1024))
            val availRamGB = String.format(java.util.Locale.US, "%.1f GB", memoryInfo.availMem / (1024.0 * 1024 * 1024))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.03f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBlock(value = totalRamGB, label = "Total RAM")
                    StatBlock(value = availRamGB, label = "Free RAM")
                    StatBlock(value = "${profileStats.value.downloadedModels}", label = "Models")
                    StatBlock(value = "${profileStats.value.conversations}", label = "Chats")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Usage",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    ProfileMetricRow("Total messages", "${profileStats.value.messages}")
                    ProfileMetricRow("User messages", "${profileStats.value.userMessages}")
                    ProfileMetricRow("AI messages", "${profileStats.value.aiMessages}")
                    ProfileMetricRow("Conversations", "${profileStats.value.conversations}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                    .clickable(enabled = onGuideClick != null) { onGuideClick?.invoke() },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(42.dp).background(PrimaryGreen.copy(alpha = 0.13f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = PrimaryGreen)
                    }
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text("App guide", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Learn models, chat, and privacy", style = MaterialTheme.typography.bodySmall, color = Color(0xFF888888))
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Open guide", tint = PrimaryGreen)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Local-first privacy",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Chats and model files stay on-device. No cloud sync is required.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF888888),
                        textAlign = TextAlign.Center
                    )
                }
            }

                    Spacer(modifier = Modifier.height(24.dp))

                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { uriHandler.openUri("https://dailyorbitstudio.space") }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(
                                id = com.example.chatapp.R.drawable.dos
                            ),
                            contentDescription = "DailyOrbit Studio",
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(72.dp)
                        )
                        Text(
                            text = "Developed by DailyOrbit Studio",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "dailyorbitstudio.space",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF888888)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StatBlock(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF888888)
        )
    }
}

@Composable
private fun ProfileMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF888888)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}
