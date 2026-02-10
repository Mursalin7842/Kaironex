package com.mursaline.kaironex.features.resources

import com.mursaline.kaironex.core.AppConfig
import com.mursaline.kaironex.core.KaironexSessionManager
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.github.vinceglb.filekit.core.PlatformFile

import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FileRepository(
    private val httpClient: HttpClient,
    private val sessionManager: KaironexSessionManager,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    private val userId: String
        get() = sessionManager.userId.value ?: "guest"
        
    data class ResourceModel(
        val id: String,
        val title: String,
        val type: String,
        val driveLink: String,
        val summary: String?
    )

    /**
     * Fetch user resources from Appwrite.
     */
    suspend fun fetchResources(): List<ResourceModel> {
        val url = "${AppConfig.Appwrite.ENDPOINT}/databases/${AppConfig.Appwrite.DATABASE_ID}/collections/${AppConfig.Collections.RESOURCES}/documents"
        return try {
            val response = httpClient.get(url) {
                header("X-Appwrite-Project", AppConfig.Appwrite.PROJECT_ID)
                if (AppConfig.Appwrite.API_KEY.isNotBlank()) {
                    header("X-Appwrite-Key", AppConfig.Appwrite.API_KEY)
                }
                parameter("queries[0]", """{"method":"equal","attribute":"userId","values":["$userId"]}""")
                parameter("queries[1]", """{"method":"orderDesc","attribute":"${'$'}createdAt"}""")
            }

            if (response.status.value == 200) {
                val bodyText = response.bodyAsText()
                val root = json.parseToJsonElement(bodyText).jsonObject
                val documents = root["documents"]?.jsonArray ?: return emptyList()

                documents.map { doc ->
                    val obj = doc.jsonObject
                    ResourceModel(
                        id = obj["\$id"]?.jsonPrimitive?.content ?: "",
                        title = obj["title"]?.jsonPrimitive?.content ?: "Untitled",
                        type = "PDF", // Default, could infer from link
                        driveLink = obj["driveLink"]?.jsonPrimitive?.content ?: "",
                        summary = obj["summaryText"]?.jsonPrimitive?.content
                    )
                }
            } else {
                println("❌ Fetch Resources Failed: ${response.status}")
                println("⚠️ Error Body: ${response.bodyAsText()}")
                emptyList()
            }
        } catch (e: Exception) {
            println("❌ Fetch Resources Error: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Upload a file to Appwrite Storage and create a Resource record.
     */
    suspend fun uploadFile(file: PlatformFile): Boolean {
        return try {
            val bytes = file.readBytes()
            val fileName = file.name

            // 1. Upload to Storage
            val storageUrl = "${AppConfig.Appwrite.ENDPOINT}/storage/buckets/${AppConfig.Appwrite.STORAGE_BUCKET_ID}/files"
            
            // Generate a unique ID (or use 'unique()')
            val fileId = "file_${kotlin.random.Random.nextInt(100000, 999999)}"

            val response = httpClient.post(storageUrl) {
                header("X-Appwrite-Project", AppConfig.Appwrite.PROJECT_ID)
                if (AppConfig.Appwrite.API_KEY.isNotBlank()) {
                    header("X-Appwrite-Key", AppConfig.Appwrite.API_KEY)
                }
                
                setBody(MultiPartFormDataContent(
                    formData {
                        append("fileId", "unique()")
                        append("file", bytes, Headers.build {
                            append(HttpHeaders.ContentType, "application/octet-stream")
                            append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                        })
                    }
                ))
            }

            if (response.status.value !in 200..299) {
                println("❌ Storage Upload Failed: ${response.bodyAsText()}")
                return false
            }

            println("✅ File uploaded to Storage")

            // 2. Create Resource Record
            createResourceRecord(
                title = fileName,
                type = "FILE",
                fileId = fileId, // Ideally parse response to get actual ID
                driveUrl = null
            )
        } catch (e: Exception) {
            println("❌ Upload Ex: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Save a Google Drive Link as a Resource.
     */
    suspend fun saveDriveLink(link: String): Boolean {
        return createResourceRecord(
            title = "Google Drive Link",
            type = "DRIVE_LINK",
            fileId = null,
            driveUrl = link
        )
    }

    private suspend fun createResourceRecord(
        title: String,
        type: String,
        fileId: String?,
        driveUrl: String?
    ): Boolean {
        val url = "${AppConfig.Appwrite.ENDPOINT}/databases/${AppConfig.Appwrite.DATABASE_ID}/collections/${AppConfig.Collections.RESOURCES}/documents"
        
            val viewUrl = if (fileId != null) {
                "${AppConfig.Appwrite.ENDPOINT}/storage/buckets/${AppConfig.Appwrite.STORAGE_BUCKET_ID}/files/$fileId/view?project=${AppConfig.Appwrite.PROJECT_ID}"
            } else {
                driveUrl
            }

            val payload = buildJsonObject {
                put("documentId", "unique()")
                put("data", buildJsonObject {
                    put("userId", userId)
                    put("title", title)
                    put("resourceId", fileId ?: "link_${kotlin.random.Random.nextInt(1000, 9999)}")
                    put("driveLink", viewUrl ?: "https://kaironex.com")
                })
            }

        return try {
            val response = httpClient.post(url) {
                header("X-Appwrite-Project", AppConfig.Appwrite.PROJECT_ID)
                if (AppConfig.Appwrite.API_KEY.isNotBlank()) {
                    header("X-Appwrite-Key", AppConfig.Appwrite.API_KEY)
                }
                header("Content-Type", "application/json")
                setBody(payload)
            }
            
            if (response.status.value in 200..299) {
                println("✅ Resource record created for $title")
                true
            } else {
                println("❌ Resource Record Failed: ${response.bodyAsText()}")
                false
            }
        } catch (e: Exception) {
            println("❌ Resource Creation Error: ${e.message}")
            false
        }
    }
}
