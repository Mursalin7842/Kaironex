package com.mursaline.kaironex

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.mursaline.kaironex.di.appModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        // printLogger() // Optional, logging
        modules(appModule)
    }

    application {
    // 1. GLOBAL STATE
    // Dummy start state
    var localPressure by remember { mutableStateOf(PressureMap(0.2f, "Routine", "Waiting...", 3)) }
    var remotePressure by remember { mutableStateOf<PressureMap?>(null) }
    var showPopup by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // --- LOGIC 1: DESKTOP SENSOR (Auto-Dismiss Logic Added) ---
    LaunchedEffect(Unit) {
        DesktopEye.watchActiveWindow().collect { windowTitle ->

            println("👁️ WATCHING: $windowTitle")

            // Detect Status
            var newPressure = 0.2f
            var newStatus = "Routine"

            val isDistracted = windowTitle.contains("Netflix", ignoreCase = true) ||
                    windowTitle.contains("YouTube", ignoreCase = true)

            if (isDistracted) {
                newPressure = 0.9f
                newStatus = "Distracted"
            }

            // Update Local Data
            localPressure = localPressure.copy(
                activeTask = if(isDistracted) "Distraction: $windowTitle" else "Focus: $windowTitle",
                stakes = if(isDistracted) "Risk Increasing..." else "Routine",
                cognitiveLoad = newPressure
            )

            // ✅ AUTO-DISMISS LOGIC:
            // If distracted -> Show Popup.
            // If NOT distracted -> Hide Popup immediately.
            showPopup = isDistracted

            // Push to Cloud
            scope.launch { FirebaseSync.updateState(newPressure, newStatus, windowTitle) }
        }
    }

    // --- LOGIC 2: CLOUD LISTENER (Auto-Dismiss from Phone) ---
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val cloudState = FirebaseSync.getState()
                if (cloudState != null) {
                    remotePressure = PressureMap(cloudState.pressure, cloudState.status, "Synced: ${cloudState.activeApp}", 3)

                    // If the cloud pressure drops (phone distraction closed), hide the popup
                    if (cloudState.pressure > 0.8f) {
                        showPopup = true
                    } else {
                        // Only hide if local desktop isn't distracted too
                        if (localPressure.cognitiveLoad < 0.8f) {
                            showPopup = false
                        }
                    }
                }
            } catch (e: Exception) {}
            delay(1000)
        }
    }

    // Combine Logic for UI
    val displayPressure = remember(localPressure, remotePressure) {
        val remote = remotePressure
        if (remote != null && remote.cognitiveLoad > localPressure.cognitiveLoad) remote else localPressure
    }

    // --- WINDOW 1: DASHBOARD ---
    Window(
        onCloseRequest = ::exitApplication,
        title = "Kaironex",
        state = rememberWindowState(width = 800.dp, height = 600.dp)
    ) {
        App(
            sensorStream = DesktopEye.watchActiveWindow(),
            isAccessibilityEnabled = true,
            onOpenSettings = {},
            onLockTriggered = { }
        )
    }

    // --- WINDOW 2: POPUP ALERT ---
    if (showPopup) {
        Window(
            onCloseRequest = {},
            title = "Kaironex Security",
            state = rememberWindowState(
                width = 500.dp,
                height = 350.dp,
                position = WindowPosition(Alignment.Center)
            ),
            alwaysOnTop = true,
            undecorated = true,
            resizable = false,
            transparent = true
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFB71C1C),
                shadowElevation = 15.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("⚠️", fontSize = 60.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("SYSTEM LOCKED", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("High cognitive load detected.\nFocus is compromised.", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.9f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            // Manual override still works
                            showPopup = false
                            scope.launch { FirebaseSync.updateState(0.3f, "Routine", "Manual Unlock") }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(55.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("NEGOTIATE UNLOCK", color = Color(0xFFB71C1C), fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
        }
    }
    }
}