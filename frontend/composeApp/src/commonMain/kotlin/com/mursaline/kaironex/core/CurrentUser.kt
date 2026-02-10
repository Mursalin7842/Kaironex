package com.mursaline.kaironex.core

/**
 * 🔑 CURRENT USER STATE
 * =======================
 * Holds the authenticated user's identity across the app.
 * Set during login/signup, read by SessionManager and all screens.
 *
 * In production: This would be backed by Appwrite's Account service.
 * For the hackathon: Derives a stable userId from the user's email.
 */
object CurrentUser {
    var userId: String = "demo_user_001"
        private set
    var email: String = ""
        private set
    var displayName: String = "Demo Student"
        private set
    var isAuthenticated: Boolean = true
        private set

    /**
     * Initialize the current user session.
     * Derives a deterministic Appwrite-compatible documentId from the email.
     */
    fun login(email: String, name: String = "") {
        this.email = email
        this.displayName = name.ifBlank { email.substringBefore("@") }
        // Create a deterministic userId from email — Appwrite doc IDs must be <= 36 chars
        this.userId = deriveUserId(email)
        this.isAuthenticated = true
    }

    /**
     * Set a specific userId (for judge demo / returning users).
     */
    fun loginWithId(userId: String, name: String = "Student") {
        this.userId = userId
        this.displayName = name
        this.email = ""
        this.isAuthenticated = true
    }

    fun logout() {
        userId = ""
        email = ""
        displayName = ""
        isAuthenticated = false
    }

    /**
     * Derive a stable, Appwrite-compatible userId from email.
     * Uses a simple hash to create a reproducible 20-char hex string.
     */
    private fun deriveUserId(email: String): String {
        val normalized = email.trim().lowercase()
        // Simple deterministic hash — same email always gives same ID
        var hash = 0L
        for (ch in normalized) {
            hash = hash * 31 + ch.code
        }
        return "user_${hash.toUInt().toString(16).padStart(8, '0')}"
    }
}
