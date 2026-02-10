package com.mursaline.kaironex.core.gemini

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val sender: String,
    val content: String,
    val timestamp: Long = 0,
    val role: String = "user"
)
