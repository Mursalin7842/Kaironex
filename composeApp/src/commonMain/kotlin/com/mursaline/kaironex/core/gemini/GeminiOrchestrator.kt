package com.mursaline.kaironex.core.gemini

import com.mursaline.kaironex.features.genesis.ChatMessage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.mursaline.kaironex.PlatformSecrets

object GeminiOrchestrator {

    private val apiKey: String
        get() = try {
            PlatformSecrets.apiKey
        } catch (e: Exception) {
            ""
        }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
    }

    suspend fun chat(history: List<ChatMessage>, userText: String, systemPrompt: String): GeminiResponse {
        println("[Gemini] API Key present: ${apiKey.isNotEmpty() && apiKey != "PLACEHOLDER" && apiKey != "PLACEHOLDER_FOR_DESKTOP"}")
        println("[Gemini] User input: $userText")

        if (apiKey.isEmpty() || apiKey == "PLACEHOLDER" || apiKey == "PLACEHOLDER_FOR_DESKTOP") {
            println("[Gemini] No API key - using demo mode")
            return getDemoResponse(userText, history.size)
        }

        val request = GeminiRequest(
            contents = history.map { 
                Content(role = if (it.sender == "user") "user" else "model", parts = listOf(Part(text = it.content))) 
            } + Content(role = "user", parts = listOf(Part(text = userText))),
            systemInstruction = Content(role = "user", parts = listOf(Part(text = systemPrompt)))
        )

        // Try multiple model endpoints with different API versions
        val endpoints = listOf(
            // v1beta endpoints (newer features)
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent",
            // v1 endpoints (stable)
            "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash-latest:generateContent",
            "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent",
            "https://generativelanguage.googleapis.com/v1/models/gemini-pro:generateContent"
        )

        for (baseUrl in endpoints) {
            try {
                val url = "$baseUrl?key=$apiKey"
                println("[Gemini] Trying: $baseUrl")

                val httpResponse = client.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                println("[Gemini] Response status: ${httpResponse.status}")

                if (httpResponse.status.value == 404) {
                    println("[Gemini] 404 - Model not found, trying next...")
                    continue
                }

                val response: GeminiApiResponse = httpResponse.body()

                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    println("[Gemini] ✓ Success!")
                    println("[Gemini] Response: ${text.take(100)}...")
                    return GeminiResponse(text)
                } else {
                    println("[Gemini] Empty response")
                }
            } catch (e: Exception) {
                println("[Gemini] ✗ Failed: ${e::class.simpleName} - ${e.message}")
                continue
            }
        }

        // All models failed - return a fallback conversational response
        println("[Gemini] All models failed, using demo mode")
        return getDemoResponse(userText, history.size)
    }

    // Demo responses for when API is unavailable
    private fun getDemoResponse(userText: String?, turnCount: Int): GeminiResponse {
        val responses = listOf(
            "Great to meet you! Tell me, what are you studying? I'd love to know your major or field.",
            "Interesting! And what semester or year are you in right now?",
            "Got it! Do you have any part-time jobs or commitments outside of school?",
            "Thanks for sharing! What would you say is your biggest challenge as a student right now?",
            "I understand. What time of day do you feel most productive for studying?",
            "Perfect! I think I have a good picture now. Let's set up your personalized dashboard!"
        )
        val index = turnCount.coerceIn(0, responses.size - 1)
        return GeminiResponse(responses[index])
    }
}

@Serializable
data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val role: String,
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class GeminiApiResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

data class GeminiResponse(
    val text: String
)

// Temporary Secrets holder if BuildKonfig isn't available
object Secrets {
    // In a real KMP project, use BuildKonfig. 
    // Here we will use a variable that can be set by the Platform.
    var API_KEY: String = "PLACEHOLDER"
}
