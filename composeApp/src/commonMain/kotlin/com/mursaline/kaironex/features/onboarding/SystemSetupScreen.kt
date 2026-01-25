package com.mursaline.kaironex.features.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SupportAgent
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.features.genesis.GenesisScreen
import com.mursaline.kaironex.ui.theme.KaironexColors
import kaironex.composeapp.generated.resources.Res
import kaironex.composeapp.generated.resources.robot_hero
import org.jetbrains.compose.resources.painterResource

/**
 * ============================================================
 * SYSTEM SETUP SCREEN - Single Screen Identity Setup
 * ============================================================
 *
 * After signup, user sets up:
 * 1. How system should address user (preferred name/title)
 * 2. What to call the system (Wake word like "Hey Kairo")
 *
 * We already have the full name from signup, so we just ask
 * for nickname preference and wake word in ONE screen.
 */

data class SystemSetupScreen(
    val userName: String = ""
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // State
        var addressMeAs by remember { mutableStateOf("") }
        var wakeWord by remember { mutableStateOf("Kairo") }

        // Floating animation for premium feel
        val infiniteTransition = rememberInfiniteTransition(label = "float")
        val floatOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "floatOffset"
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isDesktop = maxWidth > 800.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // LEFT SIDE - ROBOT HERO (Desktop Only)
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
                            modifier = Modifier.fillMaxSize().alpha(0.7f),
                            contentScale = ContentScale.Crop
                        )

                        // Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            KaironexColors.IndigoGradientStart.copy(alpha = 0.3f),
                                            KaironexColors.IndigoGradientEnd.copy(alpha = 0.85f)
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
                                    text = "Hello, $userName!",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 56.sp
                                )
                                Text(
                                    text = "Let's set up how we'll communicate. This will make our interactions feel more natural.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFFC7D2FE),
                                    lineHeight = 28.sp
                                )
                            }

                            // Footer
                            Text(
                                "Almost done • Just 2 quick questions",
                                color = KaironexColors.Indigo600,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // RIGHT SIDE - FORM
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.White)
                        .padding(if (isDesktop) 48.dp else 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 440.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Mobile: Show robot image at top
                        if (!isDesktop) {
                            Image(
                                painter = painterResource(Res.drawable.robot_hero),
                                contentDescription = "Kaironex",
                                modifier = Modifier
                                    .size(120.dp)
                                    .graphicsLayer {
                                        translationY = floatOffset
                                        scaleX = 1f + (floatOffset / 100f)
                                        scaleY = 1f + (floatOffset / 100f)
                                    }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                "Hello, $userName!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = KaironexColors.Slate900,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Header
                        Text(
                            text = "Let's Personalize",
                            style = if (isDesktop) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = KaironexColors.Slate900
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Just two quick questions to make this feel like yours",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KaironexColors.Slate500,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        // --- CARD 1: How should I address you? ---
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { translationY = floatOffset * 0.5f }
                                .shadow(8.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            tonalElevation = 2.dp
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.SupportAgent,
                                        contentDescription = null,
                                        tint = KaironexColors.GeminiBlurple,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "How should I address you?",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KaironexColors.Slate900
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "I'll use this when talking to you",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KaironexColors.Slate500
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = addressMeAs,
                                    onValueChange = { addressMeAs = it },
                                    placeholder = { Text("e.g., Boss, Professor, $userName", color = KaironexColors.Slate500.copy(alpha = 0.7f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = KaironexColors.CloudGray,
                                        unfocusedContainerColor = KaironexColors.CloudGray,
                                        focusedIndicatorColor = KaironexColors.GeminiBlurple,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = KaironexColors.GeminiBlurple,
                                        focusedTextColor = KaironexColors.Slate900,
                                        unfocusedTextColor = KaironexColors.Slate900
                                    ),
                                    singleLine = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // --- CARD 2: What should you call me? ---
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { translationY = -floatOffset * 0.3f }
                                .shadow(8.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            tonalElevation = 2.dp
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = KaironexColors.GeminiBlurple,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "What will you call me?",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KaironexColors.Slate900
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Your wake word, like \"Hey Siri\" or \"OK Google\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KaironexColors.Slate500
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = wakeWord,
                                    onValueChange = { wakeWord = it },
                                    placeholder = { Text("Kairo, Jarvis, Friday...", color = KaironexColors.Slate500.copy(alpha = 0.7f)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = KaironexColors.CloudGray,
                                        unfocusedContainerColor = KaironexColors.CloudGray,
                                        focusedIndicatorColor = KaironexColors.GeminiBlurple,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = KaironexColors.GeminiBlurple,
                                        focusedTextColor = KaironexColors.Slate900,
                                        unfocusedTextColor = KaironexColors.Slate900
                                    ),
                                    singleLine = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        // --- CTA Button ---
                        Button(
                            onClick = {
                                val finalAddress = addressMeAs.ifEmpty { userName }
                                val finalWake = wakeWord.ifEmpty { "Kairo" }
                                // Pass data to GenesisScreen for the AI interview
                                navigator.push(GenesisScreen(
                                    userName = userName,
                                    addressAs = finalAddress,
                                    wakeWord = finalWake
                                ))
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KaironexColors.Indigo600,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 8.dp
                            )
                        ) {
                            Text("Continue to AI Interview", fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Step indicator
                        Text(
                            "Next: A quick conversation so I can understand your needs",
                            style = MaterialTheme.typography.bodySmall,
                            color = KaironexColors.Slate500,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
