package com.mursaline.kaironex.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

enum class KxOrbState {
    Idle,
    Active,
    Thinking,
    Speaking
}

@Composable
fun KxOrb(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    state: KxOrbState = KxOrbState.Idle
) {
    val infiniteTransition = rememberInfiniteTransition()

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == KxOrbState.Active || state == KxOrbState.Speaking) 1.1f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (state == KxOrbState.Idle) 0.6f else 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val gradientColors = when (state) {
        KxOrbState.Idle -> listOf(KaironexColors.Indigo200, KaironexColors.Indigo600)
        KxOrbState.Active -> listOf(KaironexColors.Indigo600, KaironexColors.Purple600)
        KxOrbState.Thinking -> listOf(KaironexColors.Purple600, KaironexColors.Rose500)
        KxOrbState.Speaking -> listOf(KaironexColors.Emerald500, KaironexColors.Indigo600)
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(pulseScale)
            .alpha(pulseAlpha)
            .background(
                brush = Brush.radialGradient(
                    colors = gradientColors
                ),
                shape = CircleShape
            )
    )
}
