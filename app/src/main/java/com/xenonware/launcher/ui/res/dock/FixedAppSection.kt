package com.xenonware.launcher.ui.res.dock

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.core.graphics.drawable.toBitmap
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.notification.LauncherNotification
import com.xenonware.launcher.ui.res.notification.NotificationBadge
import com.xenonware.launcher.util.LocalDragDropState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/**
 * The middle dock section: pinned apps when expanded, a "more" affordance when
 * collapsed. Tapping it while expanded opens the app drawer.
 */
@Composable
fun AppsSection(
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onOpenDrawer: () -> Unit,
    apps: List<AppInfo>,
    notifications: List<LauncherNotification>,
    badgeType: Int,
    onAppClick: (String) -> Unit,
    onPinApp: (String, Int) -> Unit,
    onReorderApp: (Int, Int) -> Unit,
    onUnpinApp: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { if (isExpanded) onOpenDrawer() else onExpand() },
        modifier = modifier.dockSectionSize(isExpanded),
        shape = DockSectionShape,
        color = colorScheme.surfaceContainerLowest.copy(alpha = dockButtonAlpha()),
        contentColor = colorScheme.onSurface
    ) {
        if (isExpanded) {
            FixedAppSection(
                apps = apps,
                notifications = notifications,
                badgeType = badgeType,
                onAppClick = onAppClick,
                onPinApp = onPinApp,
                onReorderApp = onReorderApp,
                onUnpinApp = onUnpinApp
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.MoreHoriz,
                    null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun FixedAppSection(
    apps: List<AppInfo>,
    notifications: List<LauncherNotification>,
    badgeType: Int,
    onAppClick: (String) -> Unit,
    onPinApp: (String, Int) -> Unit,
    onReorderApp: (Int, Int) -> Unit,
    onUnpinApp: (String) -> Unit,
) {
    val dragDropState = LocalDragDropState.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()

    val groupedNotifications = remember(notifications) {
        notifications.groupBy { it.packageName }
    }

    val itemWidthPx = with(density) { 52.dp.toPx() }
    val contentPaddingPx = with(density) { 10.dp.toPx() }
    val spacingPx = with(density) { 8.dp.toPx() }

    // Unified target index calculation
    LaunchedEffect(
        dragDropState.isDragging,
        dragDropState.dragOffset,
        listState.firstVisibleItemIndex,
        listState.firstVisibleItemScrollOffset
    ) {
        if (dragDropState.isDragging && dragDropState.dockBounds.contains(dragDropState.dragOffset)) {
            val relativeX = dragDropState.dragOffset.x - dragDropState.dockBounds.left
            val viewportWidth = dragDropState.dockBounds.width
            val baseSize = if (dragDropState.sourceIndex == -1) apps.size else apps.size - 1

            // Total width including padding and spacing
            val totalContentWidth =
                (baseSize + 1) * itemWidthPx - spacingPx + (contentPaddingPx * 2)
            val scrollPos =
                listState.firstVisibleItemIndex * itemWidthPx + listState.firstVisibleItemScrollOffset

            val contentStart = if (totalContentWidth < viewportWidth) {
                (viewportWidth - totalContentWidth) / 2f + contentPaddingPx
            } else {
                contentPaddingPx - scrollPos
            }

            val xInContent = relativeX - contentStart
            val newTarget = (xInContent / itemWidthPx).roundToInt().coerceIn(0, baseSize)

            if (dragDropState.targetIndex != newTarget) {
                dragDropState.targetIndex = newTarget
            }
        } else if (!dragDropState.isDragging) {
            dragDropState.targetIndex = -1
        }
    }

    // Elegant Auto-scroll logic
    LaunchedEffect(dragDropState.isDragging) {
        if (dragDropState.isDragging) {
            while (dragDropState.isDragging) {
                if (dragDropState.dockBounds.contains(dragDropState.dragOffset)) {
                    val viewportWidth = dragDropState.dockBounds.width
                    val dragX = dragDropState.dragOffset.x - dragDropState.dockBounds.left
                    val edgeThreshold = with(density) { 40.dp.toPx() }

                    if (dragX < edgeThreshold && listState.canScrollBackward) {
                        val speed =
                            ((edgeThreshold - dragX) / edgeThreshold * 15f).coerceIn(1f, 15f)
                        listState.scrollBy(-speed)
                    } else if (dragX > viewportWidth - edgeThreshold && listState.canScrollForward) {
                        val speed =
                            ((dragX - (viewportWidth - edgeThreshold)) / edgeThreshold * 15f).coerceIn(
                                1f, 15f
                            )
                        listState.scrollBy(speed)
                    }
                }
                delay(16.milliseconds)
            }
        }
    }

    // Display list that handles the visual "push" during drag
    val displayApps = remember(
        apps, dragDropState.isDragging, dragDropState.targetIndex, dragDropState.sourceIndex
    ) {
        if (!dragDropState.isDragging || dragDropState.targetIndex == -1) {
            apps
        } else {
            val list = apps.toMutableList()
            val draggedApp = dragDropState.draggedApp ?: return@remember apps

            list.removeAll { it.packageName == draggedApp.packageName }

            if (dragDropState.sourceIndex == -1 && list.size >= 6) return@remember apps

            val insertPos = dragDropState.targetIndex.coerceIn(0, list.size)
            list.add(insertPos, draggedApp)
            list
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                dragDropState.dockBounds = coordinates.positionInRoot().let { pos ->
                    androidx.compose.ui.geometry.Rect(pos, coordinates.size.toSize())
                }
            }, contentAlignment = Alignment.Center
    ) {
        if (apps.isEmpty() && !dragDropState.isDragging) {
            Text(
                "Drag App to pin",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = LocalContentColor.current.copy(alpha = 0.5f)
            )
        } else {
            val fadeWidthPx = with(density) { 24.dp.toPx() }
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        val width = size.width
                        if (width > 0 && fadeWidthPx > 0) {
                            val fadeStop = (fadeWidthPx / width).coerceAtMost(0.5f)
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    0f to if (listState.canScrollBackward) Color.Transparent else Color.Black,
                                    fadeStop to Color.Black,
                                    (1f - fadeStop) to Color.Black,
                                    1.2f to if (listState.canScrollForward) Color.Transparent else Color.Black
                                ), blendMode = BlendMode.DstIn
                            )
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                itemsIndexed(displayApps, key = { _, app -> app.packageName }) { _, app ->
                    var itemPos by remember { mutableStateOf(Offset.Zero) }
                    var pressOffset by remember { mutableStateOf(Offset.Zero) }
                    var isActualDrag by remember { mutableStateOf(false) }
                    val isBeingDragged = dragDropState.isDragging && app == dragDropState.draggedApp

                    val viewConfiguration = LocalViewConfiguration.current
                    val customViewConfiguration = remember(viewConfiguration) {
                        object : ViewConfiguration by viewConfiguration {
                            override val touchSlop: Float
                                get() = viewConfiguration.touchSlop * 3f
                        }
                    }

                    CompositionLocalProvider(LocalViewConfiguration provides customViewConfiguration) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .onGloballyPositioned { itemPos = it.positionInRoot() }
                                .animateItem(
                                    placementSpec = tween(
                                        durationMillis = 400, easing = FastOutSlowInEasing
                                    )
                                )
                                .graphicsLayer {
                                    alpha = if (isBeingDragged) 0f else 1f
                                }) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                app.icon?.let { icon ->
                                    Image(
                                        bitmap = icon.toBitmap().asImageBitmap(),
                                        contentDescription = app.name,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .pointerInput(Unit) {
                                                detectTapGestures(onTap = { onAppClick(app.packageName) })
                                            }
                                            .pointerInput(Unit) {
                                                var totalDragDistance = 0f
                                                detectDragGesturesAfterLongPress(onDragStart = { offset ->
                                                    pressOffset = offset
                                                    isActualDrag = false
                                                    totalDragDistance = 0f
                                                }, onDrag = { change, dragAmount ->
                                                    totalDragDistance += dragAmount.getDistance()
                                                    val threshold = with(density) { 24.dp.toPx() }

                                                    if (totalDragDistance > threshold && !isActualDrag) {
                                                        isActualDrag = true
                                                        val originalIndex = apps.indexOf(app)
                                                        dragDropState.startDrag(
                                                            app, itemPos + pressOffset, originalIndex
                                                        )
                                                    }

                                                    if (isActualDrag) {
                                                        change.consume()
                                                        dragDropState.dragOffset += dragAmount
                                                    }
                                                }, onDragEnd = {
                                                    if (isActualDrag) {
                                                        val finalPos = dragDropState.dragOffset
                                                        val sourceIdx = dragDropState.sourceIndex
                                                        val targetIdx = dragDropState.targetIndex

                                                        val verticalDist =
                                                            if (finalPos.y < dragDropState.dockBounds.top) {
                                                                dragDropState.dockBounds.top - finalPos.y
                                                            } else if (finalPos.y > dragDropState.dockBounds.bottom) {
                                                                finalPos.y - dragDropState.dockBounds.bottom
                                                            } else 0f

                                                        val unpinThreshold =
                                                            with(density) { 80.dp.toPx() }
                                                        val isOutside =
                                                            !dragDropState.dockBounds.contains(finalPos) && verticalDist > unpinThreshold

                                                        if (isOutside) {
                                                            if (sourceIdx != -1) {
                                                                onUnpinApp(app.packageName)
                                                            }
                                                        } else {
                                                            if (sourceIdx == -1) {
                                                                if (targetIdx != -1) {
                                                                    onPinApp(
                                                                        app.packageName,
                                                                        targetIdx
                                                                    )
                                                                }
                                                            } else if (targetIdx != -1 && targetIdx != sourceIdx) {
                                                                onReorderApp(sourceIdx, targetIdx)
                                                            }
                                                        }
                                                    }
                                                    dragDropState.stopDrag()
                                                }, onDragCancel = { dragDropState.stopDrag() })
                                            },
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                NotificationBadge(
                                    count = groupedNotifications[app.packageName]?.size ?: 0,
                                    badgeType = badgeType,
                                    appIcon = app.icon,
                                    modifier = Modifier.offset(x = 2.dp, y = (-2).dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}