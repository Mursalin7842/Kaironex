package com.mursaline.kaironex.ui.theme

import androidx.compose.ui.graphics.Color

object VitalityColors {
    // Defcon Level Colors
    val defcon1Survival = Color(0xFFD32F2F)      // Deep Red - Critical
    val defcon2Critical = Color(0xFFF57C00)      // Orange - Warning
    val defcon3Caution = Color(0xFFFBC02D)       // Yellow - Caution
    val defcon4Stable = Color(0xFF388E3C)        // Green - Stable
    val defcon5Abundance = Color(0xFF1976D2)     // Blue - Abundance
    
    // Gradient backgrounds per defcon
    val defcon1Gradient = listOf(Color(0xFFD32F2F), Color(0xFF8B0000))
    val defcon2Gradient = listOf(Color(0xFFF57C00), Color(0xFFE65100))
    val defcon3Gradient = listOf(Color(0xFFFBC02D), Color(0xFFF9A825))
    val defcon4Gradient = listOf(Color(0xFF388E3C), Color(0xFF2E7D32))
    val defcon5Gradient = listOf(Color(0xFF1976D2), Color(0xFF1565C0))
    
    // Meal Decision Colors
    val cookGreen = Color(0xFF4CAF50)
    val orderOrange = Color(0xFFFF9800)
    val emergencyRed = Color(0xFFE53935)
    val convenienceBlue = Color(0xFF2196F3)
    
    // Victory Feast - Celebration Gold
    val victoryGold = Color(0xFFFFD700)
    val victoryGradient = listOf(Color(0xFFFFD700), Color(0xFFFFA000))
}
