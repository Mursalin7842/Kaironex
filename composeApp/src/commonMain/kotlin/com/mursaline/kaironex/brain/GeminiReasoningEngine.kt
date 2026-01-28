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

@OptIn(ExperimentalEncodingApi::class)
class GeminiReasoningEngine(
    private val client: HttpClient,
    private val audioRecorder: AudioRecorder,
    private val audioPlayer: AudioPlayer
) : KoinComponent, CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.Default + job

    // --- Data Classes for API ---
    
    @Serializable
    data class ToolCall(
        val id: String,
        val name: String,
        val args: Map<String, String>? = null // Simplified for this use case
    )

<<<<<<< HEAD
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
                            // 🔎 ROBUST JSON PARSING (Fixes truncated audio / compact JSON issues)
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
                                    val ids = toolCancellation["ids"]?.jsonArray
                                    activeToolIdsLock.withLock {
                                        ids?.forEach { 
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
                                                     val pcm = dataBase64.decodeBase64Bytes()
                                                     kotlinx.coroutines.withContext(audioDispatcher) {
                                                         audioPlayer.play(pcm)
                                                     }
                                                }
                                            }
                                        }

                                        // 2. Function Calls
                                        if ("functionCall" in pObj || "function_call" in pObj) {
                                            onToolCall(pObj.toString()) // Reuse existing parser or pass object
                                        }
                                    }
                                }
                                
                                // D. SECONDARY TOOL SEARCH (Legacy / Root level)
                                if (messageText.contains("\"functionCall\"") || messageText.contains("\"functionCalls\"") || 
                                    messageText.contains("\"function_call\"") || messageText.contains("\"function_calls\"")) {
                                    // Keep this as backup or for non-standard structures
                                    onToolCall(messageText)
                                }

                                // E. HANDLE TURN COMPLETE (After Audio)
                                val tc = serverContent?.get("turnComplete") ?: serverContent?.get("turn_complete")
                                if (tc?.jsonPrimitive?.booleanOrNull == true) {
                                     kotlinx.coroutines.withContext(audioDispatcher) {
                                         audioPlayer.endStream()
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


    suspend fun sendContextUpdate(message: String) {
        if (session?.isActive != true) return
        
        // We wrap this as a "User" message internally so the model processes it
        // as immediate context.
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
    }
    
    // --- PRIVATE HELPERS MOVED BACK INSIDE CLASS ---

    private suspend fun DefaultClientWebSocketSession.sendHandshake(systemInstruction: String?, modelName: String, toolsConfig: String?) {
        // 1. Define the Silent Sync Protocol (Safety Net)
        val syncProtocol = """
            
            **CRITICAL PROTOCOL:**
            - **ASK ONE QUESTION AT A TIME.**
            - When the user answers, validate the answer.
            - **IMMEDIATELY** call the tool `update_profile` with the value.
            - **WAIT** for the tool to execute.
            - **THEN** (after tool result) confirm briefly (e.g., "Got it") and **IMMEDIATELY** ask the next question.
            - Do NOT read out "[SYSTEM_SYNC]".
        """.trimIndent()

        // 2. The "Perfect" Interviewer Persona & Script
        val baseInstruction = systemInstruction ?: """
            You are Kairo, a professional, warm, and efficient AI interviewer for 'Kaironex'.
            Your goal is to calibrate a user's profile by asking specific questions one by one.
            
            **INTERVIEW SCRIPT (Strict Order):**
            1. University Name?
            2. Major/Field of study?
            3. Current Semester (e.g., Fall 2024, 5th)?
            4. Current CGPA?
            5. Are you an International Student? (Yes/No)
            6. Do you currently have a job? (Yes/No)
            7. Do you want help finding a job? (Yes/No)
            8. Describe your ideal job?
            9. Commute duration?
            10. High or Low Energy environments?
            11. Daily focus capacity (hours)?
            12. Non-negotiables?
            13. Learning style?
            14. Stress response?
            15. Common cause of failure?
            
            **RULES:**
            - If user's answer is unclear, ask for clarification.
            - Keep the tone professional but friendly.
            - Focus on SPEED and EFFICIENCY.
        """.trimIndent()
        
        val finalInstruction = baseInstruction + syncProtocol

        val systemInstructionJson = """
            "system_instruction": {
              "parts": [
                { "text": "${finalInstruction.replace("\n", "\\n").replace("\"", "\\\"")}" }
              ]
            }
        """.trimIndent()

        val toolsJson = toolsConfig ?: """
            "tools": [
                { "google_search_retrieval": {} } 
            ]
        """.trimIndent()

        val handshakeJson2 = """
        {
          "setup": {
            "model": "$modelName",
            "generation_config": {
              "response_modalities": ["AUDIO"],
              "speech_config": {
                "voice_config": {
                  "prebuilt_voice_config": {
                    "voice_name": "Puck"
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
        
        delay(500) // Small buffer
        val kickstart = """
        {
            "client_content": {
                "turns": [
                    {
                        "role": "user",
                        "parts": [ { "text": ". Start Interview." } ]
                    }
                ],
                "turn_complete": true
            }
        }
        """.trimIndent()
        send(Frame.Text(kickstart))
        println("🚀 Kickstart Trigger Sent (Auto-Start)")
    }

    suspend fun sendAudio(pcmData: ByteArray) {
        if (session?.isActive == true) {
            val isBusy = activeToolIdsLock.withLock { isProcessingTool }
            if (isBusy) return 

            // FIX: Use snake_case keys (realtime_input, media_chunks, mime_type) and include rate
            val json = """
            {"realtime_input": {"media_chunks": [{"mime_type": "audio/pcm;rate=16000", "data": "${pcmData.encodeBase64()}"}]}}
            """.trimIndent()
            session?.send(Frame.Text(json))
        }
    }
    
    private suspend fun DefaultClientWebSocketSession.onToolCall(jsonString: String) {
        try {
            val element = jsonParser.parseToJsonElement(jsonString)
            suspend fun findFunctionCalls(el: kotlinx.serialization.json.JsonElement) {
                when (el) {
                    is kotlinx.serialization.json.JsonObject -> {
                        // FIX: Check for snake_case keys
                        if ("functionCall" in el || "function_call" in el) {
                            val fc = (el["functionCall"] ?: el["function_call"])?.jsonObject
                            handleFunc(fc)
                        } else if ("functionCalls" in el || "function_calls" in el) {
                            val calls = (el["functionCalls"] ?: el["function_calls"])?.jsonArray
                            if (calls != null) {
                                for (call in calls) handleFunc(call.jsonObject)
                            }
                        } else {
                            for (v in el.values) findFunctionCalls(v)
                        }
                    }
                    is kotlinx.serialization.json.JsonArray -> {
                        for (item in el) findFunctionCalls(item)
                    }
                    else -> {}
                }
            }
            findFunctionCalls(element)
            
            // 🔎 DEBUG: Log Text Responses if Audio Failed
            val serverContent = try {
                 val root = jsonParser.parseToJsonElement(jsonString).jsonObject
                 (root["serverContent"] ?: root["server_content"])?.jsonObject
            } catch(e: Exception) { null }
            
            if (serverContent != null) {
                val modelTurn = (serverContent["modelTurn"] ?: serverContent["model_turn"])?.jsonObject
                val parts = modelTurn?.get("parts")?.jsonArray
                parts?.forEach { part ->
                    val text = part.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                    if (text != null) {
                        println("🗣️ MODEL TEXT: $text") // This will show up even if audio fails
                    }
                }
            }
            
        } catch (e: Exception) {
            println("❌ Error parsing message: ${e.message}")
        }
    }

    private suspend fun DefaultClientWebSocketSession.handleFunc(funcCall: kotlinx.serialization.json.JsonObject?) {
        if (funcCall == null) return
        val name = funcCall["name"]?.jsonPrimitive?.content ?: ""
        val id = funcCall["id"]?.jsonPrimitive?.content ?: ""
        val argsObj = funcCall["args"]?.jsonObject

        val argsMap = argsObj?.entries?.associate { (key, value) ->
            key to (value.jsonPrimitive.contentOrNull ?: value.toString())
        }

        if (name.isNotEmpty()) {
            println("🛠️ Tool Call Detected: $name (ID: $id) args: $argsMap")
            
            audioPlayer.stop()

            activeToolIdsLock.withLock {
                isProcessingTool = true
                if (id.isNotEmpty()) activeToolIds.add(id)
            }
            println("⏸️ Pausing Audio Upload for Tool Execution...")
            launch { _toolCalls.emit(FunctionCallPart(name, argsMap, id)) }
        }
    }

    suspend fun sendToolResponse(toolName: String, response: Map<String, Any?>, toolId: String? = null) {
        var isCancelled = false
        activeToolIdsLock.withLock {
            if (toolId != null && !activeToolIds.contains(toolId)) {
                isCancelled = true
            } else {
                if (toolId != null) activeToolIds.remove(toolId)
                isProcessingTool = activeToolIds.isNotEmpty()
            }
        }
        if (isCancelled) return

        if (session?.isActive == true) {
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
              "toolResponse": {
                "functionResponses": [
                  {
                    "id": "$toolId",
                    "name": "$toolName",
                    "response": {
                      "result": $resultJson
                    }
                  }
                ]
              }
            }
            """.trimIndent()

            try {
                session?.send(Frame.Text(msg))
                println("📤 Tool Response Sent: $toolName (ID: $toolId)")
                
                // 2. THE SILENT KICKSTART
                delay(50) 
                
                val kickstart = """
                {
                  "clientContent": {
                    "turns": [
                      {
                        "role": "user",
                        "parts": [ { "text": "[SYSTEM_SYNC]" } ]
                      }
                    ],
                    "turnComplete": true
                  }
                }
                """.trimIndent()
                session?.send(Frame.Text(kickstart))
                println("🚀 Silent Sync Triggered (Avoiding Deadlock)")
                
            } catch (e: Exception) {
                println("❌ Failed to send tool response: ${e.message} (Ignored)")
            }
        }
    }
=======
    data class FunctionCallPart(
        val name: String,
        val args: Map<String, String>
    )
>>>>>>> 3168251dc55074dd77c20a1833a1a0f0d5e38ab7

    sealed class ConnectionState {
        data object Disconnected : ConnectionState()
        data object Connecting : ConnectionState()
        data object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    // --- State ---
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _toolCalls = MutableSharedFlow<ToolCall>(replay = 0)
    val toolCalls: SharedFlow<ToolCall> = _toolCalls

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms
    
    private val _isAgentSpeaking = MutableStateFlow(false)
    val isAgentSpeaking: StateFlow<Boolean> = _isAgentSpeaking

    private var session: DefaultClientWebSocketSession? = null
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    fun connect(apiKey: String, systemInstruction: String, toolsConfig: String) {
        if (_connectionState.value is ConnectionState.Connected || _connectionState.value is ConnectionState.Connecting) return
        
        launch {
            try {
                _connectionState.value = ConnectionState.Connecting
                println("🚀 Connecting to Gemini Live...")
                
                // Monitor Speaking State
                val speakingJob = launch {
                    while (isActive) {
                        _isAgentSpeaking.value = audioPlayer.isPlaying()
                        delay(200) // Poll every 200ms
                    }
                }
                
                // Using the specific model from React implementation
                // model: 'gemini-2.5-flash-native-audio-preview-12-2025'
                val model = "gemini-2.5-flash-native-audio-preview-12-2025"
                val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
                
                client.wss(url) {
                    session = this
                    _connectionState.value = ConnectionState.Connected
                    println("✅ WebSocket Connected!")
                    
                    // 1. Send Setup
                    val setupMsg = buildJsonObject {
                        putJsonObject("setup") {
                            put("model", "models/$model")
                            putJsonObject("generation_config") {
                                putJsonArray("response_modalities") {
                                    add("AUDIO")
                                }
                                putJsonObject("speech_config") {
                                    putJsonObject("voice_config") {
                                        put("prebuilt_voice_config", buildJsonObject {
                                             put("voice_name", "Kore") // Or any efficient voice
                                        })
                                    }
                                }
                            }
                            // Convert tools definitions
                             putJsonObject("system_instruction") {
                                putJsonArray("parts") {
                                    add(buildJsonObject {
                                        put("text", systemInstruction)
                                    })
                                }
                            }
                            
                            // Parse and inject tools config from the string provided by ViewModel
                            try {
                                val toolsJsonElement = json.parseToJsonElement("{$toolsConfig}")
                                if (toolsJsonElement is JsonObject) {
                                     // The string is "tools": [...] wrapped in braces now
                                     val toolsArray = toolsJsonElement["tools"]
                                     if (toolsArray != null) {
                                         put("tools", toolsArray)
                                     }
                                }
                            } catch (e: Exception) {
                                println("⚠️ Failed to parse tools config: ${e.message}")
                            }
                        }
                    }
                    send(Frame.Text(setupMsg.toString()))
                    
                    // 2. Start Sub-jobs
                    val micJob = launch { streamMicAudio() }
                    val receiveJob = launch { receiveLoop() }
                    
                    // Wait for either to fail or cancellation
                    try {
                        joinAll(micJob, receiveJob)
                    } catch (e: CancellationException) {
                        // Normal shutdown
                    } finally {
                        micJob.cancel()
                        receiveJob.cancel()
                        speakingJob.cancel()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _connectionState.value = ConnectionState.Error(e.message ?: "Unknown Connection Error")
            } finally {
                _connectionState.value = ConnectionState.Disconnected
                session = null
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.streamMicAudio() {
        // According to React: sampleRate: 16000, channelCount: 1
        // Sending base64 PCM16
        audioRecorder.startRecording { 
            // Simple visualizer trigger if needed
        }.collect { audioData ->
            if (!isActive) return@collect
            
            // Calculate simple RMS for visualization
            var sum = 0.0
            for (byte in audioData) {
                sum += byte * byte
            }
            val rms = kotlin.math.sqrt(sum / audioData.size).toFloat() / 128f // Approx normalization
            _audioRms.value = rms
            
            // Format for RealtimeInput
            val msg = buildJsonObject {
               putJsonObject("realtime_input") {
                   putJsonArray("media_chunks") {
                       add(buildJsonObject {
                            put("mime_type", "audio/pcm;rate=16000") // Required rate param
                            put("data", Base64.encode(audioData))
                       })
                   }
               }
            }
            // println("🎤 Sending Audio Chunk: ${audioData.size} bytes")
            send(Frame.Text(msg.toString()))
        }
    }

    private suspend fun DefaultClientWebSocketSession.receiveLoop() {
        for (frame in incoming) {
            if (frame is Frame.Text) {
                val text = frame.readText()
                 // Log response (Truncated to avoid spamming pure Audio data, but allow Metadata/Tools)
                 if (!text.contains("server_content") || text.length < 500) {
                     println("📩 Received: ${text.take(1000)}")
                 } else {
                     println("📩 Received Audio Chunk (${text.length} chars)")
                 }
                
                try {
                    val root = json.parseToJsonElement(text).jsonObject
                    
                    // Handle Server Content (Audio) -> Use snake_case
                    val serverContent = root["server_content"]?.jsonObject
                    if (serverContent != null) {
                        val modelTurn = serverContent["model_turn"]?.jsonObject
                        val parts = modelTurn?.get("parts")?.jsonArray
                        
                        parts?.forEach { part ->
                            val inlineData = part.jsonObject["inline_data"]?.jsonObject
                            if (inlineData != null) {
                                val data = inlineData["data"]?.jsonPrimitive?.content
                                if (data != null) {
                                    val pcm = Base64.decode(data)
                                    audioPlayer.play(pcm)
                                }
                            }
                        }
                        
                        // Handle Loop Finished logic if needed
                        val turnComplete = serverContent["turn_complete"]?.jsonPrimitive?.booleanOrNull
                        if (turnComplete == true) {
                            // Can signal turn end
                        }
                    }
                    
                    // Handle Tool Calls -> Use snake_case
                    val toolCallObj = root["tool_call"]?.jsonObject
                    if (toolCallObj != null) {
                         val functionCalls = toolCallObj["function_calls"]?.jsonArray
                         functionCalls?.forEach { fc ->
                             val fcObj = fc.jsonObject
                             val name = fcObj["name"]?.jsonPrimitive?.content ?: ""
                             val id = fcObj["id"]?.jsonPrimitive?.content ?: ""
                             val argsObj = fcObj["args"]?.jsonObject
                             
                             // Convert generic args to Map<String, String> for simplicity
                             // Logic assumes flat args for now based on 'saveField' signature
                             val argsMap = argsObj?.entries?.associate { (k, v) -> 
                                 k to (v.jsonPrimitive.content) 
                             }
                             
                             if (name.isNotEmpty()) {
                                 println("🛠️ Tool Call: $name")
                                 _toolCalls.emit(ToolCall(id, name, argsMap))
                             }
                         }
                    }
                    
                } catch (e: Exception) {
                    println("⚠️ Parsing Error: ${e.message}")
                }
            }
        }
    }

    fun sendToolResponse(toolName: String, response: Map<String, Any>, toolId: String) {
        val s = session ?: return
        
        launch {
            val msg = buildJsonObject {
                putJsonObject("tool_response") {
                    putJsonArray("function_responses") {
                        add(buildJsonObject {
                            put("name", toolName)
                            put("id", toolId)
                            putJsonObject("response") {
                                put("result", buildJsonObject { 
                                    // Converting response map to JsonObject
                                    response.forEach { (k, v) ->
                                        put(k, v.toString())
                                    }
                                })
                            }
                        })
                    }
                }
            }
            s.send(Frame.Text(msg.toString()))
        }
    }

    suspend fun disconnect() {
        session?.close()
        session = null
        audioPlayer.stop()
        audioRecorder.stopRecording()
    }
}
