package com.xenonware.launcher.ui.res

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.WidgetEditBorder(
    alignment: Alignment,
    onDrag: (Offset) -> Unit
) {
    val handleSize = 14.dp
    val xOffset = when (alignment) {
        Alignment.CenterStart -> -handleSize / 2
        Alignment.CenterEnd -> handleSize / 2
        else -> 0.dp
    }
    val yOffset = when (alignment) {
        Alignment.TopCenter -> -handleSize / 2
        Alignment.BottomCenter -> handleSize / 2
        else -> 0.dp
    }

    Box(
        modifier = Modifier
            .align(alignment)
            .offset(x = xOffset, y = yOffset)
            .size(handleSize)
            .background(colorScheme.primary, CircleShape)
            .border(2.dp, colorScheme.surface, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            }
    )
}