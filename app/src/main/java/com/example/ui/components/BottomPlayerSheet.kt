package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Recording
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OnPrimaryCyan
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.RecordingRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomPlayerSheet(
    recording: Recording,
    isPlaying: Boolean,
    currentPositionMs: Int,
    totalDurationMs: Int,
    playbackSpeed: Float,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val maxDuration = if (totalDurationMs > 0) totalDurationMs else (recording.durationMs.toInt().coerceAtLeast(1000))
    val currentClamped = currentPositionMs.coerceIn(0, maxDuration)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = PrimaryCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = recording.appLabel.take(2).uppercase(),
                            color = PrimaryCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.fileName.removeSuffix(".m4a"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${recording.appLabel} • ${formatDate(recording.timestamp)} • ${formatFileSize(recording.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier.testTag("player_star_button")
                ) {
                    Icon(
                        imageVector = if (recording.isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Star",
                        tint = if (recording.isStarred) WarningOrange else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Waveform simulation bar visualizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val barCount = 30
                val progressRatio = if (maxDuration > 0) currentClamped.toFloat() / maxDuration else 0f
                val activeBarIndex = (progressRatio * barCount).toInt()

                for (i in 0 until barCount) {
                    // Generate pseudo-random deterministic heights for bars
                    val factor = ((i * 17 + 7) % 24) + 6
                    val isActive = i <= activeBarIndex
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(factor.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isActive) PrimaryCyan else DarkSurfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrubber Slider
            Slider(
                value = currentClamped.toFloat(),
                onValueChange = { onSeekTo(it.toInt()) },
                valueRange = 0f..maxDuration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryCyan,
                    activeTrackColor = PrimaryCyan,
                    inactiveTrackColor = DarkSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_seek_slider")
            )

            // Time stamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatDuration(currentClamped.toLong()),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = "-${formatDuration((maxDuration - currentClamped).toLong())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transport Controls: Rewind 10, Play/Pause, Forward 10
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSeekTo((currentClamped - 10000).coerceAtLeast(0)) },
                    modifier = Modifier.testTag("player_rewind_10_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                FilledIconButton(
                    onClick = onTogglePlayPause,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = PrimaryCyan,
                        contentColor = OnPrimaryCyan
                    ),
                    modifier = Modifier
                        .size(64.dp)
                        .testTag("player_play_pause_button")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                IconButton(
                    onClick = { onSeekTo((currentClamped + 10000).coerceAtMost(maxDuration)) },
                    modifier = Modifier.testTag("player_forward_10_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        tint = TextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speed selector pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf(1.0f, 1.25f, 1.5f).forEach { speed ->
                    FilterChip(
                        selected = playbackSpeed == speed,
                        onClick = { onSetSpeed(speed) },
                        label = { Text("${speed}x", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryCyan.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryCyan
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action row: Share, Rename, Delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground, RoundedCornerShape(14.dp))
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.testTag("player_share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = PrimaryCyan)
                }

                IconButton(
                    onClick = onRename,
                    modifier = Modifier.testTag("player_rename_button")
                ) {
                    Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Rename", tint = TextPrimary)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("player_delete_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RecordingRed)
                }
            }
        }
    }
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%.0f KB", kb)
    }
}

fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
}
