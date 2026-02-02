package com.mursaline.kaironex.features.voice

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.components.KxOrb
import com.mursaline.kaironex.ui.theme.KaironexColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 📞 VOICE CALL SCREEN
 * ====================
 * Full-screen voice interaction with the Kaironex AI.
 *
 * States:
 * - WAKING: Wake word detected, connecting...
 * - RINGING: Pulse animation, waiting for AI response
 * - ACTIVE: In conversation with waveform visualization
 * - PROCESSING: AI is thinking (brain icon pulsing)
 * - ENDING: Call ending, syncing changes
 *
 * Features:
 * - Real-time transcription
 * - Audio waveform visualization
 * - Context actions (detected intents)
 * - Mute toggle
 */

enum class CallState {
    WAKING,      // Connecting to Kairo
    RINGING,     // Waiting for AI response
    ACTIVE,      // In conversation
    PROCESSING,  // AI thinking
    ENDING       // Syncing and closing
}

data class VoiceCallScreenParams(
    val initialCommand: String? = null,
    val agentName: String = "Kairo"
)

class VoiceCallScreen(
    private val params: VoiceCallScreenParams = VoiceCallScreenParams()
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Get the Gemini voice engine from DI
        val reasoningEngine: com.mursaline.kaironex.brain.GeminiReasoningEngine = org.koin.compose.koinInject()
        val brainClient: com.mursaline.kaironex.brain.BrainApiClient = org.koin.compose.koinInject()
        val apiKey = com.mursaline.kaironex.PlatformSecrets.apiKey
        val scope = rememberCoroutineScope()

        var callState by remember { mutableStateOf(CallState.WAKING) }
        var isMuted by remember { mutableStateOf(false) }
        var transcription by remember { mutableStateOf("") }
        var aiResponse by remember { mutableStateOf("") }
        var detectedIntent by remember { mutableStateOf<String?>(null) }
        var callDuration by remember { mutableStateOf(0) }
        var isConnected by remember { mutableStateOf(false) }

        // Collect engine state
        val engineState by reasoningEngine.connectionState.collectAsState()
        val isAgentSpeaking by reasoningEngine.isAgentSpeaking.collectAsState()

        // Update UI based on engine state
        LaunchedEffect(engineState) {
            when (engineState) {
                is com.mursaline.kaironex.brain.GeminiReasoningEngine.ConnectionState.Connecting -> {
                    callState = CallState.WAKING
                }
                is com.mursaline.kaironex.brain.GeminiReasoningEngine.ConnectionState.Connected -> {
                    callState = CallState.ACTIVE
                    isConnected = true
                    if (aiResponse.isEmpty()) {
                        aiResponse = "Hey! What's on your mind?"
                    }
                }
                is com.mursaline.kaironex.brain.GeminiReasoningEngine.ConnectionState.Disconnected -> {
                    if (isConnected) {
                        callState = CallState.ENDING
                    }
                }
                is com.mursaline.kaironex.brain.GeminiReasoningEngine.ConnectionState.Error -> {
                    val errorState = engineState as com.mursaline.kaironex.brain.GeminiReasoningEngine.ConnectionState.Error
                    aiResponse = "Connection issue: ${errorState.message}"
                }
            }
        }

        // Update state based on agent speaking
        LaunchedEffect(isAgentSpeaking) {
            if (isAgentSpeaking) {
                callState = CallState.ACTIVE
            }
        }

        // Connect to Gemini Live API
        LaunchedEffect(Unit) {
            if (apiKey.isNotEmpty() && apiKey != "PLACEHOLDER") {
                try {
                    // Connect to Gemini voice
                    scope.launch {
                        reasoningEngine.connect(apiKey)
                    }

                    // Wait a bit for connection
                    delay(1500)

                    if (callState == CallState.WAKING) {
                        callState = CallState.RINGING
                    }

                    delay(1500)

                    callState = CallState.ACTIVE
                    aiResponse = "Hey! What's on your mind?"

                    // If there's an initial command, send it
                    params.initialCommand?.let { cmd ->
                        transcription = cmd
                        callState = CallState.PROCESSING

                        // Send the command to the brain for processing
                        val response = brainClient.quickPrompt(
                            userId = "demo_user_001",
                            prompt = cmd,
                            agent = "generic",
                            mode = "reflex"
                        )

                        callState = CallState.ACTIVE
                        if (response != null) {
                            aiResponse = response.response
                            // Try to detect intent
                            when {
                                cmd.contains("wedding", ignoreCase = true) ||
                                cmd.contains("event", ignoreCase = true) -> {
                                    detectedIntent = "life_event"
                                }
                                cmd.contains("schedule", ignoreCase = true) ||
                                cmd.contains("meeting", ignoreCase = true) -> {
                                    detectedIntent = "schedule_update"
                                }
                                cmd.contains("study", ignoreCase = true) ||
                                cmd.contains("exam", ignoreCase = true) -> {
                                    detectedIntent = "study_session"
                                }
                                cmd.contains("tired", ignoreCase = true) ||
                                cmd.contains("break", ignoreCase = true) -> {
                                    detectedIntent = "vitality_check"
                                }
                            }
                        } else {
                            aiResponse = "Got it! I'll help you with that."
                            detectedIntent = "general_request"
                        }
                    }
                } catch (e: Exception) {
                    println("Voice connection error: ${e.message}")
                    callState = CallState.ACTIVE
                    aiResponse = "Voice connection unavailable. How can I help?"
                }
            } else {
                // Fallback for demo without API key
                delay(1500)
                callState = CallState.RINGING
                delay(2000)
                callState = CallState.ACTIVE
                aiResponse = "Hey! What's on your mind?"

                params.initialCommand?.let { cmd ->
                    delay(1000)
                    transcription = cmd
                    callState = CallState.PROCESSING
                    delay(2000)
                    callState = CallState.ACTIVE
                    detectedIntent = "schedule_update"
                    aiResponse = "Got it! I'll help you with that."
                }
            }
        }

        // Cleanup on dispose
        DisposableEffect(Unit) {
            onDispose {
                scope.launch {
                    reasoningEngine.disconnect()
                }
            }
        }

        // Call duration timer
        LaunchedEffect(callState) {
            if (callState == CallState.ACTIVE || callState == CallState.PROCESSING) {
                while (true) {
                    delay(1000)
                    callDuration++
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            KaironexColors.GeminiBlurple,
                            KaironexColors.Indigo900
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top bar with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Call status
                    Column {
                        Text(
                            text = when (callState) {
                                CallState.WAKING -> "Connecting..."
                                CallState.RINGING -> "Calling ${params.agentName}..."
                                CallState.ACTIVE -> params.agentName
                                CallState.PROCESSING -> "${params.agentName} is thinking..."
                                CallState.ENDING -> "Ending call..."
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        if (callState == CallState.ACTIVE || callState == CallState.PROCESSING) {
                            Text(
                                text = formatDuration(callDuration),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Close button
                    IconButton(
                        onClick = {
                            callState = CallState.ENDING
                            // Delay then close
                            navigator.pop()
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White)
                    }
                }

                Spacer(Modifier.weight(0.3f))

                // Central Orb with state-specific animation
                AnimatedOrb(
                    callState = callState,
                    isMuted = isMuted
                )

                Spacer(Modifier.height(40.dp))

                // Status text
                AnimatedVisibility(
                    visible = aiResponse.isNotEmpty(),
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut()
                ) {
                    Text(
                        text = aiResponse,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Transcription display
                AnimatedVisibility(
                    visible = transcription.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "You said:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = transcription,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }
                    }
                }

                // Detected intent badge
                AnimatedVisibility(
                    visible = detectedIntent != null,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = KaironexColors.ElectricBlue.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🧠", fontSize = 14.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Detected: ${detectedIntent?.replace("_", " ")}",
                                style = MaterialTheme.typography.labelMedium,
                                color = KaironexColors.ElectricBlue
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Bottom controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute button
                    CallControlButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (isMuted) "Unmute" else "Mute",
                        isActive = isMuted,
                        onClick = { isMuted = !isMuted }
                    )

                    // End call button
                    Surface(
                        onClick = {
                            callState = CallState.ENDING
                            navigator.pop()
                        },
                        color = Color(0xFFE53935),
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CallEnd,
                                "End Call",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Placeholder for symmetry
                    Box(modifier = Modifier.size(64.dp))
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AnimatedOrb(
    callState: CallState,
    isMuted: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    // Pulse animation for ringing/processing
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = when (callState) {
            CallState.RINGING -> 1.15f
            CallState.PROCESSING -> 1.08f
            else -> 1f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (callState) {
                    CallState.RINGING -> 800
                    CallState.PROCESSING -> 1200
                    else -> 2000
                },
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Float animation for active state
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (callState == CallState.ACTIVE) 12f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    // Rotation for waking state
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (callState == CallState.WAKING) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
                translationY = -floatOffset
                rotationZ = if (callState == CallState.WAKING) rotation else 0f
            }
    ) {
        // Outer glow rings
        repeat(3) { index ->
            val delay = index * 400
            val ringPulse by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, delayMillis = delay, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "ring$index"
            )

            Box(
                modifier = Modifier
                    .size((160 + index * 40).dp)
                    .graphicsLayer {
                        scaleX = ringPulse
                        scaleY = ringPulse
                        alpha = 0.3f - (index * 0.1f)
                    }
                    .border(
                        width = 2.dp,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.5f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Main orb
        Box(
            modifier = Modifier
                .size(140.dp)
                .shadow(24.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (isMuted) {
                            listOf(Color.Gray, Color.DarkGray)
                        } else {
                            listOf(Color.White, KaironexColors.CloudGray)
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            KxOrb(
                isAgentSpeaking = callState == CallState.ACTIVE,
                isUserListening = !isMuted && callState != CallState.PROCESSING,
                modifier = Modifier.size(110.dp)
            )
        }
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Surface(
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.15f),
            shape = CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    label,
                    tint = if (isActive) KaironexColors.GeminiBlurple else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
