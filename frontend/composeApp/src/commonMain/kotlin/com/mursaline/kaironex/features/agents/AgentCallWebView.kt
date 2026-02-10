package com.mursaline.kaironex.features.agents

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mursaline.kaironex.brain.AgentType
import com.mursaline.kaironex.features.campaign.CampaignState

/**
 * 📞 AGENT CALL WEBVIEW
 * ======================
 * WebView-based voice call interface for autonomous agent calls.
 *
 * Similar to InterviewWebView but specialized for agent-initiated calls:
 * - Agent speaks FIRST with context about why it's calling
 * - Full Gemini Live integration via embedded React app
 * - Bidirectional audio (mic + speaker)
 *
 * The web app at assets/call_agent/index.html handles:
 * - Gemini Live WebSocket connection
 * - Microphone capture and streaming
 * - Audio playback of agent responses
 * - Visual feedback (orb animation)
 */
@Composable
expect fun AgentCallWebView(
    modifier: Modifier = Modifier,
    apiKey: String,
    userId: String,
    agentType: AgentType,
    agentName: String,
    callReason: String,
    campaignState: CampaignState = CampaignState(), // Add campaign state
    onCallEnded: () -> Unit,
    onAgentStateChange: (isTalking: Boolean, isConnected: Boolean) -> Unit
)

