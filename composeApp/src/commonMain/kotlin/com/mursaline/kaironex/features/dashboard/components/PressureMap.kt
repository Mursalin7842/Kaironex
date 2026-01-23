package com.mursaline.kaironex.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

@Composable
fun PressureMap(
    modifier: Modifier = Modifier,
    isMobile: Boolean = false
) {
    val pillWidth = if (isMobile) 24.dp else 32.dp
    val pillHeight = if (isMobile) 60.dp else 80.dp
    val dotSize = if (isMobile) 4.dp else 6.dp

    Column(modifier = modifier.padding(if (isMobile) 8.dp else 0.dp)) {
        Text(
            text = "INTENSITY",
            style = MaterialTheme.typography.labelSmall,
            color = KaironexColors.SlateGray,
            modifier = Modifier.padding(bottom = if (isMobile) 8.dp else 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 7 Days / Pills
            repeat(7) { index ->
                PressurePill(
                    isActive = index > 2,
                    pillWidth = pillWidth,
                    pillHeight = pillHeight,
                    dotSize = dotSize
                )
            }
        }

        // Day labels for mobile
        if (isMobile) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        color = KaironexColors.SlateGray
                    )
                }
            }
        }
    }
}

@Composable
fun PressurePill(
    isActive: Boolean,
    pillWidth: androidx.compose.ui.unit.Dp = 32.dp,
    pillHeight: androidx.compose.ui.unit.Dp = 80.dp,
    dotSize: androidx.compose.ui.unit.Dp = 6.dp
) {
    Column(
        modifier = Modifier
            .width(pillWidth)
            .height(pillHeight)
            .clip(RoundedCornerShape(pillWidth / 2))
            .background(if(isActive) KaironexColors.CanvasWhite else KaironexColors.CloudGray)
            .border(1.dp, if(isActive) KaironexColors.BorderGray else Color.Transparent, RoundedCornerShape(pillWidth / 2)),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(3) {
             Box(
                 modifier = Modifier
                     .size(dotSize)
                     .clip(CircleShape)
                     .background(if (isActive) KaironexColors.ElectricBlue.copy(alpha=0.6f) else KaironexColors.BorderGray)
             )
        }
    }
}
