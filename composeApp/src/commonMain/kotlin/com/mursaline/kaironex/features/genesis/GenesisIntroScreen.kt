package com.mursaline.kaironex.features.genesis

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mursaline.kaironex.ui.theme.KaironexColors
import kotlinx.coroutines.delay

object GenesisIntroScreen : Screen {
    private fun readResolve(): Any = GenesisIntroScreen
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        // Auto-navigate after delay
        LaunchedEffect(Unit) {
            delay(4000)
            navigator.replace(GenesisInterviewScreen) // Navigate to Interview
        }
        
        Box(
            modifier = Modifier.fillMaxSize().background(KaironexColors.Slate900),
            contentAlignment = Alignment.Center
        ) {
            // Breathing Orb Simulation
            val infiniteTransition = rememberInfiniteTransition(label = "orb")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f, targetValue = 1.2f,
                animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "scale"
            )

            Box(
                modifier = Modifier
                    .size(300.dp)
                    .scale(scale)
                    .background(Brush.radialGradient(listOf(KaironexColors.Indigo600.copy(alpha=0.3f), Color.Transparent)))
            )
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(24.dp))
                Text("Hello, Architect.", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Text("I am Kaironex.", color = KaironexColors.Indigo600, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(48.dp))
                Text("Calibrating Neural Link...", color = KaironexColors.Slate500, fontSize = 12.sp, letterSpacing = 2.sp)
            }
        }
    }
}
