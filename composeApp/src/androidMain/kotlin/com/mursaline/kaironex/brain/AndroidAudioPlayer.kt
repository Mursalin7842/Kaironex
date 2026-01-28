package com.mursaline.kaironex.brain

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

// 🔧 TUNING: Low Latency for Real-time Conversation (~80ms)
// 24000 Hz * 2 bytes = 48000 bytes/sec
private const val JITTER_THRESHOLD_BYTES = 4000

class AndroidAudioPlayer(context: Context) : AudioPlayer {

    private val sampleRate = 24000
    private var audioTrack: AudioTrack? = null
    @Volatile private var isPlaying = false
    private val lock = Any()
    
    // Track total bytes written to calculate pending buffer
    private var totalBytesWritten = 0L

    override fun play(pcmData: ByteArray) {
        synchronized(lock) {
            try {
                if (pcmData.isEmpty()) return

                // 1. Initialize Track if needed
                if (audioTrack == null) {
                    val minBufferSize = AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    // INCREASED BUFFER: 8x Min Buffer or 3s worth of audio to handle slow networks
                    val safeBufferSize = maxOf(minBufferSize * 8, JITTER_THRESHOLD_BYTES * 3)

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
                    totalBytesWritten = 0L // Reset on new track
                }

                // 2. Write Data
                val result = audioTrack?.write(pcmData, 0, pcmData.size) ?: 0
                if (result < 0) {
                     println("⚠️ Audio Write Failed (Code: $result). Recreating Track.")
                     try { audioTrack?.release() } catch(e:Exception){}
                     audioTrack = null
                     return
                }
                totalBytesWritten += result

                // 3. Calculate Actual Buffer Health
                // Head position is in FRAMES. 1 Frame = 2 Bytes (16-bit Mono)
                // WARN: playbackHeadPosition wrap-around is possible but unlikely in one session (hours of audio)
                val playedFrames = audioTrack?.playbackHeadPosition?.toLong() ?: 0L
                val playedBytes = playedFrames * 2
                val pendingBytes = totalBytesWritten - playedBytes

                // 4. Smart Playback Control & Underrun Recovery
                if (!isPlaying) {
                    // Start Threshold: Wait for solid buffer
                    if (pendingBytes >= JITTER_THRESHOLD_BYTES) {
                        println("🚀 Buffer Healthy ($pendingBytes bytes >= $JITTER_THRESHOLD_BYTES). Starting Playback.")
                        audioTrack?.play()
                        isPlaying = true
                    }
                } else {
                    // RECOVERY: The OS might stop the track if it underruns ("dry").
                    // We must check the actual hardware state, not just our boolean.
                    val state = audioTrack?.playState
                    
                    if (state == AudioTrack.PLAYSTATE_PAUSED || state == AudioTrack.PLAYSTATE_STOPPED) {
                        // If we have *any* meaningful data, force restart.
                        // We lower the threshold here to avoid a "stutter-stop-buffer" loop.
                        if (pendingBytes >= JITTER_THRESHOLD_BYTES / 8) {
                            println("♻️ UNDERRUN RECOVERY: Restarting Track ($pendingBytes bytes pending).")
                            audioTrack?.play()
                        }
                    }
                }
            } catch (e: Exception) {
                println("⚠️ Audio Write Error: ${e.message}")
                try { audioTrack?.release() } catch(e:Exception){}
                audioTrack = null
                isPlaying = false
                totalBytesWritten = 0L
            }
        }
    }

    override fun stop() {
        synchronized(lock) {
            try {
                if (isPlaying) {
                     // Pause and flush to kill immediate sound
                    audioTrack?.pause()
                    audioTrack?.flush()
                }
                isPlaying = false
                totalBytesWritten = 0L // Reset counter as flush clears hardware buffer
                // We keep the track to avoid expensive re-init
            } catch (e: Exception) {
            }
        }
    }

    override fun endStream() {
        synchronized(lock) {
            try {
                if (isPlaying) {
                    // Let it drain naturally
                    audioTrack?.stop() 
                    println("🛑 Audio Stream Ended (Draining Buffer)")
                }
                isPlaying = false
                // Do NOT reset totalBytesWritten here as we want it to finish playing what was written
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
