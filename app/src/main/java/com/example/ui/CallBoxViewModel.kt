package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlaybackManager
import com.example.data.CallBoxDatabase
import com.example.data.CallBoxSettings
import com.example.data.RecordingRepository
import com.example.data.SettingsRepository
import com.example.detector.CallDetector
import com.example.model.Recording
import com.example.service.FloatingOverlayService
import com.example.service.RecordingService
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CallBoxStats(
    val totalCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val totalDurationMs: Long = 0L
)

class CallBoxViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()
    private val database = CallBoxDatabase.getInstance(context)
    val recordingRepository = RecordingRepository(context, database.recordingDao())
    val settingsRepository = SettingsRepository(context)
    val shizukuManager = ShizukuManager(context)
    val playbackManager = AudioPlaybackManager(context)

    // Shizuku State
    val shizukuStatus: StateFlow<ShizukuStatus> = shizukuManager.status

    // Settings State
    val settings: StateFlow<CallBoxSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = CallBoxSettings()
    )

    // Recording State
    val isRecording: StateFlow<Boolean> = RecordingService.isServiceRecording
    val durationMs: StateFlow<Long> = RecordingService.recordingDurationMs
    val amplitude: StateFlow<Float> = RecordingService.recordingAmplitude
    val recordingAppLabel: StateFlow<String> = RecordingService.recordingAppLabel

    // Recordings Filter & Search
    val searchQuery = MutableStateFlow("")
    val selectedFilter = MutableStateFlow("All")

    // Recordings list combined with search and filter
    val recordingsList: StateFlow<List<Recording>> = combine(
        recordingRepository.allRecordings,
        searchQuery,
        selectedFilter
    ) { all, query, filter ->
        all.filter { recording ->
            val matchesQuery = query.isBlank() ||
                    recording.fileName.contains(query, ignoreCase = true) ||
                    recording.appLabel.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "All" -> true
                "Starred" -> recording.isStarred
                "WhatsApp" -> recording.appPackage.contains("whatsapp", ignoreCase = true) || recording.appLabel.contains("whatsapp", ignoreCase = true)
                "Instagram" -> recording.appPackage.contains("instagram", ignoreCase = true) || recording.appLabel.contains("instagram", ignoreCase = true)
                "Telegram" -> recording.appPackage.contains("telegram", ignoreCase = true) || recording.appLabel.contains("telegram", ignoreCase = true)
                "Phone" -> recording.appPackage.contains("phone", ignoreCase = true) || recording.appLabel.contains("phone", ignoreCase = true)
                "Google Meet" -> recording.appPackage.contains("meet", ignoreCase = true) || recording.appPackage.contains("tachyon", ignoreCase = true) || recording.appLabel.contains("meet", ignoreCase = true)
                else -> !recording.appPackage.contains("whatsapp", ignoreCase = true) &&
                        !recording.appPackage.contains("instagram", ignoreCase = true) &&
                        !recording.appPackage.contains("telegram", ignoreCase = true) &&
                        !recording.appPackage.contains("phone", ignoreCase = true) &&
                        !recording.appPackage.contains("meet", ignoreCase = true)
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    // Recent 5 recordings
    val recentRecordings: StateFlow<List<Recording>> = recordingRepository.recentRecordings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    // Quick Stats
    val stats: StateFlow<CallBoxStats> = combine(
        recordingRepository.totalCount,
        recordingRepository.totalSizeBytes,
        recordingRepository.totalDurationMs
    ) { count, size, duration ->
        CallBoxStats(
            totalCount = count,
            totalSizeBytes = size,
            totalDurationMs = duration
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = CallBoxStats()
    )

    // Playback state forwarded from playbackManager
    val currentPlayingRecording: StateFlow<Recording?> = playbackManager.currentRecording
    val isPlaying: StateFlow<Boolean> = playbackManager.isPlaying
    val currentPositionMs: StateFlow<Int> = playbackManager.currentPositionMs
    val totalPlayerDurationMs: StateFlow<Int> = playbackManager.totalDurationMs
    val playbackSpeed: StateFlow<Float> = playbackManager.playbackSpeed

    // Dialog states
    val showShizukuGuide = MutableStateFlow(false)
    val showDisclaimer = MutableStateFlow(false)
    val recordingToRename = MutableStateFlow<Recording?>(null)
    val showDeleteAllConfirmation = MutableStateFlow(false)

    // Call detector for VoIP and phone calls
    private var callDetector: CallDetector? = null

    init {
        checkShizuku()
        setupCallDetector()
    }

    private fun setupCallDetector() {
        callDetector = CallDetector(
            context = context,
            onCallStarted = { appPackage, appLabel, settingKey ->
                handleAutoCallDetected(appPackage, appLabel, settingKey)
            },
            onCallEnded = {
                handleAutoCallEnded()
            }
        )
        callDetector?.startListening()
    }

    private fun handleAutoCallDetected(appPackage: String, appLabel: String, settingKey: String) {
        val currentSettings = settings.value
        val isEnabled = when (settingKey) {
            "whatsapp" -> currentSettings.autoRecordWhatsapp
            "instagram" -> currentSettings.autoRecordInstagram
            "telegram" -> currentSettings.autoRecordTelegram
            "imo" -> currentSettings.autoRecordImo
            "meet" -> currentSettings.autoRecordMeet
            "phone" -> currentSettings.autoRecordPhone
            else -> true
        }

        if (isEnabled && !isRecording.value) {
            startRecording(appLabel, appPackage)
        }
    }

    private fun handleAutoCallEnded() {
        if (isRecording.value) {
            stopRecording()
        }
    }

    fun checkShizuku() {
        val current = shizukuManager.checkStatus()
        viewModelScope.launch {
            settingsRepository.setShizukuReady(current == ShizukuStatus.ACTIVE)
        }
    }

    fun requestShizukuPermission() {
        shizukuManager.requestPermission()
    }

    fun toggleManualRecording() {
        if (isRecording.value) {
            stopRecording()
        } else {
            startRecording("Manual Recording", "com.personal.callbox")
        }
    }

    fun startRecording(appLabel: String, appPackage: String) {
        val bitrate = settings.value.audioBitrate
        RecordingService.startService(context, appLabel, appPackage, bitrate)

        // Show floating overlay if enabled and permitted
        if (settings.value.floatingOverlayEnabled &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context))) {
            try {
                val overlayIntent = android.content.Intent(context, FloatingOverlayService::class.java)
                context.startService(overlayIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecording() {
        RecordingService.stopService(context)
    }

    fun playRecording(recording: Recording) {
        playbackManager.playRecording(recording)
    }

    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    fun seekTo(positionMs: Int) {
        playbackManager.seekTo(positionMs)
    }

    fun setSpeed(speed: Float) {
        playbackManager.setSpeed(speed)
    }

    fun closePlayer() {
        playbackManager.stop()
    }

    fun toggleStar(recording: Recording) {
        viewModelScope.launch {
            recordingRepository.toggleStar(recording)
        }
    }

    fun renameRecording(recording: Recording, newName: String) {
        viewModelScope.launch {
            recordingRepository.renameRecording(recording, newName)
            recordingToRename.value = null
        }
    }

    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            if (currentPlayingRecording.value?.id == recording.id) {
                playbackManager.stop()
            }
            recordingRepository.deleteRecording(recording)
        }
    }

    fun deleteAllRecordings() {
        viewModelScope.launch {
            playbackManager.stop()
            recordingRepository.deleteAllRecordings()
            showDeleteAllConfirmation.value = false
        }
    }

    fun shareRecording(recording: Recording) {
        recordingRepository.shareRecording(recording)
    }

    fun setAutoRecord(appKey: String, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoRecord(appKey, enabled)
        }
    }

    fun setAudioBitrate(bitrate: Int) {
        viewModelScope.launch {
            settingsRepository.setAudioBitrate(bitrate)
        }
    }

    fun setHasSeenDisclaimer(seen: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHasSeenDisclaimer(seen)
        }
    }

    fun setFloatingOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setFloatingOverlayEnabled(enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        callDetector?.stopListening()
        playbackManager.release()
        shizukuManager.unregisterListeners()
    }
}
