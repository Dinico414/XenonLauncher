package com.xenonware.launcher.ui.res.dock

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.xenon.mylibrary.values.BigSpacing
import com.xenon.mylibrary.values.BiggerSpacing
import com.xenon.mylibrary.values.BiggestBiggerSpacing
import com.xenon.mylibrary.values.BiggestSpacing
import com.xenon.mylibrary.values.ExtraBigBiggerSpacing
import com.xenon.mylibrary.values.ExtraBigSpacing
import com.xenon.mylibrary.values.IconSizeSmall
import com.xenon.mylibrary.values.IconSizeSmaller
import com.xenon.mylibrary.values.LargeMediumCornerRadius
import com.xenon.mylibrary.values.LargeMediumIconSize
import com.xenon.mylibrary.values.LargestCornerRadius
import com.xenon.mylibrary.values.LargestSpacing
import com.xenon.mylibrary.values.MediumCornerRadius
import com.xenon.mylibrary.values.MediumIconSize
import com.xenon.mylibrary.values.MediumLargePadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.MediumSpacer
import com.xenon.mylibrary.values.NoPadding
import com.xenon.mylibrary.values.SmallPadding
import com.xenon.mylibrary.values.SmallSpacer
import com.xenon.mylibrary.values.SmallerPadding
import com.xenon.mylibrary.values.SmallerStroke
import com.xenon.mylibrary.values.SmallestStroke
import com.xenonware.launcher.R
import com.xenonware.launcher.media.MediaState
import com.xenonware.launcher.ui.theme.LocalIsDarkTheme
import com.xenonware.launcher.ui.theme.mainFontFamily
import com.xenonware.launcher.util.ColorUtils
import com.xenonware.launcher.util.openMediaApp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MediaSection(
    isExpanded: Boolean,
    onExpand: () -> Unit,
    mediaState: MediaState,
    isPermissionGranted: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val buttonAlpha = dockButtonAlpha()
    val theme = rememberMediaTheme(mediaState)
    val note = rememberMusicNoteAnimation(mediaState.isPlaying)

    val backgroundColor = theme.background.copy(alpha = buttonAlpha)
    val contentColor = theme.content

    // Always have the border at 0.5 alpha when media is present, hide it if not.
    val borderAlpha = if (mediaState.packageName != null) 0.5f else 0.15f

    Surface(
        onClick = {
            if (isExpanded) openMediaApp(context, mediaState) else onExpand()
        },
        modifier = modifier.dockSectionSize(isExpanded, collapsedWidth = 31.dp),
        shape = DockSectionShape,
        color = backgroundColor,
        contentColor = contentColor,

        border = BorderStroke(SmallestStroke, theme.accent.copy(alpha = borderAlpha))
    ) {
        MaterialTheme(colorScheme = theme.scheme) {
            if (isExpanded) {
                MediaSectionContent(
                    mediaState = mediaState,
                    isPermissionGranted = isPermissionGranted,
                    onPlayPause = onPlayPause,
                    onSkipNext = onSkipNext,
                    onRequestPermission = onRequestPermission,
                    note = note
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        null,
                        modifier = Modifier
                            .size(MediumIconSize)
                            .musicNote(note)
                    )
                }
            }
        }
    }
}

@Immutable
private data class MediaTheme(
    val background: Color,
    val content: Color,
    val accent: Color,
    val scheme: ColorScheme,
)

@Composable
private fun rememberMediaTheme(mediaState: MediaState): MediaTheme {
    val context = LocalContext.current
    val isDark = LocalIsDarkTheme.current
    val scheme = colorScheme
    val surfaceContainerLowest = scheme.surfaceContainerLowest
    val onSurface = scheme.onSurface

    val albumArt = mediaState.albumArt
    val albumArtUri = mediaState.albumArtUri

    val defaultTheme = remember(scheme, surfaceContainerLowest, onSurface) {
        Triple(surfaceContainerLowest, onSurface, scheme.primaryContainer)
    }
    var base by remember { mutableStateOf(defaultTheme) }

    LaunchedEffect(albumArt, albumArtUri, isDark, scheme) {
        val bitmap = when {
            albumArt != null -> albumArt
            albumArtUri != null -> {
                val request = ImageRequest.Builder(context).data(albumArtUri).size(40, 40)
                    .allowHardware(false).build()
                (context.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap(40, 40)
            }

            else -> null
        }

        if (bitmap != null) {
            base = try {
                val seed = ColorUtils.getDominantColor(bitmap)

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
                val accent = if (isDark) {
                    lerp(seed, Color.Black, 0.3f).copy(alpha = 0.6f)
                } else {
                    lerp(seed, Color.White, 0.15f).copy(alpha = 0.3f)
                }

                Triple(bg, text, accent)
            } catch (_: Exception) {
                defaultTheme
            }
        } else {
            // Debounce returning to default theme to prevent flickering during track changes
            delay(500.milliseconds)
            base = defaultTheme
        }
    }

    val background by animateColorAsState(base.first, tween(500), label = "mediaBg")
    val content by animateColorAsState(base.second, tween(500), label = "mediaText")
    val accent by animateColorAsState(base.third, tween(500), label = "mediaPc")

    return remember(background, content, accent, scheme) {
        MediaTheme(
            background = background, content = content, accent = accent, scheme = scheme.copy(
                primary = accent,
                primaryContainer = accent,
                onPrimaryContainer = content,
                onSurface = content
            )
        )
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
        initialValue = -5f, targetValue = 5f, animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "musicNoteRotation"
    )
    val scale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.15f, animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "musicNoteScale"
    )
    val playingFactor by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f, label = "musicNotePlayingFactor"
    )

    return MusicNoteAnimation(rotation, scale, playingFactor)
}

