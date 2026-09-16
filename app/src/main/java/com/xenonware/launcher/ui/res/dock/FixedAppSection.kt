package com.xenonware.launcher.ui.res.dock

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.unit.toSize
import androidx.core.graphics.drawable.toBitmap
import com.xenon.mylibrary.values.ExtraBigBiggerSpacing
import com.xenon.mylibrary.values.ExtraBigSpacing
import com.xenon.mylibrary.values.ExtraLargerSpacing
import com.xenon.mylibrary.values.HugestSpacing
import com.xenon.mylibrary.values.MediumIconSize
import com.xenon.mylibrary.values.MediumLargePadding
import com.xenon.mylibrary.values.MediumSpacer
import com.xenon.mylibrary.values.SmallerSpacer
import com.xenon.mylibrary.values.SmallestStroke
import com.xenonware.launcher.R
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.notification.LauncherNotification
import com.xenonware.launcher.ui.res.notification.NotificationBadge
import com.xenonware.launcher.ui.theme.mainFontFamily
import com.xenonware.launcher.util.LocalDragDropState
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private const val MAX_PINNED = 6

private val ItemSize = ExtraBigBiggerSpacing
private val ItemSpacing = MediumSpacer

private const val GapStiffness = 600f
private const val GapDamping = 1f

private val MaxAutoScroll = 1000.dp

private const val ScrollRampSeconds = 0.11f

private const val TargetHysteresis = 0.18f

private const val IconFadeMs = 170
private const val EdgeFadeMs = 280
private const val EmptyStateFadeMs = 260

private const val CommitTimeoutMs = 600L

private data class DropCommit(
    val orderKey: String,
    val packageName: String,
    val source: Int,
    val target: Int,
    val unpinned: Boolean,
    val app: AppInfo? = null,
)

private fun slotOf(index: Int, sourceIndex: Int): Int =
    if (sourceIndex != -1 && index > sourceIndex) index - 1 else index

