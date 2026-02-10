package com.mursaline.kaironex.platform

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Android implementation of BackHandler
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
