package com.mursaline.kaironex.features.dashboard.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

@Composable
fun AnalyticsScreen() {
    Column {
         Text("Weekly Performance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = KaironexColors.Slate900)
         Spacer(Modifier.height(16.dp))
         
         // Bar Chart Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth().height(300.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Focus Hours", fontWeight = FontWeight.SemiBold, color = KaironexColors.Slate900)
                    Text("Last 7 Days", style = MaterialTheme.typography.bodySmall, color = KaironexColors.Slate500)
                }
                
                Spacer(Modifier.height(24.dp))
                
                // Bars
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val data = listOf(0.4f, 0.7f, 0.3f, 0.8f, 0.6f, 0.9f, 0.5f)
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    
                    data.forEachIndexed { index, value ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .fillMaxHeight(value)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (value > 0.6f) KaironexColors.Indigo600 else KaironexColors.Indigo200)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(days[index], style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate500)
                        }
                    }
                }
            }
        }
    }
}
