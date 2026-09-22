package com.xenonware.launcher.ui.pages

import android.app.WallpaperColors
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.xenon.mylibrary.res.MenuItem
import com.xenon.mylibrary.res.XenonDropDown
import com.xenon.mylibrary.values.BiggerElevation
import com.xenon.mylibrary.values.BiggerSpacing
import com.xenon.mylibrary.values.BiggestBiggerSpacing
import com.xenon.mylibrary.values.BiggestPadding
import com.xenon.mylibrary.values.BiggestSpacer
import com.xenon.mylibrary.values.ExtraBigSpacing
import com.xenon.mylibrary.values.ExtraBiggerSpacing
import com.xenon.mylibrary.values.ExtraLargeCornerRadius
import com.xenon.mylibrary.values.ExtraLargeIconSize
import com.xenon.mylibrary.values.ExtraLargeSpacing
import com.xenon.mylibrary.values.ExtraLargerCornerRadius
import com.xenon.mylibrary.values.ExtraLargerSpacer
import com.xenon.mylibrary.values.ExtraLargerSpacing
import com.xenon.mylibrary.values.HugeBiggerSpacing
import com.xenon.mylibrary.values.HugerSpacing
import com.xenon.mylibrary.values.IconSizeLarge
import com.xenon.mylibrary.values.LargeIconSize
import com.xenon.mylibrary.values.LargeMediumCornerRadius
import com.xenon.mylibrary.values.LargeMediumElevation
import com.xenon.mylibrary.values.LargeMediumIconSize
import com.xenon.mylibrary.values.LargestCornerRadius
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.LargestSpacer
import com.xenon.mylibrary.values.LargestSpacing
import com.xenon.mylibrary.values.MediumButtonHeight
import com.xenon.mylibrary.values.MediumElevation
import com.xenon.mylibrary.values.MediumIconSize
import com.xenon.mylibrary.values.MediumLargeCornerRadius
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.MediumSmallPadding
import com.xenon.mylibrary.values.MediumSmallSpacer
import com.xenon.mylibrary.values.MediumSpacer
import com.xenon.mylibrary.values.MediumSpacing
import com.xenon.mylibrary.values.NoPadding
import com.xenon.mylibrary.values.SmallPadding
import com.xenon.mylibrary.values.SmallSpacing
import com.xenon.mylibrary.values.SmallerSpacer
import com.xenonware.launcher.R
import com.xenonware.launcher.media.MediaAction
import com.xenonware.launcher.media.MediaControllerManager
import com.xenonware.launcher.media.MediaState
import com.xenonware.launcher.ui.res.MusicVisualizer
import com.xenonware.launcher.ui.res.VisualizerConfig
import com.xenonware.launcher.ui.res.rememberVisualizerPalette
import com.xenonware.launcher.ui.theme.LocalIsDarkTheme
import com.xenonware.launcher.ui.theme.mainFontFamily
import com.xenonware.launcher.util.AudioSpectrumAnalyzer
import com.xenonware.launcher.util.ColorUtils
import com.xenonware.launcher.util.blockHorizontalPagerSwipe
import com.xenonware.launcher.util.isSmallScreenDevice
import com.xenonware.launcher.util.openMediaApp
import com.xenonware.launcher.util.shouldDisableLandscapeLayout
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MediaPage(
    mediaState: MediaState,
    progress: () -> Float,
    isPermissionGranted: Boolean,
    isDarkTheme: Boolean = LocalIsDarkTheme.current,
    onOpenSettings: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    isDockVisible: Boolean = true,
    /** LauncherViewModel.audioAnalyzer when RECORD_AUDIO is granted, else null (synthetic pulse). */
    audioAnalyzer: AudioSpectrumAnalyzer? = null,
    /** Whether this page is the current pager page; pauses the visualizer's frame loop otherwise. */
    isVisible: Boolean = true,
) {
    val hazeState = rememberHazeState()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        VisualizerConfig.load(context)
    }
    val pm = remember { context.packageManager }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val disableLandscape = shouldDisableLandscapeLayout(context)
    val useLandscapeLayout = isLandscape && !disableLandscape
    val openMediaApp: () -> Unit = { openMediaApp(context, mediaState) }

    val theme = rememberMediaTheme(mediaState)

    // Captured before the album-tinted MaterialTheme below, so "Material You" = the app's scheme
    val systemColorScheme = colorScheme
    val visualizerColors = rememberVisualizerPalette(
        profile = VisualizerConfig.colorProfile,
        systemScheme = systemColorScheme,
        albumArt = mediaState.albumArt,
        albumArtUri = mediaState.albumArtUri,
    )

    val baseBgAlpha = if (isDarkTheme) 0.8f else 0.6f

    MaterialTheme(colorScheme = theme.scheme) {
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
        val topPadding = if (statusBarHeight < LargestSpacing) {
            LargestSpacing
        } else {
            statusBarHeight
        }
        val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
        val layoutDirection = LocalLayoutDirection.current
        val endPadding = safeDrawingPadding.calculateEndPadding(layoutDirection).coerceAtLeast(LargestPadding)
        val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        // 72dp (dock) + 8dp (dock padding) + 8dp (gap) + 4dp (to match widget vertical padding)
        val dockAreaHeight = if (isDockVisible) HugeBiggerSpacing + navBarHeight + MediumSpacing + MediumSpacing + SmallSpacing else navBarHeight + LargestSpacing

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

        val leftAction = mediaState.actions.getOrNull(0)
        val rightAction = mediaState.actions.getOrNull(1)

        val isSmallDevice = isSmallScreenDevice(context)
        // True window size in px (Configuration.screenSizeDp can lag resizes), converted to dp.
        val windowInfo = androidx.compose.ui.platform.LocalWindowInfo.current
        val windowWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
        val windowHeightDp = with(density) { windowInfo.containerSize.height.toDp() }
        val verticalSafeDrawHeight = (windowHeightDp - topPadding - navBarHeight).coerceAtLeast(ExtraBigSpacing)
        val maxSmallCoverSize = minOf(windowWidthDp / 3, verticalSafeDrawHeight/3)
        val portraitAlbumArtSize = if (isSmallDevice) maxSmallCoverSize else 280.dp

        // Constant background effects (no animation)
        val bgProgress = 1f

        val dynamicBackground = colorScheme.inversePrimary
        val backgroundTint = dynamicBackground.copy(alpha = baseBgAlpha)

        val textShadow = Shadow(
            color = Color.Black.copy(alpha = 0.3f), offset = Offset(0f, 2f), blurRadius = 4f
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .graphicsLayer {
                    val normalized = ((progress() - 0.75f) * 4f).coerceIn(0f, 1f)
                    val eased = EaseInOut.transform(normalized)
                    if (eased < 1f) {
                        shape = RoundedCornerShape(ExtraBigSpacing * (1f - eased))
                        clip = true
                    }
                }
        ) {
            // Background layers: tint, album art, visualizer
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(backgroundTint.copy(alpha = baseBgAlpha * bgProgress))
            ) {
                // Background Album Art
                artModel?.let { model ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = bgProgress
                                compositingStrategy = if (progress() > 0.01f) {
                                    CompositingStrategy.Offscreen
                                } else {
                                    CompositingStrategy.Auto
                                }
                            }
                    ) {
                        AsyncImage(
                            model = model,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(BiggerElevation),
                            contentScale = ContentScale.Crop,
                            colorFilter = ColorFilter.tint(
                                dynamicBackground.copy(alpha = 0.4f), blendMode = BlendMode.SrcAtop
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

                // Music visualizer: behind all content, anchored to the bottom-right screen edge
                if (isPermissionGranted) {
                    // Landscape & tablet split UI: right side only; phone portrait: full width
                    val isTablet = configuration.smallestScreenWidthDp >= 600
                    val visualizerWidth = if (useLandscapeLayout || isTablet) {
                        VisualizerConfig.widthFractionSplit.coerceIn(0.1f, 1f)
                    } else {
                        1f
                    }
                    MusicVisualizer(
                        isPlaying = mediaState.isPlaying,
                        isActive = isVisible,
                        analyzer = audioAnalyzer,
                        colors = visualizerColors,
                        leftFade = if (visualizerWidth < 1f) VisualizerConfig.leftFadeWidth else 0.dp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)   // edge to edge: no insets, no padding
                            .fillMaxWidth(visualizerWidth)
                            .fillMaxHeight(0.5f)
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
                    horizontalArrangement = Arrangement.spacedBy(LargestSpacer)
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
                                .clip(RoundedCornerShape(ExtraLargerCornerRadius)),
                            color = colorScheme.surfaceVariant.copy(alpha = surfaceAlpha),
                            tonalElevation = MediumElevation
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
                                            .size(120.dp)
                                            .musicNote(note)
                                    )
                                }
                            }
                        }
                    }

                    // Right Side: Info and Controls
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .padding(end = endPadding),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!isPermissionGranted) {
                            Text(
                                stringResource(R.string.media_access_required),
                                color = contentColor,
                                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = mainFontFamily),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(MediumSpacer))
                            Text(
                                stringResource(R.string.media_access_description),
                                color = subContentColor,
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = mainFontFamily),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(ExtraLargerSpacer))
                            Button(onClick = onOpenSettings) {
                                Text(stringResource(R.string.grant), style = MaterialTheme.typography.labelLarge.copy(fontFamily = mainFontFamily))
                            }
                        } else {
                            // App Name
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = openMediaApp,
                                    color = contentColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(ExtraLargeCornerRadius),
                                    modifier = Modifier.height(ExtraBigSpacing)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = MediumPadding, vertical = MediumPadding),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(MediumSpacer)
                                    ) {
                                        if (appIcon != null) {
                                            AsyncImage(
                                                model = appIcon,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(ExtraLargerSpacing)
                                                    .clip(RoundedCornerShape(LargeMediumCornerRadius))
                                            )
                                        }
                                        Text(
                                            text = appName,
                                            color = contentColor,
                                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = mainFontFamily),
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(
                                                start = if (appName == appNameLabel) SmallPadding else NoPadding, end = SmallPadding
                                            )
                                        )
                                    }
                                }
                                VisualizerSettingsDropdown(contentColor, hazeState)
                            }

                            Spacer(Modifier.weight(1f))

                            // Info
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val textMeasurer = rememberTextMeasurer()
                                val titleStyle = MaterialTheme.typography.headlineMedium.copy(
                                    shadow = textShadow,
                                    fontFamily = mainFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor,
                                    textAlign = TextAlign.Center
                                )
                                val titleText = mediaState.title ?: stringResource(R.string.no_media)
                                val titleWidth = remember(titleText, titleStyle) {
                                    textMeasurer.measure(titleText, titleStyle).size.width
                                }
                                var titleContainerWidth by remember { mutableIntStateOf(0) }
                                val titleNeedsMarquee = titleContainerWidth in 1..<titleWidth
                                var titleIsScrolling by remember { mutableStateOf(false) }

                                if (titleNeedsMarquee) {
                                    LaunchedEffect(titleText, titleContainerWidth) {
                                        val velocityPx = with(density) { BiggerSpacing.toPx() }
                                        val spacingPx = titleContainerWidth / 3f
                                        val scrollDistance = titleWidth + spacingPx
                                        val scrollDuration = (scrollDistance / velocityPx * 1000).toLong()

                                        while (true) {
                                            titleIsScrolling = false
                                            delay(1200.milliseconds)
                                            titleIsScrolling = true
                                            delay(scrollDuration.milliseconds)
                                        }
                                    }
                                }

                                val startFadeAlpha by animateFloatAsState(if (titleIsScrolling) 1f else 0f, tween(150), label = "titleStartFade")

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { titleContainerWidth = it.size.width }
                                        .fadingEdges(startAlpha = startFadeAlpha, endAlpha = if (titleNeedsMarquee) 1f else 0f)
                                        .basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = 1200),
                                    contentAlignment = Alignment.Center
                                )
                                {
                                    Text(
                                        text = titleText,
                                        style = titleStyle,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = LargestPadding)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = 3000),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mediaState.artist ?: "",
                                        style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow),
                                        color = subContentColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = LargestPadding)
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
                                        .padding(horizontal = LargestPadding)
                                ) {
                                    @Suppress("DEPRECATION")  // kept on the value-based overload; the newer
                                    // state-based Slider constructor varies across Material3 versions.
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
                                            .padding(horizontal = SmallPadding),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            formatTime(currentPosition.toLong()),
                                            color = contentColor.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = mainFontFamily)
                                        )
                                        Text(
                                            formatTime(mediaState.duration),
                                            color = contentColor.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = mainFontFamily)
                                        )
                                    }
                                }
                            }

                            // Controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(ExtraLargerSpacer)
                            ) {
                                leftAction?.let { action ->
                                    MediaActionButton(action, contentColor)
                                }

                                IconButton(
                                    onClick = onSkipPrevious, modifier = Modifier.size(MediumButtonHeight)
                                ) {
                                    ShadowedIcon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = stringResource(R.string.previous),
                                        modifier = Modifier.size(IconSizeLarge),
                                        tint = contentColor
                                    )
                                }

                                FilledIconButton(
                                    onClick = onTogglePlayPause,
                                    modifier = Modifier
                                        .size(HugerSpacing)
                                        .shadow(elevation = LargeMediumElevation, shape = CircleShape),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = iconButtonContainerColor,
                                        contentColor = iconButtonContentColor
                                    )
                                ) {
                                    ShadowedIcon(
                                        imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = stringResource(R.string.play_pause),
                                        modifier = Modifier.size(ExtraLargeIconSize),
                                        tint = iconButtonContentColor
                                    )
                                }

                                IconButton(
                                    onClick = onSkipNext, modifier = Modifier.size(MediumButtonHeight)
                                ) {
                                    ShadowedIcon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = stringResource(R.string.next),
                                        modifier = Modifier.size(LargeIconSize),
                                        tint = contentColor
                                    )
                                }

                                rightAction?.let { action ->
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
                        .padding(top = topPadding, bottom = dockAreaHeight)
                        .padding(horizontal = BiggestPadding)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isPermissionGranted) {
                        Text(
                            stringResource(R.string.media_access_required),
                            color = contentColor,
                            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = mainFontFamily),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(MediumSpacing))
                        Text(
                            stringResource(R.string.media_access_description),
                            color = subContentColor,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = mainFontFamily),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(ExtraLargerSpacing))
                        Button(onClick = onOpenSettings) {
                            Text(stringResource(R.string.grant), style = MaterialTheme.typography.labelLarge.copy(fontFamily = mainFontFamily))
                        }
                    } else {
                        if (!isSmallDevice) {
                            // Top App Info / Open Source Button (Only shown in normal layout)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    onClick = openMediaApp,
                                    color = contentColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(ExtraLargeCornerRadius),
                                    modifier = Modifier.height(ExtraBigSpacing)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = MediumPadding, vertical = MediumPadding),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(MediumSpacer)
                                    ) {
                                        if (appIcon != null) {
                                            AsyncImage(
                                                model = appIcon,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(ExtraLargerSpacing)
                                                    .clip(RoundedCornerShape(LargeMediumCornerRadius))
                                            )
                                        }
                                        Text(
                                            text = appName,
                                            color = contentColor,
                                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = mainFontFamily),
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(
                                                start = if (appName == appNameLabel) SmallPadding else NoPadding, end = SmallPadding
                                            )
                                        )
                                    }
                                }
                                VisualizerSettingsDropdown(contentColor, hazeState)
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        if (isSmallDevice) {
                            // Small Device Layout:
                            Row(
                                modifier = Modifier
                                    .padding(vertical = MediumPadding)
                                    .fillMaxWidth()
                                    .height(portraitAlbumArtSize),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(LargestSpacer)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Media Source / App Info (Aligned to TOP of Album Cover)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            onClick = openMediaApp,
                                            color = contentColor.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(ExtraLargeCornerRadius),
                                            modifier = Modifier.height(BiggestBiggerSpacing)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = MediumPadding, vertical = MediumSmallPadding),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(MediumSmallSpacer)
                                            ) {
                                                if (appIcon != null) {
                                                    AsyncImage(
                                                        model = appIcon,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .size(ExtraLargeSpacing)
                                                            .clip(RoundedCornerShape(MediumLargeCornerRadius))
                                                    )
                                                }
                                                Text(
                                                    text = appName,
                                                    color = contentColor,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = mainFontFamily),
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(
                                                        start = if (appName == appNameLabel) SmallPadding else NoPadding, end = SmallPadding
                                                    )
                                                )
                                            }
                                        }
                                        VisualizerSettingsDropdown(contentColor, hazeState)
                                    }

                                    // Track Title & Artist (Centered between Media Source and bottom of Album Cover)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            val textMeasurer = rememberTextMeasurer()
                                            val titleStyle = MaterialTheme.typography.headlineSmall.copy(
                                                shadow = textShadow,
                                                fontFamily = mainFontFamily,
                                                fontWeight = FontWeight.Bold,
                                                color = contentColor,
                                                textAlign = TextAlign.Center
                                            )
                                            val titleText = mediaState.title ?: stringResource(R.string.no_media)
                                            val titleWidth = remember(titleText, titleStyle) {
                                                textMeasurer.measure(titleText, titleStyle).size.width
                                            }
                                            var titleContainerWidth by remember { mutableIntStateOf(0) }
                                            val titleNeedsMarquee = titleContainerWidth in 1..<titleWidth
                                            var titleIsScrolling by remember { mutableStateOf(false) }

                                            if (titleNeedsMarquee) {
                                                LaunchedEffect(titleText, titleContainerWidth) {
                                                    val velocityPx = with(density) { BiggerSpacing.toPx() }
                                                    val spacingPx = titleContainerWidth / 3f
                                                    val scrollDistance = titleWidth + spacingPx
                                                    val scrollDuration = (scrollDistance / velocityPx * 1000).toLong()

                                                    while (true) {
                                                        titleIsScrolling = false
                                                        delay(1200.milliseconds)
                                                        titleIsScrolling = true
                                                        delay(scrollDuration.milliseconds)
                                                    }
                                                }
                                            }

                                            val startFadeAlpha by animateFloatAsState(if (titleIsScrolling) 1f else 0f, tween(150), label = "titleStartFadePortrait")

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .onGloballyPositioned { titleContainerWidth = it.size.width }
                                                    .fadingEdges(startAlpha = startFadeAlpha, endAlpha = if (titleNeedsMarquee) 1f else 0f)
                                                    .basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = 1200),
                                                contentAlignment = Alignment.Center
                                            )
                                            {
                                                Text(
                                                    text = titleText,
                                                    style = titleStyle,
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
                                        .clip(RoundedCornerShape(LargestCornerRadius)),
                                    color = theme.background.copy(alpha = surfaceAlpha),
                                    tonalElevation = MediumElevation
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
                                                    .size((portraitAlbumArtSize * 0.5f).coerceAtLeast(MediumIconSize))
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
                                    .size(portraitAlbumArtSize)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(ExtraLargerCornerRadius)),
                                color = colorScheme.surfaceVariant.copy(alpha = surfaceAlpha),
                                tonalElevation = MediumElevation
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
                                                .size(120.dp)
                                                .musicNote(note)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(BiggestSpacer))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val textMeasurer = rememberTextMeasurer()
                                val titleStyle = MaterialTheme.typography.headlineMedium.copy(
                                    shadow = textShadow,
                                    fontFamily = mainFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor,
                                    textAlign = TextAlign.Center
                                )
                                val titleText = mediaState.title ?: stringResource(R.string.no_media)
                                val titleWidth = remember(titleText, titleStyle) {
                                    textMeasurer.measure(titleText, titleStyle).size.width
                                }
                                var titleContainerWidth by remember { mutableIntStateOf(0) }
                                val titleNeedsMarquee = titleContainerWidth in 1..<titleWidth
                                var titleIsScrolling by remember { mutableStateOf(false) }

                                if (titleNeedsMarquee) {
                                    LaunchedEffect(titleText, titleContainerWidth) {
                                        val velocityPx = with(density) { BiggerSpacing.toPx() }
                                        val spacingPx = titleContainerWidth / 3f
                                        val scrollDistance = titleWidth + spacingPx
                                        val scrollDuration = (scrollDistance / velocityPx * 1000).toLong()

                                        while (true) {
                                            titleIsScrolling = false
                                            delay(1200.milliseconds)
                                            titleIsScrolling = true
                                            delay(scrollDuration.milliseconds)
                                        }
                                    }
                                }

                                val startFadeAlpha by animateFloatAsState(if (titleIsScrolling) 1f else 0f, tween(200), label = "titleStartFadeNormal")

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { titleContainerWidth = it.size.width }
                                        .fadingEdges(startAlpha = startFadeAlpha, endAlpha = if (titleNeedsMarquee) 1f else 0f)
                                        .basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = 1200),
                                    contentAlignment = Alignment.Center
                                )
                                {
                                    Text(
                                        text = titleText,
                                        style = titleStyle,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = LargestPadding)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = 3000),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mediaState.artist ?: "",
                                        style = MaterialTheme.typography.bodyLarge.copy(shadow = textShadow),
                                        color = subContentColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = LargestPadding)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1.2f))

                        // Progress Bar
                        var sliderPosition by remember { mutableStateOf<Float?>(null) }
                        val currentPosition = sliderPosition ?: mediaState.position.toFloat()
                        val duration = mediaState.duration.toFloat().coerceAtLeast(1f)

                        if (mediaState.title != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .blockHorizontalPagerSwipe()
                            ) {
                                @Suppress("DEPRECATION")  // kept on the value-based overload; the newer
                                // state-based Slider constructor varies across Material3 versions.
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
                                        .padding(horizontal = SmallPadding),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        formatTime(currentPosition.toLong()),
                                        color = contentColor.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = mainFontFamily)
                                    )
                                    Text(
                                        formatTime(mediaState.duration),
                                        color = contentColor.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = mainFontFamily)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(MediumSpacer))

                        // Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(ExtraLargerSpacer)
                        ) {
                            leftAction?.let { action ->
                                MediaActionButton(action, contentColor)
                            }

                            IconButton(
                                onClick = onSkipPrevious, modifier = Modifier.size(MediumButtonHeight)
                            ) {
                                ShadowedIcon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = stringResource(R.string.previous),
                                    modifier = Modifier.size(IconSizeLarge),
                                    tint = contentColor
                                )
                            }

                            FilledIconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier
                                    .size(HugerSpacing)
                                    .shadow(elevation = LargeMediumElevation, shape = CircleShape),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = iconButtonContainerColor,
                                    contentColor = iconButtonContentColor
                                )
                            ) {
                                ShadowedIcon(
                                    imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = stringResource(R.string.play_pause),
                                    modifier = Modifier.size(ExtraLargeIconSize),
                                    tint = iconButtonContentColor
                                )
                            }

                            IconButton(
                                onClick = onSkipNext, modifier = Modifier.size(MediumButtonHeight)
                            ) {
                                ShadowedIcon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = stringResource(R.string.next),
                                    modifier = Modifier.size(LargeIconSize),
                                    tint = contentColor
                                )
                            }

                            rightAction?.let { action ->
                                MediaActionButton(action, contentColor)
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun Modifier.musicNote(note: MusicNoteAnimation) = graphicsLayer {
    rotationZ = note.rotation * note.playingFactor
    scaleX = 1f + (note.scale - 1f) * note.playingFactor
    scaleY = 1f + (note.scale - 1f) * note.playingFactor
}

private fun Modifier.fadingEdges(
    length: Dp = LargestSpacing,
    startAlpha: Float = 1f,
    endAlpha: Float = 1f
) = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val edgeLengthPx = length.toPx()
        val width = size.width
        if (width > 0) {
            // Start Fade (Left)
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 1f - startAlpha),
                    edgeLengthPx / width to Color.Black,
                    1f to Color.Black
                ),
                blendMode = BlendMode.DstIn
            )
            // End Fade (Right)
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Black,
                    (width - edgeLengthPx * endAlpha) / width to Color.Black,
                    1f to Color.Transparent
                ),
                blendMode = BlendMode.DstIn
            )
        }
    }

