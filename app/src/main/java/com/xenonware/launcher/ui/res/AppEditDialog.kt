package com.xenonware.launcher.ui.res

//import com.xenon.mylibrary.res.XenonDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.xenon.mylibrary.res.XenonColorPicker
import com.xenon.mylibrary.res.XenonDialog
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.model.AppOverride
import com.xenonware.launcher.util.generateCustomIcon
import com.xenonware.launcher.util.loadIconFromPack
import com.xenonware.launcher.viewmodel.LauncherViewModel
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun AppEditDialog(
    app: AppInfo,
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val currentOverride =
        remember(app.packageName) { viewModel.getAppOverride(app.packageName) ?: AppOverride() }
    val currentShape by viewModel.drawerIconShape.collectAsState()

    var name by remember { mutableStateOf(app.label) }
    var zoom by remember { mutableFloatStateOf(currentOverride.zoom) }
    var bgColor by remember {
        mutableStateOf(currentOverride.backgroundColor?.let { Color(it) })
    }
    var borderColor by remember {
        mutableStateOf(currentOverride.borderColor?.let { Color(it) } ?: Color.White)
    }
    var borderWidth by remember { mutableFloatStateOf(currentOverride.borderWidth) }

    var iconPackPackage by remember { mutableStateOf(currentOverride.iconPackPackage) }
    var iconResName by remember { mutableStateOf(currentOverride.iconResourceName) }

    val pm = context.packageManager
    val originalIcon = remember(app.packageName) {
        try {
            pm.getActivityIcon(pm.getLaunchIntentForPackage(app.packageName)!!.component!!)
        } catch (e: Exception) {
            app.icon
        }
    }

    val baseIcon = remember(originalIcon, iconPackPackage, iconResName) {
        if (iconPackPackage != null && iconResName != null) {
            loadIconFromPack(context, iconPackPackage!!, iconResName!!) ?: originalIcon
        } else {
            originalIcon
        }
    }

    val previewIcon = remember(baseIcon, zoom, bgColor, borderColor, borderWidth, currentShape) {
        generateCustomIcon(
            context, baseIcon, AppOverride(
                zoom = zoom,
                backgroundColor = bgColor?.toArgb(),
                borderColor = borderColor.toArgb(),
                borderWidth = borderWidth
            ), currentShape
        )
    }

    var showIconPackPicker by remember { mutableStateOf(false) }

    XenonDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
        title = "Edit App",
        confirmButtonText = "Save",
        onConfirmButtonClick = {
            viewModel.updateAppOverride(
                app.packageName, AppOverride(
                    customName = if (name == app.name) null else name,
                    iconPackPackage = iconPackPackage,
                    iconResourceName = iconResName,
                    zoom = zoom,
                    backgroundColor = bgColor?.toArgb(),
                    borderColor = borderColor.toArgb(),
                    borderWidth = borderWidth
                )
            )
            onDismiss()
        },
        actionButton1Text = "Reset",
        onActionButton1Click = {
            viewModel.resetAppOverride(app.packageName)
            name = app.name
            zoom = 1.0f
            bgColor = null
            borderColor = Color.White
            borderWidth = 0f
            iconPackPackage = null
            iconResName = null
        },
        actionButton1ContentColor = MaterialTheme.colorScheme.primary,
        contentManagesScrolling = false
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Preview (56dp like drawer)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(currentShape.getShape())
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                    contentAlignment = Alignment.Center
                ) {
                    previewIcon?.let {
                        Image(
                            bitmap = it.toBitmap().asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("App Name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                IconButton(onClick = { showIconPackPicker = true }) {
                    Icon(Icons.Rounded.Collections, "Icon Pack", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tabs/Sections for Edit
            var selectedTab by remember { mutableIntStateOf(0) }
            SecondaryTabRow(
                selectedTabIndex = selectedTab, containerColor = Color.Transparent, divider = {}) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text(
                        "Style", Modifier.padding(8.dp), style = MaterialTheme.typography.bodyMedium
                    )
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text(
                        "Colors",
                        Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedTab == 0) {
                Column {
                    Text(
                        "Zoom: ${(zoom * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = zoom, onValueChange = { zoom = it }, valueRange = 0.5f..2.0f
                    )

                    Text(
                        "Border Width: ${borderWidth.toInt()}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = borderWidth,
                        onValueChange = { borderWidth = it },
                        valueRange = 0f..20f
                    )
                }
            } else {
                ColorSelectionSection(
                    bgColor = bgColor ?: Color.White,
                    onBgColorChange = { bgColor = it },
                    borderColor = borderColor,
                    onBorderColorChange = { borderColor = it })
            }
        }
    }

    if (showIconPackPicker) {
        IconPackPicker(viewModel = viewModel, onIconSelect = { pkg, res ->
            iconPackPackage = pkg
            iconResName = res
            showIconPackPicker = false
        }, onDismiss = { showIconPackPicker = false })
    }
}

@Composable
fun ColorSelectionSection(
    bgColor: Color,
    onBgColorChange: (Color) -> Unit,
    borderColor: Color,
    onBorderColorChange: (Color) -> Unit,
) {
    var editingBorderColor by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val bgColorActive = !editingBorderColor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp, bottomStart = 20.dp, topEnd = 4.dp, bottomEnd = 4.dp
                        )
                    )
                    .background(if (bgColorActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable { editingBorderColor = false }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center) {
                Text(
                    "Background",
                    color = if (bgColorActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            val borderColorActive = editingBorderColor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(
                        RoundedCornerShape(
                            topEnd = 20.dp, bottomEnd = 20.dp, topStart = 4.dp, bottomStart = 4.dp
                        )
                    )
                    .background(if (borderColorActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable { editingBorderColor = true }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center) {
                Text(
                    "Border",
                    color = if (borderColorActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        key(editingBorderColor) {
            XenonColorPicker(
                color = if (editingBorderColor) borderColor else bgColor,
                onColorChanged = if (editingBorderColor) onBorderColorChange else onBgColorChange
            )
        }
    }
}
