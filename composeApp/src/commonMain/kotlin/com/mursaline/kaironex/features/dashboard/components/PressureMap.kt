package com.mursaline.kaironex.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    modifier: Modifier = Modifier
) {
    // Structure: Row of 7 Vertical Pills (Rounded Rectangles)
    // Each Pill has 3 dots inside.
    
    Column(modifier = modifier) {
        Text(
            text = "INTENSITY",
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            color = KaironexColors.SlateGray,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 7 Days / Pills
            repeat(7) { index ->
                PressurePill(isActive = index > 2) // Dummy active state
            }
        }
    }
}

@Composable
fun PressurePill(isActive: Boolean) {
    val pillColor = if (isActive) KaironexColors.CloudGray else KaironexColors.CanvasWhite
    val dotColor = if (isActive) KaironexColors.AttentionOrange else KaironexColors.BorderGray
    
    Column(
        modifier = Modifier
            .width(32.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if(isActive) KaironexColors.CanvasWhite else KaironexColors.CloudGray) // Inverted for effect
            .border(1.dp, if(isActive) KaironexColors.BorderGray else Color.Transparent, RoundedCornerShape(16.dp)),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(3) {
             Box(
                 modifier = Modifier
                     .size(6.dp)
                     .clip(CircleShape)
                     .background(if (isActive) KaironexColors.ElectricBlue.copy(alpha=0.6f) else KaironexColors.BorderGray)
             )
        }
    }
}
