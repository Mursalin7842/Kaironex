package com.mursaline.kaironex.di

import com.mursaline.kaironex.brain.AndroidAudioRecorder
import com.mursaline.kaironex.brain.AudioRecorder
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import com.mursaline.kaironex.brain.AndroidAudioPlayer
import com.mursaline.kaironex.brain.AudioPlayer

actual val platformModule = module {
    single<AudioRecorder> { AndroidAudioRecorder(androidContext()) }
    single<AudioPlayer> { AndroidAudioPlayer() }
}
