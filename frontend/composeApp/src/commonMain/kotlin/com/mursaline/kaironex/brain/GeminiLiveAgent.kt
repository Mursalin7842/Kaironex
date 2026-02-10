package com.mursaline.kaironex.brain

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import com.mursaline.kaironex.core.AppConfig // Ensure this exists with your API Key
import com.mursaline.kaironex.brain.AgentType
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

    /**
     * Build a context-rich system instruction from available user data.
     * This gives the voice agent real knowledge about the student.
     */
    private fun buildSystemInstruction(userId: String): String {
        // Try to load student context from the stats repository
        val sessionManager = try {
            org.koin.core.context.GlobalContext.get().get<com.mursaline.kaironex.core.KaironexSessionManager>()
        } catch (_: Exception) { null }

        val repo = sessionManager?.getStatsRepository()
        val userName = repo?.getUserName() ?: com.mursaline.kaironex.core.CurrentUser.displayName

        return buildString {
            appendLine("You are Kaironex, an AI academic companion.")
            appendLine("You are speaking with ${userName.ifBlank { "a student" }} (userId: $userId).")
            appendLine()
            appendLine("YOUR CAPABILITIES:")
            appendLine("- You have access to the student's full academic profile, schedule, and study history via the 'consult_brain' tool.")
            appendLine("- 5 specialized backend agents: Study Agent (schedules, resources), Vitality Agent (wellness, breaks), Campaign Agent (goals, planning), Radius Agent (social, networking), Supervisor Agent (orchestration).")
            appendLine("- You can create study schedules, check deadlines, suggest breaks, and provide real-time academic guidance.")
            appendLine()
            appendLine("VOICE GUIDELINES:")
            appendLine("- Keep responses concise and conversational (2-3 sentences max).")
            appendLine("- Speak immediately when you have an answer.")
            appendLine("- For complex requests (scheduling, planning), use the consult_brain tool to delegate to the appropriate backend agent.")
            appendLine("- Be warm, encouraging, and focused on the student's academic success.")
            appendLine("- If asked about schedule or deadlines, always use consult_brain with target_agent='study'.")
            appendLine("- If asked about wellness or breaks, use target_agent='vitality'.")
            appendLine("- If asked about goals or long-term planning, use target_agent='campaign'.")
        }
    }

    suspend fun startSession(userId: String) {
        val apiKey = com.mursaline.kaironex.PlatformSecrets.apiKey
        // [FIX] Switch to v1beta to match old working implementation
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"

        val systemPrompt = buildSystemInstruction(userId)

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
                                    put("text", systemPrompt)
                                })
                            })
                        })
                    })
                }
                send(Frame.Text(setupMsg.toString()))
                println("⚡ Handshake sent with enriched system instruction")

                // [FIX] Kickstart: Greet the student by name
                delay(500)
                val greeting = com.mursaline.kaironex.core.CurrentUser.displayName.let { name ->
                    if (name.isNotBlank()) "Greet $name briefly and ask how you can help with their studies today."
                    else "Greet the student briefly and ask how you can help with their studies today."
                }
                val kickstartMsg = buildJsonObject {
                    put("clientContent", buildJsonObject {
                        put("turns", buildJsonArray {
                            add(buildJsonObject {
                                put("role", "user")
                                put("parts", buildJsonArray {
                                    add(buildJsonObject {
                                        put("text", greeting)
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

    /**
     * Start an AGENT-INITIATED voice session.
     * The agent speaks FIRST, explaining WHY it's calling the user.
     * This is the KEY differentiator of Kaironex: agents act autonomously.
     */
    suspend fun startAgentInitiatedSession(
        userId: String,
        agentType: AgentType,
        callReason: String,
        callContext: Map<String, String> = emptyMap()
    ) {
        val apiKey = com.mursaline.kaironex.PlatformSecrets.apiKey
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"

        val userName = com.mursaline.kaironex.core.CurrentUser.displayName.ifBlank { "student" }

        // Build agent-specific system instruction
        val systemPrompt = buildString {
            appendLine("You are ${agentType.label}, one of the autonomous AI agents in Kaironex.")
            appendLine("You are CALLING the student ${userName} on your own initiative.")
            appendLine("You initiated this call because: $callReason")
            appendLine()
            appendLine("CRITICAL BEHAVIOR:")
            appendLine("- You speak FIRST. Introduce yourself briefly and explain why you're calling.")
            appendLine("- Be warm, direct, and helpful. Get to the point in 2-3 sentences.")
            appendLine("- You have real authority — you can schedule study sessions, suggest breaks, update goals.")
            appendLine("- Use the consult_brain tool to take real actions when the student agrees.")
            appendLine()
            when (agentType) {
                AgentType.STUDY -> {
                    appendLine("You are the Study Agent. You manage schedules, track study hours, and optimize learning.")
                    appendLine("You might call about: upcoming deadlines, study session reminders, schedule conflicts.")
                }
                AgentType.VITALITY -> {
                    appendLine("You are the Vitality Agent. You monitor wellness, budget, nutrition, and break schedules.")
                    appendLine("You might call about: break reminders, budget alerts, meal planning, wellness check-ins.")
                }
                AgentType.CAMPAIGN -> {
                    appendLine("You are the Campaign Agent. You manage long-term goals, GPA targets, and career planning.")
                    appendLine("You might call about: goal progress, milestone achievements, strategy adjustments.")
                }
                AgentType.RADIUS -> {
                    appendLine("You are the Radius Agent. You handle cultural navigation, housing, and local survival.")
                    appendLine("You might call about: visa deadlines, local resources, cultural tips, networking opportunities.")
                }
                AgentType.SUPERVISOR -> {
                    appendLine("You are the Supervisor Agent. You oversee all other agents and coordinate complex operations.")
                    appendLine("You might call about: inter-agent coordination, critical alerts, system updates.")
                }
            }
            if (callContext.isNotEmpty()) {
                appendLine()
                appendLine("ADDITIONAL CONTEXT:")
                callContext.forEach { (key, value) ->
                    appendLine("- $key: $value")
                }
            }
        }

        try {
            client.wss(url) {
                session = this
                println("📞 Agent Call: ${agentType.label} connecting to Gemini Live...")

                // 1. Setup handshake
                val setupMsg = buildJsonObject {
                    put("setup", buildJsonObject {
                        put("model", "models/$modelName")
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
                                    put("text", systemPrompt)
                                })
                            })
                        })
                    })
                }
                send(Frame.Text(setupMsg.toString()))
                println("📞 Agent Call: Handshake sent")

                // 2. Agent speaks first — tell it to introduce itself
                delay(500)
                val kickstart = "You are now calling $userName. Introduce yourself (by your agent name), explain why you're calling (reason: $callReason), and ask if they have a moment to discuss. Be natural and conversational."
                val kickstartMsg = buildJsonObject {
                    put("clientContent", buildJsonObject {
                        put("turns", buildJsonArray {
                            add(buildJsonObject {
                                put("role", "user")
                                put("parts", buildJsonArray {
                                    add(buildJsonObject {
                                        put("text", kickstart)
                                    })
                                })
                            })
                        })
                        put("turnComplete", true)
                    })
                }
                send(Frame.Text(kickstartMsg.toString()))
                println("📞 Agent Call: Kickstart sent — agent will speak first")

                // 3. Listen for events
                incoming.consumeAsFlow().collect { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        handleServerMessage(text, userId)
                    }
                }
            }
        } catch (e: Exception) {
            println("📞 Agent Call Failed: ${e.message}")
        }
    }
    
    fun disconnect() {
        session = null
    }
}
