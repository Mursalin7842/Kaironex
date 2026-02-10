package com.mursaline.kaironex

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// 1. The Data Object (The "Brain State")
@Serializable
data class CloudState(
    val pressure: Float,
    val status: String,
    val activeApp: String,
    val lastUpdated: Long
)

// 2. The Network Manager
object FirebaseSync {
    // !!! REPLACE THIS WITH YOUR REAL FIREBASE URL !!!
    // It must end with a slash '/'
    private const val FIREBASE_URL = "https://kaironex-236c5-default-rtdb.firebaseio.com/"

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    // Function 1: Desktop uses this to PUSH status
    suspend fun updateState(pressure: Float, status: String, appName: String) {
        val payload = CloudState(
            pressure = pressure,
            status = status,
            activeApp = appName,
            lastUpdated = System.currentTimeMillis()
        )

        try {
            // We PUT to overwrite the data at "student_state.json"
            client.put(FIREBASE_URL + "student_state.json") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        } catch (e: Exception) {
            println("Sync Failed: ${e.message}")
        }
    }

    // Function 2: Android uses this to READ status
    suspend fun getState(): CloudState? {
        return try {
            val response = client.get(FIREBASE_URL + "student_state.json")
            response.body<CloudState>()
        } catch (e: Exception) {
            println("Read Failed: ${e.message}")
            null
        }
    }
}