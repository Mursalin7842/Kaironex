package com.mursaline.kaironex

actual object PlatformSecrets {
    actual val apiKey: String = System.getProperty("GEMINI_API_KEY") 
        ?: System.getenv("GEMINI_API_KEY") 
        ?: "PLACEHOLDER_FOR_DESKTOP"
}
