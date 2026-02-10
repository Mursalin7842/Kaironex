package com.mursaline.kaironex.brain

import android.content.Context
import android.media.*
import android.util.Base64
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.io.IOException

class AndroidAudioRecorder(val context: Context) : AudioRecorder {
    private var recorder: AudioRecord? = null
    private var isRecording = false
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun startRecording(onData: (ByteArray) -> Unit) {
        if (isRecording) return

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
             println("🔴 Kaironex: RECORD_AUDIO permission not granted")
             return
        }

        // [FIX] Gemini Live Input MUST be 16kHz (16000), not 24kHz
        val sampleRate = 16000 
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        // Use a slightly larger buffer to ensure smooth streaming
        val bufferSize = maxOf(minBufferSize, 4096)

        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION, // [FIX] Better for speech than MIC
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
                println("🔴 Kaironex: AudioRecord failed to initialize")
                return
            }

            recorder?.startRecording()
            isRecording = true
            println("mic started with 16k rate")

            scope.launch {
                val buffer = ByteArray(8192) // Larger chunks (approx 256ms) to match JS reference
                var packets = 0
                while (isRecording) {
                    val read = recorder?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // Create a copy of the exact bytes read
                        val data = buffer.copyOfRange(0, read)
                        onData(data)
                        
                        // Debug log occasional packet
                        packets++
                        if (packets % 50 == 0) {
                             val nonZero = buffer.any { it != 0.toByte() }
                             val hasAudio = if (nonZero) "DATA" else "SILENCE"
                             println("🎤 Mic sending ($packets) - $hasAudio")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("🔴 Kaironex Rec Error: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun stopRecording() {
        isRecording = false
        try {
            recorder?.stop()
            recorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recorder = null
    }
}

class AndroidAudioPlayer(val context: Context) : AudioPlayer {
    private var audioTrack: AudioTrack? = null
    private val sampleRate = 24000 // Gemini Native Output

    override fun playBase64(base64String: String) {
        try {
            val audioData = Base64.decode(base64String, Base64.DEFAULT)
            
            if (audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                println("🔊 Initializing AudioTrack (Stream Mode, ${sampleRate}Hz)")
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(AudioTrack.getMinBufferSize(
                        sampleRate, 
                        AudioFormat.CHANNEL_OUT_MONO, 
                        AudioFormat.ENCODING_PCM_16BIT
                    ) * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                
                audioTrack?.play()
            }

            // Write data to the streaming track
            val bytesWritten = audioTrack?.write(audioData, 0, audioData.size) ?: 0
            if (bytesWritten < 0) {
                 println("⚠️ AudioTrack write failed: $bytesWritten")
            }
        } catch (e: Exception) {
            println("❌ Audio playback failed: ${e.message}")
            e.printStackTrace()
            // Try to recover next time
            audioTrack?.release()
            audioTrack = null
        }
    }
}
