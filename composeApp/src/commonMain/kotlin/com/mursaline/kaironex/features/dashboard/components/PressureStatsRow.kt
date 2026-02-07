package com.mursaline.kaironex.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

@Composable
fun PressureStatsRow(
    dailyPressure: Float,   // 0.0 - 1.0
    weeklyPressure: Float,  // 0.0 - 1.0
    monthlyPressure: Float, // 0.0 - 1.0
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PressureStatCard(
            title = "Today",
            pressure = dailyPressure,
            modifier = Modifier.weight(1f)
        )
        PressureStatCard(
            title = "This Week",
            pressure = weeklyPressure,
            modifier = Modifier.weight(1f)
        )
        PressureStatCard(
            title = "This Month",
            pressure = monthlyPressure,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PressureStatCard(
    title: String,
    pressure: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = KaironexColors.SlateGray,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Circular Indicator or Bar
            // Using a vertical bar for "Graph" feel as requested
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .height(60.dp)
                    .width(12.dp)
                    .background(KaironexColors.CloudGray, RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(pressure.coerceIn(0.1f, 1f)) // Min height for visibility
                        .background(
                            color = getPressureColor(pressure),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "${(pressure * 100).toInt()}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = KaironexColors.InkBlack
            )
        }
    }
}

private fun getPressureColor(value: Float): Color {
    return when {
        value < 0.3f -> KaironexColors.SuccessGreen
        value < 0.7f -> KaironexColors.EventsOrange
        else -> KaironexColors.ErrorRed
    }
}
