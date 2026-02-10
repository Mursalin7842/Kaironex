package com.mursaline.kaironex.features.agents

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mursaline.kaironex.brain.*
import com.mursaline.kaironex.core.CurrentUser
import com.mursaline.kaironex.PlatformSecrets
import com.mursaline.kaironex.ui.theme.KaironexColors
import com.mursaline.kaironex.core.stats.AppwriteStatsRepository
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/**
 * ============================================================
 * AGENT VOICE CALL SCREEN
 * ============================================================
 *
 * Dedicated full-screen call experience when an agent calls 
 * the user or user accepts an incoming agent call.
 *
 * KEY DIFFERENCE from the Orb:
 * - Agent speaks FIRST (not the user)
 * - Phone-call-style UI with timer, end button
 * - Context-aware: agent knows WHY it's calling
 * - Uses WebView-based Gemini Live for real voice
 *
 * This is what makes Kaironex unique: agents act autonomously
 * and call the user on their own initiative.
 */

@Composable
fun AgentVoiceCallScreen(
    call: AgentCall,
    onEndCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Inject repository to get campaign state
    val statsRepo: AppwriteStatsRepository = koinInject()
    val campaignState by statsRepo.campaignState.collectAsState()

    // Call state
    var callDuration by remember { mutableStateOf(0) }
    var isConnected by remember { mutableStateOf(false) }
    var isAgentSpeaking by remember { mutableStateOf(false) }

    // Agent identity
    val agentColor = when (call.agent) {
        AgentType.STUDY -> Color(0xFF4285F4)
        AgentType.VITALITY -> Color(0xFF34A853)
        AgentType.CAMPAIGN -> Color(0xFFFBBC04)
        AgentType.RADIUS -> Color(0xFF9C27B0)
        AgentType.SUPERVISOR -> KaironexColors.GeminiBlurple
    }

    // Pulsing animation for active call
    val infiniteTransition = rememberInfiniteTransition(label = "callPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Call duration timer
    LaunchedEffect(isConnected) {
        if (isConnected) {
            while (true) {
                delay(1000)
                callDuration++
            }
        }
    }

    val formattedDuration = remember(callDuration) {
        val mins = callDuration / 60
        val secs = callDuration % 60
        "%02d:%02d".format(mins, secs)
    }

    // =================== UI ===================
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black) // Fallback
    ) {
        // --- Layer 1: Background Gradient (Matches Incoming Screen) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D1117),
                            agentColor.copy(alpha = 0.1f),
                            Color(0xFF0D1117)
                        )
                    )
                )
        )

        // --- Layer 2: WebView (Now provides black background) ---
        AgentCallWebView(
            modifier = Modifier.fillMaxSize(),
            apiKey = PlatformSecrets.apiKey,
            userId = CurrentUser.userId,
            agentType = call.agent,
            agentName = call.agent.label,
            callReason = call.reason,
            campaignState = campaignState,
            onCallEnded = onEndCall,
            onAgentStateChange = { isTalking, connected ->
                isAgentSpeaking = isTalking
                isConnected = connected
            }
        )

        // --- Layer 3: Main UI Overlay ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Agent Identity & Status
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = call.agent.label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(8.dp))

                if (isConnected) {
                    Text(
                        text = formattedDuration,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = agentColor,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Connecting secure channel...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Middle: Visualization
            Box(contentAlignment = Alignment.Center) {
                // Outer glow
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(if (isAgentSpeaking) pulseScale else 1f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    agentColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Avatar Icon
                Surface(
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(elevation = 16.dp, shape = CircleShape, spotColor = agentColor),
                    shape = CircleShape,
                    color = agentColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = call.agent.emoji,
                            style = MaterialTheme.typography.displayMedium
                        )
                    }
                }
            }

            // Bottom: Controls
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Live Transcript / Status
                AnimatedContent(targetState = isAgentSpeaking) { speaking ->
                    if (speaking) {
                        Text(
                            "Speaking...",
                            color = agentColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isConnected) {
                        Text(
                            "Listening...",
                            color = Color.White.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelLarge
                        )
                    } else {
                        Spacer(Modifier.height(20.dp))
                    }
                }
                
                Spacer(Modifier.height(48.dp))

                // Control Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute (Placeholder visual)
                    IconButton(
                        onClick = { /* TODO */ },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Mute", tint = Color.White)
                    }

                    // End Call
                    FloatingActionButton(
                        onClick = onEndCall,
                        containerColor = Color(0xFFEA4335),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp).shadow(12.dp, CircleShape, spotColor = Color(0xFFEA4335))
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "End Call", modifier = Modifier.size(32.dp))
                    }

                    // Speaker (Placeholder visual)
                    IconButton(
                        onClick = { /* TODO */ },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Speaker", tint = Color.White)
                    }
                }
            }
        }
    }
}
