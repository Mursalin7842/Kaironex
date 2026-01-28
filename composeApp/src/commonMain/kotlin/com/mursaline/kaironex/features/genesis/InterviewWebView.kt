package com.mursaline.kaironex.features.genesis

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun InterviewWebView(
    modifier: Modifier = Modifier,
    apiKey: String,
    onInterviewComplete: () -> Unit,
    onAgentStateChange: (Boolean, Boolean) -> Unit,
    onProfileUpdate: (String, String) -> Unit
)
