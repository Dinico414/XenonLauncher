package com.xenonware.launcher.ui.res

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.xenonware.launcher.viewmodel.LauncherViewModel

@Composable
fun WidgetSelectorDialog(
    installedWidgets: Map<LauncherViewModel.AppWidgetGroup, List<LauncherViewModel.WidgetPickerItemData>>,
    onDismiss: () -> Unit,
    onWidgetSelected: (LauncherViewModel.WidgetPickerItemData) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            color = colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Widgets",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    installedWidgets.forEach { (group, widgets) ->
                        item {
                            var expanded by remember { mutableStateOf(false) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .clickable { expanded = !expanded }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (group.icon != null) {
                                        Image(
                                            bitmap = group.icon.toBitmap().asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    } else {
                                        Icon(Icons.Rounded.Apps, null, modifier = Modifier.size(32.dp))
                                    }

                                    Text(group.appName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))

                                    Icon(
                                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        null
                                    )
                                }

                                if (expanded) {
                                    Spacer(Modifier.height(8.dp))
                                    widgets.forEach { item ->
                                        WidgetPickerItem(item, onWidgetSelected)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetPickerItem(
    item: LauncherViewModel.WidgetPickerItemData,
    onSelected: (LauncherViewModel.WidgetPickerItemData) -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val density = LocalDensity.current

    val preview = remember(item) {
        try {
            if (item.isWidget && item.widgetInfo != null) {
                item.widgetInfo.loadPreviewImage(context, density.density.toInt()) 
                    ?: item.widgetInfo.loadIcon(context, density.density.toInt())
            } else if (item.shortcutInfo != null) {
                item.shortcutInfo.loadIcon(pm)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    val bitmap = remember(preview) {
        preview?.let {
            try {
                val w = if (it.intrinsicWidth > 0) it.intrinsicWidth else (96 * density.density).toInt()
                val h = if (it.intrinsicHeight > 0) it.intrinsicHeight else (64 * density.density).toInt()
                it.toBitmap(w, h).asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected(item) }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .then(if (item.isWidget) Modifier.aspectRatio(1.5f) else Modifier.size(56.dp))
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (item.isWidget) Icons.Rounded.Widgets else Icons.Rounded.Apps,
                    null,
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            item.label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (item.isWidget && item.widgetInfo != null) {
            val info = item.widgetInfo
            
            var cols = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                info.targetCellWidth.coerceAtLeast(0)
            } else 0
            var rows = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                info.targetCellHeight.coerceAtLeast(0)
            } else 0
            
            if (cols <= 0 || rows <= 0) {
                // Standard formula: (size + 30) / 70
                // If the values are very large (like the 14x6 case), we treat them as pixels and convert to DP.
                // Otherwise we assume they are already DP.
                val isPixelScale = info.minWidth > 450 
                val w = if (isPixelScale) info.minWidth / density.density else info.minWidth.toFloat()
                val h = if (isPixelScale) info.minHeight / density.density else info.minHeight.toFloat()
                
                cols = Math.max(1, ((w + 30) / 70).toInt())
                rows = Math.max(1, ((h + 30) / 70).toInt())
            }
            
            Text(
                "${cols}x${rows}",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            Text(
                "Shortcut",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
