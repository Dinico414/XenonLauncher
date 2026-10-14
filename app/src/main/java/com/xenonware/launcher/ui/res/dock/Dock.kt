package com.xenonware.launcher.ui.res.dock

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Assistant
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.values.BigSpacing
import com.xenon.mylibrary.values.BiggestSpacing
import com.xenon.mylibrary.values.HugeBiggerSpacing
import com.xenon.mylibrary.values.HugerSpacing
import com.xenon.mylibrary.values.LargeMediumPadding
import com.xenon.mylibrary.values.LargeMediumSpacer
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.LargestSpacing
import com.xenon.mylibrary.values.MassiveCornerRadius
import com.xenon.mylibrary.values.MediumElevation
import com.xenon.mylibrary.values.MediumSpacer
import com.xenon.mylibrary.values.MediumSpacing
import com.xenon.mylibrary.values.NoElevation
import com.xenon.mylibrary.values.NoSpacing
import com.xenon.mylibrary.values.SmallPadding
import com.xenonware.launcher.R
import com.xenonware.launcher.media.MediaState
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.model.FabAction
import com.xenonware.launcher.notification.LauncherNotification
import com.xenonware.launcher.ui.theme.LocalIsDarkTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlin.math.roundToInt

internal enum class DockPage { Status, Apps, Media }

internal val DockHeight = HugeBiggerSpacing
internal val DockFabSize = HugerSpacing
internal val DockCollapsedSectionWidth = BiggestSpacing

/**
 * IME height threshold separating the word-suggestion strip from a full software keyboard.
 * With a hardware keyboard driving input only the suggestion bar shows (~40-56dp), while a full
 * on-screen keyboard is much taller (~200dp+). Anything above this is treated as the software
 * keyboard being in use.
 */
private val SuggestionStripMaxHeight = 200.dp

internal val DockSectionShape = RoundedCornerShape(MassiveCornerRadius)

@Composable
internal fun dockButtonAlpha(): Float = if (LocalIsDarkTheme.current) 0.35f else 1f

@Composable
internal fun Modifier.dockSectionSize(
    isExpanded: Boolean,
    collapsedWidth: Dp = DockCollapsedSectionWidth
): Modifier {
    val verticalPadding by animateDpAsState(
        targetValue = if (isExpanded) SmallPadding else LargeMediumPadding,
        label = "dockSectionPadding"
    )
    return this
        .fillMaxHeight()
        .padding(vertical = verticalPadding)
        .then(
            if (isExpanded) Modifier.fillMaxWidth()
            else Modifier.requiredWidth(collapsedWidth)
        )
}

