package com.mursaline.kaironex.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mursaline.kaironex.ui.theme.KaironexColors

enum class KxBadgeVariant {
    Neutral,
    Brand,
    Success,
    Warning,
    Error
}

@Composable
fun KxBadge(
    text: String,
    modifier: Modifier = Modifier,
    variant: KxBadgeVariant = KxBadgeVariant.Neutral
) {
    val (bgColor, contentColor) = when (variant) {
        KxBadgeVariant.Neutral -> KaironexColors.Slate100 to KaironexColors.Slate500
        KxBadgeVariant.Brand -> KaironexColors.Indigo50 to KaironexColors.Indigo600
        KxBadgeVariant.Success -> KaironexColors.Emerald500.copy(alpha=0.1f) to KaironexColors.Emerald500
        KxBadgeVariant.Warning -> Color(0xFFFEF3C7) to Color(0xFFD97706) // Amber
        KxBadgeVariant.Error -> KaironexColors.Rose500.copy(alpha=0.1f) to KaironexColors.Rose500
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
