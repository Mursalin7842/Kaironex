package com.mursaline.kaironex.brain

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

class AndroidAudioPlayer : AudioPlayer {

    private var audioTrack: AudioTrack? = null
    // Gemini Live output sample rate. Trying 16kHz to match input if 24kHz was wrong.
    private val SAMPLE_RATE = 24000 // Reverting to 24000 as per spec, but let's double check logic.
    // Wait, user said "only noise".
    // If I play Base64 STRING as PCM, it sounds like static noise.
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
    // Reduce buffer to 4x to minimize latency while maintaining stability
    private val BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) * 4

    init {
        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
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

            audioTrack?.play()
            Log.d("KaironexAudio", "🔊 AudioTrack Initialized at $SAMPLE_RATE Hz")
        } catch (e: Exception) {
            Log.e("KaironexAudio", "❌ Failed to init AudioTrack: ${e.message}")
        }
    }

    override fun play(pcmData: ByteArray) {
        try {
            if (audioTrack?.playState != AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack?.play()
            }
            audioTrack?.write(pcmData, 0, pcmData.size)
        } catch (e: Exception) {
            Log.e("KaironexAudio", "❌ Write failed: ${e.message}")
        }
    }

    override fun stop() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
