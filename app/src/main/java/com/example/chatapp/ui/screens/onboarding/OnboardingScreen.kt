package com.example.chatapp.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chatapp.ui.components.BrandMark
import com.example.chatapp.ui.theme.PrimaryGreen
import com.example.chatapp.ui.theme.PrimaryGreenLight
import com.example.chatapp.ui.theme.isDark
import com.example.chatapp.ui.theme.textHint

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val compact = maxWidth < 360.dp
        val horizontalPad = if (compact) 18.dp else 28.dp
        val maxContentWidth = if (maxWidth >= 700.dp) 640.dp else 520.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPad)
                .widthIn(max = maxContentWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(if (compact) 18.dp else 28.dp))

            BrandMark(large = !compact)

            Spacer(modifier = Modifier.height(if (compact) 16.dp else 28.dp))

            Text(
                text = "InnoAI",
                style = (if (compact) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayLarge).copy(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.onBackground,
                            if (MaterialTheme.colorScheme.isDark) PrimaryGreenLight else PrimaryGreen
                        )
                    )
                ),
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(if (compact) 8.dp else 10.dp))

            Text(
                text = "Run private AI chat directly on your device - text, images, and audio notes stay local.",
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(if (compact) 22.dp else 36.dp))

            Column(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                FeatureBullet("100% on-device processing", compact = compact)
                FeatureBullet("llama.cpp powered local GGUF models", compact = compact)
                FeatureBullet("No cloud connection needed", compact = compact)
                FeatureBullet("Private and secure by default", compact = compact)
                FeatureBullet("Works with CPU or supported GPU backend", compact = compact)
            }

            Spacer(modifier = Modifier.height(if (compact) 22.dp else 30.dp))

            Button(
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 52.dp else 56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Get Started",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Offline-first with llama.cpp",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textHint
            )

            Spacer(modifier = Modifier.height(if (compact) 20.dp else 28.dp))
        }
    }
}

@Composable
private fun FeatureBullet(text: String, compact: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(24.dp)
                .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = text,
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
