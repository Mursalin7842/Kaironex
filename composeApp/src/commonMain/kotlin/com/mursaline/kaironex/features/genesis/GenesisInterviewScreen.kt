package com.mursaline.kaironex.features.genesis

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mursaline.kaironex.ui.theme.KaironexColors
import kaironex.composeapp.generated.resources.Res
import kaironex.composeapp.generated.resources.robot_hero
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenesisInterviewScreen(
    viewModel: GenesisViewModel,
    onInterviewComplete: () -> Unit
) {
    val state = viewModel.uiState
    var isTextMode by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    // Floating animation
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    // Orb pulse animation
    val orbScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state.isAgentSpeaking) 1.15f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state.isAgentSpeaking) 600 else 1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isDesktop = maxWidth > 800.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT SIDE - HERO (Desktop Only)
            if (isDesktop) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(KaironexColors.Slate900)
                ) {
                    // Robot Image
                    Image(
                        painter = painterResource(Res.drawable.robot_hero),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().alpha(0.6f),
                        contentScale = ContentScale.Crop
                    )

                    // Gradient Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        KaironexColors.IndigoGradientStart.copy(alpha = 0.4f),
                                        KaironexColors.IndigoGradientEnd.copy(alpha = 0.9f)
                                    )
                                )
                            )
                    )

                    // Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(48.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Branding
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(KaironexColors.Indigo600),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("K", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Kaironex", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 24.sp, letterSpacing = (-1).sp)
                        }

                        // Hero Text with floating effect
                        Column(
                            modifier = Modifier.graphicsLayer { translationY = floatOffset },
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Text(
                                text = "Getting to Know You",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 56.sp
                            )
                            Text(
                                text = "I'm learning about your lifestyle to optimize your experience. Just have a natural conversation.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFC7D2FE),
                                lineHeight = 28.sp
                            )
                        }

                        // Footer
                        Text(
                            "Step 2 of 3 • AI Interview",
                            color = KaironexColors.Indigo600,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // RIGHT SIDE - INTERVIEW AREA
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color.White)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Mobile Header
                    if (!isDesktop) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shadowElevation = 2.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(KaironexColors.Indigo600),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("K", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("AI Interview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = KaironexColors.Slate900)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Tell me about yourself",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KaironexColors.Slate500
                                )
                            }
                        }
                    }

                    // Main Content Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // THE ORB - Central Focus
                            val connectionState by viewModel.connectionState.collectAsState()
                            val audioRms by viewModel.audioRms.collectAsState()
                            
                            com.mursaline.kaironex.ui.components.VoiceOrb(
                                connectionState = connectionState,
                                audioRms = audioRms,
                                modifier = Modifier
                                    .graphicsLayer {
                                        translationY = -floatOffset * 2
                                    }
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Agent Message Card
                            Surface(
                                modifier = Modifier
                                    .widthIn(max = 400.dp)
                                    .graphicsLayer { translationY = floatOffset }
                                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp),
                                color = KaironexColors.CloudGray,
                                tonalElevation = 2.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = state.lastAgentMessage,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = KaironexColors.Slate900,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 26.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Status Badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = when {
                                    state.isAgentSpeaking -> KaironexColors.GeminiBlurple.copy(alpha = 0.1f)
                                    state.isUserListening -> KaironexColors.SuccessGreen.copy(alpha = 0.1f)
                                    else -> KaironexColors.Slate100
                                }
                            ) {
                                Text(
                                    text = when {
                                        state.isAgentSpeaking -> "🎙️ Kaironex is speaking..."
                                        state.isUserListening -> "🎤 Listening to you..."
                                        else -> "💡 Tap the mic to respond"
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when {
                                        state.isAgentSpeaking -> KaironexColors.GeminiBlurple
                                        state.isUserListening -> KaironexColors.SuccessGreen
                                        else -> KaironexColors.Slate500
                                    },
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // --- BOTTOM CONTROL DECK ---
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shadowElevation = 8.dp,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Voice Only Mode Controls
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Skip or Continue Button
                                Surface(
                                    onClick = { onInterviewComplete() },
                                    shape = RoundedCornerShape(50),
                                    color = if (state.isComplete) KaironexColors.SuccessGreen else KaironexColors.CloudGray,
                                    modifier = Modifier.height(48.dp).padding(horizontal = 16.dp),
                                    border = BorderStroke(1.dp, if (state.isComplete) KaironexColors.SuccessGreen else KaironexColors.BorderGray)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    ) {
                                        Text(
                                            if (state.isComplete) "Continue to Google Drive Setup" else "Skip Interview",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (state.isComplete) Color.White else KaironexColors.Slate500,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (state.isComplete) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Icon(
                                                Icons.AutoMirrored.Filled.Send, 
                                                contentDescription = null, 
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                "Kaironex is listening. Just speak naturally.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KaironexColors.Slate500
                            )
                        }
                    }
                }
            }
        }
    }
}
