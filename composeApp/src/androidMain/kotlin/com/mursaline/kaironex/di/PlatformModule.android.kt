package com.mursaline.kaironex.di

import com.mursaline.kaironex.brain.AndroidAudioRecorder
import com.mursaline.kaironex.brain.AudioRecorder
import com.mursaline.kaironex.brain.AndroidAudioPlayer
import com.mursaline.kaironex.brain.AudioPlayer
import com.mursaline.kaironex.core.audio.AndroidWakeWordDetector
import com.mursaline.kaironex.core.audio.WakeWordDetector
import com.mursaline.kaironex.core.audio.WakeWordService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    // Audio Recording/Playback
    single<AudioRecorder> { AndroidAudioRecorder(androidContext()) }
    single<AudioPlayer> { AndroidAudioPlayer(androidContext()) }
    
    // Wake Word Detection
    single<WakeWordDetector> { AndroidWakeWordDetector(androidContext()) }
    
    // Persistence
    single<com.mursaline.kaironex.core.storage.ProfileStorage> { 
        com.mursaline.kaironex.core.storage.AndroidProfileStorage(androidContext()) 
    }
}
