package com.xenonware.launcher.ui.res

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.xenonware.launcher.model.AppInfo

@Composable
fun AppIcon(
    app: AppInfo,
    iconShape: IconShape,
    showShadow: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    app.icon?.let { icon ->
        val shape = iconShape.getShape()
        Image(
            bitmap = icon.toBitmap().asImageBitmap(),
            contentDescription = app.label,
            modifier = modifier
                .size(size)
                .then(if (showShadow) Modifier.shadow(4.dp, shape) else Modifier)
                .clip(shape)
        )
    }
}
