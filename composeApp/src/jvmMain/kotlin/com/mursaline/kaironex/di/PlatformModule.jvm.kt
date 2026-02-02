package com.mursaline.kaironex.di

import com.mursaline.kaironex.brain.AudioRecorder
import com.mursaline.kaironex.brain.AudioPlayer
import com.mursaline.kaironex.core.audio.WakeWordDetector
import com.mursaline.kaironex.core.audio.WakeWordService
import com.mursaline.kaironex.core.audio.createWakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.koin.dsl.module

actual val platformModule = module {
    single<AudioRecorder> { 
        object : AudioRecorder {
            override fun startRecording(onVolumeDetected: () -> Unit): Flow<ByteArray> = emptyFlow()
            override fun stopRecording() {}
        }
    }
    single<AudioPlayer> {
        object : AudioPlayer {
            override fun play(pcmData: ByteArray) {}
            override fun stop() {}
            override fun endStream() {}
            override fun isPlaying(): Boolean = false
        }
    }
    
    // Wake Word Detection (JVM stub)
    single<WakeWordDetector> { createWakeWordDetector() }
    single {
        WakeWordService(
            detector = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        )
    }

    // Persistence
    single<com.mursaline.kaironex.core.storage.ProfileStorage> { 
        com.mursaline.kaironex.core.storage.JvmProfileStorage() 
    }
}
