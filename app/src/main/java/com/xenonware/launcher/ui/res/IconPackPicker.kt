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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
//import com.xenon.mylibrary.res.XenonDialog
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
        title = if (selectedPack == null) "Select Icon Pack" else selectedPack!!.loadLabel(pm)
            .toString(),
        contentManagesScrolling = true
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (selectedPack != null) {
                IconButton(onClick = { selectedPack = null }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                }
            }

            if (selectedPack == null) {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(iconPacks) { pack ->
                        ListItem(
                            headlineContent = { Text(pack.loadLabel(pm).toString()) },
                            leadingContent = {
                                Image(
                                    bitmap = pack.loadIcon(pm).toBitmap().asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp)
                                )
                            },
                            modifier = Modifier.clickable { selectedPack = pack })
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
    val context = LocalContext.current
    val icons = remember(packageName) {
        try {
            val res = context.packageManager.getResourcesForApplication(packageName)
            // This is a hacky way to find icons. In a real launcher, we'd parse appfilter.xml
            // Here I'll try to find common resource names or just list some.
            // For a demo, I'll look for drawables that start with common prefixes.
            val list = mutableListOf<String>()
            // We can't easily list all resources. 
            // I'll try to find at least the ones that match some common apps for demo purposes
            // Or just a placeholder message if we can't list them easily.
            // Actually, I'll try to iterate IDs if I can find the range, but that's risky.

            // For now, I'll just show a "Select by name" or a few found ones.
            // In a real app, I'd use a library or a background task to parse the APK.
            listOf("icon", "app_icon", "logo")
        } catch (e: Exception) {
            emptyList<String>()
        }
    }

    Column(modifier = modifier) {
        var searchQuery by remember { mutableStateOf("") }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Resource Name (e.g. chrome)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        if (searchQuery.isNotEmpty()) {
            Button(
                onClick = { onIconSelect(packageName, searchQuery) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Use this name")
            }
        }

        Text(
            "Note: Manually enter the resource name from the icon pack (e.g., 'chrome', 'settings') until full browser is implemented.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
