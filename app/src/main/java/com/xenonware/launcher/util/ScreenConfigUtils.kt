package com.xenonware.launcher.util

import android.content.Context
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

fun shouldDisableLandscapeLayout(context: Context): Boolean {
    val resources = context.resources
    val metrics = resources.displayMetrics

    // Sichere Berechnung der DP-Abmessungen über density (160 dp = 1 inch)
    val density = if (metrics.density > 0f) metrics.density else 1.0f
    val widthDp = metrics.widthPixels / density
    val heightDp = metrics.heightPixels / density
    val screenDiagonalInches = sqrt((widthDp * widthDp) + (heightDp * heightDp)) / 160.0

    val maxDp = max(widthDp, heightDp).toDouble()
    val minDp = min(widthDp, heightDp).toDouble().coerceAtLeast(1.0)
    val aspectRatio = maxDp / minDp

    val isAlmostSquare = aspectRatio < 1.35
    val isSmallScreen = screenDiagonalInches < 5.2

    return isSmallScreen || (isAlmostSquare && screenDiagonalInches < 6.0)
}

fun isLandscapeAllowed(context: Context): Boolean {
    return !shouldDisableLandscapeLayout(context)
}

fun isSmallScreenDevice(context: Context): Boolean {
    return shouldDisableLandscapeLayout(context)
}