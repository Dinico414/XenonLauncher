package com.xenonware.launcher.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the At a Glance clock size helpers: the bounds, the 10% snapping and the
 * percentage that the settings subtitle shows.
 */
class ClockSizeTest {

    @Test
    fun `bounds describe a 100 to 300 percent range`() {
        assertEquals(1f, MIN_CLOCK_SIZE_SCALE, 0f)
        assertEquals(3f, MAX_CLOCK_SIZE_SCALE, 0f)
    }

    @Test
    fun `slider leaves nineteen interior stops between the ends`() {
        // 100, 110, ... 300 is 21 stops including both ends, so 19 are strictly inside.
        assertEquals(19, CLOCK_SIZE_SLIDER_STEPS)
    }

    @Test
    fun `every step lands on a whole percentage`() {
        // The slider's steps must line up with the quantizer, or the thumb would sit between stops.
        val steps = (MIN_CLOCK_SIZE_PERCENT..MAX_CLOCK_SIZE_PERCENT)
            .filter { (it - MIN_CLOCK_SIZE_PERCENT) % CLOCK_SIZE_STEP_PERCENT == 0 }
        assertEquals(CLOCK_SIZE_SLIDER_STEPS + 2, steps.size)
    }

    @Test
    fun `values outside the range are clamped`() {
        assertEquals(1f, sanitizeClockSizeScale(0.2f), 0f)
        assertEquals(1f, sanitizeClockSizeScale(-5f), 0f)
        assertEquals(3f, sanitizeClockSizeScale(9f), 0f)
        assertEquals(1f, sanitizeClockSizeScale(Float.NaN), 0f)
    }

    @Test
    fun `values inside the range are untouched`() {
        assertEquals(1.4f, sanitizeClockSizeScale(1.4f), 0f)
        assertEquals(2.6f, sanitizeClockSizeScale(2.6f), 0f)
    }

    @Test
    fun `quantize snaps to the nearest ten percent step`() {
        assertEquals(1.0f, quantizeClockSizeScale(1.0f), 0f)
        assertEquals(1.0f, quantizeClockSizeScale(1.04f), 0f)
        assertEquals(1.1f, quantizeClockSizeScale(1.06f), 0f)
        assertEquals(1.3f, quantizeClockSizeScale(1.2727f), 0f)
        assertEquals(2.0f, quantizeClockSizeScale(1.96f), 0f)
        assertEquals(2.5f, quantizeClockSizeScale(2.54f), 0f)
        assertEquals(3.0f, quantizeClockSizeScale(2.96f), 0f)
        assertEquals(3.0f, quantizeClockSizeScale(5f), 0f)
        assertEquals(1.0f, quantizeClockSizeScale(0f), 0f)
    }

    @Test
    fun `quantized values are always valid slider values`() {
        var scale = 1.0f
        while (scale <= 3.0001f) {
            val snapped = quantizeClockSizeScale(scale)
            // On a stop (checked via the rounding helper, since a float like 1.3f is 1.2999999 and
            // truncating its percentage would fall one step short), and unchanged by a second pass.
            assertEquals(0, clockSizePercent(snapped) % CLOCK_SIZE_STEP_PERCENT)
            assertEquals(snapped, quantizeClockSizeScale(snapped), 0f)
            scale += 0.01f
        }
    }

    @Test
    fun `percent is the whole number shown in the settings subtitle`() {
        assertEquals(100, clockSizePercent(1f))
        assertEquals(110, clockSizePercent(1.1f))
        assertEquals(200, clockSizePercent(2f))
        assertEquals(250, clockSizePercent(2.5f))
        assertEquals(300, clockSizePercent(3f))
    }
}
