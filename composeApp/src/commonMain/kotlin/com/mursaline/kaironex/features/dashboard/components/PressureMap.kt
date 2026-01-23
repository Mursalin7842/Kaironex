package com.mursaline.kaironex.features.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

@Composable
fun PressureMap(
    modifier: Modifier = Modifier
) {
    // A visual representation of pressure/stress/focus across different subjects/zones.
    // Placeholder implementation using Canvas to draw a "Radar" or "Heat" map style graphic.
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 * 0.8f
            
            // Draw concentrics
            drawCircle(
                color = KaironexColors.Slate100,
                radius = radius,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = KaironexColors.Slate100,
                radius = radius * 0.6f,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = KaironexColors.Slate100,
                radius = radius * 0.3f,
                style = Stroke(width = 1.dp.toPx())
            )
            
            // Draw generic data points (Blob)
            // This is just aesthetic for now to look "High Tech"
            val path = Path().apply {
                moveTo(center.x, center.y - radius * 0.7f) // Top
                lineTo(center.x + radius * 0.6f, center.y - radius * 0.2f) // Top Right
                lineTo(center.x + radius * 0.5f, center.y + radius * 0.5f) // Bottom Right
                lineTo(center.x - radius * 0.4f, center.y + radius * 0.6f) // Bottom Left
                lineTo(center.x - radius * 0.7f, center.y - radius * 0.1f) // Top Left
                close()
            }
            
            drawPath(
                path = path,
                color = KaironexColors.Indigo500.copy(alpha = 0.3f),
            )
            drawPath(
                path = path,
                color = KaironexColors.Indigo600,
                style = Stroke(width = 2.dp.toPx())
            )
            
            // Axis lines
            drawLine(
                color = KaironexColors.Slate100,
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = KaironexColors.Slate100,
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        Text(
            text = "ACADEMIC PRESSURE",
            color = KaironexColors.Slate500,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall
        )
    }
}
