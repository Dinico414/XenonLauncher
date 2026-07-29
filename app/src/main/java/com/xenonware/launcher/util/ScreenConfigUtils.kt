package com.xenonware.launcher.util

import android.content.Context
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

fun shouldDisableLandscapeLayout(context: Context): Boolean {
    val resources = context.resources
    val metrics = resources.displayMetrics

    // 1. Physische Abmessungen in Inches (Zoll) berechnen
    val widthInches = metrics.widthPixels / metrics.xdpi
    val heightInches = metrics.heightPixels / metrics.ydpi
    val screenDiagonalInches = sqrt((widthInches * widthInches) + (heightInches * heightInches))

    // 2. Echtes Seitenverhältnis der Hardware berechnen (Unabhängig von aktueller Orientation)
    val maxPx = max(metrics.widthPixels, metrics.heightPixels).toDouble()
    val minPx = min(metrics.widthPixels, metrics.heightPixels).toDouble()
    val aspectRatio = maxPx / minPx  // z.B. 1.07 beim Mind One, 1.25 beim Foldable

    // 3. Bedingungen prüfen:

    // Ist der Bildschirm nahezu quadratisch? (Seitenverhältnis unter 1:1.25)
    val isAlmostSquare = aspectRatio < 1.25

    // Ist es ein physisch kleiner Bildschirm? (Kleiner als 5.5 Zoll Diagonale)
    // (Mind One = ~4.0", Clicks = ~4.0", Titan Elite 2 = ~4.0")
    val isSmallScreen = screenDiagonalInches < 5.5

    // ERGEBNIS:
    // Wenn das Gerät quadratisch UND physisch klein ist -> SCHALTE LANDSCAPE AB!
    // Wenn es ein Foldable ist (quadratisch, aber > 7 Zoll) -> BLEIBT LANDSCAPE AN!
    return isAlmostSquare && isSmallScreen
}