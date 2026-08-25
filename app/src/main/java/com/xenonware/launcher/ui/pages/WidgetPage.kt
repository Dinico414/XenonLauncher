package com.xenonware.launcher.ui.pages

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.SizeF
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.drawToBitmap
import com.xenon.mylibrary.res.MenuItem
import com.xenon.mylibrary.res.XenonDropDown
import com.xenonware.launcher.model.WidgetItem
import com.xenonware.launcher.ui.res.InteractiveAppWidgetHost
import com.xenonware.launcher.ui.res.InteractiveAppWidgetHostView
import com.xenonware.launcher.ui.res.WidgetEditBorder
import com.xenonware.launcher.ui.res.WidgetSelectorDialog
import com.xenonware.launcher.viewmodel.LauncherViewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tells an AppWidgetHostView how large it actually is.
 *
 * Without this, the host view keeps the provider's declared minWidth/minHeight, so responsive
 * widgets (Calendar's event list, Maps' shortcut row, Chrome's search bar) inflate their smallest
 * RemoteViews variant no matter how big the cell is on screen.
 */
private fun AppWidgetHostView.applyGridSize(widthDp: Int, heightDp: Int) {
    if (widthDp <= 0 || heightDp <= 0) return
    updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp.toFloat(), heightDp.toFloat())))
}

/** AppWidgetProviderInfo dimensions are in pixels, not dp. Convert before comparing to cell sizes. */
private fun Int.pxToDp(density: Density): Float = with(density) { this@pxToDp.toDp().value }

/** Live state for a widget being dragged. Lives above the pager so page turns don't cancel it. */
private data class WidgetDrag(
    val widgetId: Int,
    /** Top-left of the ghost in root coordinates, px. Follows the finger 1:1. */
    val topLeft: Offset,
    /** Where inside the widget the finger grabbed, px. */
    val grab: Offset,
    val ghost: ImageBitmap?,
    /** Snapped drop target on the currently visible page, or null if no vacant cell. */
    val dropX: Int = -1,
    val dropY: Int = -1
)

