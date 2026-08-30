package com.xenonware.launcher.ui.pages

import androidx.compose.animation.core.EaseInOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
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
import androidx.compose.material3.MaterialTheme.colorScheme
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import coil.compose.AsyncImage
import com.xenonware.launcher.R
import com.xenonware.launcher.media.MediaAction
import com.xenonware.launcher.media.MediaState
import com.xenonware.launcher.ui.theme.LocalIsDarkTheme
import com.xenonware.launcher.util.blockHorizontalPagerSwipe
import com.xenonware.launcher.util.isSmallScreenDevice
import com.xenonware.launcher.util.shouldDisableLandscapeLayout
import com.xenonware.launcher.viewmodel.LauncherViewModel
import java.util.Locale

@Composable
fun MediaPage(
    mediaState: MediaState,
    progress: Float,
    isPermissionGranted: Boolean,
    isDarkTheme: Boolean = LocalIsDarkTheme.current,
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
    val isLandscape =
        configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val disableLandscape = shouldDisableLandscapeLayout(context)
    val useLandscapeLayout = isLandscape && !disableLandscape

    val contentColor = colorScheme.onSurface
    val subContentColor = contentColor.copy(alpha = 0.7f)
    val overlayColor =
        if (isDarkTheme) Color.Black.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.45f)
    val iconButtonContainerColor = colorScheme.onSurface
    val iconButtonContentColor = colorScheme.surface

    val artModel = remember(mediaState.title, mediaState.artist) {
        mediaState.albumArt ?: mediaState.albumArtUri
    }

    val surfaceAlpha = if (artModel != null) {
        if (isDarkTheme) 0.5f else 0.8f
    } else {
        if (isDarkTheme) 0.15f else 0.3f
    }

    val note = rememberMusicNoteAnimation(mediaState.isPlaying)

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topPadding = if (statusBarHeight < 16.dp) {
        16.dp
    } else {
        statusBarHeight
    }
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val layoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val startPadding =
        safeDrawingPadding.calculateStartPadding(layoutDirection).coerceAtLeast(16.dp)
    val endPadding = safeDrawingPadding.calculateEndPadding(layoutDirection).coerceAtLeast(16.dp)
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 72dp (dock) + 8dp (dock padding) + 8dp (gap) + 4dp (to match widget vertical padding)
    val dockAreaHeight = 72.dp + navBarHeight + 8.dp + 8.dp + 4.dp

    val appNameLabel = stringResource(R.string.media)
    val appName = remember(mediaState.packageName, appNameLabel) {
        mediaState.packageName?.let {
            try {
                pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString()
            } catch (_: Exception) {
                null
            }
        } ?: appNameLabel
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

    val isPhone = configuration.screenWidthDp < 600
    val leftAction = mediaState.actions.getOrNull(0)
    val rightAction = mediaState.actions.getOrNull(1)
    val extraActions = if (isPhone) emptyList() else mediaState.actions.drop(2)

    val isSmallDevice = isSmallScreenDevice(context)
    val verticalSafeDrawHeight = (configuration.screenHeightDp.dp - topPadding - navBarHeight).coerceAtLeast(40.dp)
    val maxSmallCoverSize = minOf(configuration.screenWidthDp.dp / 3, verticalSafeDrawHeight/3)
    val portraitAlbumArtSize = if (isSmallDevice) maxSmallCoverSize else 280.dp

    // Constant background effects (no animation)
    val bgProgress = 1f
    
    // Progressive corner radius: 40.dp to 0.dp after 75% swipe
    // progress is 1.0 when on MediaPage, decreases as we swipe away (0.0 when on NotificationPage)
    // We want 40.dp at progress 0.75 and 0.dp at progress 1.0
    val normalizedProgress = ((progress - 0.75f) * 4f).coerceIn(0f, 1f)
    val easedProgress = EaseInOut.transform(normalizedProgress)
    val cornerRadius = (40 * (1f - easedProgress)).dp
    val baseBgAlpha = if (isDarkTheme) 0.6f else 0.4f
    val backgroundTint = colorScheme.inversePrimary.copy(alpha = baseBgAlpha)

    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.3f), offset = Offset(0f, 2f), blurRadius = 4f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundTint.copy(alpha = baseBgAlpha * bgProgress))
    ) {
        // Background Album Art
        artModel?.let { model ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(bgProgress)
            ) {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(30.dp),
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.tint(
                        backgroundTint, blendMode = BlendMode.SrcAtop
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
                            .sizeIn(maxWidth = 400.dp)
                            .aspectRatio(1f)
                            .fillMaxSize(0.9f)
                            .clip(RoundedCornerShape(24.dp)),
                        color = colorScheme.surfaceVariant.copy(alpha = surfaceAlpha),
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
                                Icon(
                                    Icons.Rounded.MusicNote,
                                    null,
                                    tint = contentColor,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .musicNote(note)
                                )
                            }

                        }
                    }
                }

                // Right Side: Controls and Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = endPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (!isPermissionGranted) {
                        Text(
                            stringResource(R.string.media_access_required),
                            color = contentColor,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onOpenSettings) {
                            Text(stringResource(R.string.grant))
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
                                    modifier = Modifier.padding(
                                        start = if (appName == appNameLabel) 4.dp else 0.dp, end = 4.dp
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Info
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fadingEdges(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        repeatDelayMillis = 3000
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mediaState.title ?: stringResource(R.string.no_media),
                                    style = MaterialTheme.typography.headlineMedium.copy(shadow = textShadow),
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        repeatDelayMillis = 3000
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mediaState.artist ?: "",
                                    style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow),
                                    color = subContentColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Progress Bar
                        var sliderPosition by remember { mutableStateOf<Float?>(null) }
                        val currentPosition = sliderPosition ?: mediaState.position.toFloat()
                        val duration = mediaState.duration.toFloat().coerceAtLeast(1f)


                        if (mediaState.title != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .blockHorizontalPagerSwipe()
                                    .padding(horizontal = 16.dp)
                            ) {
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
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
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
                        }

                        // Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            leftAction?.let { action ->
                                MediaActionButton(action, contentColor)
                            }

                            IconButton(
                                onClick = onSkipPrevious, modifier = Modifier.size(48.dp)
                            ) {
                                ShadowedIcon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = stringResource(R.string.previous),
                                    modifier = Modifier.size(32.dp),
                                    tint = contentColor
                                )
                            }

                            FilledIconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier
                                    .size(64.dp)
                                    .shadow(elevation = 12.dp, shape = CircleShape),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = iconButtonContainerColor,
                                    contentColor = iconButtonContentColor
                                )
                            ) {
                                ShadowedIcon(
                                    imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = stringResource(R.string.play_pause),
                                    modifier = Modifier.size(40.dp),
                                    tint = iconButtonContentColor
                                )
                            }

                            IconButton(
                                onClick = onSkipNext, modifier = Modifier.size(48.dp)
                            ) {
                                ShadowedIcon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = stringResource(R.string.next),
                                    modifier = Modifier.size(32.dp),
                                    tint = contentColor
                                )
                            }

                            rightAction?.let { action ->
                                MediaActionButton(action, contentColor)
                            }

                            extraActions.forEach { action ->
                                MediaActionButton(action, contentColor)
                            }
                        }
                        Spacer(Modifier.weight(0.5f))
                    }
                }
            }
        } else {
            // Portrait
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
                        stringResource(R.string.media_access_required),
                        color = contentColor,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.media_access_description),
                        color = subContentColor,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onOpenSettings) {
                        Text(stringResource(R.string.grant))
                    }
                } else {
                    if (!isSmallDevice) {
                        // Top App Info / Open Source Button (Only shown in normal layout)
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
                                        modifier = Modifier.padding(
                                            start = if (appName == appNameLabel) 4.dp else 0.dp, end = 4.dp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (isSmallDevice) {
                        // Small Device Layout:
                        // Left side: App Info at top, Title & Artist at bottom (height = album cover height)
                        // Right side: Album Cover
                        Row(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth()
                                .height(portraitAlbumArtSize),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.Top,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Media Source / App Info (Aligned to TOP of Album Cover)
                                Surface(
                                    onClick = onOpenSource,
                                    color = contentColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (appIcon != null) {
                                            AsyncImage(
                                                model = appIcon,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        }
                                        Text(
                                            text = appName,
                                            color = contentColor,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(
                                                start = if (appName == appNameLabel) 4.dp else 0.dp, end = 4.dp
                                            )
                                        )
                                    }
                                }

                                // Track Title & Artist (Centered between Media Source and bottom of Album Cover)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fadingEdges(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .basicMarquee(
                                                    iterations = Int.MAX_VALUE,
                                                    repeatDelayMillis = 3000
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = mediaState.title ?: stringResource(R.string.no_media),
                                                style = MaterialTheme.typography.headlineSmall.copy(shadow = textShadow),
                                                fontWeight = FontWeight.Bold,
                                                color = contentColor,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1
                                            )
                                        }
                                        if (mediaState.artist != "") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .basicMarquee(
                                                        iterations = Int.MAX_VALUE,
                                                        repeatDelayMillis = 3000
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = mediaState.artist ?: "",
                                                    style = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow),
                                                    color = subContentColor,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .size(portraitAlbumArtSize)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp)),
                                color = colorScheme.surfaceVariant.copy(alpha = surfaceAlpha),
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
                                        Icon(
                                            Icons.Rounded.MusicNote,
                                            null,
                                            tint = contentColor,
                                            modifier = Modifier
                                                .size((portraitAlbumArtSize * 0.5f).coerceAtLeast(24.dp))
                                                .musicNote(note)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Normal Device Layout: Album Art centered above Info Column
                        Surface(
                            modifier = Modifier
                                .size(280.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(24.dp)),
                            color = colorScheme.surfaceVariant.copy(alpha = surfaceAlpha),
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
                                    Icon(
                                        Icons.Rounded.MusicNote,
                                        null,
                                        tint = contentColor,
                                        modifier = Modifier
                                            .size(100.dp)
                                            .musicNote(note)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1.5f))

                        // Info
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fadingEdges(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        repeatDelayMillis = 3000
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mediaState.title ?: stringResource(R.string.no_media),
                                    style = MaterialTheme.typography.headlineMedium.copy(shadow = textShadow),
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                            if (mediaState.artist != "") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .basicMarquee(
                                            iterations = Int.MAX_VALUE,
                                            repeatDelayMillis = 3000
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mediaState.artist ?: "",
                                        style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow),
                                        color = subContentColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Progress Bar
                    var sliderPosition by remember { mutableStateOf<Float?>(null) }
                    val currentPosition = sliderPosition ?: mediaState.position.toFloat()
                    val duration = mediaState.duration.toFloat().coerceAtLeast(1f)

                    if (mediaState.title != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .blockHorizontalPagerSwipe()
                                .padding(horizontal = 16.dp)
                        ) {
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
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
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        leftAction?.let { action ->
                            MediaActionButton(action, contentColor)
                        }

                        IconButton(
                            onClick = onSkipPrevious, modifier = Modifier.size(48.dp)
                        ) {
                            ShadowedIcon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(32.dp),
                                tint = contentColor
                            )
                        }

                        FilledIconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(elevation = 12.dp, shape = CircleShape),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = iconButtonContainerColor,
                                contentColor = iconButtonContentColor
                            )
                        ) {
                            ShadowedIcon(
                                imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play/Pause",
                                modifier = Modifier.size(40.dp),
                                tint = iconButtonContentColor
                            )
                        }

                        IconButton(
                            onClick = onSkipNext, modifier = Modifier.size(48.dp)
                        ) {
                            ShadowedIcon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(32.dp),
                                tint = contentColor
                            )
                        }

                        rightAction?.let { action ->
                            MediaActionButton(action, contentColor)
                        }

                        extraActions.forEach { action ->
                            MediaActionButton(action, contentColor)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.height(dockAreaHeight))
                }
            }
        }
    }
}

