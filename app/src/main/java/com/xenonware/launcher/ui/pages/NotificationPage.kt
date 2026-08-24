package com.xenonware.launcher.ui.pages

import android.app.ActivityOptions
import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.notification.LauncherNotification
import com.xenonware.launcher.ui.res.notification.ChronoCluster
import com.xenonware.launcher.ui.res.notification.NotificationItem
import com.xenonware.launcher.ui.res.notification.NotificationTabButton
import com.xenonware.launcher.util.ColorUtils
import com.xenonware.launcher.util.blockHorizontalPagerSwipe
import com.xenonware.launcher.util.shouldDisableLandscapeLayout
import com.xenonware.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun NotificationPage(
    viewModel: LauncherViewModel,
    notificationCount: Int,
    currentDate: String,
    notifications: List<LauncherNotification>,
    apps: List<AppInfo>,
    calendarEvents: List<com.xenonware.launcher.viewmodel.CalendarEvent>,
    hazeState: dev.chrisbanes.haze.HazeState?,
    blurSetting: Boolean,
    wallpaperDarkIcons: Boolean = false,
    onDismissNotification: (String) -> Unit,
    onDismissAllNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onContentShiftChanged: (Float) -> Unit = {}
) {
    val baseColor = if (wallpaperDarkIcons) Color.Black else Color.White
    val nextAlarm by viewModel.nextAlarm.collectAsState()
    val timers by viewModel.activeTimers.collectAsState(initial = emptyList())
    val stopwatches by viewModel.activeStopwatches.collectAsState(initial = emptyList())

    var selectedPackage by remember { mutableStateOf<String?>(null) }
    var showAtAGlanceMenu by remember { mutableStateOf(false) }
    var showPageMenu by remember { mutableStateOf(false) }
    var dropDownOffset by remember { mutableStateOf(Offset.Zero) }

    var atAGlanceSectionPos by remember { mutableStateOf(Offset.Zero) }
    var pageContainerPos by remember { mutableStateOf(Offset.Zero) }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val offsets = remember { mutableStateMapOf<String, Float>() }
    var deleteButtonBounds by remember { mutableStateOf(Rect.Zero) }

    // Owned by the ViewModel so LauncherScreen can close the reply when the app
    // drawer opens, and so the dock can freeze its IME padding while one is open.
    val replyingNotificationKey by viewModel.replyingNotificationKey.collectAsState()

    BackHandler(enabled = selectedPackage != null || showAtAGlanceMenu || showPageMenu) {
        if (showAtAGlanceMenu) showAtAGlanceMenu = false
        else if (showPageMenu) showPageMenu = false
        else selectedPackage = null
    }

    val groupedNotifications = remember(notifications) {
        notifications.groupBy { it.packageName }
    }

    // Reset selection if the selected app has no notifications left
    LaunchedEffect(notifications) {
        if (selectedPackage != null && !groupedNotifications.containsKey(selectedPackage)) {
            selectedPackage = null
        }
    }

    val sortedAppPackages = remember(groupedNotifications) {
        groupedNotifications.keys.sortedByDescending { pkg ->
            groupedNotifications[pkg]?.maxOfOrNull { it.postTime } ?: 0L
        }
    }

    // Keep a "last known" set of data for the tabs to prevent them from vanishing
    // instantly during the exit animation when notifications are cleared.
    var lastTabsData by remember { mutableStateOf<Pair<List<String>, Map<String, List<LauncherNotification>>>?>(null) }
    if (sortedAppPackages.isNotEmpty()) {
        lastTabsData = sortedAppPackages to groupedNotifications
    }
    val effectiveTabs = if (notificationCount > 0) sortedAppPackages else lastTabsData?.first ?: emptyList()
    val effectiveGroups = if (notificationCount > 0) groupedNotifications else lastTabsData?.second ?: emptyMap()

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val disableLandscape = shouldDisableLandscapeLayout(context)
    val useLandscapeLayout = isLandscape && !disableLandscape
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topPadding = if (statusBarHeight < 16.dp) {16.dp - statusBarHeight} else {0.dp}
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 72dp (dock) + 8dp (dock padding) + 8dp (gap) + 4dp (to match widget vertical padding)
    val dockAreaHeight = 72.dp + navBarHeight + 8.dp + 8.dp + 4.dp

    // --- Keyboard-aware lift for the notification being replied to ---

    val density = LocalDensity.current
    val windowHeightPx = LocalWindowInfo.current.containerSize.height.toFloat()
    val imeBottomPx = WindowInsets.ime.getBottom(density).toFloat()

    val hasHardwareKeyboard = configuration.keyboard != Configuration.KEYBOARD_NOKEYS &&
            configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO

    val gapPx = with(density) { 16.dp.toPx() }
    val minTopPx = with(density) { (statusBarHeight + topPadding).toPx() }
    val dockAreaPx = with(density) { dockAreaHeight.toPx() }

    // Bounds of the notification being replied to, with our own shift removed so the
    // value is a stable fixed point instead of feeding back into itself.
    var replyTopPx by remember { mutableFloatStateOf(0f) }
    var replyBottomPx by remember { mutableFloatStateOf(0f) }

    val isReplying = replyingNotificationKey != null

    // With a hardware keyboard there is no IME, so the dock is the obstruction.
    val obstructionPx = if (isReplying && hasHardwareKeyboard) {
        maxOf(imeBottomPx, dockAreaPx)
    } else {
        imeBottomPx
    }

    // Gated on an open reply: without this, any other IME (the app drawer's search,
    // for example) would re-trigger the lift using the last measured bounds.
    val targetShiftPx = if (!isReplying || replyBottomPx <= 0f || obstructionPx <= 0f) 0f else {
        val targetBottom = windowHeightPx - obstructionPx - gapPx
        val needed = (replyBottomPx - targetBottom).coerceAtLeast(0f)
        // Never push the top of the item off screen; if the item is taller than the
        // available space this pins its top instead and the list stays scrollable.
        val maxShift = (replyTopPx - minTopPx).coerceAtLeast(0f)
        needed.coerceAtMost(maxShift)
    }

    val contentShiftPx by animateFloatAsState(
        targetValue = targetShiftPx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "notificationKeyboardShift"
    )

    LaunchedEffect(contentShiftPx, hasHardwareKeyboard) {
        onContentShiftChanged(if (hasHardwareKeyboard) contentShiftPx else 0f)
    }

    // Bounds are per-reply, so clear them on every transition — including close, so
    // nothing is left behind for an unrelated keyboard to pick up.
    LaunchedEffect(replyingNotificationKey) {
        replyTopPx = 0f
        replyBottomPx = 0f
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.setReplyingNotification(null) }
    }

    val onReplyBounds: (Rect) -> Unit = { rect ->
        replyTopPx = rect.top + contentShiftPx
        replyBottomPx = rect.bottom + contentShiftPx
    }

    val focusManager = LocalFocusManager.current

    val landscapeListState = rememberLazyListState()
    val portraitListState = rememberLazyListState()

    // Hide keyboard when the main list is scrolled (outside the reply field)
    LaunchedEffect(landscapeListState.isScrollInProgress, portraitListState.isScrollInProgress) {
        if (landscapeListState.isScrollInProgress || portraitListState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    val hideKeyboardOnOverscroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // If the user pulls down (available.y > 0) and the list is at the top (consumed.y == 0)
                if (source == NestedScrollSource.UserInput && available.y > 10f && consumed.y == 0f) {
                    focusManager.clearFocus()
                }
                return Offset.Zero
            }
        }
    }

    val contentOffset = Modifier.offset { IntOffset(0, -contentShiftPx.roundToInt()) }
    val wholeScreenOffset = if (hasHardwareKeyboard) contentOffset else Modifier

    Box(modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .padding(top = topPadding)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .onGloballyPositioned { pageContainerPos = it.positionInRoot() }
        .pointerInput(Unit) {
            detectTapGestures(
                onLongPress = { offset ->
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    dropDownOffset = pageContainerPos + offset
                    showPageMenu = true
                }
            )
        }
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
                // Sits beside the list, not above it, so it only moves when the whole
                // screen moves (hardware keyboard).
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(wholeScreenOffset)
                        .padding(horizontal = 24.dp)
                        .onGloballyPositioned { atAGlanceSectionPos = it.positionInRoot() }
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onLongClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                // Approximation of center for menu anchor if no specific offset is provided by combinedClickable
                                dropDownOffset = atAGlanceSectionPos + Offset(100f, 100f)
                                showAtAGlanceMenu = true
                            },
                            onClick = {}
                        ),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center
                ) {
                    AtAGlanceSection(
                        currentDate = currentDate,
                        calendarEvents = calendarEvents,
                        isLandscape = true,
                        nextAlarm = nextAlarm,
                        timers = timers,
                        stopwatches = stopwatches,
                        isWallpaperDark = wallpaperDarkIcons,
                        modifier = Modifier.fillMaxHeight()
                    )
                }

                // Right Side: Notifications and Tabs
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                ) {
                    val stateKey = when {
                        notificationCount == 0 -> "empty"
                        selectedPackage == null -> "summary"
                        else -> "details|$selectedPackage"
                    }

                    AnimatedContent(
                        targetState = stateKey,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.98f, animationSpec = tween(150)))
                                .togetherWith(fadeOut(animationSpec = tween(80)))
                        },
                        label = "notification_content_landscape",
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) { targetState ->
                        when {
                            targetState == "empty" -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.EmojiEvents,
                                            contentDescription = null,
                                            tint = baseColor.copy(alpha = 0.8f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Text(
                                            text = "You're up to date",
                                            color = baseColor.copy(alpha = 0.8f),
                                            fontSize = 18.sp,
                                            fontFamily = QuicksandTitleVariable,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            targetState == "summary" -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.NotificationsActive,
                                            contentDescription = null,
                                            tint = baseColor.copy(alpha = 0.8f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Text(
                                            text = "You have $notificationCount notifications",
                                            color = baseColor.copy(alpha = 0.8f),
                                            fontSize = 18.sp,
                                            fontFamily = QuicksandTitleVariable,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            targetState.startsWith("details|") -> {
                                val pkg = targetState.substringAfter("|")
                                val app = apps.find { it.packageName == pkg }
                                val appColor = remember(app) { ColorUtils.getDominantColor(app?.icon) }

                                LazyColumn(
                                    state = landscapeListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(contentOffset)
                                        .nestedScroll(hideKeyboardOnOverscroll)
                                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                        .drawWithContent {
                                            drawContent()
                                            val fadeHeight = 16.dp.toPx()
                                            drawRect(
                                                brush = Brush.verticalGradient(
                                                    0f to Color.Transparent,
                                                    fadeHeight / size.height to Color.Black
                                                ),
                                                blendMode = BlendMode.DstIn
                                            )
                                            drawRect(
                                                brush = Brush.verticalGradient(
                                                    (size.height - fadeHeight) / size.height to Color.Black,
                                                    1f to Color.Transparent
                                                ),
                                                blendMode = BlendMode.DstIn
                                            )
                                        }
                                        .drawVerticalScrollbar(landscapeListState, colorScheme.primary),
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
                                            replyingNotificationKey = replyingNotificationKey,
                                            onReplyOpen = { viewModel.setReplyingNotification(it) },
                                            onReplyBoundsChanged = onReplyBounds,
                                            onOffsetChanged = { offsets[notification.key] = it },
                                            modifier = Modifier.animateItem(
                                                fadeInSpec = tween(durationMillis = 120),
                                                placementSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessHigh
                                                ),
                                                fadeOutSpec = tween(durationMillis = 120)
                                            ),
                                            onOpen = {
                                                try {
                                                    val options = ActivityOptions.makeBasic()
                                                    options.pendingIntentBackgroundActivityStartMode =
                                                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                                                    notification.contentIntent?.send(context, 0, null, null, null, null, options.toBundle())
                                                } catch (_: Exception) {
                                                    try { notification.contentIntent?.send() } catch (_: Exception) {}
                                                }
                                            },
                                            onDismiss = { onDismissNotification(notification.key) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = notificationCount > 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut(animationSpec = tween(150)) +
                               shrinkVertically(animationSpec = tween(durationMillis = 200, delayMillis = 150))
                    ) {
                        NotificationTabs(
                            sortedAppPackages = effectiveTabs,
                            groupedNotifications = effectiveGroups,
                            selectedPackage = selectedPackage,
                            apps = apps,
                            viewModel = viewModel,
                            onDismissAllNotifications = onDismissAllNotifications,
                            onPackageSelected = { selectedPackage = it },
                            deleteButtonBounds = deleteButtonBounds,
                            onDeleteButtonBoundsChanged = { deleteButtonBounds = it },
                            isLandscape = true,
                            modifier = wholeScreenOffset
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
                // At a Glance section — sits above the list, so it lifts with it
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.28f)
                        .then(wholeScreenOffset)
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp)
                        .onGloballyPositioned { atAGlanceSectionPos = it.positionInRoot() }
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onLongClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                dropDownOffset = atAGlanceSectionPos + Offset(100f, 100f)
                                showAtAGlanceMenu = true
                            },
                            onClick = {}
                        ),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Top
                ) {
                    AtAGlanceSection(
                        currentDate = currentDate,
                        calendarEvents = calendarEvents,
                        isLandscape = false,
                        nextAlarm = nextAlarm,
                        timers = timers,
                        stopwatches = stopwatches,
                        isWallpaperDark = wallpaperDarkIcons
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = dockAreaHeight)
                        .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                ) {
                    val stateKey = when {
                        notificationCount == 0 -> "empty"
                        selectedPackage == null -> "summary"
                        else -> "details|$selectedPackage"
                    }

                    AnimatedContent(
                        targetState = stateKey,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.98f, animationSpec = tween(150)))
                                .togetherWith(fadeOut(animationSpec = tween(80)))
                        },
                        label = "notification_content_portrait",
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) { targetState ->
                        when {
                            targetState == "empty" -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.EmojiEvents,
                                            contentDescription = null,
                                            tint = baseColor.copy(alpha = 0.8f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Text(
                                            text = "You're up to date",
                                            color = baseColor.copy(alpha = 0.8f),
                                            fontSize = 18.sp,
                                            fontFamily = QuicksandTitleVariable,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            targetState == "summary" -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.NotificationsActive,
                                            contentDescription = null,
                                            tint = baseColor.copy(alpha = 0.8f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Text(
                                            text = "You have $notificationCount notifications",
                                            color = baseColor.copy(alpha = 0.8f),
                                            fontSize = 18.sp,
                                            fontFamily = QuicksandTitleVariable,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            targetState.startsWith("details|") -> {
                                val pkg = targetState.substringAfter("|")
                                val app = apps.find { it.packageName == pkg }
                                val appColor = remember(app) { ColorUtils.getDominantColor(app?.icon) }

                                LazyColumn(
                                    state = portraitListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(contentOffset)
                                        .nestedScroll(hideKeyboardOnOverscroll)
                                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                        .drawWithContent {
                                            drawContent()
                                            val fadeHeight = 16.dp.toPx()
                                            drawRect(
                                                brush = Brush.verticalGradient(
                                                    0f to Color.Transparent,
                                                    fadeHeight / size.height to Color.Black
                                                ),
                                                blendMode = BlendMode.DstIn
                                            )
                                            drawRect(
                                                brush = Brush.verticalGradient(
                                                    (size.height - fadeHeight) / size.height to Color.Black,
                                                    1f to Color.Transparent
                                                ),
                                                blendMode = BlendMode.DstIn
                                            )
                                        }
                                        .drawVerticalScrollbar(portraitListState, colorScheme.primary),
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
                                            replyingNotificationKey = replyingNotificationKey,
                                            onReplyOpen = { viewModel.setReplyingNotification(it) },
                                            onReplyBoundsChanged = onReplyBounds,
                                            onOffsetChanged = { offsets[notification.key] = it },
                                            modifier = Modifier.animateItem(
                                                fadeInSpec = tween(durationMillis = 120),
                                                placementSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessHigh
                                                ),
                                                fadeOutSpec = tween(durationMillis = 120)
                                            ),
                                            onOpen = {
                                                try {
                                                    val options = ActivityOptions.makeBasic()
                                                    options.pendingIntentBackgroundActivityStartMode =
                                                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                                                    notification.contentIntent?.send(context, 0, null, null, null, null, options.toBundle())
                                                } catch (_: Exception) {
                                                    try { notification.contentIntent?.send() } catch (_: Exception) {}
                                                }
                                            },
                                            onDismiss = { onDismissNotification(notification.key) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = notificationCount > 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut(animationSpec = tween(150)) +
                               shrinkVertically(animationSpec = tween(durationMillis = 200, delayMillis = 150))
                    ) {
                        NotificationTabs(
                            sortedAppPackages = effectiveTabs,
                            groupedNotifications = effectiveGroups,
                            selectedPackage = selectedPackage,
                            apps = apps,
                            viewModel = viewModel,
                            onDismissAllNotifications = onDismissAllNotifications,
                            onPackageSelected = { selectedPackage = it },
                            deleteButtonBounds = deleteButtonBounds,
                            onDeleteButtonBoundsChanged = { deleteButtonBounds = it },
                            modifier = wholeScreenOffset
                        )
                    }
                }
            }
        }

        // Dropdown menus
        if (showAtAGlanceMenu) {
            com.xenonware.launcher.ui.res.XenonDropDown(
                expanded = showAtAGlanceMenu,
                onDismissRequest = { showAtAGlanceMenu = false },
                items = listOf(
                    com.xenonware.launcher.ui.res.MenuItem(
                        text = "Wallpaper",
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SET_WALLPAPER)
                            context.startActivity(android.content.Intent.createChooser(intent, "Select Wallpaper"))
                        },
                        leadingIcon = { Icon(Icons.Rounded.Wallpaper, null) }
                    ),
                    com.xenonware.launcher.ui.res.MenuItem(
                        text = "Settings",
                        onClick = { onOpenSettings() },
                        leadingIcon = { Icon(Icons.Rounded.Settings, null) }
                    ),
                    com.xenonware.launcher.ui.res.MenuItem(
                        text = "At a Glance Settings",
                        onClick = { viewModel.setShowCalendarSelectionDialog(true) },
                        leadingIcon = { Icon(Icons.Rounded.CalendarToday, null) }
                    )
                ),
                hazeState = if (blurSetting) hazeState else null,
                anchorPos = dropDownOffset,
                alignment = Alignment.Center
            )
        }

        if (showPageMenu) {
            com.xenonware.launcher.ui.res.XenonDropDown(
                expanded = showPageMenu,
                onDismissRequest = { showPageMenu = false },
                items = listOf(
                    com.xenonware.launcher.ui.res.MenuItem(
                        text = "Wallpaper",
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SET_WALLPAPER)
                            context.startActivity(android.content.Intent.createChooser(intent, "Select Wallpaper"))
                        },
                        leadingIcon = { Icon(Icons.Rounded.Wallpaper, null) }
                    ),
                    com.xenonware.launcher.ui.res.MenuItem(
                        text = "Settings",
                        onClick = { onOpenSettings() },
                        leadingIcon = { Icon(Icons.Rounded.Settings, null) }
                    ),
                    com.xenonware.launcher.ui.res.MenuItem(
                        text = "Notification Manager",
                        onClick = { viewModel.setShowNotificationManagerDialog(true) },
                        leadingIcon = { Icon(Icons.Rounded.NotificationsActive, null) }
                    )
                ),
                hazeState = if (blurSetting) hazeState else null,
                anchorPos = dropDownOffset,
                alignment = Alignment.Center
            )
        }
    }
}

