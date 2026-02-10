package com.mursaline.kaironex.features.study

import com.mursaline.kaironex.core.AppConfig
import com.mursaline.kaironex.core.KaironexSessionManager
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement

class ScheduleRepository(
    private val httpClient: HttpClient,
    private val sessionManager: KaironexSessionManager,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    private val userId: String
        get() = com.mursaline.kaironex.core.CurrentUser.userId.ifBlank {
            sessionManager.userId.value ?: "demo_user_001"
        }

    // Status constants matching backend
    object TaskStatusValues {
        const val PENDING = "pending"
        const val IN_PROGRESS = "active"
        const val COMPLETED = "completed"
        const val SKIPPED = "skipped"
    }

    @Serializable
    data class ScheduleTask(
        val id: String = "",
        val title: String = "",
        val description: String? = null,
        val startTime: String = "",
        val endTime: String = "",
        val status: String = "pending",
        val type: String = "study",
        val priority: Int = 5,
        val isFlexible: Boolean = false,
        val topics: String? = null,
        val linkedDeadline: String? = null,
        // NEW: Rich task metadata for resource generation
        val subject: String? = null,
        val difficulty: String? = null,
        val contentMode: String? = null,
        val metadataJson: String? = null,
        // Schema Update
        val taskId: String? = null,
        val userId: String? = null,
        val location: String? = null,
        
        // NEW: Just-in-Time Content Fields (from backend generator)
        @SerialName("just_in_time_resources")
        val justInTimeResources: String? = null,  // JSON array of resources
        @SerialName("flash_cards")
        val flashCards: String? = null,            // JSON array of flashcards
        @SerialName("macro_quizes")
        val macroQuizes: String? = null,           // JSON quiz object for gatekeeper
        @SerialName("quiz_result")
        val quizResult: String? = null             // Quiz attempt results for AI analysis
    ) : com.mursaline.kaironex.core.JavaSerializable

    private var cachedSchedule: List<ScheduleTask> = emptyList()
    
    // JSON configuration for lenient parsing of content fields
    private val contentJson = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }

    suspend fun fetchSchedule(limit: Int = 1000, forceRefresh: Boolean = false): List<ScheduleTask> {
        // Optimization: Return cached data if available and not forced
        if (!forceRefresh && cachedSchedule.isNotEmpty()) {
            println("✅ Returning cached schedule (Fast Load)")
            return cachedSchedule
        }

        val url = "${AppConfig.Appwrite.ENDPOINT}/databases/${AppConfig.Appwrite.DATABASE_ID}/collections/${AppConfig.Collections.SCHEDULE}/documents"
        
        return try {
            val response = httpClient.get(url) {
                header("X-Appwrite-Project", AppConfig.Appwrite.PROJECT_ID)
                if (AppConfig.Appwrite.API_KEY.isNotBlank()) {
                    header("X-Appwrite-Key", AppConfig.Appwrite.API_KEY)
                }
                parameter("queries[0]", """{"method":"equal","attribute":"userId","values":["$userId"]}""")
                parameter("queries[1]", """{"method":"orderAsc","attribute":"startTime"}""")
                parameter("queries[2]", """{"method":"limit","values":[$limit]}""")
            }

            if (response.status.value == 200) {
                val bodyText = response.bodyAsText()
                val root = json.parseToJsonElement(bodyText).jsonObject
                val documents = root["documents"]?.jsonArray ?: return cachedSchedule

                val newTasks = documents.mapNotNull { doc ->
                    try {
                        val obj = doc.jsonObject
                        ScheduleTask(
                            id = obj["\$id"]?.safeContent ?: "",
                            title = obj["title"]?.safeContent ?: "Untitled Task",
                            description = obj["description"]?.safeContent,
                            startTime = obj["startTime"]?.safeContent ?: "",
                            endTime = obj["endTime"]?.safeContent ?: "",
                            status = obj["status"]?.safeContent ?: "pending",
                            type = obj["type"]?.safeContent ?: "study",
                            priority = obj["priority"]?.safeContent?.toIntOrNull() ?: 5,
                            isFlexible = obj["is_flexible"]?.jsonPrimitive?.booleanOrNull ?: false,
                            topics = obj["topics"]?.safeContent,
                            linkedDeadline = obj["linked_deadline"]?.safeContent,
                            // NEW: Rich task metadata
                            subject = obj["subject"]?.safeContent,
                            difficulty = obj["difficulty"]?.safeContent,
                            contentMode = obj["content_mode"]?.safeContent,
                            metadataJson = obj["metadata_json"]?.safeContent,
                            // Schema Update
                            taskId = obj["taskId"]?.safeContent,
                            userId = obj["userId"]?.safeContent,
                            location = obj["location"]?.safeContent,
                            
                            // NEW: Parse content fields
                            justInTimeResources = obj["just_in_time_resources"]?.safeContent,
                            flashCards = obj["flash_cards"]?.safeContent,
                            macroQuizes = obj["macro_quizes"]?.safeContent,
                            quizResult = obj["quiz_result"]?.safeContent
                        )
                    } catch (e: Exception) {
                        println("⚠️ Failed to parse task: ${e.message}")
                        null
                    }
                }
                cachedSchedule = newTasks
                newTasks
            } else {
                println("❌ Fetch Schedule Failed: ${response.status}. Returning cached data.")
                cachedSchedule
            }
        } catch (e: Exception) {
            println("❌ Fetch Schedule Error: ${e.message}. Returning cached data.")
            cachedSchedule
        }
    }

    suspend fun fetchTask(taskId: String): ScheduleTask? {
        val url = "${AppConfig.Appwrite.ENDPOINT}/databases/${AppConfig.Appwrite.DATABASE_ID}/collections/${AppConfig.Collections.SCHEDULE}/documents/$taskId"
        
        return try {
            val response = httpClient.get(url) {
                header("X-Appwrite-Project", AppConfig.Appwrite.PROJECT_ID)
                if (AppConfig.Appwrite.API_KEY.isNotBlank()) {
                    header("X-Appwrite-Key", AppConfig.Appwrite.API_KEY)
                }
            }

            if (response.status.value == 200) {
                val bodyText = response.bodyAsText()
                val doc = json.parseToJsonElement(bodyText).jsonObject
                
                try {
                    ScheduleTask(
                        id = doc["\$id"]?.safeContent ?: "",
                        title = doc["title"]?.safeContent ?: "Untitled Task",
                        description = doc["description"]?.safeContent,
                        startTime = doc["startTime"]?.safeContent ?: "",
                        endTime = doc["endTime"]?.safeContent ?: "",
                        status = doc["status"]?.safeContent ?: "pending",
                        type = doc["type"]?.safeContent ?: "study",
                        priority = doc["priority"]?.safeContent?.toIntOrNull() ?: 5,
                        isFlexible = doc["is_flexible"]?.jsonPrimitive?.booleanOrNull ?: false,
                        topics = doc["topics"]?.safeContent,
                        linkedDeadline = doc["linked_deadline"]?.safeContent,
                        subject = doc["subject"]?.safeContent,
                        difficulty = doc["difficulty"]?.safeContent,
                        contentMode = doc["content_mode"]?.safeContent,
                        metadataJson = doc["metadata_json"]?.safeContent,
                        taskId = doc["taskId"]?.safeContent,
                        userId = doc["userId"]?.safeContent,
                        location = doc["location"]?.safeContent,
                        justInTimeResources = doc["just_in_time_resources"]?.safeContent,
                        flashCards = doc["flash_cards"]?.safeContent,
                        macroQuizes = doc["macro_quizes"]?.safeContent,
                        quizResult = doc["quiz_result"]?.safeContent
                    )
                } catch (e: Exception) {
                    println("⚠️ Failed to parse single task: ${e.message}")
                    null
                }
            } else {
                println("❌ Fetch Task Failed: ${response.status}")
                null
            }
        } catch (e: Exception) {
            println("❌ Fetch Task Error: ${e.message}")
            null
        }
    }

    /**
     * Update task status in Appwrite
     */
    suspend fun updateTaskStatus(taskId: String, newStatus: String): Boolean {
        val url = "${AppConfig.Appwrite.ENDPOINT}/databases/${AppConfig.Appwrite.DATABASE_ID}/collections/${AppConfig.Collections.SCHEDULE}/documents/$taskId"

        return try {
            val response = httpClient.patch(url) {
                header("X-Appwrite-Project", AppConfig.Appwrite.PROJECT_ID)
                if (AppConfig.Appwrite.API_KEY.isNotBlank()) {
                    header("X-Appwrite-Key", AppConfig.Appwrite.API_KEY)
                }
                header("Content-Type", "application/json")
                setBody("""{"data":{"status":"$newStatus"}}""")
            }

            val success = response.status.value in 200..299
            if (success) {
                // Update local cache
                cachedSchedule = cachedSchedule.map { task ->
                    if (task.id == taskId) task.copy(status = newStatus) else task
                }
            }
            success
        } catch (e: Exception) {
            println("❌ Update Task Status Error: ${e.message}")
            false
        }
    }

    /**
     * Mark task as completed
     */
    suspend fun completeTask(taskId: String): Boolean {
        return updateTaskStatus(taskId, TaskStatusValues.COMPLETED)
    }

    /**
     * Mark task as in progress
     */
    suspend fun startTask(taskId: String): Boolean {
        return updateTaskStatus(taskId, TaskStatusValues.IN_PROGRESS)
    }

    /**
     * Get today's tasks
     */
    fun getTodayTasks(): List<ScheduleTask> {
        val today = kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
            .date.toString()

        return cachedSchedule.filter { it.startTime.startsWith(today) }
    }

    /**
     * Get upcoming tasks (not completed)
     */
    fun getUpcomingTasks(): List<ScheduleTask> {
        return cachedSchedule.filter { it.status.lowercase() != "completed" }
    }

    /**
     * Clear cache to force refresh
     */
    fun clearCache() {
        cachedSchedule = emptyList()
    }
    
    // ═══════════════════════════════════════════════════════════
    // CONTENT PARSING HELPERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Parse resources from task's just_in_time_resources field
     */
    fun parseResources(task: ScheduleTask): List<StudyResource> {
        // 1. Try Top-Level Field
        if (!task.justInTimeResources.isNullOrBlank()) {
             try {
                // Handle potential double-serialization
                val cleanJson = unwrapJsonString(task.justInTimeResources)
                println("🔍 Parsing Resources (len=${cleanJson.length})...")
                return contentJson.decodeFromString<List<StudyResource>>(cleanJson)
            } catch (e: Exception) {
                println("⚠️ Top-level resource parse failed: ${e.message}")
            }
        }

        // 2. Fallback to metadata_json
        val metadata = task.metadataJson
        if (!metadata.isNullOrBlank()) {
            try {
                val cleanMeta = unwrapJsonString(metadata)
                val root = contentJson.parseToJsonElement(cleanMeta).jsonObject
                val aiContent = root["ai_content"]?.jsonObject
                
                if (aiContent != null) {
                    val resourcesArray = aiContent["resources"]?.jsonArray
                    if (resourcesArray != null) {
                         println("✅ Found resources in metadata_json")
                         return resourcesArray.map { 
                             val obj = it.jsonObject
                             StudyResource(
                                 id = obj["id"]?.jsonPrimitive?.content ?: "",
                                 name = obj["name"]?.jsonPrimitive?.content ?: obj["title"]?.jsonPrimitive?.content ?: "Untitled",
                                 type = obj["type"]?.jsonPrimitive?.content ?: "unknown",
                                 content = obj["content"]?.jsonPrimitive?.content ?: "",
                                 estimatedReadTime = obj["estimated_read_time"]?.jsonPrimitive?.content?.toIntOrNull() ?: 3
                             )
                         }
                    }
                }
            } catch (e: Exception) {
                println("⚠️ Metadata resource parse failed: ${e.message}")
            }
        }
        
        return emptyList()
    }

    /**
     * Parse flashcards from task's flash_cards field
     */
    fun parseFlashCards(task: ScheduleTask): List<FlashCard> {
        // 1. Try Top-Level Field
        if (!task.flashCards.isNullOrBlank()) {
            try {
                val cleanJson = unwrapJsonString(task.flashCards)
                println("🔍 Parsing Flashcards (len=${cleanJson.length})...")
                return contentJson.decodeFromString<List<FlashCard>>(cleanJson)
            } catch (e: Exception) {
                println("⚠️ Top-level flashcard parse failed: ${e.message}")
            }
        }
        
        // 2. Fallback to metadata_json
        val metadata = task.metadataJson
        if (!metadata.isNullOrBlank()) {
            try {
                val cleanMeta = unwrapJsonString(metadata)
                val root = contentJson.parseToJsonElement(cleanMeta).jsonObject
                val aiContent = root["ai_content"]?.jsonObject
                
                if (aiContent != null) {
                    val cardsArray = aiContent["flashcards"]?.jsonArray
                    if (cardsArray != null) {
                        println("✅ Found flashcards in metadata_json")
                        return contentJson.decodeFromJsonElement<List<FlashCard>>(cardsArray)
                    }
                }
            } catch (e: Exception) {
                println("⚠️ Metadata flashcard parse failed: ${e.message}")
            }
        }
        
        return emptyList()
    }

    /**
     * Parse gatekeeper quiz from task's macro_quizes field
     */
    fun parseGatekeeperQuiz(task: ScheduleTask): GatekeeperQuiz? {
        // 1. Try Top-Level Field
        if (!task.macroQuizes.isNullOrBlank()) {
            try {
                val cleanJson = unwrapJsonString(task.macroQuizes)
                println("🔍 Parsing Quiz (len=${cleanJson.length})...")
                return contentJson.decodeFromString<GatekeeperQuiz>(cleanJson)
            } catch (e: Exception) {
                 println("⚠️ Top-level quiz parse failed: ${e.message}")
            }
        }
        
        // 2. Fallback to metadata_json
        val metadata = task.metadataJson
        if (!metadata.isNullOrBlank()) {
            try {
                val cleanMeta = unwrapJsonString(metadata)
                val root = contentJson.parseToJsonElement(cleanMeta).jsonObject
                val aiContent = root["ai_content"]?.jsonObject
                val quizObj = aiContent?.get("quiz")?.jsonObject
                
                if (quizObj != null) {
                    println("✅ Found quiz in metadata_json")
                    return contentJson.decodeFromJsonElement<GatekeeperQuiz>(quizObj)
                }
            } catch (e: Exception) {
                 println("⚠️ Metadata quiz parse failed: ${e.message}")
            }
        }

        return null
    }

    private fun unwrapJsonString(raw: String): String {
        // If the string starts with a quote, it might be a double-encoded JSON string
        if (raw.startsWith("\"") && raw.endsWith("\"")) {
            try {
                // Helper to unescape: remove surrounding quotes and unescape \" to "
                // Simple version: use JSON parser string decoding
                return contentJson.decodeFromString<String>(raw)
            } catch (_: Exception) {
                return raw
            }
        }
        return raw
    }

    /**
     * Safe extension to get string content from JsonElement, handling JsonNull
     */
    private val JsonElement.safeContent: String?
        get() = try {
            if (this is JsonNull) null else this.jsonPrimitive.content
        } catch (_: Exception) {
            null
        }

    /**
     * Convert GatekeeperQuiz to existing GatekeeperQuestion format
     * Note: This is a bridge until UI fully adopts the new model
     */
    fun convertToGatekeeperQuestions(quiz: GatekeeperQuiz): List<com.mursaline.kaironex.features.study.gatekeeper.GatekeeperQuestion> {
        return quiz.questions.map { q ->
            com.mursaline.kaironex.features.study.gatekeeper.GatekeeperQuestion(
                id = q.questionId,
                question = q.questionText,
                options = q.options.map { it.text },
                correctAnswerIndex = q.options.indexOfFirst { it.id == q.correctAnswer },
                explanation = q.explanation
            )
        }
    }

    /**
     * Save quiz result to Appwrite for AI analysis
     */
    suspend fun saveQuizResult(taskId: String, result: QuizResult): Boolean {
        val url = "${AppConfig.Appwrite.ENDPOINT}/databases/${AppConfig.Appwrite.DATABASE_ID}/collections/${AppConfig.Collections.SCHEDULE}/documents/$taskId"

        return try {
            val resultJson = contentJson.encodeToString(QuizResult.serializer(), result)
            
            val response = httpClient.patch(url) {
                header("X-Appwrite-Project", AppConfig.Appwrite.PROJECT_ID)
                if (AppConfig.Appwrite.API_KEY.isNotBlank()) {
                    header("X-Appwrite-Key", AppConfig.Appwrite.API_KEY)
                }
                header("Content-Type", "application/json")
                setBody("""{"data":{"quiz_result":$resultJson}}""")
            }

            response.status.value in 200..299
        } catch (e: Exception) {
            println("❌ Save Quiz Result Error: ${e.message}")
            false
        }
    }

    /**
     * Check if student can proceed to next day (quiz passed)
     */
    fun canProceedToNextDay(task: ScheduleTask): Boolean {
        val result = task.quizResult ?: return false
        return try {
            val parsed = contentJson.decodeFromString<QuizResult>(result)
            parsed.passed
        } catch (_: Exception) {
            false
        }
    }
}

