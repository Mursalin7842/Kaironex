package com.mursaline.kaironex.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.brain.GeminiReasoningEngine.ConnectionState
import com.mursaline.kaironex.ui.theme.KaironexColors

@Composable
fun VoiceOrb(
    connectionState: ConnectionState,
    audioRms: Float = 0f, // New param
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")
    
    // Base breathing animation
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (connectionState is ConnectionState.Connected) 1.05f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbBreath"
    )

    // Dynamic Voice Reactivity (RMS) - Multiplier
    // RMS is typically 0.0 to 0.5. Scale up to 1.5x
    val voiceScale = 1f + (audioRms * 2.5f).coerceIn(0f, 0.5f)
    
    val finalScale = breathScale * voiceScale

    // Glow Animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbGlow"
    )

    val orbColor = when (connectionState) {
        ConnectionState.Connected -> KaironexColors.GeminiBlurple
        ConnectionState.Connecting -> KaironexColors.ElectricBlue
        is ConnectionState.Error -> KaironexColors.AlertRed
        ConnectionState.Disconnected -> Color.Gray
    }

    Box(
        modifier = modifier
            .size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Glow
        if (connectionState is ConnectionState.Connected || connectionState is ConnectionState.Connecting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(finalScale * 1.2f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(orbColor.copy(alpha = glowAlpha), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
        }

        // Core Orb
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(finalScale)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f),
                            orbColor
                        )
                    ),
                    CircleShape
                )
        )
    }
}
