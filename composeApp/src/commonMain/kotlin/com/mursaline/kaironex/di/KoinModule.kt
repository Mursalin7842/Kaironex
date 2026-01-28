package com.mursaline.kaironex.di

import com.mursaline.kaironex.brain.GeminiReasoningEngine
import com.mursaline.kaironex.features.auth.AuthRepository
import com.mursaline.kaironex.features.auth.MockAuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

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
                pingInterval = 20_000
            }
        }
    }

    // 2. The Brain (Gemini Wrapper)
    single { GeminiReasoningEngine(get(), get(), get()) }

    // 3. Authentication
    single<AuthRepository> { MockAuthRepository() }
    
    // 4. ViewModels
    factory { com.mursaline.kaironex.features.dashboard.ProfileCalibrationViewModel(get()) }
}
