package com.mursaline.kaironex.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object VitalityTypography {
    // Budget Amount - Large, Bold, Mono
    val budgetAmount = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        letterSpacing = (-2).sp
    )
    
    // Defcon Label
    val defconLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 14.sp,
        letterSpacing = 2.sp
    )
    
    // Voice Message (AI personality)
    val voiceMessage = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        fontStyle = FontStyle.Italic,
        lineHeight = 24.sp
    )
}
