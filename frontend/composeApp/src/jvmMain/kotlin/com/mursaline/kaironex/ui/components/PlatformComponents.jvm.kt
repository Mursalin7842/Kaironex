package com.mursaline.kaironex.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun KeepScreenOn(condition: Boolean) {
    // No-op for Desktop/JVM
}

@Composable
actual fun EnsureMediaPermissions(content: @Composable () -> Unit) {
    // Permissions are usually implicit or handled differently on Desktop
    content()
}
