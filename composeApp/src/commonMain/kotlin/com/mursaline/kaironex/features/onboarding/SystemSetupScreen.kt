package com.mursaline.kaironex.features.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.MainShellScreen
import com.mursaline.kaironex.features.onboarding.GenesisInterviewScreen
import com.mursaline.kaironex.ui.theme.KaironexColors
import kaironex.composeapp.generated.resources.Res
import kaironex.composeapp.generated.resources.robot_hero
import org.jetbrains.compose.resources.painterResource

/**
 * ============================================================
 * SYSTEM SETUP SCREEN
 * ============================================================
 *
 * After signup, user sets up:
 * 1. How to address the user (Name preference)
 * 2. What to call the system (Wake word like "Hey Kairo")
 * 3. Preferred interaction mode (Voice / Chat / Both)
 * 4. Google Drive connection
 */

data class SystemSetupConfig(
    val userName: String = "",
    val preferredName: String = "", // How system calls user
    val wakeWord: String = "Hey Kairo", // Default wake word
    val interactionMode: InteractionMode = InteractionMode.BOTH,
    val isDriveConnected: Boolean = false
)

enum class InteractionMode(val label: String, val description: String, val emoji: String) {
    VOICE("Voice Only", "Talk naturally like Siri or Google", "🎤"),
    CHAT("Chat Only", "Type your requests", "💬"),
    BOTH("Voice + Chat", "Use both voice and typing", "🎙️")
}

object SystemSetupScreen : Screen {
    private fun readResolve(): Any = SystemSetupScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        var currentStep by remember { mutableStateOf(0) }
        var config by remember { mutableStateOf(SystemSetupConfig()) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(KaironexColors.CloudGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Progress indicator (3 steps: Name, Wake Word, Interaction Mode)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(3) { step ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (step == currentStep) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (step <= currentStep) KaironexColors.GeminiBlurple
                                    else KaironexColors.SlateGray.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))

                // Content based on step
                when (currentStep) {
                    0 -> NameSetupStep(
                        config = config,
                        onUpdate = { config = it },
                        onNext = { currentStep = 1 }
                    )
                    1 -> WakeWordSetupStep(
                        config = config,
                        onUpdate = { config = it },
                        onNext = { currentStep = 2 },
                        onBack = { currentStep = 0 }
                    )
2 -> InteractionModeStep(
                        config = config,
                        onUpdate = { config = it },
                        onNext = {
                            // After interaction mode, go to Genesis Interview for data collection
                            navigator.push(GenesisInterviewScreen(config.interactionMode))
                        },
                        onBack = { currentStep = 1 }
                    )
                    3 -> DriveSetupStep(
                        config = config,
                        onUpdate = { config = it },
                        onComplete = {
                            // Save config and navigate to main app
                            navigator.replaceAll(MainShellScreen)
                        },
                        onBack = { currentStep = 2 }
                    )
                }
            }
        }
    }
}

