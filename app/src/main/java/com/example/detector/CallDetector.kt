package com.example.detector

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Process
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log

data class CallingAppInfo(
    val packageName: String,
    val label: String,
    val settingKey: String
)

class CallDetector(
    private val context: Context,
    private val onCallStarted: (appPackage: String, appLabel: String, settingKey: String) -> Unit,
    private val onCallEnded: () -> Unit
) {
    private val tag = "CallDetector"
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    private var isCallActive = false
    private var isTelephonyCallActive = false

    // App definitions
    companion object {
        val KNOWN_CALLING_APPS = mapOf(
            "com.whatsapp" to CallingAppInfo("com.whatsapp", "WhatsApp", "whatsapp"),
            "com.whatsapp.w4b" to CallingAppInfo("com.whatsapp.w4b", "WhatsApp Business", "whatsapp"),
            "com.instagram.android" to CallingAppInfo("com.instagram.android", "Instagram", "instagram"),
            "org.telegram.messenger" to CallingAppInfo("org.telegram.messenger", "Telegram", "telegram"),
            "org.telegram.plus" to CallingAppInfo("org.telegram.plus", "Telegram", "telegram"),
            "com.imo.android.imoim" to CallingAppInfo("com.imo.android.imoim", "IMO", "imo"),
            "com.google.android.apps.meetings" to CallingAppInfo("com.google.android.apps.meetings", "Google Meet", "meet"),
            "com.google.android.apps.tachyon" to CallingAppInfo("com.google.android.apps.tachyon", "Google Meet", "meet"),
            "com.android.phone" to CallingAppInfo("com.android.phone", "Phone", "phone"),
            "com.google.android.dialer" to CallingAppInfo("com.google.android.dialer", "Phone", "phone"),
            "com.samsung.android.dialer" to CallingAppInfo("com.samsung.android.dialer", "Phone", "phone")
        )

        fun hasUsageStatsPermission(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
    }

    // TelephonyCallback for Android 12+
    private var telephonyCallback: Any? = null

    // PhoneStateListener for older Android
    @Suppress("DEPRECATION")
    private var legacyPhoneStateListener: PhoneStateListener? = null

    // AudioModeChangeListener for API 31+
    private var modeListener: AudioManager.OnModeChangedListener? = null

    fun startListening() {
        registerTelephonyCallback()
        registerAudioModeListener()
    }

    fun stopListening() {
        unregisterTelephonyCallback()
        unregisterAudioModeListener()
    }

    private fun registerTelephonyCallback() {
        if (telephonyManager == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleTelephonyState(state)
                }
            }
            try {
                telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
                telephonyCallback = callback
            } catch (e: SecurityException) {
                Log.w(tag, "Permission READ_PHONE_STATE missing: ${e.message}")
            }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleTelephonyState(state)
                }
            }
            try {
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                legacyPhoneStateListener = listener
            } catch (e: SecurityException) {
                Log.w(tag, "Permission READ_PHONE_STATE missing: ${e.message}")
            }
        }
    }

    private fun unregisterTelephonyCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = telephonyCallback as? TelephonyCallback
            if (callback != null && telephonyManager != null) {
                telephonyManager.unregisterTelephonyCallback(callback)
            }
            telephonyCallback = null
        } else {
            if (legacyPhoneStateListener != null && telephonyManager != null) {
                @Suppress("DEPRECATION")
                telephonyManager.listen(legacyPhoneStateListener, PhoneStateListener.LISTEN_NONE)
            }
            legacyPhoneStateListener = null
        }
    }

    private fun registerAudioModeListener() {
        if (audioManager == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val listener = AudioManager.OnModeChangedListener { mode ->
                handleAudioMode(mode)
            }
            try {
                audioManager.addOnModeChangedListener(context.mainExecutor, listener)
                modeListener = listener
            } catch (e: Exception) {
                Log.e(tag, "Error adding OnModeChangedListener: ${e.message}")
            }
        }
    }

    private fun unregisterAudioModeListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modeListener?.let {
                audioManager?.removeOnModeChangedListener(it)
            }
            modeListener = null
        }
    }

    private fun handleTelephonyState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                Log.i(tag, "Phone call connected (OFFHOOK)")
                isTelephonyCallActive = true
                isCallActive = true
                onCallStarted("com.android.phone", "Phone Call", "phone")
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (isTelephonyCallActive) {
                    Log.i(tag, "Phone call ended (IDLE)")
                    isTelephonyCallActive = false
                    isCallActive = false
                    onCallEnded()
                }
            }
            TelephonyManager.CALL_STATE_RINGING -> {
                Log.d(tag, "Phone ringing...")
            }
        }
    }

    private fun handleAudioMode(mode: Int) {
        if (isTelephonyCallActive) return // Regular call handled separately

        if (mode == AudioManager.MODE_IN_COMMUNICATION) {
            if (!isCallActive) {
                isCallActive = true
                val fgApp = getForegroundCallingApp()
                Log.i(tag, "VoIP call detected in ${fgApp.label} (${fgApp.packageName})")
                onCallStarted(fgApp.packageName, fgApp.label, fgApp.settingKey)
            }
        } else if (mode == AudioManager.MODE_NORMAL) {
            if (isCallActive) {
                Log.i(tag, "VoIP call ended (audio mode returned to normal)")
                isCallActive = false
                onCallEnded()
            }
        }
    }

    private fun getForegroundCallingApp(): CallingAppInfo {
        if (usageStatsManager != null && hasUsageStatsPermission(context)) {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 1000 * 30 // Look at last 30 seconds
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            var lastForegroundPkg = ""

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastForegroundPkg = event.packageName
                }
            }

            if (lastForegroundPkg.isNotEmpty()) {
                val known = KNOWN_CALLING_APPS[lastForegroundPkg]
                if (known != null) {
                    return known
                }
                // Try to get app label from package manager
                return try {
                    val appInfo = context.packageManager.getApplicationInfo(lastForegroundPkg, 0)
                    val label = context.packageManager.getApplicationLabel(appInfo).toString()
                    CallingAppInfo(lastForegroundPkg, label, "other")
                } catch (e: Exception) {
                    CallingAppInfo(lastForegroundPkg, lastForegroundPkg, "other")
                }
            }
        }

        // Fallback default
        return CallingAppInfo("com.unknown.voip", "VoIP Call", "voip")
    }
}
