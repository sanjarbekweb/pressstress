package com.pressstress.app.domain

import com.pressstress.app.data.MessageTone

object OverlayMessagePolicy {
    private val gentle = listOf(
        "🌱 You can do this",
        "🌊 Let the urge pass",
        "🫧 One slow breath",
        "✨ Choose on purpose",
    )

    private val neutral = listOf(
        "👁 Intentional or automatic?",
        "⏸ Pause before you continue",
        "🧭 Choose your next action",
        "⏳ Give it ten seconds",
    )

    private val strict = listOf(
        "😑 Again?",
        "🤨 Is this what you chose?",
        "🤢 Break the loop",
        "💀 Autopilot detected",
    )

    fun resting(tone: MessageTone, index: Int): String = when (tone) {
        MessageTone.GENTLE -> gentle.cycle(index)
        MessageTone.NEUTRAL -> neutral.cycle(index)
        MessageTone.STRICT -> strict.cycle(index)
        MessageTone.MINIMAL -> ""
    }

    fun holding(tone: MessageTone): String = when (tone) {
        MessageTone.GENTLE -> "🌬 Keep breathing"
        MessageTone.NEUTRAL -> "⏳ Keep holding"
        MessageTone.STRICT -> "✋ Do not reset the timer"
        MessageTone.MINIMAL -> ""
    }

    fun completed(tone: MessageTone, countToday: Int): String = when (tone) {
        MessageTone.GENTLE -> "✓ Urge interrupted"
        MessageTone.NEUTRAL -> "✓ $countToday pauses today"
        MessageTone.STRICT -> "✓ Loop broken"
        MessageTone.MINIMAL -> ""
    }

    private fun List<String>.cycle(index: Int): String = this[Math.floorMod(index, size)]
}
