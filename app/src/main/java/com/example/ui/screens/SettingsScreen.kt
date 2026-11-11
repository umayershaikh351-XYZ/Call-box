package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwitchAccessShortcut
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CallBoxSettings
import com.example.detector.CallDetector
import com.example.shizuku.ShizukuStatus
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.BorderStroke
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
import java.io.File

@Composable
fun SettingsScreen(
    settings: CallBoxSettings,
    shizukuStatus: ShizukuStatus,
    recordingsDir: File,
    showDeleteAllConfirmation: Boolean,
    onOpenShizukuGuide: () -> Unit,
    onSetAutoRecord: (String, Boolean) -> Unit,
    onSetAudioBitrate: (Int) -> Unit,
    onSetFloatingOverlay: (Boolean) -> Unit,
    onShowDeleteAllDialog: (Boolean) -> Unit,
    onConfirmDeleteAll: () -> Unit
) {
    val context = LocalContext.current
    val hasUsageStats = CallDetector.hasUsageStatsPermission(context)
    val hasOverlayPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION: SHIZUKU
        SettingsSectionHeader(title = "Shizuku Shell Engine", icon = Icons.Default.Terminal)

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (shizukuStatus) {
                                            ShizukuStatus.ACTIVE -> ActiveGreen
                                            ShizukuStatus.NEEDS_PAIRING -> WarningOrange
                                            ShizukuStatus.INACTIVE -> TextMuted
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (shizukuStatus) {
                                    ShizukuStatus.ACTIVE -> "Status: Active & Linked"
                                    ShizukuStatus.NEEDS_PAIRING -> "Status: Needs Authorization"
                                    ShizukuStatus.INACTIVE -> "Status: Inactive"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Shizuku must be re-activated after every reboot.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Button(
                        onClick = onOpenShizukuGuide,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryCyan,
                            contentColor = OnPrimaryCyan
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("settings_shizuku_guide_button")
                    ) {
                        Text(
                            text = if (shizukuStatus == ShizukuStatus.ACTIVE) "Manage" else "Activate",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION: AUTO RECORD PER-APP
        SettingsSectionHeader(title = "Auto-Record Calling Apps", icon = Icons.Default.Phone)

        if (!hasUsageStats) {
            Surface(
                color = WarningOrange.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WarningOrange),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Usage Access Required",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Needed to detect which VoIP app is currently in a call.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningOrange, contentColor = DarkBackground),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                AutoRecordToggleItem("Phone Calls", "Cellular & Carrier calls", settings.autoRecordPhone) {
                    onSetAutoRecord("phone", it)
                }
                AutoRecordToggleItem("WhatsApp & WhatsApp Business", "Voice & video calls", settings.autoRecordWhatsapp) {
                    onSetAutoRecord("whatsapp", it)
                }
                AutoRecordToggleItem("Telegram", "P2P voice & group calls", settings.autoRecordTelegram) {
                    onSetAutoRecord("telegram", it)
                }
                AutoRecordToggleItem("Instagram", "Direct voice & video calls", settings.autoRecordInstagram) {
                    onSetAutoRecord("instagram", it)
                }
                AutoRecordToggleItem("Google Meet", "Meetings & video calls", settings.autoRecordMeet) {
                    onSetAutoRecord("meet", it)
                }
                AutoRecordToggleItem("IMO Messenger", "IMO voice & video calls", settings.autoRecordImo) {
                    onSetAutoRecord("imo", it)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION: AUDIO QUALITY
        SettingsSectionHeader(title = "Audio Quality (AAC-LC)", icon = Icons.Default.Mic)

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AudioQualityOption(
                    title = "Low — 64 kbps",
                    subtitle = "Minimal storage footprint, clear speech (~28 MB/hour)",
                    bitrate = 64000,
                    selectedBitrate = settings.audioBitrate,
                    onSelect = onSetAudioBitrate
                )
                Spacer(modifier = Modifier.height(10.dp))
                AudioQualityOption(
                    title = "Medium — 96 kbps (Recommended)",
                    subtitle = "Balanced speech clarity & efficient storage (~42 MB/hour)",
                    bitrate = 96000,
                    selectedBitrate = settings.audioBitrate,
                    onSelect = onSetAudioBitrate
                )
                Spacer(modifier = Modifier.height(10.dp))
                AudioQualityOption(
                    title = "High — 128 kbps",
                    subtitle = "Studio speech fidelity, captures quiet background audio (~56 MB/hour)",
                    bitrate = 128000,
                    selectedBitrate = settings.audioBitrate,
                    onSelect = onSetAudioBitrate
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION: FLOATING OVERLAY
        SettingsSectionHeader(title = "Overlay Pill", icon = Icons.Default.SwitchAccessShortcut)

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Floating Indicator Pill",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Shows '● Call Box is recording — tap to stop' over other apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = settings.floatingOverlayEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasOverlayPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                context.startActivity(Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                            }
                            onSetFloatingOverlay(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryCyan,
                            checkedTrackColor = PrimaryCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION: STORAGE & CLEANUP
        SettingsSectionHeader(title = "Storage & Privacy", icon = Icons.Default.Folder)

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Storage Directory:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Text(
                    text = recordingsDir.absolutePath,
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryCyan,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { onShowDeleteAllDialog(true) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RecordingRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RecordingRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_all_recordings_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RecordingRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete All Recordings", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION: PRIVACY & LEGAL
        SettingsSectionHeader(title = "Privacy & Legal Notice", icon = Icons.Default.Gavel)

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = ActiveGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "100% Offline & Private",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ActiveGreen
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "No analytics, no telemetry, no cloud accounts, no network calls. Recordings stay strictly inside app sandbox.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Consent & Recording Laws",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = WarningOrange
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Do not share recordings without consent of all parties. Recording laws vary by jurisdiction.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SECTION: ABOUT
        SettingsSectionHeader(title = "About Call Box", icon = Icons.Default.Info)

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Call Box v1.0",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "\"Every call, kept quietly.\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryCyan,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Personal sideload call recorder powered by Shizuku shell audio capture, AAC-LC encoding, and on-device Room persistence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }

    // Delete All Confirmation Dialog
    if (showDeleteAllConfirmation) {
        AlertDialog(
            onDismissRequest = { onShowDeleteAllDialog(false) },
            confirmButton = {
                Button(
                    onClick = onConfirmDeleteAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RecordingRed,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { onShowDeleteAllDialog(false) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            title = {
                Text(
                    text = "Delete All Recordings?",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all audio files and metadata stored in Call Box. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = PrimaryCyan,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun AutoRecordToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PrimaryCyan,
                checkedTrackColor = PrimaryCyan.copy(alpha = 0.3f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}

@Composable
private fun AudioQualityOption(
    title: String,
    subtitle: String,
    bitrate: Int,
    selectedBitrate: Int,
    onSelect: (Int) -> Unit
) {
    val isSelected = bitrate == selectedBitrate

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(bitrate) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) PrimaryCyan else TextMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) PrimaryCyan else TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
