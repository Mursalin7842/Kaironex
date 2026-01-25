package com.mursaline.kaironex.brain

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import io.ktor.util.encodeBase64

class GeminiReasoningEngine(private val client: HttpClient) {

    // --- FIX #1: THE URL ---
    // REMOVED: "/v1beta"
    // ADDED: "/ws" direct path
    private val BASE_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private var session: DefaultClientWebSocketSession? = null

    suspend fun connect(apiKey: String) {
        try {
            _connectionState.value = ConnectionState.Connecting
            println("🔌 Connecting to Gemini Live...")

            // Construct Auth URL
            val urlString = "$BASE_URL?key=$apiKey"

            client.webSocket(urlString) {
                session = this
                _connectionState.value = ConnectionState.Connected
                println("⚡ SOCKET OPENED. Sending Handshake...")

                // --- FIX #2: THE HANDSHAKE ---
                // We MUST send this immediately, or the connection closes with 400.
                sendHandshake()

                for (frame in incoming) {
                    when (frame) {
                        is Frame.Text -> {
                            val text = frame.readText()
                            println("📥 Gemini Text: $text")
                            // TODO: Parse "serverContent" JSON here to trigger UI updates
                        }
                        is Frame.Binary -> {
                            // TODO: Pass audio bytes to Speaker
                            println("🔊 Gemini Audio Packet Received")
                        }
                        else -> {}
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ CRITICAL FAILURE: ${e.message}")
            e.printStackTrace()
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown Connection Error")
        } finally {
            _connectionState.value = ConnectionState.Disconnected
            println("🔌 Socket Closed")
        }
    }

    private suspend fun DefaultClientWebSocketSession.sendHandshake() {
        // --- FIX #3: THE MODEL NAME ---
        // Using "gemini-2.0-flash-exp" is REQUIRED for the Live API to work.
        val handshakeJson = """
        {
          "setup": {
            "model": "models/gemini-2.5-flash-native-audio-preview-12-2025",
            "generationConfig": {
              "responseModalities": ["AUDIO"],
              "speechConfig": {
                "voiceConfig": { "prebuiltVoiceConfig": { "voiceName": "Aoede" } }
              }
            }
          }
        }
        """.trimIndent()

        send(Frame.Text(handshakeJson))
        println("The Handshake sent. Waiting for server...")
    }

    suspend fun sendAudio(pcmData: ByteArray) {
        if (session?.isActive == true) {
            // Real-time audio chunks to Gemini
            val json = """
            {
                "realtime_input": {
                    "media_chunks": [
                        {
                            "mime_type": "audio/pcm",
                            "data": "${pcmData.encodeBase64()}"
                        }
                    ]
                }
            }
            """.trimIndent()
            session?.send(Frame.Text(json))
        }
    }
    
    suspend fun disconnect() {
        session?.close()
        _connectionState.value = ConnectionState.Disconnected
    }

    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Connecting : ConnectionState()
        data object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }
}
