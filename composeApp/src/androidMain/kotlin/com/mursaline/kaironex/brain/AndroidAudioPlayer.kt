package com.mursaline.kaironex.brain

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

// 🔧 TUNING: 1.0 Second Safety Buffer
// 24000 Hz * 2 bytes = 48000 bytes/sec
private const val JITTER_THRESHOLD_BYTES = 48000 

class AndroidAudioPlayer(context: Context) : AudioPlayer {

    private val sampleRate = 24000 
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private val lock = Any()
    private var bufferedBytes = 0

    override fun play(pcmData: ByteArray) {
        synchronized(lock) {
            try {
                // 1. Initialize Track if needed (with 4x capacity)
                if (audioTrack == null) {
                    val minBufferSize = AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    val safeBufferSize = maxOf(minBufferSize * 4, JITTER_THRESHOLD_BYTES * 2)

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

                // 2. Write Data
                val result = audioTrack?.write(pcmData, 0, pcmData.size) ?: 0
                if (result > 0) {
                    bufferedBytes += result
                }

                // 3. THE "GREEDY" START LOGIC
                if (!isPlaying) {
                    // Don't start until we have 1 FULL SECOND of audio
                    if (bufferedBytes >= JITTER_THRESHOLD_BYTES) {
                        println("🚀 Buffer Healthy ($bufferedBytes bytes). Starting Playback.")
                        audioTrack?.play()
                        isPlaying = true
                    }
                } else {
                    // 4. UNDERRUN RECOVERY (The Fix for your Error)
                    // If Android paused the track because it ran dry:
                    if (audioTrack?.playState == AudioTrack.PLAYSTATE_PAUSED) {
                        // Wait for a small cushion (0.25s) before resuming, or it will just crash again
                        if (bufferedBytes >= JITTER_THRESHOLD_BYTES / 4) {
                            println("⚠️ Underrun Recovered. Resuming.")
                            audioTrack?.play()
                        }
                    }
                }
            } catch (e: Exception) {
                println("⚠️ Audio Write Error: ${e.message}")
            }
        }
    }

    override fun stop() {
        synchronized(lock) {
            try {
                if (isPlaying) {
                    audioTrack?.stop()
                    audioTrack?.flush()
                }
                isPlaying = false
                bufferedBytes = 0
            } catch (e: Exception) {
                // Ignore stop errors
            }
        }
    }
    
    override fun isPlaying(): Boolean {
        synchronized(lock) {
            return isPlaying && (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING)
        }
    }
}
