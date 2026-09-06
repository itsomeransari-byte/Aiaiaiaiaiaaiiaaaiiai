package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.engine.JarvisCommandEngine
import com.example.engine.JarvisCommandResult
import com.example.engine.JarvisSpeaker
import com.example.engine.JarvisVoiceRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JarvisBackgroundService : Service() {

    private lateinit var commandEngine: JarvisCommandEngine
    private lateinit var speaker: JarvisSpeaker
    private var voiceRecognizer: JarvisVoiceRecognizer? = null

    private var windowManager: WindowManager? = null
    private var floatingOverlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        _isBackgroundActive.value = true

        commandEngine = JarvisCommandEngine(this)
        speaker = JarvisSpeaker(this)

        startForegroundServiceNotification()
        initVoiceListener()
        setupFloatingOverlayIfPermitted()

        JarvisAccessibilityService.logEvent("Jarvis Background Core started. Ready for hands-free commands.")
        speaker.speak("Jarvis background automation core activated, sir.")
    }

    private fun startForegroundServiceNotification() {
        val channelId = "jarvis_automation_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
            }
            manager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("J.A.R.V.I.S. Core Active")
            .setContentText("Hands-free mobile automation & gestures listening in background.")
            .setSmallIcon(R.drawable.ic_jarvis_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun initVoiceListener() {
        voiceRecognizer = JarvisVoiceRecognizer(this) { spokenCommand ->
            handleSpokenCommand(spokenCommand)
        }
        voiceRecognizer?.startListening()
    }

    fun handleSpokenCommand(spokenCommand: String) {
        _lastCommandText.value = spokenCommand
        updateOverlayStatus(spokenCommand)

        val result = commandEngine.processCommand(spokenCommand)
        when (result) {
            is JarvisCommandResult.Success -> {
                speaker.speak(result.speechResponse)
                updateOverlayStatus(result.actionDetail)
            }
            is JarvisCommandResult.Error -> {
                speaker.speak(result.speechResponse)
                updateOverlayStatus("Error: ${result.reason}")
            }
        }
    }

    private fun setupFloatingOverlayIfPermitted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        try {
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
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 30
                y = 200
            }

            // Create floating HUD layout programmatically
            val root = FrameLayout(this).apply {
                setPadding(12, 12, 12, 12)
            }

            val icon = ImageView(this).apply {
                setImageResource(R.drawable.ic_jarvis_icon)
                layoutParams = FrameLayout.LayoutParams(130, 130)
            }

            root.addView(icon)

            // Make draggable
            root.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    when (event?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            windowManager?.updateViewLayout(root, params)
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            val diffX = Math.abs(event.rawX - initialTouchX)
                            val diffY = Math.abs(event.rawY - initialTouchY)
                            if (diffX < 10 && diffY < 10) {
                                // Tap triggers quick voice capture or launches Jarvis UI
                                val launch = Intent(this@JarvisBackgroundService, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                startActivity(launch)
                            }
                            return true
                        }
                    }
                    return false
                }
            })

            windowManager?.addView(root, params)
            floatingOverlayView = root
            _isOverlayActive.value = true
        } catch (e: Exception) {
            JarvisAccessibilityService.logEvent("Floating HUD Overlay init failed: ${e.message}")
        }
    }

    private fun updateOverlayStatus(text: String) {
        _lastStatusText.value = text
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isBackgroundActive.value = false
        _isOverlayActive.value = false

        floatingOverlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // Ignore
            }
            floatingOverlayView = null
        }

        voiceRecognizer?.destroy()
        speaker.shutdown()
        JarvisAccessibilityService.logEvent("Jarvis Background Core terminated.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 10101
        var instance: JarvisBackgroundService? = null
            private set

        private val _isBackgroundActive = MutableStateFlow(false)
        val isBackgroundActive: StateFlow<Boolean> = _isBackgroundActive.asStateFlow()

        private val _isOverlayActive = MutableStateFlow(false)
        val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

        private val _lastCommandText = MutableStateFlow("")
        val lastCommandText: StateFlow<String> = _lastCommandText.asStateFlow()

        private val _lastStatusText = MutableStateFlow("Standing by")
        val lastStatusText: StateFlow<String> = _lastStatusText.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, JarvisBackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, JarvisBackgroundService::class.java)
            context.stopService(intent)
        }
    }
}
