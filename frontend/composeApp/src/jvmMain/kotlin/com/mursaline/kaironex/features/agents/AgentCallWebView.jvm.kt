package com.mursaline.kaironex.features.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.brain.AgentType
import com.mursaline.kaironex.features.campaign.CampaignState

/**
 * JVM/Desktop stub for AgentCallWebView
 * Desktop uses a different approach (JCEF or native audio)
 */
@Composable
actual fun AgentCallWebView(
    modifier: Modifier,
    apiKey: String,
    userId: String,
    agentType: AgentType,
    agentName: String,
    callReason: String,
    campaignState: CampaignState,
    onCallEnded: () -> Unit,
    onAgentStateChange: (isTalking: Boolean, isConnected: Boolean) -> Unit
) {
    // Desktop placeholder - would use JCEF browser or native audio
    Box(
        modifier = modifier.background(Color(0xFF1a1a2e)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)
            Spacer(Modifier.height(16.dp))
            Text(
                "Agent Call (Desktop)",
                color = Color.White
            )
            Text(
                "$agentName is calling...",
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

