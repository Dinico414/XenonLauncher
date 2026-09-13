package com.xenonware.launcher.ui.res.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.xenon.mylibrary.values.ExtraBigSpacing
import com.xenon.mylibrary.values.ExtraLargerPadding
import com.xenon.mylibrary.values.IconSizeMedium
import com.xenon.mylibrary.values.LargeMediumPadding
import com.xenon.mylibrary.values.LargestSpacer
import com.xenon.mylibrary.values.MediumLargeCornerRadius
import com.xenonware.launcher.model.SearchHistoryEntry
import com.xenonware.launcher.model.SearchHistoryType
import com.xenonware.launcher.ui.res.ContactAvatar

@Composable
fun SearchHistoryItem(entry: SearchHistoryEntry, onClick: (SearchHistoryEntry) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(entry) }
            .padding(LargeMediumPadding)
    ) {
        Icon(
            Icons.Rounded.History,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(ExtraLargerPadding)
        )
        
        Spacer(Modifier.width(LargestSpacer))

        if (entry.type == SearchHistoryType.CONTACT || entry.type == SearchHistoryType.FILE) {
            Box(modifier = Modifier.size(ExtraBigSpacing), contentAlignment = Alignment.Center) {
                when (entry.type) {
                    SearchHistoryType.CONTACT -> {
                        if (!entry.iconUri.isNullOrEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(entry.iconUri),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape)
                            )
                        } else {
                            ContactAvatar(name = entry.label, modifier = Modifier.fillMaxSize())
                        }
                    }
                    SearchHistoryType.FILE -> {
                        val mimeType = entry.iconUri ?: ""
                        val isTextFile = mimeType.startsWith("text/")
                        val fileTypeInfo = when {
                            mimeType.startsWith("image/") -> Icons.Rounded.Image to Color(0xFFB39DDB)
                            mimeType.startsWith("video/") -> Icons.Rounded.Movie to Color(0xFFEF5350)
                            mimeType.startsWith("audio/") -> Icons.Rounded.AudioFile to Color(0xFFFFB74D)
                            mimeType == "application/vnd.android.package-archive" -> Icons.Rounded.Android to Color(0xFF3DDC84)
                            mimeType == "application/pdf" || isTextFile -> Icons.Rounded.Description to Color(0xFF81D4FA)
                            mimeType.contains("zip") || mimeType.contains("rar") || mimeType.contains("7z") -> Icons.Rounded.FolderZip to Color(0xFF9E9E9E)
                            else -> Icons.AutoMirrored.Rounded.InsertDriveFile to colorScheme.surfaceContainerHighest
                        }
                        val (fileIcon, bgColor) = fileTypeInfo

                        Surface(
                            modifier = Modifier.size(ExtraBigSpacing),
                            shape = RoundedCornerShape(MediumLargeCornerRadius),
                            color = bgColor.copy(alpha = 0.8f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    fileIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(IconSizeMedium),
                                    tint = if (bgColor == colorScheme.surfaceContainerHighest) colorScheme.onSurfaceVariant else Color.White
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(LargestSpacer))
        }

        Column {
            Text(
                entry.label,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            entry.subLabel?.let {
                Text(
                    it,
                    color = colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
