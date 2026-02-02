package com.mursaline.kaironex.brain

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 🧠 REFLEX AGENT
 * ===============
 * Local fast-response agent for instant UI feedback.
 *
 * Architecture:
 * - Primary: Uses BrainApiClient's /quick endpoint (Gemini 3 Flash + MINIMAL thinking)
 * - Fallback: Local pattern matching for common intents
 * - Cache: Frequently asked questions stored locally
 *
 * Response Time Target: < 500ms
 *
 * Usage:
 *   val response = reflexAgent.respond("What's my next task?")
 */
class ReflexAgent(
    private val brainClient: BrainApiClient,
    private val userId: String
) {

    // Local response cache for common patterns
    private val responseCache = mutableMapOf<String, CachedResponse>()
    private val cacheExpiryMs = 5 * 60 * 1000L // 5 minutes

    // Quick response patterns (fallback when offline)
    private val quickPatterns = listOf(
        QuickPattern(
            keywords = listOf("hello", "hi", "hey"),
            response = "Hey! I'm here to help. What's on your mind?",
            intent = "greeting"
        ),
        QuickPattern(
            keywords = listOf("next task", "what's next", "todo"),
            response = "Let me check your schedule...",
            intent = "schedule_query"
        ),
        QuickPattern(
            keywords = listOf("how am i doing", "progress", "stats"),
            response = "Looking at your recent activity...",
            intent = "progress_query"
        ),
        QuickPattern(
            keywords = listOf("tired", "exhausted", "break"),
            response = "I notice you might need a break. Your body knows best.",
            intent = "vitality_check"
        ),
        QuickPattern(
            keywords = listOf("deadline", "due", "urgent"),
            response = "Let me pull up your upcoming deadlines...",
            intent = "deadline_query"
        ),
        QuickPattern(
            keywords = listOf("study", "focus", "concentrate"),
            response = "Ready to enter focus mode? I'll minimize distractions.",
            intent = "focus_start"
        ),
        QuickPattern(
            keywords = listOf("help", "what can you do", "features"),
            response = "I can help with scheduling, studying, tracking progress, and keeping you on track with your goals. Just ask!",
            intent = "help"
        )
    )

    /**
     * Get a quick reflex response.
     * Tries: Cache -> Brain API -> Local patterns
     */
    suspend fun respond(
        prompt: String,
        agent: String = "generic",
        forceRefresh: Boolean = false
    ): ReflexResponse {
        val startTime = System.currentTimeMillis()

        // 1. Check cache (unless force refresh)
        if (!forceRefresh) {
            val cached = getCachedResponse(prompt)
            if (cached != null) {
                return ReflexResponse(
                    content = cached.response,
                    source = ReflexSource.CACHE,
                    latencyMs = System.currentTimeMillis() - startTime,
                    confidence = cached.confidence,
                    intent = cached.intent
                )
            }
        }

        // 2. Try Brain API
        try {
            val apiResponse = brainClient.quickPrompt(
                userId = userId,
                prompt = prompt,
                agent = agent,
                mode = "reflex"
            )

            if (apiResponse != null) {
                // Cache successful responses
                cacheResponse(prompt, apiResponse.response, apiResponse.confidence, null)

                return ReflexResponse(
                    content = apiResponse.response,
                    source = ReflexSource.BRAIN_API,
                    latencyMs = System.currentTimeMillis() - startTime,
                    confidence = apiResponse.confidence,
                    modeUsed = apiResponse.modeUsed
                )
            }
        } catch (e: Exception) {
            println("⚠️ Brain API call failed: ${e.message}")
        }

        // 3. Fallback to local pattern matching
        val localMatch = matchLocalPattern(prompt)

        return ReflexResponse(
            content = localMatch?.response ?: "I'm processing your request...",
            source = ReflexSource.LOCAL_PATTERN,
            latencyMs = System.currentTimeMillis() - startTime,
            confidence = if (localMatch != null) 0.6f else 0.3f,
            intent = localMatch?.intent
        )
    }

    /**
     * Quick yes/no decision.
     */
    suspend fun decide(question: String): Boolean {
        val response = respond("Answer yes or no: $question", agent = "decision")
        val content = response.content.lowercase()
        return content.contains("yes") || content.contains("true") || content.contains("affirmative")
    }

    /**
     * Extract intent from user input.
     */
    suspend fun extractIntent(input: String): IntentResult {
        // First try local patterns for speed
        val localMatch = matchLocalPattern(input)
        if (localMatch != null) {
            return IntentResult(
                intent = localMatch.intent,
                confidence = 0.7f,
                entities = emptyMap()
            )
        }

        // Otherwise ask the brain
        val response = respond(
            prompt = "Extract the intent from: \"$input\". Respond with just the intent name.",
            agent = "intent"
        )

        return IntentResult(
            intent = response.content.trim().lowercase().replace(" ", "_"),
            confidence = response.confidence,
            entities = emptyMap()
        )
    }

    /**
     * Generate a quick suggestion based on context.
     */
    suspend fun suggest(context: String): String {
        val response = respond(
            prompt = "Based on this context, give one brief helpful suggestion: $context",
            agent = "advisor"
        )
        return response.content
    }

    /**
     * Check if input matches wake word pattern.
     * Returns the extracted command if wake word is detected.
     */
    fun checkWakeWord(input: String, wakeWord: String): WakeWordResult {
        val normalizedInput = input.trim().lowercase()
        val normalizedWake = wakeWord.trim().lowercase()

        // Check for "Hey {agentName}" pattern
        val heyPattern = "hey $normalizedWake"
        val okayPattern = "okay $normalizedWake"

        return when {
            normalizedInput.startsWith(heyPattern) -> {
                val command = normalizedInput.removePrefix(heyPattern).trim()
                WakeWordResult(
                    detected = true,
                    command = command.ifEmpty { null },
                    pattern = "hey"
                )
            }
            normalizedInput.startsWith(okayPattern) -> {
                val command = normalizedInput.removePrefix(okayPattern).trim()
                WakeWordResult(
                    detected = true,
                    command = command.ifEmpty { null },
                    pattern = "okay"
                )
            }
            normalizedInput == normalizedWake -> {
                WakeWordResult(detected = true, command = null, pattern = "direct")
            }
            else -> {
                WakeWordResult(detected = false, command = null, pattern = null)
            }
        }
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private fun matchLocalPattern(input: String): QuickPattern? {
        val normalized = input.lowercase()
        return quickPatterns.find { pattern ->
            pattern.keywords.any { keyword -> normalized.contains(keyword) }
        }
    }

    private fun getCachedResponse(prompt: String): CachedResponse? {
        val key = prompt.lowercase().hashCode().toString()
        val cached = responseCache[key] ?: return null

        // Check expiry
        if (System.currentTimeMillis() - cached.timestamp > cacheExpiryMs) {
            responseCache.remove(key)
            return null
        }

        return cached
    }

    private fun cacheResponse(prompt: String, response: String, confidence: Float, intent: String?) {
        val key = prompt.lowercase().hashCode().toString()
        responseCache[key] = CachedResponse(
            response = response,
            confidence = confidence,
            intent = intent,
            timestamp = System.currentTimeMillis()
        )

        // Limit cache size
        if (responseCache.size > 100) {
            val oldest = responseCache.entries.minByOrNull { it.value.timestamp }
            oldest?.let { responseCache.remove(it.key) }
        }
    }

    fun clearCache() {
        responseCache.clear()
    }
}

// =========================================================================
// DATA CLASSES
// =========================================================================

data class ReflexResponse(
    val content: String,
    val source: ReflexSource,
    val latencyMs: Long,
    val confidence: Float,
    val intent: String? = null,
    val modeUsed: String? = null
)

enum class ReflexSource {
    CACHE,
    BRAIN_API,
    LOCAL_PATTERN
}

data class IntentResult(
    val intent: String,
    val confidence: Float,
    val entities: Map<String, String>
)

data class WakeWordResult(
    val detected: Boolean,
    val command: String?,
    val pattern: String?
)

private data class QuickPattern(
    val keywords: List<String>,
    val response: String,
    val intent: String
)

private data class CachedResponse(
    val response: String,
    val confidence: Float,
    val intent: String?,
    val timestamp: Long
)
