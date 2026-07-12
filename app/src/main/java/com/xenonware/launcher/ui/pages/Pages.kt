package com.xenonware.launcher.ui.pages

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.notification.LauncherNotification
import com.xenonware.launcher.ui.res.MenuItem
import com.xenonware.launcher.ui.res.XenonDropDown
import com.xenonware.launcher.viewmodel.LauncherViewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MediaPage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Media Player Fullscreen", color = Color.White, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun MainHomePage(
    notificationCount: Int,
    currentDate: String,
    notifications: List<LauncherNotification>,
    apps: List<AppInfo>,
    calendarEvents: List<com.xenonware.launcher.viewmodel.CalendarEvent>,
    onDismissNotification: (String) -> Unit,
    onDismissAllNotifications: () -> Unit
) {
    var selectedPackage by remember { mutableStateOf<String?>(null) }
    
    val groupedNotifications = remember(notifications) {
        notifications.groupBy { it.packageName }
    }
    
    val sortedAppPackages = remember(groupedNotifications) {
        groupedNotifications.keys.sortedByDescending { pkg ->
            groupedNotifications[pkg]?.maxOfOrNull { it.postTime } ?: 0L
        }
    }
    
    LaunchedEffect(sortedAppPackages) {
        if (selectedPackage == null || !sortedAppPackages.contains(selectedPackage)) {
            selectedPackage = sortedAppPackages.firstOrNull()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // At a Glance section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = currentDate,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            if (calendarEvents.isEmpty()) {
                Text(
                    text = "No upcoming events",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            } else {
                calendarEvents.take(2).forEach { event ->
                    Text(
                        text = event.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (notificationCount == 0) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "You're up to date",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 18.sp,
                        fontFamily = QuicksandTitleVariable,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
//            Spacer(modifier = Modifier.weight(1f))

            // Notification List
            selectedPackage?.let { pkg ->
                val app = apps.find { it.packageName == pkg }
                val appColor = remember(app) { getDominantColor(app?.icon) }
                
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(groupedNotifications[pkg] ?: emptyList(), key = { it.key }) { notification ->
                        NotificationItem(
                            notification = notification,
                            appColor = appColor,
                            onOpen = { 
                                try {
                                    notification.contentIntent?.send()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        )
                    }
                }
            } ?: Spacer(modifier = Modifier.weight(1f))

            // Notification Tabs
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 110.dp)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            val fadeWidth = 16.dp.toPx()
                            if (scrollState.value > 0) {
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0f to Color.Transparent,
                                        1f to Color.Black,
                                        startX = 0f,
                                        endX = fadeWidth
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                            if (scrollState.value < scrollState.maxValue) {
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0f to Color.Black,
                                        1f to Color.Transparent,
                                        startX = size.width - fadeWidth,
                                        endX = size.width
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        sortedAppPackages.forEach { pkg ->
                            val app = apps.find { it.packageName == pkg }
                            val notificationsForApp = groupedNotifications[pkg] ?: emptyList()
                            val latestNotification = notificationsForApp.firstOrNull()
                            val isSelected = selectedPackage == pkg
                            val appColor = remember(app) { getDominantColor(app?.icon) }
                            val contrastColor = remember(appColor) { getContrastColor(appColor) }
                            
                            NotificationTabButton(
                                app = app,
                                notificationIcon = latestNotification?.icon,
                                notificationCount = notificationsForApp.size,
                                isSelected = isSelected,
                                appColor = appColor,
                                contrastColor = contrastColor,
                                onClick = { selectedPackage = pkg },
                                modifier = Modifier.widthIn(min = 82.dp)
                            )
                        }
                    }
                }

                // Delete All Button
                val deleteInteractionSource = remember { MutableInteractionSource() }
                val isDeletePressed by deleteInteractionSource.collectIsPressedAsState()
                val deleteCornerRadius by animateDpAsState(if (isDeletePressed) 4.dp else 12.dp, label = "delete_corner")

                Surface(
                    onClick = onDismissAllNotifications,
                    interactionSource = deleteInteractionSource,
                    shape = RoundedCornerShape(deleteCornerRadius),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .height(40.dp)
                        .widthIn(min = 64.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Clear All",
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        if (notificationCount > 0) {
            Spacer(modifier = Modifier.weight(0.2f)) // Space above dock
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun NotificationItem(
    notification: LauncherNotification,
    appColor: Color,
    onOpen: () -> Unit
) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val iconToDraw = notification.icon
            if (iconToDraw != null) {
                Image(
                    bitmap = iconToDraw.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    colorFilter = ColorFilter.tint(appColor)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                notification.title?.let {
                    Text(
                        text = it,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                notification.text?.let {
                    Text(
                        text = it,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationTabButton(
    app: AppInfo?,
    notificationIcon: Drawable?,
    notificationCount: Int,
    isSelected: Boolean,
    appColor: Color,
    contrastColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val cornerRadius by animateDpAsState(
        targetValue = when {
            isPressed -> 4.dp
            isSelected -> 12.dp
            else -> 20.dp
        }
    )

    val backgroundColor = if (isSelected) appColor else MaterialTheme.colorScheme.surfaceDim.copy(alpha = 0.4f)
    val iconColor = if (isSelected) contrastColor else MaterialTheme.colorScheme.onSurface
    
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        modifier = modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val iconToDraw = notificationIcon ?: app?.icon
            if (iconToDraw != null) {
                Image(
                    bitmap = iconToDraw.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(iconColor)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconColor
                )
            }

            AnimatedVisibility(
                visible = notificationCount > 1,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                        color = iconColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = QuicksandTitleVariable
                    )
                }
            }
        }
    }
}

private fun getDominantColor(drawable: Drawable?): Color {
    if (drawable == null) return Color.Gray
    return try {
        val bitmap = drawable.toBitmap()
        
        // 1. Use Palette for brand-aware color extraction
        val palette = Palette.from(bitmap).generate()
        
        // YouTube/Reddit fix: Prioritize vibrant brand colors
        val swatch = palette.darkVibrantSwatch
            ?: palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.dominantSwatch

        val rawColor = if (swatch != null) {
            swatch.rgb
        } else {
            // 2. Fallback center logic
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            var bestColor: Int? = null
            var maxSaturation = -1f
            val steps = 5
            for (i in 1 until steps) {
                for (j in 1 until steps) {
                    val x = (width * i) / steps
                    val y = (height * j) / steps
                    val pixel = pixels[y * width + x]
                    val hsv = FloatArray(3)
                    android.graphics.Color.colorToHSV(pixel, hsv)
                    val score = hsv[1] * hsv[2]
                    if (score > maxSaturation && hsv[2] > 0.1f && hsv[2] < 0.95f) {
                        maxSaturation = score
                        bestColor = pixel
                    }
                }
            }
            bestColor ?: pixels[height/2 * width + width/2]
        }

        // Tone down the color to avoid "eye-burning" intensity
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(rawColor, hsv)

        // Cap saturation (max 70%) and brightness (max 80%)
        // This keeps the brand identity but makes it much more comfortable to look at
        hsv[1] = hsv[1].coerceAtMost(0.7f)
        hsv[2] = hsv[2].coerceAtMost(0.8f)

        Color(android.graphics.Color.HSVToColor(hsv)).copy(alpha = 1f)
    } catch (e: Exception) {
        Color.Gray
    }
}

private fun getContrastColor(color: Color): Color {
    // Standard relative luminance formula
    val luminance = 0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue
    
    // Increased threshold (0.72) to favor white icons on brand colors (like WhatsApp green)
    // even after they have been muted/de-saturated.
    return if (luminance > 0.72) Color.Black else Color.White
}

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
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = selectionProgress),
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
                                val topAcc = remember { mutableStateOf(0f) }
                                PixelResizeHandle(Alignment.TopCenter) { dragAmount ->
                                    topAcc.value += dragAmount.y
                                    val dy = (topAcc.value / cellPx).roundToInt()
                                    if (dy != 0) {
                                        val maxY = (widget.y + widget.height - minH).coerceAtLeast(0)
                                        val newY = (widget.y + dy).coerceIn(0, maxY)
                                        val newH = (widget.height - (newY - widget.y)).coerceAtLeast(minH)
                                        if (newY != widget.y || newH != widget.height) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.updateWidget(widget.id, widget.page, widget.x, newY, widget.width, newH)
                                            topAcc.value -= (newY - widget.y) * cellPx
                                        }
                                    }
                                }

                                val botAcc = remember { mutableStateOf(0f) }
                                PixelResizeHandle(Alignment.BottomCenter) { dragAmount ->
                                    botAcc.value += dragAmount.y
                                    val dh = (botAcc.value / cellPx).roundToInt()
                                    if (dh != 0) {
                                        val newH = (widget.height + dh).coerceIn(minH, (rowCount - widget.y).coerceAtLeast(minH))
                                        if (newH != widget.height) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.updateWidget(widget.id, widget.page, widget.x, widget.y, widget.width, newH)
                                            botAcc.value -= (newH - widget.height) * cellPx
                                        }
                                    }
                                }

                                val leftAcc = remember { mutableStateOf(0f) }
                                PixelResizeHandle(Alignment.CenterStart) { dragAmount ->
                                    leftAcc.value += dragAmount.x
                                    val dx = (leftAcc.value / cellPx).roundToInt()
                                    if (dx != 0) {
                                        val maxX = (widget.x + widget.width - minW).coerceAtLeast(0)
                                        val newX = (widget.x + dx).coerceIn(0, maxX)
                                        val newW = (widget.width - (newX - widget.x)).coerceAtLeast(minW)
                                        if (newX != widget.x || newW != widget.width) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.updateWidget(widget.id, widget.page, newX, widget.y, newW, widget.height)
                                            leftAcc.value -= (newX - widget.x) * cellPx
                                        }
                                    }
                                }

                                val rightAcc = remember { mutableStateOf(0f) }
                                PixelResizeHandle(Alignment.CenterEnd) { dragAmount ->
                                    rightAcc.value += dragAmount.x
                                    val dw = (rightAcc.value / cellPx).roundToInt()
                                    if (dw != 0) {
                                        val maxAllowedW = (widgetColumns - widget.x).coerceAtLeast(minW)
                                        val newW = (widget.width + dw).coerceIn(minW, maxAllowedW)
                                        if (newW != widget.width) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.updateWidget(widget.id, widget.page, widget.x, widget.y, newW, widget.height)
                                            rightAcc.value -= (newW - widget.width) * cellPx
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
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
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
            val primaryColor = MaterialTheme.colorScheme.primary
            
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

@Composable
fun WidgetSelectorDialog(
    installedWidgets: Map<LauncherViewModel.AppWidgetGroup, List<AppWidgetProviderInfo>>,
    onDismiss: () -> Unit,
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Widgets",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    installedWidgets.forEach { (group, widgets) ->
                        item {
                            var expanded by remember { mutableStateOf(false) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .clickable { expanded = !expanded }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (group.icon != null) {
                                        Image(
                                            bitmap = group.icon.toBitmap().asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    } else {
                                        Icon(Icons.Rounded.Apps, null, modifier = Modifier.size(32.dp))
                                    }
                                    
                                    Text(group.appName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    
                                    Icon(
                                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        null
                                    )
                                }
                                
                                if (expanded) {
                                    Spacer(Modifier.height(8.dp))
                                    widgets.forEach { info ->
                                        WidgetPickerItem(info, onWidgetSelected)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetPickerItem(
    info: AppWidgetProviderInfo,
    onWidgetSelected: (AppWidgetProviderInfo) -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val density = LocalDensity.current
    
    val preview = remember(info) {
        try {
            info.loadPreviewImage(context, density.density.toInt())
        } catch (_: Exception) {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onWidgetSelected(info) }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (preview != null && preview.intrinsicWidth > 0 && preview.intrinsicHeight > 0) {
            Image(
                bitmap = preview.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1.5f)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Widgets, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        Text(
            info.loadLabel(pm) ?: "Widget",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            "${info.minWidth / 40}x${info.minHeight / 40}", // Rough estimation
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun BoxScope.PixelResizeHandle(
    alignment: Alignment,
    onDrag: (Offset) -> Unit
) {
    val handleSize = 14.dp
    val xOffset = when (alignment) {
        Alignment.CenterStart -> -handleSize / 2
        Alignment.CenterEnd -> handleSize / 2
        else -> 0.dp
    }
    val yOffset = when (alignment) {
        Alignment.TopCenter -> -handleSize / 2
        Alignment.BottomCenter -> handleSize / 2
        else -> 0.dp
    }
    
    Box(
        modifier = Modifier
            .align(alignment)
            .offset(x = xOffset, y = yOffset)
            .size(handleSize)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            }
    )
}
