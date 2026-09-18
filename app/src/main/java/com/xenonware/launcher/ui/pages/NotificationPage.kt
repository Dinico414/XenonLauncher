package com.xenonware.launcher.ui.pages

import android.app.ActivityOptions
import android.app.AlarmManager
import android.content.ContentUris
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.util.LruCache
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.xenon.mylibrary.res.MenuItem
import com.xenon.mylibrary.res.XenonDropDown
import com.xenon.mylibrary.values.BigSpacing
import com.xenon.mylibrary.values.BiggerSpacing
import com.xenon.mylibrary.values.BiggestSpacing
import com.xenon.mylibrary.values.ExtraBigSpacing
import com.xenon.mylibrary.values.ExtraBiggerSpacing
import com.xenon.mylibrary.values.ExtraLargeSpacing
import com.xenon.mylibrary.values.ExtraLargerPadding
import com.xenon.mylibrary.values.ExtraLargerSpacing
import com.xenon.mylibrary.values.ExtraLargestSpacing
import com.xenon.mylibrary.values.HugeBiggerSpacing
import com.xenon.mylibrary.values.HugerSpacing
import com.xenon.mylibrary.values.HugestSpacing
import com.xenon.mylibrary.values.LargeMediumCornerRadius
import com.xenon.mylibrary.values.LargeMediumSpacer
import com.xenon.mylibrary.values.LargeMediumSpacing
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.LargestSpacing
import com.xenon.mylibrary.values.MassiveCornerRadius
import com.xenon.mylibrary.values.MediumSpacing
import com.xenon.mylibrary.values.NoPadding
import com.xenon.mylibrary.values.NoSpacing
import com.xenon.mylibrary.values.SmallCornerRadius
import com.xenon.mylibrary.values.SmallPadding
import com.xenon.mylibrary.values.SmallSpacing
import com.xenon.mylibrary.values.SmallerCornerRadius
import com.xenon.mylibrary.values.SmallerElevation
import com.xenon.mylibrary.values.SmallerPadding
import com.xenon.mylibrary.values.SmallerSpacer
import com.xenon.mylibrary.values.SmallestSpacing
import com.xenonware.launcher.R
import com.xenonware.launcher.accessibility.XenonAccessibilityService
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.notification.LauncherNotification
import com.xenonware.launcher.ui.res.dock.StatusCounters
import com.xenonware.launcher.ui.res.notification.ChronoCluster
import com.xenonware.launcher.ui.res.notification.NotificationItem
import com.xenonware.launcher.ui.res.notification.NotificationTabButton
import com.xenonware.launcher.ui.theme.mainFontFamily
import com.xenonware.launcher.ui.theme.subFontFamily
import com.xenonware.launcher.util.ColorUtils
import com.xenonware.launcher.util.PerfLog
import com.xenonware.launcher.util.blockHorizontalPagerSwipe
import com.xenonware.launcher.util.shouldDisableLandscapeLayout
import com.xenonware.launcher.viewmodel.CalendarEvent
import com.xenonware.launcher.viewmodel.CalendarInfo
import com.xenonware.launcher.viewmodel.LauncherViewModel
import com.xenonware.launcher.viewmodel.WeatherState
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Process-wide cache of dominant icon colors.
 *
 * `remember(app) { ColorUtils.getDominantColor(...) }` only survives while the composable is
 * alive. Every time this page left and re-entered composition (e.g. coming back from the media
 * page), every tab and every notification recomputed its color on the main thread.
 */
private object AppColorCache {
    private val cache = LruCache<String, Color>(256)

    fun get(app: AppInfo?): Color {
        val noIcon: Drawable? = null
        if (app == null) return ColorUtils.getDominantColor(noIcon)
        val icon: Drawable = app.icon ?: return ColorUtils.getDominantColor(noIcon)
        val key = "${app.packageName}|${System.identityHashCode(icon)}"
        cache.get(key)?.let { return it }
        return PerfLog.measure("dominantColor ${app.packageName}", thresholdMs = 8) {
            ColorUtils.getDominantColor(icon)
        }.also { cache.put(key, it) }
    }
}

@Composable
private fun rememberAppColor(app: AppInfo?): Color =
    remember(app?.packageName, app?.icon) { AppColorCache.get(app) }

/**
 * The calendar used to be reloaded every time the page entered composition. Re-entering within
 * this window skips the immediate reload; the periodic refresh still runs while the page is shown.
 */
private object CalendarRefreshThrottle {
    const val MIN_REENTRY_INTERVAL_MS = 30_000L

    @Volatile
    var lastLoadElapsed = 0L
}

/**
 * Last non-empty tab data, kept so the tabs don't vanish mid exit-animation when everything is
 * cleared. A plain holder, not snapshot state: it is only read in the same composition that makes
 * the notification count drop to zero, so observing it would just add recompositions.
 */
private class LastTabs {
    var packages: List<String> = emptyList()
    var groups: Map<String, List<LauncherNotification>> = emptyMap()
}

