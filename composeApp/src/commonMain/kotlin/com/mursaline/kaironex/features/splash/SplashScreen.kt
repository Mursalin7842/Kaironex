package com.mursaline.kaironex.features.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.features.auth.LoginScreen
import com.mursaline.kaironex.ui.theme.KaironexColors
import kaironex.composeapp.generated.resources.Res
import kaironex.composeapp.generated.resources.robot_hero
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

/**
 * Splash Screen with Robot Hero animation
 * Shows the Kaironex logo with a pulse animation before navigating to login
 */
object SplashScreen : Screen {
    private fun readResolve(): Any = SplashScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Animation states
        val infiniteTransition = rememberInfiniteTransition()

        // Pulse animation for the logo
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            )
        )

        // Glow animation
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            )
        )

        // Fade in animation
        var startAnimation by remember { mutableStateOf(false) }
        val alphaAnim by animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            animationSpec = tween(1000)
        )

        // Navigate after delay
        LaunchedEffect(Unit) {
            startAnimation = true
            delay(2500) // Show splash for 2.5 seconds
            navigator.replace(LoginScreen)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            KaironexColors.InkBlack,
                            Color(0xFF1A1A2E),
                            KaironexColors.InkBlack
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(alphaAnim)
            ) {
                // Glow effect behind robot
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .alpha(glowAlpha)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    KaironexColors.GeminiBlurple.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Robot Hero Image
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .scale(scale),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.robot_hero),
                        contentDescription = "Kaironex",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(32.dp))

                // App Name
                Text(
                    text = "KAIRONEX",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(Modifier.height(8.dp))

                // Tagline
                Text(
                    text = "Your Student Life Operating System",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KaironexColors.GeminiBlurple
                )

                Spacer(Modifier.height(48.dp))

                // Loading indicator
                Text(
                    text = "Initializing...",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}
