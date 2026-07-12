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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image as ImageIcon
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.chatapp.data.model.ChatMessage
import com.example.chatapp.data.model.MessageRole
import com.example.chatapp.data.model.MessageType
import com.example.chatapp.domain.AssistantResponseCleaner
import com.example.chatapp.ui.theme.PrimaryGreen
import com.example.chatapp.ui.theme.PlexMono
import com.example.chatapp.ui.theme.bubbleAssistant
import com.example.chatapp.ui.theme.isDark
import com.example.chatapp.ui.theme.textHint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBubble(
    message: ChatMessage,
    onRetry: (ChatMessage) -> Unit = {}
) {
    val isUser = message.role == MessageRole.USER
    val clipboardManager = LocalClipboardManager.current
    val displayContent = remember(message.content, message.role) {
        if (message.role == MessageRole.AI) {
            AssistantResponseCleaner.clean(message.content)
        } else {
            message.content
        }
    }
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
        val adaptiveMaxBubble = (maxWidth * 0.82f).coerceIn(220.dp, 600.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = if (isUser) {
                        "Your message: ${displayContent.ifBlank { message.type.name.lowercase() }}"
                    } else {
                        "Assistant message: ${displayContent.ifBlank { if (message.isStreaming) "generating" else message.type.name.lowercase() }}"
                    }
                },
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
                            .background(PrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AppLogo(modifier = Modifier.size(14.dp), contentDescription = null)
                    }
                    Text(
                        text = if (message.isStreaming) "INNOAI  •  GENERATING" else "INNOAI  •  ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (message.isStreaming) PrimaryGreen.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (message.isStreaming) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }

            Column(
                modifier = Modifier
                    .then(if (isUser) Modifier.widthIn(max = adaptiveMaxBubble) else Modifier.fillMaxWidth())
                    .background(
                        brush = if (isUser) {
                            Brush.linearGradient(
                                colors = listOf(
                                    PrimaryGreen,
                                    PrimaryGreen.copy(alpha = 0.92f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent
                                )
                            )
                        },
                        shape = if (isUser) {
                            RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                        } else {
                            RoundedCornerShape(0.dp)
                        }
                    )
                    .padding(horizontal = if (isUser) 14.dp else 0.dp, vertical = if (isUser) 12.dp else 4.dp)
            ) {
                when (message.type) {
                    MessageType.IMAGE -> ImageMessageContent(message)
                    MessageType.AUDIO -> AudioMessageContent(message)
                    MessageType.TEXT -> {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            SelectionContainer(modifier = Modifier.weight(1f, fill = false)) {
                                MarkdownMessageText(
                                    isUser = isUser,
                                    text =if (displayContent.isNotBlank() && AssistantResponseCleaner.hasVisibleAnswer(displayContent)) {
                                        displayContent
                                    } else {
                                        if (!message.isStreaming && message.role == MessageRole.AI) {
                                            "I couldn't produce a final answer. Please try again."
                                        } else {
                                            ""
                                        }
                                    }
                                )
                            }
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

            Row(
                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.textHint
                )
                if (!message.isStreaming && displayContent.isNotBlank()) {
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(displayContent)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy message",
                            tint = MaterialTheme.colorScheme.textHint,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                if (!isUser && !message.isStreaming) {
                    IconButton(
                        onClick = { onRetry(message) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Retry message",
                            tint = MaterialTheme.colorScheme.textHint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownMessageText(text: String, isUser: Boolean = false) {
    val blocks = remember(text) { splitMarkdownBlocks(text) }
    val darkContext = isUser || MaterialTheme.colorScheme.isDark
    val bodyColor = if (isUser) Color(0xFF171006) else MaterialTheme.colorScheme.onSurface
    val codeBackground = if (darkContext) Color.Black.copy(alpha = 0.28f) else Color(0xFFF1F5F9)
    val codeColor = if (darkContext) Color(0xFFEAEAEA) else Color(0xFF334155)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            if (block.isCode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = codeBackground
                ) {
                    Text(
                        text = block.text,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = PlexMono),
                        color = PrimaryGreen
                    )
                }
            } else {
                Text(
                    text = inlineMarkdown(block.text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = bodyColor
                )
            }
        }
    }
}

private data class MarkdownBlock(
    val text: String,
    val isCode: Boolean
)

private fun splitMarkdownBlocks(text: String): List<MarkdownBlock> {
    if (text.isBlank()) return emptyList()
    val blocks = mutableListOf<MarkdownBlock>()
    val fence = Regex("""```(?:[A-Za-z0-9_-]+)?\n?([\s\S]*?)```""")
    var cursor = 0
    fence.findAll(text).forEach { match ->
        val before = text.substring(cursor, match.range.first).trim()
        if (before.isNotBlank()) blocks += MarkdownBlock(before, isCode = false)
        blocks += MarkdownBlock(match.groupValues[1].trim(), isCode = true)
        cursor = match.range.last + 1
    }
    val after = text.substring(cursor).trim()
    if (after.isNotBlank()) blocks += MarkdownBlock(after, isCode = false)
    return blocks
}

private fun inlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var index = 0
    val token = Regex("""(`[^`]+`|\*\*[^*]+\*\*|__[^_]+__|\*[^*]+\*|_[^_]+_)""")
    token.findAll(text).forEach { match ->
        append(text.substring(index, match.range.first))
        val raw = match.value
        when {
            raw.startsWith("`") -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color.White.copy(alpha = 0.12f))) {
                append(raw.removeSurrounding("`"))
            }
            raw.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(raw.removeSurrounding("**"))
            }
            raw.startsWith("__") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(raw.removeSurrounding("__"))
            }
            raw.startsWith("*") -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(raw.removeSurrounding("*"))
            }
            raw.startsWith("_") -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(raw.removeSurrounding("_"))
            }
            else -> append(raw)
        }
        index = match.range.last + 1
    }
    append(text.substring(index))
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
