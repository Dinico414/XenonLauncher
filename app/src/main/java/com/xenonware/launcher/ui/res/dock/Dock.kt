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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import com.xenonware.launcher.R
import com.xenonware.launcher.media.MediaState
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.notification.LauncherNotification
import com.xenonware.launcher.ui.theme.LocalIsDarkTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/* ---------------------------------------------------------------------- */
/* Shared dock values                                                      */
/* ---------------------------------------------------------------------- */

/** Which of the three sections currently owns the free space inside the pill. */
internal enum class DockPage { Status, Apps, Media }

internal val DockHeight = 72.dp
internal val DockFabSize = 64.dp
internal val DockCollapsedSectionWidth = 32.dp

/**
 * Single source of truth for the section shape. Anything that draws a border or
 * outline for a section uses this shape instead of re-deriving corner radii.
 */
internal val DockSectionShape = RoundedCornerShape(100.dp)

/** Sections are translucent on dark backgrounds, opaque on light ones. */
@Composable
internal fun dockButtonAlpha(): Float = if (LocalIsDarkTheme.current) 0.35f else 1f

/**
 * The size behavior every section shares: full dock height minus the collapse
 * padding, stretching when expanded, a fixed 32.dp pill when not.
 */
@Composable
internal fun Modifier.dockSectionSize(isExpanded: Boolean): Modifier {
    val verticalPadding by animateDpAsState(
        targetValue = if (isExpanded) 4.dp else 12.dp,
        label = "dockSectionPadding"
    )
    return this
        .fillMaxHeight()
        .padding(vertical = verticalPadding)
        .then(
            if (isExpanded) Modifier.fillMaxWidth()
            else Modifier.requiredWidth(DockCollapsedSectionWidth)
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
    val target = if (safeDrawBottom < 16.dp) 16.dp else safeDrawBottom + 8.dp

    val animated by animateDpAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dockPaddingAnimation"
    )
    return animated.coerceAtLeast(0.dp)
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
    onSettingsClick: () -> Unit,
    onFabClick: () -> Unit,
    onFabDoubleTap: () -> Unit = {},
    onFabLongPress: () -> Unit = {},
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
            .padding(bottom = bottomPadding, start = 16.dp, end = 16.dp)
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
        Box(
            modifier = Modifier
                .height(DockHeight)
                .weight(1f)
                .graphicsLayer(clip = false)
                .then(if (hazeState == null) Modifier.shadow(8.dp, CircleShape) else Modifier)
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
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statusStartPadding by animateDpAsState(
                    targetValue = if (currentPage == DockPage.Status) 0.dp else 8.dp,
                    label = "statusStartPadding"
                )
                val mediaEndPadding by animateDpAsState(
                    targetValue = if (currentPage == DockPage.Media) 0.dp else 8.dp,
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
                    dockColor = baseDockColor.copy(alpha = dockAlpha),
                    modifier = Modifier
                        .padding(end = mediaEndPadding)
                        .then(if (currentPage == DockPage.Media) Modifier.weight(1f) else Modifier)
                        .animateContentSize()
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        DockFab(
            isAppDrawerVisible = isAppDrawerVisible,
            alpha = fabAlpha,
            hazeState = hazeState,
            onClick = onFabClick,
            onDoubleTap = onFabDoubleTap,
            onLongPress = onFabLongPress
        )
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
    onClick: () -> Unit,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fabShape = RoundedCornerShape(16.dp)

    Surface(
        shape = fabShape,
        color = colorScheme.primary.copy(alpha = alpha),
        contentColor = colorScheme.onPrimary,
        tonalElevation = 0.dp,
        modifier = modifier
            .size(DockFabSize)
            .graphicsLayer(clip = false)
            .then(if (hazeState == null) Modifier.shadow(8.dp, fabShape) else Modifier)
            .clip(fabShape)
            .pointerInput(onClick) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { onLongPress() }
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
                Icon(
                    if (visible) Icons.Rounded.Close else Icons.Rounded.Apps,
                    stringResource(R.string.toggle_apps),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}