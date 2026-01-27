package com.mursaline.kaironex.brain

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

// 🔧 TUNING: 1.5 Second Safety Buffer
// 24000 Hz * 2 bytes = 48000 bytes/sec
private const val JITTER_THRESHOLD_BYTES = 72000

class AndroidAudioPlayer(context: Context) : AudioPlayer {

    private val sampleRate = 24000
    private var audioTrack: AudioTrack? = null
    @Volatile private var isPlaying = false
    private val lock = Any()
    private var bufferedBytes = 0

    override fun play(pcmData: ByteArray) {
        synchronized(lock) {
            try {
                // 0. Safety Check
                if (pcmData.isEmpty()) return

                // 1. Initialize Track if needed (with 8x capacity for safety)
                if (audioTrack == null) {
                    val minBufferSize = AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    // Massive buffer to absorb network jitter
                    val safeBufferSize = maxOf(minBufferSize * 8, JITTER_THRESHOLD_BYTES * 2)

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
                        .setBufferSizeInBytes(safeBufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                        
                    Log.d("KaironexAudio", "🔊 AudioTrack Initialized at $sampleRate Hz (Buffer: $safeBufferSize bytes)")
                }

                // 2. Write Data with Resilience
                val result = audioTrack?.write(pcmData, 0, pcmData.size) ?: 0
                if (result < 0) {
                     println("⚠️ Audio Write Failed (Code: $result). Recreating Track.")
                     // If write fails (e.g. track dead), valid strategy is to null it so it rebuilds next frame
                     audioTrack = null
                     return
                }
                bufferedBytes += result

                // 3. THE "GREEDY" START LOGIC
                if (!isPlaying) {
                     // Don't start until we have 1.5 SECONDS of audio
                    if (bufferedBytes >= JITTER_THRESHOLD_BYTES) {
                        println("🚀 Buffer Healthy ($bufferedBytes bytes). Starting Playback.")
                        audioTrack?.play()
                        isPlaying = true
                    }
                } else {
                    // 4. UNDERRUN RECOVERY
                    if (audioTrack?.playState == AudioTrack.PLAYSTATE_PAUSED) {
                        if (bufferedBytes >= JITTER_THRESHOLD_BYTES / 2) { // Wait for 50% refill
                            println("⚠️ Underrun Recovered. Resuming.")
                            audioTrack?.play()
                        }
                    }
                }
            } catch (e: Exception) {
                println("⚠️ Audio Write Error: ${e.message}")
                 // Force reset on critical error
                try { audioTrack?.release() } catch(e:Exception){}
                audioTrack = null
                isPlaying = false
                bufferedBytes = 0
            }
        }
    }

    override fun stop() {
        synchronized(lock) {
            try {
                if (isPlaying) {
                    audioTrack?.pause() // Pause immediately
                    audioTrack?.flush() // Clear buffer
                }
                isPlaying = false
                bufferedBytes = 0
            } catch (e: Exception) {
                // Ignore stop errors
            }
        }
    }

    override fun endStream() {
        synchronized(lock) {
            try {
                if (isPlaying) {
                     // STOP triggers the "drain" mode in AudioTrack.
                     // It plays remaining data then pauses.
                    audioTrack?.stop() 
                    println("🛑 Audio Stream Ended (Draining Buffer)")
                }
                isPlaying = false // Logic assumes stopped, let hardware drain
                bufferedBytes = 0
            } catch (e: Exception) {
                println("⚠️ Audio Drain Error: ${e.message}")
            }
        }
    }
    
    override fun isPlaying(): Boolean {
        synchronized(lock) {
            return isPlaying && (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING)
        }
    }
}