@Composable
private fun rememberDockBottomPadding(
    dockSafeDrawIme: Boolean,
    dockSafeDrawImePortraitOnly: Boolean = false,
    dockSafeDrawImeHardwareKeyboardOnly: Boolean = false
): Dp {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val hasHardwareKeyboard = configuration.keyboard != android.content.res.Configuration.KEYBOARD_NOKEYS &&
            configuration.hardKeyboardHidden == android.content.res.Configuration.HARDKEYBOARDHIDDEN_NO

    val navPadding = WindowInsets.navigationBars
        .only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val imePadding = WindowInsets.ime
        .only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()

    val orientationOk = !dockSafeDrawImePortraitOnly || !isLandscape

    // With the "hardware keyboard" option: when a hardware keyboard is connected we always move
    // for the keyboard, UNLESS a full software keyboard is shown. A full software keyboard makes
    // the IME inset tall (> SuggestionStripMaxHeight); a small inset means only the suggestion
    // strip is visible and the hardware keyboard is driving input.
    val imeIsFullSoftwareKeyboard = imePadding > SuggestionStripMaxHeight
    val keyboardOk = if (dockSafeDrawImeHardwareKeyboardOnly) {
        hasHardwareKeyboard && !imeIsFullSoftwareKeyboard
    } else {
        imePadding > NoSpacing
    }
    val shouldMoveForIme = dockSafeDrawIme && orientationOk && keyboardOk

    val safeDrawBottom = if (shouldMoveForIme) maxOf(navPadding, imePadding) else navPadding
    val target = if (safeDrawBottom < LargestSpacing) LargestSpacing else safeDrawBottom + MediumSpacing

    val animated by animateDpAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dockPaddingAnimation"
    )
    return animated.coerceAtLeast(NoSpacing)
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun DockPill(
    modifier: Modifier = Modifier,
    apps: List<AppInfo>,
    notifications: List<LauncherNotification>,
    badgeType: Int,
    mediaState: MediaState,
    isMediaPermissionGranted: Boolean,
    notificationCount: Int,
    calendarEventCount: Int = 0,
    currentTime: String,
    currentDate: String,
    weatherTemp: String,
    weatherCondition: String,
    onAppClick: (String) -> Unit,
    onFabClick: () -> Unit,
    onFabDoubleTap: () -> Unit = {},
    onFabLongPress: () -> Unit = {},
    onFabSwipeUp: () -> Unit = {},
    onMediaPlayPause: () -> Unit,
    onMediaSkipNext: () -> Unit,
    onOpenMediaPermission: () -> Unit,
    onTimeClick: () -> Unit = {},
    onDateClick: () -> Unit = {},
    onWeatherClick: () -> Unit = {},
    isAppDrawerVisible: Boolean = false,
    hazeState: HazeState? = null,
    progress: Float = 1f,
    isCharging: Boolean = false,
    dockSafeDrawIme: Boolean = false,
    dockSafeDrawImePortraitOnly: Boolean = false,
    dockSafeDrawImeHardwareKeyboardOnly: Boolean = false,
    hideActionButton: Boolean = false,
    fabSingleTapAction: FabAction = FabAction.OPEN_APP_DRAWER,
    onUnpinApp: (String) -> Unit = {},
    onPinApp: (String, Int) -> Unit = { _, _ -> },
    onReorderApp: (Int, Int) -> Unit = { _, _ -> },
) {
    // True window width in px -> dp; screenWidthDp lags multi-window/foldable resizes.
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val screenWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val finalMaxDockWidth = screenWidth.coerceAtMost(540.dp)

    var currentPage by remember { mutableStateOf(DockPage.Apps) }

    val appContext = LocalContext.current

    // Number-key shortcuts are handled only while the dock itself holds focus. When a text
    // field (app-drawer search bar, notification reply) owns focus, keys are delivered to that
    // field instead, so the dock never sees them and typed digits stay in the text field.
    val dockFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // Claim focus on first composition so number keys work without an initial tap, but never
        // steal it away later: if a text field takes focus we simply stop receiving events.
        runCatching { dockFocusRequester.requestFocus() }
    }

    val numberKeyModifier = Modifier
        .focusRequester(dockFocusRequester)
        .focusable()
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            handleDockNumberKey(
                event = event,
                currentPage = currentPage,
                apps = apps,
                onSelectPage = { currentPage = it },
                onOpenDrawer = onFabClick,
                onOpenNotificationShade = { openNotifications(appContext) },
                onAppClick = onAppClick,
                onMediaPlayPause = onMediaPlayPause,
                onMediaSkipNext = onMediaSkipNext,
            )
        }

    val dockAlpha by animateFloatAsState(
        targetValue = if (isAppDrawerVisible && hazeState != null) 0.4f else 1f,
        label = "dockAlpha",
        animationSpec = tween(500)
    )
    val fabAlpha by animateFloatAsState(
        targetValue = if (isAppDrawerVisible && hazeState != null) 0.6f else 1f,
        label = "fabAlpha",
        animationSpec = tween(500)
    )

    val baseDockColor = colorScheme.surfaceContainer
    val bottomPadding = rememberDockBottomPadding(dockSafeDrawIme, dockSafeDrawImePortraitOnly, dockSafeDrawImeHardwareKeyboardOnly)

    Row(
        modifier = modifier
            .width(finalMaxDockWidth)
            .padding(bottom = bottomPadding, start = LargestPadding, end = LargestPadding)
            .then(numberKeyModifier)
            .pointerInput(isAppDrawerVisible, onFabClick) {
                var totalVerticalDrag = 0f
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount -> totalVerticalDrag += dragAmount },
                    onDragEnd = {
                        if (totalVerticalDrag < -50f && !isAppDrawerVisible) onFabClick()
                        totalVerticalDrag = 0f
                    },
                    onDragCancel = { totalVerticalDrag = 0f }
                )
            }
            .pointerInput(Unit) {
                // Consume horizontal drags to prevent them from reaching the HorizontalPager
                // behind the dock. LazyRow children will still get them first.
                detectHorizontalDragGestures { _, _ -> }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Block touches */ },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (hideActionButton) {
            Spacer(Modifier.width(DockFabSize / 2))
        }

        Box(
            modifier = Modifier
                .height(DockHeight)
                .weight(1f)
                .graphicsLayer(clip = false)
                .then(if (hazeState == null) Modifier.shadow(MediumElevation, CircleShape) else Modifier)
                .clip(CircleShape)
                .then(
                    if (hazeState != null) {
                        Modifier.hazeEffect(state = hazeState, style = HazeMaterials.ultraThin())
                    } else Modifier
                )
                .background(baseDockColor.copy(alpha = dockAlpha))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = SmallPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MediumSpacer)
            ) {
                val statusStartPadding by animateDpAsState(
                    targetValue = if (currentPage == DockPage.Status) NoSpacing else MediumSpacing,
                    label = "statusStartPadding"
                )
                val mediaEndPadding by animateDpAsState(
                    targetValue = if (currentPage == DockPage.Media) NoSpacing else MediumSpacing,
                    label = "mediaEndPadding"
                )

                StatusSection(
                    isExpanded = currentPage == DockPage.Status,
                    onExpand = { currentPage = DockPage.Status },
                    notificationCount = notificationCount,
                    calendarEventCount = calendarEventCount,
                    currentTime = currentTime,
                    currentDate = currentDate,
                    weatherTemp = weatherTemp,
                    weatherCondition = weatherCondition,
                    progress = progress,
                    isCharging = isCharging,
                    onTimeClick = onTimeClick,
                    onDateClick = onDateClick,
                    onWeatherClick = onWeatherClick,
                    modifier = Modifier
                        .padding(start = statusStartPadding)
                        .then(if (currentPage == DockPage.Status) Modifier.weight(1f) else Modifier)
                        .animateContentSize()
                )

                AppsSection(
                    isExpanded = currentPage == DockPage.Apps,
                    onExpand = { currentPage = DockPage.Apps },
                    onOpenDrawer = onFabClick,
                    apps = apps,
                    notifications = notifications,
                    badgeType = badgeType,
                    onAppClick = onAppClick,
                    onPinApp = onPinApp,
                    onReorderApp = onReorderApp,
                    onUnpinApp = onUnpinApp,
                    isAppDrawerVisible = isAppDrawerVisible,
                    modifier = Modifier
                        .then(if (currentPage == DockPage.Apps) Modifier.weight(1f) else Modifier)
                        .animateContentSize()
                )

                MediaSection(
                    isExpanded = currentPage == DockPage.Media,
                    onExpand = { currentPage = DockPage.Media },
                    mediaState = mediaState,
                    isPermissionGranted = isMediaPermissionGranted,
                    onPlayPause = onMediaPlayPause,
                    onSkipNext = onMediaSkipNext,
                    onRequestPermission = onOpenMediaPermission,
                    modifier = Modifier
                        .padding(end = mediaEndPadding)
                        .then(if (currentPage == DockPage.Media) Modifier.weight(1f) else Modifier)
                        .animateContentSize()
                )
            }
        }

        if (!hideActionButton) {
            Spacer(Modifier.width(LargeMediumSpacer))

            DockFab(
                isAppDrawerVisible = isAppDrawerVisible,
                alpha = fabAlpha,
                hazeState = hazeState,
                singleTapAction = fabSingleTapAction,
                onClick = onFabClick,
                onDoubleTap = onFabDoubleTap,
                onLongPress = onFabLongPress,
                onSwipeUp = onFabSwipeUp
            )
        } else {
            Spacer(Modifier.width(DockFabSize / 2))
        }
    }
}

