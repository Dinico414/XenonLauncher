package com.xenonware.launcher.util

internal const val MIN_FONT_SCALE = 0.5f

internal fun fitScale(
    naturalPx: Float,
    availablePx: Float,
    minScale: Float = MIN_FONT_SCALE
): Float = if (naturalPx <= 0f || availablePx <= 0f) 1f
else (availablePx / naturalPx).coerceIn(minScale, 1f)