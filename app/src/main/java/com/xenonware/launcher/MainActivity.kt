package com.xenonware.launcher

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.xenonware.launcher.data.SharedPreferenceManager
import com.xenonware.launcher.ui.layouts.main.AppDrawer
import com.xenonware.launcher.ui.pages.MediaPage
import com.xenonware.launcher.ui.pages.NotificationPage
import com.xenonware.launcher.ui.pages.WidgetPage
import com.xenonware.launcher.ui.res.CalendarSelectionDialog
import com.xenonware.launcher.ui.res.ShortcutConfigDialog
import com.xenonware.launcher.ui.res.dock.DockPill
import com.xenonware.launcher.ui.theme.ScreenEnvironment
import com.xenonware.launcher.util.DragHandler
import com.xenonware.launcher.util.WindowBlurBehind
import com.xenonware.launcher.util.rememberBlurAvailable
import com.xenonware.launcher.viewmodel.LauncherViewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var sharedPreferenceManager: SharedPreferenceManager

    private var lastAppliedTheme: Int = -1
    private var lastAppliedCoverThemeEnabled: Boolean = false
    private var lastAppliedBlackedOutMode: Boolean = false

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)

        sharedPreferenceManager = SharedPreferenceManager(applicationContext)

        val initialThemePref = sharedPreferenceManager.theme
        val initialCoverThemeEnabledSetting = sharedPreferenceManager.coverThemeEnabled
        val initialBlackedOutMode = sharedPreferenceManager.blackedOutModeEnabled

        updateAppCompatDelegateTheme(initialThemePref)

        lastAppliedTheme = initialThemePref
        lastAppliedCoverThemeEnabled = initialCoverThemeEnabledSetting
        lastAppliedBlackedOutMode = initialBlackedOutMode

        setContent {
            val themePref by viewModel.theme.collectAsState()
            val blackedOut by viewModel.blackedOutModeEnabled.collectAsState()
            val coverThemeEnabled by viewModel.coverThemeEnabled.collectAsState()
            val currentContainerSize = LocalWindowInfo.current.containerSize
            val applyCoverTheme = remember(currentContainerSize, coverThemeEnabled) {
                viewModel.isCoverThemeApplied(currentContainerSize)
            }

            ScreenEnvironment(
                themePreference = themePref,
                coverTheme = applyCoverTheme,
                blackedOutModeEnabled = blackedOut
            ) { _, _ ->
                val permissions = mutableListOf(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_CALENDAR
                )

                permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)

                val permissionsState = rememberMultiplePermissionsState(permissions = permissions)

                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                LaunchedEffect(configuration.orientation) {
                    viewModel.setIsLandscape(configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE)
                }

                LaunchedEffect(Unit) {
                    permissionsState.launchMultiplePermissionRequest()
                }

                val apps by viewModel.apps.collectAsState()
                val pinnedApps by viewModel.pinnedApps.collectAsState()
                val recentlyOpened by viewModel.recentlyOpened.collectAsState()
                val isGridLayout by viewModel.isGridLayout.collectAsState()
                val openKeyboard by viewModel.openKeyboard.collectAsState()
                val currentTime by viewModel.currentTime.collectAsState()
                val weatherState by viewModel.weatherState.collectAsState()
                val notificationCount by viewModel.notificationCount.collectAsState()
                val notifications by viewModel.notifications.collectAsState()
                val badgeType by viewModel.notificationBadgeType.collectAsState()
                val batteryLevel by viewModel.batteryLevel.collectAsState()
                val isCharging by viewModel.isCharging.collectAsState()
                val calendarEvents by viewModel.calendarEvents.collectAsState()
                val dockSafeDrawIme by viewModel.dockSafeDrawIme.collectAsState()
                val configShortcutType by viewModel.configShortcutType.collectAsState()
                val blurSetting by viewModel.blurEnabled.collectAsState()
                
                val showCalendarSelectionDialog by viewModel.showCalendarSelectionDialog.collectAsState()
                val availableCalendars by viewModel.availableCalendars.collectAsState()
                val visibleCalendars by viewModel.visibleCalendars.collectAsState()
                val showNotificationManagerDialog by viewModel.showNotificationManagerDialog.collectAsState()
                val visibleNotificationApps by viewModel.visibleNotificationApps.collectAsState()

                LauncherScreen(
                    viewModel = viewModel,
                    apps = apps,
                    pinnedApps = pinnedApps,
                    recentlyOpened = recentlyOpened,
                    isGridLayout = isGridLayout,
                    openKeyboard = openKeyboard,
                    currentTime = currentTime.format(viewModel.timeFormatter),
                    currentDate = currentTime.format(viewModel.dateFormatter),
                    weatherTemp = weatherState.temperature,
                    weatherCondition = weatherState.condition,
                    notificationCount = notificationCount,
                    notifications = notifications,
                    badgeType = badgeType,
                    batteryLevel = batteryLevel,
                    isCharging = isCharging,
                    calendarEvents = calendarEvents,
                    availableCalendars = availableCalendars,
                    visibleCalendars = visibleCalendars,
                    showCalendarSelectionDialog = showCalendarSelectionDialog,
                    showNotificationManagerDialog = showNotificationManagerDialog,
                    visibleNotificationApps = visibleNotificationApps,
                    dockSafeDrawIme = dockSafeDrawIme,
                    configShortcutType = configShortcutType,
                    blurSetting = blurSetting,
                    onAppClick = { viewModel.launchApp(it) },
                    onOpenSettings = {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val currentThemePref = sharedPreferenceManager.theme
        val currentCoverThemeEnabledSetting = sharedPreferenceManager.coverThemeEnabled
        val currentBlackedOutMode = sharedPreferenceManager.blackedOutModeEnabled

        if (currentThemePref != lastAppliedTheme ||
            currentCoverThemeEnabledSetting != lastAppliedCoverThemeEnabled ||
            currentBlackedOutMode != lastAppliedBlackedOutMode
        ) {
            if (currentThemePref != lastAppliedTheme) {
                updateAppCompatDelegateTheme(currentThemePref)
            }

            lastAppliedTheme = currentThemePref
            lastAppliedCoverThemeEnabled = currentCoverThemeEnabledSetting
            lastAppliedBlackedOutMode = currentBlackedOutMode

            recreate()
        }
    }

    private fun updateAppCompatDelegateTheme(themePref: Int) {
        if (themePref >= 0 && themePref < sharedPreferenceManager.themeFlag.size) {
            AppCompatDelegate.setDefaultNightMode(sharedPreferenceManager.themeFlag[themePref])
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}

@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    apps: List<com.xenonware.launcher.model.AppInfo>,
    pinnedApps: List<com.xenonware.launcher.model.AppInfo>,
    recentlyOpened: List<com.xenonware.launcher.model.AppInfo>,
    isGridLayout: Boolean,
    openKeyboard: Boolean,
    currentTime: String,
    currentDate: String,
    weatherTemp: String,
    weatherCondition: String,
    notificationCount: Int,
    notifications: List<com.xenonware.launcher.notification.LauncherNotification>,
    badgeType: Int,
    batteryLevel: Float,
    isCharging: Boolean,
    calendarEvents: List<com.xenonware.launcher.viewmodel.CalendarEvent>,
    availableCalendars: List<com.xenonware.launcher.viewmodel.CalendarInfo>,
    visibleCalendars: List<String>,
    showCalendarSelectionDialog: Boolean,
    showNotificationManagerDialog: Boolean,
    visibleNotificationApps: List<String>,
    dockSafeDrawIme: Boolean,
    configShortcutType: LauncherViewModel.ShortcutType?,
    blurSetting: Boolean,
    onAppClick: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val hazeState = rememberHazeState()
    val pagerState = rememberPagerState(initialPage = 1) { 3 }
    var isAppDrawerVisible by remember { mutableStateOf(false) }
    var drawerInteractiveProgress by remember { mutableFloatStateOf(1f) }
    
    var isSearchActiveInDrawer by remember { mutableStateOf(false) }
    var closeSearchTrigger by remember { mutableIntStateOf(0) }

    val appDrawerBlurProgress by animateFloatAsState(
        targetValue = if (isAppDrawerVisible) drawerInteractiveProgress else 0f,
        animationSpec = if (drawerInteractiveProgress < 0.99f && isAppDrawerVisible) {
            snap()
        } else {
            tween(durationMillis = 250)
        },
        label = "blurProgress"
    )

    val mediaBlurProgress = 1f - (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
    val blurProgress = appDrawerBlurProgress.coerceAtLeast(mediaBlurProgress)

    val blurAvailable = rememberBlurAvailable() && blurSetting
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val iconShape by viewModel.drawerIconShape.collectAsState()
    val showShadow by viewModel.drawerIconShadow.collectAsState()

    LaunchedEffect(isAppDrawerVisible) {
        if (!isAppDrawerVisible) {
            focusManager.clearFocus()
            keyboardController?.hide()
            drawerInteractiveProgress = 1f
            isSearchActiveInDrawer = false
            closeSearchTrigger = 0
        }
    }

    WindowBlurBehind(radiusPx = if (blurSetting) (30 * blurProgress).toInt() else 0)

    DragHandler {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurSetting) Modifier.hazeSource(state = hazeState) else Modifier)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (blurAvailable) {
                                Modifier.blur(radius = (20 * appDrawerBlurProgress).dp)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    val dragDropState = com.xenonware.launcher.util.LocalDragDropState.current
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1,
                        userScrollEnabled = !dragDropState.isDragging
                    ) { page ->
                        when (page) {
                            0 -> MediaPage(
                                mediaState = viewModel.mediaState,
                                progress = mediaBlurProgress,
                                isPermissionGranted = viewModel.isMediaPermissionGranted,
                                onOpenSettings = { viewModel.openNotificationAccessSettings() },
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSkipNext = { viewModel.skipNext() },
                                onSkipPrevious = { viewModel.skipPrevious() },
                                onSeek = { viewModel.seekTo(it) },
                                onOpenSource = { viewModel.openMediaApp() }
                            )
                            1 -> NotificationPage(
                                viewModel = viewModel,
                                notificationCount = notificationCount,
                                currentDate = currentDate,
                                notifications = notifications,
                                apps = apps,
                                calendarEvents = calendarEvents,
                                hazeState = hazeState,
                                blurSetting = blurSetting,
                                onDismissNotification = { viewModel.dismissNotification(it) },
                                onDismissAllNotifications = { viewModel.dismissAllNotifications() },
                                onOpenSettings = onOpenSettings
                            )
                            2 -> WidgetPage(
                                viewModel = viewModel,
                                onOpenSettings = onOpenSettings
                            )
                        }
                    }
                }

                // APP LIST
                AnimatedVisibility(
                    visible = isAppDrawerVisible,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppDrawer(
                        viewModel = viewModel,
                        apps = apps,
                        recentlyOpened = recentlyOpened,
                        containerColor = if (blurAvailable) {
                            val lerp = if (isSystemInDarkTheme()) 0.5f else 0.15f
                            lerp(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f), Color.Black.copy(alpha = 0.2f), lerp)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        onAppClick = onAppClick,
                        onSettingsClick = onOpenSettings,
                        onDismiss = { isAppDrawerVisible = false },
                        isVisible = isAppDrawerVisible,
                        onPinApp = { pkg, index -> viewModel.pinApp(pkg, index) },
                        isGridLayout = isGridLayout,
                        onToggleLayout = { viewModel.setGridLayout(!isGridLayout) },
                        openKeyboard = openKeyboard,
                        onToggleOpenKeyboard = { viewModel.setOpenKeyboard(!openKeyboard) },
                        onProgress = { drawerInteractiveProgress = it },
                        blurEnabled = blurSetting,
                        onSearchActiveChange = { isSearchActiveInDrawer = it },
                        closeSearchTrigger = closeSearchTrigger
                    )
                }
            }

                // DOCK LAYER
                DockPill(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    apps = pinnedApps,
                    notifications = notifications,
                    badgeType = badgeType,
                    mediaState = viewModel.mediaState,
                    isMediaPermissionGranted = viewModel.isMediaPermissionGranted,
                    notificationCount = notificationCount,
                    currentTime = currentTime,
                    currentDate = currentDate,
                    weatherTemp = weatherTemp,
                    weatherCondition = weatherCondition,
                    onAppClick = onAppClick,
                    onSettingsClick = onOpenSettings,
                    onFabClick = {
                        if (isAppDrawerVisible && isSearchActiveInDrawer && drawerInteractiveProgress > 0.99f) {
                            closeSearchTrigger++
                        } else {
                            isAppDrawerVisible = !isAppDrawerVisible
                        }
                    },
                    onMediaPlayPause = { viewModel.togglePlayPause() },
                    onMediaSkipNext = { viewModel.skipNext() },
                    onOpenMediaPermission = { viewModel.openNotificationAccessSettings() },
                    onTimeClick = { viewModel.handleShortcutClick(LauncherViewModel.ShortcutType.TIME) },
                    onDateClick = { viewModel.handleShortcutClick(LauncherViewModel.ShortcutType.DATE) },
                    onWeatherClick = { viewModel.handleShortcutClick(LauncherViewModel.ShortcutType.WEATHER) },
                    isAppDrawerVisible = isAppDrawerVisible,
                    hazeState = if (blurSetting) hazeState else null,
                    progress = batteryLevel,
                    isCharging = isCharging,
                    dockSafeDrawIme = dockSafeDrawIme,
                    onUnpinApp = { viewModel.unpinApp(it) },
                    onPinApp = { pkg, index -> viewModel.pinApp(pkg, index) },
                    onReorderApp = { from, to -> viewModel.reorderPinnedApp(from, to) }
                )
            
            configShortcutType?.let { type ->
                val context = androidx.compose.ui.platform.LocalContext.current
                val prefs = remember { SharedPreferenceManager(context) }
                val initialValue = when (type) {
                    LauncherViewModel.ShortcutType.TIME -> prefs.timeShortcut
                    LauncherViewModel.ShortcutType.DATE -> prefs.dateShortcut
                    LauncherViewModel.ShortcutType.WEATHER -> prefs.weatherShortcut
                }
                
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
            
            if (showCalendarSelectionDialog) {
                CalendarSelectionDialog(
                    availableCalendars = availableCalendars,
                    selectedCalendars = visibleCalendars,
                    onDismiss = { viewModel.setShowCalendarSelectionDialog(false) },
                    onToggleCalendar = { viewModel.toggleCalendarVisibility(it) },
                    onSelectAll = { viewModel.setVisibleCalendars(emptyList()) },
                    onClearAll = { viewModel.setVisibleCalendars(listOf("__NONE__")) }
                )
            }

            if (showNotificationManagerDialog) {
                com.xenonware.launcher.ui.res.NotificationManagerDialog(
                    allApps = apps,
                    visibleApps = visibleNotificationApps,
                    onDismiss = { viewModel.setShowNotificationManagerDialog(false) },
                    onToggleApp = { viewModel.toggleNotificationAppVisibility(it) },
                    onSelectAll = { viewModel.setVisibleNotificationApps(emptyList()) },
                    onClearAll = { viewModel.setVisibleNotificationApps(listOf("__NONE__")) },
                    iconShape = iconShape,
                    showShadow = showShadow
                )
            }
        }
    }
}
