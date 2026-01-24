package com.mursaline.kaironex.platform

import androidx.compose.runtime.Composable

/**
 * Desktop/JVM implementation of BackHandler
 * On desktop, we don't have a system back button
 * The close button on the overlay handles dismissal
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op on desktop - close button handles dismissal
}
