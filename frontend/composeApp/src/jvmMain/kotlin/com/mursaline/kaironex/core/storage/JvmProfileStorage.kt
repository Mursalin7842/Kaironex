package com.mursaline.kaironex.core.storage

import com.mursaline.kaironex.agents.genesis.StudentProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class JvmProfileStorage : ProfileStorage {

    private val storageDir = File(System.getProperty("user.home"), ".kaironex")
    private val fileName = "student_profile.json"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    init {
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
    }

    override suspend fun saveProfile(profile: StudentProfile) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(storageDir, fileName)
                val jsonString = json.encodeToString(profile)
                file.writeText(jsonString)
                println("💾 JVM: Profile saved to ${file.absolutePath}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun loadProfile(): StudentProfile? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(storageDir, fileName)
                if (!file.exists()) return@withContext null
                
                val jsonString = file.readText()
                json.decodeFromString<StudentProfile>(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    override suspend fun clearProfile() {
        withContext(Dispatchers.IO) {
            try {
                val file = File(storageDir, fileName)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
