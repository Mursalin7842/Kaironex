package com.mursaline.kaironex.core

import androidx.compose.runtime.Composable

@Composable
expect fun EnsureAudioPermission(onGranted: () -> Unit)
