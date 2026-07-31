package com.xenonware.launcher.ui.res.dock

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.roundToInt

private const val MAX_PINNED = 6

private val ItemSize = 44.dp
private val ItemSpacing = 8.dp

// ---------------------------------------------------------------------------
// Motion tuning. Everything that moves is driven from here.
//
// Deliberately absent: any easing on the drop itself, and any use of
// Modifier.animateItem. Layout changes land instantly; the only thing that
// animates is the gap following your finger during a drag.
// ---------------------------------------------------------------------------

/** Gap glide while dragging. Lower stiffness = softer; damping 1f = no overshoot. */
private const val GapStiffness = 600f
private const val GapDamping = 1f

/** Peak auto-scroll speed, in dp per second. */
private val MaxAutoScroll = 1000.dp

/** Auto-scroll ease in/out time constant, in seconds. Higher = lazier. */
private const val ScrollRampSeconds = 0.11f

/** How far past a slot boundary the finger must travel before the gap moves. */
private const val TargetHysteresis = 0.18f

private const val IconFadeMs = 170
private const val EdgeFadeMs = 280
private const val EmptyStateFadeMs = 260

/** Give up on a commit that never arrives and snap back to the real order. */
private const val CommitTimeoutMs = 600L

/**
 * The visual state of a finished drop, held from the moment the finger lifts
 * until the reordered [AppInfo] list actually arrives from upstream.
 *
 * Without this, releasing runs two animations back to back: the gap unwinds to
 * the OLD layout, then the layout walks everything to the NEW one. Freezing the
 * picture until the data catches up means the release is instant.
 */
private data class DropCommit(
    /** Order at the instant of the drop; the commit goes inert once it changes. */
    val orderKey: String,
    val packageName: String,
    val source: Int,
    val target: Int,
    val unpinned: Boolean,
    val app: AppInfo? = null,
)

/** Slot of [index] in the list *without* the dragged item. */
private fun slotOf(index: Int, sourceIndex: Int): Int =
    if (sourceIndex != -1 && index > sourceIndex) index - 1 else index

/**
 * Visual offset for a non-dragged item while a gap sits open at [target].
 * A [target] of -1 means no gap at all: the row closes up completely, which is
 * the preview for "this app is about to be unpinned".
 */
