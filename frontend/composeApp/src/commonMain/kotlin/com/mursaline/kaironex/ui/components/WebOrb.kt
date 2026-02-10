package com.mursaline.kaironex.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun WebOrb(
    modifier: Modifier = Modifier,
    apiKey: String,
    onProfileUpdate: (String, String) -> Unit = { _, _ -> },
    onAgentState: (String) -> Unit = {}
)
