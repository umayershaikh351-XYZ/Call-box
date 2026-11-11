package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var timerTextView: TextView? = null
    private var pulseJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        setupOverlay()
        observeRecordingState()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 32
            y = 120
        }

        val pillLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8))
            gravity = Gravity.CENTER_VERTICAL

            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#E6161B22"))
                cornerRadius = dpToPx(24).toFloat()
                setStroke(dpToPx(1), Color.parseColor("#00E5C7"))
            }
            background = bg
        }

        val dotView = View(this).apply {
            val size = dpToPx(8)
            val dotParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dpToPx(8)
            }
            layoutParams = dotParams
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FF4D5E"))
            }
        }

        val text = TextView(this).apply {
            text = "● Call Box: 00:00"
            setTextColor(Color.parseColor("#E6EDF3"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            timerTextView = this
        }

        pillLayout.addView(dotView)
        pillLayout.addView(text)

        // Drag and tap support
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMove = false

        pillLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMove = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (initialTouchX - event.rawX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isMove = true
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager?.updateViewLayout(pillLayout, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMove) {
                        // Tap to stop
                        RecordingService.stopService(this@FloatingOverlayService)
                    }
                    true
                }
                else -> false
            }
        }

        overlayView = pillLayout
        try {
            windowManager?.addView(pillLayout, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeRecordingState() {
        scope.launch {
            RecordingService.recordingDurationMs.collectLatest { durationMs ->
                val totalSeconds = durationMs / 1000
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                val formatted = String.format("%02d:%02d", minutes, seconds)
                timerTextView?.text = "● Call Box $formatted (tap to stop)"
            }
        }

        scope.launch {
            RecordingService.isServiceRecording.collectLatest { isRecording ->
                if (!isRecording) {
                    stopSelf()
                }
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        pulseJob?.cancel()
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (ignored: Exception) {}
        }
        overlayView = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
