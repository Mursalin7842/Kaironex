package com.mursaline.kaironex.brain

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.koin.core.component.KoinComponent
import kotlin.coroutines.CoroutineContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import io.ktor.client.request.parameter

@OptIn(ExperimentalEncodingApi::class)
class GeminiReasoningEngine(
    private val client: HttpClient,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer
) : KoinComponent, CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.Default + job

    // --- State ---
    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Connecting : ConnectionState()
        data object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _toolCalls = MutableSharedFlow<ToolCall>(replay = 0)
    val toolCalls: SharedFlow<ToolCall> = _toolCalls

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms
    
    // Explicitly tracking Speaking State
    private val _isAgentSpeaking = MutableStateFlow(false)
    val isAgentSpeaking: StateFlow<Boolean> = _isAgentSpeaking

    // --- Data Classes for API ---
    @Serializable
    data class ToolCall(
        val id: String,
        val name: String,
        val args: Map<String, String>? = null
    )
    
    // For Internal Usage when splitting parts
    data class FunctionCallPart(
        val name: String,
        val args: Map<String, String>?,
        val id: String
    )

    // JSON Parser for internal use
    private val jsonParser = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }

    private var session: DefaultClientWebSocketSession? = null
    
    // Concurrency Guards
    private val activeToolIds = mutableSetOf<String>()
    private val activeToolIdsLock = Mutex()
    private val audioDispatcher = Dispatchers.Default // Or separate thread

    private class QuotaExceededException(message: String) : Exception(message)

    suspend fun connect(apiKey: String, systemInstruction: String? = null, toolsConfig: String? = null) {
        // Enforcing Standardization: Using 2.5 Native Audio for all Live Interactions
        val modelName = "gemini-2.0-flash-exp"

        try {
            attemptConnection(apiKey, systemInstruction, modelName, toolsConfig)
            println("✅ Session ended normally.")
        } catch (e: Exception) {
             if (e is kotlinx.coroutines.CancellationException) {
                 println("✅ Connection closed gracefully.")
             } else {
                 println("❌ Connection Failed: ${e.message}")
                 _connectionState.value = ConnectionState.Error(e.message ?: "Unknown Error")
             }
        }
    }

    private var isInterruptedLocally = false
    private var isProcessingTool = false // 🔒 Guard: Stop sending audio when Tool is active

    private suspend fun attemptConnection(apiKey: String, systemInstruction: String?, modelName: String, toolsConfig: String?) {
        try {
            _connectionState.value = ConnectionState.Connecting
            println("🔌 Connecting to Live API ($modelName)...")
            
            // Construct URL
            val host = "generativelanguage.googleapis.com"
            val path = "/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
            
            client.wss(host = host, path = path, request = {
                 parameter("key", apiKey)
            }) {
                session = this
                _connectionState.value = ConnectionState.Connected
                println("⚡ SOCKET OPENED. Sending Handshake ($modelName)...")

                sendHandshake(systemInstruction, modelName, toolsConfig)
                
                // LAUNCH AUDIO RECORDER
                val audioJob = launch {
                    println("🎤 Starting Audio Recorder...")
                    try {
                        audioRecorder.startRecording { // onVolumeDetected (Binary)
                           // No local interruption logic (Server VAD)
                        }.collect { pcmData ->
                             // RMS Calculation for UI
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
 
                             // Send Audio
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
                
                // RECEIVE LOOP
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
                            }
                            
                            try {
                                val root = jsonParser.parseToJsonElement(messageText).jsonObject
                                // FIX: snake_case for raw API
                                val serverContent = (root["serverContent"] ?: root["server_content"])?.jsonObject

                                // A. HANDLE INTERRUPTION
                                if (serverContent?.get("interrupted")?.jsonPrimitive?.booleanOrNull == true) {
                                    println("🛑 Server Confirmed Interruption. Resetting.")
                                    isInterruptedLocally = false
                                    activeToolIdsLock.withLock {
                                        activeToolIds.clear()
                                        isProcessingTool = false
                                    }
                                    audioPlayer.stop()
                                }

                                // B. HANDLE TOOL CANCELLATION
                                val toolCancellation = (root["toolCallCancellation"] ?: root["tool_call_cancellation"])?.jsonObject
                                if (toolCancellation != null) {
                                    val idsArray = toolCancellation["ids"]?.jsonArray
                                    activeToolIdsLock.withLock {
                                        idsArray?.forEach { 
                                            val id = it.jsonPrimitive.content
                                            println("🚫 Server CANCELLED Tool ID: $id")
                                            activeToolIds.remove(id) 
                                        }
                                        isProcessingTool = activeToolIds.isNotEmpty()
                                    }
                                }

                                // C. PROCESSING CONTENT (Audio, Text, Tools)
                                if (serverContent != null) {
                                    val modelTurn = (serverContent["modelTurn"] ?: serverContent["model_turn"])?.jsonObject
                                    val parts = modelTurn?.get("parts")?.jsonArray

                                    parts?.forEach { part ->
                                        val pObj = part.jsonObject
                                        
                                        // 1. Audio (inlineData)
                                        val inlineData = (pObj["inlineData"] ?: pObj["inline_data"])?.jsonObject
                                        if (inlineData != null) {
                                            val mimeType = (inlineData["mimeType"] ?: inlineData["mime_type"])?.jsonPrimitive?.content ?: ""
                                            if (mimeType.startsWith("audio")) {
                                                val dataBase64 = inlineData["data"]?.jsonPrimitive?.content
                                                if (dataBase64 != null && !isInterruptedLocally) {
                                                     val pcm = Base64.decode(dataBase64)
                                                     withContext(audioDispatcher) {
                                                         _isAgentSpeaking.value = true
                                                         audioPlayer.play(pcm)
                                                     }
                                                }
                                            }
                                        }

                                        // 2. Function Calls (Tool Calls)
                                        if ("functionCall" in pObj || "function_call" in pObj) {
                                            onToolCall(pObj.toString()) 
                                        }
                                        
                                        // 3. Text Content (Logging)
                                        val text = pObj["text"]?.jsonPrimitive?.content
                                        if (text != null) {
                                             println("🗣️ MODEL TEXT: $text")
                                        }
                                    }
                                }
                                
                                // D. SECONDARY TOOL SEARCH (Legacy / Root level)
                                if (messageText.contains("\"toolCall\"") || messageText.contains("\"tool_call\"")) {
                                     val toolCall = (root["toolCall"] ?: root["tool_call"])?.jsonObject
                                     if (toolCall != null) {
                                         val functionCalls = (toolCall["functionCalls"] ?: toolCall["function_calls"])?.jsonArray
                                         functionCalls?.forEach { fc ->
                                             onToolCall(fc.toString())
                                         }
                                     }
                                }

                                // E. HANDLE TURN COMPLETE Users
                                val tc = serverContent?.get("turnComplete") ?: serverContent?.get("turn_complete")
                                if (tc?.jsonPrimitive?.booleanOrNull == true) {
                                     withContext(audioDispatcher) {
                                         // audioPlayer.endStream() // Optional depending on player impl
                                         _isAgentSpeaking.value = false
                                     }
                                }
                            
                            } catch (e: Exception) {
                                println("❌ Error Parsing Message: ${e.message}")
                                e.printStackTrace()
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
                } catch (e: QuotaExceededException) {
                    throw e // Propagate up
                } catch (e: Exception) {
                    println("❌ Error in incoming loop: ${e.message}")
                } finally {
                    val reason = session?.closeReason?.await()?.message ?: ""
                    println("📪 Connection closed. Reason: $reason")
                    audioJob.cancel()
                    audioPlayer.stop()
                    _isAgentSpeaking.value = false
                }
            }
        } finally {
             audioRecorder.stopRecording()
        }
    }


    suspend fun sendContextUpdate(message: String) {
        if (session?.isActive != true) return
        
        val json = """
        {
          "client_content": {
            "turns": [
              {
                "role": "user",
                "parts": [ { "text": "$message" } ]
              }
            ],
            "turn_complete": false 
          }
        }
        """.trimIndent()
        
        try {
            session?.send(Frame.Text(json))
            println("🧠 Context Injected: $message")
        } catch (e: Exception) {
            println("❌ Context Injection Failed: ${e.message}")
        }
    }

    suspend fun disconnect() {
        session?.close()
        _connectionState.value = ConnectionState.Disconnected
        _isAgentSpeaking.value = false
    }
    
    // --- PRIVATE HELPERS ---

    private suspend fun DefaultClientWebSocketSession.sendHandshake(systemInstruction: String?, modelName: String, toolsConfig: String?) {
        // ... (Keep existing prompt logic) ...
        // Simplification for brevity in this fix tool call, keeping core logic
        val systemInstructionText = systemInstruction ?: "You are Kairo."
        val systemInstructionJson = """
            "system_instruction": { "parts": [ { "text": "${systemInstructionText.replace("\n", "\\n").replace("\"", "\\\"")}" } ] }
        """.trimIndent()

        // Default tool config if null
        val toolsJson = toolsConfig ?: """ "tools": [{ "google_search_retrieval": {} }] """

        // Reconstruct JSON structure
        val handshakeJson = """
        {
          "setup": {
            "model": "$modelName",
            "generation_config": {
              "response_modalities": ["AUDIO"],
              "speech_config": {
                "voice_config": { "prebuilt_voice_config": { "voice_name": "Puck" } }
              }
            },
            $systemInstructionJson,
            $toolsJson
          }
        }
        """.trimIndent()

        send(Frame.Text(handshakeJson))
        println("✅ Handshake sent using: $modelName")
        
        // Kickstart
        delay(500)
        send(Frame.Text("""{"client_content":{"turns":[{"role":"user","parts":[{"text":". Start Interview."}]}],"turn_complete":true}}"""))
    }

    suspend fun sendAudio(pcmData: ByteArray) {
        if (session?.isActive == true) {
            val isBusy = activeToolIdsLock.withLock { isProcessingTool }
            if (isBusy) return 

            val json = """
            {"realtime_input": {"media_chunks": [{"mime_type": "audio/pcm;rate=16000", "data": "${Base64.encode(pcmData)}"}]}}
            """.trimIndent()
            session?.send(Frame.Text(json))
        }
    }
    
    private suspend fun onToolCall(jsonString: String) {
        // Parse Tool Call
        try {
             val jsonObj = jsonParser.parseToJsonElement(jsonString).jsonObject
             
             // Check generic "functionCall" (Google format) or "function_call"
             val fc = jsonObj["functionCall"]?.jsonObject ?: jsonObj["function_call"]?.jsonObject
             
             if (fc != null) {
                 handleFunc(fc)
                 return
             }
             
             // Check "functionCalls" array
             val fcs = jsonObj["functionCalls"]?.jsonArray ?: jsonObj["function_calls"]?.jsonArray
             fcs?.forEach { 
                 if (it is JsonObject) handleFunc(it) 
             }
             
        } catch (e: Exception) {
            println("❌ Error parsing tool call: ${e.message}")
        }
    }

    private suspend fun handleFunc(funcCall: kotlinx.serialization.json.JsonObject) {
        val name = funcCall["name"]?.jsonPrimitive?.content ?: ""
        val id = funcCall["id"]?.jsonPrimitive?.content ?: ""
        val argsObj = funcCall["args"]?.jsonObject

        val argsMap = argsObj?.entries?.associate { (key, value) ->
            key to (value.jsonPrimitive.contentOrNull ?: value.toString())
        }

        if (name.isNotEmpty()) {
            println("🛠️ Tool Call Detected: $name (ID: $id) args: $argsMap")
            
            audioPlayer.stop()
            _isAgentSpeaking.value = false

            activeToolIdsLock.withLock {
                isProcessingTool = true
                if (id.isNotEmpty()) activeToolIds.add(id)
            }
            println("⏸️ Pausing Audio Upload for Tool Execution...")
            _toolCalls.emit(ToolCall(id, name, argsMap))
        }
    }

    suspend fun sendToolResponse(toolName: String, response: Map<String, Any?>, toolId: String? = null) {
        // Validation logic
        var isCancelled = false
        activeToolIdsLock.withLock {
            if (toolId != null && !activeToolIds.contains(toolId)) {
                isCancelled = true // Server cancelled it previously
            } else {
                if (toolId != null) activeToolIds.remove(toolId)
                isProcessingTool = activeToolIds.isNotEmpty()
            }
        }
        if (isCancelled) return

        if (session?.isActive == true) {
            // Build response
             val resultEntries = response.entries.joinToString(",") { (k, v) ->
                val valueStr = when(v) {
                    is String -> "\"$v\""
                    is Number, is Boolean -> "$v"
                    else -> "\"$v\""
                }
                "\"$k\": $valueStr"
            }
            val resultJson = "{ $resultEntries }"
            
            val msg = """
            {
              "tool_response": {
                "function_responses": [
                  {
                    "id": "$toolId",
                    "name": "$toolName",
                    "response": { "result": $resultJson }
                  }
                ]
              }
            }
            """.trimIndent()

            try {
                session?.send(Frame.Text(msg))
                println("📤 Tool Response Sent: $toolName")
                
                // Silent Kickstart to resume flow
                delay(50) 
                 val kickstart = """
                {
                  "client_content": {
                    "turns": [
                      {
                        "role": "user",
                        "parts": [ { "text": "[SYSTEM_SYNC]" } ]
                      }
                    ],
                    "turn_complete": true
                  }
                }
                """.trimIndent()
                session?.send(Frame.Text(kickstart))

            } catch (e: Exception) {
                println("❌ Failed to send tool response: ${e.message}")
            }
        }
    }
}
