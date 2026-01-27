package com.mursaline.kaironex.brain

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import io.ktor.util.encodeBase64
import io.ktor.util.decodeBase64Bytes
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonObject

import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GeminiReasoningEngine(
    private val client: HttpClient,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer
) {

    // ✅ USE v1beta for the Live API
    private val BASE_URL = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"

    // Dedicated thread for audio processing to avoid UI jank and buffer underruns
    private val audioDispatcher = kotlinx.coroutines.Dispatchers.Default

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms = _audioRms.asStateFlow()

    // Tool Call Stream for Agent Logic
    private val _toolCalls = MutableSharedFlow<FunctionCallPart>()
    val toolCalls = _toolCalls.asSharedFlow()
    
    // 🛡️ Track Active Tools to prevent responding to Cancelled/Interrupted tools
    private val activeToolIds = mutableSetOf<String>()
    private val activeToolIdsLock = Mutex()

    // JSON Parser for internal use
    private val jsonParser = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }

    private var session: DefaultClientWebSocketSession? = null

    private class QuotaExceededException(message: String) : Exception(message)

    suspend fun connect(apiKey: String, systemInstruction: String? = null, toolsConfig: String? = null) {
        // Enforcing Standardization: Using 2.5 Native Audio for all Live Interactions
        val modelName = com.mursaline.kaironex.core.gemini.GeminiModels.LIVE_SMART_VOICE

        try {
            attemptConnection(apiKey, systemInstruction, modelName, toolsConfig)
            println("✅ Session ended normally.")
        } catch (e: Exception) {
             println("❌ Connection Failed: ${e.message}")
             _connectionState.value = ConnectionState.Error(e.message ?: "Unknown Error")
        }
    }

    private var isInterruptedLocally = false
    private var isProcessingTool = false // 🔒 Guard: Stop sending audio when Tool is active

    private suspend fun attemptConnection(apiKey: String, systemInstruction: String?, modelName: String, toolsConfig: String?) {
        try {
            _connectionState.value = ConnectionState.Connecting
            println("🔌 Connecting to Live API ($modelName)...")
            // ... (rest of connection logic)
            // Need to pass toolsConfig to sendHandshake
            client.webSocket("$BASE_URL?key=$apiKey") {
                session = this
                _connectionState.value = ConnectionState.Connected
                println("⚡ SOCKET OPENED. Sending Handshake ($modelName)...")

                sendHandshake(systemInstruction, modelName, toolsConfig)
                
                // ... (rest of logic)

                // LAUNCH AUDIO RECORDER
                val audioJob = launch {
                    println("🎤 Starting Audio Recorder...")
                    try {
                        audioRecorder.startRecording { // onVolumeDetected (Binary)
                            // ⚡ INSTANT LOCAL INTERRUPTION
                            // ⚡ SLF-SABOTAGE FIX: Disabled Local Interruption. 
                            // We rely on Server VAD to detect interruptions to avoid Echo muting the bot.
                            /*
                            if (audioPlayer.isPlaying()) { 
                                audioPlayer.stop()
                                isInterruptedLocally = true
                                println("⚡ Local VAD: User Speaking -> Muted Bot")
                                
                                launch {
                                    delay(1200) // 1.2s silence window (Balancing ghost audio vs latency)
                                    if (isInterruptedLocally) isInterruptedLocally = false
                                }
                            }
                            */
                        }.collect { pcmData ->
                            // Calculate approximate RMS for UI feedback (Visuals only)
                             // Since strict RMS is done in recorder, we can just do a rough calc here or update recorder interface to pass float.
                             // For now, let's just make it pulse if we get data?
                             // No, better to have dynamic size.
                             // Let's calculate simple RMS here again for UI (cheap operation)
                             // RMS Calculation
                             var sum = 0.0
                             for (i in 0 until pcmData.size step 8) { // Sample every 8th
                                if (i + 1 < pcmData.size) {
                                   val sample = (pcmData[i].toInt() and 0xFF) or (pcmData[i + 1].toInt() shl 8)
                                   val norm = sample.toShort() / 32768.0f
                                   sum += norm * norm
                                }
                             }
                             val rms = kotlin.math.sqrt(sum / (pcmData.size / 8)).toFloat()
                             _audioRms.value = rms

                             _audioRms.value = rms
 
                             // ⚡ TRILLION DOLLAR STRATEGY: ALWAYS STREAM
                             // We trust the Gemini Server VAD (trained on billions of hours) to distinguish 
                             // between "Silence/Hiss" and "Speech".
                             // Client-side filtering adds latency and risk. 
                             // Streaming everything ensures the server hears the *exact* millisecond speech ends.
                             
                             // 🛡️ FULL DUPLEX RESTORED
                             // We rely on AEC (Acoustic Echo Cancellation) in AndroidAudioRecorder
                             // and the Server's ability to filter echo. 
                             // Muting the mic (Half-Duplex) caused the User's input to be lost if they spoke "too soon".
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
                
                try {
                    for (frame in incoming) {
                        val messageText = when (frame) {
                            is Frame.Text -> frame.readText()
                            is Frame.Binary -> String(frame.readBytes())
                            else -> null
                        }

                        if (messageText != null) {
                            // Debug: Log Incoming Message (Only if NOT audio)
                            if (!messageText.contains("audio/pcm")) {
                                println("📩 RX: ${messageText.take(200)}...")
                            } else {
                                // print(".") // Optional: progress indicator
                            }
                            
                            // 1. Check for TOOLS (Function Calls)
                            if (messageText.contains("\"functionCall\"")) {
                                try {
                                    // Quick dirty parse to find the tool name and args
                                    // Ideally use full JSON parser, but for speed regarding Part structure:
                                    // We'll trust the flow logic to observe tool calls if we parsed them fully.
                                    // For now, let's just log it or notify a listener.
                                    // Better: We should expose a Flow<FunctionCall> !
                                    
                                    // Let's implement a listener/callback for tools
                                    onToolCall(messageText)
                                } catch (e: Exception) {
                                    println("❌ Tool Parse Failed: ${e.message}")
                                }
                            }
                            if (messageText.contains("\"interrupted\": true")) {
                                println("🛑 Server Confirmed Interruption. Resetting.")
                                isInterruptedLocally = false
                                
                                launch {
                                    activeToolIdsLock.withLock {
                                        activeToolIds.clear()
                                        isProcessingTool = false // 🔄 Full Reset
                                    }
                                }
                                audioPlayer.stop()
                            }
                            
                            if (messageText.contains("\"toolCallCancellation\"")) {
                                launch {
                                    activeToolIdsLock.withLock {
                                        // Extract cancelled IDs
                                        try {
                                            val element = jsonParser.parseToJsonElement(messageText)
                                            val cancelledIds = element.jsonObject["toolCallCancellation"]?.jsonObject?.get("ids")?.jsonArray?.map { it.jsonPrimitive.content }
                                            cancelledIds?.forEach { id -> 
                                                activeToolIds.remove(id) 
                                                println("🗑️ Removed Cancelled Tool ID: $id")
                                            }
                                        } catch (e: Exception) {
                                            println("⚠️ Error parsing cancellation: ${e.message}")
                                        }
                                        
                                        // 🛡️ SAFETY DELAY: Wait for Server State to Settle
                                        delay(500)
                                        isProcessingTool = activeToolIds.isNotEmpty()
                                        if (!isProcessingTool) println("▶️ Audio Resumed (after cancellation delay)")
                                    }
                                }
                            }

                            if (messageText.contains("audio/pcm")) {
                                if (isInterruptedLocally) {
                                    // println("👻 Dropping Ghost Audio Packet (Interrupted)")
                                    continue // SKIP THIS PACKET
                                }
                                
                                try {
                                    val dataMarker = "\"data\": \""
                                    val startIndex = messageText.indexOf(dataMarker)
                                    if (startIndex != -1) {
                                        val startQuote = startIndex + dataMarker.length
                                        val endQuote = messageText.indexOf("\"", startQuote)
                                        if (endQuote != -1) {
                                            val base64 = messageText.substring(startQuote, endQuote)
                                            val pcm = base64.decodeBase64Bytes()
                                            // Offload blocking audio write to dedicated thread
                                            kotlinx.coroutines.withContext(audioDispatcher) {
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
                            val reason = frame.readReason()?.message ?: ""
                            println("📪 Server requested close: $reason")
                            if (reason.contains("quota", ignoreCase = true)) {
                                throw QuotaExceededException(reason)
                            }
                        }
                    }
                    println("📪 Incoming loop finished (Server closed connection?)")
                } catch (e: QuotaExceededException) {
                    throw e // Propagate up
                } catch (e: Exception) {
                    println("❌ Error in incoming loop: ${e.message}")
                } finally {
                    val reason = session?.closeReason?.await()?.message ?: ""
                    println("📪 Connection closed. Reason: $reason")
                    
                     if (reason.contains("quota", ignoreCase = true)) {
                        audioJob.cancel()
                        audioPlayer.stop()
                        throw QuotaExceededException(reason) // Throw for fallback
                    }

                    audioJob.cancel()
                    audioPlayer.stop()
                    println("🎤 Audio Recorder Stopped")
                }
            }
        } finally {
             audioRecorder.stopRecording()
        }
    }

    private suspend fun DefaultClientWebSocketSession.sendHandshake(systemInstruction: String?, modelName: String, toolsConfig: String?) {
        // Best Practice: Structured System Instructions
        val structuredSystemInstruction = systemInstruction ?: """
            **Persona:**
            You are Kaironex, an advanced AI tutor. You are helpful, concise, and focused on learning.
            
            **Conversational Rules:**
            1. **Listen Activey:** Wait for the user to finish fully before answering complex queries.
            2. **Be Concise:** Give short, punchy answers unless asked for detail.
            3. **No Robot Talk:** Speak naturally.
            
            **Guardrails:**
            - Do not hallucinate facts.
            - If unsure, say "I don't know".
        """.trimIndent()

        val systemInstructionJson = """
            "systemInstruction": {
              "parts": [
                { "text": "${structuredSystemInstruction.replace("\n", "\\n").replace("\"", "\\\"")}" }
              ]
            }
        """.trimIndent()

        // Best Practice: Define Tools
        // Use provided toolsConfig, or default to Grounding if null
        val toolsJson = toolsConfig ?: """
            "tools": [
                { "google_search_retrieval": {} } 
            ]
        """.trimIndent()

        val handshakeJson2 = """
        {
          "setup": {
            "model": "$modelName",
            "generationConfig": {
              "responseModalities": ["AUDIO"],
              "speechConfig": {
                "voiceConfig": {
                  "prebuiltVoiceConfig": {
                    "voiceName": "Puck"
                  }
                }
              }
            },
            $systemInstructionJson,
            $toolsJson
          }
        }
        """.trimIndent()

        send(Frame.Text(handshakeJson2))
        println("✅ Handshake sent using: $modelName")
        
        // ⚡ AUTO-START TRIGGER
        // We force the model to generate the first turn (Intro) by sending an empty "Start" signal.
        // FIXED: Re-enabling Kickstart now that input streaming is Paused on Tool Call.
        // ⚡ AUTO-START TRIGGER
        // We force the model to generate the first turn (Intro) by sending an empty "Start" signal.
        // FIXED: Re-enabling Kickstart now that input streaming is Paused on Tool Call.
        
        delay(500) // Small buffer
        val kickstartJson = """
        {
            "client_content": {
                "turns": [
                    {
                        "role": "user",
                        "parts": [ { "text": "Start Interview." } ]
                    }
                ],
                "turn_complete": true
            }
        }
        """.trimIndent()
        send(Frame.Text(kickstartJson))
        println("🚀 Kickstart Trigger Sent (Auto-Start)")
        // println("🚀 Kickstart Disabled for Stability. Waiting for User Voice...")
    }

    suspend fun sendAudio(pcmData: ByteArray) {
        if (session?.isActive == true) {
            // 🔒 Guard: Do not send audio if we are processing a tool (Wait for Response)
            if (isProcessingTool) return 

            val json = """
            {"realtimeInput": {"media_chunks": [{"mime_type": "audio/pcm", "data": "${pcmData.encodeBase64()}"}]}}
            """.trimIndent()
            session?.send(Frame.Text(json))
        }
    }
    
    private suspend fun DefaultClientWebSocketSession.onToolCall(jsonString: String) {
        try {
            val element = jsonParser.parseToJsonElement(jsonString)
            // Traverse safety: serverContent -> modelTurn -> parts -> functionCall
            val serverContent = element.jsonObject["serverContent"]?.jsonObject
            val modelTurn = serverContent?.get("modelTurn")?.jsonObject
            val parts = modelTurn?.get("parts")?.jsonArray
            
            if (parts != null) {
                for (part in parts) {
                    val partObj = part.jsonObject
                    // Check if functionCall exists
                    if ("functionCall" in partObj) {
                        val funcCall = partObj["functionCall"]?.jsonObject
                        if (funcCall != null) {
                            val name = funcCall["name"]?.jsonPrimitive?.content ?: ""
                            val id = funcCall["id"]?.jsonPrimitive?.content ?: "" // Extract ID
                            val argsObj = funcCall["args"]?.jsonObject
                            
                            // Convert JsonObject to Map<String, String> for simplicity
                            val argsMap = argsObj?.entries?.associate { (key, value) ->
                                key to (value.jsonPrimitive.contentOrNull ?: value.toString())
                            }
                            
                            
                            if (name.isNotEmpty()) {
                                println("🛠️ Tool Call Detected: $name (ID: $id) args: $argsMap")
                                this@onToolCall.launch {
                                    activeToolIdsLock.withLock {
                                        isProcessingTool = true // ⏸️ Pause Audio Streaming
                                        if (id.isNotEmpty()) activeToolIds.add(id) // 🛡️ Track ID
                                    }
                                }
                                println("⏸️ Pausing Audio Upload for Tool Execution...")
                                _toolCalls.emit(FunctionCallPart(name, argsMap, id))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ Error parsing tool call: ${e.message}")
        }
    }

    suspend fun sendToolResponse(toolName: String, response: Map<String, Any?>, toolId: String? = null) {
        // 🛡️ Prevent sending response for cancelled tool
        var isCancelled = false
        activeToolIdsLock.withLock {
            if (toolId != null && !activeToolIds.contains(toolId)) {
                println("🛑 IGNORING Tool Response for CANCELLED ID: $toolId")
                isCancelled = true
            } else {
                if (toolId != null) activeToolIds.remove(toolId)
                // Only resume audio if NO MORE tools are pending
                isProcessingTool = activeToolIds.isNotEmpty()
            }
        }
        if (isCancelled) return

        if (session?.isActive == true) {
            // 1. Build the Result JSON (Inner Content)
            val responseJson = kotlinx.serialization.json.JsonObject(
                response.mapValues { (_, v) -> 
                     when(v) {
                         is String -> kotlinx.serialization.json.JsonPrimitive(v)
                         is Number -> kotlinx.serialization.json.JsonPrimitive(v)
                         is Boolean -> kotlinx.serialization.json.JsonPrimitive(v)
                         else -> kotlinx.serialization.json.JsonPrimitive(v.toString())
                     }
                }
            )

            // 2. Build the Live API Wrapper (The Fix)
            // ❌ OLD (REST Format - CAUSES CRASH): "client_content": { "turns": ... }
            // ✅ NEW (Live Format): "tool_response": { "function_responses": ... }
            val json = """
            {
              "tool_response": {
                "function_responses": [
                  {
                    "id": "$toolId",
                    "name": "$toolName",
                    "response": {
                      "result": $responseJson
                    }
                  }
                ]
              }
            }
            """.trimIndent()
            
            session?.send(Frame.Text(json))
            println("📤 Tool Response Sent: $toolName (ID: $toolId)")
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

    @Serializable
    data class Part(
        val text: String? = null,
        val functionCall: FunctionCallPart? = null
    )

    @Serializable
    data class FunctionCallPart(
        val name: String,
        val args: Map<String, String>? = null,
        val id: String? = null // Captured from server
    )
}
