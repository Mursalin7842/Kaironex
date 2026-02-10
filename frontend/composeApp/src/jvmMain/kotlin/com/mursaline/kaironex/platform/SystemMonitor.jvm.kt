package com.mursaline.kaironex.platform

import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.ptr.IntByReference

class JvmSystemMonitor : SystemMonitor {
    
    // Cache our own PID
    private val myPid: Int by lazy {
        // Handle Java 9+ ProcessHandle or fallback
        // For simplicity in this env, we assume standard JVM
        try {
            java.lang.management.ManagementFactory.getRuntimeMXBean().name.split("@")[0].toInt()
        } catch (e: Exception) {
            0
        }
    }

    override fun isWindowFocused(): Boolean {
        // 1. Get Foreground Window Handle
        val foregroundHwnd = User32.INSTANCE.GetForegroundWindow()
        
        if (foregroundHwnd == null) return false

        // 2. Get PID of that window
        val pidRef = IntByReference()
        User32.INSTANCE.GetWindowThreadProcessId(foregroundHwnd, pidRef)
        val foregroundPid = pidRef.value

        // 3. Compare with our PID
        // If our app is the foreground window, user is "in the box"
        return foregroundPid == myPid
    }

    override fun getCurrentProcessName(): String {
         return "Kaironex-JVM-$myPid"
    }
}

actual fun getSystemMonitor(): SystemMonitor = JvmSystemMonitor()
