package com.xenonware.launcher.ui.res

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.theme.QuicksandTitleVariable

@Composable
fun ContactAvatar(name: String, modifier: Modifier = Modifier) {
    val firstLetter = name.firstOrNull()?.uppercaseChar() ?: '?'

    // Generates a unique pastel color based on the name's hash
    val pastelBackground = remember(name) {
        val hash = name.hashCode()
        val hue = (hash % 360).toFloat().let { if (it < 0) it + 360 else it }
        Color.hsl(hue = hue, saturation = 0.5f, lightness = 0.80f)
    }

    // Generates a darker version of the same hue for the text
    val textColor = remember(name) {
        val hash = name.hashCode()
        val hue = (hash % 360).toFloat().let { if (it < 0) it + 360 else it }
        Color.hsl(hue = hue, saturation = 0.6f, lightness = 0.25f)
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(50))
            .background(pastelBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = firstLetter.toString(),
            fontSize = 28.sp,
            fontFamily = QuicksandTitleVariable,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}
