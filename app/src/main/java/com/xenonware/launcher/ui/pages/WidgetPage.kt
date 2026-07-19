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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
    val isWide = configuration.screenWidthDp >= 640

    // Grid horizontal padding
    val horizontalPadding = 24.dp
    val screenWidth = configuration.screenWidthDp.dp - (horizontalPadding * 2)

    // Page height: screen height - dock area (~120dp) - status bar
    val dockHeight = 120.dp
    val pageHeight = configuration.screenHeightDp.dp - dockHeight

    LaunchedEffect(isWide) {
        viewModel.setIsWide(isWide)
    }

    val widgetColumns by viewModel.widgetColumns.collectAsState()
    val widgets by viewModel.widgets.collectAsState()

    val density = LocalDensity.current
    val cellSizeDp = screenWidth / widgetColumns
    val rowCount = (pageHeight / cellSizeDp).roundToInt().coerceAtLeast(1)

    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val appWidgetHost = remember { AppWidgetHost(context, 1024) }

    var showDropDown by remember { mutableStateOf(false) }
    var dropDownOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedWidgetId by remember { mutableIntStateOf(-1) }
    var showWidgetSelector by remember { mutableStateOf(false) }

    val hazeState = rememberHazeState()

    val isEditing = selectedWidgetId != -1

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
                viewModel.addWidget(appWidgetId, pagerState.currentPage, 0, 0, 2.coerceAtMost(widgetColumns), 2)
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
            .statusBarsPadding()
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
        // Pixel-style Dot Grid
        Canvas(modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding)
            .padding(bottom = dockHeight)
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
                .padding(bottom = dockHeight),
            userScrollEnabled = !isEditing
        ) { pageIndex ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding)
            ) {
                widgets.filter { it.page == pageIndex }.forEach { widget ->
                    key(widget.id) {
                        val isSelected = selectedWidgetId == widget.id
                        val widgetInfo = remember(widget.id) { appWidgetManager.getAppWidgetInfo(widget.id) }

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

                        var isDraggingBody by remember { mutableStateOf(false) }
                        val selectionProgress by animateFloatAsState(if (isSelected) 1f else 0f, label = "selection")
                        val liftScale by animateFloatAsState(if (isDraggingBody) 1.05f else if (isSelected) 1.02f else 1f, label = "lift")

                        Box(
                            modifier = Modifier
                                .size(width = animW, height = animH)
                                .padding(4.dp)
                                .scale(liftScale)
                                .zIndex(if (isSelected) 1f else 0f)
                                .offset(x = animX, y = animY)
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
                                        }
                                    },
                                    update = { _ -> },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Selection Border
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

                            // Interaction Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(widget.id, isSelected) {
                                        if (isSelected) {
                                            var moveAccumulated = Offset.Zero
                                            var lastPageTurnTime = 0L
                                            detectDragGestures(
                                                onDragStart = {
                                                    isDraggingBody = true
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    moveAccumulated = Offset.Zero
                                                },
                                                onDragEnd = { isDraggingBody = false },
                                                onDragCancel = { isDraggingBody = false },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    moveAccumulated += dragAmount
                                                    val cellPx = with(density) { cellSizeDp.toPx() }
                                                    val dx = (moveAccumulated.x / cellPx).roundToInt()
                                                    val dy = (moveAccumulated.y / cellPx).roundToInt()

                                                    val touchY = change.position.y
                                                    val screenHeightPx = with(density) { pageHeight.toPx() }
                                                    val edgeThreshold = with(density) { 50.dp.toPx() }
                                                    val now = System.currentTimeMillis()

                                                    // Vertical Page Moving Logic
                                                    if (touchY < edgeThreshold && pagerState.currentPage > 0 && now - lastPageTurnTime > 1000) {
                                                        scope.launch {
                                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                            viewModel.updateWidget(widget.id, pagerState.currentPage - 1, widget.x, widget.y, widget.width, widget.height)
                                                        }
                                                        lastPageTurnTime = now
                                                    } else if (touchY > screenHeightPx - edgeThreshold && pagerState.currentPage < pageCount - 1 && now - lastPageTurnTime > 1000) {
                                                        scope.launch {
                                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                            viewModel.updateWidget(widget.id, pagerState.currentPage + 1, widget.x, widget.y, widget.width, widget.height)
                                                        }
                                                        lastPageTurnTime = now
                                                    }

                                                    if (dx != 0 || dy != 0) {
                                                        val newX = (widget.x + dx).coerceIn(0, (widgetColumns - widget.width).coerceAtLeast(0))
                                                        val newY = (widget.y + dy).coerceIn(0, (rowCount - widget.height).coerceAtLeast(0))
                                                        if (newX != widget.x || newY != widget.y) {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            viewModel.updateWidget(widget.id, widget.page, newX, newY, widget.width, widget.height)
                                                            moveAccumulated = Offset(moveAccumulated.x - (newX - widget.x) * cellPx, moveAccumulated.y - (newY - widget.y) * cellPx)
                                                        }
                                                    }
                                                }
                                            )
                                        } else {
                                            detectTapGestures(
                                                onTap = {
                                                    selectedWidgetId = widget.id
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onLongPress = {
                                                    selectedWidgetId = widget.id
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                            )
                                        }
                                    }
                            )

                            if (isSelected) {
                                val cellPx = with(density) { cellSizeDp.toPx() }

                                // Handles
                                val topAcc = remember { mutableFloatStateOf(0f) }
                                WidgetEditBorder (Alignment.TopCenter) { dragAmount ->
                                    topAcc.floatValue += dragAmount.y
                                    val dy = (topAcc.floatValue / cellPx).roundToInt()
                                    if (dy != 0) {
                                        val maxY = (widget.y + widget.height - minH).coerceAtLeast(0)
                                        val newY = (widget.y + dy).coerceIn(0, maxY)
                                        val newH = (widget.height - (newY - widget.y)).coerceAtLeast(minH)
                                        if (newY != widget.y || newH != widget.height) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.updateWidget(widget.id, widget.page, widget.x, newY, widget.width, newH)
                                            topAcc.floatValue -= (newY - widget.y) * cellPx
                                        }
                                    }
                                }

                                val botAcc = remember { mutableFloatStateOf(0f) }
                                WidgetEditBorder(Alignment.BottomCenter) { dragAmount ->
                                    botAcc.floatValue += dragAmount.y
                                    val dh = (botAcc.floatValue / cellPx).roundToInt()
                                    if (dh != 0) {
                                        val newH = (widget.height + dh).coerceIn(minH, (rowCount - widget.y).coerceAtLeast(minH))
                                        if (newH != widget.height) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.updateWidget(widget.id, widget.page, widget.x, widget.y, widget.width, newH)
                                            botAcc.floatValue -= (newH - widget.height) * cellPx
                                        }
                                    }
                                }

                                val leftAcc = remember { mutableFloatStateOf(0f) }
                                WidgetEditBorder(Alignment.CenterStart) { dragAmount ->
                                    leftAcc.floatValue += dragAmount.x
                                    val dx = (leftAcc.floatValue / cellPx).roundToInt()
                                    if (dx != 0) {
                                        val maxX = (widget.x + widget.width - minW).coerceAtLeast(0)
                                        val newX = (widget.x + dx).coerceIn(0, maxX)
                                        val newW = (widget.width - (newX - widget.x)).coerceAtLeast(minW)
                                        if (newX != widget.x || newW != widget.width) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.updateWidget(widget.id, widget.page, newX, widget.y, newW, widget.height)
                                            leftAcc.floatValue -= (newX - widget.x) * cellPx
                                        }
                                    }
                                }

                                val rightAcc = remember { mutableFloatStateOf(0f) }
                                WidgetEditBorder(Alignment.CenterEnd) { dragAmount ->
                                    rightAcc.floatValue += dragAmount.x
                                    val dw = (rightAcc.floatValue / cellPx).roundToInt()
                                    if (dw != 0) {
                                        val maxAllowedW = (widgetColumns - widget.x).coerceAtLeast(minW)
                                        val newW = (widget.width + dw).coerceIn(minW, maxAllowedW)
                                        if (newW != widget.width) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.updateWidget(widget.id, widget.page, widget.x, widget.y, newW, widget.height)
                                            rightAcc.floatValue -= (newW - widget.width) * cellPx
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Remove Bar
        AnimatedVisibility(
            visible = isEditing,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) {
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

        if (showDropDown) {
            val dropDownOffsetDpX = with(density) { dropDownOffset.x.toDp() }
            val dropDownOffsetDpY = with(density) { dropDownOffset.y.toDp() }
            val gridOptions = if (isWide) listOf(6, 8, 10) else listOf(3, 4, 5)
            val primaryColor = colorScheme.primary

            val menuItems = remember(isWide, widgetColumns, primaryColor) {
                listOf(
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
                    )
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

        // Vertical Page Indicator
        if (pageCount > 1) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .padding(bottom = dockHeight),
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

    if (showWidgetSelector) {
        val installedWidgets by viewModel.installedWidgets.collectAsState()
        WidgetSelectorDialog(
            installedWidgets = installedWidgets,
            onDismiss = { showWidgetSelector = false },
            onWidgetSelected = { info ->
                val appWidgetId = appWidgetHost.allocateAppWidgetId()
                val success = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.provider)
                if (success) {
                    viewModel.addWidget(appWidgetId, pagerState.currentPage, 0, 0, 2.coerceAtMost(widgetColumns), 2)
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