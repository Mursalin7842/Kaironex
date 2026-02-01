package com.mursaline.kaironex

import java.io.File
import java.util.Properties

actual object PlatformSecrets {
    actual val apiKey: String by lazy {
        // Try multiple sources for the API key

        // 1. JVM System Property (from Gradle jvmArgs)
        System.getProperty("GEMINI_API_KEY")?.takeIf { it.isNotBlank() }

        // 2. Environment Variable
        ?: System.getenv("GEMINI_API_KEY")?.takeIf { it.isNotBlank() }

        // 3. Read from local.properties file directly
        ?: readFromLocalProperties()

        // 4. Fallback placeholder
        ?: "PLACEHOLDER_FOR_DESKTOP"
    }

    private fun readFromLocalProperties(): String? {
        return try {
            val workDir = File(System.getProperty("user.dir"))

            // Try to find local.properties in the project root
            val candidates = listOf(
                File(workDir, "local.properties"),
                File(workDir.parentFile, "local.properties"),
                File(workDir, "../local.properties")
            )

            val propsFile = candidates.firstOrNull { it.exists() }

            if (propsFile != null) {
                val props = Properties()
                propsFile.inputStream().use { props.load(it) }
                val key = props.getProperty("GeminiAPI")
                if (!key.isNullOrBlank()) {
                    println("✅ API Key loaded from ${propsFile.absolutePath}")
                    key
                } else null
            } else {
                println("⚠️ local.properties not found in: ${candidates.map { it.absolutePath }}")
                null
            }
        } catch (e: Exception) {
            println("❌ Error reading local.properties: ${e.message}")
            null
        }
    }
}
