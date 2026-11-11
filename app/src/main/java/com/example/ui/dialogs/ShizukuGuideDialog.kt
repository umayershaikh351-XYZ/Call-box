package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shizuku.ShizukuStatus
import com.example.ui.theme.ActiveGreen
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OnPrimaryCyan
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningOrange

@Composable
fun ShizukuGuideDialog(
    status: ShizukuStatus,
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (status == ShizukuStatus.NEEDS_PAIRING) {
                    Button(
                        onClick = {
                            onRequestPermission()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryCyan,
                            contentColor = OnPrimaryCyan
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("shizuku_grant_permission_button")
                    ) {
                        Text("Grant Permission", fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            openShizukuOrStore(context)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_shizuku_app_button")
                    ) {
                        Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Shizuku")
                    }
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("shizuku_guide_close_button")
            ) {
                Text("Close", color = TextSecondary)
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = PrimaryCyan,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Shizuku Setup",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                // Current status banner
                Surface(
                    color = when (status) {
                        ShizukuStatus.ACTIVE -> ActiveGreen.copy(alpha = 0.15f)
                        ShizukuStatus.NEEDS_PAIRING -> WarningOrange.copy(alpha = 0.15f)
                        ShizukuStatus.INACTIVE -> DarkSurfaceVariant
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        when (status) {
                            ShizukuStatus.ACTIVE -> ActiveGreen
                            ShizukuStatus.NEEDS_PAIRING -> WarningOrange
                            ShizukuStatus.INACTIVE -> BorderStroke
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (status) {
                                ShizukuStatus.ACTIVE -> Icons.Default.CheckCircle
                                ShizukuStatus.NEEDS_PAIRING -> Icons.Default.Warning
                                ShizukuStatus.INACTIVE -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = when (status) {
                                ShizukuStatus.ACTIVE -> ActiveGreen
                                ShizukuStatus.NEEDS_PAIRING -> WarningOrange
                                ShizukuStatus.INACTIVE -> TextSecondary
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = when (status) {
                                    ShizukuStatus.ACTIVE -> "Shizuku is Active & Granted"
                                    ShizukuStatus.NEEDS_PAIRING -> "Shizuku Running (Needs Permission)"
                                    ShizukuStatus.INACTIVE -> "Shizuku Not Running / Not Installed"
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = when (status) {
                                    ShizukuStatus.ACTIVE -> "High-fidelity shell audio capture enabled."
                                    ShizukuStatus.NEEDS_PAIRING -> "Tap 'Grant Permission' below to authorize."
                                    ShizukuStatus.INACTIVE -> "Follow steps below or use standard recording mode."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Why Shizuku?",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryCyan,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Shizuku allows Call Box to access system-level VOICE_CALL audio buffers without rooting your device, enabling crystal-clear capture of both sides of VoIP calls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Quick Activation Steps:",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                StepRow(1, "Install Shizuku", "Get Shizuku from GitHub or Google Play Store.")
                StepRow(2, "Enable Wireless Debugging", "In Android Settings > Developer Options, turn on Wireless Debugging.")
                StepRow(3, "Pair & Start", "Open Shizuku, select 'Pairing' with pairing code, then tap 'Start'.")
                StepRow(4, "Authorize Call Box", "Grant Call Box permission in Shizuku's Authorized Applications.")

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Note: Shizuku must be re-started once if your phone reboots. If inactive, Call Box automatically falls back to standard microphone/communication capture.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun StepRow(stepNumber: Int, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(PrimaryCyan.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber.toString(),
                color = PrimaryCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

private fun openShizukuOrStore(context: Context) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        if (launchIntent != null) {
            context.startActivity(launchIntent)
            return
        }
    } catch (ignored: Exception) {}

    try {
        val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(playIntent)
    } catch (e: Exception) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    }
}
