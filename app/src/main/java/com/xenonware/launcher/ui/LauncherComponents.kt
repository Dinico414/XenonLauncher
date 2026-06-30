package com.xenonware.launcher.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.media.MediaState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun DockPill(
    modifier: Modifier = Modifier,
    apps: List<AppInfo>,
    mediaState: MediaState,
    isMediaPermissionGranted: Boolean,
    currentTime: String,
    currentDate: String,
    weatherTemp: String,
    onAppClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onFabClick: () -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaSkipNext: () -> Unit,
    onOpenMediaPermission: () -> Unit,
    isAppDrawerVisible: Boolean = false,
    hazeState: HazeState? = null
) {
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
            .fillMaxWidth()
            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
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
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .then(if (currentPage == 0) Modifier.weight(1f) else Modifier)
                        .animateContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentPage == 0) StatusSection(currentTime, currentDate, weatherTemp) 
                    else Surface(
                        onClick = { currentPage = 0 },
                        modifier = Modifier.size(width = 32.dp, height = 40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = buttonAlpha),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Info, null, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .then(if (currentPage == 1) Modifier.weight(1f) else Modifier)
                        .animateContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentPage == 1) FixedAppSection(apps, onAppClick)
                    else Surface(
                        onClick = { currentPage = 1 },
                        modifier = Modifier.size(width = 32.dp, height = 40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = buttonAlpha),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Apps, null, modifier = Modifier.size(24.dp))
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .then(if (currentPage == 2) Modifier.weight(1f) else Modifier)
                        .animateContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentPage == 2) MediaSection(
                        mediaState = mediaState,
                        isPermissionGranted = isMediaPermissionGranted,
                        onPlayPause = onMediaPlayPause,
                        onSkipNext = onMediaSkipNext,
                        onRequestPermission = onOpenMediaPermission
                    )
                    else Surface(
                        onClick = { currentPage = 2 },
                        modifier = Modifier.size(width = 32.dp, height = 40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = buttonAlpha),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(24.dp))
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
                Icon(if (isAppDrawerVisible) Icons.Default.Close else Icons.Default.Add, "Toggle Apps", modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun StatusSection(time: String, date: String, temperature: String) {
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(time, fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 16.sp, color = contentColor)
            Text(date, maxLines = 1, fontSize = 10.sp, color = contentColor.copy(alpha = 0.7f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WbSunny, null, tint = Color.Yellow, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(temperature, color = contentColor, maxLines = 1, fontSize = 14.sp)
        }
    }
}

@Composable
fun FixedAppSection(apps: List<AppInfo>, onAppClick: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(apps.take(8)) { app ->
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
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isPermissionGranted) {
            Text(
                "Media Access Required",
                modifier = Modifier.weight(1f).padding(start = 8.dp),
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
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isSystemInDarkTheme()) 0.35f else 1f)
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(9.dp)
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
                        if (mediaState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onSkipNext, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.SkipNext, null, tint = contentColor, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.2f))
            .clickable(onClick = onDismiss)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .fillMaxHeight()
                .align(Alignment.BottomCenter)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
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
                    columns = GridCells.Fixed(4),
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
