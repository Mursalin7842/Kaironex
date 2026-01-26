package com.mursaline.kaironex.core.gemini


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

@Suppress("unused")
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
            println("[Gemini] No API key - halting.")
            return GeminiResponse("Gemini API Key is missing.")
        }

        // SILENCED FOR VOICE DEBUGGING
        println("[Gemini] Text Chat Silenced by User Request.")
        return GeminiResponse("")
    }
}

@Suppress("unused")
@Serializable
data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@Suppress("unused")
@Serializable
data class Content(
    val role: String,
    val parts: List<Part>
)

@Suppress("unused")
@Serializable
data class Part(
    val text: String
)

@Suppress("unused")
@Serializable
data class GeminiApiResponse(
    val candidates: List<Candidate>? = null
)

@Suppress("unused")
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
