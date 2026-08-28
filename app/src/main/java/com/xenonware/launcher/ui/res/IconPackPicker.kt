package com.xenonware.launcher.ui.res

import android.content.pm.ResolveInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.xenon.mylibrary.res.XenonDialog
//import com.xenon.mylibrary.res.XenonDialog
import com.xenonware.launcher.R
import com.xenonware.launcher.viewmodel.LauncherViewModel

@Composable
fun IconPackPicker(
    viewModel: LauncherViewModel,
    onIconSelect: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedPack by remember { mutableStateOf<ResolveInfo?>(null) }
    val iconPacks = remember { viewModel.getInstalledIconPacks() }
    val context = LocalContext.current
    val pm = context.packageManager

    XenonDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = if (selectedPack == null) stringResource(R.string.select_icon_pack) else selectedPack!!.loadLabel(pm)
            .toString(),
        contentManagesScrolling = true
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (selectedPack != null) {
                IconButton(onClick = { selectedPack = null }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back))
                }
            }

            if (selectedPack == null) {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(iconPacks) { pack ->
                        ListItem(
                            modifier = Modifier.clickable { selectedPack = pack },
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
                    onIconSelect = onIconSelect,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun IconGrid(
    packageName: String,
    onIconSelect: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {

    Column(modifier = modifier) {
        var searchQuery by remember { mutableStateOf("") }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(stringResource(R.string.resource_name_label)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        if (searchQuery.isNotEmpty()) {
            Button(
                onClick = { onIconSelect(packageName, searchQuery) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.use_this_name))
            }
        }

        Text(
            stringResource(R.string.icon_pack_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