@Composable
fun NotificationPage(
    viewModel: LauncherViewModel,
    notificationCount: Int,
    currentTime: String,
    currentDate: String,
    showClock: Boolean,
    hideAtAGlance: Boolean,
    indicatorType: Int,
    messageType: Int,
    notifications: List<LauncherNotification>,
    apps: List<AppInfo>,
    calendarEvents: List<CalendarEvent>,
    hazeState: HazeState?,
    blurSetting: Boolean,
    wallpaperDarkIcons: Boolean = false,
    onDismissNotification: (String) -> Unit,
    onDismissAllNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onContentShiftChanged: (Float) -> Unit = {}
) {
    val weatherState by viewModel.weatherState.collectAsState()
    val baseColor = if (wallpaperDarkIcons) Color.Black else Color.White
    val nextAlarm by viewModel.nextAlarm.collectAsState()
    val availableCalendars by viewModel.availableCalendars.collectAsState()
    val timers by viewModel.activeTimers.collectAsState(initial = emptyList())
    val stopwatches by viewModel.activeStopwatches.collectAsState(initial = emptyList())
    val showMuteNotifications by viewModel.showMuteNotifications.collectAsState()
    val showPermanentNotifications by viewModel.showPermanentNotifications.collectAsState()
    val disableGrouping by viewModel.disableGrouping.collectAsState()

    var selectedPackage by remember { mutableStateOf<String?>(null) }
    var showAtAGlanceMenu by remember { mutableStateOf(false) }
    var showPageMenu by remember { mutableStateOf(false) }
    var dropDownOffset by remember { mutableStateOf(Offset.Zero) }

    // Long-pressing the At a Glance counter or its arrows opens the full event list. On a
    // compact portrait screen the notification area makes way for it; in landscape or on a
    // wide screen there is room, so only the At a Glance column itself changes.
    var isGlanceExpanded by remember { mutableStateOf(false) }

    var atAGlanceSectionPos by remember { mutableStateOf(Offset.Zero) }
    var pageContainerPos by remember { mutableStateOf(Offset.Zero) }

    val haptic = LocalHapticFeedback.current
    val offsets = remember { mutableStateMapOf<String, Float>() }
    // Kept as a State object, not a value: the delete button reports its root bounds from
    // onGloballyPositioned, which fires on every frame the pager moves. Passing the value down
    // as a parameter recomposed this page and the whole tab strip once per swipe frame; the
    // only consumer (the drag-over-delete check) reads it at gesture time instead.
    val deleteButtonBounds = remember { mutableStateOf(Rect.Zero) }

    // Owned by the ViewModel so LauncherScreen can close the reply when the app
    // drawer opens, and so the dock can freeze its IME padding while one is open.
    val replyingNotificationKey by viewModel.replyingNotificationKey.collectAsState()

    BackHandler(enabled = selectedPackage != null || showAtAGlanceMenu || showPageMenu || isGlanceExpanded) {
        if (showAtAGlanceMenu) showAtAGlanceMenu = false
        else if (showPageMenu) showPageMenu = false
        else if (isGlanceExpanded) isGlanceExpanded = false
        else selectedPackage = null
    }

    val groupedNotifications = remember(notifications, showMuteNotifications, showPermanentNotifications, disableGrouping) {
        val filtered = notifications.filter {
            val isMuted = it.isMuted && showMuteNotifications
            val isPermanent = it.isOngoing && showPermanentNotifications
            !isMuted && !isPermanent
        }
        if (disableGrouping) {
            if (filtered.isNotEmpty()) mapOf("__ALL__" to filtered) else emptyMap()
        } else {
            filtered.groupBy { it.packageName }
        }
    }

    val mutedNotifications = remember(notifications, showMuteNotifications) {
        if (showMuteNotifications) {
            notifications.filter { it.isMuted && !it.isOngoing }
        } else {
            emptyList()
        }
    }

    val permanentNotifications = remember(notifications, showPermanentNotifications) {
        if (showPermanentNotifications) {
            notifications.filter { it.isOngoing }
        } else {
            emptyList()
        }
    }

    // One map lookup per item instead of a linear search through every installed app.
    val appsByPackage = remember(apps) { apps.associateBy { it.packageName } }

    LaunchedEffect(Unit) {
        while (true) {
            val last = CalendarRefreshThrottle.lastLoadElapsed
            val sinceLast = SystemClock.elapsedRealtime() - last
            if (last == 0L || sinceLast >= CalendarRefreshThrottle.MIN_REENTRY_INTERVAL_MS) {
                PerfLog.measure("loadCalendarEvents", thresholdMs = 8) { viewModel.loadCalendarEvents() }
                CalendarRefreshThrottle.lastLoadElapsed = SystemClock.elapsedRealtime()
                delay(5.minutes)
            } else {
                delay((CalendarRefreshThrottle.MIN_REENTRY_INTERVAL_MS - sinceLast).milliseconds)
            }
        }
    }

    // Reset selection if the selected app has no notifications left
    LaunchedEffect(notifications, showMuteNotifications, showPermanentNotifications, disableGrouping) {
        if (selectedPackage != null && selectedPackage != "__MUTED__" && selectedPackage != "__PERMANENT__" && selectedPackage != "__ALL__" && !groupedNotifications.containsKey(selectedPackage)) {
            selectedPackage = null
        }
        if (selectedPackage == "__ALL__" && !groupedNotifications.containsKey("__ALL__")) {
            selectedPackage = null
        }
        if (selectedPackage == "__MUTED__" && mutedNotifications.isEmpty()) {
            selectedPackage = null
        }
        if (selectedPackage == "__PERMANENT__" && permanentNotifications.isEmpty()) {
            selectedPackage = null
        }
    }

    val sortedAppPackages = remember(groupedNotifications, mutedNotifications, permanentNotifications, disableGrouping) {
        val apps = if (disableGrouping) {
            if (groupedNotifications.containsKey("__ALL__")) mutableListOf("__ALL__") else mutableListOf()
        } else {
            val list = groupedNotifications.keys.toMutableList()
            list.sortWith(compareByDescending { pkg ->
                groupedNotifications[pkg]?.maxOfOrNull { it.postTime } ?: 0L
            })
            list
        }

        if (mutedNotifications.isNotEmpty()) {
            apps.add("__MUTED__")
        }
        if (permanentNotifications.isNotEmpty()) {
            apps.add("__PERMANENT__")
        }
        apps
    }

    // A fresh holder whenever grouping is toggled, seeded in the same composition below.
    val lastTabs = remember(disableGrouping) { LastTabs() }
    if (sortedAppPackages.isNotEmpty()) {
        lastTabs.packages = sortedAppPackages
        lastTabs.groups = groupedNotifications
    }
    LaunchedEffect(disableGrouping) {
        selectedPackage = null
    }
    val effectiveTabs = if (notificationCount > 0) sortedAppPackages else lastTabs.packages
    val effectiveGroups = if (notificationCount > 0) groupedNotifications else lastTabs.groups

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val disableLandscape = shouldDisableLandscapeLayout(context)
    val useLandscapeLayout = isLandscape && !disableLandscape
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val windowWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val isWideScreen = windowWidthDp >= 600.dp
    val glanceHidesNotifications = !useLandscapeLayout && !isWideScreen
    // 0 = default layout, 1 = the event list owns the whole column
    val glanceExpandProgress by animateFloatAsState(
        targetValue = if (isGlanceExpanded && glanceHidesNotifications) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "glanceExpand"
    )
    LaunchedEffect(hideAtAGlance) {
        if (hideAtAGlance) isGlanceExpanded = false
    }
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topPadding = if (statusBarHeight < LargestSpacing) {LargestPadding - statusBarHeight} else { NoPadding }
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 72dp (dock) + 8dp (dock padding) + 8dp (gap) + 4dp (to match widget vertical padding)
    val dockAreaHeight = HugeBiggerSpacing + navBarHeight + MediumSpacing + MediumSpacing + SmallSpacing

    // --- Keyboard-aware lift for the notification being replied to ---

    val windowHeightPx = windowInfo.containerSize.height.toFloat()
    val isReplying = replyingNotificationKey != null

    val imeInsets = WindowInsets.ime
    val imeBottomPx = if (isReplying) imeInsets.getBottom(density).toFloat() else 0f

    val hasHardwareKeyboard = configuration.keyboard != Configuration.KEYBOARD_NOKEYS &&
            configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO

    val gapPx = with(density) { LargestSpacing.toPx() }
    val minTopPx = with(density) { (statusBarHeight + topPadding).toPx() }
    val dockAreaPx = with(density) { dockAreaHeight.toPx() }

    // Bounds of the notification being replied to, with our own shift removed so the
    // value is a stable fixed point instead of feeding back into itself.
    var replyTopPx by remember { mutableFloatStateOf(0f) }
    var replyBottomPx by remember { mutableFloatStateOf(0f) }

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
        // Never push the top of the item off-screen; if the item is taller than the
        // available space this pins its top instead and the list stays scrollable.
        val maxShift = (replyTopPx - minTopPx).coerceAtLeast(0f)
        needed.coerceAtMost(maxShift)
    }

    // Kept as a State and only read in effects, callbacks and the offset lambda, so the
    // animation itself never recomposes the page.
    val contentShift = animateFloatAsState(
        targetValue = targetShiftPx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "notificationKeyboardShift"
    )

    val currentOnContentShiftChanged by rememberUpdatedState(onContentShiftChanged)
    LaunchedEffect(hasHardwareKeyboard) {
        snapshotFlow { contentShift.value }.collect { shift ->
            currentOnContentShiftChanged(if (hasHardwareKeyboard) shift else 0f)
        }
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

    val onReplyBounds: (Rect) -> Unit = remember(contentShift) {
        { rect ->
            replyTopPx = rect.top + contentShift.value
            replyBottomPx = rect.bottom + contentShift.value
        }
    }
    val onReplyOpen: (String?) -> Unit = remember(viewModel) {
        { key -> viewModel.setReplyingNotification(key) }
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

    val hideKeyboardOnOverscroll = remember(focusManager) {
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

    val contentOffset = remember(contentShift) {
        Modifier.offset { IntOffset(0, -contentShift.value.roundToInt()) }
    }
    val wholeScreenOffset = if (hasHardwareKeyboard) contentOffset else Modifier

    val stateKey = when {
        notificationCount == 0 -> "empty"
        selectedPackage == null -> "summary"
        selectedPackage == "__MUTED__" -> "muted"
        selectedPackage == "__PERMANENT__" -> "permanent"
        selectedPackage == "__ALL__" -> "all"
        else -> "details|$selectedPackage"
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .padding(top = topPadding)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .onGloballyPositioned { pageContainerPos = it.positionInRoot() }
        .pointerInput(notificationCount, selectedPackage) {
            detectTapGestures(
                onLongPress = { offset ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    dropDownOffset = pageContainerPos + offset
                    showPageMenu = true
                },
                onDoubleTap = {
                    if (notificationCount == 0 || selectedPackage == null) {
                        XenonAccessibilityService.lockScreenOrRequestAccess(context)
                    }
                }
            )
        }
        .pointerInput(notificationCount, selectedPackage) {
            var totalVerticalDrag = 0f
            detectVerticalDragGestures(
                onVerticalDrag = { _, dragAmount ->
                    if (notificationCount == 0 || selectedPackage == null) {
                        totalVerticalDrag += dragAmount
                    }
                },
                onDragEnd = {
                    if (totalVerticalDrag < -50f && (notificationCount == 0 || selectedPackage == null)) {
                        viewModel.setAppDrawerVisible(true)
                    } else if (totalVerticalDrag > 50f && (notificationCount == 0 || selectedPackage == null)) {
                        XenonAccessibilityService.openNotificationsOrRequestAccess(context)
                    }
                    totalVerticalDrag = 0f
                },
                onDragCancel = { totalVerticalDrag = 0f }
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
                if (!hideAtAGlance) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(wholeScreenOffset)
                            .padding(horizontal = ExtraLargerPadding)
                            .onGloballyPositioned { atAGlanceSectionPos = it.positionInRoot() }
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    // Approximation of center for menu anchor if no specific offset is provided by combinedClickable
                                    dropDownOffset = atAGlanceSectionPos + Offset(100f, 100f)
                                    showAtAGlanceMenu = true
                                },
                                onClick = {}
                            ),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AtAGlance(
                            currentTime = currentTime,
                            currentDate = currentDate,
                            showClock = showClock,
                            calendarEvents = calendarEvents,
                            availableCalendars = availableCalendars,
                            isLandscape = true,
                            nextAlarm = nextAlarm,
                            timers = timers,
                            stopwatches = stopwatches,
                            weatherState = weatherState,
                            isWallpaperDark = wallpaperDarkIcons,
                            expanded = isGlanceExpanded,
                            onExpandedChange = { isGlanceExpanded = it },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                dropDownOffset = atAGlanceSectionPos + Offset(100f, 100f)
                                showAtAGlanceMenu = true
                            },
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }

                // Right Side: Notifications and Tabs
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                ) {
                    NotificationContent(
                        stateKey = stateKey,
                        label = "notification_content_landscape",
                        listState = landscapeListState,
                        notificationCount = notificationCount,
                        notifications = notifications,
                        indicatorType = indicatorType,
                        messageType = messageType,
                        baseColor = baseColor,
                        groupedNotifications = groupedNotifications,
                        mutedNotifications = mutedNotifications,
                        permanentNotifications = permanentNotifications,
                        appsByPackage = appsByPackage,
                        offsets = offsets,
                        replyingNotificationKey = replyingNotificationKey,
                        onReplyOpen = onReplyOpen,
                        onReplyBounds = onReplyBounds,
                        contentOffset = contentOffset,
                        overscrollConnection = hideKeyboardOnOverscroll,
                        onDismissNotification = onDismissNotification,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )

                    AnimatedVisibility(
                        visible = notificationCount > 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut(animationSpec = tween(150)) +
                                shrinkVertically(animationSpec = tween(durationMillis = 200, delayMillis = 150))
                    ) {
                        NotificationTabs(
                            sortedAppPackages = effectiveTabs,
                            groupedNotifications = effectiveGroups,
                            mutedNotifications = if (showMuteNotifications) mutedNotifications else emptyList(),
                            permanentNotifications = if (showPermanentNotifications) permanentNotifications else emptyList(),
                            selectedPackage = selectedPackage,
                            apps = apps,
                            viewModel = viewModel,
                            onDismissAllNotifications = onDismissAllNotifications,
                            onPackageSelected = { selectedPackage = it },
                            deleteButtonBounds = deleteButtonBounds,
                            onDeleteButtonBoundsChanged = { deleteButtonBounds.value = it },
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
                if (!hideAtAGlance) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.28f + 0.72f * glanceExpandProgress)
                            .then(wholeScreenOffset)
                            .padding(horizontal = ExtraLargerPadding)
                            .padding(top = LargestPadding)
                            // Once it owns the column it must not run under the dock
                            .padding(bottom = dockAreaHeight * glanceExpandProgress)
                            .onGloballyPositioned { atAGlanceSectionPos = it.positionInRoot() }
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    dropDownOffset = atAGlanceSectionPos + Offset(100f, 100f)
                                    showAtAGlanceMenu = true
                                },
                                onClick = {}
                            ),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Top
                    ) {
                        AtAGlance(
                            currentTime = currentTime,
                            currentDate = currentDate,
                            showClock = showClock,
                            calendarEvents = calendarEvents,
                            availableCalendars = availableCalendars,
                            isLandscape = false,
                            nextAlarm = nextAlarm,
                            timers = timers,
                            stopwatches = stopwatches,
                            weatherState = weatherState,
                            isWallpaperDark = wallpaperDarkIcons,
                            expanded = isGlanceExpanded,
                            onExpandedChange = { isGlanceExpanded = it },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                dropDownOffset = atAGlanceSectionPos + Offset(100f, 100f)
                                showAtAGlanceMenu = true
                            }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Weight can't be zero, so it shrinks to a sliver and fades instead
                        .weight((1f - glanceExpandProgress).coerceAtLeast(0.001f))
                        .graphicsLayer { alpha = 1f - glanceExpandProgress }
                        .clipToBounds()
                        .padding(bottom = dockAreaHeight)
                        .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMedium))
                ) {
                    NotificationContent(
                        stateKey = stateKey,
                        label = "notification_content_portrait",
                        listState = portraitListState,
                        notificationCount = notificationCount,
                        notifications = notifications,
                        indicatorType = indicatorType,
                        messageType = messageType,
                        baseColor = baseColor,
                        groupedNotifications = groupedNotifications,
                        mutedNotifications = mutedNotifications,
                        permanentNotifications = permanentNotifications,
                        appsByPackage = appsByPackage,
                        offsets = offsets,
                        replyingNotificationKey = replyingNotificationKey,
                        onReplyOpen = onReplyOpen,
                        onReplyBounds = onReplyBounds,
                        contentOffset = contentOffset,
                        overscrollConnection = hideKeyboardOnOverscroll,
                        onDismissNotification = onDismissNotification,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )

                    AnimatedVisibility(
                        visible = notificationCount > 0,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut(animationSpec = tween(150)) +
                                shrinkVertically(animationSpec = tween(durationMillis = 200, delayMillis = 150))
                    ) {
                        NotificationTabs(
                            sortedAppPackages = effectiveTabs,
                            groupedNotifications = effectiveGroups,
                            mutedNotifications = if (showMuteNotifications) mutedNotifications else emptyList(),
                            permanentNotifications = if (showPermanentNotifications) permanentNotifications else emptyList(),
                            selectedPackage = selectedPackage,
                            apps = apps,
                            viewModel = viewModel,
                            onDismissAllNotifications = onDismissAllNotifications,
                            onPackageSelected = { selectedPackage = it },
                            deleteButtonBounds = deleteButtonBounds,
                            onDeleteButtonBoundsChanged = { deleteButtonBounds.value = it },
                            modifier = wholeScreenOffset
                        )
                    }
                }
            }
        }

        // Dropdown menus
        if (showAtAGlanceMenu) {
            val selectWallpaperLabel = stringResource(R.string.select_wallpaper)
            XenonDropDown(
                expanded = true,
                onDismissRequest = { showAtAGlanceMenu = false },
                items = listOf(
                    MenuItem(
                        text = stringResource(R.string.wallpaper),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                            context.startActivity(Intent.createChooser(intent, selectWallpaperLabel))
                        },
                        leadingIcon = { Icon(Icons.Rounded.Wallpaper, null) }
                    ),
                    MenuItem(
                        text = stringResource(R.string.settings),
                        onClick = { onOpenSettings() },
                        leadingIcon = { Icon(Icons.Rounded.Settings, null) }
                    ),
                    MenuItem(
                        text = stringResource(R.string.at_a_glance_settings),
                        onClick = { viewModel.setShowCalendarSelectionDialog(true) },
                        leadingIcon = { Icon(Icons.Rounded.CalendarToday, null) }
                    )
                ),
                hazeState = if (blurSetting) hazeState else null,
                anchorPos = dropDownOffset,
                alignment = Alignment.Center,
                mainContextFont = mainFontFamily,
                subContextFont = subFontFamily
            )
        }

        if (showPageMenu) {
            val selectWallpaperLabel = stringResource(R.string.select_wallpaper)
            XenonDropDown(
                expanded = true,
                onDismissRequest = { showPageMenu = false },
                items = listOf(
                    MenuItem(
                        text = stringResource(R.string.wallpaper),
                        onClick = {
                            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                            context.startActivity(Intent.createChooser(intent, selectWallpaperLabel))
                        },
                        leadingIcon = { Icon(Icons.Rounded.Wallpaper, null) }
                    ),
                    MenuItem(
                        text = stringResource(R.string.settings),
                        onClick = { onOpenSettings() },
                        leadingIcon = { Icon(Icons.Rounded.Settings, null) }
                    ),
                    MenuItem(
                        text = stringResource(R.string.notification_manager),
                        onClick = { viewModel.setShowNotificationManagerDialog(true) },
                        leadingIcon = { Icon(Icons.Rounded.NotificationsActive, null) }
                    )
                ),
                hazeState = if (blurSetting) hazeState else null,
                anchorPos = dropDownOffset,
                alignment = Alignment.Center,
                mainContextFont = mainFontFamily,
                subContextFont = subFontFamily
            )
        }
    }
}

