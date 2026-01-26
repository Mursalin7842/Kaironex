package com.mursaline.kaironex.brain

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidAudioRecorder(private val context: Context) : AudioRecorder {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    // Gemini 2.5 Native Audio expects 16kHz or 24kHz PCM. 
    // Standard is usually 16kHz, 1 channel, 16-bit PCM.
    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2

    @SuppressLint("MissingPermission")
    override fun startRecording(): Flow<ByteArray> = callbackFlow {
        if (checkPermission()) {
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    BUFFER_SIZE
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    close()
                    return@callbackFlow
                }

                audioRecord?.startRecording()
                isRecording = true
                println("🎤 AndroidAudioRecorder: Started recording at $SAMPLE_RATE Hz")

                launch(Dispatchers.IO) {
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (isActive && isRecording) {
                        val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                        if (readResult > 0) {
                            // Copy the valid bytes
                            val data = buffer.copyOf(readResult)
                            trySend(data)
                        }
                    }
                }

            } catch (e: Exception) {
                println("🎤 AndroidAudioRecorder Error: ${e.message}")
                close()
            }
        } else {
            println("🎤 AndroidAudioRecorder: Missing RECORD_AUDIO permission")
            close()
        }

        awaitClose {
            stopRecordingInternal()
        }
    }

    override fun stopRecording() {
        stopRecordingInternal()
    }

    private fun stopRecordingInternal() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignore
        }
        audioRecord = null
        println("🎤 AndroidAudioRecorder: Stopped")
    }

    private fun checkPermission(): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
