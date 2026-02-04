package com.mursaline.kaironex.brain

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
data class BrainExecutionPayload(
    val path: String,
    val method: String,
    val userId: String,
    val agent: String? = null,
    val prompt: String? = null
)

class AppwriteBridge(
    private val client: HttpClient,
    private val projectId: String = com.mursaline.kaironex.core.AppConfig.Appwrite.PROJECT_ID,
    private val functionId: String = com.mursaline.kaironex.core.AppConfig.Appwrite.FUNCTION_ID,
    private val apiKey: String = com.mursaline.kaironex.core.AppConfig.Appwrite.API_KEY
) {
    // Call the Python Brain (System 2) - Supports Long-Running Tasks
    suspend fun consultBrain(userId: String, agent: String, intent: String): String {
        val endpoint = "https://cloud.appwrite.io/v1/functions/$functionId/executions"
        
        val payload = BrainExecutionPayload(
            path = "/brain/deep",
            method = "POST",
            userId = userId,
            agent = agent,
            prompt = intent
        )

        val jsonBody = Json.encodeToString(BrainExecutionPayload.serializer(), payload)

        return try {
            // 1. Trigger Execution (Async)
            val triggerResponse = client.post(endpoint) {
                header("X-Appwrite-Project", projectId)
                if (apiKey.isNotBlank()) header("X-Appwrite-Key", apiKey)
                header("Content-Type", "application/json")
                setBody(buildJsonObject {
                    put("body", jsonBody)
                    put("async", true) // ENABLE 15 MIN RUNTIME
                })
            }
            
            // Parse Execution Key
            val initialJson = Json.parseToJsonElement(triggerResponse.bodyAsText()).jsonObject
            val executionId = initialJson["\$id"]?.jsonPrimitive?.content 
                ?: return "Failed to start brain execution."

            // 2. Poll for Completion (Max 90s for UI responsiveness)
            var status = initialJson["status"]?.jsonPrimitive?.content
            var attempts = 0
            val maxAttempts = 90
            var finalResponseBody = ""

            // We need a specific endpoint to GET execution details: /functions/{functionId}/executions/{executionId}
            val pollEndpoint = "$endpoint/$executionId"

            while ((status == "processing" || status == "waiting") && attempts < maxAttempts) {
                kotlinx.coroutines.delay(1000)
                attempts++
                
                val pollResponse = client.get(pollEndpoint) {
                    header("X-Appwrite-Project", projectId)
                    if (apiKey.isNotBlank()) header("X-Appwrite-Key", apiKey)
                }
                
                val pollJson = Json.parseToJsonElement(pollResponse.bodyAsText()).jsonObject
                status = pollJson["status"]?.jsonPrimitive?.content
                finalResponseBody = pollJson["responseBody"]?.jsonPrimitive?.content ?: ""
            }

            if (status == "completed") {
                finalResponseBody
            } else {
                "Brain timed out or failed. Status: $status"
            }
            
        } catch (e: Exception) {
            "Connection to Cortex severed: ${e.message}"
        }
    }
    // Generic Async Trigger for any Agent (Campaign, Vitality, etc.)
    suspend fun triggerBrain(userId: String, endpoint: String, eventType: String, data: Map<String, Any>): String {
        val functionUrl = "https://cloud.appwrite.io/v1/functions/$functionId/executions"
        
        // Construct Payload matching main.py expectations
        val payload = buildJsonObject {
            put("endpoint", endpoint)
            put("userId", userId)
            put("type", eventType)
            put("data", buildJsonObject {
                data.forEach { (k, v) ->
                    when (v) {
                        is String -> put(k, v)
                        is Number -> put(k, v)
                        is Boolean -> put(k, v)
                        else -> put(k, v.toString())
                    }
                }
            })
        }
        val jsonBody = payload.toString()

        return try {
            // 1. Trigger Async Execution (Bypass 30s Limit)
            val triggerResponse = client.post(functionUrl) {
                header("X-Appwrite-Project", projectId)
                if (apiKey.isNotBlank()) header("X-Appwrite-Key", apiKey)
                header("Content-Type", "application/json")
                setBody(buildJsonObject {
                    put("body", jsonBody)
                    put("async", true) // ENABLE 15 MIN RUNTIME
                })
            }

            val initialJson = Json.parseToJsonElement(triggerResponse.bodyAsText()).jsonObject
            val executionId = initialJson["\$id"]?.jsonPrimitive?.content 
                ?: return "Failed to trigger brain."

            // 2. Poll for Result
            var status = initialJson["status"]?.jsonPrimitive?.content
            var attempts = 0
            val maxAttempts = 90
            var finalResponseBody = ""
            val pollEndpoint = "$functionUrl/$executionId"

            while ((status == "processing" || status == "waiting") && attempts < maxAttempts) {
                kotlinx.coroutines.delay(1000)
                attempts++
                val pollResponse = client.get(pollEndpoint) {
                    header("X-Appwrite-Project", projectId)
                    if (apiKey.isNotBlank()) header("X-Appwrite-Key", apiKey)
                }
                val pollJson = Json.parseToJsonElement(pollResponse.bodyAsText()).jsonObject
                status = pollJson["status"]?.jsonPrimitive?.content
                finalResponseBody = pollJson["responseBody"]?.jsonPrimitive?.content ?: ""
            }

            if (status == "completed") {
                finalResponseBody
            } else {
                "Brain Timeout/Fail: $status"
            }
        } catch (e: Exception) {
            "Connection Error: ${e.message}"
        }
    }
}
