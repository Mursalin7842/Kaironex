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
        val endpoint = "${com.mursaline.kaironex.core.AppConfig.Appwrite.ENDPOINT}/functions/$functionId/executions"
        
        val payload = BrainExecutionPayload(
            path = "/brain/deep",
            method = "POST",
            userId = userId,
            agent = agent,
            prompt = intent
        )

        val jsonBody = Json.encodeToString(BrainExecutionPayload.serializer(), payload)

        return try {
            val response = client.post(endpoint) {
                header("X-Appwrite-Project", projectId)
                if (apiKey.isNotBlank()) header("X-Appwrite-Key", apiKey)
                header("Content-Type", "application/json")
                setBody(buildJsonObject {
                    put("body", jsonBody)
                    put("async", false) // SYNC EXECUTION FOR RELIABILITY
                })
            }
            
            val responseBody = response.bodyAsText()
            println("🧠 Brain Sync Response: $responseBody")
            
            val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject
            val status = jsonResponse["status"]?.jsonPrimitive?.content
            val result = jsonResponse["responseBody"]?.jsonPrimitive?.content ?: ""
            
            if (status == "completed") {
                result
            } else {
                buildJsonObject {
                    put("status", "error")
                    put("message", "Brain Execution Failed: $status")
                    put("details", result)
                }.toString()
            }
        } catch (e: Exception) {
            buildJsonObject {
                put("status", "error")
                put("message", "Connection to Cortex severed: ${e.message}")
            }.toString()
        }
    }
    // Generic Async Trigger for any Agent (Campaign, Vitality, etc.)
    suspend fun triggerBrain(userId: String, endpoint: String, eventType: String, data: Map<String, Any>): String {
        val functionUrl = "${com.mursaline.kaironex.core.AppConfig.Appwrite.ENDPOINT}/functions/$functionId/executions"
        
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
            val response = client.post(functionUrl) {
                header("X-Appwrite-Project", projectId)
                if (apiKey.isNotBlank()) header("X-Appwrite-Key", apiKey)
                header("Content-Type", "application/json")
                setBody(buildJsonObject {
                    put("body", jsonBody)
                    put("async", false) // SYNC EXECUTION
                })
            }

            val responseBody = response.bodyAsText()
            println("🧠 Brain Sync Response: $responseBody")
            
            val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject
            val status = jsonResponse["status"]?.jsonPrimitive?.content
            val result = jsonResponse["responseBody"]?.jsonPrimitive?.content ?: ""

            if (status == "completed") {
                result
            } else {
                buildJsonObject {
                    put("status", "error")
                    put("message", "Brain Trigger Failed: $status")
                    put("details", result)
                }.toString()
            }
        } catch (e: Exception) {
            buildJsonObject {
                put("status", "error")
                put("message", "Connection Error: ${e.message}")
            }.toString()
        }
    }
}
