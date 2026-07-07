package com.example.chatapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chatapp.ui.theme.PrimaryGreen

enum class DockTab {
    MODELS,
    CHAT,
    PROFILE
}

@Composable
fun FloatingBottomDock(
    selectedTab: DockTab,
    modifier: Modifier = Modifier,
    onChatClick: (() -> Unit)? = null,
    onModelsClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compact = maxWidth < 360.dp
        val iconSize = if (compact) 20.dp else 22.dp
        val verticalPadding = if (compact) 6.dp else 8.dp

        Column(modifier = Modifier.fillMaxWidth()) {
            // Top divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant,
                                Color.Transparent
                            )
                        )
                    )
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = verticalPadding),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockItem(
                        icon = Icons.Rounded.AutoAwesome,
                        label = "Models",
                        selected = selectedTab == DockTab.MODELS,
                        onClick = onModelsClick,
                        compact = compact,
                        iconSize = iconSize
                    )
                    DockItem(
                        icon = Icons.Rounded.ChatBubbleOutline,
                        label = "Chat",
                        selected = selectedTab == DockTab.CHAT,
                        onClick = onChatClick,
                        compact = compact,
                        iconSize = iconSize
                    )
                    DockItem(
                        icon = Icons.Rounded.Person,
                        label = "Profile",
                        selected = selectedTab == DockTab.PROFILE,
                        onClick = onProfileClick,
                        compact = compact,
                        iconSize = iconSize
                    )
                }
            }
        }
    }
}

@Composable
private fun DockItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: (() -> Unit)?,
    compact: Boolean,
    iconSize: androidx.compose.ui.unit.Dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dock_scale"
    )
    val tintColor by animateColorAsState(
        targetValue = if (selected) PrimaryGreen else Color(0xFF777777),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "dock_tint"
    )

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Column(
        modifier = clickableModifier
            .scale(scale)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(iconSize)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tintColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        // Active indicator dot
        if (selected) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(PrimaryGreen, CircleShape)
            )
        } else {
            Box(modifier = Modifier.size(4.dp))
        }
    }
}