/** Opacity of the drop shadow under the transport / action icons (was effectively 0.15). */
private const val ICON_SHADOW_ALPHA = 0.55f

@Composable
private fun ShadowedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color,
    shadowColor: Color = Color.Black.copy(alpha = ICON_SHADOW_ALPHA),
    offset: Dp = SmallerSpacer,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .offset(y = offset)
                .blur(offset),
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

/** Bitmap counterpart of [ShadowedIcon], so app-provided action icons match the transport icons. */
@Composable
private fun ShadowedBitmapIcon(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color,
    shadowColor: Color = Color.Black.copy(alpha = ICON_SHADOW_ALPHA),
    offset: Dp = SmallerSpacer,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .offset(y = offset)
                .blur(offset),
            tint = shadowColor
        )
        Icon(
            bitmap = bitmap,
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
    IconButton(
        // Custom session actions first, then the notification intent; refreshes the state
        // right away so a toggled like/shuffle icon updates without waiting for the next poll.
        onClick = { MediaControllerManager.instance?.perform(action) },
        modifier = modifier.size(ExtraBiggerSpacing)
    ) {
        val bitmap = action.iconBitmap
        if (bitmap != null) {
            ShadowedBitmapIcon(
                bitmap = bitmap,
                contentDescription = action.title,
                modifier = Modifier.size(LargeMediumIconSize),
                tint = tint
            )
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

@Immutable
private data class MusicNoteAnimation(
    val rotation: Float,
    val scale: Float,
    val playingFactor: Float,
)

@Composable
private fun rememberMusicNoteAnimation(isPlaying: Boolean): MusicNoteAnimation {
    val transition = rememberInfiniteTransition(label = "musicNoteAnim")

    val rotation by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "musicNoteRotation"
    )
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "musicNoteScale"
    )
    val playingFactor by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        label = "musicNotePlayingFactor"
    )

    return MusicNoteAnimation(rotation, scale, playingFactor)
}

