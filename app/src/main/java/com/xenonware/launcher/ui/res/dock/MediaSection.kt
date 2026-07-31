package com.xenonware.launcher.ui.res.dock

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.xenonware.launcher.media.MediaState
import com.xenonware.launcher.util.ColorUtils
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
    /** Colour the section sits on, used to decide whether it needs a border. */
    dockColor: Color,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val buttonAlpha = dockButtonAlpha()
    val theme = rememberMediaTheme(mediaState)
    val note = rememberMusicNoteAnimation(mediaState.isPlaying)

    val backgroundColor = theme.background.copy(alpha = buttonAlpha)
    val contentColor = theme.content

    // Only draw a border when the section barely separates from the dock behind it.
    val contrastRatio = remember(backgroundColor, dockColor) {
        ColorUtils.calculateContrastRatio(backgroundColor, dockColor)
    }
    val borderAlpha = ((1.05f - contrastRatio) * 10f).coerceIn(0f, 0.5f)

    Surface(
        onClick = {
            if (isExpanded) openMediaApp(context, mediaState) else onExpand()
        },
        modifier = modifier.dockSectionSize(isExpanded),
        shape = DockSectionShape,
        color = backgroundColor,
        contentColor = contentColor,
        // Drawn by the Surface itself, not on the incoming modifier: a clickable
        // Surface inflates its outer node to the 48.dp minimum touch target, so
        // anything drawn out there traces a 48x48 square (a circle, once the
        // corners are clamped) instead of the visible 32x48 pill.
        border = if (borderAlpha > 0f) {
            BorderStroke(1.dp, theme.accent.copy(alpha = borderAlpha))
        } else null
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
                            .size(24.dp)
                            .musicNote(note)
                    )
                }
            }
        }
    }
}

/* ---------------------------------------------------------------------- */
/* Album art theming                                                       */
/* ---------------------------------------------------------------------- */

@androidx.compose.runtime.Immutable
private data class MediaTheme(
    val background: Color,
    val content: Color,
    val accent: Color,
    val scheme: androidx.compose.material3.ColorScheme,
)

/**
 * Pulls a dominant colour out of the current album art and blends it into the
 * app's scheme. Falls back to the default surface colours, debounced so that
 * track changes don't make the pill flicker.
 */
@Composable
private fun rememberMediaTheme(mediaState: MediaState): MediaTheme {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
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
                val request = ImageRequest.Builder(context)
                    .data(albumArtUri)
                    .size(40, 40)
                    .allowHardware(false)
                    .build()
                (context.imageLoader.execute(request) as? SuccessResult)
                    ?.drawable?.toBitmap(40, 40)
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
            background = background,
            content = content,
            accent = accent,
            scheme = scheme.copy(
                primaryContainer = accent,
                onPrimaryContainer = content,
                onSurface = content
            )
        )
    }
}

/* ---------------------------------------------------------------------- */
/* Music note idle animation                                               */
/* ---------------------------------------------------------------------- */

@androidx.compose.runtime.Immutable
private data class MusicNoteAnimation(
    val rotation: Float,
    val scale: Float,
    val playingFactor: Float,
)

@Composable
private fun rememberMusicNoteAnimation(isPlaying: Boolean): MusicNoteAnimation {
    val infiniteTransition = rememberInfiniteTransition(label = "musicNoteAnim")

    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "musicNoteRotation"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
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

private fun Modifier.musicNote(note: MusicNoteAnimation) = graphicsLayer {
    rotationZ = note.rotation * note.playingFactor
    scaleX = 1f + (note.scale - 1f) * note.playingFactor
    scaleY = 1f + (note.scale - 1f) * note.playingFactor
}

/* ---------------------------------------------------------------------- */
/* Expanded content                                                        */
/* ---------------------------------------------------------------------- */

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
            val artModel = remember(mediaState.title, mediaState.artist) {
                mediaState.albumArt ?: mediaState.albumArtUri
            }
            if (artModel != null) {
                AsyncImage(
                    model = artModel,
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
                    color = colorScheme.surfaceVariant
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(10.dp)
                            .musicNote(note)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0.92f to Color.Black,
                                1f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                Text(
                    mediaState.title ?: "No Media",
                    color = contentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    mediaState.artist ?: "Unknown Artist",
                    color = contentColor.copy(0.7f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val playInteractionSource = remember { MutableInteractionSource() }
                val isPlayPressed by playInteractionSource.collectIsPressedAsState()
                val playRadius by animateDpAsState(
                    targetValue = when {
                        isPlayPressed -> 8.dp
                        mediaState.isPlaying -> 12.dp
                        else -> 16.dp
                    },
                    label = "playRadius"
                )

                Surface(
                    onClick = onPlayPause,
                    interactionSource = playInteractionSource,
                    shape = RoundedCornerShape(playRadius),
                    color = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(width = 28.dp, height = 36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(width = 28.dp, height = 36.dp)
                ) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

fun openMediaApp(context: Context, mediaState: MediaState) {
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