// ═══════════════════════════════════════════════════════════
// NEW DATA CLASSES
// ═══════════════════════════════════════════════════════════

@Serializable
data class StudyResource(
    val id: String,           // "resource_1", "resource_2", etc.
    val name: String,         // Human-readable title
    val type: String,         // "concept", "example", "diagram_desc", "key_points", "application"
    val content: String,      // Markdown-formatted text content
    @SerialName("estimated_read_time")
    val estimatedReadTime: Int = 3  // Minutes
)

@Serializable
data class FlashCard(
    val id: String,           // "card_1", "card_2", etc.
    val front: String,        // Question side
    val back: String,         // Answer side
    val difficulty: String,   // "easy", "medium", "hard"
    val tags: List<String> = emptyList()
)

@Serializable
data class GatekeeperQuiz(
    @SerialName("quiz_id")
    val quizId: String,
    val title: String,
    val description: String,
    @SerialName("pass_threshold")
    val passThreshold: Float = 0.7f,
    @SerialName("must_pass_to_proceed")
    val mustPassToProceed: Boolean = true,
    @SerialName("max_attempts")
    val maxAttempts: Int = 3,
    @SerialName("time_limit_minutes")
    val timeLimitMinutes: Int = 10,
    val questions: List<QuizQuestion>,
    @SerialName("total_points")
    val totalPoints: Int = 7,
    @SerialName("passing_points")
    val passingPoints: Int = 5,
    @SerialName("feedback_on_fail")
    val feedbackOnFail: String = "",
    @SerialName("feedback_on_pass")
    val feedbackOnPass: String = ""
)

