package com.mursaline.kaironex.core.storage

import com.mursaline.kaironex.agents.genesis.StudentProfile

interface ProfileStorage {
    suspend fun saveProfile(profile: StudentProfile)
    suspend fun loadProfile(): StudentProfile?
    suspend fun clearProfile()
}
