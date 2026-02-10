package com.mursaline.kaironex

import java.io.File
import java.util.Properties

actual object PlatformSecrets {
    private fun loadSecret(sysProp: String, envVar: String, localProp: String, default: String): String {
        return System.getProperty(sysProp)?.takeIf { it.isNotBlank() }
            ?: System.getenv(envVar)?.takeIf { it.isNotBlank() }
            ?: readFromLocalProperties(localProp)
            ?: default
    }

    actual val apiKey: String by lazy {
        loadSecret("GEMINI_API_KEY", "GEMINI_API_KEY", "GeminiAPI", "PLACEHOLDER_API_KEY")
    }

    actual val appwriteEndpoint: String by lazy {
        loadSecret("APPWRITE_ENDPOINT", "APPWRITE_ENDPOINT", "AppwriteEndpoint", "https://nyc.cloud.appwrite.io/v1")
    }

    actual val appwriteProject: String by lazy {
        loadSecret("APPWRITE_PROJECT", "APPWRITE_PROJECT", "AppwriteProject", "")
    }

    actual val appwriteDatabase: String by lazy {
        loadSecret("APPWRITE_DATABASE", "APPWRITE_DATABASE", "AppwriteDatabase", "")
    }

    actual val appwriteFunctionId: String by lazy {
        loadSecret("APPWRITE_FUNCTION_ID", "APPWRITE_FUNCTION_ID", "AppwriteFunctionId", "kaironex-brain")
    }

    actual val appwriteApiKey: String by lazy {
        loadSecret("APPWRITE_API_KEY", "APPWRITE_API_KEY", "AppwriteApiKey", "")
    }

    private fun readFromLocalProperties(keyName: String): String? {
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
                val value = props.getProperty(keyName)
                if (!value.isNullOrBlank()) {
                    // println("✅ $keyName loaded from ${propsFile.absolutePath}") // valid for debugging but maybe noisy
                    value
                } else null
            } else {
                // println("⚠️ local.properties not found")
                null
            }
        } catch (e: Exception) {
            println("❌ Error reading local.properties: ${e.message}")
            null
        }
    }
}
