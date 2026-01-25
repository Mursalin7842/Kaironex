package com.mursaline.kaironex.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * THE GENESIS ORB
 * Reactive visualizer for the AI Agent.
 * - Pulses Purple when Agent is thinking/speaking.
 * - Pulses Cyan when User is speaking (Mic active).
 * - Grey when Idle.
 */
@Composable
fun KxOrb(
    isAgentSpeaking: Boolean,
    isUserListening: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Dynamic Animation: Faster pulse when active
    val targetScale = if (isAgentSpeaking || isUserListening) 1.2f else 1.05f
    val animDuration = if (isAgentSpeaking || isUserListening) 600 else 2000

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = targetScale,
        animationSpec = infiniteRepeatable(
            animation = tween(animDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Dynamic Color State
    val coreColor = when {
        isAgentSpeaking -> KaironexColors.NeonPurple
        isUserListening -> KaironexColors.NeonCyan
        else -> Color.Gray
    }

    Box(
        modifier = modifier
            .scale(scale)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(coreColor.copy(alpha = 0.4f), Color.Transparent)
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // The Solid Core
        Box(
            modifier = Modifier
                .size(60.dp) // Core size
                .background(coreColor, CircleShape)
        )
    }
}