/* ---------------------------------------------------------------------- */
/* FAB                                                                     */
/* ---------------------------------------------------------------------- */

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun DockFab(
    isAppDrawerVisible: Boolean,
    alpha: Float,
    hazeState: HazeState?,
    singleTapAction: FabAction,
    onClick: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var verticalOffset by remember { mutableFloatStateOf(0f) }
    val animatedVerticalOffset by animateFloatAsState(
        targetValue = verticalOffset,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "fabBounce"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isAppDrawerVisible) LargestSpacing else (DockFabSize / 2),
        label = "fabCornerRadius",
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )
    val fabShape = RoundedCornerShape(cornerRadius)

    Surface(
        shape = fabShape,
        color = colorScheme.primary.copy(alpha = alpha),
        contentColor = colorScheme.onPrimary,
        tonalElevation = NoElevation,
        modifier = modifier
            .offset { IntOffset(0, animatedVerticalOffset.roundToInt()) }
            .size(DockFabSize)
            .graphicsLayer(clip = false)
            .then(if (hazeState == null) Modifier.shadow(MediumElevation, fabShape) else Modifier)
            .clip(fabShape)
            .pointerInput(onClick) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { onLongPress() }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount ->
                        // Only follow if dragging UP
                        if (dragAmount < 0 || verticalOffset < 0) {
                            verticalOffset = (verticalOffset + dragAmount).coerceIn(-40f, 0f)
                        }
                    },
                    onDragEnd = {
                        if (verticalOffset <= -20f) {
                            onSwipeUp()
                        }
                        verticalOffset = 0f
                    },
                    onDragCancel = {
                        verticalOffset = 0f
                    }
                )
            }
            .then(
                if (hazeState != null) {
                    Modifier.hazeEffect(state = hazeState, style = HazeMaterials.ultraThin())
                } else Modifier
            )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Crossfade(targetState = isAppDrawerVisible, label = "fabIconFade") { visible ->
                val icon = if (visible) {
                    Icons.Rounded.Close
                } else {
                    when (singleTapAction) {
                        FabAction.LOCK_DEVICE -> Icons.Rounded.Lock
                        FabAction.TRIGGER_ASSISTANT -> Icons.Rounded.Assistant
                        FabAction.OPEN_APP -> Icons.Rounded.RocketLaunch
                        FabAction.OPEN_LINK -> Icons.Rounded.Language
                        FabAction.TOGGLE_FLASHLIGHT -> Icons.Rounded.FlashlightOn
                        FabAction.OPEN_APP_DRAWER -> Icons.Rounded.Apps
                        else -> Icons.Rounded.TouchApp
                    }
                }
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.toggle_apps),
                    modifier = Modifier.size(BigSpacing)
                )
            }
        }
    }
}

