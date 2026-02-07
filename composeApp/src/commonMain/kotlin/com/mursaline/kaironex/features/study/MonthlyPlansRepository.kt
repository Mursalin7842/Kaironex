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

class MonthlyPlansRepository(
    private val httpClient: HttpClient,
    private val sessionManager: KaironexSessionManager,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    private val userId: String
        get() = sessionManager.userId.value ?: "guest"

    @Serializable
    data class MonthlyPlan(
        val id: String,
        val userId: String,
        val monthIndex: Int,
        val startDate: String,
        val endDate: String,
        val status: String,
        val goalsContext: String?,
        val achievedContext: String?,
        val missedContext: String?
    )

    suspend fun fetchMonthlyPlans(): List<MonthlyPlan> {
        val url = "${AppConfig.Appwrite.ENDPOINT}/databases/${AppConfig.Appwrite.DATABASE_ID}/collections/${AppConfig.Collections.MONTHLY_PLANS}/documents"
        
        return try {
            val response = httpClient.get(url) {
                header("X-Appwrite-Project", AppConfig.Appwrite.PROJECT_ID)
                if (AppConfig.Appwrite.API_KEY.isNotBlank()) {
                    header("X-Appwrite-Key", AppConfig.Appwrite.API_KEY)
                }
                parameter("queries[0]", """{"method":"equal","attribute":"userId","values":["$userId"]}""")
                parameter("queries[1]", """{"method":"orderAsc","attribute":"month_index"}""")
            }

            if (response.status.value == 200) {
                val bodyText = response.bodyAsText()
                val root = json.parseToJsonElement(bodyText).jsonObject
                val documents = root["documents"]?.jsonArray ?: return emptyList()

                documents.map { doc ->
                    val obj = doc.jsonObject
                    MonthlyPlan(
                        id = obj["\$id"]?.jsonPrimitive?.content ?: "",
                        userId = obj["userId"]?.jsonPrimitive?.content ?: "",
                        monthIndex = obj["month_index"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                        startDate = obj["start_date"]?.jsonPrimitive?.content ?: "",
                        endDate = obj["end_date"]?.jsonPrimitive?.content ?: "",
                        status = obj["status"]?.jsonPrimitive?.content ?: "planned",
                        goalsContext = obj["goals_context"]?.jsonPrimitive?.content,
                        achievedContext = obj["achieved_context"]?.jsonPrimitive?.content,
                        missedContext = obj["missed_context"]?.jsonPrimitive?.content
                    )
                }
            } else {
                println("❌ Fetch Monthly Plans Failed: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            println("❌ Fetch Monthly Plans Error: ${e.message}")
            emptyList()
        }
    }
}
