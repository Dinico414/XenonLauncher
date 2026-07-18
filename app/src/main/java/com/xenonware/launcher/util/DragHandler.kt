package com.xenonware.launcher.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.xenonware.launcher.model.AppInfo
import kotlin.math.roundToInt

// Custom Drag and Drop implementation for Launcher
class DragDropState {
    var draggedApp by mutableStateOf<AppInfo?>(null)
    var dragOffset by mutableStateOf(Offset.Zero)
    var isDragging by mutableStateOf(false)
    var sourceIndex by mutableIntStateOf(-1)
    var targetIndex by mutableIntStateOf(-1)

    // Position of the dock to detect drops
    var dockBounds by mutableStateOf(Rect.Zero)

    fun startDrag(app: AppInfo, offset: Offset, index: Int = -1) {
        draggedApp = app
        dragOffset = offset
        isDragging = true
        sourceIndex = index
        targetIndex = index
    }

    fun stopDrag() {
        draggedApp = null
        isDragging = false
        sourceIndex = -1
        targetIndex = -1
    }
}

val LocalDragDropState = staticCompositionLocalOf { DragDropState() }

@Composable
fun DragHandler(
    modifier: Modifier = Modifier,
    state: DragDropState = LocalDragDropState.current,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (state.isDragging && state.draggedApp != null) {
            val app = state.draggedApp!!
            val density = LocalDensity.current

            Box(modifier = Modifier
                .offset {
                    IntOffset(
                        state.dragOffset.x.roundToInt() - with(density) { 28.dp.toPx() }.roundToInt(),
                        state.dragOffset.y.roundToInt() - with(density) { 28.dp.toPx() }.roundToInt()
                    )
                }
                .size(56.dp)
                .scale(1.1f)
                .alpha(0.9f)) {
                app.icon?.let { icon ->
                    Image(
                        bitmap = icon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}