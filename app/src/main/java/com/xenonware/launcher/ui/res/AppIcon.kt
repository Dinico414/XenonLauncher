package com.xenonware.launcher.ui.res

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import com.xenon.mylibrary.values.HugeSpacing
import com.xenon.mylibrary.values.SmallSpacing
import com.xenonware.launcher.model.AppInfo

@Composable
fun AppIcon(
    app: AppInfo,
    iconShape: IconShape,
    showShadow: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = HugeSpacing
) {
    app.icon?.let { icon ->
        val shape = iconShape.getShape()
        Image(
            bitmap = icon.toBitmap().asImageBitmap(),
            contentDescription = app.label,
            modifier = modifier
                .size(size)
                .then(if (showShadow) Modifier.shadow(SmallSpacing, shape) else Modifier)
                .clip(shape)
        )
    }
}
