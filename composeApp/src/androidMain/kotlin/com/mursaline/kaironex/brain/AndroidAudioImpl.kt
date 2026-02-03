package com.mursaline.kaironex.brain

import android.content.Context
import android.media.*
import android.util.Base64
import kotlinx.coroutines.*
import java.io.IOException

class AndroidAudioRecorder(val context: Context) : AudioRecorder {
    private var recorder: AudioRecord? = null
    private var isRecording = false
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun startRecording(onData: (ByteArray) -> Unit) {
        if (isRecording) return
        val sampleRate = 16000 // Standard 16kHz for Speech
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            println("AndroidAudioRecorder: Invalid buffer size")
            return
        }

        try {
             // Check permissions before creating AudioRecord if needed, 
             // but assuming permission check is done in UI layer or Activity
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            if (recorder?.state != AudioRecord.STATE_INITIALIZED) {
                println("AndroidAudioRecorder: AudioRecord initialization failed")
                return
            }

            recorder?.startRecording()
            isRecording = true

            scope.launch {
                val buffer = ByteArray(1024) // Chunk size
                var packets = 0
                while (isRecording) {
                    val read = recorder?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        onData(buffer.copyOfRange(0, read))
                        packets++
                        if (packets % 50 == 0) {
                            val nonZero = buffer.any { it != 0.toByte() }
                            val hasAudio = if (nonZero) "Request has DATA" else "⚠️ SILENCE"
                            println("🎤 Mic sending audio... ($packets chunks) - $hasAudio")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
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
