package com.mursaline.kaironex.platform

class AndroidSystemMonitor : SystemMonitor {
    
    // This needs to be updated by MainActivity onResume/onPause
    // A simple static flag is enough for the "Single Activity" Kaironex architecture
    companion object {
        var isAppInForeground = false
    }

    override fun isWindowFocused(): Boolean {
        return isAppInForeground
    }

    override fun getCurrentProcessName(): String {
        return "com.mursaline.kaironex"
    }
}

actual fun getSystemMonitor(): SystemMonitor = AndroidSystemMonitor()
