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
    private val modelName = "gemini-2.5-flash-native-audio-preview-12-2025" // Removed models/ prefix
    
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
        val apiKey = com.mursaline.kaironex.PlatformSecrets.apiKey
        // [FIX] Switch to v1beta to match old working implementation
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"

        try {
            client.wss(url) {
                session = this
                println("⚡ Kaironex: Connected to Gemini 2.5 Live (v1beta)")

                // 1. Handshake with Config (camelCase for v1beta)
                val setupMsg = buildJsonObject {
                    put("setup", buildJsonObject {
                        put("model", "models/$modelName") // [FIX] Add 'models/' prefix
                        put("tools", buildJsonArray { add(toolsDefinition) })
                        put("generationConfig", buildJsonObject {
                            put("responseModalities", buildJsonArray { add("AUDIO") })
                            put("speechConfig", buildJsonObject {
                                put("voiceConfig", buildJsonObject {
                                    put("prebuiltVoiceConfig", buildJsonObject {
                                        put("voiceName", "Kore") 
                                    })
                                })
                            })
                        })
                        put("systemInstruction", buildJsonObject {
                            put("parts", buildJsonArray {
                                add(buildJsonObject {
                                    put("text", "You are Kaironex. Keep responses concise and conversational. Speak immediately when you have an answer.")
                                })
                            })
                        })
                    })
                }
                send(Frame.Text(setupMsg.toString()))
                println("⚡ Handshake sent")

                // [FIX] Kickstart: Force the model to start conversation
                delay(500)
                val kickstartMsg = buildJsonObject {
                    put("clientContent", buildJsonObject {
                        put("turns", buildJsonArray {
                            add(buildJsonObject {
                                put("role", "user")
                                put("parts", buildJsonArray {
                                    add(buildJsonObject {
                                        put("text", "Start Interview.")
                                    })
                                })
                            })
                        })
                        put("turnComplete", true)
                    })
                }
                send(Frame.Text(kickstartMsg.toString()))
                println("🚀 Kickstart Trigger Sent")

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
        
        // Check for "serverContent" (The Answer)
        val serverContent = json["serverContent"]?.jsonObject
        if (serverContent != null) {
            val modelTurn = serverContent["modelTurn"]?.jsonObject
            val parts = modelTurn?.get("parts")?.jsonArray
            
            parts?.forEach { part ->
                // [Check 1] Is there audio?
                val inlineData = part.jsonObject["inlineData"]
                if (inlineData != null) {
                    val data = inlineData.jsonObject["data"]?.jsonPrimitive?.content
                    if (data != null) {
                        println("⚡ Kaironex: Received Audio Chunk (${data.length} chars)") // [DEBUG LOG]
                        audioPlayer.playBase64(data)
                    }
                }
                
                // [Check 2] Is there text? (Sometimes it sends text before audio)
                val text = part.jsonObject["text"]?.jsonPrimitive?.content
                if (text != null) {
                    println("⚡ Kaironex: Agent thought: $text")
                }
            }
            
            // [Check 3] Is the turn complete?
            val turnComplete = serverContent["turnComplete"]?.jsonPrimitive?.booleanOrNull
            if (turnComplete == true) {
                println("⚡ Kaironex: Agent finished speaking turn.")
            }
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
        if (session == null) {
            // println("⚠️ Cannot send audio - Session is NULL")
            return
        }
        val count = pcmData.size
        // println("📤 Sending $count bytes to Gemini...") 

        val base64Audio = pcmData.encodeBase64()
        val msg = buildJsonObject {
            put("realtimeInput", buildJsonObject {  // [FIX] camelCase for v1beta
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
