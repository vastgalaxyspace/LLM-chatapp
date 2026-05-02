package com.example.chatapp.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image as ImageIcon
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chatapp.data.model.ChatMessage
import com.example.chatapp.data.model.MessageRole
import com.example.chatapp.data.model.MessageType
import com.example.chatapp.ui.theme.PrimaryGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == MessageRole.USER
    val transition = rememberInfiniteTransition(label = "streaming_cursor")
    val cursorAlpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // Responsive max width: 85% of screen, clamped to min 220 and max 600
        val adaptiveMaxBubble = (maxWidth * 0.85f).coerceIn(220.dp, 600.dp)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (!isUser) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // AI avatar badge
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Hub,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = if (message.isStreaming) "Generating..." else "InnoAI",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (message.isStreaming) PrimaryGreen.copy(alpha = 0.8f) else Color(0xFF888888),
                        fontWeight = if (message.isStreaming) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = adaptiveMaxBubble)
                    .background(
                        brush = if (isUser) {
                            Brush.linearGradient(
                                colors = listOf(
                                    PrimaryGreen,
                                    PrimaryGreen.copy(alpha = 0.88f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF262626),
                                    Color(0xFF262626)
                                )
                            )
                        },
                        shape = if (isUser) {
                            RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
                        } else {
                            RoundedCornerShape(4.dp, 20.dp, 20.dp, 20.dp)
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                when (message.type) {
                    MessageType.IMAGE -> ImageMessageContent(message)
                    MessageType.AUDIO -> AudioMessageContent(message)
                    MessageType.TEXT -> {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                            if (message.isStreaming) {
                                Text(
                                    text = "|",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = PrimaryGreen,
                                    modifier = Modifier.alpha(cursorAlpha)
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF555555),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

@Composable
private fun ImageMessageContent(message: ChatMessage) {
    val bitmap = remember(message.mediaUri) {
        message.mediaUri?.let { BitmapFactory.decodeFile(it) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = message.content,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp, max = 280.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            MediaFallbackRow(
                icon = Icons.Rounded.ImageIcon,
                title = "Image unavailable"
            )
        }

        if (message.content.isNotBlank()) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun AudioMessageContent(message: ChatMessage) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MediaFallbackRow(
            icon = Icons.Rounded.GraphicEq,
            title = "Audio note",
            subtitle = formatDuration(message.durationMillis)
        )
        Text(
            text = message.content.ifBlank { "Saved locally" },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

@Composable
private fun MediaFallbackRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.68f)
                )
            }
        }
    }
}

private fun formatDuration(durationMillis: Long?): String {
    val totalSeconds = ((durationMillis ?: 0L) / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