/**
 * Source color of the album cover, picked the way Material You picks it from a wallpaper
 * (WallpaperColors' primary color), with the dominant color as fallback.
 */
private fun albumSeedColor(bitmap: Bitmap): Color? {
    val soft = if (bitmap.config == Bitmap.Config.HARDWARE) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false)
    } else bitmap
    val small = if (soft.width > 112 || soft.height > 112) {
        Bitmap.createScaledBitmap(soft, 112, 112, true)
    } else soft

    runCatching { WallpaperColors.fromBitmap(small).primaryColor.toArgb() }
        .getOrNull()?.let { return Color(it) }
    return ColorUtils.getDominantColor(small).takeIf { it != Color.Unspecified }
}

@Immutable
private data class MediaTheme(
    val background: Color,
    val content: Color,
    val accent: Color,
    val scheme: ColorScheme,
)

/**
 * The media page's theme. Every color keeps its role exactly as without a cover
 * (surfaceContainerLowest background, onSurface content, primaryContainer accent, …); only the
 * SOURCE changes: with an album cover, a full Material You scheme is generated from the cover's
 * source color (same algorithm as system dynamic color), otherwise the app's scheme is used.
 */
@Composable
private fun rememberMediaTheme(mediaState: MediaState): MediaTheme {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val systemScheme = colorScheme

    val albumArt = mediaState.albumArt
    val albumArtUri = mediaState.albumArtUri

    var albumScheme by remember { mutableStateOf<ColorScheme?>(null) }

    LaunchedEffect(albumArt, albumArtUri, isDark) {
        val bitmap = when {
            albumArt != null -> albumArt
            albumArtUri != null -> {
                val request = ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .size(112, 112)
                    .allowHardware(false)
                    .build()
                (context.imageLoader.execute(request) as? SuccessResult)
                    ?.drawable?.toBitmap(112, 112)
            }
            else -> null
        }

        if (bitmap != null) {
            albumScheme = withContext(Dispatchers.Default) {
                try {
                    albumSeedColor(bitmap)?.let { seed ->
                        dynamicColorScheme(
                            seedColor = seed,
                            isDark = isDark,
                            isAmoled = false,
                            style = PaletteStyle.TonalSpot, // the style Android uses for wallpapers
                        )
                    }
                } catch (_: Exception) {
                    null
                }
            }
        } else {
            // Debounce returning to the app scheme to prevent flickering during track changes
            delay(500.milliseconds)
            albumScheme = null
        }
    }

    val source = albumScheme ?: systemScheme

    // Same roles as always, just from the album-generated scheme when there is a cover
    val background by animateColorAsState(source.surfaceContainerLowest, tween(500), label = "mediaBg")
    val content by animateColorAsState(source.onSurface, tween(500), label = "mediaText")
    val accent by animateColorAsState(source.primaryContainer, tween(500), label = "mediaPc")

    return remember(background, content, accent, source) {
        MediaTheme(
            background = background,
            content = content,
            accent = accent,
            scheme = source.copy(
                primary = accent,
                primaryContainer = accent,
                onPrimaryContainer = content,
                onSurface = content,
                inversePrimary = background,
                surface = background,
                surfaceVariant = background
            )
        )
    }
}

