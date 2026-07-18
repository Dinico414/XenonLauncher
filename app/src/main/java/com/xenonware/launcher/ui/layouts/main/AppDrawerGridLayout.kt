package com.xenonware.launcher.ui.layouts.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.ui.res.notification.NotificationBadge
import com.xenonware.launcher.util.DragDropState

@Composable
fun AppDrawerGridLayout(
    app: AppInfo,
    notificationCount: Int,
    badgeType: Int,
    onAppClick: (String) -> Unit,
    onDismiss: () -> Unit,
    onPinApp: (String, Int) -> Unit,
    dragDropState: DragDropState,
    modifier: Modifier = Modifier,
) {
    var itemPos by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .onGloballyPositioned { itemPos = it.positionInRoot() }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onAppClick(app.packageName)
                        onDismiss()
                    })
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(onDragStart = { offset ->
                    dragDropState.startDrag(app, itemPos + offset)
                }, onDrag = { change, dragAmount ->
                    change.consume()
                    dragDropState.dragOffset += dragAmount

                    if (dragDropState.dockBounds.contains(dragDropState.dragOffset)) {
                        val relativeX = dragDropState.dragOffset.x - dragDropState.dockBounds.left
                        val itemWidth = with(density) { 52.dp.toPx() }
                        dragDropState.targetIndex = (relativeX / itemWidth).toInt().coerceIn(0, 100)
                    } else {
                        dragDropState.targetIndex = -1
                    }
                }, onDragEnd = {
                    val finalPos = dragDropState.dragOffset
                    val verticalDist = if (finalPos.y < dragDropState.dockBounds.top) {
                        dragDropState.dockBounds.top - finalPos.y
                    } else if (finalPos.y > dragDropState.dockBounds.bottom) {
                        finalPos.y - dragDropState.dockBounds.bottom
                    } else 0f

                    val hitThreshold = with(density) { 80.dp.toPx() }

                    if (dragDropState.dockBounds.contains(finalPos) || verticalDist < hitThreshold) {
                        onPinApp(app.packageName, dragDropState.targetIndex)
                    }
                    dragDropState.stopDrag()
                }, onDragCancel = { dragDropState.stopDrag() })
            }) {
        Box(contentAlignment = Alignment.TopEnd) {
            app.icon?.let { icon ->
                Image(
                    bitmap = icon.toBitmap().asImageBitmap(),
                    contentDescription = app.name,
                    modifier = Modifier.size(56.dp)
                )
            }
            NotificationBadge(
                count = notificationCount,
                badgeType = badgeType,
                appIcon = app.icon,
                modifier = Modifier.offset(x = 2.dp, y = (-2).dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            app.name,
            color = colorScheme.onSurface,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}