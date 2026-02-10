package com.mursaline.kaironex.platform

interface SystemMonitor {
    fun isWindowFocused(): Boolean
    fun getCurrentProcessName(): String
}

expect fun getSystemMonitor(): SystemMonitor
