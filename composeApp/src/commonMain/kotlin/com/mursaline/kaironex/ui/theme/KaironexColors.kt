package com.mursaline.kaironex.ui.theme

import androidx.compose.ui.graphics.Color

object KaironexColors {
    val Slate900 = Color(0xFF0F172A)
    val Slate800 = Color(0xFF1E293B)
    val Slate500 = Color(0xFF64748B)
    val Slate100 = Color(0xFFF1F5F9)
    val Slate50 = Color(0xFFF8FAFC)
    val Indigo50 = Color(0xFFEEF2FF)
    val Indigo200 = Color(0xFFC7D2FE)
    val Indigo500 = Color(0xFF6366F1)
    val Indigo600 = Color(0xFF4F46E5)
    val Indigo900 = Color(0xFF312E81)
    val Emerald500 = Color(0xFF10B981)
    val Rose500 = Color(0xFFF43F5E)
    val Purple600 = Color(0xFF9333EA)
    
    // Gradients
    val IndigoGradientStart = Indigo900.copy(alpha=0.8f)
    val IndigoGradientEnd = Slate900.copy(alpha=0.9f)
}
