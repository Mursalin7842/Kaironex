package com.mursaline.kaironex.features.study.viewers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mursaline.kaironex.ui.theme.KaironexColors

/**
 * ============================================================
 * IN-APP YOUTUBE PLAYER (PLACEHOLDER)
 * ============================================================
 *
 * Controlled YouTube player with:
 * - No external navigation
 * - Controlled playlist
 * - No sidebar recommendations (uses youtube-nocookie.com)
 * - Playback tracking for Proof-of-Work
 *
 * TODO: Implement actual WebView with youtube-nocookie.com/embed/
 */

@Composable
fun InAppYouTubePlayer(
    videoId: String,
    videoTitle: String,
    onWatchProgress: (Float) -> Unit = {},
    onVideoComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var currentTime by remember { mutableStateOf("0:00") }
    var totalTime by remember { mutableStateOf("12:34") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = KaironexColors.InkBlack,
        shadowElevation = 8.dp
    ) {
        Column {
            // Video Area (Placeholder)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "YouTube Player",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Video ID: $videoId",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(24.dp))

                    // Implementation note
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = KaironexColors.AttentionOrange.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "🚧 WebView Implementation Pending\nWill use youtube-nocookie.com/embed/",
                            style = MaterialTheme.typography.labelSmall,
                            color = KaironexColors.AttentionOrange,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Play/Pause overlay button
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(80.dp)
                ) {
                    // Transparent click target over the icon
                }
            }

            // Controls Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Video Title
                Text(
                    text = videoTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )

                Spacer(Modifier.height(8.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color.Red,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )

                Spacer(Modifier.height(8.dp))

                // Time & Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time display
                    Text(
                        text = "$currentTime / $totalTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    // Control buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playback speed
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "1x",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { /* Fullscreen */ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Proof of Work indicator
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = KaironexColors.SuccessGreen.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = "Tracking",
                        tint = KaironexColors.SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Watch time tracked for Proof-of-Work",
                        style = MaterialTheme.typography.labelSmall,
                        color = KaironexColors.SuccessGreen
                    )
                }
            }
        }
    }
}

/**
 * Playlist view for controlled video navigation
 */
@Composable
fun YouTubePlaylistCard(
    videos: List<VideoItem>,
    currentIndex: Int,
    onVideoSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = KaironexColors.CanvasWhite
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Playlist",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = KaironexColors.InkBlack
            )
            Spacer(Modifier.height(8.dp))

            videos.forEachIndexed { index, video ->
                PlaylistItem(
                    video = video,
                    isPlaying = index == currentIndex,
                    onClick = { onVideoSelect(index) }
                )
                if (index < videos.size - 1) {
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun PlaylistItem(
    video: VideoItem,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isPlaying) KaironexColors.ElectricBlue.copy(alpha = 0.1f)
               else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .size(48.dp, 32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(KaironexColors.SlateGray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = KaironexColors.SlateGray,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                    color = if (isPlaying) KaironexColors.ElectricBlue else KaironexColors.InkBlack,
                    maxLines = 1
                )
                Text(
                    text = video.duration,
                    style = MaterialTheme.typography.labelSmall,
                    color = KaironexColors.SlateGray
                )
            }

            if (video.watched) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Watched",
                    tint = KaironexColors.SuccessGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

data class VideoItem(
    val id: String,
    val title: String,
    val duration: String,
    val watched: Boolean = false
)
