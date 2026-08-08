package com.xenonware.launcher.ui.layouts.settings

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import com.xenonware.launcher.ui.theme.LocalIsDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.res.SettingsGoogleTile
import com.xenon.mylibrary.theme.LayoutType
import com.xenon.mylibrary.values.ExtraLargeSpacing
import com.xenon.mylibrary.values.LargerPadding
import com.xenon.mylibrary.values.NoCornerRadius
import com.xenonware.launcher.R
import com.xenonware.launcher.presentation.sign_in.GoogleAuthUiClient
import com.xenonware.launcher.presentation.sign_in.SignInState
import com.xenonware.launcher.ui.res.IconShape
import com.xenonware.launcher.ui.res.SettingsSwitchMenuTile
import com.xenonware.launcher.ui.res.SettingsSwitchTile
import com.xenonware.launcher.ui.res.SettingsSwitchTileContext
import com.xenonware.launcher.ui.res.SettingsTile
import com.xenonware.launcher.ui.res.SettingsTileContext
import com.xenonware.launcher.ui.res.XenonSingleChoiceButtonGroup
import com.xenonware.launcher.viewmodel.LauncherViewModel
import com.xenonware.launcher.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SettingsItems(
    viewModel: SettingsViewModel,
    currentThemeTitle: String,
    applyCoverTheme: Boolean,
    coverThemeEnabled: Boolean,
    currentLanguage: String,
    appVersion: String,
    layoutType: LayoutType = LayoutType.COMPACT,
    innerGroupRadius: Dp = 4.dp,
    outerGroupRadius: Dp = 24.dp,
    innerGroupSpacing: Dp = 2.dp,
    outerGroupSpacing: Dp = ExtraLargeSpacing,
    tileBackgroundColor: Color = MaterialTheme.colorScheme.surfaceBright,
    tileContentColor: Color = MaterialTheme.colorScheme.onSurface,
    tileSubtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tileShapeOverride: Shape? = null,
    tileHorizontalPadding: Dp = LargerPadding,
    tileVerticalPadding: Dp = LargerPadding,
    switchColorsOverride: SwitchColors? = null,
    useGroupStyling: Boolean = true,
    state: SignInState,
    googleAuthUiClient: GoogleAuthUiClient,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onShowHiddenApps: () -> Unit,
    onNavigateToDeveloperOptions: () -> Unit,
    onConfigShortcut: (LauncherViewModel.ShortcutType) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    val blackedOutEnabled by viewModel.blackedOutModeEnabled.collectAsState()
    val blurEnabled by viewModel.blurEnabled.collectAsState()
    val developerModeEnabled by viewModel.developerModeEnabled.collectAsState()
    val isGridLayout by viewModel.isGridLayout.collectAsState()
    val advancedSearchEnabled by viewModel.advancedSearchEnabled.collectAsState()
    val showHiddenAppsInSearch by viewModel.showHiddenAppsInSearch.collectAsState()
    val dockSafeDrawIme by viewModel.dockSafeDrawIme.collectAsState()
    val dockSafeDrawImePortraitOnly by viewModel.dockSafeDrawImePortraitOnly.collectAsState()
    val drawerIconShape by viewModel.drawerIconShape.collectAsState()
    val drawerIconShadow by viewModel.drawerIconShadow.collectAsState()
    val badgeType by viewModel.notificationBadgeType.collectAsState()
    val timeShortcut by viewModel.timeShortcut.collectAsState()
    val dateShortcut by viewModel.dateShortcut.collectAsState()
    val weatherShortcut by viewModel.weatherShortcut.collectAsState()
    
    val userData = state.userData

    val actualInnerGroupRadius = if (useGroupStyling) innerGroupRadius else 0.dp
    val actualOuterGroupRadius = if (useGroupStyling) outerGroupRadius else 0.dp
    val actualInnerGroupSpacing = if (useGroupStyling) innerGroupSpacing else 0.dp
    val actualOuterGroupSpacing = outerGroupSpacing

    val defaultSwitchColors = SwitchDefaults.colors()

    val topShape = if (useGroupStyling) RoundedCornerShape(
        bottomStart = actualInnerGroupRadius,
        bottomEnd = actualInnerGroupRadius,
        topStart = actualOuterGroupRadius,
        topEnd = actualOuterGroupRadius
    ) else RoundedCornerShape(NoCornerRadius)

    val middleShape = if (useGroupStyling) RoundedCornerShape(
        topStart = actualInnerGroupRadius,
        topEnd = actualInnerGroupRadius,
        bottomStart = actualInnerGroupRadius,
        bottomEnd = actualInnerGroupRadius
    ) else RoundedCornerShape(NoCornerRadius)

    val bottomShape = if (useGroupStyling) RoundedCornerShape(
        topStart = actualInnerGroupRadius,
        topEnd = actualInnerGroupRadius,
        bottomStart = actualOuterGroupRadius,
        bottomEnd = actualOuterGroupRadius
    ) else RoundedCornerShape(NoCornerRadius)

    val standaloneShape = if (useGroupStyling) RoundedCornerShape(actualOuterGroupRadius)
    else RoundedCornerShape(NoCornerRadius)

    LaunchedEffect(key1 = state.signInError) {
        state.signInError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }

    // --- ACCOUNT ---
    SettingsGoogleTile(
        title = if (state.isSignInSuccessful) userData?.username ?: "Signed in" else stringResource(id = R.string.sign_in_with_google),
        subtitle = if (state.isSignInSuccessful) userData?.email else null,
        profilePictureUrl = userData?.profilePictureUrl,
        noAccIcon = painterResource(R.drawable.default_icon),
        isSignedIn = state.isSignInSuccessful,
        onClick = if (state.isSignInSuccessful) onSignOutClick else onSignInClick,
        shape = tileShapeOverride ?: standaloneShape,
        backgroundColor = Color.Transparent,
        contentColor = tileContentColor,
        subtitleColor = tileSubtitleColor,
        horizontalPadding = tileHorizontalPadding,
        verticalPadding = tileVerticalPadding,
        iconContentDescription = stringResource(id = R.string.profile_picture)
    )
    Spacer(Modifier.height(actualOuterGroupSpacing))

    // --- permission ---
    val isDefault = viewModel.isDefaultLauncher(context)
    if (!isDefault) {
        SettingsTile(
            title = "Default Launcher",
            subtitle = "Set Xenon as your default launcher",
            onClick = { viewModel.openLauncherSelector(context) },
            icon = { Icon(Icons.Default.Home, null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = "Accessibility Settings",
            subtitle = "Open system accessibility settings",
            onClick = { viewModel.openAccessibilitySettings(context) },
            icon = {  Icon(painterResource(R.drawable.accessibility), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding
        )
        Spacer(Modifier.height(actualOuterGroupSpacing))
    } else {
        SettingsTile(
            title = "Accessibility Settings",
            subtitle = "Open system accessibility settings",
            onClick = { viewModel.openAccessibilitySettings(context) },
            icon = {  Icon(painterResource(R.drawable.accessibility), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: standaloneShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding
        )
        Spacer(Modifier.height(actualOuterGroupSpacing))
    }

    // --- theming ---
    Column {
        SettingsTile(
            title = stringResource(id = R.string.theme),
            subtitle = "${stringResource(id = R.string.current)} $currentThemeTitle",
            onClick = { viewModel.onThemeSettingClicked() },
            icon = { Icon(painterResource(R.drawable.themes), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsSwitchTile(
            title = "Blacked out",
            subtitle = "Pure black AMOLED mode",
            checked = blackedOutEnabled,
            onCheckedChange = { viewModel.setBlackedOutEnabled(it) },
            onClick = { viewModel.setBlackedOutEnabled(!blackedOutEnabled) },
            icon = { Icon(painterResource(R.drawable.blacked_out), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: middleShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding,
            switchColors = switchColorsOverride ?: defaultSwitchColors
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsSwitchTile(
            title = stringResource(id = R.string.blur_effect),
            subtitle = stringResource(id = R.string.enable_glass_haze),
            checked = blurEnabled,
            onCheckedChange = { viewModel.setBlurEnabled(it) },
            onClick = { viewModel.setBlurEnabled(!blurEnabled) },
            icon = { Icon(Icons.Default.BlurOn, null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: middleShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding,
            switchColors = switchColorsOverride ?: defaultSwitchColors
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsSwitchMenuTile(
            title = stringResource(id = R.string.cover_screen_mode),
            subtitle = "${stringResource(id = R.string.cover_screen_mode_description)} (${if (applyCoverTheme) stringResource(id = R.string.yes) else stringResource(id = R.string.no)})",
            checked = coverThemeEnabled,
            onCheckedChange = { viewModel.setCoverThemeEnabled(it) },
            onClick = { viewModel.onCoverThemeClicked() },
            icon = { Icon(painterResource(R.drawable.cover_screen), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding,
            switchColors = switchColorsOverride ?: defaultSwitchColors
        )
    }
    Spacer(Modifier.height(actualOuterGroupSpacing))

    // --- Language ---
    SettingsTile(
        title = stringResource(id = R.string.language),
        subtitle = "${stringResource(id = R.string.current)} $currentLanguage",
        onClick = { viewModel.onLanguageSettingClicked(context) },
        icon = { Icon(painterResource(R.drawable.language), null, tint = tileSubtitleColor) },
        shape = tileShapeOverride ?: standaloneShape,
        backgroundColor = tileBackgroundColor,
        contentColor = tileContentColor,
        subtitleColor = tileSubtitleColor,
        horizontalPadding = tileHorizontalPadding,
        verticalPadding = tileVerticalPadding
    )
    Spacer(Modifier.height(actualOuterGroupSpacing))

    // --- appdrawer ---
    Column {
        SettingsSwitchTile(
            title = stringResource(id = R.string.grid_layout),
            subtitle = if (isGridLayout) stringResource(id = R.string.using_grid_view) else stringResource(id = R.string.using_list_view),
            checked = isGridLayout,
            onCheckedChange = { viewModel.setGridLayout(it) },
            onClick = { viewModel.setGridLayout(!isGridLayout) },
            icon = {  Icon(painterResource(R.drawable.grid), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding,
            switchColors = switchColorsOverride ?: defaultSwitchColors
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        
        // Icon Shape Selector
        SettingsTileContext(
            title = stringResource(id = R.string.icon_shape),
            icon = {  Icon(painterResource(R.drawable.shape), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: middleShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding,
            enableRipple = false,
            contextContent = {
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
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = if (LocalIsDarkTheme.current)0.5f else 1f))
                        .padding(vertical = 12.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp).height(64.dp),
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
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                            )

                            val containerRadius by animateDpAsState(
                                targetValue = when {
                                    isPressed -> 6.dp
                                    isSelected -> 16.dp
                                    else -> 32.dp
                                }, label = "containerRadius", animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                        )

                        val containerShape = RoundedCornerShape(containerRadius)

                        Box(
                            modifier = Modifier
                                .width(containerWidth)
                                .fillMaxHeight()
                                .clip(containerShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                                .border(width = 2.dp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, shape = containerShape)
                                .clickable(interactionSource = interactionSources[index], indication = null) { viewModel.setDrawerIconShape(shape) }
                                .padding(12.dp), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(shape.getShape()).background(if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }
            }
        })
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsSwitchTile(
            title = stringResource(id = R.string.icon_shadows),
            subtitle = stringResource(id = R.string.apply_depth_description),
            checked = drawerIconShadow,
            onCheckedChange = { viewModel.setDrawerIconShadow(it) },
            onClick = { viewModel.setDrawerIconShadow(!drawerIconShadow) },
            icon = {  Icon(painterResource(R.drawable.shadow), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding,
            switchColors = switchColorsOverride ?: defaultSwitchColors
        )
    }
    Spacer(Modifier.height(actualOuterGroupSpacing))

    // --- search ---
    Column {
        SettingsSwitchTile(
            title = stringResource(id = R.string.advanced_search),
            subtitle = stringResource(id = R.string.advanced_search_description),
            checked = advancedSearchEnabled,
            onCheckedChange = { viewModel.setAdvancedSearchEnabled(it) },
            onClick = { viewModel.setAdvancedSearchEnabled(!advancedSearchEnabled) },
            icon = { Icon(Icons.Default.Search, null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding,
            switchColors = switchColorsOverride ?: defaultSwitchColors
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsSwitchMenuTile(
            title = stringResource(id = R.string.show_hidden_apps),
            subtitle = stringResource(id = R.string.show_hidden_apps_description),
            checked = showHiddenAppsInSearch,
            onCheckedChange = { viewModel.setShowHiddenAppsInSearch(it) },
            onClick = onShowHiddenApps,
            icon = { Icon(Icons.Default.Visibility, null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: middleShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding,
            switchColors = switchColorsOverride ?: defaultSwitchColors
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsSwitchTileContext(
            title = stringResource(id = R.string.move_with_keyboard),
            subtitle = if (dockSafeDrawIme) stringResource(id = R.string.dock_move_up_description) else stringResource(id = R.string.dock_stay_bottom_description),
            checked = dockSafeDrawIme,
            onCheckedChange = { viewModel.setDockSafeDrawIme(it) },
            onClick = { viewModel.setDockSafeDrawIme(!dockSafeDrawIme) },
            icon = { Icon(Icons.Default.Keyboard, null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding,
            switchColors = switchColorsOverride ?: defaultSwitchColors,
            showContext = dockSafeDrawIme && (layoutType == LayoutType.SMALL || layoutType == LayoutType.COMPACT),
            contextContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = if (LocalIsDarkTheme.current) 0.5f else 1f))
                        .clickable { viewModel.setDockSafeDrawImePortraitOnly(!dockSafeDrawImePortraitOnly) }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(id = R.string.move_only_in_portrait),
                                color = tileContentColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                stringResource(id = R.string.move_only_in_portrait_description),
                                color = tileSubtitleColor,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Checkbox(
                            checked = dockSafeDrawImePortraitOnly,
                            onCheckedChange = { viewModel.setDockSafeDrawImePortraitOnly(it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = tileSubtitleColor
                            )
                        )
                    }
                }
            }
        )
    }
    Spacer(Modifier.height(actualOuterGroupSpacing))

    // --- Notification & At a Glance ---
    Column {
        SettingsTile(
            title = "At a Glance",
            subtitle = "Manage events and information shown",
            onClick = { viewModel.setShowCalendarSelectionDialog(true) },
            icon = { Icon(painterResource(R.drawable.at_a_glance), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = "Notification Manager",
            subtitle = "Choose which apps show notifications",
            onClick = { viewModel.setShowNotificationManagerDialog(true) },
            icon = { Icon(Icons.Default.NotificationsActive, null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: middleShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTileContext(
            title = stringResource(id = R.string.notification_badges),
            icon = {  Icon(painterResource(R.drawable.badge), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding,
            enableRipple = false,
            contextContent = {
                XenonSingleChoiceButtonGroup(
                    options = listOf(0, 1, 2),
                    selectedOption = badgeType,
                    onOptionSelect = { viewModel.setNotificationBadgeType(it) },
                    label = { type ->
                        when (type) {
                            0 -> stringResource(id = R.string.none)
                            1 -> stringResource(id = R.string.dot)
                            2 -> stringResource(id = R.string.number)
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
                            tint = tileSubtitleColor
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        )
    }
    Spacer(Modifier.height(actualOuterGroupSpacing))

    // --- shortcuts ---
    Column {
        SettingsTile(
            title = stringResource(id = R.string.time_shortcut),
            subtitle = timeShortcut.ifEmpty { stringResource(id = R.string.not_set) },
            onClick = { onConfigShortcut(LauncherViewModel.ShortcutType.TIME) },
            icon = {  Icon(painterResource(R.drawable.time), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = stringResource(id = R.string.date_shortcut),
            subtitle = dateShortcut.ifEmpty { stringResource(id = R.string.not_set) },
            onClick = { onConfigShortcut(LauncherViewModel.ShortcutType.DATE) },
            icon = {  Icon(painterResource(R.drawable.date), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: middleShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = stringResource(id = R.string.weather_shortcut),
            subtitle = weatherShortcut.ifEmpty { stringResource(id = R.string.not_set) },
            onClick = { onConfigShortcut(LauncherViewModel.ShortcutType.WEATHER) },
            icon = {  Icon(painterResource(R.drawable.weater), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding
        )
    }
    Spacer(Modifier.height(actualOuterGroupSpacing))

    // --- system ---
    Column {
        SettingsTile(
            title = stringResource(id = R.string.clear_data),
            subtitle = stringResource(id = R.string.clear_data_description),
            onClick = { viewModel.onClearDataClicked(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
            icon = { Icon(painterResource(R.drawable.reset), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = stringResource(id = R.string.reset_settings),
            subtitle = stringResource(id = R.string.reset_all_settings_description),
            onClick = { viewModel.onResetSettingsClicked(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
            icon = { Icon(painterResource(R.drawable.reset_settings), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: middleShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = stringResource(id = R.string.version),
            subtitle = "v $appVersion" + if (developerModeEnabled) " (Developer)" else "",
            onClick = { viewModel.onInfoTileClicked() },
            onLongClick = { viewModel.openImpressum(context) },
            icon = { Icon(painterResource(R.drawable.info), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding
        )
    }

    // --- dev ---
    if (developerModeEnabled) {
        Spacer(Modifier.height(actualOuterGroupSpacing))
        SettingsTile(
            title = stringResource(id = R.string.developer_options_title),
            subtitle = stringResource(id = R.string.dev_settings_description),
            onClick = onNavigateToDeveloperOptions,
            icon = { Icon(painterResource(R.drawable.developer), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: standaloneShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            horizontalPadding = tileHorizontalPadding,
            verticalPadding = tileVerticalPadding
        )
    }
}


