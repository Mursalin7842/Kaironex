package com.mursaline.kaironex.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text

@Composable
actual fun WebOrb(
    modifier: Modifier,
    apiKey: String,
    onProfileUpdate: (String, String) -> Unit,
    onAgentState: (String) -> Unit
) {
    // WebOrb not yet implemented for Desktop
    Text("WebOrb Placeholder (JVM)", modifier = modifier)
}
