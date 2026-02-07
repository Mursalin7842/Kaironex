package com.mursaline.kaironex.features.study

import com.mursaline.kaironex.core.AppConfig
import com.mursaline.kaironex.core.KaironexSessionManager
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull

class ScheduleRepository(
    private val httpClient: HttpClient,
    private val sessionManager: KaironexSessionManager,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    private val userId: String
        get() = sessionManager.userId.value ?: "guest"

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
        val taskId: String? = null,
        val userId: String? = null,
        val location: String? = null
    )

    private var cachedSchedule: List<ScheduleTask> = emptyList()

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
                            id = obj["\$id"]?.jsonPrimitive?.content ?: "",
                            title = obj["title"]?.jsonPrimitive?.content ?: "Untitled Task",
                            description = obj["description"]?.jsonPrimitive?.content,
                            startTime = obj["startTime"]?.jsonPrimitive?.content ?: "",
                            endTime = obj["endTime"]?.jsonPrimitive?.content ?: "",
                            status = obj["status"]?.jsonPrimitive?.content ?: "pending",
                            type = obj["type"]?.jsonPrimitive?.content ?: "study",
                            priority = obj["priority"]?.jsonPrimitive?.content?.toIntOrNull() ?: 5,
                            isFlexible = obj["is_flexible"]?.jsonPrimitive?.booleanOrNull ?: false,
                            topics = obj["topics"]?.jsonPrimitive?.content,
                            linkedDeadline = obj["linked_deadline"]?.jsonPrimitive?.content,
                            // NEW: Rich task metadata
                            subject = obj["subject"]?.jsonPrimitive?.content,
                            difficulty = obj["difficulty"]?.jsonPrimitive?.content,
                            contentMode = obj["content_mode"]?.jsonPrimitive?.content,
                            metadataJson = obj["metadata_json"]?.jsonPrimitive?.content,
                            // Schema Update
                            taskId = obj["taskId"]?.jsonPrimitive?.content,
                            userId = obj["userId"]?.jsonPrimitive?.content,
                            location = obj["location"]?.jsonPrimitive?.content
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
}