@Composable
fun WidgetPage(
    viewModel: LauncherViewModel,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Grid layout constants
    val horizontalPadding = 16.dp
    val topGridPadding = 8.dp
    val bottomGridPadding = 8.dp

    // Inner padding applied to each widget cell — subtracted before reporting size to the provider
    val cellInsetHorizontal = 2.dp
    val cellInsetVertical = 4.dp

    // Drag tuning. Trigger zones are derived from the grid further down so they line up exactly
    // with the gradient indicators drawn during a drag — what you see is what triggers.
    val edgeTurnInitialDelayMs = 140L
    val edgeTurnIntervalMs = 420L
    // How far into the first/last row the trigger zone reaches, as a fraction of one cell.
    // Lower this if page turns fire while you're trying to place a widget in the edge rows.
    val edgeTurnRowBite = 0.5f

    val horizontalSafePadding = WindowInsets.safeDrawing.asPaddingValues().run {
        calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + calculateRightPadding(
            androidx.compose.ui.unit.LayoutDirection.Ltr
        )
    }

    val screenWidth =
        configuration.screenWidthDp.dp - (horizontalPadding * 2) - horizontalSafePadding

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Dock area implementation: safe draw (nav bar) + base dock area
    // Dock top is at: navBarHeight + 8.dp (dock bottom padding) + 72.dp (dock height)
    val totalDockAreaHeight = navBarHeight + 72.dp + 8.dp

    val widgetColumns by viewModel.widgetColumns.collectAsState()
    val widgets by viewModel.widgets.collectAsState()
    val blurSetting by viewModel.blurEnabled.collectAsState()

    // Grid area boundaries
    val gridTopOffset = statusBarHeight + topGridPadding
    val gridBottomOffset = totalDockAreaHeight + bottomGridPadding

    val gridAreaHeight = configuration.screenHeightDp.dp - gridTopOffset - gridBottomOffset

    // Dynamic Grid Calculation Helper
    val getRowCountForColumns = remember(gridAreaHeight, screenWidth) {
        { cols: Int ->
            val cellWidth = screenWidth / cols
            val maxPossibleRows = (gridAreaHeight / (cellWidth * 0.75f)).toInt()
            (if (maxPossibleRows % 2 == 0) maxPossibleRows else maxPossibleRows - 1).coerceAtLeast(2)
        }
    }

    val rowCount = getRowCountForColumns(widgetColumns)

    // Cell width is strictly determined by horizontal padding and columns
    val cellWidthDp = screenWidth / widgetColumns

    // Actual cell height to fill the available space exactly
    val cellHeightDp = gridAreaHeight / rowCount

    // The grid starts exactly at the calculated gridTopOffset
    val firstRowTopOffset = gridTopOffset

    // Page-turn trigger zones. These are the single source of truth for both the gradient
    // indicators and the drag's edge detection, so they can never drift apart again.
    val edgeTurnTopZone = gridTopOffset + (cellHeightDp * edgeTurnRowBite)
    val edgeTurnBottomZone = gridBottomOffset + (cellHeightDp * edgeTurnRowBite)

    // Pixel geometry, used by the root-level drag layer for hit testing and snapping
    val cellWidthPx = with(density) { cellWidthDp.toPx() }
    val cellHeightPx = with(density) { cellHeightDp.toPx() }
    val gridOriginXPx = with(density) { horizontalPadding.toPx() }
    val gridOriginYPx = with(density) { firstRowTopOffset.toPx() }
    val edgeTurnTopZonePx = with(density) { edgeTurnTopZone.toPx() }
    val edgeTurnBottomZonePx = with(density) { edgeTurnBottomZone.toPx() }

    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val appWidgetHost = remember { InteractiveAppWidgetHost(context, 1024) }

    // Live host views, kept so a drag can snapshot the real widget for its ghost
    val hostViews = remember { mutableMapOf<Int, View>() }

    var showDropDown by remember { mutableStateOf(false) }
    var dropDownOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedWidgetId by remember { mutableIntStateOf(-1) }
    var showWidgetSelector by remember { mutableStateOf(false) }

    var drag by remember { mutableStateOf<WidgetDrag?>(null) }
    var edgeScrollDir by remember { mutableIntStateOf(0) }

    // Tracks an allocated-but-not-yet-bound widget id so it can be released if the user cancels
    var pendingWidgetId by remember { mutableIntStateOf(-1) }

    val hazeState = rememberHazeState()

    val isEditing = selectedWidgetId != -1
    val isDraggingBody = drag != null

    /**
     * Picks a sensible default span for a newly added widget.
     * Prefers the provider's declared target cell span (API 31+), otherwise derives it from
     * minWidth/minHeight converted from px to dp.
     */
    val defaultSpanFor = remember(cellWidthDp, cellHeightDp, widgetColumns, rowCount, density) {
        { info: AppWidgetProviderInfo? ->
            if (info == null) {
                Pair(2.coerceAtMost(widgetColumns), 2.coerceAtMost(rowCount))
            } else if (info.targetCellWidth > 0 && info.targetCellHeight > 0) {
                Pair(
                    info.targetCellWidth.coerceIn(1, widgetColumns),
                    info.targetCellHeight.coerceIn(1, rowCount)
                )
            } else {
                val w = ceil(info.minWidth.pxToDp(density) / cellWidthDp.value).toInt()
                val h = ceil(info.minHeight.pxToDp(density) / cellHeightDp.value).toInt()
                Pair(
                    w.coerceAtLeast(2).coerceIn(1, widgetColumns),
                    h.coerceAtLeast(2).coerceIn(1, rowCount)
                )
            }
        }
    }

    // Helper to check for collisions and boundaries
    val isAreaVacant = remember(widgets, widgetColumns, rowCount) {
        { widgetId: Int, page: Int, x: Int, y: Int, width: Int, height: Int ->
            if (x < 0 || y < 0 || x + width > widgetColumns || y + height > rowCount) false
            else widgets.none { other ->
                other.id != widgetId &&
                        other.page == page &&
                        x < other.x + other.width &&
                        x + width > other.x &&
                        y < other.y + other.height &&
                        y + height > other.y
            }
        }
    }

    // Helper to find first available space
    val findFirstAvailableSpace = remember(widgets, widgetColumns, rowCount, isAreaVacant) {
        { width: Int, height: Int, startPage: Int ->
            var found: Triple<Int, Int, Int>? = null
            for (p in startPage until 5) {
                for (y in 0..rowCount - height) {
                    for (x in 0..widgetColumns - width) {
                        if (isAreaVacant(-1, p, x, y, width, height)) {
                            found = Triple(p, x, y)
                            break
                        }
                    }
                    if (found != null) break
                }
                if (found != null) break
            }
            if (found == null) {
                for (p in 0 until startPage) {
                    for (y in 0..rowCount - height) {
                        for (x in 0..widgetColumns - width) {
                            if (isAreaVacant(-1, p, x, y, width, height)) {
                                found = Triple(p, x, y)
                                break
                            }
                        }
                        if (found != null) break
                    }
                    if (found != null) break
                }
            }
            found
        }
    }

    /** Which widget sits under a root-space point on the given page, if any. */
    val widgetAtPoint = remember(widgets, cellWidthPx, cellHeightPx, gridOriginXPx, gridOriginYPx) {
        { point: Offset, page: Int ->
            widgets.firstOrNull { w ->
                w.page == page &&
                        point.x >= gridOriginXPx + w.x * cellWidthPx &&
                        point.x < gridOriginXPx + (w.x + w.width) * cellWidthPx &&
                        point.y >= gridOriginYPx + w.y * cellHeightPx &&
                        point.y < gridOriginYPx + (w.y + w.height) * cellHeightPx
            }
        }
    }

    // Pages only exist if they have content, plus one extra if in edit mode
    val pageCount = remember(widgets, isEditing) {
        val maxWidgetPage = widgets.maxOfOrNull { it.page } ?: 0
        if (isEditing) (maxWidgetPage + 2).coerceAtMost(5)
        else (maxWidgetPage + 1).coerceAtMost(5)
    }

    val pagerState = rememberPagerState(initialPage = 0) { pageCount }

    // Auto-advance pages while the ghost is held against the top or bottom edge.
    // The gesture lives above the pager, so scrolling here never interrupts it.
    LaunchedEffect(edgeScrollDir, isDraggingBody) {
        if (edgeScrollDir == 0 || !isDraggingBody) return@LaunchedEffect
        // Short guard delay only — long enough to ignore a quick pass through the zone,
        // then the first turn fires straight away rather than after a full interval.
        delay(edgeTurnInitialDelayMs.milliseconds)
        while (true) {
            val target = pagerState.currentPage + edgeScrollDir
            if (target !in 0..<pageCount) break
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            pagerState.animateScrollToPage(target)
            delay(edgeTurnIntervalMs.milliseconds)
        }
    }

    val pickWidgetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (appWidgetId != -1) {
                val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
                val (w, h) = defaultSpanFor(info)
                val space = findFirstAvailableSpace(w, h, pagerState.currentPage)
                if (space != null) {
                    viewModel.addWidget(appWidgetId, space.first, space.second, space.third, w, h)
                    scope.launch {
                        pagerState.animateScrollToPage(space.first)
                    }
                } else {
                    viewModel.addWidget(appWidgetId, pagerState.currentPage, 0, 0, w, h)
                }
                pendingWidgetId = -1
            }
        } else {
            // Bind was cancelled or denied — release the id instead of leaking it
            if (pendingWidgetId != -1) {
                runCatching { appWidgetHost.deleteAppWidgetId(pendingWidgetId) }
                pendingWidgetId = -1
            }
        }
    }

    val shortcutLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            val intent = data.getParcelableExtra(
                Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java
            )
            val name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)
            val iconRes = data.getParcelableExtra(
                Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource::class.java
            )
            val iconBitmap = data.getParcelableExtra(
                Intent.EXTRA_SHORTCUT_ICON, Bitmap::class.java
            )

            if (intent != null && name != null) {
                val w = 1
                val h = 1
                val space = findFirstAvailableSpace(w, h, pagerState.currentPage)
                val (targetPage, targetX, targetY) = space ?: Triple(pagerState.currentPage, 0, 0)

                viewModel.addShortcut(
                    targetPage, targetX, targetY, w, h,
                    name, intent.toUri(0),
                    iconRes?.let { "${it.packageName}:${it.resourceName}" },
                    iconBitmap
                )

                scope.launch {
                    pagerState.animateScrollToPage(targetPage)
                }
            }
        }
    }

    DisposableEffect(Unit) {
        appWidgetHost.startListening()
        onDispose {
            appWidgetHost.stopListening()
        }
    }

    val gridAlpha by animateFloatAsState(if (isEditing) 0.12f else 0f, label = "gridAlpha")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .then(if (blurSetting) Modifier.hazeSource(hazeState) else Modifier)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { selectedWidgetId = -1 })
            }
            .pointerInput(Unit) {
                var tempOffset = Offset.Zero
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        tempOffset = offset
                    },
                    onDrag = { change, _ -> change.consume() },
                    onDragEnd = {
                        dropDownOffset = tempOffset
                        showDropDown = true
                        selectedWidgetId = -1
                    }
                )
            }
    ) {
        // Page Transition Zones (Visible while dragging)
        AnimatedVisibility(
            visible = isDraggingBody,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            // Same zones the drag uses, so the gradient is an honest hit target
            val topIndicatorHeight = edgeTurnTopZone
            val primaryColor = colorScheme.primary

            // Brighten the moment the turn is armed — immediate feedback that you're deep enough
            val topArmed by animateFloatAsState(
                if (edgeScrollDir == -1) 1f else 0f, label = "topArmed"
            )
            val bottomArmed by animateFloatAsState(
                if (edgeScrollDir == 1) 1f else 0f, label = "bottomArmed"
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (pagerState.currentPage > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(topIndicatorHeight)
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        primaryColor.copy(alpha = 0.18f + 0.30f * topArmed),
                                        Color.Transparent
                                    )
                                )
                            )
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cornerPx = 24.dp.toPx()
                            val path = Path().apply {
                                moveTo(0f, size.height - cornerPx)
                                quadraticTo(0f, size.height, cornerPx, size.height)
                                lineTo(size.width - cornerPx, size.height)
                                quadraticTo(
                                    size.width,
                                    size.height,
                                    size.width,
                                    size.height - cornerPx
                                )
                            }
                            drawPath(
                                path = path,
                                color = primaryColor.copy(alpha = 0.3f + 0.6f * topArmed),
                                style = Stroke(width = (1 + topArmed).dp.toPx())
                            )
                        }
                    }
                }
                if (pagerState.currentPage < pageCount - 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(edgeTurnBottomZone)
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        primaryColor.copy(alpha = 0.18f + 0.30f * bottomArmed)
                                    )
                                )
                            )
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cornerPx = 24.dp.toPx()
                            val path = Path().apply {
                                moveTo(0f, cornerPx)
                                quadraticTo(0f, 0f, cornerPx, 0f)
                                lineTo(size.width - cornerPx, 0f)
                                quadraticTo(size.width, 0f, size.width, cornerPx)
                            }
                            drawPath(
                                path = path,
                                color = primaryColor.copy(alpha = 0.3f + 0.6f * bottomArmed),
                                style = Stroke(width = (1 + bottomArmed).dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        // Main Content Layer
        Box(modifier = Modifier.fillMaxSize()) {
            // Pixel-style Dot Grid
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (gridAlpha > 0f) {
                    val startXPx = horizontalPadding.toPx()
                    val startYPx = firstRowTopOffset.toPx()
                    val dotRadius = 0.8.dp.toPx()
                    val color = Color.White.copy(alpha = gridAlpha)

                    for (i in 0..widgetColumns) {
                        for (j in 0..rowCount) {
                            drawCircle(
                                color,
                                dotRadius,
                                Offset(
                                    startXPx + i * cellWidthDp.toPx(),
                                    startYPx + j * cellHeightDp.toPx()
                                )
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        val topFadeHeight = gridTopOffset.toPx()
                        val bottomFadeHeight = gridBottomOffset.toPx()
                        val totalHeight = size.height

                        if (totalHeight > 0) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    (topFadeHeight / totalHeight).coerceIn(0f, 1f) to Color.Black,
                                    ((totalHeight - bottomFadeHeight) / totalHeight).coerceIn(
                                        0f,
                                        1f
                                    ) to Color.Black,
                                    1f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                    }
            ) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    // Keep neighbours composed so host views aren't torn down and rebuilt on
                    // every page turn during a drag
                    beyondViewportPageCount = if (isEditing) 1 else 0,
                    userScrollEnabled = !isEditing
                ) { pageIndex ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (widgets.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clickable { showWidgetSelector = true }
                                        .size(80.dp)
                                        .background(
                                            colorScheme.primaryContainer.copy(alpha = 0.3f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        null,
                                        modifier = Modifier.size(40.dp),
                                        tint = colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Add your first Widget",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        widgets.filter { it.page == pageIndex }.forEach { widget ->
                            key(widget.id) {
                                val isSelected = selectedWidgetId == widget.id
                                val isBeingDragged = drag?.widgetId == widget.id
                                val widgetInfo =
                                    remember(widget.id) { appWidgetManager.getAppWidgetInfo(widget.id) }

                                val animX by animateDpAsState(
                                    targetValue = (widget.x * cellWidthDp.value).dp,
                                    animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
                                    label = "animX"
                                )
                                val animY by animateDpAsState(
                                    targetValue = (widget.y * cellHeightDp.value).dp,
                                    animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
                                    label = "animY"
                                )
                                val animW by animateDpAsState(
                                    targetValue = (widget.width * cellWidthDp.value).dp,
                                    animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
                                    label = "animW"
                                )
                                val animH by animateDpAsState(
                                    targetValue = (widget.height * cellHeightDp.value).dp,
                                    animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
                                    label = "animH"
                                )

                                val selectionProgress by animateFloatAsState(
                                    if (isSelected && !isBeingDragged) 1f else 0f,
                                    label = "selection"
                                )
                                val restScale by animateFloatAsState(
                                    if (isSelected && !isBeingDragged) 1.02f else 1f,
                                    label = "restScale"
                                )

                                // Size reported to the widget provider, in dp, minus cell insets.
                                // Derived from grid spans (not the animated dp) so the provider is
                                // only notified once per resize instead of on every frame.
                                val reportedWidthDp = remember(
                                    widget.width, cellWidthDp, cellInsetHorizontal
                                ) {
                                    (widget.width * cellWidthDp.value - cellInsetHorizontal.value * 2)
                                        .roundToInt().coerceAtLeast(1)
                                }
                                val reportedHeightDp = remember(
                                    widget.height, cellHeightDp, cellInsetVertical
                                ) {
                                    (widget.height * cellHeightDp.value - cellInsetVertical.value * 2)
                                        .roundToInt().coerceAtLeast(1)
                                }
                                val lastAppliedSize =
                                    remember(widget.id) { mutableStateOf<Pair<Int, Int>?>(null) }

                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = animX + horizontalPadding,
                                            y = animY + firstRowTopOffset
                                        )
                                        .size(width = animW, height = animH)
                                        .padding(
                                            horizontal = cellInsetHorizontal,
                                            vertical = cellInsetVertical
                                        )
                                        .graphicsLayer {
                                            // The ghost stands in for this widget mid-drag
                                            alpha = if (isBeingDragged) 0f else 1f
                                            scaleX = restScale
                                            scaleY = restScale
                                        }
                                        .zIndex(if (isSelected) 1f else 0f)
                                ) {
                                    // Widget Content Box
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(24.dp))
                                    ) {
                                        if (widget.type == "shortcut") {
                                            ShortcutWidgetContent(widget)
                                        } else {
                                            AndroidView(
                                                factory = { ctx ->
                                                    val hostView = appWidgetHost.createView(
                                                        ctx,
                                                        widget.id,
                                                        widgetInfo
                                                    )
                                                    hostView.setPadding(0, 0, 0, 0)
                                                    // Long press enters edit mode; taps, scrolls
                                                    // and pinches still reach the widget itself.
                                                    (hostView as? InteractiveAppWidgetHostView)
                                                        ?.onWidgetLongPress = {
                                                        haptic.performHapticFeedback(
                                                            HapticFeedbackType.LongPress
                                                        )
                                                        selectedWidgetId = widget.id
                                                    }
                                                    hostViews[widget.id] = hostView
                                                    hostView
                                                },
                                                update = { hostView ->
                                                    val target =
                                                        reportedWidthDp to reportedHeightDp
                                                    if (lastAppliedSize.value != target) {
                                                        lastAppliedSize.value = target
                                                        runCatching {
                                                            hostView.applyGridSize(
                                                                reportedWidthDp,
                                                                reportedHeightDp
                                                            )
                                                        }
                                                    }
                                                },
                                                onRelease = { hostViews.remove(widget.id) },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .border(
                                                    width = 2.dp,
                                                    color = colorScheme.primary.copy(alpha = selectionProgress),
                                                    shape = RoundedCornerShape(24.dp)
                                                )
                                        )
                                    }

                                    // Outside edit mode, shortcuts still need tap-to-launch and
                                    // long-press-to-edit. Real widgets handle both themselves.
                                    if (!isEditing && widget.type == "shortcut") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .pointerInput(widget.id) {
                                                    detectTapGestures(
                                                        onLongPress = {
                                                            haptic.performHapticFeedback(
                                                                HapticFeedbackType.LongPress
                                                            )
                                                            selectedWidgetId = widget.id
                                                        },
                                                        onTap = {
                                                            try {
                                                                val intent = Intent.parseUri(
                                                                    widget.shortcutIntent,
                                                                    0
                                                                ).apply {
                                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                }
                                                                context.startActivity(intent)
                                                            } catch (_: Exception) {
                                                            }
                                                        }
                                                    )
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (showDropDown) {
                    val gridOptions = if (isLandscape) listOf(6, 8, 10) else listOf(4, 5)
                    val primaryColor = colorScheme.primary

                    val menuItems = remember(
                        isLandscape,
                        widgetColumns,
                        primaryColor,
                        isEditing,
                        getRowCountForColumns
                    ) {
                        listOfNotNull(
                            MenuItem(
                                text = "Wallpaper",
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                                    context.startActivity(
                                        Intent.createChooser(
                                            intent,
                                            "Select Wallpaper"
                                        )
                                    )
                                },
                                leadingIcon = { Icon(Icons.Rounded.Wallpaper, null) }
                            ),
                            MenuItem(
                                text = "Settings",
                                onClick = { onOpenSettings() },
                                leadingIcon = { Icon(Icons.Rounded.Settings, null) }
                            ),
                            MenuItem(
                                text = "Add Widget",
                                onClick = { showWidgetSelector = true },
                                leadingIcon = { Icon(Icons.Rounded.Add, null) }
                            ),
                            if (!isEditing) MenuItem(
                                text = "Edit Layout",
                                onClick = { selectedWidgetId = -2 },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null) }
                            ) else null
                        ) + gridOptions.map { cols ->
                            val isSelected = widgetColumns == cols
                            val targetRowCount = getRowCountForColumns(cols)
                            MenuItem(
                                text = "Grid Size ${cols}x$targetRowCount",
                                onClick = { viewModel.setWidgetColumns(cols) },
                                leadingIcon = { Icon(Icons.Rounded.AspectRatio, null) },
                                textColor = if (isSelected) primaryColor else null,
                                containerColor = if (isSelected) primaryColor.copy(alpha = 0.15f) else null
                            )
                        }
                    }

                    XenonDropDown(
                        expanded = showDropDown,
                        onDismissRequest = { showDropDown = false },
                        items = menuItems,
                        hazeState = if (blurSetting) hazeState else null,
                        offsetX = with(density) { dropDownOffset.x.toDp() },
                        offsetY = with(density) { dropDownOffset.y.toDp() },
                        anchorPos = Offset.Zero,
                        alignment = Alignment.Center
                    )
                }
            }

            // ---- Edit layer -------------------------------------------------------------
            // Sits above the pager and outside its fade mask, so a drag survives page turns
            // and the ghost stays fully opaque at the screen edges.
            if (isEditing) {
                val selected = widgets.firstOrNull { it.id == selectedWidgetId }

                val currentSelected by rememberUpdatedState(selected)
                val currentWidgetColumns by rememberUpdatedState(widgetColumns)
                val currentRowCount by rememberUpdatedState(rowCount)

                Box(modifier = Modifier.fillMaxSize()) {

                    // Tap: select a widget, or clear the selection on empty space
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(pagerState.currentPage, widgets) {
                                detectTapGestures(onTap = { point ->
                                    val hit = widgetAtPoint(point, pagerState.currentPage)
                                    selectedWidgetId = hit?.id ?: -1
                                })
                            }
                            // Drag: only starts inside the currently selected widget
                            .pointerInput(
                                selectedWidgetId, widgets, cellWidthPx, cellHeightPx,
                                edgeTurnTopZonePx, edgeTurnBottomZonePx
                            ) {
                                var grabbed: WidgetItem? = null
                                var lastDrop = -1 to -1

                                detectDragGestures(
                                    onDragStart = { start ->
                                        val sel = currentSelected ?: return@detectDragGestures
                                        val left = gridOriginXPx + sel.x * cellWidthPx
                                        val top = gridOriginYPx + sel.y * cellHeightPx
                                        val inside = start.x >= left &&
                                                start.x < left + sel.width * cellWidthPx &&
                                                start.y >= top &&
                                                start.y < top + sel.height * cellHeightPx
                                        if (!inside) return@detectDragGestures

                                        grabbed = sel
                                        lastDrop = sel.x to sel.y
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                                        val ghost = hostViews[sel.id]?.let { view ->
                                            runCatching {
                                                if (view.width > 0 && view.height > 0)
                                                    view.drawToBitmap().asImageBitmap()
                                                else null
                                            }.getOrNull()
                                        }

                                        drag = WidgetDrag(
                                            widgetId = sel.id,
                                            topLeft = Offset(left, top),
                                            grab = Offset(start.x - left, start.y - top),
                                            ghost = ghost,
                                            dropX = sel.x,
                                            dropY = sel.y
                                        )
                                    },
                                    onDrag = { change, dragAmount ->
                                        val sel = grabbed ?: return@detectDragGestures
                                        val active = drag ?: return@detectDragGestures
                                        change.consume()

                                        // 1:1 with the finger — no cell quantisation, no springs
                                        val moved = active.topLeft + dragAmount

                                        // Snap target on whichever page is currently showing
                                        val rawX = ((moved.x - gridOriginXPx) / cellWidthPx)
                                            .roundToInt()
                                            .coerceIn(0, (currentWidgetColumns - sel.width).coerceAtLeast(0))
                                        val rawY = ((moved.y - gridOriginYPx) / cellHeightPx)
                                            .roundToInt()
                                            .coerceIn(0, (currentRowCount - sel.height).coerceAtLeast(0))

                                        val vacant = isAreaVacant(
                                            sel.id, pagerState.currentPage,
                                            rawX, rawY, sel.width, sel.height
                                        )

                                        if (vacant && (rawX to rawY) != lastDrop) {
                                            lastDrop = rawX to rawY
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }

                                        drag = active.copy(
                                            topLeft = moved,
                                            dropX = if (vacant) rawX else -1,
                                            dropY = if (vacant) rawY else -1
                                        )

                                        // Edge hold turns the page; the LaunchedEffect above
                                        // keeps advancing while the finger stays there.
                                        // Whichever reaches the zone first: the finger, or the
                                        // leading edge of the ghost itself. Dragging a widget
                                        // downward arms the turn as soon as its bottom edge
                                        // enters the zone, without having to bury the finger.
                                        val fingerY = moved.y + active.grab.y
                                        val ghostTop = moved.y
                                        val ghostBottom = moved.y + sel.height * cellHeightPx
                                        edgeScrollDir = when {
                                            minOf(fingerY, ghostTop) < edgeTurnTopZonePx -> -1
                                            maxOf(fingerY, ghostBottom) >
                                                    size.height - edgeTurnBottomZonePx -> 1

                                            else -> 0
                                        }
                                    },
                                    onDragEnd = {
                                        val sel = grabbed
                                        val active = drag
                                        if (sel != null && active != null) {
                                            val page = pagerState.currentPage
                                            val fitsHere = active.dropX >= 0 && isAreaVacant(
                                                sel.id, page,
                                                active.dropX, active.dropY,
                                                sel.width, sel.height
                                            )
                                            if (fitsHere) {
                                                viewModel.updateWidget(
                                                    sel.id, page,
                                                    active.dropX, active.dropY,
                                                    sel.width, sel.height
                                                )
                                            } else if (page != sel.page) {
                                                // Dropped on a new page but the exact cell is
                                                // taken — fall back to the first free slot there
                                                val space = findFirstAvailableSpace(
                                                    sel.width, sel.height, page
                                                )
                                                if (space != null && space.first == page) {
                                                    viewModel.updateWidget(
                                                        sel.id, page,
                                                        space.second, space.third,
                                                        sel.width, sel.height
                                                    )
                                                }
                                            }
                                        }
                                        grabbed = null
                                        edgeScrollDir = 0
                                        drag = null
                                    },
                                    onDragCancel = {
                                        grabbed = null
                                        edgeScrollDir = 0
                                        drag = null
                                    }
                                )
                            }
                    )

                    // Snap target preview
                    val activeDrag = drag
                    val dragged = activeDrag?.let { d -> widgets.firstOrNull { it.id == d.widgetId } }
                    if (activeDrag != null && dragged != null && activeDrag.dropX >= 0) {
                        val previewX by animateDpAsState(
                            (activeDrag.dropX * cellWidthDp.value).dp + horizontalPadding,
                            label = "previewX"
                        )
                        val previewY by animateDpAsState(
                            (activeDrag.dropY * cellHeightDp.value).dp + firstRowTopOffset,
                            label = "previewY"
                        )
                        Box(
                            modifier = Modifier
                                .offset(x = previewX, y = previewY)
                                .size(
                                    width = (dragged.width * cellWidthDp.value).dp,
                                    height = (dragged.height * cellHeightDp.value).dp
                                )
                                .padding(
                                    horizontal = cellInsetHorizontal,
                                    vertical = cellInsetVertical
                                )
                                .background(
                                    colorScheme.primary.copy(alpha = 0.12f),
                                    RoundedCornerShape(24.dp)
                                )
                                .border(
                                    2.dp,
                                    colorScheme.primary.copy(alpha = 0.5f),
                                    RoundedCornerShape(24.dp)
                                )
                        )
                    }

                    // Resize handles, floated above the pager so the drag layer can't swallow them
                    if (selected != null && drag == null && selected.page == pagerState.currentPage) {
                        val selInfo = remember(selected.id) {
                            runCatching { appWidgetManager.getAppWidgetInfo(selected.id) }.getOrNull()
                        }

                        val minW = if (selInfo != null) {
                            val floorPx =
                                if (selInfo.minResizeWidth in 1 until selInfo.minWidth)
                                    selInfo.minResizeWidth else selInfo.minWidth
                            ceil(floorPx.pxToDp(density) / cellWidthDp.value).toInt()
                                .coerceIn(1, widgetColumns)
                        } else 1

                        val minH = if (selInfo != null) {
                            val floorPx =
                                if (selInfo.minResizeHeight in 1 until selInfo.minHeight)
                                    selInfo.minResizeHeight else selInfo.minHeight
                            ceil(floorPx.pxToDp(density) / cellHeightDp.value).toInt()
                                .coerceIn(1, rowCount)
                        } else 1

                        val handleX by animateDpAsState(
                            (selected.x * cellWidthDp.value).dp + horizontalPadding,
                            spring(stiffness = 500f, dampingRatio = 0.8f), label = "handleX"
                        )
                        val handleY by animateDpAsState(
                            (selected.y * cellHeightDp.value).dp + firstRowTopOffset,
                            spring(stiffness = 500f, dampingRatio = 0.8f), label = "handleY"
                        )
                        val handleW by animateDpAsState(
                            (selected.width * cellWidthDp.value).dp,
                            spring(stiffness = 500f, dampingRatio = 0.8f), label = "handleW"
                        )
                        val handleH by animateDpAsState(
                            (selected.height * cellHeightDp.value).dp,
                            spring(stiffness = 500f, dampingRatio = 0.8f), label = "handleH"
                        )

                        val isShortcut = selected.type == "shortcut"

                        Box(
                            modifier = Modifier
                                .offset(x = handleX, y = handleY)
                                .size(width = handleW, height = handleH)
                                .padding(
                                    horizontal = cellInsetHorizontal,
                                    vertical = cellInsetVertical
                                )
                        ) {
                            val topAcc = remember(selected.id) { mutableFloatStateOf(0f) }
                            WidgetEditBorder(Alignment.TopCenter) { dragAmount ->
                                val w = currentSelected ?: return@WidgetEditBorder
                                topAcc.floatValue += dragAmount.y
                                val dy = (topAcc.floatValue / cellHeightPx).roundToInt()
                                if (dy != 0) {
                                    val maxY = (w.y + w.height - minH).coerceAtLeast(0)
                                    val newY = (w.y + dy).coerceIn(0, maxY)
                                    val newH = (w.height - (newY - w.y)).coerceAtLeast(minH)

                                    if (isShortcut && newH > 2) return@WidgetEditBorder

                                    if ((newY != w.y || newH != w.height) && isAreaVacant(
                                            w.id, w.page, w.x, newY, w.width, newH
                                        )
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.updateWidget(w.id, w.page, w.x, newY, w.width, newH)
                                        topAcc.floatValue -= (newY - w.y) * cellHeightPx
                                    }
                                }
                            }

                            val botAcc = remember(selected.id) { mutableFloatStateOf(0f) }
                            WidgetEditBorder(Alignment.BottomCenter) { dragAmount ->
                                val w = currentSelected ?: return@WidgetEditBorder
                                botAcc.floatValue += dragAmount.y
                                val dh = (botAcc.floatValue / cellHeightPx).roundToInt()
                                if (dh != 0) {
                                    var newH = (w.height + dh).coerceIn(
                                        minH, (currentRowCount - w.y).coerceAtLeast(minH)
                                    )
                                    if (isShortcut) newH = newH.coerceAtMost(2)

                                    if (newH != w.height && isAreaVacant(
                                            w.id, w.page, w.x, w.y, w.width, newH
                                        )
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.updateWidget(w.id, w.page, w.x, w.y, w.width, newH)
                                        botAcc.floatValue -= (newH - w.height) * cellHeightPx
                                    }
                                }
                            }

                            val leftAcc = remember(selected.id) { mutableFloatStateOf(0f) }
                            WidgetEditBorder(Alignment.CenterStart) { dragAmount ->
                                val w = currentSelected ?: return@WidgetEditBorder
                                leftAcc.floatValue += dragAmount.x
                                val dx = (leftAcc.floatValue / cellWidthPx).roundToInt()
                                if (dx != 0) {
                                    val maxX = (w.x + w.width - minW).coerceAtLeast(0)
                                    val newX = (w.x + dx).coerceIn(0, maxX)
                                    val newW = (w.width - (newX - w.x)).coerceAtLeast(minW)

                                    if (isShortcut && newW > 2) return@WidgetEditBorder

                                    if ((newX != w.x || newW != w.width) && isAreaVacant(
                                            w.id, w.page, newX, w.y, newW, w.height
                                        )
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.updateWidget(w.id, w.page, newX, w.y, newW, w.height)
                                        leftAcc.floatValue -= (newX - w.x) * cellWidthPx
                                    }
                                }
                            }

                            val rightAcc = remember(selected.id) { mutableFloatStateOf(0f) }
                            WidgetEditBorder(Alignment.CenterEnd) { dragAmount ->
                                val w = currentSelected ?: return@WidgetEditBorder
                                rightAcc.floatValue += dragAmount.x
                                val dw = (rightAcc.floatValue / cellWidthPx).roundToInt()
                                if (dw != 0) {
                                    val maxAllowedW =
                                        (currentWidgetColumns - w.x).coerceAtLeast(minW)
                                    var newW = (w.width + dw).coerceIn(minW, maxAllowedW)
                                    if (isShortcut) newW = newW.coerceAtMost(2)

                                    if (newW != w.width && isAreaVacant(
                                            w.id, w.page, w.x, w.y, newW, w.height
                                        )
                                    ) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        viewModel.updateWidget(w.id, w.page, w.x, w.y, newW, w.height)
                                        rightAcc.floatValue -= (newW - w.width) * cellWidthPx
                                    }
                                }
                            }
                        }
                    }

                    // The ghost: a snapshot of the widget, tracking the finger exactly
                    if (activeDrag != null && dragged != null) {
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        activeDrag.topLeft.x.roundToInt(),
                                        activeDrag.topLeft.y.roundToInt()
                                    )
                                }
                                .size(
                                    width = (dragged.width * cellWidthDp.value).dp,
                                    height = (dragged.height * cellHeightDp.value).dp
                                )
                                .padding(
                                    horizontal = cellInsetHorizontal,
                                    vertical = cellInsetVertical
                                )
                                .graphicsLayer {
                                    scaleX = 1.06f
                                    scaleY = 1.06f
                                    alpha = 0.92f
                                    shadowElevation = 16.dp.toPx()
                                    shape = RoundedCornerShape(24.dp)
                                    clip = true
                                }
                                .background(
                                    colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    RoundedCornerShape(24.dp)
                                )
                        ) {
                            when {
                                activeDrag.ghost != null -> Image(
                                    bitmap = activeDrag.ghost,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )

                                dragged.type == "shortcut" -> ShortcutWidgetContent(dragged)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        2.dp,
                                        colorScheme.primary,
                                        RoundedCornerShape(24.dp)
                                    )
                            )
                        }
                    }
                }
            }

            // Remove Bar — above the edit layer so the buttons stay tappable
            AnimatedVisibility(
                visible = isEditing && !isDraggingBody,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { selectedWidgetId = -1 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.secondaryContainer,
                            contentColor = colorScheme.onSecondaryContainer
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.SemiBold)
                    }

                    if (selectedWidgetId >= 0) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                // Release the host's app widget id so it isn't leaked.
                                // Shortcuts use launcher-generated ids, so skip those.
                                val target = widgets.firstOrNull { it.id == selectedWidgetId }
                                if (target != null && target.type != "shortcut") {
                                    runCatching { appWidgetHost.deleteAppWidgetId(target.id) }
                                }
                                viewModel.removeWidget(selectedWidgetId)
                                selectedWidgetId = -1
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.errorContainer,
                                contentColor = colorScheme.onErrorContainer
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Remove", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Vertical Page Indicator
            if (pageCount > 1) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                        .padding(bottom = gridBottomOffset + 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(pageCount) { iteration ->
                        val isSelected = pagerState.currentPage == iteration
                        val dotSize by animateDpAsState(
                            if (isSelected) 8.dp else 6.dp,
                            label = "dotSize"
                        )
                        val alpha by animateFloatAsState(
                            if (isSelected) 1f else 0.4f,
                            label = "dotAlpha"
                        )

                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .background(Color.White.copy(alpha = alpha), CircleShape)
                        )
                    }
                }
            }
        }
    }

    if (showWidgetSelector) {
        val installedWidgets by viewModel.installedWidgets.collectAsState()
        WidgetSelectorDialog(
            installedWidgets = installedWidgets,
            onDismiss = { showWidgetSelector = false },
            onWidgetSelected = { item ->
                if (item.isWidget && item.widgetInfo != null) {
                    val info = item.widgetInfo
                    val appWidgetId = appWidgetHost.allocateAppWidgetId()
                    val success =
                        appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider)

                    // Size the new widget from what the provider actually asks for
                    val (w, h) = defaultSpanFor(info)
                    val space = findFirstAvailableSpace(w, h, pagerState.currentPage)
                    val (targetPage, targetX, targetY) = space ?: Triple(
                        pagerState.currentPage,
                        0,
                        0
                    )

                    if (success) {
                        viewModel.addWidget(appWidgetId, targetPage, targetX, targetY, w, h)
                        scope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    } else {
                        pendingWidgetId = appWidgetId
                        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                        }
                        pickWidgetLauncher.launch(intent)
                    }
                } else if (item.shortcutInfo != null) {
                    val intent = Intent(Intent.ACTION_CREATE_SHORTCUT).apply {
                        component = android.content.ComponentName(
                            item.shortcutInfo.activityInfo.packageName,
                            item.shortcutInfo.activityInfo.name
                        )
                    }
                    try {
                        shortcutLauncher.launch(intent)
                    } catch (_: Exception) {
                    }
                }
                showWidgetSelector = false
            }
        )
    }
}

