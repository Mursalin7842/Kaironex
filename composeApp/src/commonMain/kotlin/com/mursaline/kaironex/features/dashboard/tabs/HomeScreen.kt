package com.mursaline.kaironex.features.dashboard.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.sp
import com.mursaline.kaironex.ui.theme.KaironexColors

@Composable
fun HomeScreen() {
    @Suppress("UnusedBoxWithConstraintsScope")
    BoxWithConstraints {
        val isMobile = this.maxWidth < 600.dp

        Column {
            // Stats Row - Stack vertically on mobile, side-by-side on desktop
            if (isMobile) {
                // Mobile: Stack cards vertically
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FocusScoreCard(modifier = Modifier.fillMaxWidth().height(160.dp))
                    QuickStatusCard(modifier = Modifier.fillMaxWidth().height(140.dp))
                }
            } else {
                // Desktop: Side by side
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FocusScoreCard(modifier = Modifier.weight(1f).height(180.dp))
                    QuickStatusCard(modifier = Modifier.weight(1f).height(180.dp))
                }
            }

            Text("Recent Activity", style = MaterialTheme.typography.titleMedium, color = KaironexColors.Slate900, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            // Activity List
            repeat(3) {
                ActivityItem()
            }
        }
    }
}

@Composable
private fun FocusScoreCard(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Circular background
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(6.dp, KaironexColors.Slate100, CircleShape)
                )
                // Progress (Static for now)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(6.dp, KaironexColors.Emerald500, CircleShape)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("85", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = KaironexColors.Slate900)
                    Text("SCORE", style = MaterialTheme.typography.labelSmall, color = KaironexColors.Slate500)
                }
            }
        }
    }
}

@Composable
private fun QuickStatusCard(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = KaironexColors.Indigo600,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(KaironexColors.Emerald500, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text("System Active", color = KaironexColors.Indigo200, style = MaterialTheme.typography.labelMedium)
            }

            Column {
                Text("You are doing great!", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("No distractions detected.", color = KaironexColors.Indigo50.copy(alpha=0.8f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ActivityItem() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(shape = CircleShape, color = KaironexColors.Slate100, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CheckCircle, null, tint = KaironexColors.Slate500, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Deep Work Session", fontWeight = FontWeight.SemiBold, color = KaironexColors.Slate900, style = MaterialTheme.typography.bodyMedium)
                    Text("2 hours • High Focus", style = MaterialTheme.typography.bodySmall, color = KaironexColors.Slate500)
                }
            }
            Text("+120 pts", fontWeight = FontWeight.Bold, color = KaironexColors.Emerald500, style = MaterialTheme.typography.bodySmall)
        }
    }
}