@androidx.compose.runtime.Immutable
private data class MusicNoteAnimation(
    val rotation: Float,
    val scale: Float,
    val playingFactor: Float,
)

@Composable
private fun rememberMusicNoteAnimation(isPlaying: Boolean): MusicNoteAnimation {
    return MusicNoteAnimation(0f, 1f, if (isPlaying) 1f else 0f)
}

private fun Modifier.musicNote(note: MusicNoteAnimation) = graphicsLayer {
    rotationZ = note.rotation * note.playingFactor
    scaleX = 1f + (note.scale - 1f) * note.playingFactor
    scaleY = 1f + (note.scale - 1f) * note.playingFactor
}

private fun Modifier.fadingEdges(length: Dp = 16.dp) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val edgeLengthPx = length.toPx()
        val width = size.width
        if (width > 0) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    edgeLengthPx / width to Color.Black,
                    (width - edgeLengthPx) / width to Color.Black,
                    1f to Color.Transparent
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

@Composable
private fun ShadowedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color,
    shadowColor: Color = Color.Black.copy(alpha = 0.3f),
    offset: Dp = 2.dp,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .offset(y = offset)
                .blur(offset)
                .alpha(0.5f),
            tint = shadowColor
        )
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            tint = tint
        )
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

@Composable
private fun MediaActionButton(
    action: MediaAction,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val viewModel: LauncherViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    IconButton(
        onClick = {
            try {
                if (action.actionIntent != null) {
                    action.actionIntent.send()
                } else if (action.customAction != null) {
                    viewModel.sendCustomAction(action.customAction)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        modifier = modifier.size(48.dp)
    ) {
        if (action.icon != null) {
            val bitmap = remember(action.icon) {
                try {
                    val drawable = action.icon
                    val width = drawable.intrinsicWidth.coerceAtLeast(1)
                    val height = drawable.intrinsicHeight.coerceAtLeast(1)
                    val bmp = createBitmap(width, height)
                    val canvas = android.graphics.Canvas(bmp)
                    drawable.setBounds(0, 0, width, height)
                    drawable.draw(canvas)
                    bmp.asImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Icon(
                    bitmap = bitmap,
                    contentDescription = action.title,
                    tint = tint,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Text(
                    text = action.title.take(1),
                    color = tint,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = action.title.take(1),
                color = tint,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
