package com.pressstress.app

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Space
import android.widget.Switch
import android.widget.TextView
import com.pressstress.app.data.AppPreferences
import com.pressstress.app.data.MessageTone
import com.pressstress.app.overlay.MirrorHoldView
import com.pressstress.app.overlay.OverlayService

class MainActivity : Activity() {
    private lateinit var preferences: AppPreferences
    private lateinit var statusDot: View
    private lateinit var statusTitle: TextView
    private lateinit var statusBody: TextView
    private lateinit var todayValue: TextView
    private lateinit var durationValue: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private var waitingForOverlayPermission = false

    private val backgroundColor = Color.rgb(7, 11, 22)
    private val surface = Color.rgb(14, 21, 40)
    private val surfaceRaised = Color.rgb(20, 29, 54)
    private val primary = Color.rgb(124, 140, 255)
    private val textPrimary = Color.rgb(245, 247, 255)
    private val textSecondary = Color.rgb(166, 176, 205)
    private val success = Color.rgb(91, 218, 159)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = AppPreferences(this)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        buildScreen()
    }

    override fun onResume() {
        super.onResume()
        if (waitingForOverlayPermission && Settings.canDrawOverlays(this)) {
            waitingForOverlayPermission = false
            startOverlay()
        }
        refreshStatus()
    }

    private fun buildScreen() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(40))
            setBackgroundColor(backgroundColor)
        }

        content.addView(TextView(this).apply {
            text = "PRESSSTRESS  •  ANDROID"
            setTextColor(primary)
            textSize = 12f
            typeface = Typeface.create("sans", Typeface.BOLD)
            letterSpacing = 0.12f
        })
        content.addView(space(12))
        content.addView(TextView(this).apply {
            text = "A pause between\nimpulse and action."
            setTextColor(textPrimary)
            textSize = 34f
            typeface = Typeface.create("sans", Typeface.BOLD)
            setLineSpacing(0f, 0.94f)
        })
        content.addView(space(12))
        content.addView(TextView(this).apply {
            text = "A small hold button that stays available over other apps and your home screen."
            setTextColor(textSecondary)
            textSize = 16f
            setLineSpacing(dp(3).toFloat(), 1f)
        })
        content.addView(space(24))
        content.addView(buildPreviewCard())
        content.addView(space(16))
        content.addView(buildStatusCard())
        content.addView(space(24))
        content.addView(sectionLabel("RESET LENGTH"))
        content.addView(space(10))
        content.addView(buildDurationCard())
        content.addView(space(18))
        content.addView(sectionLabel("VOICE"))
        content.addView(space(10))
        content.addView(buildToneCard())
        content.addView(space(18))
        content.addView(buildMessageToggle())
        content.addView(space(24))
        content.addView(buildActions())
        content.addView(space(22))
        content.addView(TextView(this).apply {
            text = "PRIVATE BY DEFAULT\nNo account, internet access, app-history permission, or Accessibility Service. Your settings and count stay on this device."
            setTextColor(Color.rgb(119, 132, 166))
            textSize = 12f
            setLineSpacing(dp(3).toFloat(), 1f)
        })

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
            addView(content, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        setContentView(scroll)
    }

    private fun buildPreviewCard(): View {
        val frame = FrameLayout(this).apply {
            background = roundedGradient(
                intArrayOf(Color.rgb(26, 35, 67), Color.rgb(10, 16, 33)),
                24f,
                GradientDrawable.Orientation.TL_BR,
            )
            setPadding(dp(14), dp(22), dp(14), dp(22))
        }
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        val previewButton = MirrorHoldView(this).apply {
            isEnabled = false
            alpha = 0.98f
        }
        row.addView(previewButton, LinearLayout.LayoutParams(dp(64), dp(64)))
        row.addView(space(12, horizontal = true))
        row.addView(TextView(this).apply {
            text = "🌱 You can do this"
            setTextColor(textPrimary)
            textSize = 16f
            maxLines = 1
        })
        frame.addView(row)
        return frame.apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(112))
        }
    }

    private fun buildStatusCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = roundedColor(surface, 18f, Color.rgb(32, 44, 76))
        }
        statusDot = View(this).apply {
            background = roundedColor(Color.rgb(104, 116, 150), 99f)
        }
        card.addView(statusDot, LinearLayout.LayoutParams(dp(10), dp(10)))
        card.addView(space(13, horizontal = true))
        val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        statusTitle = TextView(this).apply {
            setTextColor(textPrimary)
            textSize = 15f
            typeface = Typeface.create("sans", Typeface.BOLD)
        }
        statusBody = TextView(this).apply {
            setTextColor(textSecondary)
            textSize = 13f
        }
        copy.addView(statusTitle)
        copy.addView(statusBody)
        card.addView(copy, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        todayValue = TextView(this).apply {
            gravity = Gravity.END
            setTextColor(primary)
            textSize = 13f
            typeface = Typeface.create("sans", Typeface.BOLD)
        }
        card.addView(todayValue)
        return card
    }

    private fun buildDurationCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = roundedColor(surface, 18f, Color.TRANSPARENT)
        }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(TextView(this).apply {
            text = "Hold time"
            setTextColor(textPrimary)
            textSize = 16f
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        durationValue = TextView(this).apply {
            text = "${preferences.holdSeconds} sec"
            setTextColor(primary)
            textSize = 15f
            typeface = Typeface.create("sans", Typeface.BOLD)
        }
        top.addView(durationValue)
        card.addView(top)
        card.addView(SeekBar(this).apply {
            max = 25
            progress = preferences.holdSeconds - 5
            progressTintList = ColorStateList.valueOf(primary)
            thumbTintList = ColorStateList.valueOf(Color.rgb(211, 219, 255))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val seconds = progress + 5
                    durationValue.text = "$seconds sec"
                    if (fromUser) preferences.holdSeconds = seconds
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = restartIfRunning()
            })
        })
        return card
    }

    private fun buildToneCard(): View {
        val group = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = roundedColor(surface, 18f, Color.TRANSPARENT)
        }
        val tones = listOf(
            MessageTone.GENTLE to "Gentle  ·  Do you actually want to be here?",
            MessageTone.NEUTRAL to "Neutral  ·  Pause before you continue",
            MessageTone.STRICT to "Strict  ·  🤢 Break the loop",
            MessageTone.MINIMAL to "Minimal  ·  button only",
        )
        tones.forEach { (tone, label) ->
            group.addView(RadioButton(this).apply {
                id = View.generateViewId()
                tag = tone
                text = label
                textSize = 14f
                setTextColor(textPrimary)
                buttonTintList = ColorStateList.valueOf(primary)
                setPadding(dp(4), dp(5), dp(4), dp(5))
                isChecked = preferences.messageTone == tone
            })
        }
        group.setOnCheckedChangeListener { radioGroup, checkedId ->
            val selected = radioGroup.findViewById<RadioButton>(checkedId)?.tag as? MessageTone
            if (selected != null) {
                preferences.messageTone = selected
                restartIfRunning()
            }
        }
        return group
    }

    @Suppress("DEPRECATION")
    private fun buildMessageToggle(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(18), dp(10), dp(10), dp(10))
        background = roundedColor(surface, 18f, Color.TRANSPARENT)
        addView(TextView(this@MainActivity).apply {
            text = "Show one-line cue beside the button"
            setTextColor(textPrimary)
            textSize = 14f
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        addView(Switch(this@MainActivity).apply {
            isChecked = preferences.showMessage
            thumbTintList = ColorStateList.valueOf(Color.rgb(225, 230, 255))
            trackTintList = ColorStateList.valueOf(primary)
            setOnCheckedChangeListener { _, checked ->
                preferences.showMessage = checked
                restartIfRunning()
            }
        })
    }

    private fun buildActions(): View {
        val group = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        startButton = Button(this).apply {
            text = "Enable floating reset"
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(primary)
            setOnClickListener { requestStart() }
        }
        stopButton = Button(this).apply {
            text = "Stop overlay"
            isAllCaps = false
            textSize = 15f
            setTextColor(textPrimary)
            backgroundTintList = ColorStateList.valueOf(surfaceRaised)
            setOnClickListener {
                preferences.overlayEnabled = false
                OverlayService.stop(this@MainActivity)
                refreshStatus()
            }
        }
        group.addView(startButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)))
        group.addView(space(8))
        group.addView(stopButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)))
        return group
    }

    private fun requestStart() {
        if (!Settings.canDrawOverlays(this)) {
            waitingForOverlayPermission = true
            val specific = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            )
            try {
                startActivity(specific)
            } catch (_: ActivityNotFoundException) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            }
            return
        }
        startOverlay()
    }

    private fun startOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
        }
        OverlayService.start(this)
        preferences.overlayEnabled = true
        refreshStatus()
    }

    private fun restartIfRunning() {
        if (preferences.overlayEnabled && Settings.canDrawOverlays(this)) {
            OverlayService.start(this)
        }
    }

    private fun refreshStatus() {
        if (!::statusTitle.isInitialized) return
        val permissionGranted = Settings.canDrawOverlays(this)
        val active = permissionGranted && preferences.overlayEnabled
        statusDot.background = roundedColor(if (active) success else Color.rgb(104, 116, 150), 99f)
        statusTitle.text = when {
            active -> "Overlay active"
            permissionGranted -> "Ready to start"
            else -> "Permission needed"
        }
        statusBody.text = when {
            active -> "Visible over apps and home"
            permissionGranted -> "Android overlay access granted"
            else -> "You will approve it in Android settings"
        }
        todayValue.text = "${preferences.completionsToday()} TODAY"
        stopButton.isEnabled = active
        stopButton.alpha = if (active) 1f else 0.45f
        startButton.text = if (active) "Refresh floating reset" else "Enable floating reset"
    }

    private fun sectionLabel(value: String): TextView = TextView(this).apply {
        text = value
        setTextColor(Color.rgb(121, 135, 173))
        textSize = 11f
        typeface = Typeface.create("sans", Typeface.BOLD)
        letterSpacing = 0.14f
    }

    private fun roundedColor(
        fill: Int,
        radiusDp: Float,
        stroke: Int = Color.TRANSPARENT,
    ): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp.toInt()).toFloat()
            setColor(fill)
            if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
        }

    private fun roundedGradient(
        colors: IntArray,
        radiusDp: Float,
        orientation: GradientDrawable.Orientation,
    ): GradientDrawable = GradientDrawable(orientation, colors).apply {
        cornerRadius = dp(radiusDp.toInt()).toFloat()
        setStroke(dp(1), Color.rgb(47, 60, 102))
    }

    private fun space(sizeDp: Int, horizontal: Boolean = false): Space = Space(this).apply {
        layoutParams = if (horizontal) {
            LinearLayout.LayoutParams(dp(sizeDp), 1)
        } else {
            LinearLayout.LayoutParams(1, dp(sizeDp))
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 42
    }
}
