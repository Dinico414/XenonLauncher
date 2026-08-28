package com.xenonware.launcher.ui.res.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.rememberAsyncImagePainter
import com.xenonware.launcher.R
import com.xenonware.launcher.model.SearchResult
import com.xenonware.launcher.ui.res.ContactAvatar

@Composable
fun SearchResultItem(
    result: SearchResult,
    onClick: (SearchResult) -> Unit,
    onLongClick: ((SearchResult, Offset) -> Unit)? = null,
    iconShape: com.xenonware.launcher.ui.res.IconShape = com.xenonware.launcher.ui.res.IconShape.Circle,
    showShadow: Boolean = false
) {
    var itemPos by remember { mutableStateOf(Offset.Zero) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainer.copy(alpha = 0.8f))
            .onGloballyPositioned { itemPos = it.positionInRoot() }
            .then(
                if (onLongClick != null) {
                    Modifier
                        .pointerInput(result) {
                            detectTapGestures(onTap = { onClick(result) })
                        }
                        .pointerInput(result) {
                            var tempOffset = Offset.Zero
                            detectDragGesturesAfterLongPress(
                                onDragStart = { offset -> tempOffset = offset },
                                onDrag = { change, _ -> change.consume() },
                                onDragEnd = { onLongClick(result, itemPos + tempOffset) }
                            )
                        }
                } else {
                    Modifier.clickable { onClick(result) }
                }
            )
            .padding(12.dp)
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            when (result) {
                is SearchResult.App -> {
                    result.appInfo.icon?.let { icon ->
                        val shape = iconShape.getShape()
                        Image(
                            bitmap = icon.toBitmap().asImageBitmap(), 
                            contentDescription = null, 
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (showShadow) Modifier.shadow(4.dp, shape) else Modifier)
                                .clip(shape)
                        )
                    }
                }
                is SearchResult.Contact -> {
                    if (result.photoUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(result.photoUri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        ContactAvatar(name = result.name, modifier = Modifier.fillMaxSize())
                    }
                }
                is SearchResult.File -> {
                    val isTextFile = result.mimeType.startsWith("text/")
                    val isPdf = result.mimeType == "application/pdf"
                    val fileTypeInfo = when {
                        result.mimeType.startsWith("image/") -> Icons.Rounded.Image to Color(0xFFB39DDB)
                        result.mimeType.startsWith("video/") -> Icons.Rounded.Movie to Color(0xFFEF5350)
                        result.mimeType.startsWith("audio/") -> Icons.Rounded.AudioFile to Color(0xFFFFB74D)
                        result.mimeType == "application/vnd.android.package-archive" -> Icons.Rounded.Android to Color(0xFF3DDC84)
                        isPdf -> Icons.Rounded.Description to Color(0xFFD32F2F)
                        isTextFile -> Icons.Rounded.Description to Color(0xFF81D4FA)
                        result.mimeType.contains("zip") || result.mimeType.contains("rar") || result.mimeType.contains("7z") -> Icons.Rounded.FolderZip to Color(0xFF9E9E9E)
                        else -> Icons.AutoMirrored.Rounded.InsertDriveFile to colorScheme.surfaceContainerHighest
                    }
                    val (fileIcon, bgColor) = fileTypeInfo

                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (result.preview != null && !isTextFile) Color.Transparent else bgColor.copy(alpha = 0.8f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (result.preview != null && !isTextFile) {
                                Image(
                                    bitmap = result.preview.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.4f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        fileIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = bgColor
                                    )
                                }
                            } else {
                                Icon(
                                    fileIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = if (bgColor == colorScheme.surfaceContainerHighest) colorScheme.onSurfaceVariant else Color.White
                                )
                            }
                        }
                    }
                }
                is SearchResult.Web -> {
                    Icon(if (result.isUrl) Icons.Rounded.Language else Icons.Rounded.Search, null, modifier = Modifier.size(32.dp))
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            val title = when (result) {
                is SearchResult.App -> result.appInfo.label
                is SearchResult.Contact -> result.name
                is SearchResult.File -> result.name
                is SearchResult.Web -> if (result.isUrl) stringResource(R.string.open_website) else stringResource(R.string.web_search)
            }
            val subtitle = when (result) {
                is SearchResult.App -> result.appInfo.packageName
                is SearchResult.Contact -> result.phoneNumber
                is SearchResult.File -> result.path
                is SearchResult.Web -> result.query
            }
            Text(
                title,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}