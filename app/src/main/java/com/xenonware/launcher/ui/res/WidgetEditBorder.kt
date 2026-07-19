package com.xenonware.launcher.ui.res

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
    val handleThickness = 48.dp
    val indicatorSize = 14.dp
    
    val touchAreaModifier = when (alignment) {
        Alignment.TopCenter, Alignment.BottomCenter -> {
            Modifier
                .align(alignment)
                .fillMaxWidth()
                .height(handleThickness)
        }
        Alignment.CenterStart, Alignment.CenterEnd -> {
            Modifier
                .align(alignment)
                .fillMaxHeight()
                .width(handleThickness)
        }
        else -> Modifier.align(alignment)
    }

    Box(
        modifier = touchAreaModifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            }
    ) {
        // Visual circular indicator from before
        Box(
            modifier = Modifier
                .align(alignment)
                .offset(
                    x = when (alignment) {
                        Alignment.CenterStart -> -indicatorSize / 2
                        Alignment.CenterEnd -> indicatorSize / 2
                        else -> 0.dp
                    },
                    y = when (alignment) {
                        Alignment.TopCenter -> -indicatorSize / 2
                        Alignment.BottomCenter -> indicatorSize / 2
                        else -> 0.dp
                    }
                )
                .size(indicatorSize)
                .background(colorScheme.primary, CircleShape)
                .border(2.dp, colorScheme.surface, CircleShape)
        )
    }
}