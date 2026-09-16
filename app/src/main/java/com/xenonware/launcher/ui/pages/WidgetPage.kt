package com.xenonware.launcher.ui.pages

import android.app.Activity
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.withSaveLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.drawToBitmap
import com.xenon.mylibrary.res.MenuItem
import com.xenon.mylibrary.res.XenonDropDown
import com.xenon.mylibrary.values.BiggestPadding
import com.xenon.mylibrary.values.ExtraBigSpacing
import com.xenon.mylibrary.values.ExtraLargeCornerRadius
import com.xenon.mylibrary.values.ExtraLargeIconSize
import com.xenon.mylibrary.values.ExtraLargeSpacing
import com.xenon.mylibrary.values.ExtraLargerCornerRadius
import com.xenon.mylibrary.values.ExtraLargerPadding
import com.xenon.mylibrary.values.HugeBiggerSpacing
import com.xenon.mylibrary.values.HugeSpacing
import com.xenon.mylibrary.values.HugestSpacing
import com.xenon.mylibrary.values.LargeMediumPadding
import com.xenon.mylibrary.values.LargestElevation
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.MediumSmallElevation
import com.xenon.mylibrary.values.MediumSmallPadding
import com.xenon.mylibrary.values.MediumSmallerCornerRadius
import com.xenon.mylibrary.values.MediumSpacer
import com.xenon.mylibrary.values.SmallPadding
import com.xenon.mylibrary.values.SmallerPadding
import com.xenon.mylibrary.values.SmallerStroke
import com.xenonware.launcher.R
import com.xenonware.launcher.model.WidgetItem
import com.xenonware.launcher.ui.res.WidgetEditBorder
import com.xenonware.launcher.ui.res.WidgetSelectorDialog
import com.xenonware.launcher.util.InteractiveAppWidgetHost
import com.xenonware.launcher.util.InteractiveAppWidgetHostView
import com.xenonware.launcher.viewmodel.LauncherViewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private fun AppWidgetHostView.applyGridSize(widthDp: Int, heightDp: Int) {
    if (widthDp <= 0 || heightDp <= 0) return
    updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp.toFloat(), heightDp.toFloat())))
}

private fun Int.pxToDp(density: Density): Float = with(density) { this@pxToDp.toDp().value }

private data class WidgetDrag(
    val widgetId: Int,
    val topLeft: Offset,
    val grab: Offset,
    val ghost: ImageBitmap?,
    val dropX: Int = -1,
    val dropY: Int = -1
)

