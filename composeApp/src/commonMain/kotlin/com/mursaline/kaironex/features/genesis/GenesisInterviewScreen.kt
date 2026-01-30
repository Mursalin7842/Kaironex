package com.mursaline.kaironex.features.genesis

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.mursaline.kaironex.PlatformSecrets
import com.mursaline.kaironex.ui.theme.KaironexColors
import kaironex.composeapp.generated.resources.Res
import kaironex.composeapp.generated.resources.robot_hero
import org.jetbrains.compose.resources.painterResource
import com.mursaline.kaironex.brain.GeminiReasoningEngine

@Composable
fun GenesisInterviewScreen(
    viewModel: GenesisViewModel,
    userName: String,
    agentName: String,
    onInterviewComplete: () -> Unit
) {
    // HYBRID ARCHITECTURE PIVOT:
    // The visual UI is now rendered completely by the React Application (HTML/CSS)
    // inside this WebView. The Kotlin layer serves as a host and data sink.
    
    // We retain full opacity so the React UI is visible.
    InterviewWebView(
        modifier = Modifier.fillMaxSize(),
        apiKey = PlatformSecrets.apiKey,
        userName = userName,
        agentName = agentName,
        onInterviewComplete = onInterviewComplete,
        onAgentStateChange = { _, _ -> /* Visual state handled in React now */ },
        onProfileUpdate = { field, value ->
            viewModel.updateFromAgent(field, value)
        }
    )
}
