package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.audio.AudioCaptureEngine
import com.example.data.CallBoxDatabase
import com.example.data.RecordingRepository
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RecordingService : Service() {

    companion object {
        const val CHANNEL_ID = "callbox_recording_channel"
        const val NOTIFICATION_ID = 4040

        const val ACTION_START = "com.example.action.START_RECORDING"
        const val ACTION_STOP = "com.example.action.STOP_RECORDING"

        const val EXTRA_APP_LABEL = "extra_app_label"
        const val EXTRA_APP_PACKAGE = "extra_app_package"
        const val EXTRA_BITRATE = "extra_bitrate"

        private val _isServiceRecording = MutableStateFlow(false)
        val isServiceRecording: StateFlow<Boolean> = _isServiceRecording.asStateFlow()

        private val _recordingDurationMs = MutableStateFlow(0L)
        val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

        private val _recordingAppLabel = MutableStateFlow("Manual Call")
        val recordingAppLabel: StateFlow<String> = _recordingAppLabel.asStateFlow()

        private val _recordingAmplitude = MutableStateFlow(0f)
        val recordingAmplitude: StateFlow<Float> = _recordingAmplitude.asStateFlow()

        fun startService(
            context: Context,
            appLabel: String = "Manual Call",
            appPackage: String = "com.android.phone",
            bitrate: Int = 96000
        ) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_APP_LABEL, appLabel)
                putExtra(EXTRA_APP_PACKAGE, appPackage)
                putExtra(EXTRA_BITRATE, bitrate)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private lateinit var audioCaptureEngine: AudioCaptureEngine
    private lateinit var recordingRepository: RecordingRepository
    private lateinit var settingsRepository: SettingsRepository
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var durationMonitorJob: Job? = null
    private var currentAppLabel = "Manual Call"
    private var currentAppPackage = "com.android.phone"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val database = CallBoxDatabase.getInstance(this)
        recordingRepository = RecordingRepository(this, database.recordingDao())
        settingsRepository = SettingsRepository(this)
        audioCaptureEngine = AudioCaptureEngine(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val appLabel = intent.getStringExtra(EXTRA_APP_LABEL) ?: "Manual Call"
                val appPackage = intent.getStringExtra(EXTRA_APP_PACKAGE) ?: "com.android.phone"
                val bitrate = intent.getIntExtra(EXTRA_BITRATE, 96000)
                handleStartRecording(appLabel, appPackage, bitrate)
            }
            ACTION_STOP -> {
                handleStopRecording()
            }
        }
        return START_NOT_STICKY
    }

    private fun handleStartRecording(appLabel: String, appPackage: String, bitrate: Int) {
        currentAppLabel = appLabel
        currentAppPackage = appPackage
        _recordingAppLabel.value = appLabel

        val notification = buildNotification(0L, appLabel)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        serviceScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            val outputFile = recordingRepository.generateNewOutputFile(appLabel)
            val started = audioCaptureEngine.startRecording(
                outputFile = outputFile,
                appLabel = appLabel,
                appPackage = appPackage,
                bitrate = bitrate,
                preferVoiceCallSource = settings.shizukuReady
            )

            if (started) {
                _isServiceRecording.value = true
                startMonitoring()
            } else {
                stopSelf()
            }
        }
    }

    private fun startMonitoring() {
        durationMonitorJob?.cancel()
        durationMonitorJob = serviceScope.launch {
            while (isActive && _isServiceRecording.value) {
                val duration = audioCaptureEngine.durationMs.value
                val amp = audioCaptureEngine.amplitude.value
                _recordingDurationMs.value = duration
                _recordingAmplitude.value = amp

                // Update notification every 1 second
                if (duration % 1000 < 250) {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, buildNotification(duration, currentAppLabel))
                }
                delay(200)
            }
        }
    }

    private fun handleStopRecording() {
        durationMonitorJob?.cancel()
        durationMonitorJob = null

        val duration = audioCaptureEngine.durationMs.value
        val file = audioCaptureEngine.stopRecording()

        _isServiceRecording.value = false
        _recordingDurationMs.value = 0L
        _recordingAmplitude.value = 0f

        if (file != null && file.exists()) {
            serviceScope.launch {
                recordingRepository.saveRecording(
                    file = file,
                    appPackage = currentAppPackage,
                    appLabel = currentAppLabel,
                    durationMs = duration
                )
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildNotification(durationMs: Long, appLabel: String): Notification {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val timerString = String.format("%02d:%02d", minutes, seconds)

        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Call Box is recording")
            .setContentText("Capturing $appLabel • $timerString — Tap Stop to finish")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop Recording",
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Box Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live Call Box audio recording status"
                setSound(null, null)
                enableVibration(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        audioCaptureEngine.stopRecording()
        _isServiceRecording.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
