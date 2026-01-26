package com.mursaline.kaironex.brain

interface AudioPlayer {
    fun play(pcmData: ByteArray)
    fun stop()
}
