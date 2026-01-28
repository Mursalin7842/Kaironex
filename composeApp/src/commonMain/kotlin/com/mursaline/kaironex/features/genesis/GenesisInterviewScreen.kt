package com.mursaline.kaironex.features.genesis

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mursaline.kaironex.PlatformSecrets

@Composable
fun GenesisInterviewScreen(
    viewModel: GenesisViewModel, // Kept to minimize refactoring, effectively unused for view logic now
    onInterviewComplete: () -> Unit
) {
    // Connects to local asset React App
    InterviewWebView(
        modifier = Modifier.fillMaxSize(),
        apiKey = PlatformSecrets.apiKey,
        onInterviewComplete = onInterviewComplete
    )
}
