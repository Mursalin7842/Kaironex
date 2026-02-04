package com.mursaline.kaironex.di

import com.mursaline.kaironex.brain.BrainApiClient

import com.mursaline.kaironex.brain.ReflexAgent
import com.mursaline.kaironex.core.AppConfig
import com.mursaline.kaironex.core.stats.AppwriteStatsRepository
import com.mursaline.kaironex.features.auth.AuthRepository
import com.mursaline.kaironex.features.auth.MockAuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

import com.mursaline.kaironex.core.KaironexSessionManager

/**
 * Kaironex App Koin Module
 *
 * Provides:
 * - Network client (Ktor)
 * - Brain API client (Backend connection)
 * - Gemini Reasoning Engine (Live voice)
 * - Reflex Agent (Fast local responses)
 * - Stats Repository (Real data from Appwrite)
 * - Authentication
 * - ViewModels
 */
val appModule = module {
    includes(platformModule)

    // 1. Network Client (The Ears/Mouth base)
    single {
        HttpClient(io.ktor.client.engine.cio.CIO) {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                })
            }
            install(io.ktor.client.plugins.websocket.WebSockets) {
                pingIntervalMillis = AppConfig.Timing.WS_PING_INTERVAL_MS
            }
            install(io.ktor.client.plugins.HttpTimeout) {
                requestTimeoutMillis = 60_000 // 60s for Deep Brain Reasoning
                connectTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }
        }
    }

    // 2. Brain API Client (Backend Connection)
    single {
        BrainApiClient(
            client = get()
        )
    }



    // 4. Reflex Agent (Fast local responses)
    factory { (userId: String) ->
        ReflexAgent(
            brainClient = get(),
            userId = userId
        )
    }

    // 5. Stats Repository (Real data from Appwrite)
    // 5. Stats Repository (Real data from Appwrite)
    factory { 
        val sessionManager = get<KaironexSessionManager>()
        // Delegate to session manager which holds the active user's repo
        sessionManager.getStatsRepository() ?: AppwriteStatsRepository(
            brainClient = get(),
            httpClient = get(),
            userId = "uninitialized_user" 
        )
    }

    // 6. Authentication
    single<AuthRepository> { MockAuthRepository() }
    
    // 7. ViewModels
    // 7. ViewModels
    factory { com.mursaline.kaironex.features.dashboard.ProfileCalibrationViewModel(get(), get()) }
    factory { com.mursaline.kaironex.features.voice.VoiceViewModel(get(), get()) }
    factory { com.mursaline.kaironex.features.study.StudyViewModel(get()) }


    // 8. Wake Word Service (Removed)
    // single { WakeWordService(get()) }

    
    // Voice Agent System
    single { com.mursaline.kaironex.brain.AppwriteBridge(get()) }
    single { com.mursaline.kaironex.brain.GeminiLiveAgent(get(), get(), get()) }

    // 9. Session Manager (Central Brain)
    single { KaironexSessionManager(get(), get()) }

    // 10. Resources Repository
    single { com.mursaline.kaironex.features.resources.FileRepository(get(), get()) }
    
    // 7. ViewModels

}

