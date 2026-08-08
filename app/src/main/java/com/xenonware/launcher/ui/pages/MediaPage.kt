package com.xenonware.launcher.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.xenonware.launcher.media.MediaState
import com.xenonware.launcher.util.blockHorizontalPagerSwipe
import com.xenonware.launcher.util.shouldDisableLandscapeLayout
import java.util.Locale
import kotlin.math.pow

@Composable
fun MediaPage(
    mediaState: MediaState,
    progress: Float,
    isPermissionGranted: Boolean,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onOpenSettings: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onOpenSource: () -> Unit,
) {
    val context = LocalContext.current
    val pm = remember { context.packageManager }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val disableLandscape = shouldDisableLandscapeLayout(context)
    val useLandscapeLayout = isLandscape && !disableLandscape

    val contentColor = if (isDarkTheme) Color.White else Color.Black
    val subContentColor = contentColor.copy(alpha = 0.7f)
    val overlayColor = if (isDarkTheme) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.45f)
    val surfaceAlpha = if (isDarkTheme) 0.5f else 0.8f
    val iconButtonContainerColor = if (isDarkTheme) Color.White else Color.Black
    val iconButtonContentColor = if (isDarkTheme) Color.Black else Color.White

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topPadding = if (statusBarHeight < 16.dp) {16.dp} else {statusBarHeight}
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val startPadding = safeDrawingPadding.calculateStartPadding(layoutDirection).coerceAtLeast(16.dp)
    val endPadding = safeDrawingPadding.calculateEndPadding(layoutDirection).coerceAtLeast(16.dp)
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 72dp (dock) + 8dp (dock padding) + 8dp (gap) + 4dp (to match widget vertical padding)
    val dockAreaHeight = 72.dp + navBarHeight + 8.dp + 8.dp + 4.dp

    val appName = remember(mediaState.packageName) {
        mediaState.packageName?.let {
            try {
                pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString()
            } catch (_: Exception) {
                null
            }
        } ?: "Media"
    }

    val appIcon = remember(mediaState.packageName) {
        mediaState.packageName?.let {
            try {
                pm.getApplicationIcon(it)
            } catch (_: Exception) {
                null
            }
        }
    }

    // Normalize progress for the background effects: 0.5f to 1.0f -> 0.0f to 1.0f
    val bgProgress = ((progress - 0.5f) * 2f).coerceIn(0.25f, 1f)
    val cornerProgress = ((progress - 0.85f) * 2f).coerceIn(0f, 1f)
    // Exponential corner radius: 24.dp to 0.dp
    // Using power of 3 for a more pronounced exponential curve
    val cornerRadius = (24 * (1f - cornerProgress).pow(3)).dp

    Box(modifier = Modifier
        .fillMaxSize()
    ) {
        // Background Album Art
        val artModel = remember(mediaState.title, mediaState.artist) {
            mediaState.albumArt ?: mediaState.albumArtUri
        }
        
        artModel?.let { model ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(bgProgress)
                    .clip(RoundedCornerShape(cornerRadius))
            ) {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(30.dp),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.tint(
                        MaterialTheme.colorScheme.inversePrimary.copy(alpha = if (isDarkTheme) 0.6f else 0.4f),
                        blendMode = BlendMode.SrcAtop
                    )
                )
                // Darken/Lighten the background for better readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(overlayColor)
                )
            }
        }

        if (useLandscapeLayout) {
            // Landscape Side-by-Side Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topPadding, bottom = dockAreaHeight)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Side: Album Art
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxSize(0.9f)
                            .clip(RoundedCornerShape(24.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = surfaceAlpha),
                        tonalElevation = 8.dp
                    ) {
                        if (artModel != null) {
                            AsyncImage(
                                model = artModel,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No Art",
                                    color = contentColor.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }

                // Right Side: Controls and Info
                Column(
                    modifier = Modifier.weight(1f).padding(end = endPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!isPermissionGranted) {
                        Text(
                            "Notification Access Required",
                            color = contentColor,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onOpenSettings) {
                            Text("Grant Access")
                        }
                    } else {
                        // App Name
                        Surface(
                            onClick = onOpenSource,
                            color = contentColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (appIcon != null) {
                                    AsyncImage(
                                        model = appIcon,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                                Text(
                                    text = appName,
                                    color = contentColor,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Info
                        Text(
                            text = mediaState.title ?: "Nothing Playing",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = mediaState.artist ?: "Unknown Artist",
                            style = MaterialTheme.typography.bodyLarge,
                            color = subContentColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.weight(1f))

                        // Progress Bar
                        var sliderPosition by remember { mutableStateOf<Float?>(null) }
                        val currentPosition = sliderPosition ?: mediaState.position.toFloat()
                        val duration = mediaState.duration.toFloat().coerceAtLeast(1f)

                        Column(modifier = Modifier.fillMaxWidth().blockHorizontalPagerSwipe() .padding(horizontal = 16.dp)) {
                            Slider(
                                value = currentPosition.coerceIn(0f, duration),
                                onValueChange = { sliderPosition = it },
                                onValueChangeFinished = {
                                    sliderPosition?.let { onSeek(it.toLong()) }
                                    sliderPosition = null
                                },
                                valueRange = 0f..duration,
                                colors = SliderDefaults.colors(
                                    thumbColor = contentColor,
                                    activeTrackColor = contentColor,
                                    inactiveTrackColor = contentColor.copy(alpha = 0.3f)
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    formatTime(currentPosition.toLong()),
                                    color = contentColor.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    formatTime(mediaState.duration),
                                    color = contentColor.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            IconButton(
                                onClick = onSkipPrevious,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.SkipPrevious,
                                    contentDescription = "Previous",
                                    modifier = Modifier.size(32.dp),
                                    tint = contentColor
                                )
                            }

                            FilledIconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier.size(64.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = iconButtonContainerColor,
                                    contentColor = iconButtonContentColor
                                )
                            ) {
                                Icon(
                                    if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            IconButton(
                                onClick = onSkipNext,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.SkipNext,
                                    contentDescription = "Next",
                                    modifier = Modifier.size(32.dp),
                                    tint = contentColor
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isPermissionGranted) {
                    Text(
                        "Notification Access Required",
                        color = contentColor,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "To show and control media playback, Xenon needs notification access.",
                        color = subContentColor,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onOpenSettings) {
                        Text("Grant Access")
                    }
                } else {
                    // Top App Info / Open Source Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            onClick = onOpenSource,
                            color = contentColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (appIcon != null) {
                                    AsyncImage(
                                        model = appIcon,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                }
                                Text(
                                    text = appName,
                                    color = contentColor,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Main Album Art
                    Surface(
                        modifier = Modifier
                            .size(280.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = surfaceAlpha),
                        tonalElevation = 8.dp
                    ) {
                        if (artModel != null) {
                            AsyncImage(
                                model = artModel,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No Art",
                                    color = contentColor.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // Info
                    Text(
                        text = mediaState.title ?: "Nothing Playing",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = mediaState.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodyLarge,
                        color = subContentColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Progress Bar
                    var sliderPosition by remember { mutableStateOf<Float?>(null) }
                    val currentPosition = sliderPosition ?: mediaState.position.toFloat()
                    val duration = mediaState.duration.toFloat().coerceAtLeast(1f)

                    Column(modifier = Modifier.fillMaxWidth().blockHorizontalPagerSwipe() .padding(horizontal = 16.dp)) {
                        Slider(
                            value = currentPosition.coerceIn(0f, duration),
                            onValueChange = { sliderPosition = it },
                            onValueChangeFinished = {
                                sliderPosition?.let { onSeek(it.toLong()) }
                                sliderPosition = null
                            },
                            valueRange = 0f..duration,
                            colors = SliderDefaults.colors(
                                thumbColor = contentColor,
                                activeTrackColor = contentColor,
                                inactiveTrackColor = contentColor.copy(alpha = 0.3f)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                formatTime(currentPosition.toLong()),
                                color = contentColor.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                formatTime(mediaState.duration),
                                color = contentColor.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        IconButton(
                            onClick = onSkipPrevious,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(32.dp),
                                tint = contentColor
                            )
                        }

                        FilledIconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = iconButtonContainerColor,
                                contentColor = iconButtonContentColor
                            )
                        ) {
                            Icon(
                                if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Rounded.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(32.dp),
                                tint = contentColor
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(dockAreaHeight))
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