@Composable
private fun NameSetupStep(
    config: SystemSetupConfig,
    onUpdate: (SystemSetupConfig) -> Unit,
    onNext: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Robot image
        Image(
            painter = painterResource(Res.drawable.robot_hero),
            contentDescription = "Kaironex",
            modifier = Modifier
                .size(150.dp)
                .scale(scale)
        )

        Spacer(Modifier.height(32.dp))

        Text(
            "What should I call you?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = KaironexColors.InkBlack,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "I want to address you properly",
            style = MaterialTheme.typography.bodyMedium,
            color = KaironexColors.SlateGray
        )

        Spacer(Modifier.height(32.dp))

        // Name input
        OutlinedTextField(
            value = config.preferredName,
            onValueChange = { onUpdate(config.copy(preferredName = it)) },
            label = { Text("Your preferred name") },
            placeholder = { Text("e.g., Alex, Professor, Boss") },
            modifier = Modifier.fillMaxWidth(0.8f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KaironexColors.GeminiBlurple,
                unfocusedBorderColor = KaironexColors.SlateGray.copy(alpha = 0.3f),
                focusedTextColor = KaironexColors.InkBlack,
                unfocusedTextColor = KaironexColors.InkBlack,
                focusedLabelColor = KaironexColors.GeminiBlurple,
                unfocusedLabelColor = KaironexColors.SlateGray,
                cursorColor = KaironexColors.GeminiBlurple,
                focusedContainerColor = KaironexColors.CanvasWhite,
                unfocusedContainerColor = KaironexColors.CanvasWhite
            ),
            singleLine = true
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onNext,
            enabled = config.preferredName.isNotBlank(),
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KaironexColors.GeminiBlurple
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Continue", fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun WakeWordSetupStep(
    config: SystemSetupConfig,
    onUpdate: (SystemSetupConfig) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val wakeWordOptions = listOf(
        "Hey Kairo",
        "OK Kairo",
        "Kairo",
        "Hey Assistant",
        "Custom..."
    )

    var showCustomInput by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.RecordVoiceOver,
            contentDescription = null,
            tint = KaironexColors.GeminiBlurple,
            modifier = Modifier.size(80.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "How will you wake me up?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = KaironexColors.InkBlack,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Choose your wake word (like 'Hey Siri')",
            style = MaterialTheme.typography.bodyMedium,
            color = KaironexColors.SlateGray
        )

        Spacer(Modifier.height(32.dp))

        // Wake word options
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            wakeWordOptions.forEach { option ->
                val isSelected = if (option == "Custom...") {
                    showCustomInput || !wakeWordOptions.dropLast(1).contains(config.wakeWord)
                } else {
                    config.wakeWord == option
                }

                Surface(
                    onClick = {
                        if (option == "Custom...") {
                            showCustomInput = true
                        } else {
                            showCustomInput = false
                            onUpdate(config.copy(wakeWord = option))
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) KaironexColors.GeminiBlurple.copy(alpha = 0.1f)
                           else KaironexColors.CanvasWhite,
                    shadowElevation = if (isSelected) 0.dp else 2.dp,
                    border = if (isSelected) {
                        androidx.compose.foundation.BorderStroke(2.dp, KaironexColors.GeminiBlurple)
                    } else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = KaironexColors.InkBlack
                        )
                        Spacer(Modifier.weight(1f))
                        if (isSelected && option != "Custom...") {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = KaironexColors.GeminiBlurple
                            )
                        }
                    }
                }
            }

            // Custom input
            if (showCustomInput) {
                OutlinedTextField(
                    value = if (wakeWordOptions.dropLast(1).contains(config.wakeWord)) "" else config.wakeWord,
                    onValueChange = { onUpdate(config.copy(wakeWord = it)) },
                    label = { Text("Your custom wake word") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KaironexColors.GeminiBlurple,
                        unfocusedBorderColor = KaironexColors.SlateGray.copy(alpha = 0.3f),
                        focusedTextColor = KaironexColors.InkBlack,
                        unfocusedTextColor = KaironexColors.InkBlack,
                        cursorColor = KaironexColors.GeminiBlurple,
                        focusedContainerColor = KaironexColors.CanvasWhite,
                        unfocusedContainerColor = KaironexColors.CanvasWhite
                    ),
                    singleLine = true
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = KaironexColors.SlateGray
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, KaironexColors.SlateGray.copy(alpha = 0.3f))
            ) {
                Text("Back")
            }

            Button(
                onClick = onNext,
                enabled = config.wakeWord.isNotBlank(),
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KaironexColors.GeminiBlurple
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun InteractionModeStep(
    config: SystemSetupConfig,
    onUpdate: (SystemSetupConfig) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ChatBubble,
            contentDescription = null,
            tint = KaironexColors.GeminiBlurple,
            modifier = Modifier.size(80.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "How do you want to interact?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = KaironexColors.InkBlack,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "You can change this anytime in settings",
            style = MaterialTheme.typography.bodyMedium,
            color = KaironexColors.SlateGray
        )

        Spacer(Modifier.height(32.dp))

        // Mode options
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InteractionMode.entries.forEach { mode ->
                val isSelected = config.interactionMode == mode

                Surface(
                    onClick = { onUpdate(config.copy(interactionMode = mode)) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) KaironexColors.GeminiBlurple.copy(alpha = 0.1f)
                           else KaironexColors.CanvasWhite,
                    shadowElevation = if (isSelected) 0.dp else 2.dp,
                    border = if (isSelected) {
                        androidx.compose.foundation.BorderStroke(2.dp, KaironexColors.GeminiBlurple)
                    } else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            mode.emoji,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                mode.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = KaironexColors.InkBlack
                            )
                            Text(
                                mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = KaironexColors.SlateGray
                            )
                        }
                        if (isSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = KaironexColors.GeminiBlurple
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = KaironexColors.SlateGray
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, KaironexColors.SlateGray.copy(alpha = 0.3f))
            ) {
                Text("Back")
            }

            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KaironexColors.GeminiBlurple
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Continue")
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun DriveSetupStep(
    config: SystemSetupConfig,
    onUpdate: (SystemSetupConfig) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CloudUpload,
            contentDescription = null,
            tint = KaironexColors.GeminiBlurple,
            modifier = Modifier.size(80.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "Connect your study materials",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Import from Google Drive or upload later",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(40.dp))

        // Drive connection card
        Surface(
            onClick = {
                // TODO: Implement Google Drive OAuth
                onUpdate(config.copy(isDriveConnected = !config.isDriveConnected))
            },
            shape = RoundedCornerShape(16.dp),
            color = if (config.isDriveConnected)
                KaironexColors.SuccessGreen.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.05f),
            border = if (config.isDriveConnected) {
                androidx.compose.foundation.BorderStroke(2.dp, KaironexColors.SuccessGreen)
            } else {
                androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            },
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (config.isDriveConnected) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = if (config.isDriveConnected) KaironexColors.SuccessGreen
                          else Color.White,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (config.isDriveConnected) "Google Drive Connected!"
                        else "Connect Google Drive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (config.isDriveConnected) KaironexColors.SuccessGreen
                               else Color.White
                    )
                    Text(
                        if (config.isDriveConnected) "Tap to disconnect"
                        else "Sync your PDFs, notes, and documents",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Skip option
        TextButton(onClick = { onUpdate(config.copy(isDriveConnected = false)) }) {
            Text(
                "I'll upload files manually later",
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        Spacer(Modifier.weight(1f))

        // Finish button
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(0.85f).height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KaironexColors.GeminiBlurple
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Complete Setup")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Check, null)
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(0.85f).height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
        ) {
            Text("Back")
        }

        Spacer(Modifier.height(32.dp))
    }
}
