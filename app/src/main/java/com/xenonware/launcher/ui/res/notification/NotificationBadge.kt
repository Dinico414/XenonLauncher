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
import com.xenonware.launcher.util.ColorUtils
import com.xenon.mylibrary.theme.QuicksandTitleVariable

@Composable
fun NotificationBadge(
    count: Int,
    badgeType: Int,
    appIcon: Drawable?,
    modifier: Modifier = Modifier,
) {
    if (badgeType == 0 || count <= 0) return

    val dominantColor = remember(appIcon) { ColorUtils.getDominantColor(appIcon) }
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
                color = ColorUtils.getContrastColor(badgeColor),
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


