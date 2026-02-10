package com.mursaline.kaironex.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import com.mursaline.kaironex.ui.theme.KaironexColors

@Composable
fun VoiceOrb(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")
    
    // Base breathing animation
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.2f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbBreath"
    )

    val orbColor = if (isListening) KaironexColors.GeminiBlurple else KaironexColors.ElectricBlue

    Box(
        modifier = modifier
            .size(120.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Outer Glow
        if (isListening) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(breathScale * 1.2f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(orbColor.copy(alpha = 0.5f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
        }

        // Core Orb
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(breathScale)
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


