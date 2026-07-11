package com.example.chatapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.chatapp.ui.theme.ErrorRed
import com.example.chatapp.ui.theme.PrimaryGreen
import com.example.chatapp.ui.theme.chipBackground
import com.example.chatapp.ui.theme.isDark
import com.example.chatapp.ui.theme.subtleBorder
import com.example.chatapp.ui.theme.textHint

@Composable
fun MessageInput(
    modifier: Modifier = Modifier,
    isGenerating: Boolean,
    enabled: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
    ) {
        val compact = maxWidth < 340.dp
        val actionSize = if (compact) 38.dp else 44.dp
        val barHeight = if (compact) 46.dp else 52.dp
        val corner = if (compact) 23.dp else 26.dp

        val input = text.text
        val hasText = input.trim().isNotEmpty()
        val canSend = hasText && enabled && !isGenerating

        val sendButtonColor by animateColorAsState(
            targetValue = when {
                isGenerating -> ErrorRed
                canSend -> PrimaryGreen
                else -> colors.chipBackground
            },
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "send_btn_color"
        )

        val sendIconColor = when {
            isGenerating || canSend -> androidx.compose.ui.graphics.Color.White
            else -> colors.textHint
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = barHeight)
                .background(
                    color = if (colors.isDark) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.06f) else colors.surface,
                    shape = RoundedCornerShape(corner)
                )
                .border(1.dp, colors.subtleBorder, RoundedCornerShape(corner))
                .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    enabled = enabled,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val value = text.text.trim()
                            if (value.isNotEmpty() && enabled && !isGenerating) {
                                onSend(value)
                                text = TextFieldValue("")
                            }
                        }
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(PrimaryGreen),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.text.isEmpty()) {
                                Text(
                                    text = "Ask InnoAI",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.textHint
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                IconButton(
                    onClick = {
                        if (isGenerating) {
                            onStop()
                        } else {
                            val value = text.text.trim()
                            if (value.isNotEmpty() && enabled) {
                                onSend(value)
                                text = TextFieldValue("")
                            }
                        }
                    },
                    modifier = Modifier
                        .size(actionSize)
                        .background(color = sendButtonColor, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = when {
                            isGenerating -> Icons.Rounded.Stop
                            hasText -> Icons.Rounded.ArrowUpward
                            else -> Icons.Rounded.GraphicEq
                        },
                        contentDescription = if (isGenerating) "Stop generation" else "Send message",
                        tint = sendIconColor,
                        modifier = Modifier.size(if (compact) 18.dp else 20.dp)
                    )
                }
            }
        }
    }
}
