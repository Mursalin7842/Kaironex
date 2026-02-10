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
import com.mursaline.kaironex.ui.theme.KaironexColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.runBlocking
import java.io.File
import org.koin.core.context.startKoin

@Suppress("unused")
fun main() {
    println("🚀 Kaironex Desktop Starting...")
    
    // Global Exception Handler
    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        System.err.println("🔥 Uncaught Exception in thread ${t.name}:")
        e.printStackTrace()
    }

    try {
        startKoin {
            // printLogger() // Optional, logging
            modules(appModule)
        }
        println("✅ Koin Started")

        // Initialize KCEF before application starts (blocking initialization)
        println("🌐 Initializing KCEF...")
        try {
            // Use absolute path based on current working directory
            val workDir = File(System.getProperty("user.dir"))
            val kcefInstallDir = if (workDir.name == "composeApp") {
                File(workDir, "jcef-bundle")
            } else {
                File(workDir, "composeApp/jcef-bundle")
            }
println("📁 KCEF Install Dir: ${kcefInstallDir.absolutePath}")

            // Clear old permission cache BEFORE initializing CEF
            // This ensures fresh permission state
            val prefsFile = File(kcefInstallDir, "cache/Default/Preferences")
            if (prefsFile.exists()) {
                try {
                    println("🧹 Clearing old CEF permission cache...")
                    prefsFile.delete()
                } catch (e: Exception) {
                    println("⚠️ Could not clear permission cache: ${e.message}")
                }
            }

            runBlocking {
                KCEF.init(
                    builder = {
                        installDir(kcefInstallDir)
                        progress {
                            onDownloading { progress ->
                                println("⬇️ KCEF Downloading: ${(progress * 100).toInt()}%")
                            }
                            onInitialized {
                                println("✅ KCEF Initialized Successfully")
                            }
                        }
                        settings {
                            cachePath = File(kcefInstallDir, "cache").absolutePath
                            // Enable remote debugging - access via http://localhost:9222
                            remoteDebuggingPort = 9222
                        }

                        // Add command line args for media stream permissions
                        addArgs(
                            "--enable-media-stream",
                            "--use-fake-ui-for-media-stream",
                            "--autoplay-policy=no-user-gesture-required",
                            "--disable-features=WebRtcHideLocalIpsWithMdns"
                        )
                    },
                    onError = { e: Throwable? ->
                        System.err.println("❌ KCEF Init Error:")
                        e?.printStackTrace()
                    },
                    onRestartRequired = {
                        println("⚠️ KCEF Restart Required")
                    }
                )
            }
            println("✅ KCEF Init Completed")
        } catch (e: Exception) {
            System.err.println("🔥 KCEF Init CRASH (non-fatal, continuing without WebView):")
            e.printStackTrace()
        }

        application {
        // WebView is ready (KCEF initialized before application block)
        val isWebViewReady = true

        // 1. GLOBAL STATE
        // Dummy start state
        var localPressure by remember { mutableStateOf(PressureMap(0.2f, "Routine", "Waiting...", 3)) }
        var remotePressure by remember { mutableStateOf<PressureMap?>(null) }
        var showPopup by remember { mutableStateOf(false) }
        
        // ... rest of the code ...

    val scope = rememberCoroutineScope()

    // --- LOGIC 1: DESKTOP SENSOR (Auto-Dismiss Logic Added) ---
    LaunchedEffect(Unit) {
        DesktopEye.watchActiveWindow().collect { nullableWindowTitle ->
            // Skip if DesktopEye is disabled or not monitoring (returns null)
            val windowTitle = nullableWindowTitle ?: return@collect

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
        state = rememberWindowState(
            width = 1100.dp,
            height = 750.dp,
            position = WindowPosition(Alignment.Center)
        )
    ) {
        // Set minimum window size to prevent UI breaking from unwanted resizing
        window.minimumSize = java.awt.Dimension(900, 650)
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
    } catch (e: Throwable) {
        System.err.println("🔥 FATAL MAIN CRASH:")
        e.printStackTrace()
    }
}