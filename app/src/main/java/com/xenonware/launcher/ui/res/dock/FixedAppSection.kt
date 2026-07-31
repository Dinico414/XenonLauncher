package com.xenonware.launcher.ui.res.dock

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.core.graphics.drawable.toBitmap
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.notification.LauncherNotification
import com.xenonware.launcher.ui.res.notification.NotificationBadge
import com.xenonware.launcher.util.LocalDragDropState
import kotlinx.coroutines.delay
import kotlin.math.ceil

private const val MAX_PINNED = 6

private val ItemSize = 44.dp
private val ItemSpacing = 8.dp

/** How far past a slot boundary the finger must travel before the gap moves. */
private const val TargetHysteresis = 0.18f

/** Peak auto-scroll speed, in dp per second. */
private val MaxAutoScroll = 1100.dp

/** Give up on a commit that never arrives and snap back to the real order. */
private const val CommitTimeoutMs = 600L

/**
 * The visual state of a finished drop, held from the moment the finger lifts
 * until the reordered [AppInfo] list actually arrives from upstream.
 *
 * Without this, releasing runs two animations back to back: the gap unwinds to
 * the OLD layout, then `animateItem` walks everything to the NEW one. Freezing
 * the picture until the data catches up means the release is instant.
 */
private data class DropCommit(
    /** Order at the instant of the drop; the commit goes inert once it changes. */
    val orderKey: String,
    val packageName: String,
    val source: Int,
    val target: Int,
    val unpinned: Boolean,
)

/** Slot of [index] in the list *without* the dragged item. */
private fun slotOf(index: Int, sourceIndex: Int): Int =
    if (sourceIndex != -1 && index > sourceIndex) index - 1 else index

