package com.mursaline.kaironex.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

enum class KxCardVariant {
    Flat,
    Elevated,
    Outlined,
    High
}

@Composable
fun KxCard(
    modifier: Modifier = Modifier,
    variant: KxCardVariant = KxCardVariant.Elevated,
    shape: Shape = RoundedCornerShape(12.dp),
    backgroundColor: Color = Color.White,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val elevation = when(variant) {
        KxCardVariant.Flat, KxCardVariant.Outlined -> 0.dp
        KxCardVariant.Elevated -> 2.dp
        KxCardVariant.High -> 4.dp
    }

    val border = if (variant == KxCardVariant.Outlined) 
        BorderStroke(1.dp, KaironexColors.Slate100) 
    else null

    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = border,
        onClick = onClick ?: {},
        enabled = onClick != null,
        content = content
    )
}
