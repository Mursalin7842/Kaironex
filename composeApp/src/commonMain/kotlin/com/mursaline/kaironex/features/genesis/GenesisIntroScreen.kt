package com.mursaline.kaironex.features.genesis

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
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
import com.mursaline.kaironex.ui.theme.KaironexColors
import kaironex.composeapp.generated.resources.Res
import kaironex.composeapp.generated.resources.robot_hero
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenesisIntroScreen(
    onNext: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var wakeWord by remember { mutableStateOf("Kaironex") }
    var nicknameInput by remember { mutableStateOf("") }

    // Floating animation for cards
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
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
                                text = "System Genesis",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 56.sp
                            )
                            Text(
                                text = "Let's personalize your cognitive companion. This will only take a moment.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFC7D2FE),
                                lineHeight = 28.sp
                            )
                        }

                        // Footer
                        Text(
                            "Step 1 of 3 • Identity Configuration",
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
                    horizontalAlignment = Alignment.Start
                ) {
                    // Mobile Logo
                    if (!isDesktop) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(KaironexColors.Indigo600),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("K", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kaironex", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = KaironexColors.Slate900)
                        }
                    }

                    // Header
                    Text(
                        text = "Identity Configuration",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.Slate900
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Help us personalize your experience. How should we communicate?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KaironexColors.Slate500
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // --- FLOATING CARD 1: Your Name ---
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
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = KaironexColors.GeminiBlurple,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "My name is",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KaironexColors.Slate900
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = { Text("Enter your name", color = KaironexColors.Slate500.copy(alpha = 0.7f)) },
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

                    // --- FLOATING CARD 2: Wake Word ---
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
                                    "I'll call the System",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KaironexColors.Slate900
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Choose a wake word like \"Hey Kairo\" or \"Jarvis\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = KaironexColors.Slate500
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = wakeWord,
                                onValueChange = { wakeWord = it },
                                placeholder = { Text("Kairo / Jarvis / Friday", color = KaironexColors.Slate500.copy(alpha = 0.7f)) },
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

                    // --- FLOATING CARD 3: Address User As ---
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { translationY = floatOffset * 0.4f }
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
                                    "Address me as",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KaironexColors.Slate900
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "How should the system address you?",
                                style = MaterialTheme.typography.bodySmall,
                                color = KaironexColors.Slate500
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = nicknameInput,
                                onValueChange = { nicknameInput = it },
                                placeholder = { Text("Sir / Boss / ${name.ifEmpty { "Friend" }}", color = KaironexColors.Slate500.copy(alpha = 0.7f)) },
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
                            val finalWakeWord = wakeWord.ifEmpty { "Kaironex" }
                            val finalNickname = nicknameInput.ifEmpty { name }
                            onNext(name, finalWakeWord, finalNickname)
                        },
                        enabled = name.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KaironexColors.Indigo600,
                            contentColor = Color.White,
                            disabledContainerColor = KaironexColors.Slate100,
                            disabledContentColor = KaironexColors.Slate500
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Text("Continue to Interview", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Step indicator
                    Text(
                        "Next: AI-powered conversation to understand your needs",
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
