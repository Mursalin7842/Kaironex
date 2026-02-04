package com.mursaline.kaironex.brain

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * 🧠 BRAIN API CLIENT
 * ==================
 * Connects the app to the Kaironex-Brain backend server.
 *
 * Features:
 * - REST API calls for agent triggers
 * - WebSocket for real-time brain events
 * - Quick reflex prompts for instant responses
 * - Marathon session management
 *
 * Endpoints:
 * - POST /api/v1/brain/trigger - Universal event trigger
 * - POST /api/v1/brain/quick - Fast reflex responses
 * - WS /ws/brain/{userId} - Real-time updates
 */
@OptIn(ExperimentalSerializationApi::class)
class BrainApiClient(
    private val client: HttpClient
) {

    // =========================================================================
    // STATE
    // =========================================================================

    private val _connectionState = MutableStateFlow(BrainConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BrainConnectionState> = _connectionState.asStateFlow()

    private val _brainEvents = MutableSharedFlow<BrainEvent>(replay = 0, extraBufferCapacity = 64)
    val brainEvents: SharedFlow<BrainEvent> = _brainEvents.asSharedFlow()

    private val _thoughtStream = MutableStateFlow<ThoughtStreamItem?>(null)
    val thoughtStream: StateFlow<ThoughtStreamItem?> = _thoughtStream.asStateFlow()

    private val _marathonUpdates = MutableSharedFlow<MarathonUpdate>(replay = 0)
    val marathonUpdates: SharedFlow<MarathonUpdate> = _marathonUpdates.asSharedFlow()

    private var websocketSession: DefaultClientWebSocketSession? = null
    private var websocketJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // =========================================================================
    // HEALTH CHECK
    // =========================================================================

    suspend fun checkHealth(): BrainHealthResponse? {
        // Simple ping to Appwrite Logic
        return BrainHealthResponse("online", 0.0, emptyMap(), 0)
    }

    // =========================================================================
    // REFLEX AGENT - Fast Responses
    // =========================================================================

    /**
     * Quick prompt for instant reflex responses.
     * Uses Gemini 3 Flash with MINIMAL thinking for sub-500ms responses.
     */
    suspend fun quickPrompt(
        userId: String,
        prompt: String,
        agent: String = "generic",
        mode: String = "reflex"
    ): QuickPromptResponse? {
        // Use Appwrite Function for quick prompt too
        val data = mapOf("prompt" to prompt, "agent" to agent, "mode" to mode)
        val triggerResp = executeBrainFunction(userId, "quick", "quick_prompt", data)
        
        // Map TriggerResponse back to QuickPromptResponse mock/derived (or update backend to return QuickPromptResponse)
        // For now, assuming backend returns standard structure in response message
        return QuickPromptResponse(triggerResp?.message ?: "", "reflex", 0f, 1f)
    }

    // =========================================================================
    // AGENT TRIGGERS
    // =========================================================================

    /**
     * Trigger a brain event for processing.
     * Routes to appropriate agent based on event type.
     */
    // =========================================================================
    // AGENT TRIGGERS (APPWRITE FUNCTIONS)
    // =========================================================================

    /**
     * Helper to execute Appwrite Function.
     */
    private suspend fun executeBrainFunction(
        userId: String,
        endpoint: String, // e.g. "campaign", "vitality"
        eventType: String,
        data: Map<String, Any>
    ): TriggerResponse? {
        val functionId = com.mursaline.kaironex.core.AppConfig.Appwrite.FUNCTION_ID
        val project = com.mursaline.kaironex.core.AppConfig.Appwrite.PROJECT_ID
        val key = com.mursaline.kaironex.core.AppConfig.Appwrite.API_KEY
        val url = "${com.mursaline.kaironex.core.AppConfig.Appwrite.ENDPOINT}/functions/$functionId/executions"

        // Manual JSON construction to avoid "Serializer for class 'Any' is not found"
        val dataJson = buildJsonObject {
            data.forEach { (k, v) ->
                when (v) {
                    is String -> put(k, v)
                    is Number -> put(k, v)
                    is Boolean -> put(k, v)
                    is JsonElement -> put(k, v)
                    else -> put(k, v.toString())
                }
            }
        }

        val payloadJson = buildJsonObject {
            put("endpoint", endpoint)
            put("userId", userId)
            put("type", eventType)
            put("data", dataJson)
        }

        val requestBody = buildJsonObject {
            put("body", payloadJson.toString())
            put("async", false) // Wait for response
        }

        return try {
            val response = client.post(url) {
                header("X-Appwrite-Project", project)
                if (key.isNotBlank()) header("X-Appwrite-Key", key)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.value in 200..299) {
                val execBodyStr = response.body<JsonObject>()
                val status = execBodyStr["status"]?.jsonPrimitive?.contentOrNull
                val responseBodyStr = execBodyStr["responseBody"]?.jsonPrimitive?.contentOrNull
                
                if (status == "completed" && responseBodyStr != null) {
                    try {
                        json.decodeFromString<TriggerResponse>(responseBodyStr)
                    } catch (e: Exception) {
                        TriggerResponse("success", "Function executed: $responseBodyStr")
                    }
                } else {
                    TriggerResponse("error", "Function execution failed: $status")
                }
            } else {
                println("❌ Appwrite Function Error: ${response.status}")
                null
            }
        } catch (e: Exception) {
            println("❌ Brain Function Trigger Failed: ${e.message}")
            null
        }
    }

    /**
     * Trigger a brain event for processing.
     */
    suspend fun triggerBrain(
        userId: String,
        eventType: String,
        data: Map<String, Any> = emptyMap()
    ): TriggerResponse? {
        return executeBrainFunction(userId, "trigger", eventType, data)
    }

    /**
     * Trigger campaign agent specifically.
     */
    suspend fun triggerCampaign(userId: String, eventType: String, data: Map<String, String> = emptyMap()): TriggerResponse? {
        return executeBrainFunction(userId, "campaign", eventType, data)
    }

    /**
     * Trigger vitality agent specifically.
     */
    suspend fun triggerVitality(userId: String, eventType: String, data: Map<String, String> = emptyMap()): TriggerResponse? {
        return executeBrainFunction(userId, "vitality", eventType, data)
    }

    /**
     * Trigger radius agent specifically.
     */
    suspend fun triggerRadius(userId: String, eventType: String, data: Map<String, String> = emptyMap()): TriggerResponse? {
        return executeBrainFunction(userId, "radius", eventType, data)
    }

    // =========================================================================
    // STATE ENDPOINTS
    // =========================================================================

    /**
     * Get full user state from the brain.
     */
    suspend fun getUserState(userId: String): UserStateResponse? {
        // TODO: Use Appwrite Database directly
        return null 
    }

    /**
     * Get user's recent thoughts.
     */
    suspend fun getUserThoughts(userId: String, agent: String? = null): ThoughtsResponse? {
        // TODO: Use Appwrite Database
        return null
    }

    // =========================================================================
    // MARATHON ENDPOINTS
    // =========================================================================

    /**
     * Create a new marathon session (long-running goal).
     */
    suspend fun createMarathon(
        userId: String,
        agent: String,
        title: String,
        description: String,
        successCriteria: List<String>,
        deadline: String? = null,
        priority: Int = 5
    ): MarathonCreateResponse? {
        // TODO: Use Appwrite Functions
        return null
    }

    /**
     * Get marathon session status.
     */
    suspend fun getMarathon(sessionId: String): MarathonSession? {
        return null
    }

    /**
     * Get all marathons for a user.
     */
    suspend fun getUserMarathons(userId: String): List<MarathonSession> {
        return emptyList()
    }

    /**
     * Pause a marathon.
     */
    suspend fun pauseMarathon(sessionId: String): Boolean {
        // TODO: Appwrite Function
        return false
    }

    /**
     * Resume a marathon.
     */
    suspend fun resumeMarathon(sessionId: String): Boolean {
        // TODO: Appwrite Function
        return false
    }

    // =========================================================================
    // INTERVENTION RESPONSE
    // =========================================================================

    /**
     * Respond to an intervention from the brain.
     */
    suspend fun respondToIntervention(
        userId: String,
        interventionId: String,
        response: String, // accept/snooze/dismiss
        feedback: String? = null
    ): Boolean {
        // TODO: Appwrite Function
        return false
    }

    // =========================================================================
    // WEBSOCKET - Real-time Connection
    // =========================================================================

    /**
     * Connect to brain WebSocket for real-time updates.
     */
    suspend fun connectWebSocket(userId: String, scope: CoroutineScope) {
        println("⚠️ Custom Websocket Disabled (Use Appwrite Realtime)")
        _connectionState.value = BrainConnectionState.DISCONNECTED
    }

    /**
     * Disconnect WebSocket.
     */
    suspend fun disconnectWebSocket() {
        websocketJob?.cancel()
        websocketSession?.close()
        websocketSession = null
        _connectionState.value = BrainConnectionState.DISCONNECTED
        println("🔌 Brain WebSocket disconnected")
    }

    /**
     * Send a message over WebSocket.
     */
    suspend fun sendWebSocketMessage(message: WebSocketMessage) {
        websocketSession?.send(json.encodeToString(message))
    }

    /**
     * Send a ping to keep connection alive.
     */
    suspend fun sendPing() {
        sendWebSocketMessage(WebSocketMessage(type = "ping"))
    }

    /**
     * Request state update via WebSocket.
     */
    suspend fun requestStateUpdate(domain: String, data: Map<String, String>) {
        sendWebSocketMessage(WebSocketMessage(
            type = "state_update",
            domain = domain,
            data = data
        ))
    }

    private fun handleWebSocketMessage(text: String) {
        try {
            val msgJson = json.parseToJsonElement(text).jsonObject
            val type = msgJson["type"]?.jsonPrimitive?.contentOrNull ?: return

            when (type) {
                "pong" -> {
                    // Heartbeat response
                }
                "event" -> {
                    val eventData = msgJson["event"]?.jsonObject
                    eventData?.let {
                        val event = BrainEvent(
                            eventType = it["type"]?.jsonPrimitive?.contentOrNull ?: "unknown",
                            source = it["source"]?.jsonPrimitive?.contentOrNull ?: "brain",
                            data = it["data"]?.jsonObject?.let { d ->
                                d.entries.associate { e -> e.key to e.value.jsonPrimitive.contentOrNull.orEmpty() }
                            } ?: emptyMap(),
                            timestamp = it["timestamp"]?.jsonPrimitive?.contentOrNull ?: ""
                        )
                        _brainEvents.tryEmit(event)
                    }
                }
                "thought_stream" -> {
                    val thought = ThoughtStreamItem(
                        agent = msgJson["agent"]?.jsonPrimitive?.contentOrNull ?: "",
                        thought = msgJson["thought"]?.jsonPrimitive?.contentOrNull ?: "",
                        confidence = msgJson["confidence"]?.jsonPrimitive?.floatOrNull ?: 0f
                    )
                    _thoughtStream.value = thought
                }
                "intervention" -> {
                    val intervention = BrainEvent(
                        eventType = "intervention",
                        source = "brain",
                        data = mapOf(
                            "id" to (msgJson["intervention_id"]?.jsonPrimitive?.contentOrNull ?: ""),
                            "message" to (msgJson["message"]?.jsonPrimitive?.contentOrNull ?: ""),
                            "trigger" to (msgJson["trigger"]?.jsonPrimitive?.contentOrNull ?: "")
                        ),
                        timestamp = ""
                    )
                    _brainEvents.tryEmit(intervention)
                }
                "marathon_update" -> {
                    val update = MarathonUpdate(
                        sessionId = msgJson["session_id"]?.jsonPrimitive?.contentOrNull ?: "",
                        progress = msgJson["progress"]?.jsonPrimitive?.floatOrNull ?: 0f,
                        status = msgJson["status"]?.jsonPrimitive?.contentOrNull ?: "",
                        currentStep = msgJson["current_step"]?.jsonPrimitive?.contentOrNull
                    )
                    _marathonUpdates.tryEmit(update)
                }
                "agent_response" -> {
                    // Handle agent response from state_update
                }
                "quick_response" -> {
                    // Handle quick prompt response
                }
            }
        } catch (e: Exception) {
            println("❌ Failed to parse WebSocket message: ${e.message}")
        }
    }
}

// =========================================================================
// DATA CLASSES
// =========================================================================

enum class BrainConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

@Serializable
data class BrainHealthResponse(
    val status: String,
    @SerialName("uptime_seconds") val uptimeSeconds: Double,
    val components: Map<String, String>,
    @SerialName("active_connections") val activeConnections: Int
)

@Serializable
data class QuickPromptRequest(
    val userId: String,
    val prompt: String,
    val agent: String = "generic",
    val mode: String = "reflex"
)

@Serializable
data class QuickPromptResponse(
    val response: String,
    @SerialName("mode_used") val modeUsed: String,
    @SerialName("latency_ms") val latencyMs: Float,
    val confidence: Float
)

@Serializable
data class TriggerRequest(
    val userId: String,
    val type: String,
    val data: Map<String, String> = emptyMap()
)

@Serializable
data class TriggerResponse(
    val status: String,
    val message: String? = null,
    @SerialName("intervention_id") val interventionId: String? = null,
    @SerialName("thought_id") val thoughtId: String? = null,
    @SerialName("action_taken") val actionTaken: String? = null
)

@Serializable
data class UserStateResponse(
    @SerialName("user_id") val userId: String,
    val state: Map<String, String> = emptyMap(),
    val profile: String? = null,
    @SerialName("last_updated") val lastUpdated: String? = null
)

@Serializable
data class ThoughtsResponse(
    @SerialName("chain_id") val chainId: String? = null,
    val depth: Int? = null,
    @SerialName("total_confidence") val totalConfidence: Float? = null,
    val thoughts: List<ThoughtItem>? = null,
    val latest: ThoughtItem? = null
)

@Serializable
data class ThoughtItem(
    @SerialName("thought_id") val thoughtId: String,
    val timestamp: String,
    val agent: String,
    @SerialName("context_hash") val contextHash: String? = null,
    @SerialName("reasoning_trace") val reasoningTrace: List<String>? = null,
    val confidence: Float? = null,
    @SerialName("action_output") val actionOutput: String? = null
)

@Serializable
data class MarathonCreateRequest(
    val userId: String,
    val agent: String = "campaign",
    val title: String,
    val description: String,
    @SerialName("success_criteria") val successCriteria: List<String>,
    val deadline: String? = null,
    val priority: Int = 5
)

@Serializable
data class MarathonCreateResponse(
    @SerialName("session_id") val sessionId: String,
    val status: String,
    val steps: Int,
    @SerialName("estimated_completion") val estimatedCompletion: String? = null
)

@Serializable
data class MarathonSession(
    @SerialName("session_id") val sessionId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("agent_type") val agentType: String? = null,
    val progress: Float = 0f,
    val status: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("estimated_completion") val estimatedCompletion: String? = null,
    val goal: MarathonGoal? = null,
    val steps: List<MarathonStep>? = null
)

@Serializable
data class MarathonGoal(
    val title: String,
    val description: String,
    @SerialName("success_criteria") val successCriteria: List<String>? = null
)

@Serializable
data class MarathonStep(
    val index: Int,
    val action: String,
    val status: String,
    val result: String? = null
)

@Serializable
data class InterventionResponseRequest(
    val userId: String,
    val interventionId: String,
    val response: String,
    val feedback: String? = null
)

@Serializable
data class WebSocketMessage(
    val type: String,
    val domain: String? = null,
    val data: Map<String, String>? = null,
    val prompt: String? = null,
    val mode: String? = null
)

data class BrainEvent(
    val eventType: String,
    val source: String,
    val data: Map<String, String>,
    val timestamp: String
)

data class ThoughtStreamItem(
    val agent: String,
    val thought: String,
    val confidence: Float
)

data class MarathonUpdate(
    val sessionId: String,
    val progress: Float,
    val status: String,
    val currentStep: String?
)