/** Visual offset for a non-dragged item while a gap sits open at [target]. */
private fun gapShift(index: Int, source: Int, target: Int, pitchPx: Float): Float {
    val slot = slotOf(index, source)
    val finalSlot = if (slot >= target) slot + 1 else slot
    return (finalSlot - index) * pitchPx
}

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

    // roundToPx, not toPx: the slot pitch must match what the layout actually
    // produced, or the gap drifts by a fraction of a pixel per item.
    val spacingPx = with(density) { ItemSpacing.roundToPx().toFloat() }
    val fallbackItemPx = with(density) { ItemSize.roundToPx().toFloat() }
    val pitchPx = fallbackItemPx + spacingPx

    val edgePx = with(density) { 40.dp.toPx() }
    val maxScrollPx = with(density) { MaxAutoScroll.toPx() }
    val fadeWidthPx = with(density) { 24.dp.toPx() }
    val dragThresholdPx = with(density) { 24.dp.toPx() }
    val unpinThresholdPx = with(density) { 80.dp.toPx() }

    // Live gap movement. A spring carries velocity through target changes, so a
    // fast sweep flows instead of restarting a tween per slot.
    val gapSpring = remember { spring<Float>(dampingRatio = 0.9f, stiffness = 700f) }
    // Everything outside an active drag is instant: a release should land, not travel.
    val instant = remember { snap<Float>() }

    val alphaSpec = remember { tween<Float>(140, easing = FastOutSlowInEasing) }
    val fadeEdgeSpec = remember { tween<Float>(220, easing = FastOutSlowInEasing) }
    // Only for changes that did NOT come from a drop: an app installed, removed
    // elsewhere, or the gap closing after an unpin.
    val placementSpec = remember { tween<IntOffset>(300, easing = FastOutSlowInEasing) }

    val orderKey = remember(apps) { apps.joinToString("|") { it.packageName } }
    var commit by remember { mutableStateOf<DropCommit?>(null) }

    // The commit stops applying in the very same composition that first sees the
    // new order -- shifts drop to zero and the layout moves in one atomic step,
    // so there is no frame where both are applied.
    val activeCommit = commit?.takeIf { it.orderKey == orderKey }

    // A reorder must not animate: the icons are already where they belong. An
    // unpin still animates, because the remaining icons genuinely need to close.
    val suppressPlacement = dragDropState.isDragging || (commit?.unpinned == false)

    LaunchedEffect(orderKey) { commit = null }
    LaunchedEffect(commit) {
        if (commit != null) {
            delay(CommitTimeoutMs)
            commit = null // upstream never applied the change; fall back to truth
        }
    }

    // One frame loop drives both target index and auto-scroll.
    LaunchedEffect(dragDropState.isDragging) {
        if (!dragDropState.isDragging) {
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
        val restingTarget = if (incomingFromDrawer) -1 else source

        // Start with the item in its own slot so nothing twitches on pick-up.
        dragDropState.targetIndex = restingTarget

        var armedStart = false
        var armedEnd = false
        var lastFrame = 0L

        while (true) {
            val now = withFrameNanos { it }
            // Time-based, so scrolling runs at the same speed on 60/90/120Hz
            // panels. Capped so a dropped frame can't produce a huge jump.
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
            if (verticalDist > unpinThresholdPx) {
                dragDropState.targetIndex = restingTarget
                continue
            }

            val x = (finger.x - bounds.left).coerceIn(0f, bounds.width)

            if (x > edgePx) armedStart = true
            if (x < bounds.width - edgePx) armedEnd = true

            val anchor = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            if (anchor == null) {
                dragDropState.targetIndex = 0
            } else {
                val itemPx = anchor.size.toFloat().takeIf { it > 0f } ?: fallbackItemPx
                val pitch = itemPx + spacingPx
                val gridStart = anchor.offset - anchor.index * pitch
                // Continuous slot position: target == ceil(raw).
                val raw = (x - gridStart - itemPx / 2f) / pitch

                val current = dragDropState.targetIndex
                val candidate = ceil(raw).toInt().coerceIn(0, maxTarget)
                // Hysteresis: hovering exactly on a boundary no longer makes the
                // gap flicker between two slots.
                val target = when {
                    current == -1 -> candidate
                    candidate > current && raw > current + TargetHysteresis -> candidate
                    candidate < current && raw < current - 1f - TargetHysteresis -> candidate
                    else -> current
                }.coerceIn(0, maxTarget)

                if (current != target) dragDropState.targetIndex = target
            }

            // Quadratic ramp with no minimum speed: the scroll eases in from a
            // standstill as you approach the edge instead of snapping to 1px.
            val speed = when {
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
            if (speed != 0f && dt > 0f) listState.scrollBy(speed * dt)
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
        val showEmptyHint = apps.isEmpty() && !dragDropState.isDragging

        Crossfade(
            targetState = showEmptyHint,
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            label = "dockEmptyState"
        ) { isEmpty ->
            if (isEmpty) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Drag App to pin",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = LocalContentColor.current.copy(alpha = 0.5f)
                    )
                }
            } else {
                // No `by`: these are read inside the draw block so the edge fades
                // animate on the draw phase only, without recomposing the row.
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
                                        0f to Color.Black.copy(alpha = 1f - startFade.value),
                                        fadeStop to Color.Black,
                                        (1f - fadeStop) to Color.Black,
                                        1.2f to Color.Black.copy(alpha = 1f - endFade.value)
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(
                        ItemSpacing, Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    // `apps` is never reordered while dragging, so keys and indices
                    // stay put and LazyRow never scroll-corrects to keep its anchor
                    // key in place. The gap is drawn, not laid out.
                    itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                        var itemPos by remember { mutableStateOf(Offset.Zero) }

                        val isDragging = dragDropState.isDragging
                        val isBeingDragged = isDragging &&
                                app.packageName == dragDropState.draggedApp?.packageName

                        val held = activeCommit
                        val targetShift: Float
                        val targetAlpha: Float
                        when {
                            isDragging -> {
                                targetAlpha = if (isBeingDragged) 0f else 1f
                                val t = dragDropState.targetIndex
                                targetShift = if (isBeingDragged || t == -1) 0f
                                else gapShift(index, dragDropState.sourceIndex, t, pitchPx)
                            }
                            // Held after release: keep the exact picture the drag
                            // ended on, with the dropped icon planted in its new
                            // slot, until the real list catches up.
                            held != null && held.unpinned -> {
                                targetAlpha = if (app.packageName == held.packageName) 0f else 1f
                                targetShift = 0f
                            }

                            held != null -> {
                                targetAlpha = 1f
                                targetShift = if (app.packageName == held.packageName) {
                                    (held.target - held.source) * pitchPx
                                } else {
                                    gapShift(index, held.source, held.target, pitchPx)
                                }
                            }

                            else -> {
                                targetAlpha = 1f
                                targetShift = 0f
                            }
                        }

                        // Reading these inside graphicsLayer keeps the animation on
                        // the draw phase: no recomposition, no relayout per frame.
                        val shift = animateFloatAsState(
                            targetValue = targetShift,
                            animationSpec = if (isDragging) gapSpring else instant,
                            label = "dockGapShift"
                        )
                        val itemAlpha = animateFloatAsState(
                            targetValue = targetAlpha,
                            animationSpec = if (isDragging) alphaSpec else instant,
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
                                    .onGloballyPositioned { itemPos = it.positionInRoot() }
                                    .animateItem(
                                        fadeInSpec = if (suppressPlacement) null
                                        else tween(220, easing = FastOutSlowInEasing),
                                        placementSpec = if (suppressPlacement) null else placementSpec,
                                        fadeOutSpec = if (suppressPlacement) null
                                        else tween(160, easing = FastOutSlowInEasing)
                                    )
                                    .graphicsLayer {
                                        translationX = shift.value
                                        alpha = itemAlpha.value
                                    }
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    app.icon?.let { icon ->
                                        Image(
                                            bitmap = icon.toBitmap().asImageBitmap(),
                                            contentDescription = app.name,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .pointerInput(app.packageName) {
                                                    detectTapGestures(onTap = { onAppClick(app.packageName) })
                                                }
                                                .pointerInput(app.packageName, index) {
                                                    var pressOffset = Offset.Zero
                                                    var moved = Offset.Zero
                                                    var isActualDrag = false

                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { offset ->
                                                            pressOffset = offset
                                                            moved = Offset.Zero
                                                            isActualDrag = false
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            moved += dragAmount
                                                            // Net displacement, not path length --
                                                            // jiggling in place won't start a drag.
                                                            if (!isActualDrag &&
                                                                moved.getDistance() > dragThresholdPx
                                                            ) {
                                                                isActualDrag = true
                                                                dragDropState.startDrag(
                                                                    app,
                                                                    itemPos + pressOffset + moved,
                                                                    index
                                                                )
                                                            }
                                                            if (isActualDrag) {
                                                                change.consume()
                                                                dragDropState.dragOffset += dragAmount
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            if (isActualDrag) {
                                                                val finalPos = dragDropState.dragOffset
                                                                val sourceIdx = dragDropState.sourceIndex
                                                                val targetIdx = dragDropState.targetIndex
                                                                val dock = dragDropState.dockBounds

                                                                val verticalDist = when {
                                                                    finalPos.y < dock.top -> dock.top - finalPos.y
                                                                    finalPos.y > dock.bottom -> finalPos.y - dock.bottom
                                                                    else -> 0f
                                                                }
                                                                val isOutside =
                                                                    verticalDist > unpinThresholdPx

                                                                // Freeze the picture BEFORE stopping
                                                                // the drag, so the release frame is
                                                                // continuous with the drag frame.
                                                                commit = DropCommit(
                                                                    orderKey = orderKey,
                                                                    packageName = app.packageName,
                                                                    source = sourceIdx,
                                                                    target = if (targetIdx == -1) sourceIdx else targetIdx,
                                                                    unpinned = isOutside && sourceIdx != -1
                                                                )

                                                                when {
                                                                    isOutside ->
                                                                        if (sourceIdx != -1) onUnpinApp(app.packageName)

                                                                    sourceIdx == -1 ->
                                                                        if (targetIdx != -1) onPinApp(
                                                                            app.packageName, targetIdx
                                                                        )

                                                                    targetIdx != -1 && targetIdx != sourceIdx ->
                                                                        onReorderApp(sourceIdx, targetIdx)
                                                                }
                                                            }
                                                            dragDropState.stopDrag()
                                                        },
                                                        onDragCancel = {
                                                            // Land in place rather than sliding back.
                                                            val sourceIdx = dragDropState.sourceIndex
                                                            if (isActualDrag && sourceIdx != -1) {
                                                                commit = DropCommit(
                                                                    orderKey = orderKey,
                                                                    packageName = app.packageName,
                                                                    source = sourceIdx,
                                                                    target = sourceIdx,
                                                                    unpinned = false
                                                                )
                                                            }
                                                            dragDropState.stopDrag()
                                                        }
                                                    )
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
}