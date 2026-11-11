package com.example.shizuku

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

enum class ShizukuStatus {
    ACTIVE,         // Running and permission granted
    NEEDS_PAIRING,  // Shizuku running, but permission not granted
    INACTIVE        // Shizuku service is not running / dead
}

class ShizukuManager(private val context: Context) {

    private val _status = MutableStateFlow(ShizukuStatus.INACTIVE)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _status.value = ShizukuStatus.INACTIVE
    }

    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            _status.value = ShizukuStatus.ACTIVE
        } else {
            _status.value = ShizukuStatus.NEEDS_PAIRING
        }
    }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        checkStatus()
    }

    fun checkStatus(): ShizukuStatus {
        val newStatus = try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.isPreV11()) {
                    ShizukuStatus.INACTIVE
                } else {
                    val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                    if (granted) ShizukuStatus.ACTIVE else ShizukuStatus.NEEDS_PAIRING
                }
            } else {
                ShizukuStatus.INACTIVE
            }
        } catch (e: Throwable) {
            ShizukuStatus.INACTIVE
        }
        _status.value = newStatus
        return newStatus
    }

    fun requestPermission(requestCode: Int = 1001) {
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    Shizuku.requestPermission(requestCode)
                } else {
                    _status.value = ShizukuStatus.ACTIVE
                }
            } else {
                _status.value = ShizukuStatus.INACTIVE
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            _status.value = ShizukuStatus.INACTIVE
        }
    }

    fun unregisterListeners() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
