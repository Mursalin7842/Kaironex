package com.mursaline.kaironex.features.study.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * ============================================================
 * GEMINI INTERVENTION OVERLAY
 * ============================================================
 *
 * The "Call" screen that appears when user drifts away from study.
 *
 * Design:
 * - Full screen immersive dark overlay
 * - Pulsing "incoming call" animation
 * - Kairo avatar with call icon
 * - Options to return or negotiate a break
 *
 * This makes the AI feel like a caring friend, not a surveillance system.
 */

@Composable
fun GeminiInterventionOverlay(
    timeAway: String,
    onResume: () -> Unit,
    onNegotiate: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulsing Animation for the "Call"
    val infiniteTransition = rememberInfiniteTransition()

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        )
    )

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Outer ring animation (expanding ring effect)
            Box(contentAlignment = Alignment.Center) {
                // Ring 1
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(ringScale)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(
                            width = 2.dp,
                            color = KaironexColors.GeminiBlurple.copy(alpha = ringAlpha * 0.3f),
                            shape = CircleShape
                        )
                )

                // Ring 2
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(ringScale * 0.8f)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(
                            width = 2.dp,
                            color = KaironexColors.GeminiBlurple.copy(alpha = ringAlpha * 0.5f),
                            shape = CircleShape
                        )
                )

                // Main Avatar / Orb
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    KaironexColors.ElectricBlue,
                                    KaironexColors.GeminiBlurple
                                )
                            )
                        )
                        .border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Incoming Call",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Title
            Text(
                text = "Kairo is calling...",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                text = "You've been away for $timeAway",
                style = MaterialTheme.typography.bodyLarge,
                color = KaironexColors.SlateGray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Everything okay? Need a break?",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
            ) {
                // Button 1: I'm Back (Green - Primary action)
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KaironexColors.SuccessGreen
                    ),
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("I'm Back", fontWeight = FontWeight.Bold)
                }

                // Button 2: Need a Break (White - Secondary)
                Button(
                    onClick = onNegotiate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Mic, null, tint = KaironexColors.InkBlack)
                    Spacer(Modifier.width(8.dp))
                    Text("Need a Break", color = KaironexColors.InkBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Negotiation Dialog - User explains why they need a break
 */
@Composable
fun NegotiationDialog(
    onGrantBreak: (minutes: Int) -> Unit,
    onResume: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KaironexColors.CanvasWhite,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(KaironexColors.GeminiBlurple),
                    contentAlignment = Alignment.Center
                ) {
                    Text("K", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Kairo Listening...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    "I noticed you left the study zone. No worries! Do you need a quick break, or was it just a distraction?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaironexColors.SlateGray
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    "Choose your break duration:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(12.dp))

                // Break duration options
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BreakDurationChip("2 min", onClick = { onGrantBreak(2) })
                    BreakDurationChip("5 min", onClick = { onGrantBreak(5) })
                    BreakDurationChip("10 min", onClick = { onGrantBreak(10) })
                    BreakDurationChip("15 min", onClick = { onGrantBreak(15) })
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onResume) {
                Text("It was nothing, I'm back!", color = KaironexColors.ElectricBlue)
            }
        }
    )
}

@Composable
private fun BreakDurationChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = KaironexColors.ElectricBlue.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = KaironexColors.ElectricBlue
        )
    }
}

/**
 * Drifting Warning Badge - Shows when user is starting to drift
 * Appears as a small toast/badge at the top of screen
 */
@Composable
fun DriftingWarningBadge(
    timeAway: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = KaironexColors.AttentionOrange.copy(alpha = alpha),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Away for $timeAway - Come back soon!",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

/**
 * On Break Badge - Shows when user is on approved break
 */
@Composable
fun OnBreakBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = KaironexColors.SuccessGreen,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Coffee,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "On Break - Enjoy!",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}
