package com.mursaline.kaironex.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

enum class KxToastVariant {
    Neutral,
    Success,
    Error,
    Warning
}

@Composable
fun KxToastHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(16.dp)
    ) { data ->
        // Determine variant based on message content or custom action label if possible.
        // For simplicity, we might assume the actionLabel prefix encodes the variant, 
        // or just default to Neutral since SnackbarData is limited.
        // A better approach often involves a custom SnackbarVisuals implementation, 
        // but for now we'll stick to a styled default Snackbar.
        
        val variant = when(data.visuals.actionLabel) {
            "Error" -> KxToastVariant.Error
            "Success" -> KxToastVariant.Success
            "Warning" -> KxToastVariant.Warning
            else -> KxToastVariant.Neutral
        }
        
        val (containerColor, contentColor) = when(variant) {
            KxToastVariant.Neutral -> KaironexColors.Slate800 to Color.White
            KxToastVariant.Success -> KaironexColors.Emerald500 to Color.White
            KxToastVariant.Error -> KaironexColors.Rose500 to Color.White
            KxToastVariant.Warning -> Color(0xFFD97706) to Color.White // Amber
        }

        Snackbar(
            modifier = Modifier.padding(12.dp),
            containerColor = containerColor,
            contentColor = contentColor,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            action = {
                data.visuals.actionLabel?.let { label ->
                    if (label !in listOf("Error", "Success", "Warning", "Info")) {
                        androidx.compose.material3.TextButton(
                            onClick = { data.performAction() }
                        ) {
                             Text(label, color = contentColor, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) {
            Text(data.visuals.message, fontWeight = FontWeight.Medium)
        }
    }
}
