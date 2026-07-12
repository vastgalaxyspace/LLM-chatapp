package com.example.chatapp.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chatapp.R
import com.example.chatapp.ui.theme.PrimaryGreen
import com.example.chatapp.ui.theme.SignalAmberBright
import com.example.chatapp.ui.theme.textHint

@Composable
fun BrandLockup(
    title: String = "InnoAI",
    subtitle: String? = "Your private AI assistant",
    modifier: Modifier = Modifier,
    large: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (large) 16.dp else 10.dp)
    ) {
        BrandMark(large = large)
        Text(
            text = title,
            style = if (large) MaterialTheme.typography.displayLarge else MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.ExtraBold
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.textHint,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = "InnoAI logo",
) {
    Image(
        painter = painterResource(id = R.drawable.innoailogomain),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(PrimaryGreen),
    )
}

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    large: Boolean = true
) {
    val outerSize = if (large) 112.dp else 48.dp
    val innerSize = if (large) 72.dp else 34.dp
    val iconSize = if (large) 36.dp else 18.dp
    val outerShape = RoundedCornerShape(if (large) 28.dp else 12.dp)
    val innerShape = RoundedCornerShape(if (large) 22.dp else 10.dp)

    // Subtle breathing glow animation
    val transition = rememberInfiniteTransition(label = "brand_glow")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .size(outerSize)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrimaryGreen.copy(alpha = glowAlpha * 0.7f),
                        PrimaryGreen.copy(alpha = 0.06f),
                        Color.Transparent
                    )
                ),
                shape = outerShape
            )
            .border(
                1.5.dp,
                PrimaryGreen.copy(alpha = glowAlpha),
                outerShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(innerSize)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SignalAmberBright.copy(alpha = 0.22f),
                            PrimaryGreen.copy(alpha = 0.09f)
                        )
                    ),
                    shape = innerShape
                ),
            contentAlignment = Alignment.Center
        ) {
            AppLogo(
                modifier = Modifier
                    .size(iconSize + 8.dp)
                    .padding(2.dp),
            )
        }
    }
}

@Composable
fun StatusEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    glowing: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(PrimaryGreen, CircleShape)
                .then(
                    if (glowing) {
                        Modifier.border(4.dp, PrimaryGreen.copy(alpha = 0.15f), CircleShape)
                    } else {
                        Modifier
                    }
                )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = PrimaryGreen
        )
    }
}