/**
 * The notification area shared by the landscape and portrait layouts. These branches used to be
 * written out twice, once per orientation, and differed only in the list state and label.
 */
@Composable
private fun NotificationContent(
    stateKey: String,
    label: String,
    listState: LazyListState,
    notificationCount: Int,
    notifications: List<LauncherNotification>,
    indicatorType: Int,
    messageType: Int,
    baseColor: Color,
    groupedNotifications: Map<String, List<LauncherNotification>>,
    mutedNotifications: List<LauncherNotification>,
    permanentNotifications: List<LauncherNotification>,
    appsByPackage: Map<String, AppInfo>,
    offsets: SnapshotStateMap<String, Float>,
    replyingNotificationKey: String?,
    onReplyOpen: (String?) -> Unit,
    onReplyBounds: (Rect) -> Unit,
    contentOffset: Modifier,
    overscrollConnection: NestedScrollConnection,
    onDismissNotification: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AnimatedContent(
        targetState = stateKey,
        transitionSpec = {
            (fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.98f, animationSpec = tween(150)))
                .togetherWith(fadeOut(animationSpec = tween(80)))
        },
        label = label,
        modifier = modifier
    ) { targetState ->
        when {
            targetState == "empty" -> {
                EmptyNotificationsState(
                    indicatorType = indicatorType,
                    messageType = messageType,
                    baseColor = baseColor
                )
            }
            targetState == "summary" -> {
                NotificationSummaryState(
                    notifications = notifications,
                    notificationCount = notificationCount,
                    baseColor = baseColor
                )
            }
            targetState == "muted" || targetState == "permanent" -> {
                val list = if (targetState == "muted") mutedNotifications else permanentNotifications
                NotificationLazyList(listState, contentOffset, overscrollConnection) {
                    itemsIndexed(list, key = { _, it -> it.key }) { index, notification ->
                        val appColor = rememberAppColor(appsByPackage[notification.packageName])

                        NotificationItem(
                            notification = notification,
                            appColor = appColor,
                            isFirst = index == 0,
                            isLast = index == list.size - 1,
                            offsetAbove = 0f,
                            offsetBelow = 0f,
                            replyingNotificationKey = replyingNotificationKey,
                            onReplyOpen = onReplyOpen,
                            onReplyBoundsChanged = onReplyBounds,
                            onOffsetChanged = { offsets[notification.key] = it },
                            modifier = notificationItemAnimation(),
                            onOpen = { sendContentIntent(context, notification) },
                            onDismiss = { onDismissNotification(notification.key) },
                            forceRounded = true
                        )
                    }
                }
            }
            targetState == "all" -> {
                val allNotificationsList = groupedNotifications["__ALL__"] ?: emptyList()
                val groupedByApp = remember(allNotificationsList) {
                    val map = linkedMapOf<String, MutableList<LauncherNotification>>()
                    for (notification in allNotificationsList) {
                        map.getOrPut(notification.packageName) { mutableListOf() }.add(notification)
                    }
                    map.values.toList()
                }

                NotificationLazyList(listState, contentOffset, overscrollConnection) {
                    groupedByApp.forEachIndexed { groupIndex, notificationsInGroup ->
                        itemsIndexed(notificationsInGroup, key = { _, it -> it.key }) { indexInGroup, notification ->
                            val isFirst = indexInGroup == 0
                            val isLast = indexInGroup == notificationsInGroup.size - 1
                            val isLastGroup = groupIndex == groupedByApp.size - 1
                            val offsetAbove = if (indexInGroup > 0) offsets[notificationsInGroup[indexInGroup - 1].key] ?: 0f else 0f
                            val offsetBelow = if (indexInGroup < notificationsInGroup.size - 1) offsets[notificationsInGroup[indexInGroup + 1].key] ?: 0f else 0f
                            val appColor = rememberAppColor(appsByPackage[notification.packageName])

                            NotificationItem(
                                notification = notification,
                                appColor = appColor,
                                isFirst = isFirst,
                                isLast = isLast,
                                offsetAbove = offsetAbove,
                                offsetBelow = offsetBelow,
                                replyingNotificationKey = replyingNotificationKey,
                                onReplyOpen = onReplyOpen,
                                onReplyBoundsChanged = onReplyBounds,
                                onOffsetChanged = { offsets[notification.key] = it },
                                modifier = notificationItemAnimation().then(
                                    if (isLast && !isLastGroup) Modifier.padding(bottom = SmallerPadding) else Modifier
                                ),
                                onOpen = { sendContentIntent(context, notification) },
                                onDismiss = { onDismissNotification(notification.key) }
                            )
                        }
                    }
                }
            }
            targetState.startsWith("details|") -> {
                val pkg = targetState.substringAfter("|")
                val appColor = rememberAppColor(appsByPackage[pkg])
                val notificationsInGroup = remember(groupedNotifications, pkg) {
                    groupedNotifications[pkg]?.reversed() ?: emptyList()
                }

                NotificationLazyList(listState, contentOffset, overscrollConnection) {
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
                            onReplyOpen = onReplyOpen,
                            onReplyBoundsChanged = onReplyBounds,
                            onOffsetChanged = { offsets[notification.key] = it },
                            modifier = notificationItemAnimation(),
                            onOpen = { sendContentIntent(context, notification) },
                            onDismiss = { onDismissNotification(notification.key) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationLazyList(
    listState: LazyListState,
    contentOffset: Modifier,
    overscrollConnection: NestedScrollConnection,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .then(contentOffset)
            .nestedScroll(overscrollConnection)
            .verticalEdgeFade(LargestSpacing)
            .drawVerticalScrollbar(listState, MaterialTheme.colorScheme.primary),
        verticalArrangement = Arrangement.spacedBy(SmallerSpacer, Alignment.Bottom),
        contentPadding = PaddingValues(top = LargestPadding, bottom = LargestPadding),
        content = content
    )
}

private fun LazyItemScope.notificationItemAnimation(): Modifier = Modifier.animateItem(
    fadeInSpec = tween(durationMillis = 120),
    placementSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    ),
    fadeOutSpec = tween(durationMillis = 120)
)

private fun Modifier.verticalEdgeFade(fade: Dp): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fadeHeight = fade.toPx()
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

@Composable
private fun EmptyNotificationsState(
    indicatorType: Int,
    messageType: Int,
    baseColor: Color
) {
    val emptyIcon = when (indicatorType) {
        1 -> Icons.Rounded.Check
        2 -> Icons.Rounded.EmojiEvents
        else -> null
    }
    val emptyMessage = when (messageType) {
        1 -> stringResource(R.string.notification_message_no_notification)
        2 -> stringResource(R.string.notification_message_up_to_date)
        else -> ""
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LargeMediumSpacer)
        ) {
            if (emptyIcon != null) {
                Icon(
                    imageVector = emptyIcon,
                    contentDescription = null,
                    tint = baseColor.copy(alpha = 0.8f),
                    modifier = Modifier.size(HugerSpacing)
                )
            }
            if (emptyMessage.isNotEmpty()) {
                Text(
                    text = emptyMessage,
                    color = baseColor.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

@Composable
private fun NotificationSummaryState(
    notifications: List<LauncherNotification>,
    notificationCount: Int,
    baseColor: Color
) {
    val allMuted = remember(notifications) {
        notifications.isNotEmpty() && notifications.all { it.isMuted }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LargeMediumSpacer)
        ) {
            Icon(
                imageVector = if (allMuted) Icons.Rounded.NotificationsOff else Icons.Rounded.NotificationsActive,
                contentDescription = null,
                tint = baseColor.copy(alpha = 0.8f),
                modifier = Modifier.size(HugerSpacing)
            )
            Text(
                text = if (allMuted) stringResource(R.string.notification_message_no_notification) else pluralStringResource(R.plurals.notification_count, notificationCount, notificationCount),
                color = baseColor.copy(alpha = 0.8f),
                fontSize = 18.sp,
                fontFamily = mainFontFamily,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun sendContentIntent(
    context: android.content.Context,
    notification: LauncherNotification
) {
    val intent = notification.contentIntent ?: return
    try {
        @Suppress("DEPRECATION")
        val options = ActivityOptions.makeBasic().apply {
            pendingIntentBackgroundActivityStartMode =
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }
        intent.send(context, 0, null, null, null, null, options.toBundle())
    } catch (_: Exception) {
        try { intent.send() } catch (_: Exception) {}
    }
}

fun Modifier.drawVerticalScrollbar(
    state: LazyListState,
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
            topLeft = Offset(size.width - MediumSpacing.toPx(), scrollbarOffset + SmallSpacing.toPx()),
            size = Size(SmallSpacing.toPx(), (scrollbarHeight - MediumSpacing.toPx()).coerceAtLeast(LargestPadding.toPx())),
            cornerRadius = CornerRadius(SmallerCornerRadius.toPx())
        )
    }
}

/**
 * Single-line text that scrolls only when it doesn't fit. The right edge fades whenever the text
 * overflows, the left edge only while it is moving, like the event titles.
 */
@Composable
private fun FadingMarqueeText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = mainFontFamily
    )
    val textWidth = remember(text, textStyle) {
        textMeasurer.measure(text, textStyle, maxLines = 1).size.width
    }
    var containerWidthPx by remember { mutableIntStateOf(0) }
    val needsMarquee = containerWidthPx in 1..<textWidth

    var isScrolling by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    LaunchedEffect(text, textWidth, containerWidthPx, needsMarquee) {
        isScrolling = false
        if (!needsMarquee) return@LaunchedEffect
        // Follows basicMarquee's cycle so the left fade only shows while the text moves
        val velocityPx = with(density) { BiggerSpacing.toPx() }
        val scrollDistance = textWidth + containerWidthPx / 3f
        val scrollDuration = (scrollDistance / velocityPx * 1000).toLong()
        while (true) {
            isScrolling = false
            delay(1200.milliseconds)
            isScrolling = true
            delay(scrollDuration.milliseconds)
        }
    }

    val startFadeAlpha by animateFloatAsState(
        targetValue = if (isScrolling) 1f else 0f,
        animationSpec = tween(150),
        label = "marqueeStartFade"
    )
    val endFadeAlpha by animateFloatAsState(
        targetValue = if (needsMarquee) 1f else 0f,
        animationSpec = tween(150),
        label = "marqueeEndFade"
    )

    Text(
        text = text,
        style = textStyle,
        color = color,
        maxLines = 1,
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { containerWidthPx = it.size.width }
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                if (!needsMarquee) return@drawWithContent
                val fadeWidth = BiggestSpacing.toPx()
                // Start Fade (Left)
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 1f - startFadeAlpha),
                        fadeWidth / size.width to Color.Black,
                        1f to Color.Black
                    ),
                    blendMode = BlendMode.DstIn
                )
                // End Fade (Right)
                drawRect(
                    brush = Brush.horizontalGradient(
                        0f to Color.Black,
                        (size.width - fadeWidth * endFadeAlpha) / size.width to Color.Black,
                        1f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
            .basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = 1200)
    )
}

private enum class WeatherViewMode { NOW, TODAY }

@Composable
fun AtAGlance(
    currentTime: String,
    currentDate: String,
    showClock: Boolean,
    calendarEvents: List<CalendarEvent>,
    availableCalendars: List<CalendarInfo>,
    isLandscape: Boolean,
    nextAlarm: AlarmManager.AlarmClockInfo?,
    timers: List<LauncherNotification>,
    stopwatches: List<LauncherNotification>,
    weatherState: WeatherState,
    isWallpaperDark: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    expanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val baseColor = if (isWallpaperDark) Color.Black else Color.White
    val dateFontSize = if (isLandscape) 18.sp else 16.sp
    val eventTitleFontSize = if (isLandscape) 32.sp else 24.sp
    val subtitleFontSize = if (isLandscape) 16.sp else 14.sp
    val spacing = if (isLandscape) MediumSpacing else SmallSpacing
    val pageHeight = if (isLandscape) 80.dp else 60.dp

    val pagerState = rememberPagerState { calendarEvents.size + 1 }
    val scope = rememberCoroutineScope()

    var weatherViewMode by remember { mutableStateOf(WeatherViewMode.TODAY) }

    val isDay = remember(currentTime) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        hour in 6..18
    }

    fun getWeatherIcon(condition: String, currentIsDay: Boolean): Int {
        val c = condition.lowercase()
        val (day, night) = when {
            c.contains("thunder shower") || c.contains("t-shower") || c.contains("gewitterregen") -> R.drawable.tshower1 to R.drawable.tshower0
            c.contains("thunder") || c.contains("storm") || c.contains("gewitter") || c.contains("sturm") -> R.drawable.tstorm1 to R.drawable.tstorm0
            c.contains("tornado") -> R.drawable.tornado1 to R.drawable.tornado0
            c.contains("hail") || c.contains("hagel") -> R.drawable.hail1 to R.drawable.hail0
            c.contains("sleet") || c.contains("schneeregen") -> R.drawable.sleet1 to R.drawable.sleet0
            c.contains("light snow") || c.contains("flurr") || c.contains("leichter schnee") -> R.drawable.lsnow1 to R.drawable.lsnow0
            c.contains("snow") || c.contains("ice") || c.contains("schnee") || c.contains("eis") -> R.drawable.snow1 to R.drawable.snow0
            c.contains("shower") || c.contains("drizzle") || c.contains("schauer") || c.contains("niesel") -> R.drawable.shower1 to R.drawable.shower0
            c.contains("rain") || c.contains("regen") -> R.drawable.rain1 to R.drawable.rain0
            c.contains("fog") || c.contains("mist") || c.contains("haze") || c.contains("nebel") || c.contains("dunst") -> R.drawable.fog1 to R.drawable.fog0
            c.contains("wind") -> R.drawable.windy1 to R.drawable.windy0
            c.contains("partly") || c.contains("teilweise") || c.contains("leicht bewölkt") -> R.drawable.pcloudy1 to R.drawable.pcloudy0
            c.contains("overcast") || c.contains("cloud") || c.contains("bedeckt") || c.contains("wolken") || c.contains("wolkig") || c.contains("bewölkt") -> R.drawable.mcloudy1 to R.drawable.mcloudy0
            c.contains("clear") || c.contains("sunny") || c.contains("klar") || c.contains("sonnig") -> R.drawable.clear1 to R.drawable.clear0
            else -> R.drawable.unknown1 to R.drawable.unknown0
        }
        return if (currentIsDay) day else night
    }

    var totalDrag by remember { mutableFloatStateOf(0f) }
    var dragTriggered by remember { mutableStateOf(false) }
    val swipeThreshold = with(LocalDensity.current) { ExtraLargerSpacing.toPx() }
    val haptic = LocalHapticFeedback.current

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

    Box(
        modifier = modifier
            .fillMaxHeight()
            // Swiping pages the compact pager; the expanded list scrolls on its own
            .pointerInput(calendarEvents.size + 1, expanded) {
                if (expanded || calendarEvents.size + 1 <= 1) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = {
                        totalDrag = 0f
                        dragTriggered = false
                    },
                    onVerticalDrag = { _, dragAmount ->
                        if (dragTriggered) return@detectVerticalDragGestures
                        totalDrag += dragAmount
                        if (abs(totalDrag) > swipeThreshold) {
                            dragTriggered = true
                            scope.launch {
                                if (totalDrag < 0 && pagerState.currentPage < calendarEvents.size) {
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
            modifier = if (expanded) Modifier.fillMaxHeight() else Modifier.wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showClock) {
                        Text(
                            text = "$currentTime · ",
                            fontSize = dateFontSize,
                            fontWeight = FontWeight.Bold,
                            color = baseColor.copy(alpha = 0.95f)
                        )
                    }
                    Text(
                        text = currentDate,
                        fontSize = dateFontSize,
                        fontWeight = FontWeight.Medium,
                        color = baseColor.copy(alpha = 0.7f)
                    )
                }

                ChronoCluster(
                    timers = timers,
                    stopwatches = stopwatches,
                    nextAlarm = nextAlarm,
                    fontSize = dateFontSize,
                    isWallpaperDark = isWallpaperDark
                )
            }

            AnimatedContent(
                targetState = expanded,
                transitionSpec = {
                    fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "glanceContent",
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (expanded) Modifier.weight(1f) else Modifier)
            ) { showList ->
                if (showList) {
                    GlanceEventList(
                        calendarEvents = calendarEvents,
                        availableCalendars = availableCalendars,
                        weatherState = weatherState,
                        weatherIconRes = getWeatherIcon(weatherState.dailyCondition ?: weatherState.condition, true),
                        timeFormatter = timeFormatter,
                        baseColor = baseColor,
                        titleFontSize = dateFontSize,
                        subtitleFontSize = subtitleFontSize,
                        onPick = { page ->
                            scope.launch { pagerState.scrollToPage(page) }
                            onExpandedChange(false)
                        }
                    )
                } else {
                    if (calendarEvents.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_upcoming_events),
                            fontSize = eventTitleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = baseColor,
                            modifier = Modifier
                                .height(pageHeight)
                                .wrapContentHeight(Alignment.CenterVertically)
                        )
                    } else {
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
                                    .verticalEdgeFade(MediumSpacing),
                                horizontalAlignment = Alignment.Start
                            ) { index ->
                                if (index == 0) {
                                    // WEATHER PAGE
                                    AnimatedContent(
                                        targetState = weatherViewMode,
                                        transitionSpec = {
                                            fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                                        },
                                        label = "weatherFade"
                                    ) { mode ->
                                        val conditionText = if (mode == WeatherViewMode.TODAY) {
                                            weatherState.dailyCondition ?: weatherState.condition
                                        } else {
                                            weatherState.condition
                                        }
                                        val iconRes = if (mode == WeatherViewMode.TODAY) {
                                            getWeatherIcon(conditionText, true)
                                        } else {
                                            getWeatherIcon(conditionText, isDay)
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = {
                                                        weatherViewMode = if (weatherViewMode == WeatherViewMode.NOW) WeatherViewMode.TODAY else WeatherViewMode.NOW
                                                    }
                                                )
                                        ) {
                                            Box(
                                                modifier = Modifier.size(BigSpacing),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    painter = painterResource(id = iconRes),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(BigSpacing)
                                                )
                                            }

                                            Column(
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                val todayLabel = stringResource(R.string.today)
                                                val currentLocale = LocalConfiguration.current.locales[0]
                                                val nowLabel = stringResource(R.string.now).replaceFirstChar { if (it.isLowerCase()) it.titlecase(currentLocale) else it.toString() }
                                                val tempText = if (mode == WeatherViewMode.TODAY) {
                                                    if (weatherState.maxTemp != null && weatherState.minTemp != null) {
                                                        "$todayLabel ${weatherState.maxTemp.replace("+", "")}/${weatherState.minTemp.replace("+", "")}"
                                                    } else {
                                                        weatherState.temperature.replace("+", "")
                                                    }
                                                } else {
                                                    "$nowLabel ${weatherState.temperature.replace("+", "")}"
                                                }
                                                FadingMarqueeText(
                                                    text = tempText,
                                                    fontSize = eventTitleFontSize,
                                                    fontWeight = FontWeight.Bold,
                                                    color = baseColor
                                                )
                                                FadingMarqueeText(
                                                    text = conditionText,
                                                    fontSize = subtitleFontSize,
                                                    color = baseColor.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    val event = calendarEvents[index - 1]
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(LargeMediumSpacing),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .combinedClickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onLongClick = {
                                                    onLongClick?.invoke()
                                                },
                                                onClick = {
                                                    try {
                                                        val uri = ContentUris.withAppendedId(
                                                            CalendarContract.Events.CONTENT_URI,
                                                            event.id
                                                        )
                                                        val intent = Intent(Intent.ACTION_VIEW).setData(uri)
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {
                                                        // Fallback to opening calendar at specific time
                                                        val builder = CalendarContract.CONTENT_URI.buildUpon()
                                                            .appendPath("time")
                                                        ContentUris.appendId(builder, event.startTime)
                                                        val intent = Intent(Intent.ACTION_VIEW).setData(builder.build())
                                                        context.startActivity(intent)
                                                    }
                                                }
                                            )
                                    ) {
                                        val calInfo = availableCalendars.find { it.id == event.calendarId }
                                        val pillColor = if (event.color != null && event.color != 0) {
                                            Color(event.color)
                                        } else {
                                            calInfo?.color?.let { Color(it) } ?: baseColor.copy(alpha = 0.5f)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(MediumSpacing)
                                                .height(ExtraLargestSpacing)
                                                .shadow(elevation = SmallerElevation, shape = RoundedCornerShape(MassiveCornerRadius))
                                                .background(pillColor, RoundedCornerShape(MassiveCornerRadius))
                                        )

                                        Column(
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            FadingMarqueeText(
                                                text = event.title,
                                                fontSize = eventTitleFontSize,
                                                fontWeight = FontWeight.Bold,
                                                color = baseColor
                                            )
                                            val todayLabel = stringResource(R.string.today)
                                            val tomorrowLabel = stringResource(R.string.tomorrow)
                                            val allDayLabel = stringResource(R.string.all_day)
                                            val timeText = remember(event, timeFormatter, todayLabel, tomorrowLabel, allDayLabel) {
                                                eventTimeText(event, timeFormatter, todayLabel, tomorrowLabel, allDayLabel)
                                            }
                                            Text(
                                                text = timeText,
                                                fontSize = subtitleFontSize,
                                                color = baseColor.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }

                            if (calendarEvents.isNotEmpty()) {
                                val expand = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onExpandedChange(true)
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(SmallSpacing, Alignment.CenterVertically),
                                    modifier = Modifier
                                        .width(ExtraBiggerSpacing)
                                        .height(HugestSpacing + SmallSpacing)
                                        .pointerInput(Unit) {
                                            awaitEachGesture {
                                                awaitFirstDown(requireUnconsumed = false).consume()
                                            }
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowUp,
                                        contentDescription = stringResource(R.string.scroll_up),
                                        tint = if (pagerState.currentPage > 0) baseColor.copy(alpha = 0.5f) else baseColor.copy(alpha = 0.15f),
                                        modifier = Modifier
                                            .size(ExtraLargerSpacing)
                                            .combinedClickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onLongClick = expand
                                            ) {
                                                if (pagerState.currentPage > 0) {
                                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                                }
                                            }
                                    )

                                    Box(
                                        modifier = Modifier.combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onLongClick = expand,
                                            onClick = {}
                                        )
                                    ) {
                                        if (pagerState.currentPage == 0) {
                                            StatusCounters(
                                                notificationCount = 0,
                                                calendarEventCount = calendarEvents.size,
                                                calendarColor = baseColor.copy(alpha = 0.7f),
                                                calendarTextColor = if (isWallpaperDark) Color.White else Color.Black,
                                                modifier = Modifier.padding(bottom = SmallPadding)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(bottom = SmallPadding).height(ExtraLargeSpacing),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${pagerState.currentPage}/${calendarEvents.size}",
                                                    color = baseColor.copy(alpha = 0.5f),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = mainFontFamily
                                                )
                                            }
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = stringResource(R.string.scroll_down),
                                        tint = if (pagerState.currentPage < calendarEvents.size) baseColor.copy(alpha = 0.5f) else baseColor.copy(alpha = 0.15f),
                                        modifier = Modifier
                                            .size(ExtraLargerSpacing)
                                            .combinedClickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onLongClick = expand
                                            ) {
                                                if (pagerState.currentPage < calendarEvents.size) {
                                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                                }
                                            }
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

private data class CachedTabInfo(
    val app: AppInfo?,
    val iconBitmap: ImageBitmap?,
    val iconKey: String?,
    val notificationCount: Int,
    val appColor: Color,
    val contrastColor: Color,
    val isSelected: Boolean
)

@Composable
fun NotificationTabs(
    sortedAppPackages: List<String>,
    groupedNotifications: Map<String, List<LauncherNotification>>,
    mutedNotifications: List<LauncherNotification> = emptyList(),
    permanentNotifications: List<LauncherNotification> = emptyList(),
    selectedPackage: String?,
    apps: List<AppInfo>,
    viewModel: LauncherViewModel,
    onDismissAllNotifications: () -> Unit,
    onPackageSelected: (String?) -> Unit,
    deleteButtonBounds: State<Rect>,
    onDeleteButtonBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .blockHorizontalPagerSwipe()
    ) {
        val context = LocalContext.current
        val notificationDeleteSinglePress by viewModel.notificationDeleteSinglePress.collectAsState()
        val containerWidth = maxWidth
        val horizontalPadding = LargestPadding
        val availableWidth = containerWidth - (horizontalPadding * 2)

        val tabCount = sortedAppPackages.size
        val tabSpacing = SmallSpacing
        val maxVisible = 5
        val totalItems = tabCount + 1

        val isScrollable = totalItems > maxVisible

        val visibleCount = totalItems.coerceAtMost(maxVisible)
        val totalSpacing = tabSpacing * (visibleCount - 1)
        val targetItemWidth = ((availableWidth - totalSpacing) / visibleCount).coerceAtLeast(40.dp)

        val animatedItemWidth by animateDpAsState(
            targetValue = targetItemWidth,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "tab_item_width"
        )

        var displayedPackages by remember { mutableStateOf(sortedAppPackages) }
        val leavingPackages = remember { mutableStateMapOf<String, Boolean>() }
        // Plain map, not snapshot state. It was written during composition on every pass with a
        // fresh CachedTabInfo instance, which counted as a change and invalidated the tab strip
        // that reads it, causing extra recompositions. It is only a fallback for tabs that are
        // leaving, and the change that makes a tab leave already recomposes this scope.
        val cachedTabInfo = remember { HashMap<String, CachedTabInfo>() }

        LaunchedEffect(sortedAppPackages) {
            val currentSet = sortedAppPackages.toSet()
            val special = listOf("__MUTED__", "__PERMANENT__")

            // 1. Mark removed tabs as leaving; forget tabs that came back.
            displayedPackages.forEach { pkg ->
                if (pkg in currentSet) {
                    leavingPackages.remove(pkg)
                } else if (leavingPackages[pkg] != true) {
                    leavingPackages[pkg] = true
                }
            }

            // 2. Start from the live order (special tabs are already last there) and put every
            //    leaving tab back where it was, so it collapses in place instead of jumping.
            val newDisplayed = sortedAppPackages.toMutableList()
            var anchor: String? = null
            for (pkg in displayedPackages) {
                if (pkg in currentSet) {
                    anchor = pkg
                    continue
                }
                val insertAt = if (pkg in special) {
                    // Keep special tabs after all normal tabs and in their fixed order
                    val order = special.indexOf(pkg)
                    newDisplayed.indexOfFirst { it in special && special.indexOf(it) > order }
                        .takeIf { it >= 0 } ?: newDisplayed.size
                } else {
                    anchor?.let { newDisplayed.indexOf(it) + 1 } ?: 0
                }
                newDisplayed.add(insertAt, pkg)
                anchor = pkg
            }

            displayedPackages = newDisplayed
        }

        val scrollState = rememberScrollState()
        val view = LocalView.current
        val haptic = LocalHapticFeedback.current

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
        val deleteCornerRadius by animateDpAsState(
            targetValue = if (isDeletePressed) SmallCornerRadius else LargeMediumCornerRadius,
            label = "delete_corner"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .nestedScroll(blockPagerScroll)
                    .then(
                        if (isScrollable) {
                            Modifier
                                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                .drawWithContent {
                                    drawContent()
                                    val fadeWidth = LargestSpacing.toPx()
                                    val contentRight = size.width - animatedItemWidth.toPx()

                                    if (scrollState.value > 0.5f) {
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(Color.Transparent, Color.Black),
                                                startX = 0f, endX = fadeWidth
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                                    if (scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue - 0.5f) {
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(Color.Black, Color.Transparent),
                                                startX = contentRight - fadeWidth, endX = contentRight
                                            ),
                                            blendMode = BlendMode.DstIn
                                        )
                                    }
                                }
                        } else {
                            Modifier
                        }
                    )
                    .horizontalScroll(scrollState, enabled = isScrollable)
                    .graphicsLayer {
                        shape = RoundedCornerShape(
                            topStart = deleteCornerRadius,
                            bottomStart = deleteCornerRadius,
                            topEnd = deleteCornerRadius,
                            bottomEnd = deleteCornerRadius
                        )
                        clip = true
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (pkg in displayedPackages) {
                    key(pkg) {
                        val isLeaving = leavingPackages[pkg] == true
                        val isVisible = !isLeaving

                        val tabWidth by animateDpAsState(
                            targetValue = if (isVisible) animatedItemWidth + tabSpacing else NoSpacing,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "tab_width_$pkg",
                            finishedListener = { finalWidth ->
                                if (isLeaving && finalWidth <= 0.5.dp) {
                                    leavingPackages.remove(pkg)
                                    cachedTabInfo.remove(pkg)
                                    displayedPackages = displayedPackages.filter { it != pkg }
                                }
                            }
                        )

                        if (tabWidth > 0.1.dp) {
                            Box(
                                modifier = Modifier
                                    .width(tabWidth)
                                    .graphicsLayer {
                                        alpha = (tabWidth / (animatedItemWidth + tabSpacing).coerceAtLeast(SmallestSpacing)).coerceIn(0f, 1f)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val isMutedTab = pkg == "__MUTED__"
                                val isPermanentTab = pkg == "__PERMANENT__"
                                val isAllTab = pkg == "__ALL__"
                                val liveApp = remember(apps, pkg) {
                                    if (isMutedTab || isPermanentTab || isAllTab) null else apps.find { it.packageName == pkg }
                                }
                                val liveNotifications = when {
                                    isMutedTab -> mutedNotifications
                                    isPermanentTab -> permanentNotifications
                                    else -> groupedNotifications[pkg] ?: emptyList()
                                }
                                val liveLatestNotification = liveNotifications.firstOrNull()

                                val liveAppColor = when {
                                    isMutedTab -> MaterialTheme.colorScheme.surfaceContainerHighest
                                    isPermanentTab -> MaterialTheme.colorScheme.primary
                                    isAllTab -> MaterialTheme.colorScheme.primary
                                    else -> rememberAppColor(liveApp)
                                }

                                val liveContrastColor = remember(liveAppColor) { ColorUtils.getContrastColor(liveAppColor) }
                                val isSelected = selectedPackage == pkg

                                val liveIconBitmap = remember(liveLatestNotification?.iconKey, liveLatestNotification?.icon, liveApp?.icon) {
                                    if (isMutedTab || isPermanentTab || isAllTab) return@remember null
                                    val drawable = liveLatestNotification?.icon ?: liveApp?.icon
                                    try {
                                        drawable?.toBitmap(width = 40, height = 40)?.asImageBitmap()
                                    } catch (_: Exception) {
                                        null
                                    }
                                }

                                if (liveNotifications.isNotEmpty() && (liveIconBitmap != null || isMutedTab || isPermanentTab || isAllTab)) {
                                    val iconKeyPrefix = when {
                                        isMutedTab -> "muted_tab"
                                        isPermanentTab -> "permanent_tab"
                                        isAllTab -> "all_tab"
                                        else -> liveLatestNotification?.iconKey
                                    }
                                    cachedTabInfo[pkg] = CachedTabInfo(
                                        app = liveApp,
                                        iconBitmap = liveIconBitmap,
                                        iconKey = iconKeyPrefix,
                                        notificationCount = liveNotifications.size,
                                        appColor = liveAppColor,
                                        contrastColor = liveContrastColor,
                                        isSelected = isSelected
                                    )
                                }

                                val cached = cachedTabInfo[pkg]

                                val appToUse = liveApp ?: cached?.app
                                val iconBitmapToUse = cached?.iconBitmap ?: liveIconBitmap
                                val iconKeyToUse = cached?.iconKey ?: when {
                                    isMutedTab -> "muted_tab"
                                    isPermanentTab -> "permanent_tab"
                                    isAllTab -> "all_tab"
                                    else -> liveLatestNotification?.iconKey
                                }
                                val countToUse = if (liveNotifications.isEmpty()) (cached?.notificationCount ?: 1) else liveNotifications.size
                                val appColorToUse = if (liveNotifications.isEmpty()) (cached?.appColor ?: liveAppColor) else liveAppColor
                                val contrastColorToUse = if (liveNotifications.isEmpty()) (cached?.contrastColor ?: liveContrastColor) else liveContrastColor
                                val isSelectedToUse = if (liveNotifications.isEmpty()) (cached?.isSelected ?: isSelected) else isSelected

                                NotificationTabButton(
                                    app = appToUse,
                                    notificationIconBitmap = iconBitmapToUse,
                                    overrideIcon = when {
                                        isMutedTab -> Icons.Rounded.NotificationsOff
                                        isPermanentTab -> Icons.Rounded.PushPin
                                        isAllTab -> Icons.Rounded.Notifications
                                        else -> null
                                    },
                                    notificationCount = countToUse,
                                    isSelected = isSelectedToUse,
                                    appColor = appColorToUse,
                                    contrastColor = contrastColorToUse,
                                    onClick = { onPackageSelected(if (isSelected) null else pkg) },
                                    onDismiss = {
                                        when {
                                            isMutedTab -> viewModel.dismissMutedNotifications()
                                            isPermanentTab -> viewModel.dismissPermanentNotifications()
                                            isAllTab -> onDismissAllNotifications()
                                            else -> viewModel.dismissNotificationsByPackage(pkg)
                                        }
                                    },
                                    isOverDelete = { tabRect ->
                                        val bounds = deleteButtonBounds.value
                                        if (bounds.isEmpty) return@NotificationTabButton false
                                        val intersection = bounds.intersect(tabRect)
                                        val overlapRatio = if (intersection.isEmpty) 0f else {
                                            (intersection.width * intersection.height) / (bounds.width * bounds.height)
                                        }
                                        overlapRatio >= 0.5f || tabRect.center.x >= bounds.left
                                    },
                                    iconKey = iconKeyToUse,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = tabSpacing)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(animatedItemWidth))
            }

            Surface(
                shape = RoundedCornerShape(deleteCornerRadius),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(ExtraBigSpacing)
                    .width(animatedItemWidth)
                    .onGloballyPositioned { onDeleteButtonBoundsChanged(it.boundsInRoot()) }
                    .clip(RoundedCornerShape(deleteCornerRadius))
                    .combinedClickable(
                        interactionSource = deleteInteractionSource,
                        indication = LocalIndication.current,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismissAllNotifications()
                        },
                        onClick = {
                            if (notificationDeleteSinglePress) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDismissAllNotifications()
                            } else {
                                Toast.makeText(
                                    context,
                                    R.string.long_press_to_delete_all,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.clear_all),
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(ExtraLargeSpacing)
                    )
                }
            }
        }
    }
}

private fun eventTimeText(
    event: CalendarEvent,
    timeFormatter: SimpleDateFormat,
    todayLabel: String,
    tomorrowLabel: String,
    allDayLabel: String
): String {
    val now = System.currentTimeMillis()
    val nowCal = Calendar.getInstance()
    val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

    // Check if the event is currently active (ongoing)
    val isOngoing = now in event.startTime..event.endTime

    val eventStartMillis = if (event.isAllDay) {
        event.startTime - TimeZone.getDefault().getOffset(event.startTime)
    } else {
        event.startTime
    }
    val eventStartCal = Calendar.getInstance().apply { timeInMillis = eventStartMillis }

    val isToday = eventStartCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            eventStartCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
    val isTomorrow = eventStartCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
            eventStartCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)

    val dayPrefix = when {
        isToday || isOngoing -> "$todayLabel "
        isTomorrow -> "$tomorrowLabel "
        else -> SimpleDateFormat("EEE, d MMM ", Locale.getDefault()).format(eventStartMillis)
    }

    return if (event.isAllDay) {
        dayPrefix + allDayLabel
    } else {
        dayPrefix + "${timeFormatter.format(event.startTime)} - ${timeFormatter.format(event.endTime)}"
    }
}

/**
 * At a Glance, expanded: the weather on top and every event below it in a scrolling list.
 * Tapping a row jumps the pager to that page and hands control back to the default layout.
 */
@Composable
private fun GlanceEventList(
    calendarEvents: List<CalendarEvent>,
    availableCalendars: List<CalendarInfo>,
    weatherState: WeatherState,
    weatherIconRes: Int,
    timeFormatter: SimpleDateFormat,
    baseColor: Color,
    titleFontSize: TextUnit,
    subtitleFontSize: TextUnit,
    onPick: (page: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayLabel = stringResource(R.string.today)
    val tomorrowLabel = stringResource(R.string.tomorrow)
    val allDayLabel = stringResource(R.string.all_day)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(LargeMediumSpacing),
        contentPadding = PaddingValues(vertical = SmallPadding)
    ) {
        item {
            val conditionText = weatherState.dailyCondition ?: weatherState.condition
            val tempText = if (weatherState.maxTemp != null && weatherState.minTemp != null) {
                "$todayLabel ${weatherState.maxTemp.replace("+", "")}/${weatherState.minTemp.replace("+", "")}"
            } else {
                weatherState.temperature.replace("+", "")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onPick(0) }
            ) {
                Image(
                    painter = painterResource(id = weatherIconRes),
                    contentDescription = null,
                    modifier = Modifier.size(BigSpacing)
                )
                Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                    Text(
                        text = tempText,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = baseColor,
                        fontFamily = mainFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = conditionText,
                        fontSize = subtitleFontSize,
                        color = baseColor.copy(alpha = 0.7f),
                        fontFamily = mainFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // No keys: instances of a recurring event share one event id
        itemsIndexed(calendarEvents) { index, event ->
            val calInfo = availableCalendars.find { it.id == event.calendarId }
            val pillColor = if (event.color != null && event.color != 0) {
                Color(event.color)
            } else {
                calInfo?.color?.let { Color(it) } ?: baseColor.copy(alpha = 0.5f)
            }
            val timeText = remember(event, timeFormatter, todayLabel, tomorrowLabel, allDayLabel) {
                eventTimeText(event, timeFormatter, todayLabel, tomorrowLabel, allDayLabel)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LargeMediumSpacing),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onPick(index + 1) }
            ) {
                Box(
                    modifier = Modifier
                        .width(MediumSpacing)
                        .height(ExtraLargestSpacing)
                        .shadow(elevation = SmallerElevation, shape = RoundedCornerShape(MassiveCornerRadius))
                        .background(pillColor, RoundedCornerShape(MassiveCornerRadius))
                )
                Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = baseColor,
                        fontFamily = mainFontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = timeText,
                        fontSize = subtitleFontSize,
                        color = baseColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}