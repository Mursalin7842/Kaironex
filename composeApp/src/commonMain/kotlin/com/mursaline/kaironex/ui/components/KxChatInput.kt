package com.mursaline.kaironex.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

@Composable
fun KxChatInput(
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cancel Button
        IconButton(onClick = onCancel) {
            Icon(Icons.Default.Close, "Cancel", tint = Color.Gray)
        }

        // Text Field
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Type to Kairo...", color = Color.Gray) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White, // Add this
                cursorColor = KaironexColors.NeonCyan,
                focusedBorderColor = KaironexColors.NeonCyan,
                unfocusedBorderColor = Color.Gray
            ),
            singleLine = true
        )

        // Send Button
        IconButton(
            onClick = {
                if (text.isNotBlank()) {
                    onSend(text)
                    text = ""
                }
            },
            enabled = text.isNotBlank()
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                "Send",
                tint = if (text.isNotBlank()) KaironexColors.NeonCyan else Color.Gray
            )
        }
    }
}
