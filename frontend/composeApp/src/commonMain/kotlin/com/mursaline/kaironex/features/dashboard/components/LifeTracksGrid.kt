package com.mursaline.kaironex.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.features.zones.LifeTrack
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * Life Tracks Grid - The "Defense System" that handles life
 *
 * 3-Zone Architecture:
 * - CAMPAIGN (Career & Growth) 🚀
 * - VITALITY (Food & Finance) ⚡
 * - RADIUS   (Habitat & Culture) 📡
 *
 * These agents run in the background while the student focuses on studying in the Cortex.
 */
@Composable
fun LifeTracksGrid(
    isMobile: Boolean,
    onTrackClick: (LifeTrack) -> Unit = {}
) {
    if (isMobile) {
        // Mobile: Vertical stack with compact cards
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LifeTrack.entries.forEach { track ->
                LifeTrackCard(
                    track = track,
                    onClick = { onTrackClick(track) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    isCompact = true
                )
            }
        }
    } else {
        // Desktop: 3-column layout for 3 tracks
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LifeTrack.entries.forEach { track ->
                LifeTrackCard(
                    track = track,
                    onClick = { onTrackClick(track) },
                    modifier = Modifier.weight(1f).height(160.dp)
                )
            }
        }
    }
}

/**
 * Individual Life Track Card
 * Shows the track's purpose and status
 */
@Composable
fun LifeTrackCard(
    track: LifeTrack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = KaironexColors.CanvasWhite,
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Accent gradient on the left edge
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                track.color,
                                track.color.copy(alpha = 0.6f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 12.dp, top = if (isCompact) 12.dp else 16.dp, bottom = if (isCompact) 12.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji Icon Container
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 44.dp else 56.dp)
                        .clip(CircleShape)
                        .background(track.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = track.emoji,
                        style = if (isCompact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Text Content
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = if (isCompact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KaironexColors.InkBlack,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = track.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = track.color,
                        fontWeight = FontWeight.Medium
                    )

                    if (!isCompact) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = track.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = KaironexColors.SlateGray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status indicator
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    // Status badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = KaironexColors.Emerald500.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(KaironexColors.Emerald500)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = KaironexColors.Emerald500,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (!isCompact) {
                        Spacer(Modifier.height(8.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open",
                            tint = KaironexColors.SlateGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact horizontal scrolling view for Life Tracks (alternative layout)
 */
@Composable
fun LifeTracksRow(
    onTrackClick: (LifeTrack) -> Unit = {}
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        LifeTrack.entries.forEach { track ->
            LifeTrackMiniCard(
                track = track,
                onClick = { onTrackClick(track) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun LifeTrackMiniCard(
    track: LifeTrack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = track.color.copy(alpha = 0.1f),
        modifier = modifier.height(80.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.emoji,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = track.title.split(" ").first(), // Just first word
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = track.color,
                maxLines = 1
            )
        }
    }
}
