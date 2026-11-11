package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Recording
import com.example.shizuku.ShizukuStatus
import com.example.ui.CallBoxStats
import com.example.ui.components.formatDate
import com.example.ui.components.formatDuration
import com.example.ui.components.formatFileSize
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.ColorGoogleMeet
import com.example.ui.theme.ColorInstagram
import com.example.ui.theme.ColorPhone
import com.example.ui.theme.ColorTelegram
import com.example.ui.theme.ColorWhatsApp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OnPrimaryCyan
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.RecordingRed
import com.example.ui.theme.RecordingRedGlow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningOrange

@Composable
fun HomeScreen(
    shizukuStatus: ShizukuStatus,
    isRecording: Boolean,
    durationMs: Long,
    amplitude: Float,
    activeAppLabel: String,
    stats: CallBoxStats,
    recentRecordings: List<Recording>,
    onToggleRecording: () -> Unit,
    onOpenShizukuGuide: () -> Unit,
    onPlayRecording: (Recording) -> Unit,
    onShareRecording: (Recording) -> Unit,
    onDeleteRecording: (Recording) -> Unit,
    onNavigateToRecordings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp)
            .testTag("home_screen_column")
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // App Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = PrimaryCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = PrimaryCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Call Box",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    Text(
                        text = "Every call, kept quietly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Shizuku Status Card
            ShizukuStatusCard(
                status = shizukuStatus,
                onActionClick = onOpenShizukuGuide
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Central Recording Orb
            CentralRecordingOrb(
                isRecording = isRecording,
                durationMs = durationMs,
                amplitude = amplitude,
                activeAppLabel = activeAppLabel,
                onToggleRecording = onToggleRecording
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Quick Stats Row
            QuickStatsRow(stats = stats)

            Spacer(modifier = Modifier.height(28.dp))

            // Recent Recordings Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Recordings",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (recentRecordings.isNotEmpty()) {
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelMedium,
                        color = PrimaryCyan,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onNavigateToRecordings() }
                            .padding(4.dp)
                            .testTag("home_view_all_recordings_button")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (recentRecordings.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = DarkSurfaceVariant,
                            shape = CircleShape,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No recordings yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap the central orb or start a call in WhatsApp, Phone, or Telegram.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(recentRecordings, key = { it.id }) { recording ->
                RecentRecordingItem(
                    recording = recording,
                    onPlay = { onPlayRecording(recording) },
                    onShare = { onShareRecording(recording) },
                    onDelete = { onDeleteRecording(recording) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
private fun ShizukuStatusCard(
    status: ShizukuStatus,
    onActionClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shizuku_status_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Status dot
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (status) {
                                ShizukuStatus.ACTIVE -> ActiveGreen
                                ShizukuStatus.NEEDS_PAIRING -> WarningOrange
                                ShizukuStatus.INACTIVE -> TextMuted
                            }
                        )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Shizuku Engine: ",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = when (status) {
                                ShizukuStatus.ACTIVE -> "ACTIVE"
                                ShizukuStatus.NEEDS_PAIRING -> "NEEDS PERMISSION"
                                ShizukuStatus.INACTIVE -> "INACTIVE"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (status) {
                                ShizukuStatus.ACTIVE -> ActiveGreen
                                ShizukuStatus.NEEDS_PAIRING -> WarningOrange
                                ShizukuStatus.INACTIVE -> TextSecondary
                            }
                        )
                    }
                    Text(
                        text = when (status) {
                            ShizukuStatus.ACTIVE -> "System VOICE_CALL stream active"
                            ShizukuStatus.NEEDS_PAIRING -> "Pairing required to tap shell stream"
                            ShizukuStatus.INACTIVE -> "Using standard microphone capture"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (status == ShizukuStatus.ACTIVE) DarkSurfaceVariant else PrimaryCyan,
                    contentColor = if (status == ShizukuStatus.ACTIVE) TextPrimary else OnPrimaryCyan
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("shizuku_cta_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (status) {
                        ShizukuStatus.ACTIVE -> "Status"
                        ShizukuStatus.NEEDS_PAIRING -> "Authorize"
                        ShizukuStatus.INACTIVE -> "Activate"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CentralRecordingOrb(
    isRecording: Boolean,
    durationMs: Long,
    amplitude: Float,
    activeAppLabel: String,
    onToggleRecording: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isRecording) 1.15f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isRecording) 600 else 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode pill
        Surface(
            color = if (isRecording) RecordingRed.copy(alpha = 0.2f) else DarkSurfaceVariant,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isRecording) RecordingRed else BorderStroke
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) RecordingRed else PrimaryCyan)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRecording) "RECORDING: $activeAppLabel" else "CAPTURE READY",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isRecording) RecordingRed else PrimaryCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Orb Container
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer Halo
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        if (isRecording) {
                            Brush.radialGradient(
                                listOf(RecordingRedGlow, Color.Transparent)
                            )
                        } else {
                            Brush.radialGradient(
                                listOf(PrimaryCyan.copy(alpha = 0.15f), Color.Transparent)
                            )
                        }
                    )
            )

            // Dynamic Amplitude Ring
            if (isRecording) {
                val ampScale = 1.0f + (amplitude * 0.25f)
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .scale(ampScale)
                        .clip(CircleShape)
                        .border(2.dp, RecordingRed.copy(alpha = 0.5f), CircleShape)
                )
            }

            // Main 180dp Orb
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRecording) {
                            Brush.linearGradient(
                                listOf(Color(0xFFFF5252), Color(0xFFD32F2F))
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(Color(0xFF00E5C7), Color(0xFF00BFA5))
                            )
                        }
                    )
                    .clickable { onToggleRecording() }
                    .testTag("central_recording_orb"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                        tint = if (isRecording) Color.White else OnPrimaryCyan,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isRecording) "STOP" else "RECORD",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isRecording) Color.White else OnPrimaryCyan,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timer
        Text(
            text = formatDuration(durationMs),
            style = MaterialTheme.typography.displaySmall,
            color = if (isRecording) RecordingRed else TextPrimary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.testTag("home_timer_text")
        )

        Text(
            text = if (isRecording) "Tap orb to stop and finalize audio" else "Tap orb or receive VoIP call to start",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun QuickStatsRow(stats: CallBoxStats) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = stats.totalCount.toString(),
                label = "Recordings"
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(BorderStroke)
            )
            StatItem(
                value = formatFileSize(stats.totalSizeBytes),
                label = "Stored"
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(BorderStroke)
            )
            StatItem(
                value = "${(stats.totalDurationMs / 60000)}m",
                label = "Total Time"
            )
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryCyan
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun RecentRecordingItem(
    recording: Recording,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val appColor = getAppBadgeColor(recording.appPackage)

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("recent_recording_item_${recording.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App badge
            Surface(
                color = appColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = recording.appLabel.take(2).uppercase(),
                        color = appColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.fileName.removeSuffix(".m4a"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${recording.appLabel} • ${formatDate(recording.timestamp)} • ${formatDuration(recording.durationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onPlay) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = PrimaryCyan)
            }

            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

fun getAppBadgeColor(appPackage: String): Color {
    return when {
        appPackage.contains("whatsapp", ignoreCase = true) -> ColorWhatsApp
        appPackage.contains("telegram", ignoreCase = true) -> ColorTelegram
        appPackage.contains("instagram", ignoreCase = true) -> ColorInstagram
        appPackage.contains("meet", ignoreCase = true) || appPackage.contains("tachyon", ignoreCase = true) -> ColorGoogleMeet
        appPackage.contains("phone", ignoreCase = true) -> ColorPhone
        else -> PrimaryCyan
    }
}
