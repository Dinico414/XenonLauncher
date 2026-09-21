package com.xenonware.launcher

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.app.WallpaperManager.getInstance
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.VibratorManager
import android.view.WindowManager.LayoutParams
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat.setDecorFitsSystemWindows
import com.xenon.mylibrary.res.AnimatedGradientBackground
import com.xenon.mylibrary.values.NoSpacing
import com.xenonware.launcher.data.SharedPreferenceManager
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.model.FabAction
import com.xenonware.launcher.notification.LauncherNotification
import com.xenonware.launcher.ui.layouts.main.AppDrawer
import com.xenonware.launcher.ui.pages.MediaPage
import com.xenonware.launcher.ui.pages.NotificationPage
import com.xenonware.launcher.ui.pages.WidgetPage
import com.xenonware.launcher.ui.res.AppEditDialog
import com.xenonware.launcher.ui.res.CalendarSelectionDialog
import com.xenonware.launcher.ui.res.NotificationManagerDialog
import com.xenonware.launcher.ui.res.ShortcutConfigDialog
import com.xenonware.launcher.ui.res.dock.DockPill
import com.xenonware.launcher.ui.theme.FontAxes
import com.xenonware.launcher.ui.theme.FontType
import com.xenonware.launcher.ui.theme.ScreenEnvironment
import com.xenonware.launcher.ui.theme.createCustomFontFamily
import com.xenonware.launcher.ui.theme.mainFontFamily
import com.xenonware.launcher.util.DragHandler
import com.xenonware.launcher.util.PerfLog
import com.xenonware.launcher.util.WidgetConfig
import com.xenonware.launcher.util.WindowBlurBehind
import com.xenonware.launcher.util.rememberBlurAvailable
import com.xenonware.launcher.viewmodel.CalendarEvent
import com.xenonware.launcher.viewmodel.CalendarInfo
import com.xenonware.launcher.viewmodel.LauncherViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()
    private lateinit var sharedPreferenceManager: SharedPreferenceManager

    private var lastAppliedTheme: Int = -1
    private var lastAppliedCoverThemeEnabled: Boolean = false
    private var lastAppliedBlackedOutMode: Boolean = false

    companion object {
        private var bootWelcomeAlreadyShown = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val isFreshBoot = SystemClock.elapsedRealtime() < 60_000 && !bootWelcomeAlreadyShown
        if (isFreshBoot) {
            bootWelcomeAlreadyShown = true
            viewModel.setBooting(true)
            window.setFlags(LayoutParams.FLAG_NOT_TOUCHABLE, LayoutParams.FLAG_NOT_TOUCHABLE)
            window.setFlags(LayoutParams.FLAG_LAYOUT_NO_LIMITS, LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
        // Debug builds only: logs main-thread stalls under the XenonJank tag. No-op in release.
        PerfLog.init(this)

        super.onCreate(savedInstanceState)
        setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        window.addFlags(LayoutParams.FLAG_SHOW_WALLPAPER)

        sharedPreferenceManager = SharedPreferenceManager(applicationContext)

        if (!isFreshBoot && sharedPreferenceManager.isFirstLaunch) {
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
            return
        }

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

            val isAppDrawerVisible by viewModel.isAppDrawerVisible.collectAsState()
            val isBooting by viewModel.isBooting.collectAsState()
            val pagerState = rememberPagerState(initialPage = 1) { 3 }

            LaunchedEffect(viewModel) {
                viewModel.navigationEvents.collect { page ->
                    pagerState.animateScrollToPage(page)
                }
            }

            val wallpaperDarkIcons = rememberWallpaperDarkIcons()

            val appIsDarkTheme = when {
                applyCoverTheme -> true
                else -> when (themePref) {
                    0 -> false
                    1 -> true
                    else -> isSystemInDarkTheme()
                }
            }

            val statusBarDarkIcons by remember(appIsDarkTheme, wallpaperDarkIcons) {
                derivedStateOf {
                    if (isAppDrawerVisible) {
                        wallpaperDarkIcons
                    } else {
                        val pageOffset = pagerState.currentPage + pagerState.currentPageOffsetFraction
                        val mediaDarkIcons = !appIsDarkTheme

                        if (pageOffset < 1f) {
                            if (mediaDarkIcons == wallpaperDarkIcons) {
                                mediaDarkIcons
                            } else {
                                if (pageOffset < 0.5f) mediaDarkIcons else wallpaperDarkIcons
                            }
                        } else {
                            wallpaperDarkIcons
                        }
                    }
                }
            }

            val fontType by viewModel.fontType.collectAsState()
            val mainFontType by viewModel.mainFontType.collectAsState()
            val robotoSettings by viewModel.robotoFlexSettings.collectAsState()
            val googleSansSettings by viewModel.googleSansFlexSettings.collectAsState()

            val customSecondaryFontFamily = remember(fontType, robotoSettings, googleSansSettings) {
                createCustomFontFamily(
                    fontType = FontType.fromId(fontType),
                    robotoSettings = FontAxes.parseSettings(
                        robotoSettings,
                        FontAxes.ROBOTO_FLEX_AXES
                    ),
                    googleSansSettings = FontAxes.parseSettings(
                        googleSansSettings,
                        FontAxes.GOOGLE_SANS_AXES
                    )
                )
            }

            val customMainFontFamily = remember(mainFontType, robotoSettings, googleSansSettings) {
                createCustomFontFamily(
                    fontType = FontType.fromId(mainFontType),
                    robotoSettings = FontAxes.parseSettings(
                        robotoSettings,
                        FontAxes.ROBOTO_FLEX_AXES
                    ),
                    googleSansSettings = FontAxes.parseSettings(
                        googleSansSettings,
                        FontAxes.GOOGLE_SANS_AXES
                    )
                )
            }

            ScreenEnvironment(
                themePreference = themePref,
                coverTheme = applyCoverTheme,
                blackedOutModeEnabled = blackedOut,
                statusBarDarkIconsOverride = statusBarDarkIcons,
                fontFamily = customSecondaryFontFamily,
                mainFont = customMainFontFamily
            ) { _, _ ->
                if (isBooting) {
                    BackHandler(enabled = true) {}
                }

                val configuration = LocalConfiguration.current
                LaunchedEffect(configuration.orientation) {
                    viewModel.setIsLandscape(configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
                }

                LaunchedEffect(Unit) {
                    viewModel.loadCalendarEvents()
                }

                val apps by viewModel.apps.collectAsState()
                val pinnedApps by viewModel.pinnedApps.collectAsState()
                val recentlyOpened by viewModel.recentlyOpened.collectAsState()
                val isGridLayout by viewModel.isGridLayout.collectAsState()
                val appLabelsEnabled by viewModel.appLabelsEnabled.collectAsState()
                val currentTime by viewModel.currentTime.collectAsState()
                val weatherState by viewModel.weatherState.collectAsState()
                val notificationCount by viewModel.notificationCount.collectAsState()
                val notifications by viewModel.notifications.collectAsState()
                val badgeType by viewModel.notificationBadgeType.collectAsState()
                val batteryLevel by viewModel.batteryLevel.collectAsState()
                val isCharging by viewModel.isCharging.collectAsState()
                val calendarEvents by viewModel.calendarEvents.collectAsState()
                val dockSafeDrawIme by viewModel.dockSafeDrawIme.collectAsState()
                val dockSafeDrawImePortraitOnly by viewModel.dockSafeDrawImePortraitOnly.collectAsState()
                val configShortcutType by viewModel.configShortcutType.collectAsState()
                val blurSetting by viewModel.blurEnabled.collectAsState()
                val showClockAtAGlance by viewModel.showClockAtAGlance.collectAsState()
                val hideAtAGlance by viewModel.hideAtAGlance.collectAsState()
                val hideDockScrolling by viewModel.hideDockScrolling.collectAsState()
                val hideDockScrollingOnlySmall by viewModel.hideDockScrollingOnlySmall.collectAsState()
                val hideDockWidgets by viewModel.hideDockWidgets.collectAsState()
                val hideDockWidgetsLandscapeOnly by viewModel.hideDockWidgetsLandscapeOnly.collectAsState()
                val hideDockMedia by viewModel.hideDockMedia.collectAsState()
                val hideDockMediaLandscapeOnly by viewModel.hideDockMediaLandscapeOnly.collectAsState()
                val hideActionButton by viewModel.hideActionButton.collectAsState()
                val moveWebSearch by viewModel.moveWebSearch.collectAsState()
                val notificationIndicatorType by viewModel.notificationIndicatorType.collectAsState()
                val notificationMessageType by viewModel.notificationMessageType.collectAsState()

                val showCalendarSelectionDialog by viewModel.showCalendarSelectionDialog.collectAsState()
                val availableCalendars by viewModel.availableCalendars.collectAsState()
                val visibleCalendars by viewModel.visibleCalendars.collectAsState()
                val showNotificationManagerDialog by viewModel.showNotificationManagerDialog.collectAsState()
                val visibleNotificationApps by viewModel.visibleNotificationApps.collectAsState()

                val fabSingleTapAction by viewModel.fabSingleTapAction.collectAsState()
                val fabDoubleTapAction by viewModel.fabDoubleTapAction.collectAsState()
                val fabLongPressAction by viewModel.fabLongPressAction.collectAsState()
                val fabSingleTapValue by viewModel.fabSingleTapValue.collectAsState()
                val fabDoubleTapValue by viewModel.fabDoubleTapValue.collectAsState()
                val fabLongPressValue by viewModel.fabLongPressValue.collectAsState()
                val fabSwipeUpAction by viewModel.fabSwipeUpAction.collectAsState()
                val fabSwipeUpValue by viewModel.fabSwipeUpValue.collectAsState()

                LauncherScreen(
                    viewModel = viewModel,
                    apps = apps,
                    pinnedApps = pinnedApps,
                    recentlyOpened = recentlyOpened,
                    isGridLayout = isGridLayout,
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
                    dockSafeDrawImePortraitOnly = dockSafeDrawImePortraitOnly,
                    configShortcutType = configShortcutType,
                    blurSetting = blurSetting,
                    showClockAtAGlance = showClockAtAGlance,
                    hideAtAGlance = hideAtAGlance,
                    hideDockScrolling = hideDockScrolling,
                    hideDockScrollingOnlySmall = hideDockScrollingOnlySmall,
                    hideDockWidgets = hideDockWidgets,
                    hideDockWidgetsLandscapeOnly = hideDockWidgetsLandscapeOnly,
                    hideDockMedia = hideDockMedia,
                    hideDockMediaLandscapeOnly = hideDockMediaLandscapeOnly,
                    hideActionButton = hideActionButton,
                    moveWebSearch = moveWebSearch,
                    notificationIndicatorType = notificationIndicatorType,
                    notificationMessageType = notificationMessageType,
                    appLabelsEnabled = appLabelsEnabled,
                    isDarkTheme = appIsDarkTheme,
                    wallpaperDarkIcons = wallpaperDarkIcons,
                    isAppDrawerVisible = isAppDrawerVisible,
                    onAppDrawerVisibilityChange = { viewModel.setAppDrawerVisible(it) },
                    pagerState = pagerState,
                    onAppClick = { viewModel.launchApp(it) },
                    onOpenSettings = {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    },
                    fabSingleTapAction = fabSingleTapAction,
                    onFabSingleTap = {
                        if (fabSingleTapAction == FabAction.TRIGGER_ASSISTANT) {
                            showAssist(Bundle())
                        } else {
                            viewModel.executeFabAction(fabSingleTapAction, fabSingleTapValue)
                        }
                    },
                    onFabDoubleTap = {
                        if (fabDoubleTapAction == FabAction.TRIGGER_ASSISTANT) {
                            showAssist(Bundle())
                        } else {
                            viewModel.executeFabAction(fabDoubleTapAction, fabDoubleTapValue)
                        }
                    },
                    onFabLongPress = {
                        if (fabLongPressAction != FabAction.NONE) {
                            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                            val vibrator = vibratorManager.defaultVibrator
                            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                        if (fabLongPressAction == FabAction.TRIGGER_ASSISTANT) {
                            showAssist(Bundle())
                        } else {
                            viewModel.executeFabAction(fabLongPressAction, fabLongPressValue)
                        }
                    },
                    onFabSwipeUp = {
                        viewModel.executeFabAction(fabSwipeUpAction, fabSwipeUpValue)
                    },
                    showBootWelcome = isBooting,
                    onBootWelcomeFinished = {
                        viewModel.setBooting(false)
                        window.clearFlags(LayoutParams.FLAG_NOT_TOUCHABLE)
                        if (sharedPreferenceManager.isFirstLaunch) {
                            startActivity(Intent(this@MainActivity, PermissionActivity::class.java))
                            finish()
                        }
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.setForeground(true)
    }

    override fun onStop() {
        super.onStop()
        viewModel.setForeground(false)
    }

    /**
     * AppWidgetHost.startAppWidgetConfigureActivityForResult() uses the classic
     * startActivityForResult, so the widget config result lands here, not in an
     * ActivityResultLauncher. super first: ComponentActivity hands its own
     * (random, large) request codes to the Activity Result registry.
     */
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == WidgetConfig.REQUEST_CONFIGURE ||
            requestCode == WidgetConfig.REQUEST_RECONFIGURE
        ) {
            WidgetConfig.deliver(requestCode, resultCode)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadAvailableCalendars()
        viewModel.loadCalendarEvents()

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val isHomeIntent = intent.hasCategory(Intent.CATEGORY_HOME) ||
                (intent.action == Intent.ACTION_MAIN && intent.categories == null)

        if (isHomeIntent) {
            viewModel.onHomePressed()
        }
        viewModel.loadCalendarEvents()
    }

    private fun updateAppCompatDelegateTheme(themePref: Int) {
        if (themePref >= 0 && themePref < sharedPreferenceManager.themeFlag.size) {
            setDefaultNightMode(sharedPreferenceManager.themeFlag[themePref])
        } else {
            setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}

@Composable
fun rememberWallpaperDarkIcons(): Boolean {
    val context = LocalContext.current
    val wallpaperManager = remember { getInstance(context) }
    var darkIcons by remember { mutableStateOf(false) }

    DisposableEffect(wallpaperManager) {
        val listener = WallpaperManager.OnColorsChangedListener { colors, _ ->
            darkIcons = (colors?.colorHints?.and(WallpaperColors.HINT_SUPPORTS_DARK_TEXT)) != 0
        }

        val colors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        if (colors != null) {
            darkIcons = (colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0
        }
        wallpaperManager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))

        onDispose {
            wallpaperManager.removeOnColorsChangedListener(listener)
        }
    }

    return darkIcons
}

@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    apps: List<AppInfo>,
    pinnedApps: List<AppInfo>,
    recentlyOpened: List<AppInfo>,
    isGridLayout: Boolean,
    currentTime: String,
    currentDate: String,
    weatherTemp: String,
    weatherCondition: String,
    notificationCount: Int,
    notifications: List<LauncherNotification>,
    badgeType: Int,
    batteryLevel: Float,
    isCharging: Boolean,
    calendarEvents: List<CalendarEvent>,
    availableCalendars: List<CalendarInfo>,
    visibleCalendars: List<String>,
    showCalendarSelectionDialog: Boolean,
    showNotificationManagerDialog: Boolean,
    visibleNotificationApps: List<String>,
    dockSafeDrawIme: Boolean,
    dockSafeDrawImePortraitOnly: Boolean = false,
    configShortcutType: LauncherViewModel.ShortcutType?,
    blurSetting: Boolean,
    showClockAtAGlance: Boolean,
    hideAtAGlance: Boolean,
    notificationIndicatorType: Int,
    notificationMessageType: Int,
    appLabelsEnabled: Boolean,
    isDarkTheme: Boolean,
    wallpaperDarkIcons: Boolean,
    isAppDrawerVisible: Boolean,
    onAppDrawerVisibilityChange: (Boolean) -> Unit,
    pagerState: PagerState,
    onAppClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    fabSingleTapAction: FabAction,
    onFabSingleTap: () -> Unit = {},
    onFabDoubleTap: () -> Unit = {},
    onFabLongPress: () -> Unit = {},
    onFabSwipeUp: () -> Unit = {},
    hideDockScrolling: Boolean = false,
    hideDockScrollingOnlySmall: Boolean = false,
    hideDockWidgets: Boolean = false,
    hideDockWidgetsLandscapeOnly: Boolean = false,
    hideDockMedia: Boolean = false,
    hideDockMediaLandscapeOnly: Boolean = false,
    hideActionButton: Boolean = false,
    moveWebSearch: Boolean = false,
    showBootWelcome: Boolean = false,
    onBootWelcomeFinished: () -> Unit = {}
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val windowWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val hazeState = rememberHazeState()
    val screenHazeState = rememberHazeState()
    var appToEdit by remember { mutableStateOf<AppInfo?>(null) }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = true) {
        if (isAppDrawerVisible) {
            onAppDrawerVisibilityChange(false)
        } else if (pagerState.currentPage != 1) {
            scope.launch {
                pagerState.animateScrollToPage(1)
            }
        }
    }

    var drawerInteractiveProgress by remember { mutableFloatStateOf(1f) }

    var isSearchActiveInDrawer by remember { mutableStateOf(false) }
    var closeSearchTrigger by remember { mutableIntStateOf(0) }
    var isDockVisibleByScroll by remember { mutableStateOf(true) }

    var notificationShift by remember { mutableFloatStateOf(0f) }

    val replyingNotificationKey by viewModel.replyingNotificationKey.collectAsState()
    val isReplyingToNotification = replyingNotificationKey != null

    LaunchedEffect(isAppDrawerVisible) {
        if (isAppDrawerVisible) viewModel.setReplyingNotification(null)
    }

    val appDrawerBlurProgress by animateFloatAsState(
        targetValue = if (isAppDrawerVisible && !showBootWelcome) drawerInteractiveProgress else 0f,
        animationSpec = if (drawerInteractiveProgress < 0.99f && isAppDrawerVisible) {
            snap()
        } else {
            tween(durationMillis = 250)
        },
        label = "blurProgress"
    )

    val mediaProgress: () -> Float = {
        if (showBootWelcome) 0f
        else 1f - (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
    }

    val blurAvailable = rememberBlurAvailable() && blurSetting && !showBootWelcome

    val windowBlurRadiusPx by remember(showBootWelcome) {
        derivedStateOf {
            val mediaBlur = (2f * mediaProgress()).coerceIn(0f, 1f)
            val p = appDrawerBlurProgress.coerceAtLeast(mediaBlur)
            ((30 * p).toInt() / 6) * 6
        }
    }

    val liveContentAlpha by animateFloatAsState(
        targetValue = if (appToEdit != null && blurAvailable) 0f else 1f,
        // Cross-fade in; snap back so nothing blinks when the dialog closes
        animationSpec = if (appToEdit != null) tween(durationMillis = 250) else snap(),
        label = "liveContentAlpha"
    )
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isImeVisible = WindowInsets.ime.asPaddingValues().calculateBottomPadding() > NoSpacing

    val iconShape by viewModel.drawerIconShape.collectAsState()
    val showShadow by viewModel.drawerIconShadow.collectAsState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isDockHiddenByWidgetPage = hideDockWidgets && (!hideDockWidgetsLandscapeOnly || isLandscape)
    val isDockHiddenByMediaPage = hideDockMedia && (!hideDockMediaLandscapeOnly || isLandscape)

    LaunchedEffect(isAppDrawerVisible) {
        if (!isAppDrawerVisible) {
            focusManager.clearFocus()
            keyboardController?.hide()
            drawerInteractiveProgress = 1f
            isSearchActiveInDrawer = false
            closeSearchTrigger = 0
            appToEdit = null
        }
    }

    WindowBlurBehind(radiusPx = if (blurSetting && !showBootWelcome) windowBlurRadiusPx else 0)

    DragHandler {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = liveContentAlpha }
                    .then(
                        if (appToEdit != null && blurAvailable) Modifier.hazeSource(screenHazeState)
                        else Modifier
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (blurSetting && !showBootWelcome) Modifier.hazeSource(state = hazeState) else Modifier)
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
                            beyondViewportPageCount = 2,
                            userScrollEnabled = !dragDropState.isDragging
                        ) { page ->
                            when (page) {
                                0 -> Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex(1f)
                                ) {
                                    MediaPage(
                                        mediaState = viewModel.mediaState,
                                        progress = mediaProgress,
                                        isPermissionGranted = viewModel.isMediaPermissionGranted,
                                        isDarkTheme = isDarkTheme,
                                        isDockVisible = !isDockHiddenByMediaPage,
                                        onOpenSettings = { viewModel.openNotificationAccessSettings() },
                                        onTogglePlayPause = { viewModel.togglePlayPause() },
                                        onSkipNext = { viewModel.skipNext() },
                                        onSkipPrevious = { viewModel.skipPrevious() },
                                        onSeek = { viewModel.seekTo(it) },
                                    )
                                }
                                1 -> Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .zIndex(0f)
                                        .clipToBounds()
                                        .graphicsLayer {
                                            val p = mediaProgress()
                                            if (p > 0f) {
                                                translationX = -0.25f * size.width * p
                                                alpha = 1f - p
                                                scaleX = 1f - 0.06f * p
                                                scaleY = 1f - 0.06f * p
                                                compositingStrategy = CompositingStrategy.Offscreen
                                            }
                                        }
                                ) {
                                    NotificationPage(
                                        viewModel = viewModel,
                                        notificationCount = notificationCount,
                                        currentTime = currentTime,
                                        currentDate = currentDate,
                                        showClock = showClockAtAGlance,
                                        hideAtAGlance = hideAtAGlance,
                                        indicatorType = notificationIndicatorType,
                                        messageType = notificationMessageType,
                                        notifications = notifications,
                                        apps = apps,
                                        calendarEvents = calendarEvents,
                                        hazeState = hazeState,
                                        blurSetting = blurSetting,
                                        wallpaperDarkIcons = wallpaperDarkIcons,
                                        onDismissNotification = { viewModel.dismissNotification(it) },
                                        onDismissAllNotifications = { viewModel.dismissAllNotifications() },
                                        onOpenSettings = onOpenSettings,
                                        onContentShiftChanged = { notificationShift = it }
                                    )
                                }
                                2 -> WidgetPage(
                                    viewModel = viewModel,
                                    isDockVisible = !isDockHiddenByWidgetPage,
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
                                val lerp = if (isDarkTheme) 0.5f else 0.15f
                                lerp(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f), Color.Black.copy(alpha = 0.2f), lerp)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            onAppClick = onAppClick,
                            onSettingsClick = onOpenSettings,
                            onDismiss = { onAppDrawerVisibilityChange(false) },
                            isVisible = isAppDrawerVisible,
                            onPinApp = { pkg, index -> viewModel.pinApp(pkg, index) },
                            isGridLayout = isGridLayout,
                            onToggleLayout = { viewModel.setGridLayout(!isGridLayout) },
                            onProgress = { drawerInteractiveProgress = it },
                            blurEnabled = blurSetting,
                            onSearchActiveChange = { isSearchActiveInDrawer = it },
                            closeSearchTrigger = closeSearchTrigger,
                            showLabels = appLabelsEnabled,
                            hideDockScrolling = hideDockScrolling,
                            onDockVisibilityChange = { isDockVisibleByScroll = it },
                            moveWebSearch = moveWebSearch,
                            onEditApp = { appToEdit = it }
                        )
                    }
                }

                // DOCK LAYER
                val configuration = LocalConfiguration.current
                val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                val isSmallDevice = windowWidthDp < 400.dp

                val shouldAnimateDockOff = if (hideDockScrollingOnlySmall) {
                    isLandscape || isSmallDevice
                } else {
                    true
                }

                val isOnWidgetPage = pagerState.currentPage == 2
                val isOnMediaPage = pagerState.currentPage == 0

                val isDockHiddenByPage = (isOnWidgetPage && isDockHiddenByWidgetPage) || (isOnMediaPage && isDockHiddenByMediaPage)

                val dockYOffset by animateDpAsState(
                    targetValue = if (isDockVisibleByScroll && !isDockHiddenByPage || !isAppDrawerVisible && !isDockHiddenByPage || !shouldAnimateDockOff && !isDockHiddenByPage) 0.dp else 120.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "dockYOffset"
                )

                DockPill(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset { IntOffset(0, -notificationShift.roundToInt() + density.run { dockYOffset.roundToPx() }) },
                    apps = pinnedApps,
                    notifications = notifications,
                    badgeType = badgeType,
                    mediaState = viewModel.mediaState,
                    isMediaPermissionGranted = viewModel.isMediaPermissionGranted,
                    notificationCount = notificationCount,
                    calendarEventCount = calendarEvents.size,
                    currentTime = currentTime,
                    currentDate = currentDate,
                    weatherTemp = weatherTemp,
                    weatherCondition = weatherCondition,
                    onAppClick = onAppClick,
                    onFabClick = {
                        if (isImeVisible) {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        } else if (isAppDrawerVisible && isSearchActiveInDrawer && drawerInteractiveProgress > 0.99f) {
                            closeSearchTrigger++
                        } else {
                            onFabSingleTap()
                        }
                    },
                    onMediaPlayPause = { viewModel.togglePlayPause() },
                    onMediaSkipNext = { viewModel.skipNext() },
                    onOpenMediaPermission = { viewModel.openNotificationAccessSettings() },
                    onTimeClick = { viewModel.handleShortcutClick(LauncherViewModel.ShortcutType.TIME) },
                    onDateClick = { viewModel.handleShortcutClick(LauncherViewModel.ShortcutType.DATE) },
                    onWeatherClick = { viewModel.handleShortcutClick(LauncherViewModel.ShortcutType.WEATHER) },
                    onFabDoubleTap = onFabDoubleTap,
                    onFabLongPress = onFabLongPress,
                    onFabSwipeUp = onFabSwipeUp,
                    isAppDrawerVisible = isAppDrawerVisible,
                    hazeState = if (blurSetting) hazeState else null,
                    progress = batteryLevel,
                    isCharging = isCharging,
                    hideActionButton = hideActionButton,
                    fabSingleTapAction = fabSingleTapAction,
                    dockSafeDrawIme = dockSafeDrawIme && !isReplyingToNotification,
                    dockSafeDrawImePortraitOnly = dockSafeDrawImePortraitOnly,
                    onUnpinApp = { viewModel.unpinApp(it) },
                    onPinApp = { pkg, index -> viewModel.pinApp(pkg, index) },
                    onReorderApp = { from, to -> viewModel.reorderPinnedApp(from, to) }
                )
            }

            appToEdit?.let { app ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeEffect(screenHazeState)
                ) {
                    AppEditDialog(
                        app = app,
                        viewModel = viewModel,
                        onDismiss = { appToEdit = null }
                    )
                }
            }

            configShortcutType?.let { type ->
                val context = LocalContext.current
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
            }

            // BOOT WELCOME OVERLAY
            if (showBootWelcome) {
                val alpha = remember { Animatable(0f) }
                val backgroundAlpha = remember { Animatable(0f) }

                LaunchedEffect(Unit) {
                    backgroundAlpha.animateTo(1f, tween(1000))
                    alpha.animateTo(1f, tween(1000))
                    delay(2000.milliseconds)
                    backgroundAlpha.animateTo(0f, tween(1000))
                    alpha.animateTo(0f, tween(1000))
                    onBootWelcomeFinished()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(999f)
                        .pointerInput(Unit) {} // Consume all touches
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {} // Secondary touch blocking
                ) {
                    Box(modifier = Modifier.fillMaxSize().alpha(backgroundAlpha.value)) {
                        AnimatedGradientBackground(modifier = Modifier.fillMaxSize()) {}
                    }
                    Text(
                        text = stringResource(R.string.welcome),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .alpha(alpha.value),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = mainFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}