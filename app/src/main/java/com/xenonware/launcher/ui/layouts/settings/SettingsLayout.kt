package com.xenonware.launcher.ui.layouts.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.res.SettingsSwitchTile
import com.xenon.mylibrary.res.SettingsTile
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.NoSpacing
import com.xenonware.launcher.ui.res.IconShape
import com.xenonware.launcher.ui.res.ShortcutConfigDialog
import com.xenonware.launcher.ui.res.XenonSingleChoiceButtonGroup
import com.xenonware.launcher.viewmodel.LauncherViewModel
import com.xenonware.launcher.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SettingsLayout(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    hazeState: HazeState
) {
    val currentThemeTitle by viewModel.currentThemeTitle.collectAsState()
    val blackedOutEnabled by viewModel.blackedOutModeEnabled.collectAsState()
    val isGridLayout by viewModel.isGridLayout.collectAsState()
    val openKeyboard by viewModel.openKeyboard.collectAsState()
    val advancedSearchEnabled by viewModel.advancedSearchEnabled.collectAsState()
    val showHiddenAppsInSearch by viewModel.showHiddenAppsInSearch.collectAsState()
    val hiddenApps by viewModel.hiddenApps.collectAsState()
    val dockSafeDrawIme by viewModel.dockSafeDrawIme.collectAsState()
    
    val apps by viewModel.apps.collectAsState()
    val timeShortcut by viewModel.timeShortcut.collectAsState()
    val dateShortcut by viewModel.dateShortcut.collectAsState()
    val weatherShortcut by viewModel.weatherShortcut.collectAsState()
    
    var configShortcutType by remember { mutableStateOf<LauncherViewModel.ShortcutType?>(null) }
    var showHiddenAppsDialog by remember { mutableStateOf(false) }

    val innerRadius = 4.dp
    val outerRadius = 24.dp
    val tileColor = MaterialTheme.colorScheme.surfaceBright
    
    val topShape = RoundedCornerShape(topStart = outerRadius, topEnd = outerRadius, bottomStart = innerRadius, bottomEnd = innerRadius)
    val middleShape = RoundedCornerShape(innerRadius)
    val bottomShape = RoundedCornerShape(topStart = innerRadius, topEnd = innerRadius, bottomStart = outerRadius, bottomEnd = outerRadius)
    val standaloneShape = RoundedCornerShape(outerRadius)

    ActivityScreen(
        titleText = "Settings",
        expandable = true,
        navigationIconStartPadding = MediumPadding,
        navigationIconPadding = MediumPadding,
        navigationIconSpacing = NoSpacing,
        navigationIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(24.dp)
            )
        },
        onNavigationIconClick = onNavigateBack,
        modifier = Modifier.hazeSource(hazeState),
        content = { _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(LargestPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "PERSONALIZATION", 
                    color = MaterialTheme.colorScheme.primary, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold, 
                    modifier = Modifier.padding(start = 12.dp)
                )
                
                Column {
                    SettingsSwitchTile(
                        title = "Grid Layout",
                        subtitle = if (isGridLayout) "Using grid view" else "Using list view",
                        checked = isGridLayout,
                        onCheckedChange = { viewModel.setGridLayout(it) },
                        icon = { Icon(Icons.Default.GridView, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        backgroundColor = tileColor,
                        shape = topShape
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsSwitchTile(
                        title = "Open Keyboard",
                        subtitle = "Focus search at top of drawer",
                        checked = openKeyboard,
                        onCheckedChange = { viewModel.setOpenKeyboard(it) },
                        icon = { Icon(Icons.Default.Keyboard, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        backgroundColor = tileColor,
                        shape = middleShape
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsSwitchTile(
                        title = "Blur Effect",
                        subtitle = "Enable glass haze",
                        checked = blackedOutEnabled,
                        onCheckedChange = { viewModel.setBlackedOutEnabled(it) },
                        icon = { Icon(Icons.Default.BlurOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        backgroundColor = tileColor,
                        shape = middleShape
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsTile(
                        title = "Theme",
                        subtitle = "Current: $currentThemeTitle",
                        icon = { Icon(Icons.Default.Palette, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        backgroundColor = tileColor,
                        shape = middleShape,
                        onClick = { viewModel.onThemeSettingClicked() }
                    )
                    Spacer(Modifier.height(2.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(tileColor, middleShape)
                            .padding(vertical = 16.dp)
                    ) {
                        Text(
                            "Icon Shape",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        )
                        val drawerIconShape by viewModel.drawerIconShape.collectAsState()
                        val entries = IconShape.entries
                        val interactionSources = remember { entries.map { MutableInteractionSource() } }
                        val pressedStates = remember { mutableStateListOf<Boolean>().apply { repeat(entries.size) { add(false) } } }

                        entries.forEachIndexed { index, _ ->
                            LaunchedEffect(interactionSources[index]) {
                                var pressStartTime = 0L
                                interactionSources[index].interactions.collect { interaction ->
                                    when (interaction) {
                                        is PressInteraction.Press -> {
                                            pressedStates[index] = true
                                            pressStartTime = System.currentTimeMillis()
                                        }
                                        is PressInteraction.Release -> {
                                            val duration = System.currentTimeMillis() - pressStartTime
                                            if (duration < 200) delay((200 - duration).milliseconds)
                                            pressedStates[index] = false
                                        }
                                        is PressInteraction.Cancel -> pressedStates[index] = false
                                    }
                                }
                            }
                        }

                        val pressedIndex = pressedStates.indexOfFirst { it }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .padding(vertical = 12.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            val entries = IconShape.entries
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .height(64.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                entries.forEachIndexed { index, shape ->
                                    val isSelected = shape == drawerIconShape
                                    val isPressed = pressedStates[index]

                                    val isNeighborPressed = pressedIndex != -1 && abs(index - pressedIndex) == 1
                                    
                                    val targetWidth = when {
                                        isPressed -> {
                                            val neighbors = if (index == 0 || index == entries.size - 1) 1 else 2
                                            64.dp + (if (neighbors == 1) 6.dp else 12.dp)
                                        }
                                        isNeighborPressed -> 58.dp
                                        else -> 64.dp
                                    }

                                    val containerWidth by animateDpAsState(
                                        targetValue = targetWidth,
                                        label = "containerWidth",
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )

                                    val containerRadius by animateDpAsState(
                                        targetValue = when {
                                            isPressed -> 6.dp
                                            isSelected -> 16.dp
                                            else -> 32.dp
                                        },
                                        label = "containerRadius",
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                    
                                    val containerShape = RoundedCornerShape(containerRadius)
    
                                    Box(
                                        modifier = Modifier
                                            .width(containerWidth)
                                            .fillMaxHeight()
                                            .clip(containerShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                                            )
                                            .border(
                                                width = 2.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                                        else Color.Transparent,
                                                shape = containerShape
                                            )
                                            .clickable(
                                                interactionSource = interactionSources[index],
                                                indication = null
                                            ) { 
                                                viewModel.setDrawerIconShape(shape) 
                                            }
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp) // Fixed size to prevent shape distortion
                                                .clip(shape.getShape())
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                        )
                                    }
                                }
                            }
                        }
                 }
                    Spacer(Modifier.height(2.dp))
                    val drawerIconShadow by viewModel.drawerIconShadow.collectAsState()
                    SettingsSwitchTile(
                        title = "Icon Shadows",
                        subtitle = "Apply depth to app icons",
                        checked = drawerIconShadow,
                        onCheckedChange = { viewModel.setDrawerIconShadow(it) },
                        icon = { Icon(Icons.Default.BlurOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        backgroundColor = tileColor,
                        shape = bottomShape
                    )
                }

                Text(
                    "DOCK",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )

                Column {
                    SettingsSwitchTile(
                        title = "Move with keyboard",
                        subtitle = if (dockSafeDrawIme) "Dock moves up to stay visible" else "Dock stays at the bottom",
                        checked = dockSafeDrawIme,
                        onCheckedChange = { viewModel.setDockSafeDrawIme(it) },
                        icon = { Icon(Icons.Default.Keyboard, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        backgroundColor = tileColor,
                        shape = standaloneShape
                    )
                }

                Text(
                    "NOTIFICATIONS",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tileColor, standaloneShape)
                        .padding(16.dp)
                ) {
                    Text(
                        "Notification Badges",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    val badgeType by viewModel.notificationBadgeType.collectAsState()
                    XenonSingleChoiceButtonGroup(
                        options = listOf(0, 1, 2),
                        selectedOption = badgeType,
                        onOptionSelect = { viewModel.setNotificationBadgeType(it) },
                        label = { type ->
                            when (type) {
                                0 -> "None"
                                1 -> "Dot"
                                2 -> "Number"
                                else -> ""
                            }
                        },
                        unselectedIcon = { type ->
                            Icon(
                                imageVector = when (type) {
                                    0 -> Icons.Default.NotificationsOff
                                    1 -> Icons.Default.Circle
                                    else -> Icons.Default.Numbers
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    "SEARCH",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )

                Column {
                    SettingsSwitchTile(
                        title = "Advanced Search",
                        subtitle = "Include contacts, files and web results",
                        checked = advancedSearchEnabled,
                        onCheckedChange = { viewModel.setAdvancedSearchEnabled(it) },
                        icon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        backgroundColor = tileColor,
                        shape = topShape
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsSwitchTile(
                        title = "Show Hidden Apps",
                        subtitle = "Show hidden apps in search results",
                        checked = showHiddenAppsInSearch,
                        onCheckedChange = { viewModel.setShowHiddenAppsInSearch(it) },
                        icon = { Icon(Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        backgroundColor = tileColor,
                        shape = bottomShape,
                        onClick = { showHiddenAppsDialog = true }
                    )
                }

                Text(
                    "SHORTCUTS",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )

                Column {
                    SettingsTile(
                        title = "Time Shortcut",
                        subtitle = timeShortcut.ifEmpty { "Not set" },
                        icon = { Icon(Icons.Default.Schedule, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        backgroundColor = tileColor,
                        shape = topShape,
                        onClick = { configShortcutType = LauncherViewModel.ShortcutType.TIME }
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsTile(
                        title = "Date Shortcut",
                        subtitle = dateShortcut.ifEmpty { "Not set" },
                        icon = { Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        backgroundColor = tileColor,
                        shape = middleShape,
                        onClick = { configShortcutType = LauncherViewModel.ShortcutType.DATE }
                    )
                    Spacer(Modifier.height(2.dp))
                    SettingsTile(
                        title = "Weather Shortcut",
                        subtitle = weatherShortcut.ifEmpty { "Not set" },
                        icon = { Icon(Icons.Default.Cloud, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        backgroundColor = tileColor,
                        shape = bottomShape,
                        onClick = { configShortcutType = LauncherViewModel.ShortcutType.WEATHER }
                    )
                }

                Text(
                    "SYSTEM", 
                    color = MaterialTheme.colorScheme.primary, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold, 
                    modifier = Modifier.padding(start = 12.dp)
                )

                SettingsTile(
                    title = "Default Home",
                    subtitle = "Set as default launcher",
                    icon = { Icon(Icons.Default.Home, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    backgroundColor = tileColor,
                    shape = standaloneShape,
                    onClick = { viewModel.onResetSettingsClicked() }
                )
            }
            
            configShortcutType?.let { type ->
                val initialValue = when (type) {
                    LauncherViewModel.ShortcutType.TIME -> timeShortcut
                    LauncherViewModel.ShortcutType.DATE -> dateShortcut
                    LauncherViewModel.ShortcutType.WEATHER -> weatherShortcut
                }
                ShortcutConfigDialog(
                    type = type,
                    apps = apps,
                    initialValue = initialValue,
                    onDismiss = { configShortcutType = null },
                    onSave = {
                        when (type) {
                            LauncherViewModel.ShortcutType.TIME -> viewModel.setTimeShortcut(it)
                            LauncherViewModel.ShortcutType.DATE -> viewModel.setDateShortcut(it)
                            LauncherViewModel.ShortcutType.WEATHER -> viewModel.setWeatherShortcut(it)
                        }
                        configShortcutType = null
                    }
                )
            }

            if (showHiddenAppsDialog) {
                AlertDialog(
                    onDismissRequest = { showHiddenAppsDialog = false },
                    title = { Text("Hidden Apps") },
                    text = {
                        val hiddenAppInfos = apps.filter { it.packageName in hiddenApps }
                        if (hiddenAppInfos.isEmpty()) {
                            Text("No apps are hidden.")
                        } else {
                            LazyColumn(modifier = Modifier.height(300.dp)) {
                                items(hiddenAppInfos) { app ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        app.icon?.let { icon ->
                                            Image(
                                                bitmap = icon.toBitmap().asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Text(app.name, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { viewModel.unhideApp(app.packageName) }) {
                                            Icon(Icons.Rounded.Visibility, contentDescription = "Unhide")
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showHiddenAppsDialog = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    )
}
