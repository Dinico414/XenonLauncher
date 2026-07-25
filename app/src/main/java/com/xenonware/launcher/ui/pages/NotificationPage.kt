package com.xenonware.launcher.ui.pages

import android.app.ActivityOptions
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.notification.LauncherNotification
import com.xenonware.launcher.ui.res.notification.NotificationItem
import com.xenonware.launcher.ui.res.notification.NotificationTabButton
import com.xenonware.launcher.viewmodel.LauncherViewModel
import kotlin.math.abs

@Composable
fun NotificationPage(
    viewModel: LauncherViewModel,
    notificationCount: Int,
    currentDate: String,
    notifications: List<LauncherNotification>,
    apps: List<AppInfo>,
    calendarEvents: List<com.xenonware.launcher.viewmodel.CalendarEvent>,
    onDismissNotification: (String) -> Unit,
    onDismissAllNotifications: () -> Unit
) {
    var selectedPackage by remember { mutableStateOf<String?>(null) }
    val view = LocalView.current
    val offsets = remember { mutableStateMapOf<String, Float>() }
    var deleteButtonBounds by remember { mutableStateOf(Rect.Zero) }

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

    val context = LocalContext.current
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 72dp (dock) + 8dp (dock padding) + 8dp (gap) + 4dp (to match widget vertical padding)
    val dockAreaHeight = 72.dp + navBarHeight + 8.dp + 8.dp + 4.dp

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
            Spacer(Modifier.height(dockAreaHeight)) // Accounts for dock area to center correctly
        } else {
            // Notification List
            selectedPackage?.let { pkg ->
                val app = apps.find { it.packageName == pkg }
                val appColor = remember(app) { getDominantColor(app?.icon) }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                ) {
                    val notificationsInGroup = groupedNotifications[pkg]?.reversed() ?: emptyList()
                    itemsIndexed(notificationsInGroup, key = { _, it -> it.key }) { index, notification ->
                        val context = LocalContext.current
                        val offsetAbove = if (index > 0) offsets[notificationsInGroup[index - 1].key] ?: 0f else 0f
                        val offsetBelow = if (index < notificationsInGroup.size - 1) offsets[notificationsInGroup[index + 1].key] ?: 0f else 0f

                        NotificationItem(
                            notification = notification,
                            appColor = appColor,
                            isFirst = index == 0,
                            isLast = index == notificationsInGroup.size - 1,
                            offsetAbove = offsetAbove,
                            offsetBelow = offsetBelow,
                            onOffsetChanged = { offsets[notification.key] = it },
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(durationMillis = 200),
                                placementSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                fadeOutSpec = tween(durationMillis = 200)
                            ),
                            onOpen = {
                                try {
                                    Log.d(
                                        "XenonNotification",
                                        "Opening notification: pkg=${notification.packageName}, title=${notification.title}"
                                    )

                                    val options = ActivityOptions.makeBasic()
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                        options.setPendingIntentBackgroundActivityStartMode(
                                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                                        )
                                    }

                                    notification.contentIntent?.let { intent ->
                                        Log.d(
                                            "XenonNotification",
                                            "Sending contentIntent with context: $intent"
                                        )
                                        intent.send(
                                            context,
                                            0,
                                            null,
                                            null,
                                            null,
                                            null,
                                            options.toBundle()
                                        )
                                    } ?: Log.w(
                                        "XenonNotification",
                                        "No contentIntent found for notification"
                                    )
                                } catch (e: Exception) {
                                    Log.e(
                                        "XenonNotification",
                                        "Failed to send contentIntent with context",
                                        e
                                    )
                                    try {
                                        Log.d(
                                            "XenonNotification",
                                            "Retrying contentIntent without context"
                                        )
                                        notification.contentIntent?.send()
                                    } catch (e2: Exception) {
                                        Log.e(
                                            "XenonNotification",
                                            "Failed to send contentIntent without context",
                                            e2
                                        )
                                    }
                                }
                            },
                            onDismiss = {
                                onDismissNotification(notification.key)
                            }
                        )
                    }
                }
            } ?: Spacer(modifier = Modifier.weight(1f))

            // Notification Tabs
            val scrollState = rememberScrollState()
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp.dp
            val horizontalPadding = 16.dp
            val availableWidth = screenWidth - (horizontalPadding * 2)

            val tabCount = sortedAppPackages.size
            val tabSpacing = 4.dp
            val deleteSpacing = 8.dp
            val totalItems = tabCount + 1
            val maxVisible = 5

            val isScrollable = totalItems > maxVisible

            // Calculate item width for the scrollable case to show exactly 'maxVisible' items
            val itemWidth = if (isScrollable) {
                (availableWidth - (tabSpacing * (maxVisible - 1)) - deleteSpacing) / maxVisible
            } else {
                0.dp
            }

            // Block parent pager from hijacking horizontal swipes
            // and ensure overscroll stays local
            val blockPagerScroll = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                        // Disallow parent pager from starting a scroll if we are interacting with tabs
                        if (source == NestedScrollSource.UserInput && abs(available.x) > abs(available.y)) {
                            view.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        return Offset.Zero
                    }

                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        return if (source == NestedScrollSource.UserInput) {
                            Offset(x = available.x, y = 0f)
                        } else {
                            Offset.Zero
                        }
                    }
                }
            }

            val deleteInteractionSource = remember { MutableInteractionSource() }
            val isDeletePressed by deleteInteractionSource.collectIsPressedAsState()
            val deleteCornerRadius by animateDpAsState(if (isDeletePressed) 4.dp else 12.dp, label = "delete_corner")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dockAreaHeight)
                    .padding(horizontal = horizontalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isScrollable) {
                    // Reliable non-scrollable Row using weights
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
                            onDismiss = { viewModel.dismissNotificationsByPackage(pkg) },
                            isOverDelete = { tabRect ->
                                val intersection = deleteButtonBounds.intersect(tabRect)
                                val overlapRatio = if (intersection.isEmpty) 0f else {
                                    (intersection.width * intersection.height) / (deleteButtonBounds.width * deleteButtonBounds.height)
                                }
                                // Deletes if 50% overlap OR if the tab is dragged past the start of the delete button
                                overlapRatio >= 0.5f || tabRect.center.x >= deleteButtonBounds.left
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(tabSpacing))
                    }

                    Spacer(Modifier.width(deleteSpacing - tabSpacing))

                    Surface(
                        onClick = onDismissAllNotifications,
                        interactionSource = deleteInteractionSource,
                        shape = RoundedCornerShape(deleteCornerRadius),
                        color = colorScheme.error,
                        modifier = Modifier
                            .height(40.dp)
                            .weight(1f)
                            .onGloballyPositioned { deleteButtonBounds = it.boundsInRoot() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Clear All",
                                tint = colorScheme.onError,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    // Scrollable Tabs
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .nestedScroll(blockPagerScroll)
                            .drawWithContent {
                                drawContent()
                                val fadeWidth = 16.dp.toPx()
                                if (scrollState.value > 0.5f) {
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color.Transparent, Color.Black),
                                            startX = 0f,
                                            endX = fadeWidth
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                                if (scrollState.value < scrollState.maxValue - 0.5f) {
                                    drawRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(Color.Black, Color.Transparent),
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
                            horizontalArrangement = Arrangement.spacedBy(tabSpacing),
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
                                    onDismiss = { viewModel.dismissNotificationsByPackage(pkg) },
                                    isOverDelete = { tabRect ->
                                        val intersection = deleteButtonBounds.intersect(tabRect)
                                        val overlapRatio = if (intersection.isEmpty) 0f else {
                                            (intersection.width * intersection.height) / (deleteButtonBounds.width * deleteButtonBounds.height)
                                        }
                                        // Deletes if 50% overlap OR if the tab is dragged past the start of the delete button
                                        overlapRatio >= 0.5f || tabRect.center.x >= deleteButtonBounds.left
                                    },
                                    modifier = Modifier.width(itemWidth)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(deleteSpacing))

                    Surface(
                        onClick = onDismissAllNotifications,
                        interactionSource = deleteInteractionSource,
                        shape = RoundedCornerShape(deleteCornerRadius),
                        color = colorScheme.error,
                        modifier = Modifier
                            .height(40.dp)
                            .width(itemWidth)
                            .onGloballyPositioned { deleteButtonBounds = it.boundsInRoot() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Clear All",
                                tint = colorScheme.onError,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getDominantColor(drawable: Drawable?): Color {
    if (drawable == null) return Color.Unspecified
    return try {
        // Use a small, fixed size for color extraction to be safe and efficient
        val bitmap = drawable.toBitmap(width = 40, height = 40)

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
            if (width <= 0 || height <= 0) return Color.Unspecified
            
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
            bestColor ?: pixels[(height/2 * width + width/2).coerceIn(pixels.indices)]
        }

        // Tone down the color to avoid "eye-burning" intensity
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(rawColor, hsv)

        // Cap saturation (max 70%) and brightness (max 80%)
        // This keeps the brand identity but makes it much more comfortable to look at
        hsv[1] = hsv[1].coerceAtMost(0.7f)
        hsv[2] = hsv[2].coerceAtMost(0.8f)

        Color(android.graphics.Color.HSVToColor(hsv)).copy(alpha = 1f)
    } catch (_: Exception) {
        Color.Unspecified
    }
}

fun getContrastColor(color: Color): Color {
    // Standard relative luminance formula
    val luminance = 0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue

    // Increased threshold (0.72) to favor white icons on brand colors (like WhatsApp green)
    // even after they have been muted/de-saturated.
    return if (luminance > 0.72) Color.Black else Color.White
}

