package com.xenonware.launcher.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.font.FontFamily
import kotlin.math.roundToInt

/**
 * Bounds for the At a Glance clock size, expressed as a percentage of the stock text size and shared
 * by the preference getter, the settings slider and the rendering code.
 *
 * Only the clock is scaled: the date next to it keeps its stock size, so a large clock never drags
 * the rest of the At a Glance line along with it.
 *
 * The stock size is the *minimum* - the UI has no smaller variant to fall back to - so 100% is the
 * floor and 300% is the slider's top end.
 */
const val MIN_CLOCK_SIZE_PERCENT = 100
const val MAX_CLOCK_SIZE_PERCENT = 300

/** The slider only offers multiples of this percentage. */
const val CLOCK_SIZE_STEP_PERCENT = 10

/**
 * Number of discrete stops *strictly between* the two ends, which is what Material3's `Slider`
 * expects for its `steps` argument. 10% increments over 100%..300% leaves 19 interior stops.
 *
 * Derived from the range rather than hard-coded, so widening the range only means changing
 * [MAX_CLOCK_SIZE_PERCENT].
 */
const val CLOCK_SIZE_SLIDER_STEPS =
    ((MAX_CLOCK_SIZE_PERCENT - MIN_CLOCK_SIZE_PERCENT) / CLOCK_SIZE_STEP_PERCENT) - 1

/** The scale bounds as a multiplier, for code that multiplies a `TextUnit`. */
const val MIN_CLOCK_SIZE_SCALE = MIN_CLOCK_SIZE_PERCENT / 100f
const val MAX_CLOCK_SIZE_SCALE = MAX_CLOCK_SIZE_PERCENT / 100f

/** Clamps any incoming scale into the supported range. */
fun sanitizeClockSizeScale(scale: Float): Float = when {
    scale.isNaN() -> MIN_CLOCK_SIZE_SCALE
    else -> scale.coerceIn(MIN_CLOCK_SIZE_SCALE, MAX_CLOCK_SIZE_SCALE)
}

/**
 * Rounds [scale] to the nearest step the slider offers, so a stored or programmatically set value
 * can never sit between two stops. Worked in whole percent to keep float error out of the result.
 */
fun quantizeClockSizeScale(scale: Float): Float {
    val percent = (sanitizeClockSizeScale(scale) * 100f).roundToInt()
    // (percent + halfStep) / step * step rounds to nearest; all values here are positive.
    val snapped = (percent + CLOCK_SIZE_STEP_PERCENT / 2) /
            CLOCK_SIZE_STEP_PERCENT * CLOCK_SIZE_STEP_PERCENT
    return snapped.coerceIn(MIN_CLOCK_SIZE_PERCENT, MAX_CLOCK_SIZE_PERCENT) / 100f
}

/** [scale] as the whole percentage shown in the settings subtitle. */
fun clockSizePercent(scale: Float): Int =
    (sanitizeClockSizeScale(scale) * 100f).roundToInt()

/**
 * Which of the launcher's two configured fonts the At a Glance clock is drawn in. Both families are
 * already published through the composition locals, so this is only a choice between them.
 */
enum class ClockFont(/** The value persisted in preferences. */ val id: Int) {
    MAIN(0),
    SECONDARY(1);

    companion object {
        fun fromId(id: Int): ClockFont = entries.find { it.id == id } ?: MAIN
    }
}

/**
 * Resolves a [ClockFont] to the actual family. Read through the same composition locals the rest of
 * the launcher uses, so the clock always follows a change to either font immediately.
 */
@Composable
@ReadOnlyComposable
fun ClockFont.family(): FontFamily = when (this) {
    ClockFont.MAIN -> mainFontFamily
    ClockFont.SECONDARY -> subFontFamily
}
