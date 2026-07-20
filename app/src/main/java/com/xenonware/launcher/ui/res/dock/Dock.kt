package com.xenonware.launcher.ui.res.dock

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.get
import androidx.core.graphics.scale
import com.xenonware.launcher.accessibility.XenonAccessibilityService
import com.xenonware.launcher.media.MediaState
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.notification.LauncherNotification
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

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
    currentTime: String,
    currentDate: String,
    weatherTemp: String,
    weatherCondition: String,
    onAppClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onFabClick: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaSkipNext: () -> Unit,
    onOpenMediaPermission: () -> Unit,
    isAppDrawerVisible: Boolean = false,
    hazeState: HazeState? = null,
    progress: Float = 1f,
    onUnpinApp: (String) -> Unit = {},
    onPinApp: (String, Int) -> Unit = { _, _ -> },
    onReorderApp: (Int, Int) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val finalMaxDockWidth = (screenWidth).coerceAtMost(540.dp)

    var currentPage by remember { mutableIntStateOf(1) }
    val dockAlpha by animateFloatAsState(
        targetValue = if (isAppDrawerVisible) 0.4f else 1f,
        label = "dockAlpha",
        animationSpec = tween(500)
    )
    val fabAlpha by animateFloatAsState(
        targetValue = if (isAppDrawerVisible) 0.6f else 1f,
        label = "dockAlpha",
        animationSpec = tween(500)
    )
    val buttonAlpha = if (isSystemInDarkTheme()) 0.35f else 1f

    val surfaceContainerLowest = colorScheme.surfaceContainerLowest
    val onSurface = colorScheme.onSurface

    val infiniteTransition = rememberInfiniteTransition(label = "musicNoteAnim")
    val musicNoteRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "musicNoteRotation"
    )

    val musicNoteScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "musicNoteScale"
    )

    val musicNotePlayingFactor by animateFloatAsState(
        targetValue = if (mediaState.isPlaying) 1f else 0f,
        label = "musicNotePlayingFactor"
    )

    val albumArt = mediaState.albumArt
    val isDark = isSystemInDarkTheme()
    val currentColorScheme = colorScheme

    val defaultTheme = remember(currentColorScheme, surfaceContainerLowest, onSurface) {
        Triple(surfaceContainerLowest, onSurface, currentColorScheme.primaryContainer)
    }

    var mediaThemeBase by remember { mutableStateOf(defaultTheme) }

    LaunchedEffect(albumArt, isDark, currentColorScheme) {
        if (albumArt != null) {
            try {
                val scaled = albumArt.scale(1, 1)
                val colorInt = scaled[0, 0]
                scaled.recycle()
                val seed = Color(colorInt)

                val bg = if (isDark) {
                    lerp(seed, surfaceContainerLowest, 0.52f)
                } else {
                    lerp(seed, surfaceContainerLowest, 0.95f)
                }

                val text = if (isDark) {
                    lerp(seed, onSurface, 0.85f)
                } else {
                    lerp(seed, onSurface, 0.7f)
                }

                val pc = if (isDark) {
                    lerp(seed, Color.Black, 0.3f).copy(alpha = 0.6f)
                } else {
                    lerp(seed, Color.White, 0.15f).copy(alpha = 0.3f)
                }

                mediaThemeBase = Triple(bg, text, pc)
            } catch (_: Exception) {
                mediaThemeBase = defaultTheme
            }
        } else {
            // Debounce returning to default theme to prevent flickering during track changes
            delay(500.milliseconds)
            mediaThemeBase = defaultTheme
        }
    }

    val animatedBg by animateColorAsState(targetValue = mediaThemeBase.first, label = "mediaBg", animationSpec = tween(500))
    val animatedText by animateColorAsState(targetValue = mediaThemeBase.second, label = "mediaText", animationSpec = tween(500))
    val animatedPc by animateColorAsState(targetValue = mediaThemeBase.third, label = "mediaPc", animationSpec = tween(500))

    val mediaTheme = remember(animatedBg, animatedText, animatedPc, currentColorScheme) {
        Triple(
            animatedBg,
            animatedText,
            currentColorScheme.copy(
                primaryContainer = animatedPc,
                onPrimaryContainer = animatedText,
                onSurface = animatedText
            )
        )
    }

    val baseDockColor = colorScheme.surfaceContainer
    val safeDrawBottom = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
    val bottomPadding = if (safeDrawBottom < 16.dp) {16.dp} else {safeDrawBottom + 8.dp}

    Row(
        modifier = modifier
            .width(finalMaxDockWidth)
            .padding(bottom = bottomPadding, start = 16.dp, end = 16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() }, indication = null
            ) { /* Block touches */ },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .height(72.dp)
                .weight(1f)
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
                    targetValue = if (currentPage == 0) 0.dp else 8.dp, label = "statusStartPadding"
                )
                // Status Section
                StatusSection(
                    isExpanded = currentPage == 0,
                    notificationCount = notificationCount,
                    currentTime = currentTime,
                    currentDate = currentDate,
                    weatherTemp = weatherTemp,
                    weatherCondition = weatherCondition,
                    progress = progress,
                    buttonAlpha = buttonAlpha,
                    onExpand = { currentPage = 0 },
                    onClickExpanded = { openNotifications(context) },
                    modifier = Modifier
                        .padding(start = statusStartPadding)
                        .then(if (currentPage == 0) Modifier.weight(1f) else Modifier)
                        .animateContentSize()
                )

                // Apps Section
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .then(if (currentPage == 1) Modifier.weight(1f) else Modifier)
                        .animateContentSize(), contentAlignment = Alignment.Center
                ) {
                    val isExpanded = currentPage == 1
                    val verticalPadding by animateDpAsState(
                        targetValue = if (isExpanded) 4.dp else 12.dp, label = "appsPadding"
                    )
                    val backgroundColor =
                        colorScheme.surfaceContainerLowest.copy(alpha = buttonAlpha)
                    val contentColor = colorScheme.onSurface

                    Surface(
                        onClick = {
                            if (currentPage == 1) {
                                onFabClick()
                            } else {
                                currentPage = 1
                            }
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = verticalPadding)
                            .then(if (isExpanded) Modifier.fillMaxWidth() else Modifier.width(32.dp)),
                        shape = CircleShape,
                        color = backgroundColor,
                        contentColor = contentColor
                    ) {
                        if (isExpanded) {
                            FixedAppSection(
                                apps,
                                notifications,
                                badgeType,
                                onAppClick,
                                onPinApp,
                                onReorderApp,
                                onUnpinApp
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.MoreHoriz,
                                    null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Media Section
                val mediaEndPadding by animateDpAsState(
                    targetValue = if (currentPage == 2) 0.dp else 8.dp, label = "mediaEndPadding"
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = mediaEndPadding)
                        .then(if (currentPage == 2) Modifier.weight(1f) else Modifier)
                        .animateContentSize(), contentAlignment = Alignment.Center
                ) {
                    val isExpanded = currentPage == 2
                    val verticalPadding by animateDpAsState(
                        targetValue = if (isExpanded) 4.dp else 12.dp, label = "mediaPadding"
                    )

                    val backgroundColor = animatedBg.copy(alpha = buttonAlpha)
                    val contentColor = animatedText
                    val localScheme = mediaTheme.third

                    Surface(
                        onClick = {
                            if (currentPage == 2) {
                                openMediaApp(context, mediaState)
                            } else {
                                currentPage = 2
                            }
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = verticalPadding)
                            .then(if (isExpanded) Modifier.fillMaxWidth() else Modifier.width(32.dp)),
                        shape = CircleShape,
                        color = backgroundColor,
                        contentColor = contentColor
                    ) {
                        androidx.compose.material3.MaterialTheme(colorScheme = localScheme) {
                            if (isExpanded) {
                                MediaSection(
                                    mediaState = mediaState,
                                    isPermissionGranted = isMediaPermissionGranted,
                                    onPlayPause = onMediaPlayPause,
                                    onSkipNext = onMediaSkipNext,
                                    onRequestPermission = onOpenMediaPermission,
                                    musicNoteRotation = { musicNoteRotation },
                                    musicNoteScale = { musicNoteScale },
                                    musicNotePlayingFactor = { musicNotePlayingFactor }
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.MusicNote,
                                        null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .graphicsLayer {
                                                rotationZ = musicNoteRotation * musicNotePlayingFactor
                                                scaleX = 1f + (musicNoteScale - 1f) * musicNotePlayingFactor
                                                scaleY = 1f + (musicNoteScale - 1f) * musicNotePlayingFactor
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        val fabShape = RoundedCornerShape(16.dp)
        Surface(
            shape = fabShape,
            color = colorScheme.primary.copy(alpha = fabAlpha),
            contentColor = colorScheme.onPrimary,
            tonalElevation = 0.dp,
            modifier = Modifier
                .size(64.dp)
                .clip(fabShape)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onFabClick() }, onDoubleTap = {
                        val service = XenonAccessibilityService.instance
                        if (service != null) {
                            service.lockScreen()
                        } else {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    })
                }
                .then(
                    if (hazeState != null) {
                        Modifier.hazeEffect(state = hazeState, style = HazeMaterials.ultraThin())
                    } else Modifier
                )) {
            Box(contentAlignment = Alignment.Center) {
                Crossfade(targetState = isAppDrawerVisible, label = "fabIconFade") { visible ->
                    Icon(
                        if (visible) Icons.Rounded.Close else Icons.Rounded.Apps,
                        "Toggle Apps",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
