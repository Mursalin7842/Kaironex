package com.mursaline.kaironex.features.agents

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mursaline.kaironex.brain.AgentCall
import com.mursaline.kaironex.brain.AgentCallPriority
import com.mursaline.kaironex.brain.AgentCallStatus
import com.mursaline.kaironex.brain.AgentType
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * ============================================================
 * INCOMING AGENT CALL OVERLAY
 * ============================================================
 *
 * Full-screen "Agent is calling you" UI that appears when an
 * autonomous agent decides to reach out to the user.
 *
 * This is the KEY differentiator of Kaironex:
 * Agents act on their own will, not just when asked.
 *
 * Design: Phone-call-style with pulsing animation,
 * agent identity, reason for call, accept/dismiss buttons.
 */

@Composable
fun IncomingAgentCallOverlay(
    call: AgentCall,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "incomingCallLoop")

    // 1. Background Animation (Pulse Gradient)
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientShift"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarPulse"
    )

    val ringScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f, // Larger ripple
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring1"
    )

    val ringAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1"
    )

    val ringScale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.2f, // Larger ripple
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOut, delayMillis = 600),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring2"
    )

    val ringAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOut, delayMillis = 600),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha2"
    )

    // Agent Theme Colors
    val agentColor = when (call.agent) {
        AgentType.STUDY -> Color(0xFF4285F4)
        AgentType.VITALITY -> Color(0xFF34A853)
        AgentType.CAMPAIGN -> Color(0xFFFBBC04)
        AgentType.RADIUS -> Color(0xFF9C27B0)
        AgentType.SUPERVISOR -> KaironexColors.GeminiBlurple
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black), // Fallback
        contentAlignment = Alignment.Center
    ) {
        // --- Layer 1: Animated Background Gradient ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D1117),
                            agentColor.copy(alpha = 0.15f), // Subtle agent tint
                            Color(0xFF0D1117)
                        )
                    )
                )
        )

        // --- Layer 2: Main Content ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(vertical = 48.dp, horizontal = 24.dp)
        ) {
            // Top Section: Agent Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (call.priority == AgentCallPriority.URGENT) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFEA4335).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEA4335).copy(alpha = 0.5f))
                    ) {
                        Text(
                            "⚡ CRITICAL INTERVENTION",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = Color(0xFFEA4335),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                } else {
                    Spacer(Modifier.height(48.dp))
                }

                // Avatar Area
                Box(contentAlignment = Alignment.Center) {
                    // Ripples
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(ringScale1)
                            .clip(CircleShape)
                            .border(1.dp, agentColor.copy(alpha = ringAlpha1), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(ringScale2)
                            .clip(CircleShape)
                            .border(1.dp, agentColor.copy(alpha = ringAlpha2), CircleShape)
                    )

                    // Main Avatar
                    Surface(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(pulseScale)
                            .shadow(24.dp, CircleShape, spotColor = agentColor),
                        shape = CircleShape,
                        color = agentColor
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = call.agent.emoji,
                                style = MaterialTheme.typography.displayLarge
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text = call.agent.label,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = "is calling you...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Middle Section: Context / Reason
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.08f)) // Glassy
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "\"${call.reason}\"",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    if (call.suggestedAction != null) {
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = agentColor, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = call.suggestedAction,
                                style = MaterialTheme.typography.labelLarge,
                                color = agentColor
                            )
                        }
                    }
                }
            }

            // Bottom Section: Actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onDismiss,
                        containerColor = Color(0xFFEA4335).copy(alpha = 0.2f),
                        contentColor = Color(0xFFEA4335),
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Decline", modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Decline", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.6f))
                }

                // Accept
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onAccept,
                        containerColor = Color(0xFF34A853),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(88.dp).shadow(16.dp, CircleShape, spotColor = Color(0xFF34A853))
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Accept", modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Accept", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
