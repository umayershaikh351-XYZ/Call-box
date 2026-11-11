package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.CallBoxViewModel
import com.example.ui.components.BottomPlayerSheet
import com.example.ui.dialogs.LegalDisclaimerDialog
import com.example.ui.dialogs.RenameDialog
import com.example.ui.dialogs.ShizukuGuideDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RecordingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.CallBoxTheme
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OnPrimaryCyan
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

enum class Screen(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    RECORDINGS("Recordings", Icons.Filled.Folder, Icons.Outlined.Folder),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: CallBoxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CallBoxTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainContent(viewModel: CallBoxViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    // Request necessary runtime permissions on first composition
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.checkShizuku()
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    // State collections
    val shizukuStatus by viewModel.shizukuStatus.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val recordingAppLabel by viewModel.recordingAppLabel.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val recentRecordings by viewModel.recentRecordings.collectAsState()
    val recordingsList by viewModel.recordingsList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    val currentPlayingRecording by viewModel.currentPlayingRecording.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val totalPlayerDurationMs by viewModel.totalPlayerDurationMs.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()

    val settings by viewModel.settings.collectAsState()
    val showShizukuGuide by viewModel.showShizukuGuide.collectAsState()
    val recordingToRename by viewModel.recordingToRename.collectAsState()
    val showDeleteAllConfirmation by viewModel.showDeleteAllConfirmation.collectAsState()

    // Handle back press if not on Home screen
    BackHandler(enabled = currentScreen != Screen.HOME) {
        currentScreen = Screen.HOME
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .testTag("callbox_bottom_navigation")
            ) {
                Screen.values().forEach { screen ->
                    val selected = currentScreen == screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = OnPrimaryCyan,
                            selectedTextColor = PrimaryCyan,
                            indicatorColor = PrimaryCyan,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            when (currentScreen) {
                Screen.HOME -> {
                    HomeScreen(
                        shizukuStatus = shizukuStatus,
                        isRecording = isRecording,
                        durationMs = durationMs,
                        amplitude = amplitude,
                        activeAppLabel = recordingAppLabel,
                        stats = stats,
                        recentRecordings = recentRecordings,
                        onToggleRecording = { viewModel.toggleManualRecording() },
                        onOpenShizukuGuide = { viewModel.showShizukuGuide.value = true },
                        onPlayRecording = { viewModel.playRecording(it) },
                        onShareRecording = { viewModel.shareRecording(it) },
                        onDeleteRecording = { viewModel.deleteRecording(it) },
                        onNavigateToRecordings = { currentScreen = Screen.RECORDINGS }
                    )
                }
                Screen.RECORDINGS -> {
                    RecordingsScreen(
                        recordings = recordingsList,
                        searchQuery = searchQuery,
                        selectedFilter = selectedFilter,
                        onSearchQueryChange = { viewModel.searchQuery.value = it },
                        onFilterChange = { viewModel.selectedFilter.value = it },
                        onPlayRecording = { viewModel.playRecording(it) },
                        onShareRecording = { viewModel.shareRecording(it) },
                        onRenameRecording = { viewModel.recordingToRename.value = it },
                        onToggleStar = { viewModel.toggleStar(it) },
                        onDeleteRecording = { viewModel.deleteRecording(it) }
                    )
                }
                Screen.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        shizukuStatus = shizukuStatus,
                        recordingsDir = viewModel.recordingRepository.getRecordingsDir(),
                        showDeleteAllConfirmation = showDeleteAllConfirmation,
                        onOpenShizukuGuide = { viewModel.showShizukuGuide.value = true },
                        onSetAutoRecord = { key, enabled -> viewModel.setAutoRecord(key, enabled) },
                        onSetAudioBitrate = { viewModel.setAudioBitrate(it) },
                        onSetFloatingOverlay = { viewModel.setFloatingOverlayEnabled(it) },
                        onShowDeleteAllDialog = { viewModel.showDeleteAllConfirmation.value = it },
                        onConfirmDeleteAll = { viewModel.deleteAllRecordings() }
                    )
                }
            }
        }
    }

    // First-launch legal acknowledgment dialog
    if (!settings.hasSeenDisclaimer) {
        LegalDisclaimerDialog(
            onAcknowledge = { viewModel.setHasSeenDisclaimer(true) }
        )
    }

    // Shizuku setup guide modal
    if (showShizukuGuide) {
        ShizukuGuideDialog(
            status = shizukuStatus,
            onRequestPermission = { viewModel.requestShizukuPermission() },
            onDismiss = { viewModel.showShizukuGuide.value = false }
        )
    }

    // Rename recording dialog
    recordingToRename?.let { recording ->
        RenameDialog(
            recording = recording,
            onConfirm = { newName -> viewModel.renameRecording(recording, newName) },
            onDismiss = { viewModel.recordingToRename.value = null }
        )
    }

    // Bottom sheet inline audio player
    currentPlayingRecording?.let { recording ->
        BottomPlayerSheet(
            recording = recording,
            isPlaying = isPlaying,
            currentPositionMs = currentPositionMs,
            totalDurationMs = totalPlayerDurationMs,
            playbackSpeed = playbackSpeed,
            onTogglePlayPause = { viewModel.togglePlayPause() },
            onSeekTo = { viewModel.seekTo(it) },
            onSetSpeed = { viewModel.setSpeed(it) },
            onShare = { viewModel.shareRecording(recording) },
            onRename = { viewModel.recordingToRename.value = recording },
            onToggleStar = { viewModel.toggleStar(recording) },
            onDelete = {
                viewModel.deleteRecording(recording)
                viewModel.closePlayer()
            },
            onDismiss = { viewModel.closePlayer() }
        )
    }
}
