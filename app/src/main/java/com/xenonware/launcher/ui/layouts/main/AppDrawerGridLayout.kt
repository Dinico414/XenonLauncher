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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.ui.res.MenuItem
import com.xenonware.launcher.ui.res.XenonDropDown
import com.xenonware.launcher.ui.res.notification.NotificationBadge
import com.xenonware.launcher.util.DragDropState
import dev.chrisbanes.haze.HazeState

@Composable
fun AppDrawerGridLayout(
    app: AppInfo,
    notificationCount: Int,
    badgeType: Int,
    onAppClick: (String) -> Unit,
    onDismiss: () -> Unit,
    onPinApp: (String, Int) -> Unit,
    dragDropState: DragDropState,
    hazeState: HazeState,
    onUninstallApp: (String) -> Unit,
    onAppInfo: (String) -> Unit,
    onHideApp: (String) -> Unit,
    onUnhideApp: (String) -> Unit,
    isHidden: Boolean = false,
    iconShape: com.xenonware.launcher.ui.res.IconShape = com.xenonware.launcher.ui.res.IconShape.Circle,
    showShadow: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var itemPos by remember { mutableStateOf(Offset.Zero) }
    var showMenu by remember { mutableStateOf(false) }
    var isActualDrag by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .onGloballyPositioned { itemPos = it.positionInRoot() }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onAppClick(app.packageName)
                        onDismiss()
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(onDragStart = { offset ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isActualDrag = false
                    dragDropState.startDrag(app, itemPos + offset)
                }, onDrag = { change, dragAmount ->
                    if (dragAmount.getDistance() > 1f) isActualDrag = true
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
                    if (isActualDrag) {
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
                    } else {
                        showMenu = true
                    }
                    dragDropState.stopDrag()
                }, onDragCancel = { dragDropState.stopDrag() })
            }) {
        Box(contentAlignment = Alignment.TopEnd) {
            app.icon?.let { icon ->
                val shape = iconShape.getShape()
                Image(
                    bitmap = icon.toBitmap().asImageBitmap(),
                    contentDescription = app.name,
                    modifier = Modifier
                        .size(56.dp)
                        .then(if (showShadow) Modifier.shadow(4.dp, shape) else Modifier)
                        .clip(shape)
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

    XenonDropDown(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        items = listOf(
            MenuItem(
                text = "Uninstall",
                onClick = { onUninstallApp(app.packageName) },
                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                textColor = colorScheme.error,
                containerColor = colorScheme.error.copy(alpha = 0.25f)
            ),
            MenuItem(
                text = "App Info",
                onClick = { onAppInfo(app.packageName) },
                leadingIcon = { Icon(Icons.Rounded.Info, null) }
            ),
            MenuItem(
                text = "Edit",
                onClick = { /* Keep as placeholder */ },
                leadingIcon = { Icon(Icons.Rounded.Edit, null) }
            ),
            MenuItem(
                text = if (isHidden) "Unhide" else "Hide",
                onClick = { if (isHidden) onUnhideApp(app.packageName) else onHideApp(app.packageName) },
                leadingIcon = { Icon(if (isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, null) }
            )
        ),
        hazeState = hazeState,
        offsetY = 0.dp,
        offsetX = 0.dp,
        alignment = Alignment.Center
    )
}
