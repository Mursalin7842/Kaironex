package com.mursaline.kaironex.core.storage

import android.content.Context
import com.mursaline.kaironex.agents.genesis.StudentProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class AndroidProfileStorage(
    private val context: Context
) : ProfileStorage {

    private val fileName = "student_profile.json"
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    override suspend fun saveProfile(profile: StudentProfile) {
        withContext(Dispatchers.IO) {
            try {
                val jsonString = json.encodeToString(profile)
                context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                    it.write(jsonString.toByteArray())
                }
                println("💾 Android: Profile saved to $fileName")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun loadProfile(): StudentProfile? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, fileName)
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
                context.deleteFile(fileName)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
