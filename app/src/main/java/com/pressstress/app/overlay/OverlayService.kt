package com.pressstress.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.pressstress.app.MainActivity
import com.pressstress.app.R
import com.pressstress.app.data.AppPreferences
import com.pressstress.app.domain.OverlayMessagePolicy

class OverlayService : Service() {
    private lateinit var preferences: AppPreferences
    private lateinit var windowManager: WindowManager
    private var buttonView: MirrorHoldView? = null
    private var messageView: TextView? = null
    private var buttonParams: WindowManager.LayoutParams? = null
    private var messageParams: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())
    private var messageIndex = 0

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopOverlay()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            preferences.lastOverlayError = "Display over other apps permission is off"
            stopOverlay()
            return START_NOT_STICKY
        }

        try {
            startInForeground()
            removeOverlay()
            showOverlay()
            preferences.lastOverlayError = null
        } catch (error: Exception) {
            preferences.lastOverlayError =
                "${error.javaClass.simpleName}: ${error.message ?: "overlay start failed"}"
            stopOverlay()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        removeOverlay()
        preferences.overlayEnabled = false
        super.onDestroy()
    }

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun showOverlay() {
        val overlayContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = getSystemService(DisplayManager::class.java)
                .getDisplay(Display.DEFAULT_DISPLAY)
                ?: error("Primary display is unavailable")
            createDisplayContext(display).createWindowContext(
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                null,
            )
        } else {
            this
        }
        windowManager = overlayContext.getSystemService(WindowManager::class.java)
        val density = resources.displayMetrics.density
        val buttonSize = (64f * density).toInt()
        val leftGap = (5f * density).toInt()
        val messageGap = (10f * density).toInt()
        val savedY = preferences.overlayY

        val button = MirrorHoldView(overlayContext).apply {
            holdDurationMs = preferences.holdSeconds * 1_000L
            onHoldStateChanged = { isHolding ->
                if (isHolding) {
                    updateMessage(OverlayMessagePolicy.holding(preferences.messageTone))
                } else if (buttonView != null) {
                    updateMessage(OverlayMessagePolicy.resting(preferences.messageTone, messageIndex))
                }
            }
            onHoldCompleted = {
                val count = preferences.recordCompletion()
                messageIndex += 1
                updateMessage(OverlayMessagePolicy.completed(preferences.messageTone, count))
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({
                    updateMessage(OverlayMessagePolicy.resting(preferences.messageTone, messageIndex))
                }, COMPLETION_MESSAGE_MS)
            }
            onVerticalDrag = { delta, finished -> moveVertically(delta, finished) }
        }

        val label = TextView(overlayContext).apply {
            setTextColor(Color.argb(235, 244, 246, 255))
            textSize = 16f
            typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL)
            maxLines = 1
            isSingleLine = true
            includeFontPadding = false
            setPadding(0, 0, (8f * density).toInt(), 0)
            setShadowLayer(1.5f * density, 0f, 1f * density, Color.argb(145, 0, 0, 0))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }

        buttonParams = WindowManager.LayoutParams(
            buttonSize,
            buttonSize,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            x = leftGap
            y = savedY
        }

        messageParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            (48f * density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            x = leftGap + buttonSize + messageGap
            y = savedY
            alpha = 0.8f
        }

        buttonView = button
        messageView = label
        windowManager.addView(button, buttonParams)
        windowManager.addView(label, messageParams)
        isOverlayVisible = true
        preferences.overlayEnabled = true
        updateMessage(OverlayMessagePolicy.resting(preferences.messageTone, messageIndex))
    }

    private fun moveVertically(delta: Int, finished: Boolean) {
        val button = buttonView ?: return
        val label = messageView ?: return
        val buttonLayout = buttonParams ?: return
        val labelLayout = messageParams ?: return
        if (delta != 0) {
            val maxOffset = ((resources.displayMetrics.heightPixels - button.height) / 2)
                .coerceAtLeast(0)
            buttonLayout.y = (buttonLayout.y + delta).coerceIn(-maxOffset, maxOffset)
            labelLayout.y = buttonLayout.y
            windowManager.updateViewLayout(button, buttonLayout)
            windowManager.updateViewLayout(label, labelLayout)
        }
        if (finished) preferences.overlayY = buttonLayout.y
    }

    private fun updateMessage(message: String) {
        val label = messageView ?: return
        val visibleMessage = preferences.showMessage && message.isNotBlank()
        label.text = message
        label.visibility = if (visibleMessage) View.VISIBLE else View.GONE
    }

    private fun removeOverlay() {
        buttonView?.let { runCatching { windowManager.removeView(it) } }
        messageView?.let { runCatching { windowManager.removeView(it) } }
        buttonView = null
        messageView = null
        buttonParams = null
        messageParams = null
        isOverlayVisible = false
    }

    private fun stopOverlay() {
        preferences.overlayEnabled = false
        removeOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            },
        )
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(0, getString(R.string.stop), stopIntent)
            .build()
    }

    companion object {
        @Volatile
        var isOverlayVisible: Boolean = false
            private set

        const val ACTION_START = "com.pressstress.app.action.START_OVERLAY"
        const val ACTION_STOP = "com.pressstress.app.action.STOP_OVERLAY"
        private const val CHANNEL_ID = "floating_reset"
        private const val NOTIFICATION_ID = 1001
        private const val COMPLETION_MESSAGE_MS = 2_800L

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java).setAction(ACTION_START)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, OverlayService::class.java).setAction(ACTION_STOP))
        }
    }
}
