package com.xenonware.launcher.ui.layouts.settings

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.theme.DeviceConfigProvider
import com.xenon.mylibrary.theme.LayoutType
import com.xenon.mylibrary.theme.LocalDeviceConfig
import com.xenon.mylibrary.values.IconSizeMedium
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.NoSpacing
import com.xenonware.launcher.R
import com.xenonware.launcher.model.FabAction
import com.xenonware.launcher.ui.res.AppMenuOrderDialog
import com.xenonware.launcher.ui.res.FabActionConfigDialog
import com.xenonware.launcher.ui.res.FontConfigDialog
import com.xenonware.launcher.ui.res.GlobalIconPackPicker
import com.xenonware.launcher.ui.res.NotificationManagerDialog
import com.xenonware.launcher.ui.res.NotificationMessageDialog
import com.xenonware.launcher.ui.res.VisualizerConfigDialog
import com.xenonware.launcher.ui.theme.LocalMainFontFamily
import com.xenonware.launcher.ui.theme.LocalSubFontFamily
import com.xenonware.launcher.viewmodel.FabConfigMode
import com.xenonware.launcher.viewmodel.SettingsViewModel
import com.xenonware.launcher.viewmodel.classes.CustomizationItems
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun CustomizationLayout(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    layoutType: LayoutType,
    isLandscape: Boolean,
    appSize: IntSize,
) {
    DeviceConfigProvider(appSize = appSize) {

        val isCompact =
            LocalDeviceConfig.current.isCommunicator || LocalDeviceConfig.current.isMindOne
        val windowInfo = LocalWindowInfo.current
        val density = LocalDensity.current
        val appHeight = with(density) { windowInfo.containerSize.height.toDp() }

        val isAppBarExpandable = when (layoutType) {
            LayoutType.COVER -> false
            LayoutType.SMALL -> false
            LayoutType.COMPACT -> !isLandscape && !isCompact && appHeight >= 460.dp
            LayoutType.MEDIUM -> true
            LayoutType.EXPANDED -> true
        }

        val hazeState = rememberHazeState()

        val mainFont = LocalMainFontFamily.current
        val subFont = LocalSubFontFamily.current

        ActivityScreen(
            titleText = stringResource(id = R.string.customization),

            expandable = isAppBarExpandable,

            navigationIconStartPadding = MediumPadding,
            navigationIconPadding = MediumPadding,
            navigationIconSpacing = NoSpacing,
            navigationIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back_description),
                    modifier = Modifier.size(IconSizeMedium)
                )
            },
            onNavigationIconClick = onNavigateBack,
            hasNavigationIconExtraContent = false,
            actions = {},
            mainContextFont = mainFont,
            subContextFont = subFont,
            modifier = Modifier.hazeSource(hazeState),
            content = { _ ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = LargestPadding,
                            end = LargestPadding,
                            top = LargestPadding,
                            bottom = WindowInsets.safeDrawing.asPaddingValues()
                                .calculateBottomPadding() + LargestPadding
                        )
                ) {
                    CustomizationItems(
                        viewModel = viewModel,
                        layoutType = layoutType,
                        mainContextFont = mainFont,
                        subContextFont = subFont,
                        onShowHiddenApps = { viewModel.setShowHiddenApps(true) }
                    )
                }
            })

        val showFontConfigDialog by viewModel.showFontConfigDialog.collectAsState()
        val showAppMenuOrderDialog by viewModel.showAppMenuOrderDialog.collectAsState()
        val fontType by viewModel.fontType.collectAsState()
        val mainFontType by viewModel.mainFontType.collectAsState()
        val robotoFlexSettings by viewModel.robotoFlexSettings.collectAsState()
        val googleSansFlexSettings by viewModel.googleSansFlexSettings.collectAsState()

        val globalIconPack by viewModel.globalIconPack.collectAsState()
        val showGlobalIconPackDialog by viewModel.showGlobalIconPackDialog.collectAsState()

        val showHiddenAppsDialog by viewModel.showHiddenAppsDialog.collectAsState()
        val hiddenApps by viewModel.hiddenApps.collectAsState()
        val apps by viewModel.apps.collectAsState()
        val iconShape by viewModel.drawerIconShape.collectAsState()
        val showShadow by viewModel.drawerIconShadow.collectAsState()

        val fabSingleTapAction by viewModel.fabSingleTapAction.collectAsState()
        val fabDoubleTapAction by viewModel.fabDoubleTapAction.collectAsState()
        val fabLongPressAction by viewModel.fabLongPressAction.collectAsState()
        val fabSwipeUpAction by viewModel.fabSwipeUpAction.collectAsState()
        val fabSingleTapValue by viewModel.fabSingleTapValue.collectAsState()
        val fabDoubleTapValue by viewModel.fabDoubleTapValue.collectAsState()
        val fabLongPressValue by viewModel.fabLongPressValue.collectAsState()
        val fabSwipeUpValue by viewModel.fabSwipeUpValue.collectAsState()
        val showFabConfigMode by viewModel.showFabConfigMode.collectAsState()
        val installedShortcuts by viewModel.installedShortcuts.collectAsState()

        if (showFontConfigDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                FontConfigDialog(
                    initialMainFontType = mainFontType,
                    initialSecondaryFontType = fontType,
                    initialRobotoSettings = robotoFlexSettings,
                    initialGoogleSansSettings = googleSansFlexSettings,
                    onDismiss = { viewModel.setShowFontConfigDialog(false) },
                    onSave = { mainType, secType, roboto, googleSans ->
                        viewModel.setMainFontType(mainType)
                        viewModel.setFontType(secType)
                        viewModel.setRobotoFlexSettings(roboto)
                        viewModel.setGoogleSansFlexSettings(googleSans)
                        viewModel.setShowFontConfigDialog(false)
                    }
                )
            }
        }

        if (showAppMenuOrderDialog) {
            val currentOrder by viewModel.appMenuOrder.collectAsState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                AppMenuOrderDialog(
                    initialOrder = currentOrder,
                    onDismiss = { viewModel.setShowAppMenuOrderDialog(false) },
                    onSave = { newOrder ->
                        viewModel.setAppMenuOrder(newOrder)
                        viewModel.setShowAppMenuOrderDialog(false)
                    }
                )
            }
        }

        val showNotificationMessageDialog by viewModel.showNotificationMessageDialog.collectAsState()
        val notificationMessageType by viewModel.notificationMessageType.collectAsState()

        if (showNotificationMessageDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                NotificationMessageDialog(
                    selectedType = notificationMessageType,
                    onDismiss = { viewModel.setShowNotificationMessageDialog(false) },
                    onSelectType = { type ->
                        viewModel.setNotificationMessageType(type)
                        viewModel.setShowNotificationMessageDialog(false)
                    }
                )
            }
        }

        if (showGlobalIconPackDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                GlobalIconPackPicker(
                    iconPacks = remember { viewModel.getInstalledIconPacks() },
                    selectedPackage = globalIconPack,
                    onPackSelect = { viewModel.setGlobalIconPack(it) },
                    onDismiss = { viewModel.setShowGlobalIconPackDialog(false) }
                )
            }
        }

        if (showHiddenAppsDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                NotificationManagerDialog(
                    allApps = apps,
                    visibleApps = hiddenApps,
                    title = stringResource(R.string.hidden_apps),
                    description = stringResource(R.string.hidden_apps_description),
                    onDismiss = { viewModel.setShowHiddenApps(false) },
                    onToggleApp = {
                        if (it in hiddenApps) viewModel.unhideApp(it)
                        else viewModel.hideApp(it)
                    },
                    onSelectAll = { },
                    onClearAll = { },
                    iconShape = iconShape,
                    showShadow = showShadow
                )
            }
        }

        if (showFabConfigMode != FabConfigMode.NONE) {
            val initialAction = when (showFabConfigMode) {
                FabConfigMode.SINGLE -> fabSingleTapAction
                FabConfigMode.DOUBLE -> fabDoubleTapAction
                FabConfigMode.LONG -> fabLongPressAction
                FabConfigMode.SWIPE_UP -> fabSwipeUpAction
                else -> FabAction.NONE
            }
            val initialValue = when (showFabConfigMode) {
                FabConfigMode.SINGLE -> fabSingleTapValue
                FabConfigMode.DOUBLE -> fabDoubleTapValue
                FabConfigMode.LONG -> fabLongPressValue
                FabConfigMode.SWIPE_UP -> fabSwipeUpValue
                else -> ""
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                FabActionConfigDialog(
                    configMode = showFabConfigMode,
                    apps = apps,
                    installedShortcuts = installedShortcuts,
                    initialAction = initialAction,
                    initialValue = initialValue,
                    iconShape = iconShape,
                    showShadow = showShadow,
                    onDismiss = { viewModel.setShowFabConfig(FabConfigMode.NONE) },
                    onSave = { action, value ->
                        viewModel.setFabAction(showFabConfigMode, action, value)
                        viewModel.setShowFabConfig(FabConfigMode.NONE)
                    },
                    onPickShortcut = { item ->
                        val intent = Intent(Intent.ACTION_CREATE_SHORTCUT).apply {
                            component = ComponentName(
                                item.shortcutInfo!!.activityInfo.packageName,
                                item.shortcutInfo.activityInfo.name
                            )
                        }
                        // If shortcut launcher needed, or handled
                    }
                )
            }
        }

        val showVisualizerConfigDialog by viewModel.showVisualizerConfigDialog.collectAsState()

        if (showVisualizerConfigDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                VisualizerConfigDialog(
                    onDismiss = { viewModel.setShowVisualizerConfigDialog(false) }
                )
            }
        }
    }
}
