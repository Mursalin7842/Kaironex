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

class AndroidAudioRecorder(private val context: Context) : AudioRecorder {

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var acousticEchoCanceler: android.media.audiofx.AcousticEchoCanceler? = null
    private var noiseSuppressor: android.media.audiofx.NoiseSuppressor? = null

    // Gemini Input standard is 16kHz. Output is 24kHz.
    // Keeping Input at 16kHz to avoid "Chipmunk" effect.
    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2

    @SuppressLint("MissingPermission")
    override fun startRecording(onVolumeDetected: () -> Unit): Flow<ByteArray> = callbackFlow {
        if (checkPermission()) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    audioRecord = AudioRecord.Builder()
                        .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AUDIO_FORMAT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(CHANNEL_CONFIG)
                                .build()
                        )
                        .setBufferSizeInBytes(BUFFER_SIZE)
                        .setContext(context) // Required for attribution
                        // Context must be the one with attribution tag if we were using createAttributionContext,
                        // but since it's in manifest <application> tag, we just need to associate context.
                        // However, to be explicit per error:
                        // .setAttributionTag("voice_agent") 
                        // Actually, if it's in the manifest <application> tag, the context passed in might need to be created with createAttributionContext?
                        // The error "Attribution not found... pkg=com.mursaline.kaironex(null)" suggests the context used doesn't have the tag derived.
                        // But normally simpler is just:
                        .build()
                    // Re-attempting strictly with the Builder which is better for modern Android anyway.
                    // If we want to strictly set attribution tag, we might need context.createAttributionContext("voice_agent")
                } else {
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        BUFFER_SIZE
                    )
                }

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    close()
                    return@callbackFlow
                }

                audioRecord?.startRecording()
                isRecording = true
                
                // Initialize Audio Effects
                val sessionId = audioRecord?.audioSessionId ?: 0
                if (sessionId != 0) {
                    if (android.media.audiofx.AcousticEchoCanceler.isAvailable()) {
                        acousticEchoCanceler = android.media.audiofx.AcousticEchoCanceler.create(sessionId)
                        acousticEchoCanceler?.enabled = true
                        println("✅ Acoustic Echo Canceler Enabled")
                    } else {
                        println("⚠️ Acoustic Echo Canceler NOT Available")
                    }
                    
                    if (android.media.audiofx.NoiseSuppressor.isAvailable()) {
                        noiseSuppressor = android.media.audiofx.NoiseSuppressor.create(sessionId)
                        noiseSuppressor?.enabled = true
                        println("✅ Noise Suppressor Enabled")
                    } else {
                         println("⚠️ Noise Suppressor NOT Available")
                    }
                }

                println("🎤 AndroidAudioRecorder: Started recording at $SAMPLE_RATE Hz")

                launch(Dispatchers.IO) {
                    // 256ms chunks (4096 samples * 2 bytes = 8192 bytes at 16kHz) - Matches React implementation
                    val readSize = 8192 
                    val buffer = ByteArray(readSize)
                    
                    while (isActive && isRecording) {
                        val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                        if (readResult > 0) {
                            // 1. Calculate RMS Amplitude
                            var sum = 0.0
                            // Check every 4th sample to save CPU
                            for (i in 0 until readResult step 4) {
                                // Little Endian 16-bit
                                if (i + 1 < readResult) {
                                    val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                                    val normalized = sample.toShort() / 32768.0
                                    sum += normalized * normalized
                                }
                            }
                            val rms = kotlin.math.sqrt(sum / (readResult / 4))
                            
                            // 2. Client-Side VAD Trigger (Threshold ~0.1 is usually talking)
                            if (rms > 0.1) { 
                                onVolumeDetected()
                            }

                            // 3. Send Data
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
            acousticEchoCanceler?.enabled = false
            acousticEchoCanceler?.release()
            acousticEchoCanceler = null
            
            noiseSuppressor?.enabled = false
            noiseSuppressor?.release()
            noiseSuppressor = null

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
