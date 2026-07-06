package com.xenonware.launcher.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.res.SettingsSwitchTile
import com.xenon.mylibrary.res.SettingsTile
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.NoSpacing
import com.xenonware.launcher.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

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
                        shape = bottomShape,
                        onClick = { viewModel.onThemeSettingClicked() }
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
                        shape = standaloneShape
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
        }
    )
}
