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
        val id: String,
        val title: String,
        val description: String?,
        val startTime: String,
        val endTime: String,
        val status: String,
        val type: String,
        val priority: Int,
        val isFlexible: Boolean,
        val topics: String?,
        val linkedDeadline: String?
    )

    suspend fun fetchSchedule(limit: Int = 1000): List<ScheduleTask> {
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
                val documents = root["documents"]?.jsonArray ?: return emptyList()

                documents.map { doc ->
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
                        linkedDeadline = obj["linked_deadline"]?.jsonPrimitive?.content
                    )
                }
            } else {
                println("❌ Fetch Schedule Failed: ${response.status}")
                println("⚠️ Error Body: ${response.bodyAsText()}")
                emptyList()
            }
        } catch (e: Exception) {
            println("❌ Fetch Schedule Error: ${e.message}")
            emptyList()
        }
    }
}
