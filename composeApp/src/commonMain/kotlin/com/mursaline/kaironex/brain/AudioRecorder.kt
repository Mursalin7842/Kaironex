package com.mursaline.kaironex.brain

import kotlinx.coroutines.flow.Flow

interface AudioRecorder {
    fun startRecording(onVolumeDetected: () -> Unit): Flow<ByteArray>
    fun stopRecording()
}