private fun Modifier.musicNote(note: MusicNoteAnimation) = graphicsLayer {
    rotationZ = note.rotation * note.playingFactor
    scaleX = 1f + (note.scale - 1f) * note.playingFactor
    scaleY = 1f + (note.scale - 1f) * note.playingFactor
}

@Composable
private fun MediaSectionContent(
    mediaState: MediaState,
    isPermissionGranted: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onRequestPermission: () -> Unit,
    note: MusicNoteAnimation,
) {
    val contentColor = LocalContentColor.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(MediumLargePadding),
        horizontalArrangement = Arrangement.spacedBy(MediumSpacer),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isPermissionGranted) {
            Text(
                stringResource(R.string.media_access_required),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = MediumPadding),
                fontSize = 12.sp,
                fontFamily = mainFontFamily,
                color = contentColor,
                maxLines = 1,
                softWrap = false
            )
            Button(
                onClick = onRequestPermission,
                contentPadding = PaddingValues(horizontal = MediumPadding, vertical = SmallPadding),
                modifier = Modifier.height(BiggestSpacing)
            ) {
                Text(
                    stringResource(R.string.grant),
                    fontSize = 10.sp,
                    fontFamily = mainFontFamily,
                    maxLines = 1,
                    softWrap = false
                )
            }
        } else {
            val artModel = remember(mediaState.title, mediaState.artist) {
                mediaState.albumArt ?: mediaState.albumArtUri
            }
            val progress = if (mediaState.duration > 0) {
                (mediaState.position.toFloat() / mediaState.duration.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Box(
                contentAlignment = Alignment.Center, modifier = Modifier.size(ExtraBigBiggerSpacing)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (mediaState.title != null) 1f else 0f),
                    color = colorScheme.onPrimaryContainer,
                    strokeWidth = SmallerStroke,
                    trackColor = colorScheme.onSurface.copy(alpha = 0.1f),
                )
                if (artModel != null) {
                    AsyncImage(
                        model = artModel,
                        contentDescription = stringResource(R.string.album_art),
                        modifier = Modifier
                            .padding(if (mediaState.title != null) SmallerPadding else NoPadding)
                            .size(if (mediaState.title != null) BiggestBiggerSpacing else ExtraBigBiggerSpacing)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(ExtraBigSpacing),
                        shape = CircleShape,
                        color = colorScheme.surfaceVariant
                    ) {
                        Icon(
                            Icons.Rounded.MusicNote,
                            null,
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(MediumPadding)
                                .musicNote(note)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center
            ) {
                val textMeasurer = rememberTextMeasurer()
                val titleStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = mainFontFamily,
                    color = contentColor
                )
                val titleText = mediaState.title ?: stringResource(R.string.media)
                val titleWidth = remember(titleText, titleStyle) {
                    textMeasurer.measure(titleText, titleStyle).size.width
                }
                var titleContainerWidth by remember { mutableIntStateOf(0) }
                val titleNeedsMarquee = titleContainerWidth in 1..<titleWidth
                var titleIsScrolling by remember { mutableStateOf(false) }

                if (titleNeedsMarquee) {
                    val density = LocalDensity.current
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

                val startFadeAlpha by animateFloatAsState(
                    if (titleIsScrolling) 1f else 0f, tween(150), label = "dockMediaStartFade"
                )

                Text(
                    text = titleText,
                    style = titleStyle,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { titleContainerWidth = it.size.width }
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            val fadeWidth = LargestSpacing.toPx()
                            if (titleNeedsMarquee) {
                                // Start Fade (Left)
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0f to Color.Black.copy(alpha = 1f - startFadeAlpha),
                                        fadeWidth / size.width to Color.Black,
                                        1f to Color.Black
                                    ), blendMode = BlendMode.DstIn
                                )
                                // End Fade (Right)
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        (size.width - fadeWidth) / size.width to Color.Black,
                                        1f to Color.Transparent
                                    ), blendMode = BlendMode.DstIn
                                )
                            }
                        }
                        .basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = 1200))
                if (!mediaState.artist.isNullOrBlank()) {
                    Text(
                        mediaState.artist,
                        color = contentColor.copy(0.7f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val playInteractionSource = remember { MutableInteractionSource() }
                val isPlayPressed by playInteractionSource.collectIsPressedAsState()
                val playRadius by animateDpAsState(
                    targetValue = when {
                        isPlayPressed -> MediumCornerRadius
                        mediaState.isPlaying -> LargeMediumCornerRadius
                        else -> LargestCornerRadius
                    }, label = "playRadius"
                )

                Surface(
                    onClick = onPlayPause,
                    interactionSource = playInteractionSource,
                    shape = RoundedCornerShape(playRadius),
                    color = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(width = BigSpacing, height = BiggestBiggerSpacing)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            null,
                            modifier = Modifier.size(IconSizeSmall)
                        )
                    }
                }

                Spacer(Modifier.width(SmallSpacer))

                IconButton(
                    onClick = onSkipNext, modifier = Modifier.size(
                        width = LargeMediumIconSize, height = BiggestBiggerSpacing
                    )
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        null,
                        tint = contentColor,
                        modifier = Modifier.size(IconSizeSmaller)
                    )
                }
            }
        }
    }
}