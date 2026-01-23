package com.mursaline.kaironex.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

enum class KxIconButtonVariant {
    Ghost,
    Filled,
    Outline
}

@Composable
fun KxIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: KxIconButtonVariant = KxIconButtonVariant.Ghost,
    contentDescription: String? = null,
    enabled: Boolean = true
) {
    val colors = when (variant) {
        KxIconButtonVariant.Ghost -> IconButtonDefaults.iconButtonColors(
            contentColor = KaironexColors.Slate500,
            containerColor = Color.Transparent
        )
        KxIconButtonVariant.Filled -> IconButtonDefaults.filledIconButtonColors(
            containerColor = KaironexColors.Indigo50,
            contentColor = KaironexColors.Indigo600
        )
        KxIconButtonVariant.Outline -> IconButtonDefaults.outlinedIconButtonColors(
            contentColor = KaironexColors.Slate900
        )
    }

    // TODO: For Outline variant, we usually need 'OutlinedIconButton', but standard IconButton with border modifier works too. 
    // For now, mapping Outline to standard with border logic could be complex if we stick to just `IconButton`. 
    // Material3 has `OutlinedIconButton`.
    
    if (variant == KxIconButtonVariant.Outline) {
         androidx.compose.material3.OutlinedIconButton(
            onClick = onClick,
            modifier = modifier.size(40.dp),
            enabled = enabled,
            shape = CircleShape,
            colors = colors,
            border = androidx.compose.foundation.BorderStroke(1.dp, KaironexColors.Slate100)
        ) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
        }
    } else {
        IconButton(
            onClick = onClick,
            modifier = modifier.size(40.dp), // Standard touch target
            enabled = enabled,
            colors = colors
        ) {
            Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
        }
    }
}
