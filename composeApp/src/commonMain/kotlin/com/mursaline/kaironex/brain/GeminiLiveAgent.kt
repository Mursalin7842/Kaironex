package com.mursaline.kaironex.brain

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import com.mursaline.kaironex.core.AppConfig // Ensure this exists with your API Key
import io.ktor.util.encodeBase64
import kotlinx.serialization.encodeToString

class GeminiLiveAgent(
    private val client: HttpClient,
    private val appwriteBridge: AppwriteBridge,
    private val audioPlayer: AudioPlayer // Interface defined in Phase 4
) {
    private var session: DefaultClientWebSocketSession? = null
    private val modelName = "models/gemini-2.5-flash-native-audio-preview-12-2025" // STRICTLY THIS MODEL
    
    // Tools: Let Gemini know it can talk to your Python backend
    private val toolsDefinition = buildJsonObject {
        put("function_declarations", buildJsonArray {
            add(buildJsonObject {
                put("name", "consult_brain")
                put("description", "Use this for complex queries, scheduling, or checking student status.")
                put("parameters", buildJsonObject {
                    put("type", "OBJECT")
                    put("properties", buildJsonObject {
                        put("target_agent", buildJsonObject { put("type", "STRING") })
                        put("user_intent", buildJsonObject { put("type", "STRING") })
                    })
                    put("required", buildJsonArray { add("target_agent"); add("user_intent") })
                })
            })
        })
    }

    suspend fun startSession(userId: String) {
        val apiKey = com.mursaline.kaironex.PlatformSecrets.apiKey // Ensure your API Key is here
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"

        try {
            client.wss(url) {
                session = this
                println("⚡ Kaironex: Connected to Gemini 2.5 Live")

                // 1. Handshake with Config
                val setupMsg = buildJsonObject {
                    put("setup", buildJsonObject {
                        put("model", modelName)
                        put("tools", buildJsonArray { add(toolsDefinition) })
                        put("generation_config", buildJsonObject {
                            put("response_modalities", buildJsonArray { add("AUDIO") })
                            put("speech_config", buildJsonObject {
                                put("voice_config", buildJsonObject {
                                    put("prebuilt_voice_config", buildJsonObject {
                                        put("voice_name", "Kore") // Options: Aoede, Charon, Fenrir, Kore, Puck
                                    })
                                })
                            })
                        })
                    })
                }
                send(Frame.Text(setupMsg.toString()))

                // 2. Listen for Events
                incoming.consumeAsFlow().collect { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        println("📥 Gemini Msg: $text")
                        handleServerMessage(text, userId)
                    }
                }
            }
        } catch (e: Exception) {
            println("⚡ Connection Failed: ${e.message}")
        }
    }

    private suspend fun handleServerMessage(jsonString: String, userId: String) {
        val json = Json.parseToJsonElement(jsonString).jsonObject
        
        // A. Handle Audio (Voice Response)
        val audioData = json["serverContent"]?.jsonObject
            ?.get("modelTurn")?.jsonObject
            ?.get("parts")?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("inlineData")?.jsonObject
            ?.get("data")?.jsonPrimitive?.content

        if (audioData != null) {
            println("🔊 Received audio chunk from Gemini")
            // Decode Base64 and Play
            // Note: You need a Base64 decoder. Ktor has one or use a utility.
            // For simplicity in KMP, pass the base64 string to the platform player
            audioPlayer.playBase64(audioData) 
        }

        // B. Handle Tool Calls (Routing to Appwrite)
        val toolCall = json["toolCall"]?.jsonObject
        val calls = toolCall?.get("functionCalls")?.jsonArray

        calls?.forEach { call ->
            val name = call.jsonObject["name"]?.jsonPrimitive?.content
            val id = call.jsonObject["id"]?.jsonPrimitive?.content ?: ""
            val args = call.jsonObject["args"]?.jsonObject

            if (name == "consult_brain") {
                val agent = args?.get("target_agent")?.jsonPrimitive?.content ?: "supervisor"
                val intent = args?.get("user_intent")?.jsonPrimitive?.content ?: ""

                // Call Appwrite Function
                val result = appwriteBridge.consultBrain(userId, agent, intent)

                // Send Result Back to Gemini
                val responseMsg = buildJsonObject {
                    put("tool_response", buildJsonObject {
                        put("function_responses", buildJsonArray {
                            add(buildJsonObject {
                                put("id", id)
                                put("name", "consult_brain")
                                put("response", buildJsonObject {
                                    put("result", buildJsonObject { put("object_value", result) })
                                })
                            })
                        })
                    })
                }
                session?.send(Frame.Text(responseMsg.toString()))
            }
        }
    }

    suspend fun sendUserAudio(pcmData: ByteArray) {
        val base64Audio = pcmData.encodeBase64()
        val msg = buildJsonObject {
            put("realtime_input", buildJsonObject {
                put("media_chunks", buildJsonArray {
                    add(buildJsonObject {
                        put("mime_type", "audio/pcm;rate=16000")
                        put("data", base64Audio)
                    })
                })
            })
        }
        session?.send(Frame.Text(msg.toString()))
    }
    
    fun disconnect() {
        session = null
    }
}
