package com.mursaline.kaironex

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KaironexAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onServiceConnected() {
        super.onServiceConnected()

        // --- DYNAMIC CONFIGURATION ---
        // This ensures we listen to EVERYTHING, bypassing any XML issues.
        val info = AccessibilityServiceInfo()

        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100

        // Use 0 for default, plus ensure we see container views
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS

        this.serviceInfo = info

        println("✅ MOBILE EYE: Service Connected & Configured Dynamically!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Safely get the package name (the app the user is looking at)
        val packageName = event.packageName?.toString() ?: return

        // 1. LOG EVERYTHING (For debugging)
        println("👁️ MOBILE EYE SEES: $packageName")

        // 2. DETECT DISTRACTION
        // Add any other apps you want to block here
        if (packageName.contains("youtube", ignoreCase = true) ||
            packageName.contains("netflix", ignoreCase = true) ||
            packageName.contains("tiktok", ignoreCase = true) ||
            packageName.contains("facebook", ignoreCase = true) ||
            packageName.contains("instagram", ignoreCase = true)) {

            println("🚨 FOUND DISTRACTION: $packageName")

            // 3. SEND SIGNAL TO CLOUD (Syncs with Desktop)
            serviceScope.launch {
                try {
                    FirebaseSync.updateState(
                        pressure = 0.9f,
                        status = "Mobile Distraction",
                        appName = "Mobile: $packageName"
                    )
                    println("📤 SENT SIGNAL TO CLOUD!")
                } catch (e: Exception) {
                    println("❌ SEND FAILED: ${e.message}")
                }
            }

            // 4. THE ENFORCER: LOCK THE SCREEN 🛡️
            // This forces the Kaironex App to open ON TOP of the distraction.
            try {
                val intent = Intent(this, MainActivity::class.java)
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK)    // Mandatory for Service -> Activity
                intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP)  // Don't open duplicates
                intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP)   // Reset the app stack
                startActivity(intent)
                println("🚀 ENFORCER ACTIVATED: Kaironex pulled to front!")
            } catch (e: Exception) {
                println("❌ ENFORCER FAILED: ${e.message}")
            }
        }
    }

    override fun onInterrupt() {
        // Required override, but we don't need to do anything here.
    }
}