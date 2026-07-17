package com.xenonware.launcher.ui.res

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.xenon.mylibrary.theme.QuicksandTitleVariable

@Composable
fun NotificationBadge(
    count: Int,
    badgeType: Int,
    appIcon: Drawable?,
    modifier: Modifier = Modifier
) {
    if (badgeType == 0 || count <= 0) return

    val dominantColor = remember(appIcon) { getDominantColor(appIcon) }
    val badgeColor = remember(dominantColor) {
        if (dominantColor == Color.Unspecified) Color.Red else dominantColor
    }

    Box(
        modifier = modifier
            .size(if (badgeType == 2) 24.dp else 18.dp)
            .background(badgeColor, CircleShape)
            .padding(if (badgeType == 2) 0.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        if (badgeType == 2) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                color = getContrastColor(badgeColor),
                fontSize = 12.sp,
                fontFamily = QuicksandTitleVariable,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun getDominantColor(drawable: Drawable?): Color {
    if (drawable == null) return Color.Unspecified
    return try {
        val bitmap = drawable.toBitmap()
        val palette = Palette.from(bitmap).generate()
        val swatch = palette.darkVibrantSwatch
            ?: palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.dominantSwatch

        val rawColor = swatch?.rgb ?: run {
            val width = bitmap.width
            val height = bitmap.height
            bitmap.getPixel(width / 2, height / 2)
        }

        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(rawColor, hsv)
        hsv[1] = hsv[1].coerceAtMost(0.7f)
        hsv[2] = hsv[2].coerceAtMost(0.8f)

        Color(android.graphics.Color.HSVToColor(hsv)).copy(alpha = 1f)
    } catch (e: Exception) {
        Color.Unspecified
    }
}

private fun getContrastColor(color: Color): Color {
    val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
    return if (luminance > 0.5) Color.Black else Color.White
}
