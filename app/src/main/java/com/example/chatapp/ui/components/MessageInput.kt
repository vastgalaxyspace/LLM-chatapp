package com.example.chatapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.chatapp.ui.theme.ErrorRed
import com.example.chatapp.ui.theme.PrimaryGreen

@Composable
fun MessageInput(
    modifier: Modifier = Modifier,
    isGenerating: Boolean,
    isRecording: Boolean = false,
    enabled: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onAttachImage: () -> Unit = {},
    onToggleRecording: () -> Unit = {}
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
    ) {
        val compact = maxWidth < 340.dp
        val actionSize = if (compact) 38.dp else 44.dp
        val leadingSize = if (compact) 44.dp else 50.dp
        val barHeight = if (compact) 46.dp else 52.dp
        val corner = if (compact) 23.dp else 26.dp

        val input = text.text
        val hasText = input.trim().isNotEmpty()
        val canSend = hasText && enabled && !isGenerating
        val plusGap by animateDpAsState(
            targetValue = if (hasText) 10.dp else 6.dp,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "plus_gap"
        )

        val sendButtonColor by animateColorAsState(
            targetValue = when {
                isGenerating -> ErrorRed
                canSend -> PrimaryGreen
                else -> PrimaryGreen
            },
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "send_btn_color"
        )

        val sendIconColor = when {
            isGenerating -> Color.White
            canSend -> Color.White
            else -> Color(0xFFF1F6FF)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(plusGap)
        ) {
            Box(
                modifier = Modifier
                    .size(leadingSize)
                    .background(Color(0x1FFFFFFF), CircleShape)
                    .border(1.dp, Color(0x55FFFFFF), CircleShape)
                    .clickable(enabled = enabled && !isGenerating, onClick = onAttachImage),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Attach",
                    tint = Color(0xFFE6E6E6),
                    modifier = Modifier.size(if (compact) 24.dp else 28.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = barHeight)
                    .background(Color(0x1FFFFFFF), RoundedCornerShape(corner))
                    .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(corner))
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
                            .padding(horizontal = 4.dp, vertical = 8.dp),
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
                        textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                        cursorBrush = SolidColor(Color(0xFF2C9DFF)),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (text.text.isEmpty()) {
                                    Text(
                                        text = "Ask InnoAI",
                                        color = Color(0xCFE0E0E0)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    if (!hasText && !isGenerating) {
                        IconButton(
                            onClick = onToggleRecording,
                            enabled = enabled,
                            modifier = Modifier.size(if (compact) 34.dp else 38.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Rounded.MicOff else Icons.Rounded.KeyboardVoice,
                                contentDescription = if (isRecording) "Stop recording" else "Record audio",
                                tint = if (isRecording) ErrorRed else Color(0xD9E2E2E2),
                                modifier = Modifier.size(if (compact) 20.dp else 22.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (isGenerating) {
                                onStop()
                            } else if (isRecording) {
                                onToggleRecording()
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
                                isGenerating || isRecording -> Icons.Rounded.Stop
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
}
