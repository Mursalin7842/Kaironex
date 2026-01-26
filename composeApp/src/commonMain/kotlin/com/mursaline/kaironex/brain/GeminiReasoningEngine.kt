package com.mursaline.kaironex.brain

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import io.ktor.util.encodeBase64
import io.ktor.util.decodeBase64Bytes

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class GeminiReasoningEngine(
    private val client: HttpClient,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer
) {

    // ✅ USE v1beta for the Live API
    private val BASE_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private var session: DefaultClientWebSocketSession? = null

    suspend fun connect(apiKey: String, systemInstruction: String? = null) {
        try {
            _connectionState.value = ConnectionState.Connecting
            println("🔌 Connecting to Live API (Native Audio Dialog)...")

            val urlString = "$BASE_URL?key=$apiKey"

            client.webSocket(urlString) {
                session = this
                _connectionState.value = ConnectionState.Connected
                println("⚡ SOCKET OPENED. Sending Handshake...")

                sendHandshake(systemInstruction)

                // LAUNCH AUDIO RECORDER
                val audioJob = launch {
                    println("🎤 Starting Audio Recorder...")
                    try {
                        audioRecorder.startRecording().collect { pcmData ->
                            sendAudio(pcmData)
                        }
                    } catch (e: Exception) {
                        if (e is kotlinx.coroutines.CancellationException) {
                            println("🎤 Audio Recorder Cancelled (Normal)")
                        } else {
                            println("🎤 Audio Recorder Crashed: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                }
// ...
        val handshakeJson2 = """
        {
          "setup": {
            "model": "models/gemini-2.0-flash-exp",
            "generationConfig": {
              "responseModalities": ["AUDIO"],
              "speechConfig": {
                "voiceConfig": {
                  "prebuiltVoiceConfig": {
                    "voiceName": "Puck"
                  }
                }
              }
            }
          }
        }
        """.trimIndent()

                try {
                    for (frame in incoming) {
                        val messageText = when (frame) {
                            is Frame.Text -> frame.readText()
                            is Frame.Binary -> String(frame.readBytes()) // Try to decode binary as UTF-8 JSON
                            else -> null
                        }

                        if (messageText != null) {
                            // Print first 100 chars to debug
                            // println("📥 Rx: ${messageText.take(100)}...") 
                            
                            if (messageText.contains("\"interrupted\": true")) {
                                println("🛑 Interruption detected! Clearing audio buffer.")
                                audioPlayer.stop()
                            }

                            if (messageText.contains("audio/pcm")) {
                                try {
                                    val dataMarker = "\"data\": \""
                                    val startIndex = messageText.indexOf(dataMarker)
                                    if (startIndex != -1) {
                                        val startQuote = startIndex + dataMarker.length
                                        val endQuote = messageText.indexOf("\"", startQuote)
                                        if (endQuote != -1) {
                                            val base64 = messageText.substring(startQuote, endQuote)
                                            val pcm = base64.decodeBase64Bytes()
                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                                audioPlayer.play(pcm)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    println("❌ Audio Decode Error: ${e.message}")
                                }
                            }
                        }

                        if (frame is Frame.Close) {
                            println("📪 Server requested close: ${frame.readReason()}")
                        }
                    }
                    println("📪 Incoming loop finished (Server closed connection?)")
                } catch (e: Exception) {
                    println("❌ Error in incoming loop: ${e.message}")
                } finally {
                    val reason = session?.closeReason?.await()
                    println("📪 Connection closed. Reason: $reason")
                    audioJob.cancel()
                    audioPlayer.stop()
                    println("🎤 Audio Recorder Stopped")
                }
            }
        } catch (e: Exception) {
            println("❌ SOCKET ERROR: ${e.message}")
            _connectionState.value = ConnectionState.Error(e.message ?: "Unknown Error")
        } finally {
             audioRecorder.stopRecording()
        }
    }

    private suspend fun DefaultClientWebSocketSession.sendHandshake(systemInstruction: String?) {
        val systemInstructionJson = if (systemInstruction != null) {
            """,
            "systemInstruction": {
              "parts": [
                { "text": "${systemInstruction.replace("\n", " ").replace("\"", "\\\"")}" }
              ]
            }
            """.trimIndent()
        } else ""

        val handshakeJson2 = """
        {
          "setup": {
            "model": "models/gemini-2.5-flash-native-audio-preview-12-2025",
            "generationConfig": {
              "responseModalities": ["AUDIO"],
              "speechConfig": {
                "voiceConfig": {
                  "prebuiltVoiceConfig": {
                    "voiceName": "Puck"
                  }
                }
              }
            }$systemInstructionJson
          }
        }
        """.trimIndent()

        send(Frame.Text(handshakeJson2))
        println("✅ Handshake sent using: gemini-2.5-flash-native-audio-preview-12-2025 (with Voice Config)")
    }

    suspend fun sendAudio(pcmData: ByteArray) {
        if (session?.isActive == true) {
            val json = """
            {"realtime_input": {"media_chunks": [{"mime_type": "audio/pcm", "data": "${pcmData.encodeBase64()}"}]}}
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
