package com.xenonware.launcher.ui.pages

import android.app.ActivityOptions
import android.os.Build
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.notification.LauncherNotification
import com.xenonware.launcher.ui.res.notification.NotificationItem
import com.xenonware.launcher.ui.res.notification.NotificationTabButton
import com.xenonware.launcher.util.ColorUtils
import com.xenonware.launcher.util.shouldDisableLandscapeLayout
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

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val disableLandscape = shouldDisableLandscapeLayout(context)
    val useLandscapeLayout = isLandscape && !disableLandscape
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topPadding = if (statusBarHeight < 16.dp) {16.dp - statusBarHeight} else {0.dp}
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 72dp (dock) + 8dp (dock padding) + 8dp (gap) + 4dp (to match widget vertical padding)
    val dockAreaHeight = 72.dp + navBarHeight + 8.dp + 8.dp + 4.dp

    Box(modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .padding(top = topPadding)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        if (useLandscapeLayout) {
            // Landscape side-by-side layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = dockAreaHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: At a Glance
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentDate,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    if (calendarEvents.isEmpty()) {
                        Text(
                            text = "No upcoming events",
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    } else {
                        calendarEvents.take(3).forEach { event ->
                            Text(
                                text = event.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }

                // Right Side: Notifications and Tabs
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (notificationCount == 0) {
                        Box(modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                        if (selectedPackage == null) {
                            Box(modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Text(
                                        text = "You have $notificationCount notifications",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 18.sp,
                                        fontFamily = QuicksandTitleVariable,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            val pkg = selectedPackage!!
                            val app = apps.find { it.packageName == pkg }
                            val appColor = remember(app) { ColorUtils.getDominantColor(app?.icon) }

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
                                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                            ) {
                                val notificationsInGroup = groupedNotifications[pkg]?.reversed() ?: emptyList()
                                itemsIndexed(notificationsInGroup, key = { _, it -> it.key }) { index, notification ->
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
                                                val options = ActivityOptions.makeBasic()
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                    options.setPendingIntentBackgroundActivityStartMode(
                                                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                                                    )
                                                }
                                                notification.contentIntent?.send(context, 0, null, null, null, null, options.toBundle())
                                            } catch (e: Exception) {
                                                try { notification.contentIntent?.send() } catch (_: Exception) {}
                                            }
                                        },
                                        onDismiss = { onDismissNotification(notification.key) }
                                    )
                                }
                            }
                        }

                        NotificationTabs(
                            sortedAppPackages = sortedAppPackages,
                            groupedNotifications = groupedNotifications,
                            selectedPackage = selectedPackage,
                            apps = apps,
                            viewModel = viewModel,
                            onDismissAllNotifications = onDismissAllNotifications,
                            onPackageSelected = { selectedPackage = it },
                            deleteButtonBounds = deleteButtonBounds,
                            onDeleteButtonBoundsChanged = { deleteButtonBounds = it }
                        )
                    }
                }
            }
        } else {
            // Portrait Layout
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // At a Glance section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.35f)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
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
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                    Spacer(Modifier.height(dockAreaHeight)) 
                } else {
                    if (selectedPackage == null) {
                        Box(modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = "You have $notificationCount notifications",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 18.sp,
                                    fontFamily = QuicksandTitleVariable,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        val pkg = selectedPackage!!
                        val app = apps.find { it.packageName == pkg }
                        val appColor = remember(app) { ColorUtils.getDominantColor(app?.icon) }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                        ) {
                            val notificationsInGroup = groupedNotifications[pkg]?.reversed() ?: emptyList()
                            itemsIndexed(notificationsInGroup, key = { _, it -> it.key }) { index, notification ->
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
                                            val options = ActivityOptions.makeBasic()
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                                options.setPendingIntentBackgroundActivityStartMode(
                                                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                                                )
                                            }
                                            notification.contentIntent?.send(context, 0, null, null, null, null, options.toBundle())
                                        } catch (e: Exception) {
                                            try { notification.contentIntent?.send() } catch (_: Exception) {}
                                        }
                                    },
                                    onDismiss = { onDismissNotification(notification.key) }
                                )
                            }
                        }
                    }

                    NotificationTabs(
                        sortedAppPackages = sortedAppPackages,
                        groupedNotifications = groupedNotifications,
                        selectedPackage = selectedPackage,
                        apps = apps,
                        viewModel = viewModel,
                        onDismissAllNotifications = onDismissAllNotifications,
                        onPackageSelected = { selectedPackage = it },
                        deleteButtonBounds = deleteButtonBounds,
                        onDeleteButtonBoundsChanged = { deleteButtonBounds = it },
                        modifier = Modifier.padding(bottom = dockAreaHeight)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationTabs(
    sortedAppPackages: List<String>,
    groupedNotifications: Map<String, List<LauncherNotification>>,
    selectedPackage: String?,
    apps: List<AppInfo>,
    viewModel: LauncherViewModel,
    onDismissAllNotifications: () -> Unit,
    onPackageSelected: (String?) -> Unit,
    deleteButtonBounds: Rect,
    onDeleteButtonBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
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
    val view = LocalView.current

    val itemWidth = if (isScrollable) {
        (availableWidth - (tabSpacing * (maxVisible - 1)) - deleteSpacing) / maxVisible
    } else {
        0.dp
    }

    val blockPagerScroll = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && abs(available.x) > abs(available.y)) {
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                return if (source == NestedScrollSource.UserInput) Offset(x = available.x, y = 0f) else Offset.Zero
            }
        }
    }

    val deleteInteractionSource = remember { MutableInteractionSource() }
    val isDeletePressed by deleteInteractionSource.collectIsPressedAsState()
    val deleteCornerRadius by animateDpAsState(if (isDeletePressed) 4.dp else 12.dp, label = "delete_corner")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isScrollable) {
            sortedAppPackages.forEach { pkg ->
                androidx.compose.runtime.key(pkg) {
                    val app = apps.find { it.packageName == pkg }
                    val notificationsForApp = groupedNotifications[pkg] ?: emptyList()
                    val latestNotification = notificationsForApp.firstOrNull()
                    val isSelected = selectedPackage == pkg
                    val appColor = remember(app) { ColorUtils.getDominantColor(app?.icon) }
                    val contrastColor = remember(appColor) { ColorUtils.getContrastColor(appColor) }

                    NotificationTabButton(
                        app = app,
                        notificationIcon = latestNotification?.icon,
                        notificationCount = notificationsForApp.size,
                        isSelected = isSelected,
                        appColor = appColor,
                        contrastColor = contrastColor,
                        onClick = { onPackageSelected(if (isSelected) null else pkg) },
                        onDismiss = { viewModel.dismissNotificationsByPackage(pkg) },
                        isOverDelete = { tabRect ->
                            val intersection = deleteButtonBounds.intersect(tabRect)
                            val overlapRatio = if (intersection.isEmpty) 0f else {
                                (intersection.width * intersection.height) / (deleteButtonBounds.width * deleteButtonBounds.height)
                            }
                            overlapRatio >= 0.5f || tabRect.center.x >= deleteButtonBounds.left
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
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
                    .onGloballyPositioned { onDeleteButtonBoundsChanged(it.boundsInRoot()) }
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
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black
                                    ), startX = 0f, endX = fadeWidth
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                        if (scrollState.value < scrollState.maxValue - 0.5f) {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Black,
                                        Color.Transparent
                                    ), startX = size.width - fadeWidth, endX = size.width
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
                        androidx.compose.runtime.key(pkg) {
                            val app = apps.find { it.packageName == pkg }
                            val notificationsForApp = groupedNotifications[pkg] ?: emptyList()
                            val latestNotification = notificationsForApp.firstOrNull()
                            val isSelected = selectedPackage == pkg
                            val appColor = remember(app) { ColorUtils.getDominantColor(app?.icon) }
                            val contrastColor = remember(appColor) { ColorUtils.getContrastColor(appColor) }

                            NotificationTabButton(
                                app = app,
                                notificationIcon = latestNotification?.icon,
                                notificationCount = notificationsForApp.size,
                                isSelected = isSelected,
                                appColor = appColor,
                                contrastColor = contrastColor,
                                onClick = { onPackageSelected(if (isSelected) null else pkg) },
                                onDismiss = { viewModel.dismissNotificationsByPackage(pkg) },
                                isOverDelete = { tabRect ->
                                    val intersection = deleteButtonBounds.intersect(tabRect)
                                    val overlapRatio = if (intersection.isEmpty) 0f else {
                                        (intersection.width * intersection.height) / (deleteButtonBounds.width * deleteButtonBounds.height)
                                    }
                                    overlapRatio >= 0.5f || tabRect.center.x >= deleteButtonBounds.left
                                },
                                modifier = Modifier.width(itemWidth)
                            )
                        }
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
                    .onGloballyPositioned { onDeleteButtonBoundsChanged(it.boundsInRoot()) }
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
