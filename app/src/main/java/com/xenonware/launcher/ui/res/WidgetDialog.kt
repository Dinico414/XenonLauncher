package com.xenonware.launcher.ui.res

import android.appwidget.AppWidgetProviderInfo
import android.util.Log
import android.util.LruCache
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.xenonware.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val bitmapCache = LruCache<String, ImageBitmap>(100)

@Composable
fun WidgetSelectorDialog(
    installedWidgets: Map<LauncherViewModel.AppWidgetGroup, List<LauncherViewModel.WidgetPickerItemData>>,
    onDismiss: () -> Unit,
    onWidgetSelected: (LauncherViewModel.WidgetPickerItemData) -> Unit
) {
    val expandedGroups = remember { mutableStateListOf<String>() }

    // Pre-calculate flattened list for performance
    val flattenedList = remember(installedWidgets, expandedGroups.size) {
        val list = mutableListOf<PickerListItem>()
        installedWidgets.forEach { (group, items) ->
            list.add(PickerListItem.Header(group))
            if (expandedGroups.contains(group.appName)) {
                val shortcuts = items.filter { !it.isWidget }
                val widgets = items.filter { it.isWidget }
                
                if (shortcuts.isNotEmpty()) {
                    list.add(PickerListItem.ShortcutsGrid(group.appName, shortcuts))
                }
                
                widgets.forEach { list.add(PickerListItem.Widget(it)) }
            }
        }
        list
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(32.dp),
            color = colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Widgets",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(flattenedList, key = { it.key }) { item ->
                        when (item) {
                            is PickerListItem.Header -> {
                                val isExpanded = expandedGroups.contains(item.group.appName)
                                CategoryHeader(
                                    group = item.group,
                                    isExpanded = isExpanded,
                                    onToggle = {
                                        if (isExpanded) expandedGroups.remove(item.group.appName)
                                        else expandedGroups.add(item.group.appName)
                                    }
                                )
                            }
                            is PickerListItem.ShortcutsGrid -> {
                                ShortcutsGrid(item.shortcuts, onWidgetSelected)
                            }
                            is PickerListItem.Widget -> {
                                WidgetPickerItem(item.data, onWidgetSelected)
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class PickerListItem {
    abstract val key: String
    data class Header(val group: LauncherViewModel.AppWidgetGroup) : PickerListItem() {
        override val key: String = "header_${group.appName}"
    }
    data class ShortcutsGrid(val appName: String, val shortcuts: List<LauncherViewModel.WidgetPickerItemData>) : PickerListItem() {
        override val key: String = "shortcuts_${appName}"
    }
    data class Widget(val data: LauncherViewModel.WidgetPickerItemData) : PickerListItem() {
        override val key: String = data.id
    }
}

@Composable
fun ShortcutsGrid(
    shortcuts: List<LauncherViewModel.WidgetPickerItemData>,
    onSelected: (LauncherViewModel.WidgetPickerItemData) -> Unit
) {
    val rowConfigs = remember(shortcuts.size) {
        when (shortcuts.size) {
            1 -> listOf(1)
            2 -> listOf(2)
            3 -> listOf(3)
            4 -> listOf(2, 2)
            5 -> listOf(3, 2)
            6 -> listOf(3, 3)
            else -> {
                val list = mutableListOf<Int>()
                var remaining = shortcuts.size
                while (remaining > 0) {
                    if (remaining == 4) {
                        list.add(2)
                        list.add(2)
                        remaining = 0
                    } else {
                        val count = minOf(3, remaining)
                        list.add(count)
                        remaining -= count
                    }
                }
                list
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var currentIndex = 0
        rowConfigs.forEach { rowCount ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // To center the items while keeping their size consistent (1/3 of row width)
                // we add equal weight spacers on both sides.
                val sideSpaceWeight = (3f - rowCount) / 2f
                if (sideSpaceWeight > 0f) {
                    Spacer(Modifier.weight(sideSpaceWeight))
                }

                repeat(rowCount) {
                    val item = shortcuts[currentIndex++]
                    Box(modifier = Modifier.weight(1f)) {
                        ShortcutPickerItem(item, onSelected)
                    }
                }

                if (sideSpaceWeight > 0f) {
                    Spacer(Modifier.weight(sideSpaceWeight))
                }
            }
        }
    }
}

@Composable
fun ShortcutPickerItem(
    item: LauncherViewModel.WidgetPickerItemData,
    onSelected: (LauncherViewModel.WidgetPickerItemData) -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val density = LocalDensity.current

    var bitmap by remember(item.id) { mutableStateOf<ImageBitmap?>(bitmapCache.get(item.id)) }

    LaunchedEffect(item.id) {
        if (bitmap != null) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val drawable = item.shortcutInfo?.loadIcon(pm)
                val result = drawable?.let { d ->
                    val sizePx = (48 * density.density).toInt()
                    d.toBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
                }
                result?.let { bitmapCache.put(item.id, it) }
                bitmap = result
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelected(item) }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier.size(44.dp)
            )
        } else {
            Box(
                modifier = Modifier.size(44.dp).background(colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Apps, null, modifier = Modifier.size(24.dp), tint = colorScheme.onSurfaceVariant)
            }
        }
        Text(
            item.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun CategoryHeader(
    group: LauncherViewModel.AppWidgetGroup,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(16.dp),
        color = if (isExpanded) colorScheme.secondaryContainer.copy(alpha = 0.4f) else colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp)
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

            Text(
                group.appName, 
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, 
                modifier = Modifier.weight(1f)
            )

            Icon(
                if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                null,
                tint = colorScheme.onSurfaceVariant
            )
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

    var bitmap by remember(item.id) { mutableStateOf<ImageBitmap?>(bitmapCache.get(item.id)) }

    LaunchedEffect(item.id) {
        if (bitmap != null) return@LaunchedEffect
        
        withContext(Dispatchers.IO) {
            try {
                val displayMetrics = context.resources.displayMetrics
                val targetDpi = displayMetrics.densityDpi
                
                var source = "None"
                var drawable: android.graphics.drawable.Drawable? = null

                if (item.isWidget && item.widgetInfo != null) {
                    val info = item.widgetInfo
                    
                    // 1. On Android 12+, try previewLayout FIRST
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && info.previewLayout != 0) {
                        try {
                            source = "PreviewLayout"
                            withContext(Dispatchers.Main) {
                                val remoteContext = context.createPackageContext(info.provider.packageName, 0)
                                val inflater = android.view.LayoutInflater.from(remoteContext)
                                val view = inflater.inflate(info.previewLayout, null, false)
                                
                                val widthPx = info.minWidth.coerceAtLeast(200)
                                val heightPx = info.minHeight.coerceAtLeast(200)
                                
                                view.measure(
                                    android.view.View.MeasureSpec.makeMeasureSpec(widthPx, android.view.View.MeasureSpec.EXACTLY),
                                    android.view.View.MeasureSpec.makeMeasureSpec(heightPx, android.view.View.MeasureSpec.EXACTLY)
                                )
                                view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                                
                                val canvasBitmap = android.graphics.Bitmap.createBitmap(
                                    view.measuredWidth.coerceAtLeast(1), 
                                    view.measuredHeight.coerceAtLeast(1), 
                                    android.graphics.Bitmap.Config.ARGB_8888
                                )
                                val canvas = android.graphics.Canvas(canvasBitmap)
                                view.draw(canvas)
                                drawable = android.graphics.drawable.BitmapDrawable(context.resources, canvasBitmap)
                            }
                        } catch (e: Exception) {
                            Log.w("WidgetType", "Failed previewLayout for ${item.label}: ${e.message}")
                        }
                    }

                    // 2. Fallback to actual preview image
                    if (drawable == null) {
                        val preview = info.loadPreviewImage(context, targetDpi)
                        if (preview != null) {
                            source = "Preview"
                            drawable = preview
                        }
                    }
                    
                    // 3. Fallback to app icon
                    if (drawable == null) {
                        source = "Icon"
                        drawable = info.loadIcon(context, targetDpi)
                    }
                } else if (item.shortcutInfo != null) {
                    source = "ShortcutIcon"
                    drawable = item.shortcutInfo.loadIcon(pm)
                }

                val result = drawable?.let { d ->
                    val targetWidthDp = 100f
                    val targetWidthPx = (targetWidthDp * density.density).toInt()
                    
                    val intrinsicW = d.intrinsicWidth.coerceAtLeast(1)
                    val intrinsicH = d.intrinsicHeight.coerceAtLeast(1)
                    val aspect = intrinsicW.toFloat() / intrinsicH.toFloat()
                    
                    val w = targetWidthPx
                    val h = (targetWidthPx / aspect).toInt().coerceIn(10, 400)
                    
                    Log.d("WidgetType", "ASYNC LOAD [${item.label}] from $source. Size: ${w}x${h}")
                    
                    val b = d.toBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap()
                    bitmapCache.put(item.id, b)
                    b
                }
                
                bitmap = result
            } catch (e: Exception) {
                Log.e("WidgetType", "Error loading [${item.label}]: ${e.message}")
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
                bitmap = bitmap!!,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
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
                val isPixelScale = info.minWidth > 450 
                val w = if (isPixelScale) info.minWidth / density.density else info.minWidth.toFloat()
                val h = if (isPixelScale) info.minHeight / density.density else info.minHeight.toFloat()
                
                cols = Math.max(1, ((w + 20) / 60).toInt())
                rows = Math.max(1, ((h + 20) / 60).toInt())
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
