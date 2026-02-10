package com.mursaline.kaironex.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun KeepScreenOn(condition: Boolean)

@Composable
expect fun EnsureMediaPermissions(content: @Composable () -> Unit)