@Composable
private fun VisualizerSettingsDropdown(contentColor: Color, hazeState: HazeState) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(
            onClick = {
                menuExpanded = true
            },
            modifier = Modifier.size(ExtraBigSpacing)
        ) {
            ShadowedIcon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Visualizer Settings",
                tint = contentColor,
                modifier = Modifier.size(LargeMediumIconSize)
            )
        }

        val menuItems = remember(VisualizerConfig.visualizerStyle, VisualizerConfig.reactivity, VisualizerConfig.geometricStyle, VisualizerConfig.waves, VisualizerConfig.colorProfile) {
            listOf(
                MenuItem(
                    text = "Style: ${VisualizerConfig.getStyleName(VisualizerConfig.visualizerStyle)}",
                    onClick = {
                        VisualizerConfig.nextStyle()
                        VisualizerConfig.save(context)
                    },
                    dismissOnClick = false
                ),
                MenuItem(
                    text = "Reactivity: ${VisualizerConfig.getReactivityName(VisualizerConfig.reactivity)}",
                    onClick = {
                        VisualizerConfig.nextReactivity()
                        VisualizerConfig.save(context)
                    },
                    dismissOnClick = false
                ),
                MenuItem(
                    text = "Geometry: ${VisualizerConfig.getGeometryName(VisualizerConfig.geometricStyle)}",
                    onClick = {
                        VisualizerConfig.nextGeometry()
                        VisualizerConfig.save(context)
                    },
                    dismissOnClick = false
                ),
                MenuItem(
                    text = "Waves: ${if (VisualizerConfig.waves == 1) "On" else "Off"}",
                    onClick = {
                        VisualizerConfig.waves = if (VisualizerConfig.waves == 1) 0 else 1
                        VisualizerConfig.save(context)
                    },
                    dismissOnClick = false
                ),
                MenuItem(
                    text = "Color Profile: ${VisualizerConfig.getColorProfileName(VisualizerConfig.colorProfile)}",
                    onClick = {
                        VisualizerConfig.nextColorProfile()
                        VisualizerConfig.save(context)
                    },
                    dismissOnClick = false
                )
            )
        }

        XenonDropDown(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            items = menuItems,
            hazeState = hazeState,
            offsetY = 48.dp,
            offsetX = 48.dp,
            widthMin = 210.dp,
            widthMax = 210.dp,
            alignment = Alignment.TopEnd,
            mainContextFont = mainFontFamily
        )
    }
}