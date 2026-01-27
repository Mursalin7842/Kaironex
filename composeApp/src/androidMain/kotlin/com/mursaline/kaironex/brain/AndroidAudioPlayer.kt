package com.mursaline.kaironex.brain

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

class AndroidAudioPlayer : AudioPlayer {

    private var audioTrack: AudioTrack? = null
    companion object {
        // ------------------------------------------------------------
        // 🔧 TUNING FOR 24kHz (Gemini Live)
        // ------------------------------------------------------------
        // ✅ NEW (Doubled for stability)
        // We need more data buffered before starting to prevent "underrun"
        private const val JITTER_THRESHOLD_BYTES = 1024 * 10 
    } 
    
    // Gemini Live output sample rate. using 24kHz to match Gemini output.
    private val SAMPLE_RATE = 24000
    // Ensure we are not accidentally playing the JSON text as audio bytes?
    // In Frame.Binary, we play bytes directly. If Gemini sends JSON in Binary frame? No.
    
    // Let's try 24000 first, but maybe the issue is little endian vs big endian? Android is Little Endian (ENCODING_PCM_16BIT).
    
    // Actually, let's try 24000 -> 16000?
    // Usage: Input 16k. Output 24k.
    
    // Let's stick to 24000 but add a check. 
    
    // Actually, I will try 24000.
    private val SAMPLE_RATE_FIXED = 24000 
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    // Increase buffer massively (8x) to ensure smooth playback even with network jitter
    private val BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 8
    
    // Jitter Buffer: Wait for this many bytes before starting playback
    // 24000Hz * 2 bytes = 48000 bytes/sec. 
    // Target 200ms = 9600 bytes.
    // minBufferSize is likely ~4800 (100ms).
    // Let's stick to minBufferSize * 1 for safety but fast start.
    // Jitter Buffer: Smart balance. 
    // MinBuffer * 1 (approx 50ms-100ms) prevents chop but starts fast.
    // Jitter Buffer: 2x min buffer (~160ms) - Balanced for speed + stability with underrun recovery
    // Jitter Buffer: 2x min buffer (~160ms) - Balanced for speed + stability with underrun recovery
    // private val START_THRESHOLD = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 2
    private var bytesBuffered = 0
    private var isPlayingState = false

    override fun isPlaying(): Boolean = isPlayingState

    init {
        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build()
                )
                .setBufferSizeInBytes(BUFFER_SIZE)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            Log.d("KaironexAudio", "🔊 AudioTrack Initialized at $SAMPLE_RATE Hz (Jitter Buffer: $JITTER_THRESHOLD_BYTES bytes)")
        } catch (e: Exception) {
            Log.e("KaironexAudio", "❌ Failed to init AudioTrack: ${e.message}")
        }
    }

    override fun play(pcmData: ByteArray) {
        try {
             if (audioTrack == null) return

            // 0. Underrun Recovery Check
            // If we think we are playing, but the track stopped, it's an underrun.
            if (isPlayingState && audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                Log.w("KaironexAudio", "⚠️ Audio Underrun Detected! Re-buffering...")
                isPlayingState = false
                bytesBuffered = 0 // Force re-buffer
                audioTrack?.flush()
            }

            // 1. Write data to the buffer
            val bytesWritten = audioTrack?.write(pcmData, 0, pcmData.size) ?: 0
            
            if (bytesWritten > 0) {
                bytesBuffered += bytesWritten
            }
            
            // 2. Check if we should start playing (Jitter Buffer Logic)
            // 2. Check if we should start playing (Jitter Buffer Logic)
            if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                if (bytesBuffered >= JITTER_THRESHOLD_BYTES) {
                    Log.d("KaironexAudio", "🚀 Jitter Buffer Full ($bytesBuffered bytes). Starting Playback.")
                    audioTrack?.play()
                    isPlayingState = true
                }
            }
        } catch (e: Exception) {
            Log.e("KaironexAudio", "❌ Write failed: ${e.message}")
        }
    }

    override fun stop() {
        try {
            isPlayingState = false
            audioTrack?.pause()
            audioTrack?.flush()
            bytesBuffered = 0 // Reset jitter buffer count
            Log.d("KaironexAudio", "🛑 Audio Stopped & Flushed")
        } catch (e: Exception) {
            // Ignore
        }
    }
}
