package com.xenonware.launcher.ui.res.notification

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.get
import androidx.palette.graphics.Palette
import com.xenon.mylibrary.theme.QuicksandTitleVariable

@Composable
fun NotificationBadge(
    count: Int,
    badgeType: Int,
    appIcon: Drawable?,
    modifier: Modifier = Modifier,
) {
    if (badgeType == 0 || count <= 0) return

    val dominantColor = remember(appIcon) { getDominantColor(appIcon) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val badgeColor = remember(dominantColor, primaryColor) {
        val base = if (dominantColor == Color.Unspecified) Color.Red else dominantColor
        
        // If extremely dark (like Twitter/X), mix with system primary
        val luminance = 0.2126 * base.red + 0.7152 * base.green + 0.0722 * base.blue
        val mixedBase = if (luminance < 0.15) {
            lerp(base, primaryColor, 0.5f)
        } else {
            base
        }
        
        getTertiaryColor(mixedBase)
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

private fun getTertiaryColor(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    hsv[0] = (hsv[0] + 60f) % 360f // Material 3 tertiary hue shift
    return Color(android.graphics.Color.HSVToColor(hsv))
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
            bitmap[width / 2, height / 2]
        }

        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(rawColor, hsv)
        hsv[1] = hsv[1].coerceAtMost(0.7f)
        hsv[2] = hsv[2].coerceAtMost(0.8f)

        Color(android.graphics.Color.HSVToColor(hsv)).copy(alpha = 1f)
    } catch (_: Exception) {
        Color.Unspecified
    }
}

private fun getContrastColor(color: Color): Color {
    val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
    return if (luminance > 0.5) Color.Black else Color.White
}
