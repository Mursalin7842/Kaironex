package com.mursaline.kaironex.platform

import androidx.compose.runtime.Composable

/**
 * Multiplatform BackHandler
 * Handles back button/gesture on different platforms
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
