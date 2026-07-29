package com.xenonware.launcher.util

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette

object ColorUtils {
    fun getDominantColor(drawable: Drawable?): Color {
        if (drawable == null) return Color.Unspecified
        return getDominantColor(drawable.toBitmap(width = 40, height = 40))
    }

    fun getDominantColor(bitmap: Bitmap?): Color {
        if (bitmap == null) return Color.Unspecified
        return try {
            // 1. Use Palette for brand-aware color extraction
            val palette = Palette.from(bitmap).generate()

            // YouTube/Reddit fix: Prioritize vibrant brand colors
            val swatch = palette.darkVibrantSwatch
                ?: palette.vibrantSwatch
                ?: palette.lightVibrantSwatch
                ?: palette.dominantSwatch

            val rawColor = if (swatch != null) {
                swatch.rgb
            } else {
                // 2. Fallback center logic
                val width = bitmap.width
                val height = bitmap.height
                if (width <= 0 || height <= 0) return Color.Unspecified
                
                val pixels = IntArray(width * height)
                bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                var bestColor: Int? = null
                var maxSaturation = -1f
                val steps = 5
                for (i in 1 until steps) {
                    for (j in 1 until steps) {
                        val x = (width * i) / steps
                        val y = (height * j) / steps
                        val pixel = pixels[y * width + x]
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(pixel, hsv)
                        val score = hsv[1] * hsv[2]
                        if (score > maxSaturation && hsv[2] > 0.1f && hsv[2] < 0.95f) {
                            maxSaturation = score
                            bestColor = pixel
                        }
                    }
                }
                bestColor ?: pixels[(height/2 * width + width/2).coerceIn(pixels.indices)]
            }

            // Tone down the color to avoid "eye-burning" intensity
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(rawColor, hsv)

            // Cap saturation (max 70%) and brightness (max 80%)
            hsv[1] = hsv[1].coerceAtMost(0.7f)
            hsv[2] = hsv[2].coerceAtMost(0.8f)

            Color(android.graphics.Color.HSVToColor(hsv)).copy(alpha = 1f)
        } catch (_: Exception) {
            Color.Unspecified
        }
    }

    fun getContrastColor(color: Color): Color {
        if (color == Color.Unspecified) return Color.White
        
        // Standard relative luminance formula
        val luminance = 0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue

        // Increased threshold (0.72) to favor white icons on brand colors
        return if (luminance > 0.72) Color.Black else Color.White
    }
}
