package com.pressstress.app.domain

import com.pressstress.app.data.MessageTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayMessagePolicyTest {
    @Test
    fun `minimal tone never shows copy`() {
        assertEquals("", OverlayMessagePolicy.resting(MessageTone.MINIMAL, 9))
        assertEquals("", OverlayMessagePolicy.holding(MessageTone.MINIMAL))
        assertEquals("", OverlayMessagePolicy.completed(MessageTone.MINIMAL, 4))
    }

    @Test
    fun `message index wraps in both directions`() {
        assertEquals(
            OverlayMessagePolicy.resting(MessageTone.GENTLE, 0),
            OverlayMessagePolicy.resting(MessageTone.GENTLE, 4),
        )
        assertTrue(OverlayMessagePolicy.resting(MessageTone.STRICT, -1).isNotBlank())
    }

    @Test
    fun `neutral completion includes local daily count`() {
        assertEquals("✓ 3 pauses today", OverlayMessagePolicy.completed(MessageTone.NEUTRAL, 3))
    }
}
