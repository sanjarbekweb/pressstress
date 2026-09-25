package com.pressstress.app.data

import android.content.Context
import java.time.LocalDate

enum class MessageTone(val storageValue: String) {
    GENTLE("gentle"),
    NEUTRAL("neutral"),
    STRICT("strict"),
    MINIMAL("minimal");

    companion object {
        fun fromStorage(value: String?): MessageTone =
            entries.firstOrNull { it.storageValue == value } ?: GENTLE
    }
}

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var holdSeconds: Int
        get() = prefs.getInt(KEY_HOLD_SECONDS, DEFAULT_HOLD_SECONDS).coerceIn(5, 30)
        set(value) = prefs.edit().putInt(KEY_HOLD_SECONDS, value.coerceIn(5, 30)).apply()

    var messageTone: MessageTone
        get() = MessageTone.fromStorage(prefs.getString(KEY_TONE, null))
        set(value) = prefs.edit().putString(KEY_TONE, value.storageValue).apply()

    var showMessage: Boolean
        get() = prefs.getBoolean(KEY_SHOW_MESSAGE, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_MESSAGE, value).apply()

    var overlayY: Int
        get() = prefs.getInt(KEY_OVERLAY_Y, 0)
        set(value) = prefs.edit().putInt(KEY_OVERLAY_Y, value).apply()

    var overlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, value).apply()

    fun completionsToday(today: LocalDate = LocalDate.now()): Int {
        val date = prefs.getString(KEY_COUNT_DATE, null)
        return if (date == today.toString()) prefs.getInt(KEY_COUNT, 0) else 0
    }

    fun recordCompletion(today: LocalDate = LocalDate.now()): Int {
        val next = completionsToday(today) + 1
        prefs.edit()
            .putString(KEY_COUNT_DATE, today.toString())
            .putInt(KEY_COUNT, next)
            .apply()
        return next
    }

    companion object {
        const val DEFAULT_HOLD_SECONDS = 10
        private const val FILE_NAME = "pressstress_preferences"
        private const val KEY_HOLD_SECONDS = "hold_seconds"
        private const val KEY_TONE = "message_tone"
        private const val KEY_SHOW_MESSAGE = "show_message"
        private const val KEY_OVERLAY_Y = "overlay_y"
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        private const val KEY_COUNT_DATE = "completion_date"
        private const val KEY_COUNT = "completion_count"
    }
}
