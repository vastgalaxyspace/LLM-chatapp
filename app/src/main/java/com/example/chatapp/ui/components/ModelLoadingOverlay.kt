package com.example.chatapp.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chatapp.ui.theme.DarkBackground
import com.example.chatapp.ui.theme.PrimaryGreen
import com.example.chatapp.ui.theme.PrimaryGreenLight

@Composable
fun ModelLoadingOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "loading_progress")
    val sweep by transition.animateFloat(
        initialValue = 30f,
        targetValue = 330f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_sweep"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val compact = maxWidth < 360.dp
        val maxContentWidth = if (maxWidth >= 900.dp) 760.dp else 620.dp
        val heroSize = if (compact) 220.dp else 300.dp

        Box(
            modifier = Modifier
                .size(heroSize)
                .align(Alignment.TopCenter)
                .background(PrimaryGreen.copy(alpha = 0.06f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 14.dp else 24.dp)
                .widthIn(max = maxContentWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.size(if (compact) 40.dp else 72.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                BrandMark(modifier = Modifier.size(if (compact) 110.dp else 140.dp))

                Text(
                    text = "InnoAI",
                    style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "On-device AI",
                    style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF888888)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                LoadingRing(
                    modifier = Modifier.size(64.dp),
                    sweep = sweep
                )

                Text(
                    text = "LOADING MODEL...",
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryGreenLight,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Powered by ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
                Text("G", color = Color(0xFF4285F4), style = MaterialTheme.typography.bodyMedium)
                Text("o", color = Color(0xFFEA4335), style = MaterialTheme.typography.bodyMedium)
                Text("o", color = Color(0xFFFBBC05), style = MaterialTheme.typography.bodyMedium)
                Text("g", color = Color(0xFF4285F4), style = MaterialTheme.typography.bodyMedium)
                Text("l", color = Color(0xFF34A853), style = MaterialTheme.typography.bodyMedium)
                Text("e", color = Color(0xFFEA4335), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "  LiteRT-LM",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF888888)
                )
            }

            Spacer(modifier = Modifier.size(36.dp))
        }
    }
}

@Composable
private fun LoadingRing(
    modifier: Modifier,
    sweep: Float
) {
    Canvas(modifier = modifier) {
        val stroke = 6.dp.toPx()
        val inset = stroke / 2f
        drawArc(
            color = Color(0xFF2A2A2A),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    PrimaryGreen,
                    PrimaryGreenLight,
                    PrimaryGreen
                )
            ),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}