private fun gapShift(index: Int, source: Int, target: Int, pitchPx: Float): Float {
    val slot = slotOf(index, source)
    if (target == -1) return (slot - index) * pitchPx
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

    val currentApps by rememberUpdatedState(apps)
    val currentOnPinApp by rememberUpdatedState(onPinApp)
    val currentOnReorderApp by rememberUpdatedState(onReorderApp)
    val currentOnUnpinApp by rememberUpdatedState(onUnpinApp)

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
    val gapSpring = remember {
        spring<Float>(dampingRatio = GapDamping, stiffness = GapStiffness)
    }
    val alphaSpec = remember { tween<Float>(IconFadeMs, easing = FastOutSlowInEasing) }
    val fadeEdgeSpec = remember { tween<Float>(EdgeFadeMs, easing = FastOutSlowInEasing) }

    val isDragging = dragDropState.isDragging

    val orderKey = remember(apps) { apps.joinToString("|") { it.packageName } }
    var commit by remember { mutableStateOf<DropCommit?>(null) }

    // The commit stops applying in the very same composition that first sees the
    // new order -- shifts drop to zero and the layout moves in one atomic step,
    // so there is no frame where both are applied.
    val activeCommit = commit?.takeIf { it.orderKey == orderKey }

    var rowPos by remember { mutableStateOf(Offset.Zero) }
    // Scroll offset in absolute pixels, captured at the drop. LazyList re-anchors
    // its scroll to the first visible KEY when the order changes, which shunts the
    // viewport sideways by an item; this puts the pixels back where they were.
    // Left at -1 when the row cannot scroll, so no needless remeasure is forced.
    var restoreScrollPx by remember { mutableStateOf(-1f) }
    // True from the long press until the finger lifts. While set, LazyRow's own
    // scroll gesture is disabled so it cannot consume -- and thereby cancel -- the
    // drag. Programmatic scrollBy (the auto-scroll) is unaffected.
    var gestureActive by remember { mutableStateOf(false) }

    LaunchedEffect(orderKey) {
        commit = null
        val px = restoreScrollPx
        if (px >= 0f) {
            restoreScrollPx = -1f
            val idx = (px / pitchPx).toInt().coerceAtLeast(0)
            val off = (px - idx * pitchPx).roundToInt().coerceAtLeast(0)
            // On Foundation 1.8+, listState.requestScrollToItem(idx, off) applies
            // this during the next measure instead, removing even the one-frame
            // window where the re-anchored position could be visible.
            listState.scrollToItem(idx, off)
        }
    }
    LaunchedEffect(commit) {
        if (commit != null) {
            delay(CommitTimeoutMs)
            commit = null // upstream never applied the change; fall back to truth
        }
    }

    // How many slots the final layout will differ by, versus what is laid out now.
    val slotDelta = when {
        isDragging && dragDropState.sourceIndex == -1 ->
            if (dragDropState.targetIndex != -1) 1 else 0

        isDragging -> if (dragDropState.targetIndex == -1) -1 else 0

        activeCommit != null && activeCommit.source == -1 -> 1
        activeCommit != null && activeCommit.unpinned -> -1
        else -> 0
    }
    // Arrangement.CenterHorizontally re-centres when the item count changes: one
    // more item puts the row's left edge half a pitch further LEFT, one fewer puts
    // it half a pitch RIGHT. Preview that, so the commit is a no-op visually.
    // Only applies while the content actually fits -- once it scrolls, nothing centres.
    val contentFits = !listState.canScrollForward && !listState.canScrollBackward
    val centerTarget = if (contentFits) -slotDelta * pitchPx / 2f else 0f
    val centerAnim = animateFloatAsState(
        targetValue = centerTarget,
        animationSpec = gapSpring,
        label = "dockCenterShift"
    )

    // One frame loop drives both target index and auto-scroll.
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

        // Start with the item in its own slot so nothing twitches on pick-up.
        dragDropState.targetIndex = if (incomingFromDrawer) -1 else source

        var armedStart = false
        var armedEnd = false
        var lastFrame = 0L
        var scrollSpeed = 0f

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
            val pulledOut = verticalDist > unpinThresholdPx

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
                    // + centre shift, because that is a draw-time translation the
                    // layout knows nothing about: without it the slot grid drifts
                    // half a pitch away from the icons the user can actually see.
                    val gridStart = anchor.offset - anchor.index * pitch + centerAnim.value
                    // Continuous slot position: target == ceil(raw).
                    val raw = (x - gridStart - itemPx / 2f) / pitch

                    val current = dragDropState.targetIndex
                    val candidate = ceil(raw).toInt().coerceIn(0, maxTarget)
                    // Hysteresis: hovering exactly on a boundary no longer makes
                    // the gap flicker between two slots.
                    val target = when {
                        current == -1 -> candidate
                        candidate > current && raw > current + TargetHysteresis -> candidate
                        candidate < current && raw < current - 1f - TargetHysteresis -> candidate
                        else -> current
                    }.coerceIn(0, maxTarget)

                    if (current != target) dragDropState.targetIndex = target
                }
            }

            // Quadratic ramp, then a low-pass filter toward it. The filter is what
            // makes the scroll glide up from rest and coast to a stop when you
            // leave the edge zone, instead of switching on and off.
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
                // Frame-rate independent exponential smoothing.
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
        // `commit == null` matters: pinning the first app leaves `apps` empty for a
        // moment after release, and without this the hint flashes in and back out.
        val showEmptyHint = apps.isEmpty() && !isDragging && commit == null

        Crossfade(
            targetState = showEmptyHint,
            animationSpec = tween(EmptyStateFadeMs, easing = FastOutSlowInEasing),
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
                    // The gesture below lives OUTSIDE LazyRow's internal scrollable,
                    // and Compose delivers the Main pass innermost-first -- so while
                    // a drag is live, scrollable would consume the movement and the
                    // detector would read that as a cancellation. Switching user
                    // scrolling off for the gesture is what keeps the drag alive.
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
                                    // Net displacement, not path length -- jiggling
                                    // in place won't start a drag.
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
                                        val isOutside = verticalDist > unpinThresholdPx

                                        restoreScrollPx =
                                            if (listState.canScrollForward || listState.canScrollBackward) {
                                                listState.firstVisibleItemIndex * pitchPx +
                                                        listState.firstVisibleItemScrollOffset
                                            } else -1f

                                        // Freeze the picture BEFORE stopping the drag,
                                        // so the release frame is continuous with the
                                        // last drag frame.
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
                        // Offscreen layer + mask stay OUTSIDE the centre shift, so the
                        // edge fades stay pinned to the viewport instead of sliding
                        // with the content.
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
                        // Animated value ONLY while dragging. animateFloatAsState
                        // delivers through a channel and lands on the next frame, so
                        // reading it at commit time would leave the row half a pitch
                        // out for exactly one frame.
                        .graphicsLayer {
                            translationX = if (isDragging) centerAnim.value else centerTarget
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
                    //
                    // Note the absence of Modifier.animateItem. Every layout change
                    // here is one we have already drawn ahead of time, and toggling
                    // its specs on and off around a commit can make the whole row
                    // re-run its appearance animation at once.
                    itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                        // Memoised: Drawable.toBitmap() allocates and rasterises for
                        // adaptive icons, and this composable recomposes on every gap
                        // change and again on release. Doing it inline was rebuilding
                        // every icon in the row on the same frame as the drop.
                        val iconBitmap = remember(app.packageName, app.icon) {
                            app.icon?.toBitmap()?.asImageBitmap()
                        }

                        val isBeingDragged = isDragging &&
                                app.packageName == dragDropState.draggedApp?.packageName

                        val held = activeCommit
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
                            // Held after release: keep the exact picture the drag
                            // ended on until the real list catches up. For an unpin
                            // that means the CLOSED row the drag was already showing.
                            held != null && held.unpinned -> {
                                targetAlpha = if (app.packageName == held.packageName) 0f else 1f
                                targetShift = gapShift(index, held.source, -1, pitchPx)
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
                                    // Animated while dragging, raw otherwise. The raw
                                    // value applies in the SAME frame as the layout
                                    // change; the animated one is a frame behind.
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
                                            // Drag lives on the LazyRow container: an
                                            // item-level detector gets disposed when the
                                            // item recycles mid-scroll, killing the drag.
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

                // Stand-in for a freshly pinned app: it is not in `apps` yet, but the
                // drag ghost has already gone, so without this the icon blinks out of
                // existence until upstream delivers the new list.
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