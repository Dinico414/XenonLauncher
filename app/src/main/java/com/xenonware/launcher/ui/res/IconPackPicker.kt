package com.xenonware.launcher.ui.res

//import com.xenon.mylibrary.res.XenonDialog
import android.content.pm.ResolveInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.xenon.mylibrary.res.XenonDialog
import com.xenonware.launcher.R
import com.xenonware.launcher.util.getAllIconPackIcons
import com.xenonware.launcher.util.loadIconFromPack
import com.xenonware.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun IconPackPicker(
    viewModel: LauncherViewModel,
    onIconSelect: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedPack by remember { mutableStateOf<ResolveInfo?>(null) }
    var selectedIconResName by remember { mutableStateOf<String?>(null) }
    val iconPacks = remember { viewModel.getInstalledIconPacks() }
    val context = LocalContext.current
    val pm = context.packageManager

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    XenonDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = if (selectedPack == null) stringResource(R.string.select_icon_pack) else selectedPack!!.loadLabel(pm)
            .toString(),
        confirmButtonText = if (selectedPack != null) stringResource(R.string.ok) else null,
        onConfirmButtonClick = {
            selectedIconResName?.let { resName ->
                selectedPack?.activityInfo?.packageName?.let { pkg ->
                    onIconSelect(pkg, resName)
                }
            }
        },
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = if (selectedPack == null) 12.dp else 0.dp),
        actionButton1Text = if (selectedPack != null) stringResource(R.string.back) else null,
        onActionButton1Click = {
            selectedPack = null
            selectedIconResName = null
        },
        contentManagesScrolling = true,
        externalShowTopDivider = if (selectedPack == null) listState.canScrollBackward else gridState.canScrollBackward,
        externalShowBottomDivider = if (selectedPack == null) listState.canScrollForward else gridState.canScrollForward,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (selectedPack == null) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(iconPacks) { pack ->
                        ListItem(
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { selectedPack = pack },
                            leadingContent = {
                                Image(
                                    bitmap = pack.loadIcon(pm).toBitmap().asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp)
                                )
                            },
                            trailingContent = null,
                            overlineContent = null,
                            supportingContent = null,
                            colors = ListItemDefaults.colors(),
                            elevation = ListItemDefaults.elevation(ListItemDefaults.Elevation),
                            content = { Text(pack.loadLabel(pm).toString()) },
                        )
                    }
                }
            } else {
                IconGrid(
                    packageName = selectedPack!!.activityInfo.packageName,
                    selectedIconResName = selectedIconResName,
                    onIconResNameSelect = { selectedIconResName = it },
                    state = gridState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun GlobalIconPackPicker(
    iconPacks: List<ResolveInfo>,
    selectedPackage: String?,
    onPackSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val pm = LocalContext.current.packageManager
    val listState = rememberLazyListState()

    XenonDialog(
        properties = DialogProperties(usePlatformDefaultWidth = true),
        onDismissRequest = onDismiss,
        title = stringResource(R.string.select_icon_pack),
        contentManagesScrolling = true,
        externalShowTopDivider = listState.canScrollBackward,
        externalShowBottomDivider = listState.canScrollForward,
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 12.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                ListItem(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onPackSelect(null) },
                    leadingContent = {
                        Icon(Icons.Rounded.Block, null, modifier = Modifier.size(40.dp))
                    },
                    content = { Text(stringResource(R.string.system_default)) },
                    overlineContent = null,
                    supportingContent = null,
                    trailingContent = null,
                    colors = ListItemDefaults.colors(),
                    elevation = ListItemDefaults.elevation(ListItemDefaults.Elevation),
                )
            }
            items(iconPacks) { pack ->
                val pkgName = pack.activityInfo.packageName
                ListItem(
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onPackSelect(pkgName) },
                    leadingContent = {
                        Image(
                            bitmap = pack.loadIcon(pm).toBitmap().asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    trailingContent = {
                        if (pkgName == selectedPackage) {
                            Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    content = { Text(pack.loadLabel(pm).toString()) },
                    overlineContent = null,
                    supportingContent = null,
                    colors = ListItemDefaults.colors(),
                    elevation = ListItemDefaults.elevation(ListItemDefaults.Elevation),
                )
            }
        }
    }
}

@Composable
fun IconGrid(
    packageName: String,
    selectedIconResName: String?,
    onIconResNameSelect: (String) -> Unit,
    state: LazyGridState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val allIcons = remember { mutableStateListOf<String>() }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(packageName) {
        isLoading = true
        val icons = withContext(Dispatchers.IO) {
            getAllIconPackIcons(context, packageName)
        }
        allIcons.clear()
        allIcons.addAll(icons)
        isLoading = false
    }

    val filteredIcons = remember(searchQuery, allIcons.size) {
        if (searchQuery.isEmpty()) {
            allIcons
        } else {
            allIcons.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(64.dp),
        state = state,
        modifier = modifier.heightIn(max = 500.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.search_icons)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(16.dp))
            }
        }

        if (isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (filteredIcons.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(if (searchQuery.isEmpty()) R.string.no_icons_found else R.string.no_results),
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(filteredIcons) { resName ->
                val icon = remember(resName) {
                    loadIconFromPack(context, packageName, resName)
                }
                if (icon != null) {
                    val isSelected = resName == selectedIconResName
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .border(
                                width = 2.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onIconResNameSelect(resName) }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = icon.toBitmap().asImageBitmap(),
                            contentDescription = resName,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
    }
}