fun Modifier.drawVerticalScrollbar(
    state: androidx.compose.foundation.lazy.LazyListState,
    color: Color
): Modifier = drawWithContent {
    drawContent()
    val layoutInfo = state.layoutInfo
    val viewportHeight = layoutInfo.viewportSize.height.toFloat()
    if (layoutInfo.totalItemsCount == 0 || viewportHeight <= 0) return@drawWithContent

    val items = layoutInfo.visibleItemsInfo
    if (items.isEmpty()) return@drawWithContent

    val totalItems = layoutInfo.totalItemsCount.toFloat()
    val visibleItems = items.size.toFloat()

    val scrollbarHeight = (visibleItems / totalItems) * viewportHeight
    val scrollbarOffset = (state.firstVisibleItemIndex.toFloat() / totalItems) * viewportHeight

    if (scrollbarHeight < viewportHeight) {
        drawRoundRect(
            color = color.copy(alpha = 0.5f),
            topLeft = Offset(size.width - 8.dp.toPx(), scrollbarOffset + 4.dp.toPx()),
            size = Size(4.dp.toPx(), (scrollbarHeight - 8.dp.toPx()).coerceAtLeast(16.dp.toPx())),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
    }
}

@Composable
fun AtAGlanceSection(
    currentDate: String,
    calendarEvents: List<com.xenonware.launcher.viewmodel.CalendarEvent>,
    isLandscape: Boolean,
    nextAlarm: android.app.AlarmManager.AlarmClockInfo?,
    timers: List<LauncherNotification>,
    stopwatches: List<LauncherNotification>,
    isWallpaperDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val baseColor = if (isWallpaperDark) Color.Black else Color.White
    val dateFontSize = if (isLandscape) 18.sp else 16.sp
    val eventTitleFontSize = if (isLandscape) 32.sp else 24.sp
    val subtitleFontSize = if (isLandscape) 16.sp else 14.sp
    val spacing = if (isLandscape) 8.dp else 4.dp
    val pageHeight = if (isLandscape) 80.dp else 60.dp

    val pagerState = rememberPagerState { calendarEvents.size.coerceAtLeast(1) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var totalDrag by remember { mutableFloatStateOf(0f) }
    var dragTriggered by remember { mutableStateOf(false) }
    val swipeThreshold = with(LocalDensity.current) { 24.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(calendarEvents.size) {
                if (calendarEvents.size <= 1) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        dragTriggered = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (dragTriggered) return@detectVerticalDragGestures
                        totalDrag += dragAmount
                        if (abs(totalDrag) > swipeThreshold) {
                            dragTriggered = true
                            scope.launch {
                                if (totalDrag < 0 && pagerState.currentPage < calendarEvents.size - 1) {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                } else if (totalDrag > 0 && pagerState.currentPage > 0) {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // The "Old Style" layout: Stationary Column with Date row and a compact Pager
        Column(
            modifier = Modifier.wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentDate,
                    fontSize = dateFontSize,
                    fontWeight = FontWeight.Medium,
                    color = baseColor.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )

                ChronoCluster(
                    timers = timers,
                    stopwatches = stopwatches,
                    nextAlarm = nextAlarm,
                    fontSize = dateFontSize,
                    isWallpaperDark = isWallpaperDark
                )
            }

            if (calendarEvents.isEmpty()) {
                Text(
                    text = "No upcoming events",
                    fontSize = eventTitleFontSize,
                    fontWeight = FontWeight.Bold,
                    color = baseColor,
                    modifier = Modifier
                        .height(pageHeight)
                        .wrapContentHeight(Alignment.CenterVertically)
                )
            } else {
                val context = LocalContext.current
                val is24Hour = DateFormat.is24HourFormat(context)
                val timeFormatter = remember(is24Hour) {
                    val locale = Locale.getDefault()
                    if (is24Hour) {
                        if (locale.language == "de") {
                            SimpleDateFormat("HH:mm'Uhr'", locale)
                        } else {
                            SimpleDateFormat("HH:mm", locale)
                        }
                    } else {
                        SimpleDateFormat("h:mm a", locale)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // This is the visible pager, using the same state but fixed height
                    VerticalPager(
                        state = pagerState,
                        userScrollEnabled = false,
                        modifier = Modifier
                            .weight(1f)
                            .height(pageHeight)
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                            .drawWithContent {
                                drawContent()
                                val fadeHeight = 8.dp.toPx()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        fadeHeight / size.height to Color.Black
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        (size.height - fadeHeight) / size.height to Color.Black,
                                        1f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            },
                        horizontalAlignment = Alignment.Start
                    ) { index ->
                        val event = calendarEvents[index]
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    try {
                                        val uri = android.content.ContentUris.withAppendedId(
                                            android.provider.CalendarContract.Events.CONTENT_URI,
                                            event.id
                                        )
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).setData(uri)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback to opening calendar at specific time
                                        val builder = android.provider.CalendarContract.CONTENT_URI.buildUpon()
                                            .appendPath("time")
                                        android.content.ContentUris.appendId(builder, event.startTime)
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).setData(builder.build())
                                        context.startActivity(intent)
                                    }
                                }
                        ) {
                            Text(
                                text = event.title,
                                fontSize = eventTitleFontSize,
                                fontWeight = FontWeight.Bold,
                                color = baseColor,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                    .drawWithContent {
                                        drawContent()
                                        val fadeWidth = 32.dp.toPx()
                                        if (size.width > fadeWidth) {
                                            drawRect(
                                                brush = Brush.horizontalGradient(
                                                    0f to Color.Black,
                                                    (size.width - fadeWidth) / size.width to Color.Black,
                                                    1f to Color.Transparent
                                                ),
                                                blendMode = BlendMode.DstIn
                                            )
                                        }
                                    }
                                    .basicMarquee()
                            )
                            val timeText = remember(event, timeFormatter) {
                                val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                                val eventStartCal = Calendar.getInstance().apply { timeInMillis = event.startTime }
                                val isTomorrow = eventStartCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                                        eventStartCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)
                                val prefix = if (isTomorrow) {
                                    if (Locale.getDefault().language == "de") "Morgen " else "Tomorrow "
                                } else ""
                                if (event.isAllDay) {
                                    prefix + if (Locale.getDefault().language == "de") "Ganztägig" else "All Day"
                                } else {
                                    prefix + "${timeFormatter.format(event.startTime)} - ${timeFormatter.format(event.endTime)}"
                                }
                            }
                            Text(
                                text = timeText,
                                fontSize = subtitleFontSize,
                                color = baseColor.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (calendarEvents.size > 1) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowUp,
                                contentDescription = "Scroll Up",
                                tint = if (pagerState.currentPage > 0) baseColor.copy(alpha = 0.5f) else baseColor.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        enabled = pagerState.currentPage > 0
                                    ) {
                                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                    }
                            )
                            Text(
                                text = "${pagerState.currentPage + 1}/${calendarEvents.size}",
                                color = baseColor.copy(alpha = 0.5f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Scroll Down",
                                tint = if (pagerState.currentPage < calendarEvents.size - 1) baseColor.copy(alpha = 0.5f) else baseColor.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        enabled = pagerState.currentPage < calendarEvents.size - 1
                                    ) {
                                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                    }
                            )
                        }
                    }
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
    isLandscape: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val screenWidth = if (isLandscape) configuration.screenWidthDp.dp / 2 else configuration.screenWidthDp.dp
    val horizontalPadding = 16.dp
    val availableWidth = screenWidth - (horizontalPadding * 2)

    val tabCount = sortedAppPackages.size
    val tabSpacing = 4.dp
    val deleteSpacing = 8.dp
    val totalItems = tabCount + 1
    val maxVisible = 5

    val isScrollable = totalItems > maxVisible
    val view = LocalView.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

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
            .blockHorizontalPagerSwipe()
            .padding(horizontal = horizontalPadding)
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(tabSpacing)
    ) {
        if (!isScrollable) {
            sortedAppPackages.forEach { pkg ->
                key(pkg) {
                    val app = apps.find { it.packageName == pkg }
                    val notificationsForApp = groupedNotifications[pkg] ?: emptyList()
                    val latestNotification = notificationsForApp.firstOrNull()
                    val isSelected = selectedPackage == pkg
                    val appColor = remember(app) { ColorUtils.getDominantColor(app?.icon) }
                    val contrastColor = remember(appColor) { ColorUtils.getContrastColor(appColor) }

                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally(),
                        modifier = Modifier.weight(1f)
                    ) {
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
                            iconKey = latestNotification?.iconKey,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(deleteCornerRadius),
                color = colorScheme.error,
                modifier = Modifier
                    .height(40.dp)
                    .weight(1f)
                    .onGloballyPositioned { onDeleteButtonBoundsChanged(it.boundsInRoot()) }
                    .combinedClickable(
                        interactionSource = deleteInteractionSource,
                        indication = androidx.compose.foundation.LocalIndication.current,
                        onLongClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onDismissAllNotifications()
                        },
                        onClick = {}
                    )
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
                        key(pkg) {
                            val app = apps.find { it.packageName == pkg }
                            val notificationsForApp = groupedNotifications[pkg] ?: emptyList()
                            val latestNotification = notificationsForApp.firstOrNull()
                            val isSelected = selectedPackage == pkg
                            val appColor = remember(app) { ColorUtils.getDominantColor(app?.icon) }
                            val contrastColor = remember(appColor) { ColorUtils.getContrastColor(appColor) }

                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + expandHorizontally(),
                                exit = fadeOut() + shrinkHorizontally()
                            ) {
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
                                    iconKey = latestNotification?.iconKey,
                                    modifier = Modifier.width(itemWidth)
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(deleteCornerRadius),
                color = colorScheme.error,
                modifier = Modifier
                    .height(40.dp)
                    .width(itemWidth)
                    .onGloballyPositioned { onDeleteButtonBoundsChanged(it.boundsInRoot()) }
                    .combinedClickable(
                        interactionSource = deleteInteractionSource,
                        indication = androidx.compose.foundation.LocalIndication.current,
                        onLongClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onDismissAllNotifications()
                        },
                        onClick = {}
                    )
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