package com.mursaline.kaironex.features.dashboard.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

@Composable
fun SettingsScreen() {
    Column {
        Text("General Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = KaironexColors.Slate900)
        Spacer(Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Column {
                SettingsItem("Enable Voice Mode", "Allow the Reasoning Engine to speak", true)
                HorizontalDivider(color = KaironexColors.Slate100)
                SettingsItem("Strict Lockdown", "Block all browsers during focus time", false)
                HorizontalDivider(color = KaironexColors.Slate100)
                SettingsItem("Desktop Monitoring", "Allow window title tracking", true)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text("Account", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = KaironexColors.Slate900)
        Spacer(Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Column {
                // Simplified items
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Profile Status", color = KaironexColors.Slate900)
                    Text("Active Student", color = KaironexColors.Indigo600, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, initialValue: Boolean) {
    var checked by remember { mutableStateOf(initialValue) }
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = KaironexColors.Slate900)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = KaironexColors.Slate500)
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked, 
            onCheckedChange = { checked = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = KaironexColors.Indigo600,
                uncheckedThumbColor = KaironexColors.Slate500,
                uncheckedTrackColor = KaironexColors.Slate100,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
