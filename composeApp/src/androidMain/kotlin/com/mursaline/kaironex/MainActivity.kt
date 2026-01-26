package com.mursaline.kaironex

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils.SimpleStringSplitter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mursaline.kaironex.core.audio.initAndroidSpeaker
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

@Suppress("unused")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize TTS Speaker with application context
        initAndroidSpeaker(applicationContext)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission Granted
            } else {
                // Permission Denied
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            // Track if permission is granted
            var isPermissionGranted by remember { mutableStateOf(checkAccessibilityPermission()) }

            // Re-check permission whenever the app resumes (user comes back from settings)
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        isPermissionGranted = checkAccessibilityPermission()
                        com.mursaline.kaironex.platform.AndroidSystemMonitor.isAppInForeground = true
                    } else if (event == Lifecycle.Event.ON_PAUSE) {
                         com.mursaline.kaironex.platform.AndroidSystemMonitor.isAppInForeground = false
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Pass these to your App UI
            org.koin.compose.KoinContext {
                App(
                    isAccessibilityEnabled = isPermissionGranted,
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        startActivity(intent)
                    },
                    onLockTriggered = { /* Android handles this via Service Intent, do nothing here */ }
                )
            }
        }
    }

    // Helper function to check if OUR service is actually on
    private fun checkAccessibilityPermission(): Boolean {
        val componentName = "${packageName}/${KaironexAccessibilityService::class.java.name}"
        val accessibilityEnabled = Settings.Secure.getInt(
            applicationContext.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED, 0
        )

        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(
                applicationContext.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            if (settingValue != null) {
                val splitter = SimpleStringSplitter(':')
                splitter.setString(settingValue)
                while (splitter.hasNext()) {
                    val accessibilityService = splitter.next()
                    if (accessibilityService.equals(componentName, ignoreCase = true)) {
                        return true
                    }
                }
            }
        }
        return false
    }
}