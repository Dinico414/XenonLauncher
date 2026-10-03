package com.xenonware.launcher.viewmodel.classes

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.R
import com.xenon.mylibrary.res.SettingsSwitchMenuTile
import com.xenon.mylibrary.res.SettingsSwitchTile
import com.xenon.mylibrary.res.SettingsSwitchTileContext
import com.xenon.mylibrary.res.SettingsTile
import com.xenon.mylibrary.res.SettingsTileContext
import com.xenon.mylibrary.res.XenonSingleChoiceButtonGroup
import com.xenon.mylibrary.theme.LayoutType
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.BiggestCornerRadius
import com.xenon.mylibrary.values.BiggestSpacing
import com.xenon.mylibrary.values.ExtraLargeCornerRadius
import com.xenon.mylibrary.values.ExtraLargeSpacing
import com.xenon.mylibrary.values.ExtraLargerCornerRadius
import com.xenon.mylibrary.values.HugerSpacing
import com.xenon.mylibrary.values.LargeMediumPadding
import com.xenon.mylibrary.values.LargeMediumSpacer
import com.xenon.mylibrary.values.LargestBiggerSpacing
import com.xenon.mylibrary.values.LargestCornerRadius
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumSmallSpacing
import com.xenon.mylibrary.values.MediumSmallerCornerRadius
import com.xenon.mylibrary.values.MediumSpacer
import com.xenon.mylibrary.values.NoCornerRadius
import com.xenon.mylibrary.values.SmallCornerRadius
import com.xenon.mylibrary.values.SmallerSpacer
import com.xenon.mylibrary.values.SmallerStroke
import com.xenonware.launcher.R.string
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.model.FabAction
import com.xenonware.launcher.ui.res.IconShape
import com.xenonware.launcher.ui.theme.LocalIsDarkTheme
import com.xenonware.launcher.viewmodel.FabConfigMode
import com.xenonware.launcher.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CustomizationItems(
    viewModel: SettingsViewModel,
    layoutType: LayoutType = LayoutType.COMPACT,
    innerGroupRadius: Dp = SmallCornerRadius,
    outerGroupRadius: Dp = ExtraLargerCornerRadius,
    innerGroupSpacing: Dp = SmallerSpacer,
    outerGroupSpacing: Dp = ExtraLargeSpacing,
    tileBackgroundColor: Color = MaterialTheme.colorScheme.surfaceBright,
    tileContentColor: Color = MaterialTheme.colorScheme.onSurface,
    tileSubtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    tileShapeOverride: Shape? = null,
    useGroupStyling: Boolean = true,
    mainContextFont: FontFamily = QuicksandTitleVariable,
    subContextFont: FontFamily? = null,
    onShowHiddenApps: () -> Unit = {},
) {
    val context = LocalContext.current
    val actualInnerGroupRadius = if (useGroupStyling) innerGroupRadius else NoCornerRadius
    val actualOuterGroupRadius = if (useGroupStyling) outerGroupRadius else NoCornerRadius
    val actualInnerGroupSpacing = if (useGroupStyling) innerGroupSpacing else NoCornerRadius
    val actualOuterGroupSpacing = if (useGroupStyling) outerGroupSpacing else NoCornerRadius

    val topShape = if (useGroupStyling) RoundedCornerShape(
        bottomStart = actualInnerGroupRadius,
        bottomEnd = actualInnerGroupRadius,
        topStart = actualOuterGroupRadius,
        topEnd = actualOuterGroupRadius
    ) else RoundedCornerShape(NoCornerRadius)

    val standaloneShape = if (useGroupStyling) RoundedCornerShape(actualOuterGroupRadius)
    else RoundedCornerShape(NoCornerRadius)

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

    val showClock by viewModel.showClockAtAGlance.collectAsState()
    val experimentalOptionsEnabled by viewModel.experimentalOptionsEnabled.collectAsState()
    val experimentalWidgetAdjustmentsEnabled by viewModel.experimentalWidgetAdjustmentsEnabled.collectAsState()
    val notificationIndicatorType by viewModel.notificationIndicatorType.collectAsState()
    val notificationMessageType by viewModel.notificationMessageType.collectAsState()
    val tempUnit by viewModel.tempUnit.collectAsState()
    val hideAtAGlance by viewModel.hideAtAGlance.collectAsState()
    val hideDockScrolling by viewModel.hideDockScrolling.collectAsState()
    val hideDockScrollingOnlySmall by viewModel.hideDockScrollingOnlySmall.collectAsState()
    val hideDockWidgets by viewModel.hideDockWidgets.collectAsState()
    val hideDockWidgetsLandscapeOnly by viewModel.hideDockWidgetsLandscapeOnly.collectAsState()
    val hideDockMedia by viewModel.hideDockMedia.collectAsState()
    val hideDockMediaLandscapeOnly by viewModel.hideDockMediaLandscapeOnly.collectAsState()
    val hideDockInAppDrawer by viewModel.hideDockInAppDrawer.collectAsState()
    val hideActionButton by viewModel.hideActionButton.collectAsState()
    val showMuteNotifications by viewModel.showMuteNotifications.collectAsState()
    val showPermanentNotifications by viewModel.showPermanentNotifications.collectAsState()
    val disableGrouping by viewModel.disableGrouping.collectAsState()
    val notificationDeleteSinglePress by viewModel.notificationDeleteSinglePress.collectAsState()

    // Moved items states
    val isGridLayout by viewModel.isGridLayout.collectAsState()
    val openKeyboard by viewModel.openKeyboard.collectAsState()
    val openKeyboardPortraitOnly by viewModel.openKeyboardPortraitOnly.collectAsState()
    val advancedSearchEnabled by viewModel.advancedSearchEnabled.collectAsState()
    val showHiddenAppsInSearch by viewModel.showHiddenAppsInSearch.collectAsState()
    val dockSafeDrawIme by viewModel.dockSafeDrawIme.collectAsState()
    val dockSafeDrawImePortraitOnly by viewModel.dockSafeDrawImePortraitOnly.collectAsState()
    val drawerIconShape by viewModel.drawerIconShape.collectAsState()
    val drawerIconShadow by viewModel.drawerIconShadow.collectAsState()
    val globalIconPack by viewModel.globalIconPack.collectAsState()
    val badgeType by viewModel.notificationBadgeType.collectAsState()
    val appLabelsEnabled by viewModel.appLabelsEnabled.collectAsState()

    val fabSingleTapAction by viewModel.fabSingleTapAction.collectAsState()
    val fabDoubleTapAction by viewModel.fabDoubleTapAction.collectAsState()
    val fabLongPressAction by viewModel.fabLongPressAction.collectAsState()
    val fabSwipeUpAction by viewModel.fabSwipeUpAction.collectAsState()
    val fabSingleTapValue by viewModel.fabSingleTapValue.collectAsState()
    val fabDoubleTapValue by viewModel.fabDoubleTapValue.collectAsState()
    val fabLongPressValue by viewModel.fabLongPressValue.collectAsState()
    val fabSwipeUpValue by viewModel.fabSwipeUpValue.collectAsState()
    val apps by viewModel.apps.collectAsState()

    Column {
        // --- App Drawer & Icons ---
        Column {
            SettingsSwitchTile(
                title = stringResource(id = string.grid_layout),
                subtitle = if (isGridLayout) stringResource(id = string.using_grid_view) else stringResource(id = string.using_list_view),
                checked = isGridLayout,
                onCheckedChange = { viewModel.setGridLayout(it) },
                onClick = { viewModel.setGridLayout(!isGridLayout) },
                icon = { Icon(painterResource(R.drawable.ic_grid), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: topShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )
            Spacer(Modifier.height(actualInnerGroupSpacing))
            SettingsTile(
                title = stringResource(id = string.global_icon_pack),
                subtitle = if (globalIconPack != null) {
                    val pack = remember(globalIconPack) { viewModel.getInstalledIconPacks().find { it.activityInfo.packageName == globalIconPack } }
                    pack?.loadLabel(context.packageManager)?.toString() ?: globalIconPack!!
                } else stringResource(string.system_default),
                onClick = { viewModel.setShowGlobalIconPackDialog(true) },
                icon = { Icon(painterResource(R.drawable.ic_package), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: middleShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )
            Spacer(Modifier.height(actualInnerGroupSpacing))
            // Icon Shape Selector
            SettingsTileContext(
                title = stringResource(id = string.icon_shape),
                icon = { Icon(painterResource(R.drawable.ic_shape), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: middleShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont,
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
                            .padding(start = LargestPadding, end = LargestPadding, bottom = LargestPadding)
                            .clip(RoundedCornerShape(ExtraLargeCornerRadius))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = if (LocalIsDarkTheme.current) 0.5f else 1f))
                            .padding(vertical = LargeMediumPadding)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = LargeMediumPadding).height(HugerSpacing),
                            horizontalArrangement = Arrangement.spacedBy(LargeMediumSpacer),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            entries.forEachIndexed { index, shape ->
                                val isSelected = shape == drawerIconShape
                                val isPressed = pressedStates[index]
                                val isNeighborPressed = pressedIndex != -1 && abs(index - pressedIndex) == 1

                                val targetWidth = when {
                                    isPressed -> {
                                        val neighbors = if (index == 0 || index == entries.size - 1) 1 else 2
                                        HugerSpacing + (if (neighbors == 1) MediumSmallSpacing else LargeMediumSpacer)
                                    }
                                    isNeighborPressed -> 58.dp
                                    else -> HugerSpacing
                                }

                                val containerWidth by animateDpAsState(
                                    targetValue = targetWidth,
                                    label = "containerWidth",
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                )

                                val containerRadius by animateDpAsState(
                                    targetValue = when {
                                        isPressed -> MediumSmallerCornerRadius
                                        isSelected -> LargestCornerRadius
                                        else -> BiggestCornerRadius
                                    }, label = "containerRadius", animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                )

                                val containerShape = RoundedCornerShape(containerRadius)

                                Box(
                                    modifier = Modifier
                                        .width(containerWidth)
                                        .fillMaxHeight()
                                        .clip(containerShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                                        .border(width = SmallerStroke, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, shape = containerShape)
                                        .clickable(interactionSource = interactionSources[index], indication = null) { viewModel.setDrawerIconShape(shape) }
                                        .padding(LargeMediumPadding), contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier.size(BiggestSpacing).clip(shape.getShape()).background(if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }
            )
            Spacer(Modifier.height(actualInnerGroupSpacing))
            SettingsSwitchTile(
                title = stringResource(string.app_labels),
                subtitle = if (appLabelsEnabled) stringResource(string.show_app_labels) else stringResource(id = string.hide_app_labels),
                checked = appLabelsEnabled,
                onCheckedChange = { viewModel.setAppLabelsEnabled(it) },
                onClick = { viewModel.setAppLabelsEnabled(!appLabelsEnabled) },
                icon = { Icon(painterResource(R.drawable.ic_visibility_off), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: middleShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )
            Spacer(Modifier.height(actualInnerGroupSpacing))
            SettingsSwitchTile(
                title = stringResource(id = string.icon_shadows),
                subtitle = stringResource(id = string.apply_depth_description),
                checked = drawerIconShadow,
                onCheckedChange = { viewModel.setDrawerIconShadow(it) },
                onClick = { viewModel.setDrawerIconShadow(!drawerIconShadow) },
                icon = { Icon(painterResource(R.drawable.ic_shadow), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: bottomShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )
        }
        Spacer(Modifier.height(actualOuterGroupSpacing))

        // --- Search & Keyboard Tweaks ---
        Column {
            SettingsSwitchTile(
                title = stringResource(id = string.advanced_search),
                subtitle = stringResource(id = string.advanced_search_description),
                checked = advancedSearchEnabled,
                onCheckedChange = { viewModel.setAdvancedSearchEnabled(it) },
                onClick = { viewModel.setAdvancedSearchEnabled(!advancedSearchEnabled) },
                icon = { Icon(painterResource(R.drawable.ic_search_advanced), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: topShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )
            Spacer(Modifier.height(actualInnerGroupSpacing))
            SettingsSwitchMenuTile(
                title = stringResource(id = string.show_hidden_apps),
                subtitle = stringResource(id = string.show_hidden_apps_description),
                checked = showHiddenAppsInSearch,
                onCheckedChange = { viewModel.setShowHiddenAppsInSearch(it) },
                onClick = onShowHiddenApps,
                icon = { Icon(painterResource(R.drawable.ic_hide), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: middleShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )
            Spacer(Modifier.height(actualInnerGroupSpacing))
            SettingsSwitchTileContext(
                title = stringResource(id = string.move_with_keyboard),
                subtitle = if (dockSafeDrawIme) stringResource(id = string.dock_move_up_description) else stringResource(id = string.dock_stay_bottom_description),
                checked = dockSafeDrawIme,
                onCheckedChange = { viewModel.setDockSafeDrawIme(it) },
                onClick = { viewModel.setDockSafeDrawIme(!dockSafeDrawIme) },
                icon = { Icon(painterResource(R.drawable.ic_keyboard_move), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: middleShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont,
                showContext = dockSafeDrawIme && (layoutType == LayoutType.SMALL || layoutType == LayoutType.COMPACT),
                contextContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = LargestPadding, end = LargestPadding, bottom = LargestPadding)
                            .clip(RoundedCornerShape(ExtraLargeCornerRadius))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = if (LocalIsDarkTheme.current) 0.5f else 1f))
                            .clickable { viewModel.setDockSafeDrawImePortraitOnly(!dockSafeDrawImePortraitOnly) }
                            .padding(LargeMediumPadding)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(id = string.move_only_in_portrait),
                                    color = tileContentColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    stringResource(id = string.move_only_in_portrait_description),
                                    color = tileSubtitleColor,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.width(MediumSpacer))
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
            Spacer(Modifier.height(actualInnerGroupSpacing))
            SettingsSwitchTileContext(
                title = stringResource(id = string.open_keyboard),
                subtitle = stringResource(id = string.focus_search_description),
                checked = openKeyboard,
                onCheckedChange = { viewModel.setOpenKeyboard(it) },
                onClick = { viewModel.setOpenKeyboard(!openKeyboard) },
                icon = { Icon(painterResource(R.drawable.ic_keyboard), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: bottomShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont,
                showContext = openKeyboard && (layoutType == LayoutType.SMALL || layoutType == LayoutType.COMPACT),
                contextContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = LargestPadding, end = LargestPadding, bottom = LargestPadding)
                            .clip(RoundedCornerShape(ExtraLargeCornerRadius))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = if (LocalIsDarkTheme.current) 0.5f else 1f))
                            .clickable { viewModel.setOpenKeyboardPortraitOnly(!openKeyboardPortraitOnly) }
                            .padding(LargeMediumPadding)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(id = string.open_only_in_portrait),
                                    color = tileContentColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    stringResource(id = string.open_only_in_portrait_description),
                                    color = tileSubtitleColor,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.width(MediumSpacer))
                            Checkbox(
                                checked = openKeyboardPortraitOnly,
                                onCheckedChange = { viewModel.setOpenKeyboardPortraitOnly(it) },
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

        // --- Action Button / FAB ---
        Column {
            SettingsSwitchTile(
                title = stringResource(string.hide_action_button),
                subtitle = stringResource(string.hide_action_button_description),
                checked = hideActionButton,
                onCheckedChange = { viewModel.setHideActionButton(it) },
                onClick = { viewModel.setHideActionButton(!hideActionButton) },
                icon = { Icon(painterResource(R.drawable.ic_touch_off), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: if (hideActionButton) standaloneShape else topShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )
            if (!hideActionButton) {
                Spacer(Modifier.height(actualInnerGroupSpacing))
                SettingsTile(
                    title = stringResource(id = string.fab_single_tap),
                    subtitle = getFabActionTitle(fabSingleTapAction, fabSingleTapValue, apps),
                    onClick = { viewModel.setShowFabConfig(FabConfigMode.SINGLE) },
                    icon = { Icon(painterResource(R.drawable.ic_touch_app), null, tint = tileSubtitleColor) },
                    shape = tileShapeOverride ?: middleShape,
                    backgroundColor = tileBackgroundColor,
                    contentColor = tileContentColor,
                    subtitleColor = tileSubtitleColor,
                    mainContextFont = mainContextFont,
                    subContextFont = subContextFont
                )
                Spacer(Modifier.height(actualInnerGroupSpacing))
                SettingsTile(
                    title = stringResource(id = string.fab_double_tap),
                    subtitle = getFabActionTitle(fabDoubleTapAction, fabDoubleTapValue, apps),
                    onClick = { viewModel.setShowFabConfig(FabConfigMode.DOUBLE) },
                    icon = { Icon(painterResource(R.drawable.ic_touch_double), null, tint = tileSubtitleColor) },
                    shape = tileShapeOverride ?: middleShape,
                    backgroundColor = tileBackgroundColor,
                    contentColor = tileContentColor,
                    subtitleColor = tileSubtitleColor,
                    mainContextFont = mainContextFont,
                    subContextFont = subContextFont
                )
                Spacer(Modifier.height(actualInnerGroupSpacing))
                SettingsTile(
                    title = stringResource(id = string.fab_long_press),
                    subtitle = getFabActionTitle(fabLongPressAction, fabLongPressValue, apps),
                    onClick = { viewModel.setShowFabConfig(FabConfigMode.LONG) },
                    icon = { Icon(painterResource(R.drawable.ic_touch_long), null, tint = tileSubtitleColor) },
                    shape = tileShapeOverride ?: middleShape,
                    backgroundColor = tileBackgroundColor,
                    contentColor = tileContentColor,
                    subtitleColor = tileSubtitleColor,
                    mainContextFont = mainContextFont,
                    subContextFont = subContextFont
                )
                Spacer(Modifier.height(actualInnerGroupSpacing))
                SettingsTile(
                    title = stringResource(id = string.fab_swipe_up),
                    subtitle = getFabActionTitle(fabSwipeUpAction, fabSwipeUpValue, apps),
                    onClick = { viewModel.setShowFabConfig(FabConfigMode.SWIPE_UP) },
                    icon = { Icon(painterResource(R.drawable.ic_touch_swipe_up), null, tint = tileSubtitleColor) },
                    shape = tileShapeOverride ?: bottomShape,
                    backgroundColor = tileBackgroundColor,
                    contentColor = tileContentColor,
                    subtitleColor = tileSubtitleColor,
                    mainContextFont = mainContextFont,
                    subContextFont = subContextFont
                )
            }
        }
        Spacer(Modifier.height(actualOuterGroupSpacing))

        // --- Notification Badges Tweaks ---
        Column {
            SettingsTileContext(
                title = stringResource(id = string.notification_badges),
                icon = { Icon(painterResource(R.drawable.ic_badge), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: standaloneShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont,
                enableRipple = false,
                contextContent = {
                    XenonSingleChoiceButtonGroup(
                        options = listOf(0, 1, 2),
                        selectedOption = badgeType,
                        onOptionSelect = { viewModel.setNotificationBadgeType(it) },
                        label = { type ->
                            when (type) {
                                0 -> stringResource(id = string.none)
                                1 -> stringResource(id = string.dot)
                                2 -> stringResource(id = string.number)
                                else -> ""
                            }
                        },
                        mainContextFont = mainContextFont,
                        subContextFont = subContextFont,
                        unselectedIcon = { type ->
                            Icon(
                                imageVector = when (type) {
                                    0 -> Icons.Rounded.NotificationsOff
                                    1 -> Icons.Rounded.Circle
                                    else -> Icons.Rounded.Numbers
                                },
                                contentDescription = null,
                                modifier = Modifier.size(LargestBiggerSpacing),
                                tint = tileSubtitleColor
                            )
                        },
                        modifier = Modifier.fillMaxWidth().padding(start = LargestPadding, end = LargestPadding, bottom = LargestPadding)
                    )
                }
            )
        }
        Spacer(Modifier.height(actualOuterGroupSpacing))

        // --- At a Glance Tweaks ---
        Column {
            SettingsSwitchTile(
                title = stringResource(string.show_clock_at_a_glance),
                subtitle = stringResource(string.show_clock_at_a_glance_description),
                checked = showClock,
                onCheckedChange = { viewModel.setShowClockAtAGlance(it) },
                onClick = { viewModel.setShowClockAtAGlance(!showClock) },
                icon = { Icon(painterResource(R.drawable.ic_at_a_glance_clock), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: topShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsTileContext(
                title = stringResource(string.temp_unit),
                icon = { Icon(painterResource(R.drawable.ic_temperature), null, tint = tileSubtitleColor) },
                showContext = true,
                shape = tileShapeOverride ?: middleShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont,
                enableRipple = false,
                contextContent = {
                    XenonSingleChoiceButtonGroup(
                        options = listOf(0, 1),
                        selectedOption = tempUnit,
                        onOptionSelect = { viewModel.setTempUnit(it) },
                        label = { type ->
                            when (type) {
                                0 -> "Celsius (°C)"
                                1 -> "Fahrenheit (°F)"
                                else -> ""
                            }
                        },
                        mainContextFont = mainContextFont,
                        subContextFont = subContextFont,
                        modifier = Modifier.fillMaxWidth().padding(start = LargestPadding, end = LargestPadding, bottom = LargestPadding)
                    )
                }
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsSwitchTile(
                title = stringResource(string.hide_at_a_glance),
                subtitle = stringResource(string.hide_at_a_glance_description),
                checked = hideAtAGlance,
                onCheckedChange = { viewModel.setHideAtAGlance(it) },
                onClick = { viewModel.setHideAtAGlance(!hideAtAGlance) },
                icon = { Icon(painterResource(R.drawable.ic_at_a_glance_show), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: bottomShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )
        }

        Spacer(Modifier.height(actualOuterGroupSpacing))

        // --- Notification Tweaks ---
        Column {
            SettingsTileContext(
                title = stringResource(string.notification_indicator),
                icon = { Icon(painterResource(R.drawable.ic_notification), null, tint = tileSubtitleColor) },
                showContext = true,
                shape = tileShapeOverride ?: topShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont,
                enableRipple = false,
                contextContent = {
                    val entries = listOf(0, 1, 2)
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
                            .padding(start = LargestPadding, end = LargestPadding, bottom = LargestPadding)
                            .clip(RoundedCornerShape(ExtraLargeCornerRadius))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = if (LocalIsDarkTheme.current) 0.5f else 1f))
                            .padding(vertical = LargeMediumPadding)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = LargeMediumPadding).height(HugerSpacing),
                            horizontalArrangement = Arrangement.spacedBy(LargeMediumSpacer),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            entries.forEachIndexed { index, type ->
                                val isSelected = type == notificationIndicatorType
                                val isPressed = pressedStates[index]
                                val isNeighborPressed = pressedIndex != -1 && abs(index - pressedIndex) == 1

                                val targetWidth = when {
                                    isPressed -> {
                                        val neighbors = if (index == 0 || index == entries.size - 1) 1 else 2
                                        HugerSpacing + (if (neighbors == 1) MediumSmallSpacing else LargeMediumSpacer)
                                    }
                                    isNeighborPressed -> 58.dp
                                    else -> HugerSpacing
                                }

                                val containerWidth by animateDpAsState(
                                    targetValue = targetWidth,
                                    label = "containerWidth",
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                )

                                val containerRadius by animateDpAsState(
                                    targetValue = when {
                                        isPressed -> MediumSmallerCornerRadius
                                        isSelected -> LargestCornerRadius
                                        else -> BiggestCornerRadius
                                    }, label = "containerRadius", animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                )

                                val containerShape = RoundedCornerShape(containerRadius)

                                Box(
                                    modifier = Modifier
                                        .width(containerWidth)
                                        .fillMaxHeight()
                                        .clip(containerShape)
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                                        .border(width = SmallerStroke, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, shape = containerShape)
                                        .clickable(interactionSource = interactionSources[index], indication = null) { viewModel.setNotificationIndicatorType(type) }
                                        .padding(LargeMediumPadding), contentAlignment = Alignment.Center
                                ) {
                                    val iconVector = when (type) {
                                        0 -> Icons.Rounded.Block
                                        1 -> Icons.Rounded.Check
                                        else -> Icons.Rounded.EmojiEvents
                                    }
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = when (type) {
                                            0 -> stringResource(string.notification_indicator_none)
                                            1 -> stringResource(string.notification_indicator_checkmark)
                                            else -> stringResource(string.notification_indicator_trophy)
                                        },
                                        modifier = Modifier.size(BiggestSpacing),
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsTile(
                title = stringResource(string.notification_message),
                subtitle = when (notificationMessageType) {
                    0 -> stringResource(string.notification_message_none)
                    1 -> "\"${stringResource(string.notification_message_no_notification)}\""
                    else -> "\"${stringResource(string.notification_message_up_to_date)}\""
                },
                onClick = { viewModel.setShowNotificationMessageDialog(true) },
                icon = { Icon(painterResource(R.drawable.ic_message), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: middleShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsSwitchTile(
                title = stringResource(string.show_mute_notifications),
                subtitle = stringResource(string.show_mute_notifications_description),
                checked = showMuteNotifications,
                onCheckedChange = { viewModel.setShowMuteNotifications(it) },
                onClick = { viewModel.setShowMuteNotifications(!showMuteNotifications) },
                icon = { Icon(painterResource(R.drawable.ic_notification_silenced), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: middleShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsSwitchTile(
                title = stringResource(string.show_permanent_notifications),
                subtitle = stringResource(string.show_permanent_notifications_description),
                checked = showPermanentNotifications,
                onCheckedChange = { viewModel.setShowPermanentNotifications(it) },
                onClick = { viewModel.setShowPermanentNotifications(!showPermanentNotifications) },
                icon = { Icon(painterResource(R.drawable.ic_notification_pin), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: middleShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsSwitchTile(
                title = stringResource(string.experimental_disable_grouping),
                subtitle = stringResource(string.experimental_disable_grouping_description),
                checked = disableGrouping,
                onCheckedChange = { viewModel.setDisableGrouping(it) },
                onClick = { viewModel.setDisableGrouping(!disableGrouping) },
                icon = { Icon(painterResource(R.drawable.ic_block), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: middleShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsSwitchTile(
                title = stringResource(string.notification_delete_single_press),
                subtitle = stringResource(string.notification_delete_single_press_description),
                checked = notificationDeleteSinglePress,
                onCheckedChange = { viewModel.setNotificationDeleteSinglePress(it) },
                onClick = { viewModel.setNotificationDeleteSinglePress(!notificationDeleteSinglePress) },
                icon = { Icon(painterResource(R.drawable.ic_delete), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: bottomShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )
        }

        Spacer(Modifier.height(actualOuterGroupSpacing))

        // --- Font Tweaks ---
        Column {
            SettingsTile(
                title = stringResource(string.font_settings),
                subtitle = stringResource(string.font_settings_description),
                onClick = { viewModel.setShowFontConfigDialog(true) },
                icon = { Icon(painterResource(R.drawable.ic_font), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: standaloneShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )
        }

        Spacer(Modifier.height(actualOuterGroupSpacing))

        // --- App Menu Tweaks ---
        Column {
            SettingsTile(
                title = stringResource(string.app_menu_order),
                subtitle = stringResource(string.app_menu_order_description),
                onClick = { viewModel.setShowAppMenuOrderDialog(true) },
                icon = { Icon(painterResource(R.drawable.ic_reorder), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: standaloneShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )
        }

        Spacer(Modifier.height(actualOuterGroupSpacing))

        // --- Dock Tweaks ---
        Column {
            var dockIndex = 0
            val totalDockItems = 4

            fun getDockShape(index: Int): Shape {
                return tileShapeOverride ?: when {
                    totalDockItems == 1 -> standaloneShape
                    index == 0 -> topShape
                    index == totalDockItems - 1 -> bottomShape
                    else -> middleShape
                }
            }

            SettingsSwitchTileContext(
                title = stringResource(string.hide_dock_scrolling),
                subtitle = stringResource(string.hide_dock_scrolling_description),
                checked = hideDockScrolling,
                onCheckedChange = { viewModel.setHideDockScrolling(it) },
                onClick = { viewModel.setHideDockScrolling(!hideDockScrolling) },
                icon = { Icon(painterResource(R.drawable.ic_scroll), null, tint = tileSubtitleColor) },
                shape = getDockShape(dockIndex++),
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont,
                showContext = hideDockScrolling && (layoutType == LayoutType.SMALL || layoutType == LayoutType.COMPACT),
                contextContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = if (LocalIsDarkTheme.current) 0.5f else 1f))
                            .clickable { viewModel.setHideDockScrollingOnlySmall(!hideDockScrollingOnlySmall) }
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(string.hide_dock_scrolling_only_small),
                                    color = tileContentColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    stringResource(string.hide_dock_scrolling_only_small_description),
                                    color = tileSubtitleColor,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Checkbox(
                                checked = hideDockScrollingOnlySmall,
                                onCheckedChange = { viewModel.setHideDockScrollingOnlySmall(it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = tileSubtitleColor
                                )
                            )
                        }
                    }
                }
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsSwitchTileContext(
                title = stringResource(string.hide_dock_widgets),
                subtitle = stringResource(string.hide_dock_widgets_description),
                checked = hideDockWidgets,
                onCheckedChange = { viewModel.setHideDockWidgets(it) },
                onClick = { viewModel.setHideDockWidgets(!hideDockWidgets) },
                icon = { Icon(painterResource(R.drawable.ic_widgets), null, tint = tileSubtitleColor) },
                shape = getDockShape(dockIndex++),
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont,
                showContext = hideDockWidgets,
                contextContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = if (LocalIsDarkTheme.current) 0.5f else 1f))
                            .clickable { viewModel.setHideDockWidgetsLandscapeOnly(!hideDockWidgetsLandscapeOnly) }
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(string.hide_dock_only_landscape),
                                    color = tileContentColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    stringResource(string.hide_dock_only_landscape_description),
                                    color = tileSubtitleColor,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Checkbox(
                                checked = hideDockWidgetsLandscapeOnly,
                                onCheckedChange = { viewModel.setHideDockWidgetsLandscapeOnly(it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = tileSubtitleColor
                                )
                            )
                        }
                    }
                }
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsSwitchTileContext(
                title = stringResource(string.hide_dock_media),
                subtitle = stringResource(string.hide_dock_media_description),
                checked = hideDockMedia,
                onCheckedChange = { viewModel.setHideDockMedia(it) },
                onClick = { viewModel.setHideDockMedia(!hideDockMedia) },
                icon = { Icon(painterResource(R.drawable.ic_media), null, tint = tileSubtitleColor) },
                shape = getDockShape(dockIndex++),
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont,
                showContext = hideDockMedia,
                contextContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = if (LocalIsDarkTheme.current) 0.5f else 1f))
                            .clickable { viewModel.setHideDockMediaLandscapeOnly(!hideDockMediaLandscapeOnly) }
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(string.hide_dock_only_landscape),
                                    color = tileContentColor,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    stringResource(string.hide_dock_only_landscape_description),
                                    color = tileSubtitleColor,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Checkbox(
                                checked = hideDockMediaLandscapeOnly,
                                onCheckedChange = { viewModel.setHideDockMediaLandscapeOnly(it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = tileSubtitleColor
                                )
                            )
                        }
                    }
                }
            )

            Spacer(Modifier.height(actualInnerGroupSpacing))

            SettingsSwitchTile(
                title = stringResource(string.hide_dock_app_drawer),
                subtitle = stringResource(string.hide_dock_app_drawer_description),
                checked = hideDockInAppDrawer,
                onCheckedChange = { viewModel.setHideDockInAppDrawer(it) },
                onClick = { viewModel.setHideDockInAppDrawer(!hideDockInAppDrawer) },
                icon = { Icon(painterResource(R.drawable.ic_dock_hide), null, tint = tileSubtitleColor) },
                shape = getDockShape(dockIndex++),
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )
        }

        Spacer(Modifier.height(actualOuterGroupSpacing))

        // --- Music Visualizer Tweaks ---
        Column {
            SettingsTile(
                title = "Music Visualizer",
                subtitle = "Customize the appearance and behavior of the music visualizer",
                onClick = { viewModel.setShowVisualizerConfigDialog(true) },
                icon = { Icon(painterResource(R.drawable.ic_media_visualizer), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: standaloneShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )
        }

        if (experimentalOptionsEnabled) {
            Spacer(Modifier.height(actualOuterGroupSpacing))
            Column {
                SettingsSwitchTile(
                    title = stringResource(string.experimental_widget_adjustments),
                    subtitle = stringResource(string.experimental_widget_adjustments_description),
                    checked = experimentalWidgetAdjustmentsEnabled,
                    onCheckedChange = { viewModel.setExperimentalWidgetAdjustmentsEnabled(it) },
                    onClick = { viewModel.setExperimentalWidgetAdjustmentsEnabled(!experimentalWidgetAdjustmentsEnabled) },
                    icon = { Icon(painterResource(R.drawable.ic_widgets), null, tint = tileSubtitleColor) },
                    shape = tileShapeOverride ?: standaloneShape,
                    backgroundColor = tileBackgroundColor,
                    contentColor = tileContentColor,
                    subtitleColor = tileSubtitleColor,
                    mainContextFont = mainContextFont,
                    subContextFont = subContextFont
                )
            }
        }
    }
}

@Composable
fun getFabActionTitle(action: FabAction, value: String, apps: List<AppInfo>): String {
    return when (action) {
        FabAction.LOCK_DEVICE -> stringResource(string.action_lock_device)
        FabAction.TRIGGER_ASSISTANT -> stringResource(string.action_trigger_assistant)
        FabAction.OPEN_APP -> {
            val app = apps.find { it.packageName == value }
            if (app != null) "${stringResource(string.action_open_app)}: ${app.label}"
            else stringResource(string.action_open_app)
        }
        FabAction.OPEN_LINK -> {
            if (value.isNotEmpty()) "${stringResource(string.action_open_link)}: $value"
            else stringResource(string.action_open_link)
        }
        FabAction.OPEN_SHORTCUT -> {
            if (value.isNotEmpty()) {
                val name = value.substringBefore("|")
                "${stringResource(string.action_open_shortcut)}: $name"
            } else stringResource(string.action_open_shortcut)
        }
        FabAction.TOGGLE_FLASHLIGHT -> stringResource(string.action_toggle_flashlight)
        FabAction.OPEN_APP_DRAWER -> stringResource(string.action_open_app_drawer)
        FabAction.NONE -> stringResource(string.action_none)
    }
}
