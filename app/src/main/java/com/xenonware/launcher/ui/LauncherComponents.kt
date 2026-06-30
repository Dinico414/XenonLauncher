package com.xenonware.launcher.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.xenonware.launcher.R
import com.xenonware.launcher.media.MediaState
import com.xenonware.launcher.model.AppInfo
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import java.util.Calendar

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
    progress: Float = 1f
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
        val baseDockColor = MaterialTheme.colorScheme.surfaceContainer
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
                            !isExpanded && notificationCount > 0 -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = buttonAlpha)
                        },
                        label = "statusBg"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = when {
                            !isExpanded && notificationCount > 0 -> MaterialTheme.colorScheme.onPrimary
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        label = "statusContent"
                    )

                    val borderBrush = remember(isExpanded, progress) {
                        if (isExpanded) {
                            Brush.horizontalGradient(
                                0.0f to Color.Green,
                                progress to Color.Green,
                                progress to Color.Transparent,
                                1.0f to Color.Transparent
                            )
                        } else {
                            Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                (1f - progress) to Color.Transparent,
                                (1f - progress) to Color.Green,
                                1.0f to Color.Green
                            )
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
                        border = BorderStroke(1.dp, borderBrush)
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
                    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = buttonAlpha)
                    val contentColor = MaterialTheme.colorScheme.onSurface

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
                                FixedAppSection(apps, onAppClick)
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
                    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = buttonAlpha)
                    val contentColor = MaterialTheme.colorScheme.onSurface

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
            color = MaterialTheme.colorScheme.primary.copy(alpha = fabAlpha),
            contentColor = MaterialTheme.colorScheme.onPrimary,
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
                Icon(if (isAppDrawerVisible) Icons.Rounded.Close else Icons.Rounded.Apps, "Toggle Apps", modifier = Modifier.size(32.dp))
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
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        notificationCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
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
fun FixedAppSection(apps: List<AppInfo>, onAppClick: (String) -> Unit) {
    LazyRow(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 10.dp)
    ) {
        items(apps.take(6)) { app ->
            app.icon?.let { icon ->
                Image(
                    bitmap = icon.toBitmap().asImageBitmap(),
                    contentDescription = app.name,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onAppClick(app.packageName) },
                    contentScale = ContentScale.Fit
                )
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isSystemInDarkTheme()) 0.35f else 1f)
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface,
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

@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    containerColor: Color,
    onAppClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 640

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
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
                
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            CircleShape
                        )
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Applications", 
                        color = MaterialTheme.colorScheme.onSurface, 
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        softWrap = false
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (isWideScreen) 6 else 4),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(apps) { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { 
                                onAppClick(app.packageName)
                                onDismiss()
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
                                color = MaterialTheme.colorScheme.onSurface, 
                                fontSize = 12.sp, 
                                maxLines = 1,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