@Composable
fun ShortcutWidgetContent(widget: WidgetItem) {
    val context = LocalContext.current

    val iconDrawable = remember(widget.shortcutIconRes, widget.shortcutIntent) {
        try {
            if (widget.shortcutIconRes?.startsWith("file:") == true) {
                val fileName = widget.shortcutIconRes.substring(5)
                val file = context.getFileStreamPath(fileName)
                if (file.exists()) {
                    BitmapDrawable(context.resources, file.absolutePath)
                } else null
            } else if (widget.shortcutIconRes != null) {
                val parts = widget.shortcutIconRes.split(":")
                if (parts.size == 2) {
                    val pkg = parts[0]
                    val resName = parts[1]
                    val appRes = context.packageManager.getResourcesForApplication(pkg)
                    val id = appRes.getIdentifier(resName, null, null)
                    if (id != 0) appRes.getDrawable(id, null) else null
                } else null
            } else {
                val intent = Intent.parseUri(widget.shortcutIntent, 0)
                val pkg = intent.`package` ?: intent.component?.packageName
                if (pkg != null) context.packageManager.getApplicationIcon(pkg) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            if (iconDrawable != null) {
                Image(
                    bitmap = iconDrawable.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(if (widget.width > 1) 56.dp else 40.dp)
                )
            } else {
                Icon(Icons.Rounded.Apps, null, modifier = Modifier.size(40.dp))
            }

            Text(
                text = widget.shortcutLabel ?: "",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                maxLines = if (widget.height > 1) 2 else 1,
                fontWeight = FontWeight.Medium,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}