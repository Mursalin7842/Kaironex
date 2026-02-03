package com.mursaline.kaironex

actual object PlatformSecrets {
    actual val apiKey: String = BuildConfig.GEMINI_API_KEY
    actual val appwriteEndpoint: String = BuildConfig.APPWRITE_ENDPOINT
    actual val appwriteProject: String = BuildConfig.APPWRITE_PROJECT
    actual val appwriteDatabase: String = BuildConfig.APPWRITE_DATABASE
    actual val appwriteFunctionId: String = BuildConfig.APPWRITE_FUNCTION_ID
    actual val appwriteApiKey: String = BuildConfig.APPWRITE_API_KEY
}
