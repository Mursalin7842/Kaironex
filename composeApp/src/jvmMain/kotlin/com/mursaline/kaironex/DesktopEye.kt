package com.mursaline.kaironex

import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object DesktopEye {
    // This tells Kaironex how to "read" the window title buffer
    private val MAX_TITLE_LENGTH = 1024

    // This function creates a stream of data (Flow) that updates every second
    fun watchActiveWindow(): Flow<String> = flow {
        val buffer = CharArray(MAX_TITLE_LENGTH)
        val user32 = User32.INSTANCE

        while (true) {
            // 1. Get the "Handle" (ID) of the currently active window
            val hwnd = user32.GetForegroundWindow()

            // 2. Read the title text of that window
            val length = user32.GetWindowText(hwnd, buffer, MAX_TITLE_LENGTH)
            val windowTitle = if (length > 0) {
                String(buffer, 0, length)
            } else {
                "Unknown / Idle"
            }

            // 3. Emit the result to the app
            emit(windowTitle)

            // 4. Wait 1 second before checking again
            delay(1000)
        }
    }
}