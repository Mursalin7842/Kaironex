package com.mursaline.kaironex.di

import com.mursaline.kaironex.brain.AudioRecorder
import com.mursaline.kaironex.brain.AudioPlayer
import org.koin.dsl.module

actual val platformModule = module {
    single<AudioRecorder> { 
        object : AudioRecorder {
            override fun startRecording(onData: (ByteArray) -> Unit) { /* JVM stub */ }
            override fun stopRecording() {}
        }
    }
    single<AudioPlayer> {
        object : AudioPlayer {
            override fun playBase64(base64String: String) { /* JVM stub */ }
        }
    }

    // Persistence
    single<com.mursaline.kaironex.core.storage.ProfileStorage> { 
        com.mursaline.kaironex.core.storage.JvmProfileStorage() 
    }
}
