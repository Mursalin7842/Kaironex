package com.mursaline.kaironex.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
actual fun KeepScreenOn(condition: Boolean) {
    val context = LocalContext.current
    DisposableEffect(condition) {
        val activity = context as? Activity
        if (condition) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@Composable
actual fun EnsureMediaPermissions(content: @Composable () -> Unit) {
    var permissionsGranted by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { result ->
            permissionsGranted = result.values.all { it }
        }
    )

    // Check implementation - in Compose we often need to check initial state too, 
    // but RequestMultiplePermissions doesn't return state immediately without launch.
    // simpler to just launch once.
    LaunchedEffect(Unit) {
        launcher.launch(arrayOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO"
        ))
    }

    if (permissionsGranted) {
        content()
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Camera & Microphone Required", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("The simulation requires audio/video input to function.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                launcher.launch(arrayOf(
                    "android.permission.CAMERA",
                    "android.permission.RECORD_AUDIO"
                ))
            }) {
                Text("Grant Permissions")
            }
        }
    }
}
