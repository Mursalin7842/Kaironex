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
    val Slate700 = Color(0xFF374151) // Adding missing color
    val Slate600 = Color(0xFF4B5563) // Adding missing color
    val Slate500 = SlateGray
    val Slate400 = Color(0xFF9CA3AF) // Adding missing color
    val Slate300 = Color(0xFFD1D5DB) // Adding missing color
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
    val Amber500 = AttentionOrange // For streaks and warnings

    // Gradients (Mapped to new scheme)
    val IndigoGradientStart = GeminiBlurple.copy(alpha=0.9f)
    val IndigoGradientEnd = InkBlack.copy(alpha=0.95f)

    // Genesis / Iron Man Palette
    val NeonCyan = Color(0xFF00FFFF)
    val NeonPurple = Color(0xFFD000FF)
    val NeonGreen = Color(0xFF39FF14)
    val BackgroundBlack = Color(0xFF101010) // Deep black for OLED visuals
}