@Composable
fun WidgetPage(
    viewModel: LauncherViewModel,
    onOpenSettings: () -> Unit,
    isDockVisible: Boolean = true
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val windowInfo = LocalWindowInfo.current
    val windowWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val windowHeightDp = with(density) { windowInfo.containerSize.height.toDp() }

    val horizontalPadding = LargestPadding
    val topGridPadding = MediumPadding
    val bottomGridPadding = MediumPadding

    val cellInsetHorizontal = SmallerPadding
    val cellInsetVertical = SmallPadding

    val edgeTurnInitialDelayMs = 140L
    val edgeTurnIntervalMs = 420L
    val edgeTurnRowBite = 0.5f

    val horizontalSafePadding = WindowInsets.safeDrawing.asPaddingValues().run {
        calculateLeftPadding(LayoutDirection.Ltr) + calculateRightPadding(
            LayoutDirection.Ltr
        )
    }

    val screenWidth =
        windowWidthDp - (horizontalPadding * 2) - horizontalSafePadding

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val totalDockAreaHeight = if (isDockVisible) navBarHeight + HugeBiggerSpacing + MediumPadding else navBarHeight + LargestPadding

    val widgetColumns by viewModel.widgetColumns.collectAsState()
    val widgets by viewModel.widgets.collectAsState()
    val blurSetting by viewModel.blurEnabled.collectAsState()

    val gridTopOffset = statusBarHeight + topGridPadding
    val gridBottomOffset = totalDockAreaHeight + bottomGridPadding

    val gridAreaHeight = windowHeightDp - gridTopOffset - gridBottomOffset

    val getRowCountForColumns = remember(gridAreaHeight, screenWidth) {
        { cols: Int ->
            val cellWidth = screenWidth / cols
            val maxPossibleRows = (gridAreaHeight / (cellWidth * 0.75f)).toInt()
            (if (maxPossibleRows % 2 == 0) maxPossibleRows else maxPossibleRows - 1).coerceAtLeast(2)
        }
    }

    val rowCount = getRowCountForColumns(widgetColumns)

    val cellWidthDp = screenWidth / widgetColumns

    val cellHeightDp = gridAreaHeight / rowCount

    val firstRowTopOffset: Dp = gridTopOffset

    val edgeTurnTopZone = gridTopOffset + (cellHeightDp * edgeTurnRowBite)
    val edgeTurnBottomZone = gridBottomOffset + (cellHeightDp * edgeTurnRowBite)

    val cellWidthPx = with(density) { cellWidthDp.toPx() }
    val cellHeightPx = with(density) { cellHeightDp.toPx() }
    val gridOriginXPx = with(density) { horizontalPadding.toPx() }
    val gridOriginYPx = with(density) { firstRowTopOffset.toPx() }
    val edgeTurnTopZonePx = with(density) { edgeTurnTopZone.toPx() }
    val edgeTurnBottomZonePx = with(density) { edgeTurnBottomZone.toPx() }

    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val appWidgetHost = remember { InteractiveAppWidgetHost(context, 1024) }
    val hostViews = remember { mutableMapOf<Int, View>() }

    var showDropDown by remember { mutableStateOf(false) }
    var dropDownOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedWidgetId by remember { mutableIntStateOf(-1) }
    var showWidgetSelector by remember { mutableStateOf(false) }

    var drag by remember { mutableStateOf<WidgetDrag?>(null) }
    var edgeScrollDir by remember { mutableIntStateOf(0) }

    var pendingWidgetId by remember { mutableIntStateOf(-1) }

    val hazeState = rememberHazeState()

    val isEditing = selectedWidgetId != -1
    val isDraggingBody = drag != null

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

    val pageCount = remember(widgets, isEditing) {
        val maxWidgetPage = widgets.maxOfOrNull { it.page } ?: 0
        if (isEditing) (maxWidgetPage + 2).coerceAtMost(5)
        else (maxWidgetPage + 1).coerceAtMost(5)
    }

    val pagerState = rememberPagerState(initialPage = 0) { pageCount }

    var keepAllPages by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        keepAllPages = true
    }

    LaunchedEffect(edgeScrollDir, isDraggingBody) {
        if (edgeScrollDir == 0 || !isDraggingBody) return@LaunchedEffect
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
        if (result.resultCode == Activity.RESULT_OK) {
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
            if (pendingWidgetId != -1) {
                runCatching { appWidgetHost.deleteAppWidgetId(pendingWidgetId) }
                pendingWidgetId = -1
            }
        }
    }

    val shortcutLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data ?: return@rememberLauncherForActivityResult
            val intent = data.getParcelableExtra(
                "android.intent.extra.shortcut.INTENT", Intent::class.java
            )
            val name = data.getStringExtra("android.intent.extra.shortcut.NAME")
            val iconRes = data.getParcelableExtra(
                "android.intent.extra.shortcut.ICON_RESOURCE", Intent.ShortcutIconResource::class.java
            )
            val iconBitmap = data.getParcelableExtra(
                "android.intent.extra.shortcut.ICON", Bitmap::class.java
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
                detectTapGestures(
                    onTap = { selectedWidgetId = -1 },
                    onLongPress = { pos ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        dropDownOffset = pos
                        showDropDown = true
                        selectedWidgetId = -1
                    }
                )
            }
    ) {
        AnimatedVisibility(
            visible = isDraggingBody,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            val topIndicatorHeight:Dp = edgeTurnTopZone
            val primaryColor = colorScheme.primary

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
                            val cornerPx = ExtraLargerCornerRadius.toPx()
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
                            val cornerPx = ExtraLargerCornerRadius.toPx()
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

        Box(modifier = Modifier.fillMaxSize()) {
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
                    .fadeEdges(top = gridTopOffset, bottom = gridBottomOffset)
            ) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = if (keepAllPages) pageCount - 1 else 0,
                    userScrollEnabled = !isEditing
                ) { pageIndex ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (widgets.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = BiggestPadding),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clickable { showWidgetSelector = true }
                                        .size(HugestSpacing)
                                        .background(
                                            colorScheme.primaryContainer.copy(alpha = 0.3f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        null,
                                        modifier = Modifier.size(ExtraLargeIconSize),
                                        tint = colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    stringResource(R.string.add_first_widget),
                                    style = MaterialTheme.typography.titleMedium,
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
                                        .offset {
                                            IntOffset(
                                                (animX + horizontalPadding).roundToPx(),
                                                (animY + firstRowTopOffset).roundToPx()
                                            )
                                        }
                                        .size(width = animW, height = animH)
                                        .padding(
                                            horizontal = cellInsetHorizontal,
                                            vertical = cellInsetVertical
                                        )
                                        .graphicsLayer {
                                            alpha = if (isBeingDragged) 0f else 1f
                                            scaleX = restScale
                                            scaleY = restScale
                                        }
                                        .zIndex(if (isSelected) 1f else 0f)

                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(ExtraLargerCornerRadius))
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
                                                    width = SmallerStroke,
                                                    color = colorScheme.primary.copy(alpha = selectionProgress),
                                                    shape = RoundedCornerShape(ExtraLargerCornerRadius)
                                                )
                                        )
                                    }

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
                    val wallpaperLabel = stringResource(R.string.wallpaper)
                    val selectWallpaperLabel = stringResource(R.string.select_wallpaper)
                    val settingsLabel = stringResource(R.string.settings)
                    val addWidgetLabel = stringResource(R.string.add_widget)
                    val editLayoutLabel = stringResource(R.string.edit_layout)
                    val gridSizeFormat = stringResource(R.string.grid_size_format)

                    val menuItems = remember(
                        isLandscape,
                        widgetColumns,
                        primaryColor,
                        isEditing,
                        getRowCountForColumns,
                        wallpaperLabel,
                        selectWallpaperLabel,
                        settingsLabel,
                        addWidgetLabel,
                        editLayoutLabel,
                        gridSizeFormat
                    ) {
                        listOfNotNull(
                            MenuItem(
                                text = wallpaperLabel,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                                    context.startActivity(
                                        Intent.createChooser(
                                            intent,
                                            selectWallpaperLabel
                                        )
                                    )
                                },
                                leadingIcon = { Icon(Icons.Rounded.Wallpaper, null) }
                            ),
                            MenuItem(
                                text = settingsLabel,
                                onClick = { onOpenSettings() },
                                leadingIcon = { Icon(Icons.Rounded.Settings, null) }
                            ),
                            MenuItem(
                                text = addWidgetLabel,
                                onClick = { showWidgetSelector = true },
                                leadingIcon = { Icon(Icons.Rounded.Add, null) }
                            ),
                            if (!isEditing) MenuItem(
                                text = editLayoutLabel,
                                onClick = { selectedWidgetId = -2 },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null) }
                            ) else null
                        ) + gridOptions.map { cols ->
                            val isSelected = widgetColumns == cols
                            val targetRowCount = getRowCountForColumns(cols)
                            MenuItem(
                                text = String.format(gridSizeFormat, cols, targetRowCount),
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

            if (isEditing) {
                val selected = widgets.firstOrNull { it.id == selectedWidgetId }

                val currentSelected by rememberUpdatedState(selected)
                val currentWidgetColumns by rememberUpdatedState(widgetColumns)
                val currentRowCount by rememberUpdatedState(rowCount)

                Box(modifier = Modifier.fillMaxSize()) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(pagerState.currentPage, widgets) {
                                detectTapGestures(onTap = { point ->
                                    val hit = widgetAtPoint(point, pagerState.currentPage)
                                    selectedWidgetId = hit?.id ?: -1
                                })
                            }
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

                                        val moved = active.topLeft + dragAmount

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
                                .offset { IntOffset(previewX.roundToPx(), previewY.roundToPx()) }
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
                                    RoundedCornerShape(ExtraLargerCornerRadius)
                                )
                                .border(
                                    2.dp,
                                    colorScheme.primary.copy(alpha = 0.5f),
                                    RoundedCornerShape(ExtraLargerCornerRadius)
                                )
                        )
                    }

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
                                .offset { IntOffset(handleX.roundToPx(), handleY.roundToPx()) }
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
                                    shadowElevation = LargestElevation.toPx()
                                    shape = RoundedCornerShape(ExtraLargerCornerRadius)
                                    clip = true
                                }
                                .background(
                                    colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    RoundedCornerShape(ExtraLargerCornerRadius)
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
                                        SmallerStroke,
                                        colorScheme.primary,
                                        RoundedCornerShape(ExtraLargerCornerRadius)
                                    )
                            )
                        }
                    }
                }
            }

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
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = MediumSmallElevation),
                        shape = RoundedCornerShape(ExtraLargeCornerRadius),
                        modifier = Modifier.padding(bottom = MediumPadding)
                    ) {
                        Text(stringResource(R.string.done), fontWeight = FontWeight.SemiBold)
                    }

                    if (selectedWidgetId != -1 && selectedWidgetId != -2) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = MediumSmallElevation),
                            shape = RoundedCornerShape(ExtraLargeCornerRadius),
                            contentPadding = PaddingValues(horizontal = ExtraLargerPadding, vertical = LargeMediumPadding)
                        ) {
                            Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(ExtraLargeSpacing))
                            Spacer(Modifier.width(MediumSpacer))
                            Text(stringResource(R.string.remove), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (pageCount > 1) {
                PageIndicator(
                    pagerState = pagerState,
                    pageCount = pageCount,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = MediumSmallPadding)
                        .padding(bottom = gridBottomOffset + SmallPadding)
                )
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
                        component = ComponentName(
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

@Suppress("DiscouragedApi")
private fun resolveForeignDrawableId(
    res: Resources,
    resName: String,
    pkg: String
): Int {
    val direct = res.getIdentifier(resName, null, null)
    if (direct != 0) return direct
    return res.getIdentifier(resName.substringAfterLast("/"), "drawable", pkg)
}

@Composable
fun ShortcutWidgetContent(widget: WidgetItem) {
    val context = LocalContext.current
    val resources = LocalResources.current

    val iconDrawable = remember(widget.shortcutIconRes, widget.shortcutIntent) {
        try {
            if (widget.shortcutIconRes?.startsWith("file:") == true) {
                val fileName = widget.shortcutIconRes.substring(5)
                val file = context.getFileStreamPath(fileName)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                        ?.toDrawable(resources)
                } else null
            } else if (widget.shortcutIconRes != null) {
                val stored = widget.shortcutIconRes
                val pkg = stored.substringBefore(":")
                val resName = stored.substringAfter(":")
                val appRes = context.packageManager.getResourcesForApplication(pkg)
                val id = resolveForeignDrawableId(appRes, resName, pkg)
                if (id != 0) ResourcesCompat.getDrawable(appRes, id, null) else null
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
            modifier = Modifier.padding(MediumPadding)
        ) {
            if (iconDrawable != null) {
                Image(
                    bitmap = iconDrawable.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(if (widget.width > 1) HugeSpacing else ExtraBigSpacing)
                )
            } else {
                Icon(Icons.Rounded.Apps, null, modifier = Modifier.size(ExtraLargeIconSize))
            }

            Text(
                text = widget.shortcutLabel ?: "",
                style = MaterialTheme.typography.labelSmall,
                maxLines = if (widget.height > 1) 2 else 1,
                fontWeight = FontWeight.Medium,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = SmallPadding)
            )
        }
    }
}

@Composable
private fun PageIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MediumSpacer),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(pageCount) { iteration ->
            val isSelected = pagerState.currentPage == iteration
            val dotSize by animateDpAsState(
                if (isSelected) MediumCornerRadius else MediumSmallerCornerRadius,
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

private fun Modifier.fadeEdges(top: Dp, bottom: Dp): Modifier = drawWithCache {
    val topPx = top.roundToPx().toFloat().coerceIn(0f, size.height)
    val bottomPx = bottom.roundToPx().toFloat().coerceIn(0f, size.height - topPx)

    val topStrip = Rect(0f, 0f, size.width, topPx)
    val bottomStrip = Rect(0f, size.height - bottomPx, size.width, size.height)
    val topFade = Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color.Black),
        startY = topStrip.top,
        endY = topStrip.bottom
    )
    val bottomFade = Brush.verticalGradient(
        colors = listOf(Color.Black, Color.Transparent),
        startY = bottomStrip.top,
        endY = bottomStrip.bottom
    )
    val layerPaint = Paint()

    onDrawWithContent {
        clipRect(top = topPx, bottom = size.height - bottomPx) {
            this@onDrawWithContent.drawContent()
        }

        if (topPx > 0f) {
            drawContext.canvas.withSaveLayer(topStrip, layerPaint) {
                drawContent()
                drawRect(
                    brush = topFade,
                    topLeft = topStrip.topLeft,
                    size = topStrip.size,
                    blendMode = BlendMode.DstIn
                )
            }
        }
        if (bottomPx > 0f) {
            drawContext.canvas.withSaveLayer(bottomStrip, layerPaint) {
                drawContent()
                drawRect(
                    brush = bottomFade,
                    topLeft = bottomStrip.topLeft,
                    size = bottomStrip.size,
                    blendMode = BlendMode.DstIn
                )
            }
        }
    }
}