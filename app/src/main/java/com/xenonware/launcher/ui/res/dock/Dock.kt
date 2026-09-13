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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
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

/* ---------------------------------------------------------------------- */
/* Shared dock values                                                      */
/* ---------------------------------------------------------------------- */

/** Which of the three sections currently owns the free space inside the pill. */
internal enum class DockPage { Status, Apps, Media }

internal val DockHeight = HugeBiggerSpacing
internal val DockFabSize = HugerSpacing
internal val DockCollapsedSectionWidth = BiggestSpacing

/**
 * Single source of truth for the section shape. Anything that draws a border or
 * outline for a section uses this shape instead of re-deriving corner radii.
 */
internal val DockSectionShape = RoundedCornerShape(MassiveCornerRadius)

/** Sections are translucent on dark backgrounds, opaque on light ones. */
@Composable
internal fun dockButtonAlpha(): Float = if (LocalIsDarkTheme.current) 0.35f else 1f

/**
 * The size behavior every section shares: full dock height minus the collapse
 * padding, stretching when expanded, a fixed 32.dp pill when not.
 */
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

/** Bottom inset for the dock, following the nav bar and (optionally) the IME. */
@Composable
private fun rememberDockBottomPadding(
    dockSafeDrawIme: Boolean,
    dockSafeDrawImePortraitOnly: Boolean = false
): Dp {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    val navPadding = WindowInsets.navigationBars
        .only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val imePadding = WindowInsets.ime
        .only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()

    val shouldMoveForIme = if (dockSafeDrawImePortraitOnly) {
        dockSafeDrawIme && !isLandscape
    } else {
        dockSafeDrawIme
    }

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

/* ---------------------------------------------------------------------- */
/* Layout                                                                  */
/* ---------------------------------------------------------------------- */

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
    hideActionButton: Boolean = false,
    fabSingleTapAction: FabAction = FabAction.OPEN_APP_DRAWER,
    onUnpinApp: (String) -> Unit = {},
    onPinApp: (String, Int) -> Unit = { _, _ -> },
    onReorderApp: (Int, Int) -> Unit = { _, _ -> },
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val finalMaxDockWidth = screenWidth.coerceAtMost(540.dp)

    var currentPage by remember { mutableStateOf(DockPage.Apps) }

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
    val bottomPadding = rememberDockBottomPadding(dockSafeDrawIme, dockSafeDrawImePortraitOnly)

    Row(
        modifier = modifier
            .width(finalMaxDockWidth)
            .padding(bottom = bottomPadding, start = LargestPadding, end = LargestPadding)
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