/* ---------------------------------------------------------------------- */
/* Number-key dock shortcuts                                               */
/* ---------------------------------------------------------------------- */

/**
 * Handles a single number key (0-9) pressed while the dock holds focus and performs the action
 * mapped to the currently expanded panel.
 *
 * Keys are only consumed when they actually trigger something, so other handlers/typing keep
 * working for anything we do not act on. Returns true when the event was handled.
 */
private fun handleDockNumberKey(
    event: KeyEvent,
    currentPage: DockPage,
    apps: List<AppInfo>,
    onSelectPage: (DockPage) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenNotificationShade: () -> Unit,
    onAppClick: (String) -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaSkipNext: () -> Unit,
): Boolean {
    when (event.key) {
        Key.Zero -> {
            onOpenDrawer()
            return true
        }

        Key.One -> {
            when (currentPage) {
                DockPage.Status -> onOpenNotificationShade()
                DockPage.Apps, DockPage.Media -> onSelectPage(DockPage.Status)
            }
            return true
        }

        Key.Eight -> {
            when (currentPage) {
                DockPage.Media -> onMediaPlayPause()
                DockPage.Status, DockPage.Apps -> onSelectPage(DockPage.Media)
            }
            return true
        }

        Key.Nine -> {
            when (currentPage) {
                DockPage.Media -> onMediaSkipNext()
                DockPage.Status, DockPage.Apps -> onSelectPage(DockPage.Media)
            }
            return true
        }
    }

    // Keys 2-7: launch a pinned dock app when the Apps panel is open, otherwise switch panels.
    val appIndex = numberToAppIndex(event.key) ?: return false
    return when (currentPage) {
        DockPage.Apps -> {
            val app = apps.getOrNull(appIndex) ?: return false
            onAppClick(appKeyOf(app))
            true
        }

        DockPage.Status, DockPage.Media -> {
            onSelectPage(DockPage.Apps)
            true
        }
    }
}

/** Maps Key.Two..Key.Seven to a zero-based dock app index (0..5), or null for other keys. */
private fun numberToAppIndex(key: Key): Int? = when (key) {
    Key.Two -> 0
    Key.Three -> 1
    Key.Four -> 2
    Key.Five -> 3
    Key.Six -> 4
    Key.Seven -> 5
    else -> null
}

/** Matches the key used elsewhere when launching a dock app (see FixedAppSection). */
private fun appKeyOf(app: AppInfo): String =
    if (app.className.isNotEmpty()) "${app.packageName}/${app.className}" else app.packageName