package com.xenonware.launcher.ui.layouts.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.xenon.mylibrary.ActivityScreen
import com.xenon.mylibrary.res.DialogClearDataConfirmation
import com.xenon.mylibrary.res.DialogCoverDisplaySelection
import com.xenon.mylibrary.res.DialogResetSettingsConfirmation
import com.xenon.mylibrary.res.DialogSignOut
import com.xenon.mylibrary.res.DialogThemeSelection
import com.xenon.mylibrary.res.DialogVersionNumber
import com.xenon.mylibrary.theme.DeviceConfigProvider
import com.xenon.mylibrary.theme.LocalDeviceConfig
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.MediumPadding
import com.xenon.mylibrary.values.NoSpacing
import com.xenonware.launcher.BuildConfig
import com.xenonware.launcher.presentation.sign_in.GoogleAuthUiClient
import com.xenonware.launcher.presentation.sign_in.SignInState
import com.xenonware.launcher.ui.res.CalendarSelectionDialog
import com.xenonware.launcher.ui.res.ShortcutConfigDialog
import com.xenonware.launcher.ui.res.XenonDialog
import com.xenonware.launcher.viewmodel.LauncherViewModel
import com.xenonware.launcher.viewmodel.LayoutType
import com.xenonware.launcher.viewmodel.SettingsViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun SettingsLayout(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    layoutType: LayoutType,
    isLandscape: Boolean,
    state: SignInState,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onConfirmSignOut: () -> Unit,
    googleAuthUiClient: GoogleAuthUiClient,
    appSize: IntSize,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        DefaultSettings(
            onNavigateBack = onNavigateBack,
            viewModel = viewModel,
            layoutType = layoutType,
            isLandscape = isLandscape,
            state = state,
            onSignInClick = onSignInClick,
            onSignOutClick = onSignOutClick,
            onConfirmSignOut = onConfirmSignOut,
            googleAuthUiClient = googleAuthUiClient,
            appSize = appSize
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultSettings(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    layoutType: LayoutType,
    isLandscape: Boolean,
    state: SignInState,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onConfirmSignOut: () -> Unit,
    googleAuthUiClient: GoogleAuthUiClient,
    appSize: IntSize,
) {
    var isDeveloperOptionsVisible by remember { mutableStateOf(false) }

    if (isDeveloperOptionsVisible) {
        ActivityScreen(
            titleText = "Developer Options",
            navigationIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp)
                )
            },
            onNavigationIconClick = { isDeveloperOptionsVisible = false },
            content = { _ ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Empty Developer Options", style = MaterialTheme.typography.bodyLarge)
                }
            }
        )
    } else {
        DeviceConfigProvider(appSize = appSize) {

            val context = LocalContext.current

            val currentThemeTitle by viewModel.currentThemeTitle.collectAsState()
            val showThemeDialog by viewModel.showThemeDialog.collectAsState()
            val themeOptions = viewModel.themeOptions
            val dialogSelectedThemeIndex by viewModel.dialogPreviewThemeIndex.collectAsState()
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val showClearDataDialog by viewModel.showClearDataDialog.collectAsState()
            val showResetSettingsDialog by viewModel.showResetSettingsDialog.collectAsState()
            val showCoverSelectionDialog by viewModel.showCoverSelectionDialog.collectAsState()
            val coverThemeEnabled by viewModel.enableCoverTheme.collectAsState()

            val showVersionDialog by viewModel.showVersionDialog.collectAsState()
            val showSignOutDialog by viewModel.showSignOutDialog.collectAsState()

            val apps by viewModel.apps.collectAsState()
            val hiddenApps by viewModel.hiddenApps.collectAsState()
            val showCalendarSelectionDialog by viewModel.showCalendarSelectionDialog.collectAsState()
            val availableCalendars by viewModel.availableCalendars.collectAsState()
            val visibleCalendars by viewModel.visibleCalendars.collectAsState()
            val timeShortcut by viewModel.timeShortcut.collectAsState()
            val dateShortcut by viewModel.dateShortcut.collectAsState()
            val weatherShortcut by viewModel.weatherShortcut.collectAsState()
            val iconShape by viewModel.drawerIconShape.collectAsState()
            val showShadow by viewModel.drawerIconShadow.collectAsState()
        val blurEnabled by viewModel.blurEnabled.collectAsState()

            var configShortcutType by remember { mutableStateOf<LauncherViewModel.ShortcutType?>(null) }
            var showHiddenAppsDialog by remember { mutableStateOf(false) }

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
                titleText = "Settings",

                expandable = isAppBarExpandable,

                navigationIconStartPadding = MediumPadding,
                navigationIconPadding = MediumPadding,
                navigationIconSpacing = NoSpacing,
                navigationIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Navigate Back",
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
                            state = state,
                            onSignInClick = onSignInClick,
                            onSignOutClick = onSignOutClick,
                            googleAuthUiClient = googleAuthUiClient,
                            onShowHiddenApps = { showHiddenAppsDialog = true },
                            onNavigateToDeveloperOptions = { isDeveloperOptionsVisible = true },
                            onConfigShortcut = { configShortcutType = it }
                        )
                    }
                })

            // Original Launcher Dialogs
            configShortcutType?.let { type ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurEnabled) Modifier.hazeEffect(hazeState) else Modifier)
                        .background(if (blurEnabled) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    val initialValue = when (type) {
                        LauncherViewModel.ShortcutType.TIME -> timeShortcut
                        LauncherViewModel.ShortcutType.DATE -> dateShortcut
                        LauncherViewModel.ShortcutType.WEATHER -> weatherShortcut
                    }
                    ShortcutConfigDialog(
                        type = type,
                        apps = apps,
                        initialValue = initialValue,
                        iconShape = iconShape,
                        showShadow = showShadow,
                        onDismiss = { configShortcutType = null },
                        onSave = {
                            when (type) {
                                LauncherViewModel.ShortcutType.TIME -> viewModel.setTimeShortcut(it)
                                LauncherViewModel.ShortcutType.DATE -> viewModel.setDateShortcut(it)
                                LauncherViewModel.ShortcutType.WEATHER -> viewModel.setWeatherShortcut(
                                    it
                                )
                            }
                            configShortcutType = null
                        })
                }
            }

            if (showHiddenAppsDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurEnabled) Modifier.hazeEffect(hazeState) else Modifier)
                        .background(if (blurEnabled) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    val listState = rememberLazyListState()
                    val showTopDivider by remember {
                        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
                    }
                    val showBottomDivider by remember {
                        derivedStateOf { listState.canScrollForward }
                    }

                    XenonDialog(
                        onDismissRequest = { showHiddenAppsDialog = false },
                        properties = DialogProperties(usePlatformDefaultWidth = true),
                        title = "Hidden Apps",
                        contentManagesScrolling = true,
                        externalShowTopDivider = showTopDivider,
                        externalShowBottomDivider = showBottomDivider
                    ) {
                        val hiddenAppInfos = apps.filter { it.packageName in hiddenApps }
                        LazyColumn(
                            state = listState, modifier = Modifier.heightIn(max = 400.dp)
                        ) {
                            if (hiddenAppInfos.isEmpty()) {
                                item {
                                    Text("No apps are hidden.", modifier = Modifier.padding(16.dp))
                                }
                            } else {
                                items(hiddenAppInfos) { app ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        com.xenonware.launcher.ui.res.AppIcon(
                                            app = app,
                                            iconShape = iconShape,
                                            showShadow = showShadow,
                                            size = 32.dp
                                        )
                                        Text(app.label, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { viewModel.unhideApp(app.packageName) }) {
                                            Icon(
                                                Icons.Rounded.Visibility,
                                                contentDescription = "Unhide"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Standard Settings Dialogs
            if (showThemeDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurEnabled) Modifier.hazeEffect(hazeState) else Modifier)
                        .background(if (blurEnabled) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    DialogThemeSelection(
                        themeOptions = themeOptions,
                        currentThemeIndex = dialogSelectedThemeIndex,
                        onThemeSelected = { index -> viewModel.onThemeOptionSelectedInDialog(index) },
                        onDismiss = { viewModel.dismissThemeDialog() },
                        onConfirm = { viewModel.applySelectedTheme() },
                        dialogTitle = "Theme",
                        confirmText = "OK"
                    )
                }
            }
            if (showCoverSelectionDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurEnabled) Modifier.hazeEffect(hazeState) else Modifier)
                        .background(if (blurEnabled) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    DialogCoverDisplaySelection(
                        onConfirm = {
                            viewModel.saveCoverDisplayMetrics(
                                containerSize
                            )
                        },
                        onDismiss = { viewModel.dismissCoverThemeDialog() },
                        dialogTitle = "Cover Screen",
                        confirmText = "Yes",
                        action2Text = "No",
                        descriptionText = "Is this your cover display?"
                    )
                }
            }
            if (showClearDataDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurEnabled) Modifier.hazeEffect(hazeState) else Modifier)
                        .background(if (blurEnabled) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    DialogClearDataConfirmation(
                        onConfirm = { viewModel.confirmClearData() },
                        onDismiss = { viewModel.dismissClearDataDialog() },
                        dialogTitle = "Clear Data",
                        confirmText = "Confirm",
                        descriptionText = "Are you sure you want to clear all data?"
                    )
                }
            }
            if (showResetSettingsDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurEnabled) Modifier.hazeEffect(hazeState) else Modifier)
                        .background(if (blurEnabled) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    DialogResetSettingsConfirmation(
                        onConfirm = { viewModel.confirmResetSettings() },
                        onDismiss = { viewModel.dismissResetSettingsDialog() },
                        dialogTitle = "Reset Settings",
                        confirmText = "Confirm",
                        descriptionText = "Are you sure you want to reset all settings?"
                    )
                }
            }
            if (showVersionDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurEnabled) Modifier.hazeEffect(hazeState) else Modifier)
                        .background(if (blurEnabled) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    DialogVersionNumber(
                        onDismiss = { viewModel.dismissVersionDialog() },
                        dialogTitle = "Version",
                        confirmText = "Close",
                        appString = "App Version",
                        appVersion = appVersion,
                        xenonUiString = "Xenon UI",
                        xenonUIVersion = xenonUIVersion,
                        xenonCommonsString = "Xenon Commons",
                        xenonCommonsVersion = xenonCommonsVersion
                    )
                }
            }

            if (showCalendarSelectionDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurEnabled) Modifier.hazeEffect(hazeState) else Modifier)
                        .background(if (blurEnabled) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    CalendarSelectionDialog(
                        availableCalendars = availableCalendars,
                        selectedCalendars = visibleCalendars,
                        onDismiss = { viewModel.setShowCalendarSelectionDialog(false) },
                        onToggleCalendar = { viewModel.toggleCalendarVisibility(it) },
                        onSelectAll = { viewModel.setVisibleCalendars(emptyList()) },
                        onClearAll = { viewModel.setVisibleCalendars(listOf("__NONE__")) }
                    )
                }
            }

            if (showSignOutDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurEnabled) Modifier.hazeEffect(hazeState) else Modifier)
                        .background(if (blurEnabled) Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    DialogSignOut(
                        onConfirm = onConfirmSignOut,
                        onDismiss = { viewModel.dismissSignOutDialog() },
                        dialogTitle = "Sign Out",
                        confirmText = "Confirm",
                        descriptionText = "Are you sure you want to sign out?"
                    )
                }
            }
        }
    }
}
