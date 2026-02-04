
package com.mursaline.kaironex.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun SimulacrumWebView(
    url: String, 
    modifier: Modifier = Modifier,
    onClose: () -> Unit
)
