package com.mursaline.kaironex.features.auth

import kotlinx.coroutines.delay

interface AuthRepository {
    suspend fun login(email: String, passing: String): Result<Boolean>
    suspend fun signup(name: String, email: String, passing: String): Result<Boolean>
    suspend fun judgeLogin(accessCode: String): Result<Boolean>
}

class MockAuthRepository : AuthRepository {
    override suspend fun login(email: String, passing: String): Result<Boolean> {
        delay(1000) // Simulate network
        return if (email.isNotEmpty() && passing.isNotEmpty()) {
            Result.success(true)
        } else {
            Result.failure(Exception("Invalid credentials"))
        }
    }

    override suspend fun signup(name: String, email: String, passing: String): Result<Boolean> {
        delay(1500)
        return Result.success(true)
    }

    override suspend fun judgeLogin(accessCode: String): Result<Boolean> {
        delay(500)
        return if (accessCode == "0000") {
            Result.success(true)
        } else {
            Result.failure(Exception("Access Denied"))
        }
    }
}