@Serializable
data class QuizQuestion(
    @SerialName("question_id")
    val questionId: String,
    @SerialName("question_text")
    val questionText: String,
    @SerialName("question_type")
    val questionType: String,  // "multiple_choice", "true_false"
    val options: List<QuizOption>,
    @SerialName("correct_answer")
    val correctAnswer: String,  // Option id ("a", "b", "c", "d" or "true"/"false")
    val explanation: String,
    val points: Int = 1,
    val difficulty: String = "medium"
)

@Serializable
data class QuizOption(
    val id: String,
    val text: String
)

@Serializable
data class QuizResult(
    @SerialName("attempt_number")
    val attemptNumber: Int,
    val timestamp: String,
    val passed: Boolean,
    @SerialName("score_percent")
    val scorePercent: Float,
    @SerialName("points_earned")
    val pointsEarned: Int,
    @SerialName("total_points")
    val totalPoints: Int,
    @SerialName("time_taken_seconds")
    val timeTakenSeconds: Int,
    @SerialName("question_results")
    val questionResults: List<QuestionResult>,
    @SerialName("weak_topics")
    val weakTopics: List<String> = emptyList()
)

@Serializable
data class QuestionResult(
    @SerialName("question_id")
    val questionId: String,
    @SerialName("selected_answer")
    val selectedAnswer: String,
    @SerialName("is_correct")
    val isCorrect: Boolean,
    @SerialName("time_spent_seconds")
    val timeSpentSeconds: Int = 0
)
