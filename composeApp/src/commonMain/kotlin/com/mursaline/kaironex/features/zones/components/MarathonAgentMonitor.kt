package com.mursaline.kaironex.features.zones.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.features.zones.*
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * ============================================================
 * MARATHON AGENT MONITOR
 * ============================================================
 *
 * This component shows the VISIBLE REASONING of Marathon Agents.
 * Judges will see:
 * - Live thought signatures
 * - Tool call history
 * - Self-correction events
 * - Progress tracking over days
 *
 * This is what separates us from "prompt wrappers" -
 * we show the THINKING, not just the output.
 */

@Composable
fun MarathonAgentMonitor(
    agentState: MarathonAgentState,
    thoughtHistory: List<ThoughtSignatureUI>,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = KaironexColors.InkBlack,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with status
            MarathonAgentHeader(agentState)

            Spacer(Modifier.height(16.dp))

            // Progress bar with time estimate
            ProgressSection(agentState)

            Spacer(Modifier.height(16.dp))

            // Current thought (animated)
            CurrentThoughtBubble(agentState.currentThought, agentState.status)

            Spacer(Modifier.height(16.dp))

            // Stats row
            AgentStatsRow(agentState)

            Spacer(Modifier.height(16.dp))

            // Thought history (scrollable)
            ThoughtHistoryList(thoughtHistory)

            Spacer(Modifier.height(16.dp))

            // Control buttons
            AgentControls(
                status = agentState.status,
                onPause = onPause,
                onResume = onResume,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun MarathonAgentHeader(agentState: MarathonAgentState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Pulsing status indicator
        PulsingStatusDot(agentState.status)

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Marathon Agent",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = agentState.objective,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 1
            )
        }

        // Zone badge
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = agentState.zone.color.copy(alpha = 0.2f)
        ) {
            Text(
                text = agentState.zone.emoji,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun PulsingStatusDot(status: MarathonUIStatus) {
    val infiniteTransition = rememberInfiniteTransition()

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    val color = when (status) {
        MarathonUIStatus.RUNNING, MarathonUIStatus.EXECUTING_TOOL -> KaironexColors.SuccessGreen
        MarathonUIStatus.THINKING -> KaironexColors.ElectricBlue
        MarathonUIStatus.SELF_CORRECTING -> KaironexColors.AttentionOrange
        MarathonUIStatus.PAUSED, MarathonUIStatus.WAITING_FOR_DATA -> KaironexColors.SlateGray
        MarathonUIStatus.COMPLETED -> KaironexColors.SuccessGreen
        MarathonUIStatus.FAILED -> KaironexColors.AlertRed
        else -> KaironexColors.SlateGray
    }

    Box(
        modifier = Modifier
            .size((12 * if (status == MarathonUIStatus.RUNNING) scale else 1f).dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun ProgressSection(agentState: MarathonAgentState) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${(agentState.progress * 100).toInt()}% Complete",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = formatTimeRemaining(agentState.estimatedCompletion - System.currentTimeMillis()),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        Spacer(Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { agentState.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = KaironexColors.ElectricBlue,
            trackColor = Color.White.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun CurrentThoughtBubble(thought: String, status: MarathonUIStatus) {
    val statusLabel = when (status) {
        MarathonUIStatus.THINKING -> "🧠 Thinking..."
        MarathonUIStatus.EXECUTING_TOOL -> "🔧 Executing..."
        MarathonUIStatus.SELF_CORRECTING -> "🔄 Self-Correcting..."
        MarathonUIStatus.WAITING_FOR_DATA -> "⏳ Waiting for data..."
        else -> "💭 Current thought"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(KaironexColors.ElectricBlue, KaironexColors.GeminiBlurple)
            )
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = KaironexColors.ElectricBlue
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = thought,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun AgentStatsRow(agentState: MarathonAgentState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatBox(
            icon = "🔧",
            value = agentState.toolCallCount.toString(),
            label = "Tool Calls"
        )
        StatBox(
            icon = "🔄",
            value = agentState.selfCorrectionCount.toString(),
            label = "Corrections"
        )
        StatBox(
            icon = "⏱️",
            value = formatDuration(System.currentTimeMillis() - agentState.startTime),
            label = "Running"
        )
    }
}

@Composable
private fun StatBox(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, style = MaterialTheme.typography.titleMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ThoughtHistoryList(thoughts: List<ThoughtSignatureUI>) {
    Column {
        Text(
            text = "Thought History",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.8f)
        )

        Spacer(Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.05f)
        ) {
            LazyColumn(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(thoughts.reversed()) { thought ->
                    ThoughtHistoryItem(thought)
                }
            }
        }
    }
}

@Composable
private fun ThoughtHistoryItem(thought: ThoughtSignatureUI) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Confidence indicator
        val confidenceColor = when {
            thought.confidence > 0.8f -> KaironexColors.SuccessGreen
            thought.confidence > 0.5f -> KaironexColors.AttentionOrange
            else -> KaironexColors.AlertRed
        }

        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(confidenceColor)
        )

        Spacer(Modifier.width(8.dp))

        Column {
            Text(
                text = thought.thought,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 2
            )

            if (thought.correction != null) {
                Text(
                    text = "↳ Correction: ${thought.correction}",
                    style = MaterialTheme.typography.labelSmall,
                    color = KaironexColors.AttentionOrange,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AgentControls(
    status: MarathonUIStatus,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (status == MarathonUIStatus.PAUSED) {
            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KaironexColors.SuccessGreen
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, "Resume")
                Spacer(Modifier.width(4.dp))
                Text("Resume")
            }
        } else if (status == MarathonUIStatus.RUNNING || status == MarathonUIStatus.THINKING) {
            OutlinedButton(
                onClick = onPause,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Pause, "Pause")
                Spacer(Modifier.width(4.dp))
                Text("Pause")
            }
        }

        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = KaironexColors.AlertRed
            ),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Close, "Cancel")
            Spacer(Modifier.width(4.dp))
            Text("Cancel")
        }
    }
}

// ===== HELPER FUNCTIONS =====

private fun formatTimeRemaining(millis: Long): String {
    if (millis <= 0) return "Completing..."

    val hours = millis / (1000 * 60 * 60)
    val minutes = (millis / (1000 * 60)) % 60

    return when {
        hours > 24 -> "${hours / 24}d ${hours % 24}h remaining"
        hours > 0 -> "${hours}h ${minutes}m remaining"
        else -> "${minutes}m remaining"
    }
}

private fun formatDuration(millis: Long): String {
    val hours = millis / (1000 * 60 * 60)
    val minutes = (millis / (1000 * 60)) % 60

    return when {
        hours > 24 -> "${hours / 24}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

// ===== COMPACT VERSION FOR DASHBOARD =====

@Composable
fun MarathonAgentMiniCard(
    agentState: MarathonAgentState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = KaironexColors.InkBlack.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status + Zone
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(agentState.zone.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(agentState.zone.emoji, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulsingStatusDot(agentState.status)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = agentState.objective,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        maxLines = 1
                    )
                }
                Text(
                    text = "${(agentState.progress * 100).toInt()}% • ${agentState.toolCallCount} calls",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View",
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
