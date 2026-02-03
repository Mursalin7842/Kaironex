package com.mursaline.kaironex.brain

interface AudioRecorder {
    fun startRecording(onData: (ByteArray) -> Unit)
    fun stopRecording()
}

interface AudioPlayer {
    fun playBase64(base64String: String)
}
