package com.xenonware.launcher.ui.pages

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.xenonware.launcher.ui.res.MenuItem
import com.xenonware.launcher.ui.res.WidgetEditBorder
import com.xenonware.launcher.ui.res.WidgetSelectorDialog
import com.xenonware.launcher.ui.res.XenonDropDown
import com.xenonware.launcher.viewmodel.LauncherViewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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
    val isWide = configuration.screenWidthDp >= 640

    // Grid layout constants
    val horizontalPadding = 24.dp
    val screenWidth = configuration.screenWidthDp.dp - (horizontalPadding * 2)

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    
    // Dock area implementation: safe draw (nav bar) + base dock area
    val dockBaseHeight = 110.dp
    val totalDockAreaHeight = navBarHeight + dockBaseHeight
    
    val widgetColumns by viewModel.widgetColumns.collectAsState()
    val widgets by viewModel.widgets.collectAsState()

    val cellSizeDp = screenWidth / widgetColumns
    
    // Optimized row calculation to fit maximum widgets (e.g., 4x7)
    val gridAreaHeight = configuration.screenHeightDp.dp - statusBarHeight - totalDockAreaHeight
    val rowCount = (gridAreaHeight / cellSizeDp).toInt().coerceAtLeast(1)
    
    // Calculate precise vertical padding to center the grid rows
    val verticalPadding = (gridAreaHeight - (cellSizeDp * rowCount)) / 2

    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val appWidgetHost = remember { AppWidgetHost(context, 1024) }

    var showDropDown by remember { mutableStateOf(false) }
    var dropDownOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedWidgetId by remember { mutableIntStateOf(-1) }
    var showWidgetSelector by remember { mutableStateOf(false) }
    var isDraggingBody by remember { mutableStateOf(false) }

    val hazeState = rememberHazeState()

    val isEditing = selectedWidgetId != -1

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

    // Pages only exist if they have content, plus one extra if in edit mode
    val pageCount = remember(widgets, isEditing) {
        val maxWidgetPage = widgets.maxOfOrNull { it.page } ?: 0
        if (isEditing) (maxWidgetPage + 2).coerceAtMost(5)
        else (maxWidgetPage + 1).coerceAtMost(5)
    }

    val pagerState = rememberPagerState(initialPage = 0) { pageCount }

    val pickWidgetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (appWidgetId != -1) {
                val w = 2.coerceAtMost(widgetColumns)
                val h = 2
                val space = findFirstAvailableSpace(w, h, pagerState.currentPage)
                if (space != null) {
                    viewModel.addWidget(appWidgetId, space.first, space.second, space.third, w, h)
                    scope.launch {
                        pagerState.animateScrollToPage(space.first)
                    }
                } else {
                    viewModel.addWidget(appWidgetId, pagerState.currentPage, 0, 0, w, h)
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
            .hazeSource(hazeState)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { selectedWidgetId = -1 },
                    onLongPress = { offset ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        dropDownOffset = offset
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
            // Precision indicator heights to cover exactly 50% of first/last rows
            val topIndicatorHeight = statusBarHeight + verticalPadding + (cellSizeDp / 2)
            val bottomIndicatorHeight = totalDockAreaHeight + verticalPadding + (cellSizeDp / 2)
            val primaryColor = colorScheme.primary
            
            Box(modifier = Modifier.fillMaxSize()) {
                if (pagerState.currentPage > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(topIndicatorHeight)
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent)
                                )
                            )
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cornerPx = 24.dp.toPx()
                            val path = Path().apply {
                                moveTo(0f, size.height - cornerPx)
                                quadraticBezierTo(0f, size.height, cornerPx, size.height)
                                lineTo(size.width - cornerPx, size.height)
                                quadraticBezierTo(size.width, size.height, size.width, size.height - cornerPx)
                            }
                            drawPath(
                                path = path,
                                color = primaryColor.copy(alpha = 0.3f),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                }
                if (pagerState.currentPage < pageCount - 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(bottomIndicatorHeight)
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(Color.Transparent, primaryColor.copy(alpha = 0.25f))
                                )
                            )
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cornerPx = 24.dp.toPx()
                            val path = Path().apply {
                                moveTo(0f, cornerPx)
                                quadraticBezierTo(0f, 0f, cornerPx, 0f)
                                lineTo(size.width - cornerPx, 0f)
                                quadraticBezierTo(size.width, 0f, size.width, cornerPx)
                            }
                            drawPath(
                                path = path,
                                color = primaryColor.copy(alpha = 0.3f),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                }
            }
        }

        // Main Content Layer with Status Bar Padding
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Pixel-style Dot Grid
            Canvas(modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                .padding(bottom = totalDockAreaHeight - navBarHeight) // Adjust for dock
            ) {
                if (gridAlpha > 0f) {
                    val cellPx = cellSizeDp.toPx()
                    val dotRadius = 0.8.dp.toPx()
                    val color = Color.White.copy(alpha = gridAlpha)

                    for (i in 0..widgetColumns) {
                        for (j in 0..rowCount) {
                            drawCircle(color, dotRadius, Offset(i * cellPx, j * cellPx))
                        }
                    }
                }
            }

            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = totalDockAreaHeight),
                userScrollEnabled = !isEditing
            ) { pageIndex ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                ) {
                    widgets.filter { it.page == pageIndex }.forEach { widget ->
                        key(widget.id) {
                            val isSelected = selectedWidgetId == widget.id
                            val widgetInfo = remember(widget.id) { appWidgetManager.getAppWidgetInfo(widget.id) }
                            
                            val currentWidget by rememberUpdatedState(widget)
                            val currentWidgetColumns by rememberUpdatedState(widgetColumns)
                            val currentRowCount by rememberUpdatedState(rowCount)
                            val currentIsEditing by rememberUpdatedState(isEditing)
                            val currentIsSelected by rememberUpdatedState(isSelected)

                            val minW = if (widgetInfo != null) (widgetInfo.minWidth / cellSizeDp.value).roundToInt().coerceIn(1, widgetColumns) else 1
                            val minH = if (widgetInfo != null) (widgetInfo.minHeight / cellSizeDp.value).roundToInt().coerceIn(1, rowCount) else 1

                            val animX by animateDpAsState(
                                targetValue = (widget.x * cellSizeDp.value).dp,
                                animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
                                label = "animX"
                            )
                            val animY by animateDpAsState(
                                targetValue = (widget.y * cellSizeDp.value).dp,
                                animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
                                label = "animY"
                            )
                            val animW by animateDpAsState(
                                targetValue = (widget.width * cellSizeDp.value).dp,
                                animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
                                label = "animW"
                            )
                            val animH by animateDpAsState(
                                targetValue = (widget.height * cellSizeDp.value).dp,
                                animationSpec = spring(stiffness = 500f, dampingRatio = 0.8f),
                                label = "animH"
                            )

                            val selectionProgress by animateFloatAsState(if (isSelected) 1f else 0f, label = "selection")
                            val liftScale by animateFloatAsState(if (isDraggingBody && isSelected) 1.05f else if (isSelected) 1.02f else 1f, label = "lift")

                            Box(
                                modifier = Modifier
                                    .offset(x = animX, y = animY)
                                    .size(width = animW, height = animH)
                                    .padding(4.dp)
                                    .scale(liftScale)
                                    .zIndex(if (isSelected) 1f else 0f)
                            ) {
                                // Widget Content Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(24.dp))
                                ) {
                                    AndroidView(
                                        factory = { ctx ->
                                            appWidgetHost.createView(ctx, widget.id, widgetInfo).apply {
                                                setPadding(0, 0, 0, 0)
                                                setOnLongClickListener { 
                                                    selectedWidgetId = widget.id
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    true 
                                                }
                                            }
                                        },
                                        update = { _ -> },
                                        modifier = Modifier.fillMaxSize()
                                    )

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

                                if (currentIsEditing) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pointerInput(widget.id, currentIsSelected) {
                                                var moveAccumulated = Offset.Zero
                                                var lastPageTurnTime = 0L

                                                val handleDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Offset ->
                                                    change.consume()
                                                    moveAccumulated += dragAmount
                                                    val cellPx = with(density) { cellSizeDp.toPx() }
                                                    val dx = (moveAccumulated.x / cellPx).roundToInt()
                                                    val dy = (moveAccumulated.y / cellPx).roundToInt()

                                                    val touchYInPager = change.position.y + with(density) { animY.toPx() }
                                                    val pagerHeightPx = with(density) { gridAreaHeight.toPx() }
                                                    val edgeThreshold = with(density) { 100.dp.toPx() }
                                                    val now = System.currentTimeMillis()

                                                    if (touchYInPager < edgeThreshold && pagerState.currentPage > 0 && now - lastPageTurnTime > 1200) {
                                                        val newPage = pagerState.currentPage - 1
                                                        var targetX = currentWidget.x
                                                        var targetY = currentWidget.y

                                                        if (!isAreaVacant(currentWidget.id, newPage, targetX, targetY, currentWidget.width, currentWidget.height)) {
                                                            val space = findFirstAvailableSpace(currentWidget.width, currentWidget.height, newPage)
                                                            if (space != null && space.first == newPage) {
                                                                targetX = space.second
                                                                targetY = space.third
                                                            }
                                                        }

                                                        if (isAreaVacant(currentWidget.id, newPage, targetX, targetY, currentWidget.width, currentWidget.height)) {
                                                            scope.launch {
                                                                pagerState.animateScrollToPage(newPage)
                                                                viewModel.updateWidget(currentWidget.id, newPage, targetX, targetY, currentWidget.width, currentWidget.height)
                                                            }
                                                            lastPageTurnTime = now
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        }
                                                    } else if (touchYInPager > pagerHeightPx - edgeThreshold && pagerState.currentPage < pageCount - 1 && now - lastPageTurnTime > 1200) {
                                                        val newPage = pagerState.currentPage + 1
                                                        var targetX = currentWidget.x
                                                        var targetY = currentWidget.y

                                                        if (!isAreaVacant(currentWidget.id, newPage, targetX, targetY, currentWidget.width, currentWidget.height)) {
                                                            val space = findFirstAvailableSpace(currentWidget.width, currentWidget.height, newPage)
                                                            if (space != null && space.first == newPage) {
                                                                targetX = space.second
                                                                targetY = space.third
                                                            }
                                                        }

                                                        if (isAreaVacant(currentWidget.id, newPage, targetX, targetY, currentWidget.width, currentWidget.height)) {
                                                            scope.launch {
                                                                pagerState.animateScrollToPage(newPage)
                                                                viewModel.updateWidget(currentWidget.id, newPage, targetX, targetY, currentWidget.width, currentWidget.height)
                                                            }
                                                            lastPageTurnTime = now
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        }
                                                    }

                                                    if (dx != 0 || dy != 0) {
                                                        val newX = (currentWidget.x + dx).coerceIn(0, (currentWidgetColumns - currentWidget.width).coerceAtLeast(0))
                                                        val newY = (currentWidget.y + dy).coerceIn(0, (currentRowCount - currentWidget.height).coerceAtLeast(0))
                                                        
                                                        if ((newX != currentWidget.x || newY != currentWidget.y) && isAreaVacant(currentWidget.id, currentWidget.page, newX, newY, currentWidget.width, currentWidget.height)) {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            viewModel.updateWidget(currentWidget.id, currentWidget.page, newX, newY, currentWidget.width, currentWidget.height)
                                                            moveAccumulated = Offset(
                                                                moveAccumulated.x - (newX - currentWidget.x) * cellPx,
                                                                moveAccumulated.y - (newY - currentWidget.y) * cellPx
                                                            )
                                                        }
                                                    }
                                                }

                                                if (currentIsSelected) {
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            isDraggingBody = true
                                                            moveAccumulated = Offset.Zero
                                                        },
                                                        onDragEnd = { isDraggingBody = false },
                                                        onDragCancel = { isDraggingBody = false },
                                                        onDrag = { change, dragAmount -> handleDrag(change, dragAmount) }
                                                    )
                                                } else {
                                                    detectTapGestures(onTap = { selectedWidgetId = widget.id })
                                                }
                                            }
                                    )
                                }

                                if (isSelected) {
                                    val cellPx = with(density) { cellSizeDp.toPx() }

                                    val topAcc = remember { mutableFloatStateOf(0f) }
                                    WidgetEditBorder (Alignment.TopCenter) { dragAmount ->
                                        topAcc.floatValue += dragAmount.y
                                        val dy = (topAcc.floatValue / cellPx).roundToInt()
                                        if (dy != 0) {
                                            val maxY = (currentWidget.y + currentWidget.height - minH).coerceAtLeast(0)
                                            val newY = (currentWidget.y + dy).coerceIn(0, maxY)
                                            val newH = (currentWidget.height - (newY - currentWidget.y)).coerceAtLeast(minH)
                                            if ((newY != currentWidget.y || newH != currentWidget.height) && isAreaVacant(currentWidget.id, currentWidget.page, currentWidget.x, newY, currentWidget.width, newH)) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.updateWidget(currentWidget.id, currentWidget.page, currentWidget.x, newY, currentWidget.width, newH)
                                                topAcc.floatValue -= (newY - currentWidget.y) * cellPx
                                            }
                                        }
                                    }

                                    val botAcc = remember { mutableFloatStateOf(0f) }
                                    WidgetEditBorder(Alignment.BottomCenter) { dragAmount ->
                                        botAcc.floatValue += dragAmount.y
                                        val dh = (botAcc.floatValue / cellPx).roundToInt()
                                        if (dh != 0) {
                                            val newH = (currentWidget.height + dh).coerceIn(minH, (currentRowCount - currentWidget.y).coerceAtLeast(minH))
                                            if (newH != currentWidget.height && isAreaVacant(currentWidget.id, currentWidget.page, currentWidget.x, currentWidget.y, currentWidget.width, newH)) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.updateWidget(currentWidget.id, currentWidget.page, currentWidget.x, currentWidget.y, currentWidget.width, newH)
                                                botAcc.floatValue -= (newH - currentWidget.height) * cellPx
                                            }
                                        }
                                    }

                                    val leftAcc = remember { mutableFloatStateOf(0f) }
                                    WidgetEditBorder(Alignment.CenterStart) { dragAmount ->
                                        leftAcc.floatValue += dragAmount.x
                                        val dx = (leftAcc.floatValue / cellPx).roundToInt()
                                        if (dx != 0) {
                                            val maxX = (currentWidget.x + currentWidget.width - minW).coerceAtLeast(0)
                                            val newX = (currentWidget.x + dx).coerceIn(0, maxX)
                                            val newW = (currentWidget.width - (newX - currentWidget.x)).coerceAtLeast(minW)
                                            if ((newX != currentWidget.x || newW != currentWidget.width) && isAreaVacant(currentWidget.id, currentWidget.page, newX, currentWidget.y, newW, currentWidget.height)) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.updateWidget(currentWidget.id, currentWidget.page, newX, currentWidget.y, newW, currentWidget.height)
                                                leftAcc.floatValue -= (newX - currentWidget.x) * cellPx
                                            }
                                        }
                                    }

                                    val rightAcc = remember { mutableFloatStateOf(0f) }
                                    WidgetEditBorder(Alignment.CenterEnd) { dragAmount ->
                                        rightAcc.floatValue += dragAmount.x
                                        val dw = (rightAcc.floatValue / cellPx).roundToInt()
                                        if (dw != 0) {
                                            val maxAllowedW = (currentWidgetColumns - currentWidget.x).coerceAtLeast(minW)
                                            val newW = (currentWidget.width + dw).coerceIn(minW, maxAllowedW)
                                            if (newW != currentWidget.width && isAreaVacant(currentWidget.id, currentWidget.page, currentWidget.x, currentWidget.y, newW, currentWidget.height)) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                viewModel.updateWidget(currentWidget.id, currentWidget.page, currentWidget.x, currentWidget.y, newW, currentWidget.height)
                                                rightAcc.floatValue -= (newW - currentWidget.width) * cellPx
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Remove Bar - Fixed sibling to pager
            AnimatedVisibility(
                visible = isEditing,
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
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.SemiBold)
                    }
                    
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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

            if (showDropDown) {
                val dropDownOffsetDpX = with(density) { dropDownOffset.x.toDp() }
                val dropDownOffsetDpY = with(density) { dropDownOffset.y.toDp() }
                val gridOptions = if (isWide) listOf(6, 8, 10) else listOf(3, 4, 5)
                val primaryColor = colorScheme.primary

                val menuItems = remember(isWide, widgetColumns, primaryColor, isEditing) {
                    listOfNotNull(
                        MenuItem(
                            text = "Wallpaper",
                            onClick = {
                                val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                                context.startActivity(Intent.createChooser(intent, "Select Wallpaper"))
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
                            leadingIcon = { Icon(Icons.Rounded.Settings, null) }
                        ) else null
                    ) + gridOptions.map { cols ->
                        MenuItem(
                            text = "Grid Size ${cols}x$rowCount",
                            onClick = { viewModel.setWidgetColumns(cols) },
                            leadingIcon = { Icon(Icons.Rounded.AspectRatio, null) },
                            textColor = if (widgetColumns == cols) primaryColor else null
                        )
                    }
                }

                XenonDropDown(
                    expanded = showDropDown,
                    onDismissRequest = { showDropDown = false },
                    items = menuItems,
                    hazeState = hazeState,
                    offsetX = dropDownOffsetDpX,
                    offsetY = dropDownOffsetDpY,
                    alignment = Alignment.TopStart
                )
            }

            // Vertical Page Indicator - Fixed sibling to pager
            if (pageCount > 1) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .padding(bottom = totalDockAreaHeight),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(pageCount) { iteration ->
                        val isSelected = pagerState.currentPage == iteration
                        val size by animateDpAsState(if (isSelected) 8.dp else 6.dp, label = "dotSize")
                        val alpha by animateFloatAsState(if (isSelected) 1f else 0.4f, label = "dotAlpha")

                        Box(
                            modifier = Modifier
                                .size(size)
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
            onWidgetSelected = { info ->
                val appWidgetId = appWidgetHost.allocateAppWidgetId()
                val success = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider)
                val w = 2.coerceAtMost(widgetColumns)
                val h = 2
                val space = findFirstAvailableSpace(w, h, pagerState.currentPage)
                val (targetPage, targetX, targetY) = space ?: Triple(pagerState.currentPage, 0, 0)

                if (success) {
                    viewModel.addWidget(appWidgetId, targetPage, targetX, targetY, w, h)
                    scope.launch {
                        pagerState.animateScrollToPage(targetPage)
                    }
                } else {
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                    }
                    pickWidgetLauncher.launch(intent)
                }
                showWidgetSelector = false
            }
        )
    }
}
