package com.mursaline.kaironex.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.UiComposable
import androidx.compose.ui.ExperimentalComposeUiApi

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@UiComposable
fun AuthLayout(
    navToSignup: Boolean = false,
    content: @Composable @UiComposable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB)), // Gray-50 equivalent
        contentAlignment = Alignment.Center
    ) {
        // The Main Card Container
        Box(
            modifier = Modifier
                .widthIn(max = 1000.dp)
                .fillMaxWidth(0.95f) // Slightly wider on small screens
                .heightIn(min = 600.dp)
                .clip(RoundedCornerShape(24.dp)) // Rounded-3xl approx
                .background(Color.White)
                .graphicsLayer {
                    shadowElevation = 16.dp.toPx() // Shadow-2xl approx
                    shape = RoundedCornerShape(24.dp)
                    clip = true
                }
        ) {
            content()
        }
    }
}
