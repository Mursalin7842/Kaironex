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

    data class FunctionCallPart(
        val name: String,
        val args: Map<String, String>
    )

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
                   putJsonObject("media_chunks") {
                       put("mime_type", "audio/pcm") // Defaults to 16kHz Little Endian PCM
                       put("data", Base64.encode(audioData))
                   }
               }
            }
                    send(Frame.Text(msg.toString()))
        }
    }

    private suspend fun DefaultClientWebSocketSession.receiveLoop() {
        for (frame in incoming) {
            if (frame is Frame.Text) {
                val text = frame.readText()
               // println("📩 Received: ${text.take(100)}...")
                
                try {
                    val root = json.parseToJsonElement(text).jsonObject
                    
                    // Handle Server Content (Audio)
                    val serverContent = root["serverContent"]?.jsonObject
                    if (serverContent != null) {
                        val modelTurn = serverContent["modelTurn"]?.jsonObject
                        val parts = modelTurn?.get("parts")?.jsonArray
                        
                        parts?.forEach { part ->
                            val inlineData = part.jsonObject["inlineData"]?.jsonObject
                            if (inlineData != null) {
                                val data = inlineData["data"]?.jsonPrimitive?.content
                                if (data != null) {
                                    val pcm = Base64.decode(data)
                                    audioPlayer.play(pcm)
                                }
                            }
                        }
                        
                        // Handle Loop Finished logic if needed
                        val turnComplete = serverContent["turnComplete"]?.jsonPrimitive?.booleanOrNull
                        if (turnComplete == true) {
                            // Can signal turn end
                        }
                    }
                    
                    // Handle Tool Calls
                    val toolCallObj = root["toolCall"]?.jsonObject
                    if (toolCallObj != null) {
                         val functionCalls = toolCallObj["functionCalls"]?.jsonArray
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
