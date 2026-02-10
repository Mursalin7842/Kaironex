package com.mursaline.kaironex.features.dashboard.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.features.zones.CortexState
import com.mursaline.kaironex.features.zones.PressureLevel
import com.mursaline.kaironex.ui.components.KxBadge
import com.mursaline.kaironex.ui.components.KxBadgeVariant
import com.mursaline.kaironex.ui.theme.KaironexColors
import kotlin.math.sin

/**
 * The Cortex Hero Card - The main entry point to the Study Room
 * This is the "Safe Space" for deep work, prominently displayed at the top of the dashboard.
 * Based on the "Hero + Support" dashboard strategy.
 */
@Composable
fun CortexHeroCard(
    cortexState: CortexState,
    onEnterFlow: () -> Unit,
    modifier: Modifier = Modifier,
    isMobile: Boolean = false
) {
    val cardHeight = if (isMobile) 180.dp else 220.dp

    // Determine pressure level
    val pressureLevel = when {
        cortexState.pressure >= 0.9f -> PressureLevel.Peak
        cortexState.pressure >= 0.6f -> PressureLevel.High
        cortexState.pressure >= 0.3f -> PressureLevel.Medium
        else -> PressureLevel.Low
    }

    // Badge variant based on activity
    val badgeVariant = if (cortexState.isActive) KxBadgeVariant.Success else KxBadgeVariant.Neutral
    val badgeText = if (cortexState.isActive) "CORTEX ACTIVE" else "READY TO FOCUS"

    Surface(
        onClick = onEnterFlow,
        shape = RoundedCornerShape(24.dp),
        color = KaironexColors.InkBlack,
        shadowElevation = 12.dp,
        modifier = modifier.fillMaxWidth().height(cardHeight)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Animated Pressure Background
            PressureWaveBackground(
                pressure = cortexState.pressure,
                pressureColor = pressureLevel.color
            )

            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                KaironexColors.InkBlack.copy(alpha = 0.7f),
                                KaironexColors.InkBlack.copy(alpha = 0.95f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(if (isMobile) 16.dp else 24.dp)
            ) {
                KxBadge(text = badgeText, variant = badgeVariant)

                Spacer(Modifier.height(if (isMobile) 8.dp else 12.dp))

                Text(
                    text = if (cortexState.isActive) "Resume: ${cortexState.currentSubject}" else "Start Your Focus Session",
                    style = if (isMobile) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                if (cortexState.currentTopic.isNotEmpty()) {
                    Text(
                        text = cortexState.currentTopic,
                        style = MaterialTheme.typography.bodyMedium,
                        color = KaironexColors.CloudGray,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Pressure indicator text
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(pressureLevel.color)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (cortexState.upcomingDeadlines > 0)
                            "${cortexState.upcomingDeadlines} Deadlines approaching • ${pressureLevel.label} pressure"
                        else
                            "No immediate deadlines • ${pressureLevel.label} pressure",
                        style = MaterialTheme.typography.bodySmall,
                        color = KaironexColors.SlateGray
                    )
                }
            }

            // Enter Flow FAB
            FloatingActionButton(
                onClick = onEnterFlow,
                containerColor = KaironexColors.ElectricBlue,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(if (isMobile) 12.dp else 20.dp)
                    .size(if (isMobile) 48.dp else 56.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Enter Flow",
                    modifier = Modifier.size(if (isMobile) 24.dp else 28.dp)
                )
            }

            // Study Streak Badge (top right)
            if (cortexState.studyStreak > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = KaironexColors.AttentionOrange.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(if (isMobile) 12.dp else 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${cortexState.studyStreak} day streak",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = KaironexColors.InkBlack
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated pressure wave background visualization
 * Shows the cognitive load as animated waves
 */
@Composable
fun PressureWaveBackground(
    pressure: Float,
    pressureColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition()

    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val waveAmplitude by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val baseY = height * 0.6f
        val amplitude = height * 0.15f * pressure * waveAmplitude

        // Draw multiple wave layers
        for (layer in 0..2) {
            val layerAlpha = 0.3f - (layer * 0.1f)
            val layerOffset = layer * 0.5f

            val path = Path().apply {
                moveTo(0f, baseY)

                var x = 0f
                while (x <= width) {
                    val y = baseY + sin((x / width * 4 * Math.PI + wavePhase + layerOffset).toDouble()).toFloat() * amplitude
                    lineTo(x, y)
                    x += 4f
                }

                lineTo(width, height)
                lineTo(0f, height)
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        pressureColor.copy(alpha = layerAlpha),
                        pressureColor.copy(alpha = layerAlpha * 0.3f)
                    )
                )
            )
        }

        // Draw pressure dots
        val dotCount = (pressure * 15).toInt().coerceIn(3, 15)
        for (i in 0 until dotCount) {
            val dotX = width * (i + 1) / (dotCount + 1)
            val dotY = baseY + sin((dotX / width * 4 * Math.PI + wavePhase).toDouble()).toFloat() * amplitude * 0.5f

            drawCircle(
                color = pressureColor.copy(alpha = 0.6f),
                radius = 4f + (pressure * 4f),
                center = Offset(dotX, dotY - 20f)
            )
        }
    }
}
