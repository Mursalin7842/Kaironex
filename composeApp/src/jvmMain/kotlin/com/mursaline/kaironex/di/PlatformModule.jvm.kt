package com.mursaline.kaironex.di

import com.mursaline.kaironex.brain.AudioRecorder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.koin.dsl.module

import com.mursaline.kaironex.brain.AudioPlayer

actual val platformModule = module {
    single<AudioRecorder> { 
        object : AudioRecorder {
            override fun startRecording(): Flow<ByteArray> = emptyFlow()
            override fun stopRecording() {}
        }
    }
    single<AudioPlayer> {
        object : AudioPlayer {
            override fun play(pcmData: ByteArray) {}
            override fun stop() {}
        }
    }
}
