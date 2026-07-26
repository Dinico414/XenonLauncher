package com.xenonware.launcher.ui.res

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.viewmodel.LauncherViewModel

@Composable
fun ShortcutConfigDialog(
    type: LauncherViewModel.ShortcutType,
    apps: List<AppInfo>,
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var linkValue by remember { mutableStateOf(if (initialValue.startsWith("link:")) initialValue.substring(5) else "") }
    var selectedPackage by remember { mutableStateOf(if (initialValue.startsWith("app:")) initialValue.substring(4) else "") }
    var selectionMode by remember { 
        mutableStateOf(if (initialValue.startsWith("app:")) "app" else "link") 
    }

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
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = "Configure ${type.name.lowercase().replaceFirstChar { it.uppercase() }} Shortcut",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { selectionMode = "link" }
                ) {
                    RadioButton(
                        selected = selectionMode == "link",
                        onClick = { selectionMode = "link" }
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = linkValue,
                        onValueChange = { 
                            linkValue = it
                            selectionMode = "link"
                        },
                        label = { Text("Link") },
                        placeholder = { Text("https://...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectionMode == "link"
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Select App",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Top
                ) {
                    items(apps) { app ->
                        val isSelected = selectionMode == "app" && selectedPackage == app.packageName
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(100.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                    else Color.Transparent
                                )
                                .clickable { 
                                    selectedPackage = app.packageName
                                    selectionMode = "app"
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { 
                                        selectedPackage = app.packageName
                                        selectionMode = "app"
                                    }
                                )
                                
                                if (app.icon != null) {
                                    Image(
                                        bitmap = app.icon.toBitmap().asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                
                                Text(
                                    text = app.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectionMode == "link" && linkValue.isNotEmpty()) {
                                onSave("link:$linkValue")
                            } else if (selectionMode == "app" && selectedPackage.isNotEmpty()) {
                                onSave("app:$selectedPackage")
                            } else {
                                onSave("")
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
