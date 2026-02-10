package com.mursaline.kaironex.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun EnsureAudioPermission(onGranted: () -> Unit) {
    // Desktop permission usually strictly OS level, no runtime request needed in same way
    LaunchedEffect(Unit) {
        onGranted()
    }
}
