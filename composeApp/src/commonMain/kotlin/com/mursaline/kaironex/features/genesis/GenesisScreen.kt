package com.mursaline.kaironex.features.genesis

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import com.mursaline.kaironex.brain.GeminiReasoningEngine
import kotlinx.coroutines.delay

class GenesisScreen : Screen {
    
    @Composable
    override fun Content() {
        var transcript by remember { mutableStateOf("Listening...") }
        var isThinking by remember { mutableStateOf(false) }

        // Mock interaction for now
        LaunchedEffect(Unit) {
            delay(2000)
            transcript = "Who are you?"
            isThinking = true
            delay(1500)
            isThinking = false
            transcript = "I am Kaironex. I see you are setting up your environment."
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000)),
            contentAlignment = Alignment.Center
        ) {
            // Ambient Background
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF220044), Color.Transparent),
                        center = center,
                        radius = size.minDimension / 1.5f
                    )
                )
            }

            // The Orb
            Orb(isThinking)

            // Transcript
            Text(
                text = transcript,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
            )
        }
    }

    @Composable
    fun Orb(isActive: Boolean) {
        val infiniteTransition = rememberInfiniteTransition()
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing)
            )
        )

        Canvas(modifier = Modifier.size(200.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.minDimension / 2 * 0.8f

            // Core
            drawCircle(
                color = Color.White,
                radius = 10f,
                center = center
            )

            // Inner Ring
            drawCircle(
                color = Color(0xFFBB86FC),
                radius = baseRadius * pulse,
                center = center,
                style = Stroke(width = 2f)
            )

            // Outer Glitch Ring
            drawCircle(
                color = Color(0xFF03DAC6).copy(alpha = 0.5f),
                radius = baseRadius * pulse * 1.1f,
                center = center,
                style = Stroke(width = 1f)
            )
        }
    }
}
