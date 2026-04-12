package com.example.chatapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.chatapp.ui.theme.DarkBackground
import com.example.chatapp.ui.theme.PrimaryGreen
import com.example.chatapp.ui.theme.WarningAmber as WarningAmberColor

@Composable
fun ErrorStateView(
    message: String,
    onRetry: () -> Unit,
    onRedownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        val horizontalPad = when {
            maxWidth >= 900.dp -> 40.dp
            maxWidth >= 600.dp -> 28.dp
            maxWidth >= 360.dp -> 22.dp
            else -> 14.dp
        }
        val maxContentWidth = if (maxWidth >= 900.dp) 760.dp else 620.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPad)
                .widthIn(max = maxContentWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StatusEyebrow(
                text = "LOCAL MODEL ERROR",
                glowing = false
            )

            Spacer(modifier = Modifier.size(24.dp))

            Box(
                modifier = Modifier
                    .size(92.dp)
                    .background(WarningAmberColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = WarningAmberColor,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.size(26.dp))

            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(12.dp))

            Text(
                text = "Could not start the selected model on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = PrimaryGreen,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(28.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Try Again",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            OutlinedButton(
                onClick = onRedownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF333333)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFBBBBBB)
                )
            ) {
                Text(
                    text = "Re-download Model",
                    modifier = Modifier.padding(vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.size(20.dp))
        }
    }
}
