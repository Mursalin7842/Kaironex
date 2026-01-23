package com.mursaline.kaironex.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

enum class KxButtonVariant {
    Primary,
    Secondary,
    Ghost,
    Outline
}

@Composable
fun KxButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    variant: KxButtonVariant = KxButtonVariant.Primary,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(24.dp),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit = {
        if (text != null) Text(text)
    }
) {
    val height = 48.dp

    when (variant) {
        KxButtonVariant.Primary -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(height),
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KaironexColors.Indigo600,
                    contentColor = Color.White,
                    disabledContainerColor = KaironexColors.Slate100,
                    disabledContentColor = KaironexColors.Slate500
                ),
                contentPadding = contentPadding,
                interactionSource = interactionSource,
                content = content
            )
        }
        KxButtonVariant.Secondary -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(height),
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KaironexColors.Indigo50,
                    contentColor = KaironexColors.Indigo600,
                    disabledContainerColor = KaironexColors.Slate50,
                    disabledContentColor = KaironexColors.Slate500
                ),
                contentPadding = contentPadding,
                interactionSource = interactionSource,
                content = content
            )
        }
        KxButtonVariant.Outline -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.height(height),
                enabled = enabled,
                shape = shape,
                border = BorderStroke(1.dp, KaironexColors.Slate500),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = KaironexColors.Slate900
                ),
                contentPadding = contentPadding,
                interactionSource = interactionSource,
                content = content
            )
        }
        KxButtonVariant.Ghost -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.height(height),
                enabled = enabled,
                shape = shape,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = KaironexColors.Slate500
                ),
                contentPadding = contentPadding,
                interactionSource = interactionSource,
                content = content
            )
        }
    }
}
