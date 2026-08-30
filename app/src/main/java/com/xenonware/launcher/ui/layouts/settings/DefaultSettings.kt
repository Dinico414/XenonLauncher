package com.xenonware.launcher.ui.layouts.settings

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.res.DialogClearDataConfirmation
import com.xenon.mylibrary.res.DialogCoverDisplaySelection
import com.xenon.mylibrary.res.DialogLanguageSelection
import com.xenon.mylibrary.res.DialogResetSettingsConfirmation
import com.xenon.mylibrary.res.DialogSignOut
import com.xenon.mylibrary.res.DialogThemeSelection
import com.xenon.mylibrary.res.DialogVersionNumber
import com.xenon.mylibrary.res.ThemeSetting
import com.xenon.mylibrary.theme.DeviceConfigProvider
import com.xenon.mylibrary.theme.LayoutType
import com.xenon.mylibrary.theme.LocalDeviceConfig
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.NoSpacing
import com.xenonware.launcher.BuildConfig
import com.xenonware.launcher.R
import com.xenonware.launcher.presentation.sign_in.GoogleAuthUiClient
import com.xenonware.launcher.presentation.sign_in.SignInState
import com.xenonware.launcher.ui.res.BackupRestoreDialog
import com.xenonware.launcher.ui.res.CalendarSelectionDialog
import com.xenonware.launcher.ui.res.FabActionConfigDialog
import com.xenonware.launcher.ui.res.GlobalIconPackPicker
import com.xenonware.launcher.ui.res.NotificationManagerDialog
import com.xenonware.launcher.ui.res.ShortcutConfigDialog
import com.xenonware.launcher.viewmodel.LauncherViewModel
import com.xenonware.launcher.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultSettings(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    layoutType: LayoutType,
    isLandscape: Boolean,
    onNavigateToDeveloperOptions: () -> Unit,
    state: SignInState,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onConfirmSignOut: () -> Unit,
    googleAuthUiClient: GoogleAuthUiClient,
    appSize: IntSize,
) {
    DeviceConfigProvider(appSize = appSize) {

        val context = LocalContext.current

        val currentThemeTitle by viewModel.currentThemeTitle.collectAsState()
        val showThemeDialog by viewModel.showThemeDialog.collectAsState()
        val themeOptions = remember { ThemeSetting.entries.toTypedArray() }
        val dialogSelectedThemeIndex by viewModel.dialogPreviewThemeIndex.collectAsState()
        val showClearDataDialog by viewModel.showClearDataDialog.collectAsState()
        val showResetSettingsDialog by viewModel.showResetSettingsDialog.collectAsState()
        val showCoverSelectionDialog by viewModel.showCoverSelectionDialog.collectAsState()
        val coverThemeEnabled by viewModel.enableCoverTheme.collectAsState()

        val showVersionDialog by viewModel.showVersionDialog.collectAsState()
        val showBackupDialog by viewModel.showBackupDialog.collectAsState()
        val showSignOutDialog by viewModel.showSignOutDialog.collectAsState()
        val currentLanguage by viewModel.currentLanguage.collectAsState()
        val showLanguageDialog by viewModel.showLanguageDialog.collectAsState()
        val availableLanguages by viewModel.availableLanguages.collectAsState()
        val selectedLanguageTagInDialog by viewModel.selectedLanguageTagInDialog.collectAsState()

        val showCalendarSelectionDialog by viewModel.showCalendarSelectionDialog.collectAsState()
        val availableCalendars by viewModel.availableCalendars.collectAsState()
        val visibleCalendars by viewModel.visibleCalendars.collectAsState()
        val showNotificationManagerDialog by viewModel.showNotificationManagerDialog.collectAsState()
        val visibleNotificationApps by viewModel.visibleNotificationApps.collectAsState()
        val showHiddenAppsDialog by viewModel.showHiddenAppsDialog.collectAsState()
        val hiddenApps by viewModel.hiddenApps.collectAsState()
        val configShortcutType by viewModel.configShortcutType.collectAsState()

        val globalIconPack by viewModel.globalIconPack.collectAsState()
        val showGlobalIconPackDialog by viewModel.showGlobalIconPackDialog.collectAsState()

        val showFabConfigIsDoubleTap by viewModel.showFabConfigIsDoubleTap.collectAsState()
        val fabDoubleTapAction by viewModel.fabDoubleTapAction.collectAsState()
        val fabLongPressAction by viewModel.fabLongPressAction.collectAsState()
        val fabDoubleTapValue by viewModel.fabDoubleTapValue.collectAsState()
        val fabLongPressValue by viewModel.fabLongPressValue.collectAsState()

        val apps by viewModel.apps.collectAsState()
        val iconShape by viewModel.drawerIconShape.collectAsState()
        val showShadow by viewModel.drawerIconShadow.collectAsState()

        val timeShortcut by viewModel.timeShortcut.collectAsState()
        val dateShortcut by viewModel.dateShortcut.collectAsState()
        val weatherShortcut by viewModel.weatherShortcut.collectAsState()


        val packageManager = context.packageManager
        val packageName = context.packageName
        val packageInfo = remember {
            try {
                packageManager.getPackageInfo(packageName, 0)
            } catch (_: Exception) {
                null
            }
        }
        val appVersion = packageInfo?.versionName ?: "N/A"
        val xenonUIVersion = BuildConfig.XENON_UI_VERSION
        val xenonCommonsVersion = BuildConfig.XENON_COMMONS_VERSION


        val containerSize = LocalWindowInfo.current.containerSize
        val applyCoverTheme = remember(containerSize, coverThemeEnabled) {
            viewModel.applyCoverTheme(containerSize)
        }

        val configuration = LocalConfiguration.current
        val isCompact =
            LocalDeviceConfig.current.isCommunicator || LocalDeviceConfig.current.isMindOne
        val appHeight = configuration.screenHeightDp.dp

        val isAppBarExpandable = when (layoutType) {
            LayoutType.COVER -> false
            LayoutType.SMALL -> false
            LayoutType.COMPACT -> !isLandscape && !isCompact && appHeight >= 460.dp
            LayoutType.MEDIUM -> true
            LayoutType.EXPANDED -> true
        }

        val hazeState = rememberHazeState()

        ActivityScreen(
            titleText = stringResource(id = R.string.settings),

            expandable = isAppBarExpandable,

            navigationIconStartPadding = MediumPadding,
            navigationIconPadding = MediumPadding,
            navigationIconSpacing = NoSpacing,
            navigationIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back_description),
                    modifier = Modifier.size(24.dp)
                )
            },
            onNavigationIconClick = onNavigateBack,
            hasNavigationIconExtraContent = false,
            actions = {},
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
                    SettingsItems(
                        viewModel = viewModel,
                        currentThemeTitle = currentThemeTitle,
                        applyCoverTheme = applyCoverTheme,
                        coverThemeEnabled = coverThemeEnabled,
                        currentLanguage = currentLanguage,
                        appVersion = appVersion,
                        layoutType = layoutType,
                        state = state,
                        googleAuthUiClient = googleAuthUiClient,
                        onNavigateToDeveloperOptions = onNavigateToDeveloperOptions,
                        onSignInClick = onSignInClick,
                        onSignOutClick = onSignOutClick,
                        onShowHiddenApps = { viewModel.setShowHiddenApps(true) },
                        onConfigShortcut = { viewModel.setConfigShortcut(it) }
                    )
                }
            })
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
        if (showThemeDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                DialogThemeSelection(
                    themeOptions = themeOptions,
                    currentThemeIndex = dialogSelectedThemeIndex,
                    onThemeSelected = { index -> viewModel.onThemeOptionSelectedInDialog(index) },
                    onDismiss = { viewModel.dismissThemeDialog() },
                    onConfirm = { viewModel.applySelectedTheme() },
                    dialogTitle = stringResource(id = R.string.theme),
                    confirmText = stringResource(id = R.string.ok)
                )
            }
        }
        if (showCoverSelectionDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                DialogCoverDisplaySelection(
                    onConfirm = {
                        viewModel.saveCoverDisplayMetrics(
                            containerSize
                        )
                    },
                    onDismiss = { viewModel.dismissCoverThemeDialog() },
                    dialogTitle = stringResource(id = R.string.cover_screen_mode),
                    confirmText = stringResource(id = R.string.yes),
                    action2Text = stringResource(id = R.string.no),
                    descriptionText = stringResource(id = R.string.cover_screen_mode_description)
                )
            }
        }
        if (showClearDataDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                DialogClearDataConfirmation(
                    onConfirm = { viewModel.confirmClearData() },
                    onDismiss = { viewModel.dismissClearDataDialog() },
                    dialogTitle = stringResource(id = R.string.clear_data),
                    confirmText = stringResource(id = R.string.confirm),
                    descriptionText = stringResource(id = R.string.clear_data_description)
                )
            }
        }
        if (showResetSettingsDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                DialogResetSettingsConfirmation(
                    onConfirm = { viewModel.confirmResetSettings() },
                    onDismiss = { viewModel.dismissResetSettingsDialog() },
                    dialogTitle = stringResource(id = R.string.reset_settings),
                    confirmText = stringResource(id = R.string.confirm),
                    descriptionText = stringResource(id = R.string.reset_all_settings_description)
                )
            }
        }
        if (showVersionDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                DialogVersionNumber(
                    onDismiss = { viewModel.dismissVersionDialog() },
                    dialogTitle = stringResource(id = R.string.version),
                    confirmText = stringResource(id = R.string.more_infos),
                    appString = stringResource(id = R.string.app_version),
                    appVersion = appVersion,
                    xenonUiString = stringResource(id = R.string.xenon_ui_version),
                    xenonUIVersion = xenonUIVersion,
                    xenonCommonsString = stringResource(id = R.string.xenon_commons_version),
                    xenonCommonsVersion = xenonCommonsVersion
                )
            }
        }
        if (showSignOutDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                DialogSignOut(
                    onConfirm = onConfirmSignOut,
                    onDismiss = { viewModel.dismissSignOutDialog() },
                    dialogTitle = stringResource(id = R.string.sign_out),
                    confirmText = stringResource(id = R.string.confirm),
                    descriptionText = stringResource(id = R.string.sign_out_description)
                )
            }
        }

        if (showBackupDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                BackupRestoreDialog(
                    viewModel = viewModel,
                    onDismiss = { viewModel.setShowBackupDialog(false) }
                )
            }
        }

        if (showLanguageDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                DialogLanguageSelection(
                    availableLanguages = availableLanguages,
                    currentLanguageTag = selectedLanguageTagInDialog,
                    onLanguageSelected = { viewModel.onLanguageSelectedInDialog(it) },
                    onDismiss = { viewModel.dismissLanguageDialog() },
                    onConfirm = { viewModel.applySelectedLanguage() },
                    dialogTitle = stringResource(id = R.string.language),
                    confirmText = stringResource(id = R.string.ok)
                )
            }
        }

        if (showCalendarSelectionDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
            CalendarSelectionDialog(
                availableCalendars = availableCalendars,
                selectedCalendars = visibleCalendars,
                onDismiss = { viewModel.setShowCalendarSelectionDialog(false) },
                onToggleCalendar = { viewModel.toggleCalendarVisibility(it) },
                onSelectAll = { viewModel.setVisibleCalendars(emptyList()) },
                onClearAll = { viewModel.setVisibleCalendars(listOf("__NONE__")) }
            )
        }}

        if (showNotificationManagerDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
            NotificationManagerDialog(
                allApps = apps,
                visibleApps = visibleNotificationApps,
                onDismiss = { viewModel.setShowNotificationManagerDialog(false) },
                onToggleApp = { viewModel.toggleNotificationAppVisibility(it) },
                onSelectAll = { viewModel.setVisibleNotificationApps(emptyList()) },
                onClearAll = { viewModel.setVisibleNotificationApps(listOf("__NONE__")) },
                iconShape = iconShape,
                showShadow = showShadow
            )
        }}

        if (showHiddenAppsDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
            NotificationManagerDialog(
                allApps = apps,
                visibleApps = hiddenApps,
                onDismiss = { viewModel.setShowHiddenApps(false) },
                onToggleApp = { 
                    if (it in hiddenApps) viewModel.unhideApp(it)
                    else viewModel.hideApp(it)
                },
                onSelectAll = { /* Not applicable for hidden apps */ },
                onClearAll = { viewModel.setVisibleCalendars(emptyList()) }, // Just clear all if needed
                iconShape = iconShape,
                showShadow = showShadow
            )
        }}

        configShortcutType?.let { type ->
            val initialValue = when (type) {
                LauncherViewModel.ShortcutType.TIME -> timeShortcut
                LauncherViewModel.ShortcutType.DATE -> dateShortcut
                LauncherViewModel.ShortcutType.WEATHER -> weatherShortcut
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                ShortcutConfigDialog(
                    type = type,
                    apps = apps,
                    initialValue = initialValue,
                    iconShape = iconShape,
                    showShadow = showShadow,
                    onDismiss = { viewModel.setConfigShortcut(null) },
                    onSave = { viewModel.saveShortcut(type, it) }
                )
            }
        }

        showFabConfigIsDoubleTap?.let { isDoubleTap ->
            val initialAction = if (isDoubleTap) fabDoubleTapAction else fabLongPressAction
            val initialValue = if (isDoubleTap) fabDoubleTapValue else fabLongPressValue
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeEffect(hazeState)
            ) {
                FabActionConfigDialog(
                    isDoubleTap = isDoubleTap,
                    apps = apps,
                    initialAction = initialAction,
                    initialValue = initialValue,
                    iconShape = iconShape,
                    showShadow = showShadow,
                    onDismiss = { viewModel.setShowFabConfig(null) },
                    onSave = { action, value ->
                        viewModel.setFabAction(isDoubleTap, action, value)
                        viewModel.setShowFabConfig(null)
                    }
                )
            }
        }
    }
}
