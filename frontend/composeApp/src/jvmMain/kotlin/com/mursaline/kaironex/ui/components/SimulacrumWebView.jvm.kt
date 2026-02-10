
package com.mursaline.kaironex.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment

@Composable
actual fun SimulacrumWebView(
    url: String, 
    modifier: Modifier,
    onClose: () -> Unit
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text("Simulacrum WebView not supported on Desktop yet.\nUrl: $url")
    }
}
