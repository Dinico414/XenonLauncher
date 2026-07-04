package com.xenonware.launcher.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.launcher.R
import com.xenonware.launcher.media.MediaState
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.ui.res.MenuItem
import com.xenonware.launcher.ui.res.XenonDropDown
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

// Custom Drag and Drop implementation for Launcher
class DragDropState {
    var draggedApp by mutableStateOf<AppInfo?>(null)
    var dragOffset by mutableStateOf(Offset.Zero)
    var isDragging by mutableStateOf(false)
    var sourceIndex by mutableIntStateOf(-1)
    var targetIndex by mutableIntStateOf(-1)
    
    // Position of the dock to detect drops
    var dockBounds by mutableStateOf(androidx.compose.ui.geometry.Rect.Zero)
    
    fun startDrag(app: AppInfo, offset: Offset, index: Int = -1) {
        draggedApp = app
        dragOffset = offset
        isDragging = true
        sourceIndex = index
        targetIndex = index
    }
    
    fun stopDrag() {
        draggedApp = null
        isDragging = false
        sourceIndex = -1
        targetIndex = -1
    }
}

val LocalDragDropState = staticCompositionLocalOf { DragDropState() }

@Composable
fun LauncherDragLayer(
    modifier: Modifier = Modifier,
    state: DragDropState = LocalDragDropState.current,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()
        
        if (state.isDragging && state.draggedApp != null) {
            val app = state.draggedApp!!
            val density = LocalDensity.current
            
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            state.dragOffset.x.roundToInt() - with(density) { 28.dp.toPx() }.roundToInt(),
                            state.dragOffset.y.roundToInt() - with(density) { 28.dp.toPx() }.roundToInt()
                        )
                    }
                    .size(56.dp)
                    .scale(1.1f)
                    .alpha(0.9f)
            ) {
                app.icon?.let { icon ->
                    Image(
                        bitmap = icon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun DockPill(
    modifier: Modifier = Modifier,
    apps: List<AppInfo>,
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
    onReorderApp: (Int, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val finalMaxDockWidth = (screenWidth).coerceAtMost(538.dp)
    
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
    
    Row(
        modifier = modifier
            .width(finalMaxDockWidth)
            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Block touches */ },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val baseDockColor = colorScheme.surfaceContainer
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
                // Status Section
                val statusStartPadding by animateDpAsState(
                    targetValue = if (currentPage == 0) 0.dp else 8.dp,
                    label = "statusStartPadding"
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = statusStartPadding)
                        .then(if (currentPage == 0) Modifier.weight(1f) else Modifier)
                        .animateContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val isExpanded = currentPage == 0
                    val verticalPadding by animateDpAsState(
                        targetValue = if (isExpanded) 4.dp else 12.dp,
                        label = "statusPadding"
                    )
                    val backgroundColor by animateColorAsState(
                        targetValue = when {
                            !isExpanded && notificationCount > 0 -> colorScheme.primaryContainer
                            else -> colorScheme.surfaceContainerLowest.copy(alpha = buttonAlpha)
                        },
                        label = "statusBg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = when {
                            !isExpanded && notificationCount > 0 -> colorScheme.onPrimaryContainer
                            else -> colorScheme.onSurface
                        },
                        label = "statusContent"
                    )

                    val strokeRotationProgress by animateFloatAsState(
                        targetValue = if (isExpanded) 1f else 0f,
                        animationSpec = tween(500),
                        label = "strokeRotationProgress"
                    )

                    val strokeWidth by animateDpAsState(
                        targetValue = if (isExpanded) 2.dp else 1.dp,
                        label = "strokeWidth"
                    )

                    val borderBrush = remember(strokeRotationProgress, progress) {
                        val strokeColor = when {
                            progress <= 0.15f -> Color.Red
                            progress <= 0.20f -> {
                                val fraction = (progress - 0.15f) / 0.05f
                                lerp(Color.Red, Color.Yellow, fraction)
                            }
                            progress <= 0.25f -> {
                                val fraction = (progress - 0.20f) / 0.05f
                                lerp(Color.Yellow, Color.Green, fraction)
                            }
                            else -> Color.Green
                        }
                        object : ShaderBrush() {
                            override fun createShader(size: Size): Shader {
                                val start = Offset(
                                    x = 0f,
                                    y = size.height * (1f - strokeRotationProgress)
                                )
                                val end = Offset(
                                    x = size.width * strokeRotationProgress,
                                    y = 0f
                                )
                                return LinearGradientShader(
                                    from = start,
                                    to = end,
                                    colors = listOf(strokeColor, strokeColor, Color.Transparent, Color.Transparent),
                                    colorStops = listOf(0.0f, progress, progress, 1.0f)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = { 
                            if (currentPage == 0) {
                                openNotifications(context)
                            } else {
                                currentPage = 0
                            }
                        },
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = verticalPadding)
                            .then(if (isExpanded) Modifier.fillMaxWidth() else Modifier.width(32.dp)),
                        shape = CircleShape,
                        color = backgroundColor,
                        contentColor = contentColor,
                        border = BorderStroke(strokeWidth, borderBrush)
                    ) {
                        AnimatedContent(
                            targetState = isExpanded,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                            },
                            label = "statusTransition"
                        ) { targetExpanded ->
                            if (targetExpanded) {
                                StatusSection(currentTime, currentDate, weatherTemp, weatherCondition, notificationCount)
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    if (notificationCount > 0) {
                                        Text(
                                            text = notificationCount.toString(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    } else {
                                        Icon(Icons.Rounded.Info, null, modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Apps Section
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .then(if (currentPage == 1) Modifier.weight(1f) else Modifier)
                        .animateContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val isExpanded = currentPage == 1
                    val verticalPadding by animateDpAsState(
                        targetValue = if (isExpanded) 4.dp else 12.dp,
                        label = "appsPadding"
                    )
                    val backgroundColor = colorScheme.surfaceContainerLowest.copy(alpha = buttonAlpha)
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
                        AnimatedContent(
                            targetState = isExpanded,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                            },
                            label = "appsTransition"
                        ) { targetExpanded ->
                            if (targetExpanded) {
                                FixedAppSection(apps, onAppClick, onPinApp, onReorderApp, onUnpinApp)
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.MoreHoriz, null, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }

                // Media Section
                val mediaEndPadding by animateDpAsState(
                    targetValue = if (currentPage == 2) 0.dp else 8.dp,
                    label = "mediaEndPadding"
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(end = mediaEndPadding)
                        .then(if (currentPage == 2) Modifier.weight(1f) else Modifier)
                        .animateContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val isExpanded = currentPage == 2
                    val verticalPadding by animateDpAsState(
                        targetValue = if (isExpanded) 4.dp else 12.dp,
                        label = "mediaPadding"
                    )
                    val backgroundColor = colorScheme.surfaceContainerLowest.copy(alpha = buttonAlpha)
                    val contentColor = colorScheme.onSurface

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
                        AnimatedContent(
                            targetState = isExpanded,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                            },
                            label = "mediaTransition"
                        ) { targetExpanded ->
                            if (targetExpanded) {
                                MediaSection(
                                    mediaState = mediaState,
                                    isPermissionGranted = isMediaPermissionGranted,
                                    onPlayPause = onMediaPlayPause,
                                    onSkipNext = onMediaSkipNext,
                                    onRequestPermission = onOpenMediaPermission
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Surface(
            onClick = onFabClick,
            shape = CircleShape,
            color = colorScheme.primary.copy(alpha = fabAlpha),
            contentColor = colorScheme.onPrimary,
            tonalElevation = 0.dp,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
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
                        "Toggle Apps",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

private fun openNotifications(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val expandMethod = statusBarManager.getMethod("expandNotificationsPanel")
        expandMethod.isAccessible = true
        expandMethod.invoke(statusBarService)
    } catch (_: Exception) {
        try {
            val intent = Intent("android.intent.action.SHOW_NOTIFICATIONS_PANEL")
            context.sendBroadcast(intent)
        } catch (_: Exception) {
        }
    }
}

private fun openMediaApp(context: Context, mediaState: MediaState) {
    val packageName = mediaState.packageName
    if (!packageName.isNullOrEmpty()) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            context.startActivity(intent)
            return
        }
    }
    
    try {
        val audioIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType("content://media/external/audio/media".toUri(), "audio/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooserIntent = Intent.createChooser(audioIntent, "SELECT AUDIO SOURCE")
        context.startActivity(chooserIntent)
    } catch (_: Exception) {
    }
}

@Composable
fun StatusSection(time: String, date: String, temperature: String, condition: String, notificationCount: Int) {
    val contentColor = LocalContentColor.current

    // Re-evaluated whenever the clock string changes so it flips at dusk/dawn.
    val isDay = remember(time) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        hour in 6..18 // 6:00 AM – 6:59 PM counts as day
    }

    val weatherRes = remember(condition, isDay) {
        val (day, night) = when {
            condition.contains("Thunder Shower", true) ||
                    condition.contains("T-Shower", true) ->
                R.drawable.tshower1 to R.drawable.tshower0

            condition.contains("Thunder", true) ||
                    condition.contains("Storm", true) ->
                R.drawable.tstorm1 to R.drawable.tstorm0

            condition.contains("Tornado", true) ->
                R.drawable.tornado1 to R.drawable.tornado0

            condition.contains("Hail", true) ->
                R.drawable.hail1 to R.drawable.hail0

            condition.contains("Sleet", true) ->
                R.drawable.sleet1 to R.drawable.sleet0

            condition.contains("Light Snow", true) ||
                    condition.contains("Flurr", true) ->
                R.drawable.lsnow1 to R.drawable.lsnow0

            condition.contains("Snow", true) ||
                    condition.contains("Ice", true) ->
                R.drawable.snow1 to R.drawable.snow0

            condition.contains("Shower", true) ||
                    condition.contains("Drizzle", true) ->
                R.drawable.shower1 to R.drawable.shower0

            condition.contains("Rain", true) ->
                R.drawable.rain1 to R.drawable.rain0

            condition.contains("Fog", true) ||
                    condition.contains("Mist", true) ||
                    condition.contains("Haze", true) ->
                R.drawable.fog1 to R.drawable.fog0

            condition.contains("Wind", true) ->
                R.drawable.windy1 to R.drawable.windy0

            condition.contains("Partly", true) ->
                R.drawable.pcloudy1 to R.drawable.pcloudy0

            condition.contains("Overcast", true) ||
                    condition.contains("Cloud", true) ->
                R.drawable.mcloudy1 to R.drawable.mcloudy0

            condition.contains("Clear", true) ||
                    condition.contains("Sunny", true) ->
                R.drawable.clear1 to R.drawable.clear0

            else ->
                R.drawable.unknown1 to R.drawable.unknown0
        }
        if (isDay) day else night
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxHeight()
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy((-6).dp, Alignment.CenterVertically)
        ) {
            Spacer(modifier = Modifier.height(9.dp))
            Text(time, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 16.sp, color = contentColor)
            Text(date, maxLines = 1, fontSize = 10.sp, color = contentColor.copy(alpha = 0.7f))
        }

        if (notificationCount > 0) {
            Surface(
                color = colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        notificationCount.toString(),
                        color = colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                // Shadow
                Image(
                    painter = painterResource(id = weatherRes),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.Black.copy(alpha = 0.1f)),
                    modifier = Modifier.size(26.dp)
                )
                // Real icon
                Image(
                    painter = painterResource(id = weatherRes),
                    contentDescription = condition,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(temperature.replace("+", ""), color = contentColor, maxLines = 1, fontSize = 14.sp)
        }
    }
}

@Composable
fun FixedAppSection(
    apps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    onPinApp: (String, Int) -> Unit,
    onReorderApp: (Int, Int) -> Unit,
    onUnpinApp: (String) -> Unit
) {
    val dragDropState = LocalDragDropState.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    
    val itemWidthPx = with(density) { 52.dp.toPx() }
    val contentPaddingPx = with(density) { 10.dp.toPx() }
    val spacingPx = with(density) { 8.dp.toPx() }

    // Unified target index calculation
    LaunchedEffect(dragDropState.isDragging, dragDropState.dragOffset, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (dragDropState.isDragging && dragDropState.dockBounds.contains(dragDropState.dragOffset)) {
            val relativeX = dragDropState.dragOffset.x - dragDropState.dockBounds.left
            val viewportWidth = dragDropState.dockBounds.width
            val baseSize = if (dragDropState.sourceIndex == -1) apps.size else apps.size - 1
            
            // Total width including padding and spacing
            val totalContentWidth = (baseSize + 1) * itemWidthPx - spacingPx + (contentPaddingPx * 2)
            val scrollPos = listState.firstVisibleItemIndex * itemWidthPx + listState.firstVisibleItemScrollOffset
            
            val contentStart = if (totalContentWidth < viewportWidth) {
                (viewportWidth - totalContentWidth) / 2f + contentPaddingPx
            } else {
                contentPaddingPx - scrollPos
            }
            
            val xInContent = relativeX - contentStart
            val newTarget = (xInContent / itemWidthPx).roundToInt().coerceIn(0, baseSize)
            
            if (dragDropState.targetIndex != newTarget) {
                dragDropState.targetIndex = newTarget
            }
        } else if (!dragDropState.isDragging) {
            dragDropState.targetIndex = -1
        }
    }

    // Elegant Auto-scroll logic
    LaunchedEffect(dragDropState.isDragging) {
        if (dragDropState.isDragging) {
            while (dragDropState.isDragging) {
                if (dragDropState.dockBounds.contains(dragDropState.dragOffset)) {
                    val viewportWidth = dragDropState.dockBounds.width
                    val dragX = dragDropState.dragOffset.x - dragDropState.dockBounds.left
                    val edgeThreshold = with(density) { 40.dp.toPx() }
                    
                    if (dragX < edgeThreshold && listState.canScrollBackward) {
                        val speed = ((edgeThreshold - dragX) / edgeThreshold * 15f).coerceIn(1f, 15f)
                        listState.scrollBy(-speed)
                    } else if (dragX > viewportWidth - edgeThreshold && listState.canScrollForward) {
                        val speed = ((dragX - (viewportWidth - edgeThreshold)) / edgeThreshold * 15f).coerceIn(1f, 15f)
                        listState.scrollBy(speed)
                    }
                }
                delay(16)
            }
        }
    }
    
    // Display list that handles the visual "push" during drag
    val displayApps = remember(apps, dragDropState.isDragging, dragDropState.targetIndex, dragDropState.sourceIndex) {
        if (!dragDropState.isDragging || dragDropState.targetIndex == -1) {
            apps
        } else {
            val list = apps.toMutableList()
            val draggedApp = dragDropState.draggedApp ?: return@remember apps
            
            list.removeAll { it.packageName == draggedApp.packageName }
            
            if (dragDropState.sourceIndex == -1 && list.size >= 6) return@remember apps
            
            val insertPos = dragDropState.targetIndex.coerceIn(0, list.size)
            list.add(insertPos, draggedApp)
            list
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .onGloballyPositioned { coordinates ->
                dragDropState.dockBounds = coordinates.positionInRoot().let { pos ->
                    androidx.compose.ui.geometry.Rect(pos, coordinates.size.toSize())
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (apps.isEmpty() && !dragDropState.isDragging) {
            Text(
                "Drag App to pin",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = LocalContentColor.current.copy(alpha = 0.5f)
            )
        } else {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                itemsIndexed(displayApps, key = { _, app -> app.packageName }) { index, app ->
                    var itemPos by remember { mutableStateOf(Offset.Zero) }
                    val isBeingDragged = dragDropState.isDragging && app == dragDropState.draggedApp

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .onGloballyPositioned { itemPos = it.positionInRoot() }
                            .animateItem(
                                placementSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                            )
                            .graphicsLayer {
                                alpha = if (isBeingDragged) 0f else 1f
                            }
                    ) {
                        app.icon?.let { icon ->
                            Image(
                                bitmap = icon.toBitmap().asImageBitmap(),
                                contentDescription = app.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .pointerInput(Unit) {
                                        detectTapGestures(onTap = { onAppClick(app.packageName) })
                                    }
                                    .pointerInput(Unit) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset ->
                                                val originalIndex = apps.indexOf(app)
                                                dragDropState.startDrag(app, itemPos + offset, originalIndex)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragDropState.dragOffset += dragAmount
                                            },
                                            onDragEnd = {
                                                val finalPos = dragDropState.dragOffset
                                                val sourceIdx = dragDropState.sourceIndex
                                                val targetIdx = dragDropState.targetIndex
                                                
                                                val verticalDist = if (finalPos.y < dragDropState.dockBounds.top) {
                                                    dragDropState.dockBounds.top - finalPos.y
                                                } else if (finalPos.y > dragDropState.dockBounds.bottom) {
                                                    finalPos.y - dragDropState.dockBounds.bottom
                                                } else 0f
                                                
                                                val unpinThreshold = with(density) { 80.dp.toPx() }
                                                val isOutside = !dragDropState.dockBounds.contains(finalPos) && verticalDist > unpinThreshold

                                                if (isOutside) {
                                                    if (sourceIdx != -1) {
                                                        onUnpinApp(app.packageName)
                                                    }
                                                } else {
                                                    if (sourceIdx == -1) {
                                                        if (targetIdx != -1) {
                                                            onPinApp(app.packageName, targetIdx)
                                                        }
                                                    } else if (targetIdx != -1 && targetIdx != sourceIdx) {
                                                        onReorderApp(sourceIdx, targetIdx)
                                                    }
                                                }
                                                dragDropState.stopDrag()
                                            },
                                            onDragCancel = { dragDropState.stopDrag() }
                                        )
                                    },
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaSection(
    mediaState: MediaState,
    isPermissionGranted: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val contentColor = LocalContentColor.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isPermissionGranted) {
            Text(
                "Media Access Required",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                fontSize = 12.sp,
                color = contentColor,
                maxLines = 1,
                softWrap = false
            )
            Button(
                onClick = onRequestPermission,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Grant", fontSize = 10.sp, maxLines = 1, softWrap = false)
            }
        } else {
            if (mediaState.albumArt != null) {
                Image(
                    bitmap = mediaState.albumArt.asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = colorScheme.onSurfaceVariant.copy(alpha = if (isSystemInDarkTheme()) 0.35f else 1f)
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        null,
                        tint = colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    mediaState.title ?: "No Media",
                    color = contentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    mediaState.artist ?: "Unknown Artist",
                    color = contentColor.copy(0.7f),
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            Row {
                IconButton(onClick = onPlayPause, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onSkipNext, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.SkipNext, null, tint = contentColor, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    containerColor: Color,
    onAppClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onDismiss: () -> Unit,
    onPinApp: (String, Int) -> Unit = { _, _ -> },
    isGridLayout: Boolean = true,
    onToggleLayout: () -> Unit = {},
    autoFocusSearch: Boolean = false,
    onToggleAutoFocus: () -> Unit = {}
) {
    val dragDropState = LocalDragDropState.current
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 640
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val hazeState = remember { HazeState() }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var barHeightPx by remember { mutableIntStateOf(0) }

    val isAtTop by remember(isGridLayout) {
        derivedStateOf {
            if (isGridLayout) {
                gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
            } else {
                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            }
        }
    }

    // Auto-focus search when scrolled to top
    LaunchedEffect(isAtTop, autoFocusSearch) {
        if (autoFocusSearch && isAtTop) {
            focusRequester.requestFocus()
            delay(100)
            keyboardController?.show()
        }
    }

    // Unfocus and hide keyboard when scrolling down if search is empty
    LaunchedEffect(isAtTop) {
        if (!isAtTop && searchQuery.isEmpty()) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    fun closeSearchOrDismiss() {
        if (isSearchFocused || searchQuery.isNotEmpty()) {
            searchQuery = ""
            focusManager.clearFocus()
            keyboardController?.hide()
        } else {
            onDismiss()
        }
    }


    val scope = rememberCoroutineScope()
    val backProgress = remember { Animatable(0f) }
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    PredictiveBackHandler(enabled = true) { progress ->
        try {
            progress.collect { backEvent ->
                val searchActive = isSearchFocused || searchQuery.isNotEmpty()
                // While searching, back just closes search — no dismiss preview.
                val eased = FastOutSlowInEasing.transform(backEvent.progress)
                backProgress.snapTo(if (searchActive) 0f else eased)
            }
            // Committed.
            if (isSearchFocused || searchQuery.isNotEmpty()) {
                searchQuery = ""
                focusManager.clearFocus()
                keyboardController?.hide()
            } else {
                currentOnDismiss()
            }
        } catch (_: CancellationException) {
            scope.launch { backProgress.animateTo(0f, tween(220)) }
        }
    }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var sheetHeightPx by remember { mutableIntStateOf(0) }
    val dismissThresholdPx = with(density) { 120.dp.toPx() }
    val flingDismissVelocity = 1200f

    val sheetDragConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && offsetY > 0f) {
                    val consume = available.y.coerceAtLeast(-offsetY)
                    offsetY += consume
                    return Offset(0f, consume)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    offsetY += available.y
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offsetY > 0f) {
                    val target = if (sheetHeightPx > 0) sheetHeightPx.toFloat() else offsetY + 1000f
                    if (offsetY > dismissThresholdPx || available.y > flingDismissVelocity) {
                        animate(offsetY, target, animationSpec = tween(220)) { v, _ -> offsetY = v }
                        currentOnDismiss()
                    } else {
                        animate(offsetY, 0f) { v, _ -> offsetY = v }
                    }
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    val p = backProgress.value
                    val s = 1f - 0.08f * p
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    translationY = offsetY + 24.dp.toPx() * p
                }
                .onSizeChanged { sheetHeightPx = it.height }
                .statusBarsPadding()
                .then(
                    if (isWideScreen) {
                        Modifier
                            .padding(horizontal = 56.dp)
                            .widthIn(max = 640.dp)
                            .fillMaxHeight()
                    } else {
                        Modifier.fillMaxSize()
                    }
                )
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = containerColor,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.height(16.dp))

                // Drag handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            CircleShape
                        )
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(16.dp))

                // Bar floats over the scrolling list; the list is hazed where it passes behind it.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))

                ) {
                    val contentTopPadding =
                        if (barHeightPx > 0) with(density) { barHeightPx.toDp() } + 16.dp else 64.dp

                    if (isGridLayout) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (isWideScreen) 6 else 4),
                            state = gridState,
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(sheetDragConnection)
                                .hazeSource(hazeState),
                            contentPadding = PaddingValues(
                                top = contentTopPadding,
                                bottom = 120.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredApps) { app ->
                                var itemPos by remember { mutableStateOf(Offset.Zero) }
                                
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .onGloballyPositioned { itemPos = it.positionInRoot() }
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = {
                                                    onAppClick(app.packageName)
                                                    onDismiss()
                                                }
                                            )
                                        }
                                        .pointerInput(Unit) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { offset ->
                                                    dragDropState.startDrag(app, itemPos + offset)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragDropState.dragOffset += dragAmount
                                                    
                                                    if (dragDropState.dockBounds.contains(dragDropState.dragOffset)) {
                                                        val relativeX = dragDropState.dragOffset.x - dragDropState.dockBounds.left
                                                        val itemWidth = with(density) { 52.dp.toPx() }
                                                        dragDropState.targetIndex = (relativeX / itemWidth).toInt().coerceIn(0, 100) // 100 is just a safe upper bound
                                                    } else {
                                                        dragDropState.targetIndex = -1
                                                    }
                                                },
                                                onDragEnd = {
                                                    val finalPos = dragDropState.dragOffset
                                                    val verticalDist = if (finalPos.y < dragDropState.dockBounds.top) {
                                                        dragDropState.dockBounds.top - finalPos.y
                                                    } else if (finalPos.y > dragDropState.dockBounds.bottom) {
                                                        finalPos.y - dragDropState.dockBounds.bottom
                                                    } else 0f
                                                    
                                                    val hitThreshold = with(density) { 80.dp.toPx() }
                                                    
                                                    if (dragDropState.dockBounds.contains(finalPos) || verticalDist < hitThreshold) {
                                                        onPinApp(app.packageName, dragDropState.targetIndex)
                                                    }
                                                    dragDropState.stopDrag()
                                                },
                                                onDragCancel = { dragDropState.stopDrag() }
                                            )
                                        }
                                ) {
                                    app.icon?.let { icon ->
                                        Image(
                                            bitmap = icon.toBitmap().asImageBitmap(),
                                            contentDescription = app.name,
                                            modifier = Modifier.size(56.dp)
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        app.name,
                                        color = colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(sheetDragConnection)
                                .hazeSource(hazeState),
                            contentPadding = PaddingValues(
                                top = contentTopPadding,
                                bottom = 120.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredApps) { app ->
                                var itemPos by remember { mutableStateOf(Offset.Zero) }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .onGloballyPositioned { itemPos = it.positionInRoot() }
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = {
                                                    onAppClick(app.packageName)
                                                    onDismiss()
                                                }
                                            )
                                        }
                                        .pointerInput(Unit) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { offset ->
                                                    dragDropState.startDrag(app, itemPos + offset)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragDropState.dragOffset += dragAmount
                                                    
                                                    if (dragDropState.dockBounds.contains(dragDropState.dragOffset)) {
                                                        val relativeX = dragDropState.dragOffset.x - dragDropState.dockBounds.left
                                                        val itemWidth = with(density) { 52.dp.toPx() }
                                                        dragDropState.targetIndex = (relativeX / itemWidth).toInt().coerceIn(0, 100) // 100 is just a safe upper bound
                                                    } else {
                                                        dragDropState.targetIndex = -1
                                                    }
                                                },
                                                onDragEnd = {
                                                    val finalPos = dragDropState.dragOffset
                                                    val verticalDist = if (finalPos.y < dragDropState.dockBounds.top) {
                                                        dragDropState.dockBounds.top - finalPos.y
                                                    } else if (finalPos.y > dragDropState.dockBounds.bottom) {
                                                        finalPos.y - dragDropState.dockBounds.bottom
                                                    } else 0f
                                                    
                                                    val hitThreshold = with(density) { 80.dp.toPx() }
                                                    
                                                    if (dragDropState.dockBounds.contains(finalPos) || verticalDist < hitThreshold) {
                                                        onPinApp(app.packageName, dragDropState.targetIndex)
                                                    }
                                                    dragDropState.stopDrag()
                                                },
                                                onDragCancel = { dragDropState.stopDrag() }
                                            )
                                        }
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    app.icon?.let { icon ->
                                        Image(
                                            bitmap = icon.toBitmap().asImageBitmap(),
                                            contentDescription = app.name,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        app.name,
                                        color = colorScheme.onSurface,
                                        fontSize = 16.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    if (filteredApps.isEmpty() && searchQuery.isNotBlank()) {
                        Text(
                            "No apps found",
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .onSizeChanged { barHeightPx = it.height }
                            .clip(RoundedCornerShape(100f))
                            .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin())
                            .background(colorScheme.surfaceContainer.copy(alpha = 0.4f)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { closeSearchOrDismiss() }, Modifier.padding(4.dp)) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                tint = colorScheme.onSurface,
                                contentDescription = if (isSearchFocused || searchQuery.isNotEmpty())
                                    "Close search" else "Close"
                            )
                        }

                        val textStyle = typography.titleLarge.merge(
                            TextStyle(fontFamily = QuicksandTitleVariable, textAlign = TextAlign.Center, color = colorScheme.onSurface)
                        )

                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .onFocusChanged { isSearchFocused = it.isFocused },
                            singleLine = true,
                            textStyle = textStyle,
                            cursorBrush = SolidColor(colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    if (searchQuery.isEmpty() && !isSearchFocused) {
                                        Text(
                                            text = "Search",
                                            style = textStyle,
                                            color = colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        Box {
                            IconButton(onClick = { showMenu = !showMenu }, modifier = Modifier.padding(4.dp)) {
                                Icon(Icons.Rounded.MoreVert, tint = colorScheme.onSurface, contentDescription = "More options")
                            }
                            XenonDropDown(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                items = listOf(
                                    MenuItem(
                                        text = if (isGridLayout) "List view" else "Grid view",
                                        onClick = onToggleLayout,
                                        dismissOnClick = true,
                                        leadingIcon = {
                                            Icon(
                                                if (isGridLayout) Icons.AutoMirrored.Rounded.ViewList
                                                else Icons.Rounded.GridView,
                                                contentDescription = "Toggle layout"
                                            )
                                        }
                                    ),
                                    MenuItem(
                                        text = "Auto focus keyboard",
                                        onClick = onToggleAutoFocus,
                                        dismissOnClick = false,
                                        trailingIcon = {
                                            androidx.compose.material3.Switch(
                                                checked = autoFocusSearch,
                                                onCheckedChange = null,
                                                modifier = Modifier.scale(0.7f)
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.Keyboard,
                                                contentDescription = null
                                            )
                                        }
                                    ),
                                    MenuItem(
                                        text = "Settings",
                                        onClick = { onSettingsClick() },
                                        dismissOnClick = true,
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.Settings,
                                                contentDescription = "Settings"
                                            )
                                        }
                                    )
                                ),
                                hazeState = hazeState
                            )
                        }
                    }
                }
            }
        }
    }
}
