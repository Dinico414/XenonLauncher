package com.xenonware.launcher.ui.res.dock

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.xenonware.launcher.media.MediaState

@Composable
fun MediaSection(
    mediaState: MediaState,
    isPermissionGranted: Boolean,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onRequestPermission: () -> Unit,
    musicNoteRotation: () -> Float = { 0f },
    musicNoteScale: () -> Float = { 1f },
    musicNotePlayingFactor: () -> Float = { 0f },
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
                            .graphicsLayer {
                                rotationZ = musicNoteRotation() * musicNotePlayingFactor()
                                scaleX = 1f + (musicNoteScale() - 1f) * musicNotePlayingFactor()
                                scaleY = 1f + (musicNoteScale() - 1f) * musicNotePlayingFactor()
                            }
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
                    modifier = Modifier
                        .size(width = 28.dp, height = 36.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                    ) {
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
