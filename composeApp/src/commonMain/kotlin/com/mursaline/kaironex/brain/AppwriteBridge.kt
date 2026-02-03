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
    // Call the Python Brain (System 2)
    suspend fun consultBrain(userId: String, agent: String, intent: String): String {
        val endpoint = "https://cloud.appwrite.io/v1/functions/$functionId/executions"
        
        val payload = BrainExecutionPayload(
            path = "/brain/deep", // Matches your main.py route
            method = "POST",
            userId = userId,
            agent = agent,
            prompt = intent
        )

        val jsonBody = Json.encodeToString(BrainExecutionPayload.serializer(), payload)

        return try {
            val response = client.post(endpoint) {
                header("X-Appwrite-Project", projectId)
                if (apiKey.isNotBlank()) {
                    header("X-Appwrite-Key", apiKey)
                }
                header("Content-Type", "application/json")
                setBody(buildJsonObject {
                    put("body", jsonBody)
                    put("async", false) // Wait for the answer
                })
            }
            // Parse the function response string
            val responseJson = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            responseJson["responseBody"]?.jsonPrimitive?.content ?: "The brain was silent."
        } catch (e: Exception) {
            "Connection to Cortex severed: ${e.message}"
        }
    }
}