private fun gapShift(index: Int, source: Int, target: Int, pitchPx: Float): Float {
    val slot = slotOf(index, source)
    if (target == -1) return (slot - index) * pitchPx
    val finalSlot = if (slot >= target) slot + 1 else slot
    return (finalSlot - index) * pitchPx
}

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
    isAppDrawerVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { if (isExpanded) onOpenDrawer() else onExpand() },
        modifier = modifier.dockSectionSize(isExpanded),
        shape = DockSectionShape,
        color = colorScheme.surfaceContainerLowest.copy(alpha = dockButtonAlpha()),
        contentColor = colorScheme.onSurface,
        border = BorderStroke(SmallestStroke, colorScheme.onSurface.copy(alpha = 0.15f))
    ) {
        if (isExpanded) {
            FixedAppSection(
                apps = apps,
                notifications = notifications,
                badgeType = badgeType,
                onAppClick = onAppClick,
                onPinApp = onPinApp,
                onReorderApp = onReorderApp,
                onUnpinApp = onUnpinApp,
                isAppDrawerVisible = isAppDrawerVisible
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.MoreHoriz,
                    null,
                    modifier = Modifier.size(MediumIconSize)
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
    isAppDrawerVisible: Boolean,
) {
    val dragDropState = LocalDragDropState.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()

    val currentApps by rememberUpdatedState(apps)
    val currentOnPinApp by rememberUpdatedState(onPinApp)
    val currentOnReorderApp by rememberUpdatedState(onReorderApp)
    val currentOnUnpinApp by rememberUpdatedState(onUnpinApp)
    val currentIsAppDrawerVisible by rememberUpdatedState(isAppDrawerVisible)

    val groupedNotifications = remember(notifications) {
        notifications.groupBy { it.packageName }
    }

    val spacingPx = with(density) { ItemSpacing.roundToPx().toFloat() }
    val fallbackItemPx = with(density) { ItemSize.roundToPx().toFloat() }
    val pitchPx = fallbackItemPx + spacingPx

    val edgePx = with(density) { ExtraBigSpacing.toPx() }
    val maxScrollPx = with(density) { MaxAutoScroll.toPx() }
    val fadeWidthPx = with(density) { ExtraLargerSpacing.toPx() }
    val dragThresholdPx = with(density) { ExtraLargerSpacing.toPx() }
    val unpinThresholdPx = with(density) { HugestSpacing.toPx() }

    val gapSpring = remember {
        spring<Float>(dampingRatio = GapDamping, stiffness = GapStiffness)
    }
    val alphaSpec = remember { tween<Float>(IconFadeMs, easing = FastOutSlowInEasing) }
    val fadeEdgeSpec = remember { tween<Float>(EdgeFadeMs, easing = FastOutSlowInEasing) }

    val isDragging = dragDropState.isDragging

    val orderKey = remember(apps) { apps.joinToString("|") { it.packageName } }
    var commit by remember { mutableStateOf<DropCommit?>(null) }

    val activeCommit = commit?.takeIf { it.orderKey == orderKey }

    var rowPos by remember { mutableStateOf(Offset.Zero) }

    var restoreScrollPx by remember { mutableFloatStateOf(-1f) }

    var gestureActive by remember { mutableStateOf(false) }

    LaunchedEffect(orderKey) {
        commit = null
        val px = restoreScrollPx
        if (px >= 0f) {
            restoreScrollPx = -1f
            val idx = (px / pitchPx).toInt().coerceAtLeast(0)
            val off = (px - idx * pitchPx).roundToInt().coerceAtLeast(0)
            listState.scrollToItem(idx, off)
        }
    }
    LaunchedEffect(commit) {
        if (commit != null) {
            delay(CommitTimeoutMs.milliseconds)
            commit = null
        }
    }

    val slotDelta = when {
        isDragging && dragDropState.sourceIndex == -1 ->
            if (dragDropState.targetIndex != -1) 1 else 0

        isDragging -> if (dragDropState.targetIndex == -1) -1 else 0

        activeCommit != null && activeCommit.source == -1 -> 1
        activeCommit != null && activeCommit.unpinned -> -1
        else -> 0
    }
    val contentFits = !listState.canScrollForward && !listState.canScrollBackward
    val centerTarget = if (contentFits) -slotDelta * pitchPx / 2f else 0f
    val centerAnim = animateFloatAsState(
        targetValue = centerTarget,
        animationSpec = gapSpring,
        label = "dockCenterShift"
    )

    LaunchedEffect(isDragging) {
        if (!isDragging) {
            dragDropState.targetIndex = -1
            return@LaunchedEffect
        }

        val source = dragDropState.sourceIndex
        val incomingFromDrawer = source == -1
        if (incomingFromDrawer && apps.size >= MAX_PINNED) {
            dragDropState.targetIndex = -1
            return@LaunchedEffect
        }
        val maxTarget = if (incomingFromDrawer) apps.size else (apps.size - 1).coerceAtLeast(0)

        dragDropState.targetIndex = if (incomingFromDrawer) -1 else source

        var armedStart = false
        var armedEnd = false
        var lastFrame = 0L
        var scrollSpeed = 0f

        while (true) {
            val now = withFrameNanos { it }
            val dt = if (lastFrame == 0L) 0f
            else ((now - lastFrame) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastFrame = now

            val bounds = dragDropState.dockBounds
            if (bounds.width <= 0f) continue
            val finger = dragDropState.dragOffset

            val verticalDist = when {
                finger.y < bounds.top -> bounds.top - finger.y
                finger.y > bounds.bottom -> finger.y - bounds.bottom
                else -> 0f
            }
            val pulledOut = verticalDist > unpinThresholdPx && currentIsAppDrawerVisible

            val x = (finger.x - bounds.left).coerceIn(0f, bounds.width)

            if (x > edgePx) armedStart = true
            if (x < bounds.width - edgePx) armedEnd = true

            if (pulledOut) {
                if (dragDropState.targetIndex != -1) dragDropState.targetIndex = -1
            } else {
                val anchor = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                if (anchor == null) {
                    dragDropState.targetIndex = 0
                } else {
                    val itemPx = anchor.size.toFloat().takeIf { it > 0f } ?: fallbackItemPx
                    val pitch = itemPx + spacingPx

                    val gridStart = anchor.offset - anchor.index * pitch + centerAnim.value
                    val raw = (x - gridStart - itemPx / 2f) / pitch

                    val current = dragDropState.targetIndex
                    val candidate = ceil(raw).toInt().coerceIn(0, maxTarget)

                    val target = when {
                        current == -1 -> candidate
                        candidate > current && raw > current + TargetHysteresis -> candidate
                        candidate < current && raw < current - 1f - TargetHysteresis -> candidate
                        else -> current
                    }.coerceIn(0, maxTarget)

                    if (current != target) dragDropState.targetIndex = target
                }
            }
            val desired = when {
                pulledOut -> 0f

                x < edgePx && armedStart && listState.canScrollBackward -> {
                    val t = ((edgePx - x) / edgePx).coerceIn(0f, 1f)
                    -maxScrollPx * t * t
                }

                x > bounds.width - edgePx && armedEnd && listState.canScrollForward -> {
                    val t = ((x - (bounds.width - edgePx)) / edgePx).coerceIn(0f, 1f)
                    maxScrollPx * t * t
                }

                else -> 0f
            }

            if (dt > 0f) {
                scrollSpeed += (desired - scrollSpeed) * (1f - exp(-dt / ScrollRampSeconds))
                if (abs(scrollSpeed) > 1f) listState.scrollBy(scrollSpeed * dt)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                dragDropState.dockBounds =
                    Rect(coordinates.positionInRoot(), coordinates.size.toSize())
            },
        contentAlignment = Alignment.Center
    ) {

        val showEmptyHint = apps.isEmpty() && !isDragging && commit == null

        Crossfade(
            targetState = showEmptyHint,
            animationSpec = tween(EmptyStateFadeMs, easing = FastOutSlowInEasing),
            label = "dockEmptyState"
        ) { isEmpty ->
            if (isEmpty) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.drag_app_to_pin),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = mainFontFamily,
                        color = LocalContentColor.current.copy(alpha = 0.5f)
                    )
                }
            } else {

                val startFade = animateFloatAsState(
                    targetValue = if (listState.canScrollBackward) 1f else 0f,
                    animationSpec = fadeEdgeSpec,
                    label = "dockFadeStart"
                )
                val endFade = animateFloatAsState(
                    targetValue = if (listState.canScrollForward) 1f else 0f,
                    animationSpec = fadeEdgeSpec,
                    label = "dockFadeEnd"
                )

                LazyRow(
                    state = listState,

                    userScrollEnabled = !gestureActive,
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { rowPos = it.positionInRoot() }
                        .pointerInput(Unit) {
                            var pressPoint = Offset.Zero
                            var moved = Offset.Zero
                            var isActualDrag = false
                            var draggedApp: AppInfo? = null
                            var initialIndex = -1

                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset ->
                                    moved = Offset.Zero
                                    isActualDrag = false
                                    draggedApp = null
                                    initialIndex = -1

                                    val hit = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull {
                                            offset.x >= it.offset && offset.x <= it.offset + it.size
                                        }
                                    val app = hit?.let { currentApps.getOrNull(it.index) }
                                    if (hit != null && app != null) {
                                        draggedApp = app
                                        initialIndex = hit.index
                                        pressPoint = offset
                                        gestureActive = true
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    val app = draggedApp
                                        ?: return@detectDragGesturesAfterLongPress
                                    moved += dragAmount

                                    if (!isActualDrag && moved.getDistance() > dragThresholdPx) {
                                        isActualDrag = true
                                        dragDropState.startDrag(
                                            app, rowPos + pressPoint + moved, initialIndex
                                        )
                                    }
                                    if (isActualDrag) {
                                        change.consume()
                                        dragDropState.dragOffset += dragAmount
                                    }
                                },
                                onDragEnd = {
                                    val app = draggedApp
                                    if (isActualDrag && app != null) {
                                        val finalPos = dragDropState.dragOffset
                                        val sourceIdx = dragDropState.sourceIndex
                                        val targetIdx = dragDropState.targetIndex
                                        val dock = dragDropState.dockBounds

                                        val verticalDist = when {
                                            finalPos.y < dock.top -> dock.top - finalPos.y
                                            finalPos.y > dock.bottom -> finalPos.y - dock.bottom
                                            else -> 0f
                                        }
                                        val isOutside = verticalDist > unpinThresholdPx && currentIsAppDrawerVisible

                                        restoreScrollPx =
                                            if (listState.canScrollForward || listState.canScrollBackward) {
                                                listState.firstVisibleItemIndex * pitchPx +
                                                        listState.firstVisibleItemScrollOffset
                                            } else -1f


                                        commit = DropCommit(
                                            orderKey = orderKey,
                                            packageName = app.packageName,
                                            source = sourceIdx,
                                            target = if (targetIdx == -1) sourceIdx else targetIdx,
                                            unpinned = isOutside && sourceIdx != -1,
                                            app = app
                                        )

                                        when {
                                            isOutside ->
                                                if (sourceIdx != -1) currentOnUnpinApp(app.packageName)

                                            sourceIdx == -1 ->
                                                if (targetIdx != -1) currentOnPinApp(
                                                    app.packageName, targetIdx
                                                )

                                            targetIdx != -1 && targetIdx != sourceIdx ->
                                                currentOnReorderApp(sourceIdx, targetIdx)
                                        }
                                    }
                                    dragDropState.stopDrag()
                                    draggedApp = null
                                    gestureActive = false
                                },
                                onDragCancel = {
                                    val app = draggedApp
                                    if (isActualDrag && app != null) {
                                        // Land in place rather than sliding back.
                                        val sourceIdx = dragDropState.sourceIndex
                                        if (sourceIdx != -1) {
                                            commit = DropCommit(
                                                orderKey = orderKey,
                                                packageName = app.packageName,
                                                source = sourceIdx,
                                                target = sourceIdx,
                                                unpinned = false,
                                                app = app
                                            )
                                        }
                                    }
                                    dragDropState.stopDrag()
                                    draggedApp = null
                                    gestureActive = false
                                }
                            )
                        }

                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            val width = size.width
                            if (width > 0 && fadeWidthPx > 0) {
                                val fadeStop = (fadeWidthPx / width).coerceAtMost(0.5f)
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0f to Color.Black.copy(alpha = 1f - startFade.value),
                                        fadeStop to Color.Black,
                                        (1f - fadeStop) to Color.Black,
                                        1.2f to Color.Black.copy(alpha = 1f - endFade.value)
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                        }
                        .graphicsLayer {
                            translationX = if (isDragging) centerAnim.value else centerTarget
                        },
                    horizontalArrangement = Arrangement.spacedBy(
                        ItemSpacing, Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(horizontal = MediumLargePadding)
                ) {
                    itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                        val iconBitmap = remember(app.packageName, app.icon) {
                            app.icon?.toBitmap()?.asImageBitmap()
                        }

                        val isBeingDragged = isDragging &&
                                app.packageName == dragDropState.draggedApp?.packageName

                        val targetShift: Float
                        val targetAlpha: Float
                        when {
                            isDragging -> {
                                targetAlpha = if (isBeingDragged) 0f else 1f
                                targetShift = if (isBeingDragged) 0f
                                else gapShift(
                                    index,
                                    dragDropState.sourceIndex,
                                    dragDropState.targetIndex,
                                    pitchPx
                                )
                            }
                            activeCommit != null && activeCommit.unpinned -> {
                                targetAlpha = if (app.packageName == activeCommit.packageName) 0f else 1f
                                targetShift = gapShift(index, activeCommit.source, -1, pitchPx)
                            }

                            activeCommit != null -> {
                                targetAlpha = 1f
                                targetShift = if (app.packageName == activeCommit.packageName) {
                                    (activeCommit.target - activeCommit.source) * pitchPx
                                } else {
                                    gapShift(index, activeCommit.source,
                                        activeCommit.target, pitchPx)
                                }
                            }

                            else -> {
                                targetAlpha = 1f
                                targetShift = 0f
                            }
                        }

                        val shift = animateFloatAsState(
                            targetValue = targetShift,
                            animationSpec = gapSpring,
                            label = "dockGapShift"
                        )
                        val itemAlpha = animateFloatAsState(
                            targetValue = targetAlpha,
                            animationSpec = alphaSpec,
                            label = "dockItemAlpha"
                        )

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
                                    .size(ItemSize)
                                    .graphicsLayer {
                                        translationX = if (isDragging) shift.value else targetShift
                                        alpha = if (isDragging) itemAlpha.value else targetAlpha
                                    }
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    iconBitmap?.let { bitmap ->
                                        Image(
                                            bitmap = bitmap,
                                            contentDescription = app.name,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .pointerInput(app.packageName) {
                                                    detectTapGestures(
                                                        onTap = { onAppClick(app.packageName) }
                                                    )
                                                },
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    NotificationBadge(
                                        count = groupedNotifications[app.packageName]?.size ?: 0,
                                        badgeType = badgeType,
                                        appIcon = app.icon,
                                        modifier = Modifier.offset(x = SmallerSpacer, y = (-1) * SmallerSpacer)
                                    )
                                }
                            }
                        }
                    }
                }

                val pinCommit = activeCommit?.takeIf { it.source == -1 && it.app != null }
                if (pinCommit != null) {
                    val phantomIcon = remember(pinCommit.packageName) {
                        pinCommit.app?.icon?.toBitmap()?.asImageBitmap()
                    }
                    val phantomX by remember(pinCommit, pitchPx) {
                        derivedStateOf {
                            val anchor = listState.layoutInfo.visibleItemsInfo.firstOrNull()
                            anchor?.let {
                                val gridStart = it.offset - it.index * pitchPx
                                gridStart + pinCommit.target * pitchPx
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = if (isDragging) centerAnim.value else centerTarget
                            },
                        contentAlignment = if (phantomX == null) Alignment.Center
                        else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(phantomX?.roundToInt() ?: 0, 0) }
                                .size(ItemSize)
                        ) {
                            phantomIcon?.let { bitmap ->
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}