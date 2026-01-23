package com.mursaline.kaironex.ui.theme

import androidx.compose.ui.graphics.Color

object KaironexColors {
    // Backgrounds
    val CanvasWhite = Color(0xFFFFFFFF) // Pure White
    val CloudGray = Color(0xFFF8FAFD) // Off-white for Desktop Shell
    
    // Primary Accents (The Brain)
    val GeminiBlurple = Color(0xFF65558F) // AI thoughts, Generate buttons
    val ElectricBlue = Color(0xFF0061A4)  // Primary Nav, CTAs
    
    // Functional Colors
    val SuccessGreen = Color(0xFF1E8E3E) // In Flow, Safe
    val AttentionOrange = Color(0xFFE37400) // Drift, Low Energy
    val AlertRed = Color(0xFFB3261E) // Recall, Gatekeeper Failed
    
    // Text
    val InkBlack = Color(0xFF1F1F1F) // Primary Headings
    val SlateGray = Color(0xFF444746) // Body text, subtitles
    
    // Borders & Dividers
    val BorderGray = Color(0xFFE0E2E7)
    
    // Legacy mapping helpers (to avoid breaking existing code immediately, will map to new scheme)
    // We will refactor usages gradually, but for now map old names to new vibe or keep duplicates if needed for strict compatibility
    
    // Mapping old Slate/Indigo to new palette for backward compat during refactor:
    val Slate900 = InkBlack
    val Slate800 = InkBlack
    val Slate500 = SlateGray
    val Slate100 = CloudGray
    val Slate50 = CanvasWhite // Was background
    
    val Indigo50 = Color(0xFFF3F6FC) // Map to CloudGray equivalentish
    val Indigo200 = Color(0xFFD0BCFF) // Light Purple/Blurple
    val Indigo500 = GeminiBlurple
    val Indigo600 = GeminiBlurple
    val Indigo900 = Color(0xFF381E72) // Darker Blurple
    
    val Emerald500 = SuccessGreen
    val Rose500 = AlertRed
    val Purple600 = GeminiBlurple
    
    // Gradients (Mapped to new scheme)
    val IndigoGradientStart = GeminiBlurple.copy(alpha=0.9f)
    val IndigoGradientEnd = InkBlack.copy(alpha=0.95f)
}
