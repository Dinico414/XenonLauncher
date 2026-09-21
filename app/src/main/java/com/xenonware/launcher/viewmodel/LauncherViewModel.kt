package com.xenonware.launcher.viewmodel

import android.Manifest
import android.accounts.Account
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks2
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraManager
import android.location.Location
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import android.util.Log
import android.util.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.xenonware.launcher.R
import com.xenonware.launcher.SplitScreenPickerActivity
import com.xenonware.launcher.accessibility.XenonAccessibilityService
import com.xenonware.launcher.data.LauncherCache
import com.xenonware.launcher.data.SharedPreferenceManager
import com.xenonware.launcher.media.AudioSpectrumAnalyzer
import com.xenonware.launcher.media.MediaControllerManager
import com.xenonware.launcher.media.MediaState
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.model.AppOverride
import com.xenonware.launcher.model.AppWidgetGroup
import com.xenonware.launcher.model.FabAction
import com.xenonware.launcher.model.SearchHistoryEntry
import com.xenonware.launcher.model.SearchHistoryType
import com.xenonware.launcher.model.SearchResult
import com.xenonware.launcher.model.WidgetItem
import com.xenonware.launcher.model.WidgetPickerItemData
import com.xenonware.launcher.notification.NotificationManager
import com.xenonware.launcher.notification.XenonNotificationService
import com.xenonware.launcher.ui.res.IconShape
import com.xenonware.launcher.util.ColorUtils
import com.xenonware.launcher.util.generateCustomIcon
import com.xenonware.launcher.util.getIconPackMap
import com.xenonware.launcher.util.loadIconFromPack
import com.xenonware.launcher.util.matches
import com.xenonware.launcher.util.matchesSearch
import com.xenonware.launcher.util.normalizeIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Collections
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

data class WeatherState(
    val temperature: String,
    val condition: String,
    val maxTemp: String? = null,
    val minTemp: String? = null,
    val dailyCondition: String? = null
)

data class CalendarInfo(
    val id: String,
    val name: String,
    val color: Int,
    val accountName: String,
    val syncEvents: Boolean = true,
    val visible: Boolean = true,
    val accountType: String = ""
)

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val location: String?,
    val isAllDay: Boolean,
    val calendarId: String,
    val color: Int? = null
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "LauncherViewModel"

        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

        private const val LOCATION_MAX_AGE_MS = 30L * 60 * 1000
        private const val LOCATION_FIX_TIMEOUT_MS = 10_000L

        /** Weather younger than this is shown from cache without hitting the network. */
        private const val WEATHER_TTL_MS = 15L * 60 * 1000

        /** Package broadcasts come in bursts (an update = REMOVED + ADDED + CHANGED). */
        private const val PACKAGE_EVENT_DEBOUNCE_MS = 500L

        /** Many triggers (onStart, observer, provider broadcast, tick) collapse into one query. */
        private const val CALENDAR_DEBOUNCE_MS = 250L

        private val sharedApps = MutableStateFlow<List<AppInfo>>(emptyList())

        val launchableApps: StateFlow<List<AppInfo>> = sharedApps
    }

    private val prefManager = SharedPreferenceManager(application)

    // ---------------------------------------------------------------------------------
    // Lifecycle / performance state.
    // Declared BEFORE init {}: Kotlin runs initializers in textual order, so anything
    // declared after init would be reset (or still null) while init is using it.
    // ---------------------------------------------------------------------------------

    private val cache = LauncherCache(application)

    /** finishInitialization() must run exactly once per ViewModel. */
    private val initialized = AtomicBoolean(false)

    /**
     * True between MainActivity.onStart and onStop. Everything that only matters while the
     * launcher is visible (clock, media polling, weather, calendar, battery) pauses otherwise.
     */
    private val _isForeground = MutableStateFlow(false)

    private var foregroundReceiversRegistered = false

    /** Set when packages changed while in background; rescanned on return. */
    @Volatile
    private var appsDirty = false

    @Volatile
    private var calendarDirty = false

    private var appsJob: Job? = null
    private var calendarJob: Job? = null
    private var weatherJob: Job? = null

    /** In-memory icon cache: "package/activity" -> rendered app, reused while unchanged. */
    private val appMemCache = ConcurrentHashMap<String, LauncherCache.CachedApp>()

    /** Each calendar gets exactly one automatic sync-enable attempt per process. */
    private val syncAttemptedCalendars: MutableSet<String> =
        Collections.synchronizedSet(HashSet())

    private var torchCallback: CameraManager.TorchCallback? = null

    /**
     * FFT of the global audio output for the media visualizer. Only captures while the launcher
     * is in foreground, the media page is visible, RECORD_AUDIO is granted and media is playing
     * (see syncAudioAnalyzer). Declared before init: the media loop started from init uses it.
     */
    val audioAnalyzer = AudioSpectrumAnalyzer()

    private val _audioPermissionGranted = MutableStateFlow(hasAudioPermission())
    val audioPermissionGranted: StateFlow<Boolean> = _audioPermissionGranted

    private val _isMediaPageVisible = MutableStateFlow(false)
    val isMediaPageVisible: StateFlow<Boolean> = _isMediaPageVisible

    /** Language + dark mode the app labels/icons were last built for. */
    @Volatile
    private var lastConfigKey: String = ""

    /**
     * Fires on every configuration change of the process (system language, per-app language,
     * dark mode), even while MainActivity is being recreated — the ViewModel survives that,
     * so without this the drawer kept the old labels until the next package change.
     */
    private val configCallbacks = object : ComponentCallbacks2 {
        override fun onConfigurationChanged(newConfig: Configuration) {
            onConfigMaybeChanged()
        }

        @Deprecated("Deprecated in Java")
        override fun onLowMemory() {}

        override fun onTrimMemory(level: Int) {}
    }

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "is_grid_layout" -> _isGridLayout.value = prefManager.isGridLayout
            "notification_badge_type" -> _notificationBadgeType.value = prefManager.notificationBadgeType
            "open_keyboard" -> _openKeyboard.value = prefManager.openKeyboard
            "open_keyboard_portrait_only" -> _openKeyboardPortraitOnly.value = prefManager.openKeyboardPortraitOnly
            "widget_columns_portrait", "widget_columns_landscape" -> {
                _widgetColumns.value = if (_isLandscape.value) prefManager.widgetColumnsLandscape else prefManager.widgetColumnsPortrait
            }
            "widget_layout_portrait", "widget_layout_landscape" -> loadWidgets()
            "advanced_search_enabled" -> _advancedSearchEnabled.value = prefManager.advancedSearchEnabled
            "search_history" -> _searchHistory.value = loadSearchHistory()
            "dock_safedraw_ime" -> _dockSafeDrawIme.value = prefManager.dockSafeDrawIme
            "dock_safedraw_ime_portrait_only" -> _dockSafeDrawImePortraitOnly.value = prefManager.dockSafeDrawImePortraitOnly
            "show_hidden_apps_in_search" -> _showHiddenAppsInSearch.value = prefManager.showHiddenAppsInSearch
            "hidden_apps" -> {
                _hiddenApps.value = prefManager.hiddenApps.toSet()
                loadApps()
            }
            "drawer_icon_shape" -> {
                _drawerIconShape.value = IconShape.valueOf(prefManager.drawerIconShape)
                loadApps()
            }
            "drawer_icon_shadow" -> _drawerIconShadow.value = prefManager.drawerIconShadow
            "app_labels_enabled" -> _appLabelsEnabled.value = prefManager.appLabelsEnabled
            "blur_enabled" -> _blurEnabled.value = prefManager.blurEnabled
            "app_overrides" -> loadApps()
            "visible_calendars" -> {
                _visibleCalendars.value = prefManager.visibleCalendars
                loadCalendarEvents()
            }
            "visible_notification_apps" -> {
                _visibleNotificationApps.value = prefManager.visibleNotificationApps
            }
            "theme" -> _theme.value = prefManager.theme
            "blacked_out_mode_enabled" -> _blackedOutModeEnabled.value = prefManager.blackedOutModeEnabled
            "cover_theme_enabled" -> _coverThemeEnabled.value = prefManager.coverThemeEnabled
            "fab_single_tap_action" -> _fabSingleTapAction.value = FabAction.fromString(prefManager.fabSingleTapAction)
            "fab_double_tap_action" -> _fabDoubleTapAction.value = FabAction.fromString(prefManager.fabDoubleTapAction)
            "fab_long_press_action" -> _fabLongPressAction.value = FabAction.fromString(prefManager.fabLongPressAction)
            "fab_single_tap_value" -> _fabSingleTapValue.value = prefManager.fabSingleTapValue
            "fab_double_tap_value" -> _fabDoubleTapValue.value = prefManager.fabDoubleTapValue
            "fab_long_press_value" -> _fabLongPressValue.value = prefManager.fabLongPressValue
            "fab_swipe_up_action" -> _fabSwipeUpAction.value = FabAction.fromString(prefManager.fabSwipeUpAction)
            "fab_swipe_up_value" -> _fabSwipeUpValue.value = prefManager.fabSwipeUpValue
            "global_icon_pack" -> {
                _globalIconPack.value = prefManager.globalIconPack
                loadApps()
            }
            "show_clock_at_a_glance" -> _showClockAtAGlance.value = prefManager.showClockAtAGlance
            "hide_at_a_glance" -> _hideAtAGlance.value = prefManager.hideAtAGlance
            "hide_dock_scrolling" -> _hideDockScrolling.value = prefManager.hideDockScrolling
            "hide_dock_scrolling_only_small" -> _hideDockScrollingOnlySmall.value = prefManager.hideDockScrollingOnlySmall
            "hide_dock_widgets" -> _hideDockWidgets.value = prefManager.hideDockWidgets
            "hide_dock_widgets_landscape_only" -> _hideDockWidgetsLandscapeOnly.value = prefManager.hideDockWidgetsLandscapeOnly
            "hide_dock_media" -> _hideDockMedia.value = prefManager.hideDockMedia
            "hide_dock_media_landscape_only" -> _hideDockMediaLandscapeOnly.value = prefManager.hideDockMediaLandscapeOnly
            "hide_action_button" -> _hideActionButton.value = prefManager.hideActionButton
            "move_web_search" -> _moveWebSearch.value = prefManager.moveWebSearch
            "show_mute_notifications" -> {
                _showMuteNotifications.value = prefManager.showMuteNotifications
                NotificationManager.showMuteNotifications = prefManager.showMuteNotifications
            }
            "show_permanent_notifications" -> {
                _showPermanentNotifications.value = prefManager.showPermanentNotifications
                NotificationManager.showPermanentNotifications = prefManager.showPermanentNotifications
            }
            "disable_grouping" -> {
                _disableGrouping.value = prefManager.disableGrouping
                NotificationManager.disableGrouping = prefManager.disableGrouping
            }
            "notification_delete_single_press" -> _notificationDeleteSinglePress.value = prefManager.notificationDeleteSinglePress
            "notification_indicator_type" -> _notificationIndicatorType.value = prefManager.notificationIndicatorType
            "notification_message_type" -> _notificationMessageType.value = prefManager.notificationMessageType
            "temp_unit" -> {
                // Unit changed: the cached reading is stale, refetch right away
                viewModelScope.launch { updateWeatherOnce() }
            }
            "font_type" -> _fontType.value = prefManager.fontType
            "main_font_type" -> _mainFontType.value = prefManager.mainFontType
            "roboto_flex_settings" -> _robotoFlexSettings.value = prefManager.robotoFlexSettings
            "google_sans_flex_settings" -> _googleSansFlexSettings.value = prefManager.googleSansFlexSettings
            "app_menu_order" -> _appMenuOrder.value = prefManager.appMenuOrder
        }
    }

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val allApps: StateFlow<List<AppInfo>> = _allApps

    private val _hiddenApps = MutableStateFlow(prefManager.hiddenApps.toSet())
    val hiddenApps: StateFlow<Set<String>> = _hiddenApps

    private val _showHiddenAppsInSearch = MutableStateFlow(prefManager.showHiddenAppsInSearch)
    val showHiddenAppsInSearch: StateFlow<Boolean> = _showHiddenAppsInSearch

    private val _drawerIconShape = MutableStateFlow(IconShape.valueOf(prefManager.drawerIconShape))
    val drawerIconShape: StateFlow<IconShape> = _drawerIconShape

    private val _drawerIconShadow = MutableStateFlow(prefManager.drawerIconShadow)
    val drawerIconShadow: StateFlow<Boolean> = _drawerIconShadow

    private val _appLabelsEnabled = MutableStateFlow(prefManager.appLabelsEnabled)
    val appLabelsEnabled: StateFlow<Boolean> = _appLabelsEnabled

    private val _blurEnabled = MutableStateFlow(prefManager.blurEnabled)
    val blurEnabled: StateFlow<Boolean> = _blurEnabled

    private val _globalIconPack = MutableStateFlow(prefManager.globalIconPack)

    private val _pinnedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val pinnedApps: StateFlow<List<AppInfo>> = _pinnedApps

    private val _isGridLayout = MutableStateFlow(prefManager.isGridLayout)
    val isGridLayout: StateFlow<Boolean> = _isGridLayout

    private val _notificationBadgeType = MutableStateFlow(prefManager.notificationBadgeType)
    val notificationBadgeType: StateFlow<Int> = _notificationBadgeType

    private val _openKeyboard = MutableStateFlow(prefManager.openKeyboard)
    val openKeyboard: StateFlow<Boolean> = _openKeyboard

    private val _openKeyboardPortraitOnly = MutableStateFlow(prefManager.openKeyboardPortraitOnly)
    val openKeyboardPortraitOnly: StateFlow<Boolean> = _openKeyboardPortraitOnly

    private val _recentlyOpened = MutableStateFlow<List<AppInfo>>(emptyList())
    val recentlyOpened: StateFlow<List<AppInfo>> = _recentlyOpened

    private val _isLandscape = MutableStateFlow(false)
    val isLandscape: StateFlow<Boolean> = _isLandscape

    private val _widgetColumns = MutableStateFlow(prefManager.widgetColumnsPortrait)
    val widgetColumns: StateFlow<Int> = _widgetColumns

    private val _widgets = MutableStateFlow<List<WidgetItem>>(emptyList())
    val widgets: StateFlow<List<WidgetItem>> = _widgets

    private val _installedWidgets = MutableStateFlow<Map<AppWidgetGroup, List<WidgetPickerItemData>>>(emptyMap())
    val installedWidgets: StateFlow<Map<AppWidgetGroup, List<WidgetPickerItemData>>> = _installedWidgets

    private val _advancedSearchEnabled = MutableStateFlow(prefManager.advancedSearchEnabled)
    val advancedSearchEnabled: StateFlow<Boolean> = _advancedSearchEnabled

    private val _dockSafeDrawIme = MutableStateFlow(prefManager.dockSafeDrawIme)
    val dockSafeDrawIme: StateFlow<Boolean> = _dockSafeDrawIme

    private val _dockSafeDrawImePortraitOnly = MutableStateFlow(prefManager.dockSafeDrawImePortraitOnly)
    val dockSafeDrawImePortraitOnly: StateFlow<Boolean> = _dockSafeDrawImePortraitOnly

    private val _searchHistory = MutableStateFlow(loadSearchHistory())
    val searchHistory: StateFlow<List<SearchHistoryEntry>> = _searchHistory

    private val _configShortcutType = MutableStateFlow<ShortcutType?>(null)
    val configShortcutType: StateFlow<ShortcutType?> = _configShortcutType

    private val _theme = MutableStateFlow(prefManager.theme)
    val theme: StateFlow<Int> = _theme

    private val _fabSingleTapAction = MutableStateFlow(FabAction.fromString(prefManager.fabSingleTapAction))
    val fabSingleTapAction: StateFlow<FabAction> = _fabSingleTapAction

    private val _fabDoubleTapAction = MutableStateFlow(FabAction.fromString(prefManager.fabDoubleTapAction))
    val fabDoubleTapAction: StateFlow<FabAction> = _fabDoubleTapAction

    private val _fabLongPressAction = MutableStateFlow(FabAction.fromString(prefManager.fabLongPressAction))
    val fabLongPressAction: StateFlow<FabAction> = _fabLongPressAction

    private val _fabSingleTapValue = MutableStateFlow(prefManager.fabSingleTapValue)
    val fabSingleTapValue: StateFlow<String> = _fabSingleTapValue

    private val _fabDoubleTapValue = MutableStateFlow(prefManager.fabDoubleTapValue)
    val fabDoubleTapValue: StateFlow<String> = _fabDoubleTapValue

    private val _fabLongPressValue = MutableStateFlow(prefManager.fabLongPressValue)
    val fabLongPressValue: StateFlow<String> = _fabLongPressValue

    private val _fabSwipeUpAction = MutableStateFlow(FabAction.fromString(prefManager.fabSwipeUpAction))
    val fabSwipeUpAction: StateFlow<FabAction> = _fabSwipeUpAction

    private val _fabSwipeUpValue = MutableStateFlow(prefManager.fabSwipeUpValue)
    val fabSwipeUpValue: StateFlow<String> = _fabSwipeUpValue

    private val _isAppDrawerVisible = MutableStateFlow(false)
    val isAppDrawerVisible: StateFlow<Boolean> = _isAppDrawerVisible

    private val _showClockAtAGlance = MutableStateFlow(prefManager.showClockAtAGlance)
    val showClockAtAGlance: StateFlow<Boolean> = _showClockAtAGlance

    private val _hideAtAGlance = MutableStateFlow(prefManager.hideAtAGlance)
    val hideAtAGlance: StateFlow<Boolean> = _hideAtAGlance

    private val _hideDockScrolling = MutableStateFlow(prefManager.hideDockScrolling)
    val hideDockScrolling: StateFlow<Boolean> = _hideDockScrolling

    private val _hideDockScrollingOnlySmall = MutableStateFlow(prefManager.hideDockScrollingOnlySmall)
    val hideDockScrollingOnlySmall: StateFlow<Boolean> = _hideDockScrollingOnlySmall

    private val _hideDockWidgets = MutableStateFlow(prefManager.hideDockWidgets)
    val hideDockWidgets: StateFlow<Boolean> = _hideDockWidgets

    private val _hideDockWidgetsLandscapeOnly = MutableStateFlow(prefManager.hideDockWidgetsLandscapeOnly)
    val hideDockWidgetsLandscapeOnly: StateFlow<Boolean> = _hideDockWidgetsLandscapeOnly

    private val _hideDockMedia = MutableStateFlow(prefManager.hideDockMedia)
    val hideDockMedia: StateFlow<Boolean> = _hideDockMedia

    private val _hideDockMediaLandscapeOnly = MutableStateFlow(prefManager.hideDockMediaLandscapeOnly)
    val hideDockMediaLandscapeOnly: StateFlow<Boolean> = _hideDockMediaLandscapeOnly

    private val _hideActionButton = MutableStateFlow(prefManager.hideActionButton)
    val hideActionButton: StateFlow<Boolean> = _hideActionButton

    private val _moveWebSearch = MutableStateFlow(prefManager.moveWebSearch)
    val moveWebSearch: StateFlow<Boolean> = _moveWebSearch

    private val _showMuteNotifications = MutableStateFlow(prefManager.showMuteNotifications)
    val showMuteNotifications: StateFlow<Boolean> = _showMuteNotifications

    private val _showPermanentNotifications = MutableStateFlow(prefManager.showPermanentNotifications)
    val showPermanentNotifications: StateFlow<Boolean> = _showPermanentNotifications

    private val _disableGrouping = MutableStateFlow(prefManager.disableGrouping)
    val disableGrouping: StateFlow<Boolean> = _disableGrouping

    private val _notificationDeleteSinglePress = MutableStateFlow(prefManager.notificationDeleteSinglePress)
    val notificationDeleteSinglePress: StateFlow<Boolean> = _notificationDeleteSinglePress

    private val _notificationIndicatorType = MutableStateFlow(prefManager.notificationIndicatorType)
    val notificationIndicatorType: StateFlow<Int> = _notificationIndicatorType

    private val _notificationMessageType = MutableStateFlow(prefManager.notificationMessageType)
    val notificationMessageType: StateFlow<Int> = _notificationMessageType

    private val _fontType = MutableStateFlow(prefManager.fontType)
    val fontType: StateFlow<Int> = _fontType

    private val _mainFontType = MutableStateFlow(prefManager.mainFontType)
    val mainFontType: StateFlow<Int> = _mainFontType

    private val _robotoFlexSettings = MutableStateFlow(prefManager.robotoFlexSettings)
    val robotoFlexSettings: StateFlow<String> = _robotoFlexSettings

    private val _googleSansFlexSettings = MutableStateFlow(prefManager.googleSansFlexSettings)
    val googleSansFlexSettings: StateFlow<String> = _googleSansFlexSettings

    private val _appMenuOrder = MutableStateFlow(prefManager.appMenuOrder)
    val appMenuOrder: StateFlow<List<String>> = _appMenuOrder

    fun setAppDrawerVisible(visible: Boolean) {
        _isAppDrawerVisible.value = visible
    }

    private val _blackedOutModeEnabled = MutableStateFlow(prefManager.blackedOutModeEnabled)
    val blackedOutModeEnabled: StateFlow<Boolean> = _blackedOutModeEnabled

    private val _isBooting = MutableStateFlow(false)
    val isBooting: StateFlow<Boolean> = _isBooting

    /**
     * Only toggles the welcome overlay now. Loading already started in init (the ViewModel is
     * created by the first `viewModel` access, before setBooting(true) runs), and thanks to the
     * disk cache there's nothing heavy left to defer. finishInitialization() is idempotent, so
     * the second call that used to load everything twice is a no-op.
     */
    fun setBooting(booting: Boolean) {
        _isBooting.value = booting
        if (!booting) finishInitialization()
    }

    private val _coverThemeEnabled = MutableStateFlow(prefManager.coverThemeEnabled)
    val coverThemeEnabled: StateFlow<Boolean> = _coverThemeEnabled

    fun isCoverThemeApplied(size: IntSize): Boolean {
        return prefManager.isCoverThemeApplied(size)
    }

    enum class ShortcutType { TIME, DATE, WEATHER }

    fun setConfigShortcut(type: ShortcutType?) {
        _configShortcutType.value = type
    }

    fun handleShortcutClick(type: ShortcutType) {
        val shortcut = when (type) {
            ShortcutType.TIME -> prefManager.timeShortcut
            ShortcutType.DATE -> prefManager.dateShortcut
            ShortcutType.WEATHER -> prefManager.weatherShortcut
        }

        if (shortcut.isEmpty()) {
            _configShortcutType.value = type
        } else {
            executeShortcut(shortcut)
        }
    }

    private fun executeShortcut(shortcut: String) {
        val context = getApplication<Application>()
        if (shortcut.startsWith("link:")) {
            val url = shortcut.substring(5)
            try {
                val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        } else if (shortcut.startsWith("app:")) {
            val pkg = shortcut.substring(4)
            launchApp(pkg)
        }
    }

    fun saveShortcut(type: ShortcutType, value: String) {
        when (type) {
            ShortcutType.TIME -> prefManager.timeShortcut = value
            ShortcutType.DATE -> prefManager.dateShortcut = value
            ShortcutType.WEATHER -> prefManager.weatherShortcut = value
        }
        _configShortcutType.value = null
    }

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults

    private var searchJob: Job? = null

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pkg = intent?.data?.schemeSpecificPart
            val replacing = intent?.getBooleanExtra(Intent.EXTRA_REPLACING, false) == true

            // A real uninstall (not the REMOVED half of an update): drop it from every list
            // right now, in foreground and background alike. It's only a list filter.
            if (intent?.action == Intent.ACTION_PACKAGE_REMOVED && !replacing && pkg != null) {
                removePackageNow(pkg)
            }

            // Everything else (installs, updates, enabled/disabled components) needs a rescan.
            // In background nobody sees the drawer: just remember to rescan on return.
            if (_isForeground.value) loadApps(PACKAGE_EVENT_DEBOUNCE_MS) else appsDirty = true
        }
    }

    /** Registered only while in foreground (battery broadcasts fire very often). */
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level != -1 && scale != -1) {
                _batteryLevel.value = level.toFloat() / scale.toFloat()
            }
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            _isCharging.value = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
        }
    }

    val mediaControllerManager = MediaControllerManager(application)
    val mediaState: MediaState get() = mediaControllerManager.mediaState

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private var lastWeatherLocation: Location? = null

    val notificationCount = NotificationManager.notificationCount
    val notifications = NotificationManager.notifications

    fun dismissNotification(key: String) {
        NotificationManager.removeNotificationOptimistically(key)
        XenonNotificationService.dismissNotification(key)
    }

    fun dismissAllNotifications() {
        NotificationManager.removeAllNotificationsOptimistically()
        XenonNotificationService.dismissAllNotifications()
    }

    fun dismissNotificationsByPackage(packageName: String) {
        NotificationManager.removeNotificationsByPackageOptimistically(packageName)
        XenonNotificationService.dismissNotificationsByPackage(packageName)
    }

    fun dismissMutedNotifications() {
        NotificationManager.removeMutedOptimistically()
        XenonNotificationService.dismissMuted()
    }

    fun dismissPermanentNotifications() {
        XenonNotificationService.dismissPermanent()
    }

    private val _replyingNotificationKey = MutableStateFlow<String?>(null)
    val replyingNotificationKey: StateFlow<String?> = _replyingNotificationKey

    fun setReplyingNotification(key: String?) {
        _replyingNotificationKey.value = key
    }

    /**
     * Minute precision on purpose: StateFlow drops equal values, so the whole launcher UI
     * (which collects this at the top of setContent) recomposes once a minute instead of
     * every second. The UI only shows HH:mm anyway.
     */
    private val _currentTime = MutableStateFlow(nowToMinute())
    val currentTime: StateFlow<LocalDateTime> = _currentTime

    private val cachedWeather = cache.loadWeather()

    @Volatile
    private var lastWeatherFetch: Long = cachedWeather?.fetchedAt ?: 0L

    @Volatile
    private var lastWeatherKey: String? = cachedWeather?.unitKey

    private val _weatherState = MutableStateFlow(
        cachedWeather?.state ?: WeatherState(
            temperature = application.getString(R.string.no_weather_data),
            condition = ""
        )
    )
    val weatherState: StateFlow<WeatherState> = _weatherState

    private val _batteryLevel = MutableStateFlow(1f)
    val batteryLevel: StateFlow<Float> = _batteryLevel

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging

    private val _isFlashlightOn = MutableStateFlow(false)

    private val cameraManager by lazy { application.getSystemService(Context.CAMERA_SERVICE) as CameraManager }
    private var cameraId: String? = null

    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents

    private val _availableCalendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
    val availableCalendars: StateFlow<List<CalendarInfo>> = _availableCalendars

    private val _showCalendarSelectionDialog = MutableStateFlow(false)
    val showCalendarSelectionDialog: StateFlow<Boolean> = _showCalendarSelectionDialog

    private val _visibleCalendars = MutableStateFlow(prefManager.visibleCalendars)
    val visibleCalendars: StateFlow<List<String>> = _visibleCalendars

    private val _unsyncedSelectedCalendars = MutableStateFlow<List<CalendarInfo>>(emptyList())

    private val _showNotificationManagerDialog = MutableStateFlow(false)
    val showNotificationManagerDialog: StateFlow<Boolean> = _showNotificationManagerDialog

    private val _visibleNotificationApps = MutableStateFlow(prefManager.visibleNotificationApps)
    val visibleNotificationApps: StateFlow<List<String>> = _visibleNotificationApps

    private val _nextAlarm = MutableStateFlow<AlarmManager.AlarmClockInfo?>(null)
    val nextAlarm: StateFlow<AlarmManager.AlarmClockInfo?> = _nextAlarm

    private val _navigationEvents = MutableSharedFlow<Int>(replay = 1)
    val navigationEvents: SharedFlow<Int> = _navigationEvents

    fun onHomePressed() {
        viewModelScope.launch {
            _isAppDrawerVisible.value = false
            delay(100.milliseconds)
            _navigationEvents.emit(1)
        }
    }

    fun updateNextAlarm() {
        val am = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        _nextAlarm.value = am.nextAlarmClock
    }

    val activeTimers = NotificationManager.notifications.map { list ->
        list.filter { it.isTimer }
    }

    val activeStopwatches = NotificationManager.notifications.map { list ->
        list.filter { it.isStopwatch }
    }

    private val alarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateNextAlarm()
        }
    }

    val timeFormatter: DateTimeFormatter? = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter: DateTimeFormatter? = DateTimeFormatter.ofPattern("EEE, MMM d")

    private var calendarObserver: ContentObserver? = null

    /** Registered only while in foreground: time tick, time/zone/date changes, calendar provider. */
    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            _currentTime.value = nowToMinute()
            loadCalendarEvents()
        }
    }

    init {
        prefManager.registerListener(preferenceListener)

        NotificationManager.showMuteNotifications = prefManager.showMuteNotifications
        NotificationManager.showPermanentNotifications = prefManager.showPermanentNotifications
        NotificationManager.disableGrouping = prefManager.disableGrouping

        lastConfigKey = currentConfigKey()
        restoreCachedCalendarEvents()
        startTimeUpdates()

        finishInitialization()
    }

    private fun finishInitialization() {
        if (!initialized.compareAndSet(false, true)) return

        loadApps()          // paints the disk cache first, then rescans
        loadWidgets()
        loadInstalledWidgets()
        startMediaUpdates()
        startWeatherUpdates()
        loadAvailableCalendars()
        loadCalendarEvents()
        setupCalendarObserver()
        updateNextAlarm()

        val application = getApplication<Application>()
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull()
            val callback = object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(id: String, enabled: Boolean) {
                    if (id == cameraId) _isFlashlightOn.value = enabled
                }
            }
            cameraManager.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
            torchCallback = callback
        } catch (_: Exception) {}

        // Always-on receivers: cheap and rare
        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        application.registerReceiver(packageReceiver, packageFilter)
        application.registerReceiver(alarmReceiver, IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED))
        application.registerComponentCallbacks(configCallbacks)

        // If onStart already happened before init finished, catch up
        if (_isForeground.value) registerForegroundReceivers()
    }

    // ---------------------------------------------------------------------------------
    // Foreground / background
    // ---------------------------------------------------------------------------------

    /** Call from MainActivity.onStart (true) and onStop (false). */
    fun setForeground(foreground: Boolean) {
        if (_isForeground.value == foreground) return
        _isForeground.value = foreground

        if (foreground) {
            _currentTime.value = nowToMinute()
            onConfigMaybeChanged()
            registerForegroundReceivers()
            updateNextAlarm()
            loadAvailableCalendars()
            loadCalendarEvents()
            calendarDirty = false
            if (appsDirty) {
                appsDirty = false
                loadApps()
            }
            // The permission may have been granted in PermissionActivity or the system settings
            _audioPermissionGranted.value = hasAudioPermission()
            syncAudioAnalyzer()
        } else {
            unregisterForegroundReceivers()
            // No FFT capture while the launcher is invisible
            audioAnalyzer.stop()
            // Stop whatever is still queued; it would only update an invisible UI
            calendarJob?.cancel()
            searchJob?.cancel()
        }
    }

    private fun registerForegroundReceivers() {
        if (foregroundReceiversRegistered || !initialized.get()) return
        val application = getApplication<Application>()

        // Sticky broadcast: registering immediately delivers the current battery state
        application.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val timeFilter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        application.registerReceiver(timeTickReceiver, timeFilter)

        val providerFilter = IntentFilter(Intent.ACTION_PROVIDER_CHANGED).apply {
            addDataScheme("content")
            addDataAuthority("com.android.calendar", null)
        }
        ContextCompat.registerReceiver(
            application,
            timeTickReceiver,
            providerFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
        foregroundReceiversRegistered = true
    }

    private fun unregisterForegroundReceivers() {
        if (!foregroundReceiversRegistered) return
        val application = getApplication<Application>()
        try { application.unregisterReceiver(batteryReceiver) } catch (_: Exception) {}
        try { application.unregisterReceiver(timeTickReceiver) } catch (_: Exception) {}
        foregroundReceiversRegistered = false
    }

    override fun onCleared() {
        prefManager.unregisterListener(preferenceListener)
        val application = getApplication<Application>()
        unregisterForegroundReceivers()
        audioAnalyzer.stop()
        if (initialized.get()) {
            try { application.unregisterReceiver(packageReceiver) } catch (_: Exception) {}
            try { application.unregisterReceiver(alarmReceiver) } catch (_: Exception) {}
            try { application.unregisterComponentCallbacks(configCallbacks) } catch (_: Exception) {}
        }
        torchCallback?.let {
            try { cameraManager.unregisterTorchCallback(it) } catch (_: Exception) {}
        }
        calendarObserver?.let {
            try { application.contentResolver.unregisterContentObserver(it) } catch (_: Exception) {}
        }
    }

    // ---------------------------------------------------------------------------------
    // Weather
    // ---------------------------------------------------------------------------------

    private fun isMetric(): Boolean = when (prefManager.tempUnit) {
        1 -> true
        2 -> false
        else -> Locale.getDefault().country != "US"
    }

    /** A cached reading is only valid for the unit and language it was made with. */
    private fun currentWeatherKey(): String =
        "${if (isMetric()) "C" else "F"}|${Locale.getDefault().language}"

    private fun startWeatherUpdates() {
        weatherJob?.cancel()
        weatherJob = viewModelScope.launch {
            var failures = 0
            while (true) {
                // No network, no GPS while the launcher isn't visible
                _isForeground.first { it }

                val age = System.currentTimeMillis() - lastWeatherFetch
                val fresh = age in 0 until WEATHER_TTL_MS && lastWeatherKey == currentWeatherKey()
                if (fresh) {
                    delay((WEATHER_TTL_MS - age).milliseconds)
                    continue
                }

                val gotReading = updateWeatherOnce()
                if (gotReading) {
                    failures = 0
                } else {
                    failures++
                    delay(minOf(15, 1 shl (failures - 1).coerceAtMost(4)).minutes)
                }
            }
        }
    }

    private suspend fun updateWeatherOnce(): Boolean {
        val location = getDeviceLocation() ?: return false
        val lat = location.latitude
        val lon = location.longitude

        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val metric = isMetric()
                val tempParam = if (metric) "celsius" else "fahrenheit"
                val unit = if (metric) "C" else "F"

                val url = URL(
                    "https://api.open-meteo.com/v1/forecast" +
                            "?latitude=$lat&longitude=$lon" +
                            "&current=temperature_2m,weather_code" +
                            "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                            "&hourly=weather_code" +
                            "&temperature_unit=$tempParam" +
                            "&timezone=auto&forecast_days=1"
                )
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 20_000
                    setRequestProperty("Accept", "application/json")
                }

                val status = connection.responseCode
                if (status != HttpURLConnection.HTTP_OK) {
                    Log.w(TAG, "Weather: open-meteo answered HTTP $status")
                    return@withContext false
                }

                val text = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                if (text.isEmpty() || !text.startsWith("{")) {
                    Log.w(TAG, "Weather: open-meteo returned a non-JSON body")
                    return@withContext false
                }

                val json = JSONObject(text)
                val current = json.getJSONObject("current")
                val daily = json.getJSONObject("daily")

                val currentTempValue = current.getDouble("temperature_2m").roundToInt()
                val currentCode = current.getInt("weather_code")

                val maxTempValue = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt()
                val minTempValue = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt()
                val dailyCode = daily.getJSONArray("weather_code").getInt(0)

                // Midday code for the "today" summary when hourly is present, else the daily code
                val middayCode = json.optJSONObject("hourly")
                    ?.optJSONArray("weather_code")
                    ?.let { if (it.length() > 12) it.optInt(12, dailyCode) else dailyCode }
                    ?: dailyCode

                val state = WeatherState(
                    temperature = "$currentTempValue°$unit",
                    condition = weatherCodeToCondition(currentCode),
                    maxTemp = "$maxTempValue°$unit",
                    minTemp = "$minTempValue°$unit",
                    dailyCondition = weatherCodeToCondition(middayCode)
                )
                _weatherState.value = state

                val now = System.currentTimeMillis()
                val key = currentWeatherKey()
                lastWeatherFetch = now
                lastWeatherKey = key
                cache.saveWeather(state, now, key)
                true
            } catch (e: Exception) {
                Log.w(TAG, "Weather update failed: ${e.javaClass.simpleName}: ${e.message}")
                false
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun weatherCodeToCondition(code: Int): String {
        val english = when (code) {
            0 -> "Clear"
            1, 2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55, 56, 57 -> "Drizzle"
            61, 63, 80, 81, 82 -> "Rain"
            65, 66, 67 -> "Heavy rain"
            71, 73, 75, 77, 85, 86 -> "Snow"
            95, 96, 99 -> "Thunder"
            else -> "Clear"
        }
        return translateWeatherCondition(english)
    }

    private fun translateWeatherCondition(condition: String): String {
        if (Locale.getDefault().language != "de") return condition
        val c = condition.lowercase().trim()
        return when {
            c.contains("overcast") -> "Bedeckt"
            c.contains("partly cloudy") -> "Teilweise bewölkt"
            c.contains("cloudy") -> "Bewölkt"
            c.contains("sunny") -> "Sonnig"
            c.contains("clear") -> "Klar"
            c.contains("mist") -> "Dunst"
            c.contains("fog") -> "Nebel"
            c.contains("patchy rain") || c.contains("light rain") -> "Leichter Regen"
            c.contains("moderate rain") -> "Mäßiger Regen"
            c.contains("heavy rain") -> "Starker Regen"
            c.contains("thunder") -> "Gewitter"
            c.contains("snow") -> {
                if (c.contains("heavy")) "Starker Schneefall"
                else if (c.contains("moderate")) "Mäßiger Schneefall"
                else "Leichter Schneefall"
            }
            c.contains("sleet") -> "Schneeregen"
            c.contains("drizzle") -> "Nieselregen"
            c.contains("shower") -> {
                if (c.contains("rain")) "Regenschauer"
                else if (c.contains("snow")) "Schneeschauer"
                else "Schauer"
            }
            else -> condition
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getDeviceLocation(): Location? {
        val context = getApplication<Application>()
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return null // open-meteo needs coordinates: no permission, no weather

        val last = withTimeoutOrNull(3_000L.milliseconds) {
            suspendCancellableCoroutine { cont ->
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            }
        }
        if (last != null && System.currentTimeMillis() - last.time < LOCATION_MAX_AGE_MS) {
            lastWeatherLocation = last
            return last
        }

        val fresh = withTimeoutOrNull(LOCATION_FIX_TIMEOUT_MS.milliseconds) {
            suspendCancellableCoroutine { cont ->
                val cts = CancellationTokenSource()
                fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                cont.invokeOnCancellation { cts.cancel() }
            }
        }

        val result = fresh ?: last ?: lastWeatherLocation
        if (result != null) lastWeatherLocation = result
        return result
    }

    // ---------------------------------------------------------------------------------
    // Clock & media (foreground only)
    // ---------------------------------------------------------------------------------

    private fun nowToMinute(): LocalDateTime = LocalDateTime.now().withSecond(0).withNano(0)

    /** Wakes once per minute (aligned to the minute boundary), and never in background. */
    private fun startTimeUpdates() {
        viewModelScope.launch {
            while (true) {
                _isForeground.first { it }
                _currentTime.value = nowToMinute()
                val msToNextMinute = 60_000L - (System.currentTimeMillis() % 60_000L)
                delay((msToNextMinute + 20).milliseconds)
            }
        }
    }

    private fun startMediaUpdates() {
        viewModelScope.launch {
            while (true) {
                _isForeground.first { it }
                mediaControllerManager.updateActiveSession()
                // Picks up play/pause changes for the visualizer (≤ 1 s latency; the
                // visualizer shows its synthetic pulse until the real capture kicks in)
                syncAudioAnalyzer()
                delay(1000.milliseconds)
            }
        }
    }

    fun togglePlayPause() = mediaControllerManager.togglePlayPause()
    fun skipNext() = mediaControllerManager.skipNext()
    fun skipPrevious() = mediaControllerManager.skipPrevious()
    fun seekTo(position: Long) = mediaControllerManager.seekTo(position)

    val isMediaPermissionGranted: Boolean get() = mediaControllerManager.isPermissionGranted

    // ---------------------------------------------------------------------------------
    // Media visualizer (foreground + media page visible + playing only)
    // ---------------------------------------------------------------------------------

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            getApplication(), Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    /** Starts/stops the FFT capture. Idempotent, so it's safe to call from several places. */
    private fun syncAudioAnalyzer() {
        val shouldRun = _isForeground.value &&
                _isMediaPageVisible.value &&
                _audioPermissionGranted.value &&
                mediaState.isPlaying
        if (shouldRun) {
            audioAnalyzer.start()
        } else if (audioAnalyzer.isRunning) {
            audioAnalyzer.stop()
        }
    }

    /** Call from the pager whenever the media page becomes (in)visible. */
    fun setMediaPageVisible(visible: Boolean) {
        if (_isMediaPageVisible.value == visible) return
        _isMediaPageVisible.value = visible
        syncAudioAnalyzer()
    }

    /**
     * Call from the RECORD_AUDIO permission launcher's result callback. The system dialog
     * only pauses MainActivity (no onStop/onStart), so setForeground won't notice the grant.
     */
    fun onAudioPermissionResult() {
        _audioPermissionGranted.value = hasAudioPermission()
        syncAudioAnalyzer()
    }

    fun toggleFlashlight() {
        try {
            cameraId?.let {
                cameraManager.setTorchMode(it, !_isFlashlightOn.value)
            }
        } catch (_: Exception) {}
    }

    fun executeFabAction(action: FabAction, value: String) {
        val context = getApplication<Application>()
        when (action) {
            FabAction.LOCK_DEVICE -> XenonAccessibilityService.lockScreenOrRequestAccess(context)
            FabAction.TRIGGER_ASSISTANT -> {
                try {
                    context.startActivity(Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: Exception) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: Exception) {}
                }
            }
            FabAction.OPEN_APP -> launchApp(value)
            FabAction.OPEN_LINK -> {
                if (value.isNotEmpty()) {
                    try {
                        val uri = if (value.startsWith("http://") || value.startsWith("https://")) {
                            value.toUri()
                        } else {
                            "https://$value".toUri()
                        }
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (_: Exception) {}
                }
            }
            FabAction.TOGGLE_FLASHLIGHT -> toggleFlashlight()
            FabAction.OPEN_APP_DRAWER -> {
                _isAppDrawerVisible.value = !_isAppDrawerVisible.value
            }
            FabAction.OPEN_SHORTCUT -> {
                if (value.isNotEmpty()) {
                    try {
                        val intentUri = value.substringAfter("|")
                        val intent = Intent.parseUri(intentUri, 0).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
            FabAction.NONE -> {}
        }
    }

    fun openNotificationAccessSettings() {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    // ---------------------------------------------------------------------------------
    // Apps (cached, incremental)
    // ---------------------------------------------------------------------------------

    /**
     * 1. Cold start: paints the app list (with icons) from disk right away.
     * 2. Rescans the launcher activities, but only re-renders an icon when the app was
     *    updated or an icon-affecting setting changed; everything else reuses the exact same
     *    AppInfo object, so the StateFlows see equal lists and the UI doesn't recompose.
     * Repeated calls cancel the previous scan (package broadcasts come in bursts).
     */
    fun loadApps(debounceMs: Long = 0L) {
        appsJob?.cancel()
        appsJob = viewModelScope.launch(Dispatchers.IO) {
            if (debounceMs > 0) delay(debounceMs.milliseconds)
            val context = getApplication<Application>()

            // 1) Instant paint after process death
            if (appMemCache.isEmpty()) {
                val restored = cache.loadApps()
                restored.forEach { appMemCache[it.key] = it }
                if (restored.isNotEmpty() && _allApps.value.isEmpty()) {
                    publishApps(restored.map { it.info }.sortedBy { it.label.lowercase() })
                }
            }

            // 2) Rescan
            val pm = context.packageManager
            val launcherPackage = context.packageName
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val overrides = prefManager.getAppOverrides()
            val currentShape = _drawerIconShape.value
            val globalPack = _globalIconPack.value
            // Parsing an icon pack's appfilter is expensive: only do it if an icon must be rebuilt
            val globalPackMap by lazy { globalPack?.let { getIconPackMap(context, it) } ?: emptyMap() }

            // One binder call for every app's version instead of one per app
            @Suppress("DEPRECATION")
            val versions: Map<String, Long> = try {
                pm.getInstalledPackages(0).associate { it.packageName to it.lastUpdateTime }
            } catch (_: Exception) {
                emptyMap()
            }

            val settingsStamp = listOf(
                "v6", // bump to force every icon (and its color) to be rebuilt once
                currentShape.name,
                globalPack ?: "-",
                globalPack?.let { versions[it] } ?: 0L,
                currentConfigKey()
            ).joinToString("|")

            fun build(ri: ResolveInfo, pkgName: String, override: AppOverride?): AppInfo? = try {
                val originalLabel = ri.activityInfo.loadLabel(pm).toString()
                val originalIcon = ri.loadIcon(pm)

                var finalLabel = originalLabel
                val finalIcon: Drawable?
                var isCustomized = false

                if (override != null) {
                    isCustomized = true
                    val key = "$pkgName/${ri.activityInfo.name}"
                    val isSpecific = overrides.containsKey(key)
                    if (isSpecific || ri.activityInfo.name == pm.getLaunchIntentForPackage(pkgName)?.component?.className) {
                        override.customName?.let { finalLabel = it }
                    }

                    val baseIcon = if (override.iconPackPackage != null && override.iconResourceName != null) {
                        loadIconFromPack(context, override.iconPackPackage, override.iconResourceName) ?: originalIcon
                    } else {
                        originalIcon
                    }
                    finalIcon = generateCustomIcon(context, baseIcon, override, currentShape)
                } else {
                    val componentName = "ComponentInfo{${ri.activityInfo.packageName}/${ri.activityInfo.name}}"
                    val globalIconRes = if (globalPack != null) globalPackMap[componentName] else null

                    val baseIcon = if (globalPack != null && globalIconRes != null) {
                        loadIconFromPack(context, globalPack, globalIconRes) ?: originalIcon
                    } else {
                        originalIcon
                    }
                    finalIcon = normalizeIcon(context, baseIcon)
                }

                AppInfo(
                    name = originalLabel,
                    packageName = pkgName,
                    // Rendered once into a fixed-size bitmap: cacheable, and toBitmap() in
                    // the UI becomes free instead of redrawing on every recomposition
                    icon = finalIcon?.let { cache.rasterize(it) },
                    label = finalLabel,
                    isCustomized = isCustomized,
                    // From the original icon, BEFORE flattening (flattening loses the
                    // adaptive background layer and made X come out bright)
                    color = finalIcon?.let { computeAppColor(it) },
                    className = ri.activityInfo.name
                )
            } catch (_: Exception) {
                null
            }

            val seenKeys = HashSet<String>()
            val rebuilt = mutableListOf<LauncherCache.CachedApp>()

            val appList = pm.queryIntentActivities(intent, 0).mapNotNull { ri ->
                ensureActive()
                val pkgName = ri.activityInfo.packageName
                if (pkgName == launcherPackage) return@mapNotNull null

                val key = "$pkgName/${ri.activityInfo.name}"
                if (!seenKeys.add(key)) return@mapNotNull null

                val override = overrides[key] ?: overrides[pkgName]
                val stamp = "${versions[pkgName] ?: 0L}|$settingsStamp|${override?.toString()?.hashCode() ?: 0}"

                val cached = appMemCache[key]
                if (cached != null && cached.stamp == stamp) return@mapNotNull cached.info

                val info = build(ri, pkgName, override) ?: return@mapNotNull null
                val entry = LauncherCache.CachedApp(key, stamp, info)
                appMemCache[key] = entry
                rebuilt += entry
                info
            }.sortedBy { it.label.lowercase() }

            ensureActive()

            val removed = appMemCache.keys.filter { it !in seenKeys }
            removed.forEach { appMemCache.remove(it) }

            publishApps(appList)

            // Persist only what changed
            if (rebuilt.isNotEmpty() || removed.isNotEmpty()) {
                rebuilt.forEach { cache.saveIcon(it.key, it.info.icon) }
                removed.forEach { cache.deleteIcon(it) }
                cache.saveApps(appMemCache.values)
            }
        }
    }

    /**
     * The notification color, calculated once per app with the exact same
     * ColorUtils.getDominantColor() logic as before — but on the ORIGINAL icon, before it is
     * flattened. Running it on the flattened 64dp bitmap is what made X come out bright.
     */
    private fun computeAppColor(icon: Drawable): Int? {
        val color = ColorUtils.getDominantColor(icon)
        return if (color == Color.Unspecified) null else color.toArgb()
    }

    /**
     * System language (app labels come from the other apps' resources), the launcher's own
     * language (weather/calendar strings) and dark mode (themed icons).
     */
    private fun currentConfigKey(): String {
        val systemLocales = Resources.getSystem().configuration.locales.toLanguageTags()
        val appLocale = Locale.getDefault().toLanguageTag()
        val night = getApplication<Application>().resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        return "$systemLocales|$appLocale|$night"
    }

    private fun onConfigMaybeChanged() {
        val key = currentConfigKey()
        if (key == lastConfigKey) return
        val languageChanged = key.substringBeforeLast('|') != lastConfigKey.substringBeforeLast('|')
        lastConfigKey = key
        Log.d(TAG, "Configuration changed ($key); refreshing apps")

        if (!initialized.get()) return // the first scan picks up the new config anyway
        if (_isForeground.value) loadApps() else appsDirty = true

        if (languageChanged) {
            // Weather condition text is translated: restart the loop so it refetches
            // (it only actually runs once the launcher is visible)
            startWeatherUpdates()
            loadCalendarEvents() // no-op in background, onStart reloads then
        }
    }

    /** Drops an uninstalled package from every list immediately, without a rescan. */
    private fun removePackageNow(pkg: String) {
        val keys = appMemCache.keys.filter { it.startsWith("$pkg/") }
        keys.forEach { appMemCache.remove(it) }

        val current = _allApps.value
        if (current.any { it.packageName == pkg }) {
            publishApps(current.filter { it.packageName != pkg })
        }
        _searchResults.value = _searchResults.value.filterNot {
            it is SearchResult.App && it.appInfo.packageName == pkg
        }

        if (keys.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                keys.forEach { cache.deleteIcon(it) }
                cache.saveApps(appMemCache.values)
            }
        }
    }

    /** StateFlow skips equal values, so republishing an unchanged list costs nothing. */
    private fun publishApps(appList: List<AppInfo>) {
        _allApps.value = appList
        _apps.value = appList.filter { it.packageName !in _hiddenApps.value }
        sharedApps.value = _apps.value

        val savedPinnedPkgs = prefManager.pinnedApps
        _pinnedApps.value = savedPinnedPkgs.mapNotNull { key ->
            appList.find { app ->
                val appKey = if (app.className.isNotEmpty()) "${app.packageName}/${app.className}" else app.packageName
                appKey == key
            } ?: appList.find { it.packageName == key && it.className.isEmpty() }
            ?: appList.find { it.packageName == key } // Last resort fallback
        }
        loadRecentlyOpened()
    }

    fun updateAppOverride(key: String, override: AppOverride) {
        prefManager.saveAppOverride(key, override)
        loadApps()
    }

    fun resetAppOverride(key: String) {
        prefManager.resetAppOverride(key)
        loadApps()
    }

    fun getAppOverride(key: String): AppOverride? {
        return prefManager.getAppOverrides()[key]
    }

    fun getInstalledIconPacks(): List<ResolveInfo> {
        val pm = getApplication<Application>().packageManager
        val intent = Intent("com.novalauncher.THEME")
        val adwIntent = Intent("org.adw.launcher.THEMES")
        val goIntent = Intent("com.gau.go.launcherex.theme")

        val list = mutableListOf<ResolveInfo>()
        list.addAll(pm.queryIntentActivities(intent, PackageManager.GET_META_DATA))
        list.addAll(pm.queryIntentActivities(adwIntent, PackageManager.GET_META_DATA))
        list.addAll(pm.queryIntentActivities(goIntent, PackageManager.GET_META_DATA))

        return list.distinctBy { it.activityInfo.packageName }
    }

    fun hideApp(packageName: String) {
        val current = _hiddenApps.value.toMutableSet()
        current.add(packageName)
        _hiddenApps.value = current
        prefManager.hiddenApps = current.toList()
        publishApps(_allApps.value)
    }

    fun unhideApp(packageName: String) {
        val current = _hiddenApps.value.toMutableSet()
        current.remove(packageName)
        _hiddenApps.value = current
        prefManager.hiddenApps = current.toList()
        publishApps(_allApps.value)
    }

    private fun recordLaunch(key: String) {
        val now = System.currentTimeMillis()
        val usageStr = prefManager.appUsage
        val entries = usageStr.split(",").filter { it.isNotEmpty() }.toMutableList()
        entries.add("$key|$now")

        val oneDayAgo = now - DAY_MILLIS
        val filteredEntries = entries.filter {
            val parts = it.split("|")
            parts.size == 2 && (parts[1].toLongOrNull() ?: 0L) > oneDayAgo
        }

        prefManager.appUsage = filteredEntries.joinToString(",")
        loadRecentlyOpened()
    }

    private fun loadRecentlyOpened() {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - DAY_MILLIS
        val usageStr = prefManager.appUsage
        val recentApps = usageStr.split(",")
            .filter { it.isNotEmpty() }
            .mapNotNull {
                val parts = it.split("|")
                if (parts.size == 2) parts[0] to (parts[1].toLongOrNull() ?: 0L) else null
            }
            .filter { it.second > oneDayAgo }
            .groupBy { it.first }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }

        val byKey = _apps.value.associateBy { if (it.className.isNotEmpty()) "${it.packageName}/${it.className}" else it.packageName }
        _recentlyOpened.value = recentApps.mapNotNull { byKey[it] ?: _apps.value.find { app -> app.packageName == it } }
    }

    private fun savePinnedApps() {
        prefManager.pinnedApps = _pinnedApps.value.map { if (it.className.isNotEmpty()) "${it.packageName}/${it.className}" else it.packageName }
    }

    fun launchApp(packageName: String, className: String = "") {
        val pm = getApplication<Application>().packageManager
        val finalPkg = if (packageName.contains("/")) packageName.split("/")[0] else packageName
        val finalCls = if (packageName.contains("/")) packageName.split("/")[1] else className

        val launchIntent = if (finalCls.isNotEmpty()) {
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(finalPkg, finalCls)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            pm.getLaunchIntentForPackage(finalPkg)
        }

        if (launchIntent != null) {
            recordLaunch(if (finalCls.isNotEmpty()) "$finalPkg/$finalCls" else finalPkg)
            getApplication<Application>().startActivity(launchIntent)
        }
    }

    fun launchAppInSplitScreen(packageName: String) {
        val context = getApplication<Application>()
        recordLaunch(packageName)
        context.startActivity(SplitScreenPickerActivity.intent(context, firstPackage = packageName))
    }

    fun pinApp(packageName: String, atIndex: Int = -1) {
        val app = _apps.value.find {
            val key = if (it.className.isNotEmpty()) "${it.packageName}/${it.className}" else it.packageName
            key == packageName
        } ?: return
        val currentPinned = _pinnedApps.value.toMutableList()

        val alreadyPinned = currentPinned.any {
            val key = if (it.className.isNotEmpty()) "${it.packageName}/${it.className}" else it.packageName
            key == packageName
        }
        if (!alreadyPinned && currentPinned.size >= 6) return

        // Remove if already exists to avoid duplicates
        currentPinned.removeAll {
            val key = if (it.className.isNotEmpty()) "${it.packageName}/${it.className}" else it.packageName
            key == packageName
        }

        if (atIndex == -1 || atIndex >= currentPinned.size) {
            currentPinned.add(app)
        } else {
            currentPinned.add(atIndex.coerceAtLeast(0), app)
        }
        _pinnedApps.value = currentPinned
        savePinnedApps()
    }

    fun unpinApp(packageName: String) {
        _pinnedApps.value = _pinnedApps.value.filter {
            val key = if (it.className.isNotEmpty()) "${it.packageName}/${it.className}" else it.packageName
            key != packageName
        }
        savePinnedApps()
    }

    fun reorderPinnedApp(fromIndex: Int, toIndex: Int) {
        val list = _pinnedApps.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _pinnedApps.value = list
            savePinnedApps()
        }
    }

    fun setGridLayout(isGrid: Boolean) {
        _isGridLayout.value = isGrid
        prefManager.isGridLayout = isGrid
    }

    fun setOpenKeyboard(enabled: Boolean) {
        _openKeyboard.value = enabled
        prefManager.openKeyboard = enabled
    }

    fun setIsLandscape(landscape: Boolean) {
        if (_isLandscape.value != landscape) {
            _isLandscape.value = landscape
            _widgetColumns.value = if (landscape) prefManager.widgetColumnsLandscape else prefManager.widgetColumnsPortrait
            loadWidgets()
        }
    }

    fun setWidgetColumns(cols: Int) {
        _widgetColumns.value = cols
        if (_isLandscape.value) {
            prefManager.widgetColumnsLandscape = cols
        } else {
            prefManager.widgetColumnsPortrait = cols
        }
    }

    fun setAdvancedSearchEnabled(enabled: Boolean) {
        _advancedSearchEnabled.value = enabled
        prefManager.advancedSearchEnabled = enabled
    }

    // ---------------------------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------------------------

    private fun loadSearchHistory(): List<SearchHistoryEntry> {
        val jsonStr = prefManager.searchHistory
        if (jsonStr.isEmpty()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<SearchHistoryEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SearchHistoryEntry(
                        type = SearchHistoryType.valueOf(obj.getString("type")),
                        value = obj.getString("value"),
                        label = obj.getString("label"),
                        subLabel = if (obj.has("subLabel")) obj.getString("subLabel") else null,
                        iconUri = if (obj.has("iconUri")) obj.getString("iconUri") else null
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addToSearchHistory(result: SearchResult) {
        val entry = when (result) {
            is SearchResult.App -> {
                val appVal = if (result.appInfo.className.isNotEmpty()) "${result.appInfo.packageName}/${result.appInfo.className}" else result.appInfo.packageName
                SearchHistoryEntry(SearchHistoryType.APP, appVal, result.appInfo.label, result.appInfo.packageName)
            }
            is SearchResult.Contact -> SearchHistoryEntry(SearchHistoryType.CONTACT, result.id, result.name, result.phoneNumber, result.photoUri?.toString())
            is SearchResult.File -> SearchHistoryEntry(SearchHistoryType.FILE, result.uri.toString(), result.name, result.path, result.mimeType)
            is SearchResult.Web -> SearchHistoryEntry(SearchHistoryType.WEB, result.query, result.query)
        }

        val current = _searchHistory.value.toMutableList()
        current.removeAll { it.value == entry.value && it.type == entry.type }
        current.add(0, entry)
        val limited = current.take(20)
        _searchHistory.value = limited

        val arr = JSONArray()
        limited.forEach {
            val obj = JSONObject()
            obj.put("type", it.type.name)
            obj.put("value", it.value)
            obj.put("label", it.label)
            it.subLabel?.let { s -> obj.put("subLabel", s) }
            it.iconUri?.let { i -> obj.put("iconUri", i) }
            arr.put(obj)
        }
        prefManager.searchHistory = arr.toString()
    }

    fun performSearch(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch(Dispatchers.IO) {
            val results = mutableListOf<SearchResult>()

            // 1. Search Apps
            val appResults = _allApps.value
                .filter { it.packageName !in _hiddenApps.value || _showHiddenAppsInSearch.value }
                .filter { it.matches(query) }
                .map { SearchResult.App(it) }
            results.addAll(appResults)

            if (_advancedSearchEnabled.value) {
                // 2. Search Contacts
                results.addAll(searchContacts(query))
                ensureActive()

                // 3. Search Files
                results.addAll(searchFiles(query))
                ensureActive()

                // 4. Web Search and Website suggestions
                results.add(SearchResult.Web(query, false))
                results.add(SearchResult.Web(query, true))
            } else if (_moveWebSearch.value) {
                results.add(SearchResult.Web(query, false))
                results.add(SearchResult.Web(query, true))
            }

            _searchResults.value = results
        }
    }

    private fun searchContacts(query: String): List<SearchResult.Contact> {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val results = mutableListOf<SearchResult.Contact>()
        val uri = Phone.CONTENT_URI
        val projection = arrayOf(
            Phone.CONTACT_ID,
            Phone.DISPLAY_NAME,
            Phone.NUMBER,
            Phone.PHOTO_THUMBNAIL_URI
        )

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(Phone.DISPLAY_NAME)
            val numberIdx = cursor.getColumnIndex(Phone.NUMBER)
            val photoIdx = cursor.getColumnIndex(Phone.PHOTO_THUMBNAIL_URI)

            while (cursor.moveToNext() && results.size < 20) {
                val id = cursor.getString(idIdx)
                val name = cursor.getString(nameIdx) ?: ""
                val number = cursor.getString(numberIdx)
                val photoUriStr = cursor.getString(photoIdx)

                if (name.matchesSearch(query)) {
                    results.add(SearchResult.Contact(id, name, number, photoUriStr?.toUri()))
                }
            }
        }
        return results
    }

    private fun searchFiles(query: String): List<SearchResult.File> {
        val context = getApplication<Application>()
        val results = mutableListOf<SearchResult.File>()

        val externalUri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            FileColumns.DISPLAY_NAME,
            FileColumns.DATA,
            FileColumns.MIME_TYPE,
            FileColumns._ID
        )

        context.contentResolver.query(externalUri, projection, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(FileColumns.DISPLAY_NAME)
            val dataIdx = cursor.getColumnIndex(FileColumns.DATA)
            val mimeIdx = cursor.getColumnIndex(FileColumns.MIME_TYPE)
            val idIdx = cursor.getColumnIndex(FileColumns._ID)

            while (cursor.moveToNext() && results.size < 20) {
                val name = cursor.getString(nameIdx) ?: ""
                val path = cursor.getString(dataIdx)
                val mimeType = cursor.getString(mimeIdx) ?: "application/octet-stream"

                if (!name.matchesSearch(query)) continue

                // Filter out directories
                if (path != null && File(path).isDirectory) continue

                val id = cursor.getLong(idIdx)
                val uri = Uri.withAppendedPath(externalUri, id.toString())

                var preview: Bitmap? = null
                try {
                    preview = context.contentResolver.loadThumbnail(uri, Size(128, 128), null)
                } catch (_: Exception) {}

                results.add(SearchResult.File(name, path, uri, mimeType, preview))
            }
        }
        return results
    }

    // ---------------------------------------------------------------------------------
    // Widgets
    // ---------------------------------------------------------------------------------

    private fun loadInstalledWidgets() {
        viewModelScope.launch(Dispatchers.IO) {
            val manager = AppWidgetManager.getInstance(getApplication())
            val pm = getApplication<Application>().packageManager

            val providers = manager.installedProviders
            val shortcutIntent = Intent(Intent.ACTION_CREATE_SHORTCUT)
            val shortcuts = pm.queryIntentActivities(shortcutIntent, 0)

            val allPackages = (providers.map { it.provider.packageName } + shortcuts.map { it.activityInfo.packageName }).toSet()

            val grouped = allPackages.map { pkg ->
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (_: Exception) {
                    pkg
                }
                val icon = try {
                    pm.getApplicationIcon(pkg)
                } catch (_: Exception) {
                    null
                }

                val widgetItems = providers.filter { it.provider.packageName == pkg }.map {
                    val label = it.loadLabel(pm)
                    WidgetPickerItemData(
                        label = label,
                        isWidget = true,
                        widgetInfo = it,
                        id = "widget_${it.provider.flattenToString()}_$label"
                    )
                }
                val shortcutItems = shortcuts.filter { it.activityInfo.packageName == pkg }.map {
                    val label = it.loadLabel(pm).toString()
                    WidgetPickerItemData(
                        label = label,
                        isWidget = false,
                        shortcutInfo = it,
                        id = "shortcut_${it.activityInfo.packageName}_${it.activityInfo.name}_$label"
                    )
                }

                AppWidgetGroup(appName, icon) to (widgetItems + shortcutItems).sortedBy { it.label }
            }
                .filter { it.second.isNotEmpty() }
                .toMap()
                .toSortedMap()

            _installedWidgets.value = grouped
        }
    }

    private fun loadWidgets() {
        val layout = if (_isLandscape.value) prefManager.widgetLayoutLandscape else prefManager.widgetLayoutPortrait
        if (layout.isEmpty()) {
            _widgets.value = emptyList()
            return
        }
        var nextShortcutId = -100
        val items = layout.split(",").mapNotNull {
            val parts = it.split("|")
            if (parts.size >= 6) {
                var id = parts[0].toIntOrNull() ?: -1
                val page = parts[1].toIntOrNull() ?: 0
                val x = parts[2].toIntOrNull() ?: 0
                val y = parts[3].toIntOrNull() ?: 0
                val width = parts[4].toIntOrNull() ?: 1
                val height = parts[5].toIntOrNull() ?: 1
                val type = if (parts.size > 6) parts[6] else "widget"

                // Fix broken IDs (-1 or -2) for shortcuts
                if (type == "shortcut" && id >= -2) {
                    id = nextShortcutId--
                }

                WidgetItem(
                    id = id,
                    page = page,
                    x = x,
                    y = y,
                    width = width,
                    height = height,
                    type = type,
                    shortcutIntent = if (parts.size > 7) parts[7].replace("~", "|").replace("^", ",") else null,
                    shortcutLabel = if (parts.size > 8) parts[8].replace("~", "|").replace("^", ",") else null,
                    shortcutIconRes = if (parts.size > 9) parts[9] else null
                )
            } else if (parts.size == 5) {
                // Backward compatibility
                WidgetItem(
                    parts[0].toIntOrNull() ?: -1,
                    0,
                    parts[1].toIntOrNull() ?: 0,
                    parts[2].toIntOrNull() ?: 0,
                    parts[3].toIntOrNull() ?: 1,
                    parts[4].toIntOrNull() ?: 1
                )
            } else null
        }.filter { it.id != -1 || it.type == "shortcut" }
        _widgets.value = items

        // If we fixed any IDs, save them back immediately
        if (layout.contains("|-1|shortcut") || layout.contains("|-2|shortcut")) {
            saveWidgets()
        }
    }

    fun addWidget(id: Int, page: Int, x: Int, y: Int, w: Int, h: Int) {
        val current = _widgets.value.toMutableList()
        current.add(WidgetItem(id, page, x, y, w, h))
        _widgets.value = current
        saveWidgets()
    }

    fun addShortcut(page: Int, x: Int, y: Int, w: Int, h: Int, label: String, intent: String, iconRes: String?, iconBitmap: Bitmap? = null) {
        val current = _widgets.value.toMutableList()
        // Generate a unique ID that isn't -1 or -2
        val id = (current.minOfOrNull { it.id } ?: 0).coerceAtMost(0) - 100

        var finalIconRes = iconRes
        if (iconBitmap != null) {
            try {
                val context = getApplication<Application>()
                val fileName = "shortcut_icon_${System.currentTimeMillis()}.png"
                context.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                    iconBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                finalIconRes = "file:$fileName"
            } catch (_: Exception) {
            }
        }

        current.add(
            WidgetItem(
                id = id,
                page = page,
                x = x,
                y = y,
                width = w,
                height = h,
                type = "shortcut",
                shortcutLabel = label,
                shortcutIntent = intent,
                shortcutIconRes = finalIconRes
            )
        )
        _widgets.value = current
        saveWidgets()
    }

    fun removeWidget(id: Int) {
        val current = _widgets.value.filter { it.id != id }
        _widgets.value = current
        saveWidgets()
    }

    fun updateWidget(id: Int, page: Int, x: Int, y: Int, w: Int, h: Int) {
        val current = _widgets.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val old = current[index]
            current[index] = old.copy(page = page, x = x, y = y, width = w, height = h)
            _widgets.value = current
            saveWidgets()
        }
    }

    // ---------------------------------------------------------------------------------
    // Calendar
    // ---------------------------------------------------------------------------------

    private fun setupCalendarObserver() {
        if (calendarObserver == null) {
            val context = getApplication<Application>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            calendarObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    // Sync adapters write constantly in background; reload only when visible
                    if (_isForeground.value) loadCalendarEvents() else calendarDirty = true
                }
            }
            try {
                // CalendarContract.CONTENT_URI with notifyForDescendants covers Events,
                // Instances and Calendars — registering those again only multiplied callbacks.
                context.contentResolver.registerContentObserver(
                    CalendarContract.CONTENT_URI,
                    true,
                    calendarObserver!!
                )
            } catch (e: Exception) {
                Log.e(TAG, "Could not register calendar observer", e)
            }
        }
    }

    private fun restoreCachedCalendarEvents() {
        val cached = cache.loadCalendarEvents() ?: return
        val tz = TimeZone.getDefault()
        val bounds = computeDayBounds()
        _calendarEvents.value = cached
            .filter { it.isRelevant(bounds, tz) }
            .sortedWith(
                compareBy<CalendarEvent> { rankOf(it, bounds, tz) }
                    .thenBy { it.localStart(tz) }
                    .thenBy { it.localEnd(tz) }
            )
    }

    private fun publishCalendarEvents(events: List<CalendarEvent>) {
        if (events == _calendarEvents.value) return
        _calendarEvents.value = events
        cache.saveCalendarEvents(events)
    }

    private data class DayBounds(
        val now: Long,
        val startOfToday: Long,
        val endOfToday: Long,
        val endOfTomorrow: Long
    )

    private fun computeDayBounds(): DayBounds {
        val now = System.currentTimeMillis()

        val startOfToday = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfToday = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val endOfTomorrow = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        return DayBounds(now, startOfToday, endOfToday, endOfTomorrow)
    }

    /**
     * All-day events are stored by the provider with UTC midnight boundaries, so an
     * all-day event "yesterday" ends at 02:00 local in UTC+2 and looks like it belongs
     * to today. Shift all-day boundaries into local time before comparing anything.
     */
    private fun CalendarEvent.localStart(tz: TimeZone): Long =
        if (isAllDay) startTime - tz.getOffset(startTime) else startTime

    private fun CalendarEvent.localEnd(tz: TimeZone): Long =
        if (isAllDay) endTime - tz.getOffset(endTime) else endTime

    private fun CalendarEvent.isRelevant(bounds: DayBounds, tz: TimeZone): Boolean {
        val start = localStart(tz)
        val end = localEnd(tz)
        if (start > bounds.endOfTomorrow) return false

        return if (isAllDay) {
            end > bounds.startOfToday
        } else {
            end > bounds.now
        }
    }

    private fun rankOf(event: CalendarEvent, bounds: DayBounds, tz: TimeZone): Int {
        val start = event.localStart(tz)
        val end = event.localEnd(tz)
        return when {
            !event.isAllDay && start <= bounds.now && bounds.now < end -> 1 // running now
            !event.isAllDay && start > bounds.now && start <= bounds.endOfToday -> 2 // later today
            event.isAllDay && start <= bounds.endOfToday && end > bounds.startOfToday -> 3 // all-day today
            !event.isAllDay && end <= bounds.now && end > bounds.startOfToday -> 4 // finished earlier today
            !event.isAllDay && start > bounds.endOfToday && start <= bounds.endOfTomorrow -> 5 // tomorrow
            event.isAllDay -> 6 // all-day tomorrow
            else -> 7
        }
    }

    /**
     * Reads calendar rows into [CalendarEvent]s. A single unreadable row is skipped and
     * logged instead of aborting the whole query.
     */
    private fun readEvents(
        context: Context,
        uri: Uri,
        projection: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?,
        bounds: DayBounds,
        tz: TimeZone
    ): List<CalendarEvent> {
        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"
        val events = mutableListOf<CalendarEvent>()
        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val startIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                val locIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val calIdIdx = cursor.getColumnIndex(CalendarContract.Instances.CALENDAR_ID)
                val colorIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_COLOR)

                if (listOf(idIdx, titleIdx, startIdx, endIdx, allDayIdx, calIdIdx).any { it < 0 }) {
                    Log.e(TAG, "Calendar cursor is missing expected columns; aborting read")
                    return emptyList()
                }

                while (cursor.moveToNext()) {
                    try {
                        val rawTitle = cursor.getString(titleIdx)
                        val start = cursor.getLong(startIdx)
                        val end = cursor.getLong(endIdx)

                        // Skip events with no title and duration 0
                        if (rawTitle.isNullOrBlank() && start == end) continue

                        val event = CalendarEvent(
                            id = cursor.getLong(idIdx),
                            title = rawTitle ?: context.getString(R.string.no_title),
                            startTime = start,
                            endTime = end,
                            location = if (locIdx >= 0) cursor.getString(locIdx) else null,
                            isAllDay = cursor.getInt(allDayIdx) != 0,
                            calendarId = cursor.getString(calIdIdx) ?: "",
                            color = if (colorIdx >= 0 && !cursor.isNull(colorIdx)) cursor.getInt(colorIdx) else null
                        )

                        if (event.isRelevant(bounds, tz)) events.add(event)
                    } catch (e: Exception) {
                        Log.e(TAG, "Skipping unreadable calendar row", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying calendar instances", e)
        }
        return events
    }

    private fun readNonRecurringEvents(
        context: Context,
        selection: String?,
        selectionArgs: Array<String>?,
        bounds: DayBounds,
        tz: TimeZone
    ): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val baseSelection = "${CalendarContract.Events.DELETED} = 0 AND ${CalendarContract.Events.RRULE} IS NULL" +
                // Let the provider skip everything that ended before yesterday
                " AND ${CalendarContract.Events.DTSTART} <= ${bounds.endOfTomorrow + DAY_MILLIS}" +
                " AND (${CalendarContract.Events.DTEND} IS NULL OR ${CalendarContract.Events.DTEND} >= ${bounds.startOfToday - DAY_MILLIS})"
        val fullSelection = if (selection != null) {
            "$baseSelection AND $selection"
        } else {
            baseSelection
        }
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.EVENT_COLOR
        )
        try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                fullSelection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                val endIdx = cursor.getColumnIndex(CalendarContract.Events.DTEND)
                val locIdx = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Events.ALL_DAY)
                val calIdIdx = cursor.getColumnIndex(CalendarContract.Events.CALENDAR_ID)
                val colorIdx = cursor.getColumnIndex(CalendarContract.Events.EVENT_COLOR)

                if (listOf(idIdx, titleIdx, startIdx, allDayIdx, calIdIdx).any { it < 0 }) {
                    return emptyList()
                }

                while (cursor.moveToNext()) {
                    try {
                        val rawTitle = cursor.getString(titleIdx)
                        val startTime = cursor.getLong(startIdx)
                        val endTime = if (endIdx >= 0 && !cursor.isNull(endIdx)) cursor.getLong(endIdx) else startTime

                        // Skip events with no title and duration 0
                        if (rawTitle.isNullOrBlank() && startTime == endTime) continue

                        val event = CalendarEvent(
                            id = cursor.getLong(idIdx),
                            title = rawTitle ?: context.getString(R.string.no_title),
                            startTime = startTime,
                            endTime = endTime,
                            location = if (locIdx >= 0) cursor.getString(locIdx) else null,
                            isAllDay = cursor.getInt(allDayIdx) != 0,
                            calendarId = cursor.getString(calIdIdx) ?: "",
                            color = if (colorIdx >= 0 && !cursor.isNull(colorIdx)) cursor.getInt(colorIdx) else null
                        )
                        if (event.isRelevant(bounds, tz)) {
                            events.add(event)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Skipping unreadable event row", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying calendar events table", e)
        }
        return events
    }

    /**
     * Debounced: onStart, the observer, the provider broadcast and the minute tick often fire
     * together; they now collapse into a single query. Does nothing in background (the next
     * onStart reloads anyway).
     */
    fun loadCalendarEvents() {
        if (!_isForeground.value) {
            calendarDirty = true
            return
        }
        calendarJob?.cancel()
        calendarJob = viewModelScope.launch(Dispatchers.IO) {
            delay(CALENDAR_DEBOUNCE_MS.milliseconds)

            val context = getApplication<Application>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "READ_CALENDAR not granted; skipping calendar load")
                return@launch
            }

            setupCalendarObserver()

            val tz = TimeZone.getDefault()
            val bounds = computeDayBounds()
            val searchStart = bounds.startOfToday - DAY_MILLIS

            val availableCalendars = fetchAvailableCalendars(context)
            val availableIds = availableCalendars.map { it.id }.toSet()

            var visibleCalendars = prefManager.visibleCalendars

            if (visibleCalendars.contains("__NONE__")) {
                publishCalendarEvents(emptyList())
                return@launch
            }

            // Clean up stale IDs if present
            if (visibleCalendars.isNotEmpty()) {
                val validVisible = visibleCalendars.filter { availableIds.contains(it) }
                if (validVisible.size != visibleCalendars.size) {
                    Log.w(TAG, "Dropping stale calendar ids: ${visibleCalendars - validVisible.toSet()}")
                    visibleCalendars = validVisible
                    prefManager.visibleCalendars = validVisible
                    _visibleCalendars.value = validVisible
                }
            }

            // If visibleCalendars contains all available calendars, treat it as no filter (empty)
            if (visibleCalendars.isNotEmpty() && availableIds.isNotEmpty() &&
                visibleCalendars.toSet().containsAll(availableIds)
            ) {
                visibleCalendars = emptyList()
                prefManager.visibleCalendars = emptyList()
                _visibleCalendars.value = emptyList()
            }

            // Keep the picker's copy fresh so it can render the sync state.
            _availableCalendars.value = availableCalendars

            val effectiveSelection = if (visibleCalendars.isEmpty()) availableIds else visibleCalendars.toSet()
            val unsynced = availableCalendars.filter { it.id in effectiveSelection && (!it.syncEvents || !it.visible) }
            _unsyncedSelectedCalendars.value = unsynced

            // One attempt per calendar per process. Before, every load re-attempted and
            // scheduled 4 more loads, which re-attempted again: an ever-growing loop whenever
            // a calendar refused to switch on.
            val toEnable = unsynced.filter { syncAttemptedCalendars.add(it.id) }
            if (toEnable.isNotEmpty()) {
                Log.w(
                    TAG,
                    "Selected but NOT synced to device — attempting automatic sync enable once: " +
                            toEnable.joinToString { "${it.id}:'${it.name}' (${it.accountName})" }
                )
                enableSyncForCalendars(context, toEnable)
            }

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, searchStart)
            ContentUris.appendId(builder, bounds.endOfTomorrow + DAY_MILLIS)
            val uri = builder.build()

            val projection = arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_ID,
                CalendarContract.Instances.EVENT_COLOR
            )

            var selection: String? = null
            var selectionArgs: Array<String>? = null

            if (visibleCalendars.isNotEmpty()) {
                val placeholders = visibleCalendars.joinToString(",") { "?" }
                selection = "${CalendarContract.Instances.CALENDAR_ID} IN ($placeholders)"
                selectionArgs = visibleCalendars.toTypedArray()
            }

            var events = readEvents(context, uri, projection, selection, selectionArgs, bounds, tz)
            ensureActive()

            // Also check Events table directly for any non-recurring events that Instances might have missed
            val directEvents = readNonRecurringEvents(context, selection, selectionArgs, bounds, tz)
            val existingIds = events.map { it.id }.toSet()
            val supplemental = directEvents.filter { it.id !in existingIds }
            if (supplemental.isNotEmpty()) {
                events = events + supplemental
            }

            if (selection != null && events.isEmpty()) {
                val unfiltered = readEvents(context, uri, projection, null, null, bounds, tz)
                // Safety net: if the filter matched nothing at all, show everything.
                if (unfiltered.isNotEmpty()) {
                    Log.w(TAG, "Filtered query returned 0 events; falling back to unfiltered results")
                    events = unfiltered
                }
            }

            ensureActive()

            val sortedEvents = events
                .sortedWith(
                    compareBy<CalendarEvent> { rankOf(it, bounds, tz) }
                        .thenBy { it.localStart(tz) }
                        .thenBy { it.localEnd(tz) }
                )
                .take(25)

            publishCalendarEvents(sortedEvents)
        }
    }

    private fun fetchAvailableCalendars(context: Context): List<CalendarInfo> {
        val calendars = mutableListOf<CalendarInfo>()
        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.SYNC_EVENTS,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val colorIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
                val accountIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val syncIdx = cursor.getColumnIndex(CalendarContract.Calendars.SYNC_EVENTS)
                val visibleIdx = cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)
                val typeIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx)
                    val name = cursor.getString(nameIdx) ?: context.getString(R.string.unknown)
                    val color = cursor.getInt(colorIdx)
                    val accountName = cursor.getString(accountIdx) ?: ""
                    val syncEvents = syncIdx < 0 || cursor.getInt(syncIdx) != 0
                    val visible = visibleIdx < 0 || cursor.getInt(visibleIdx) != 0
                    val accountType = if (typeIdx >= 0) cursor.getString(typeIdx) ?: "" else ""

                    calendars.add(
                        CalendarInfo(id, name, color, accountName, syncEvents, visible, accountType)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching available calendars", e)
        }
        return calendars.sortedBy { it.name.lowercase() }
    }

    private fun enableSyncForCalendars(context: Context, calendars: List<CalendarInfo>) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val syncedAccounts = mutableSetOf<Pair<String, String>>()
        for (cal in calendars) {
            try {
                val accName = cal.accountName
                val accType = cal.accountType.ifEmpty { "com.google" }

                val uri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, cal.id.toLong())
                val syncAdapterUri = uri.buildUpon()
                    .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accName)
                    .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accType)
                    .build()

                val values = ContentValues().apply {
                    put(CalendarContract.Calendars.SYNC_EVENTS, 1)
                    put(CalendarContract.Calendars.VISIBLE, 1)
                }
                context.contentResolver.update(uri, values, null, null)
                context.contentResolver.update(syncAdapterUri, values, null, null)

                if (accName.isNotEmpty()) {
                    syncedAccounts.add(accName to accType)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not update sync flags for calendar ${cal.id}", e)
            }
        }
        for ((accountName, accountType) in syncedAccounts) {
            try {
                val account = Account(accountName, accountType)
                ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1)
                ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true)
                val bundle = Bundle().apply {
                    putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                    putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                }
                ContentResolver.requestSync(account, CalendarContract.AUTHORITY, bundle)
                Log.d(TAG, "Requested calendar sync for account: $accountName")
            } catch (e: Exception) {
                Log.e(TAG, "Could not request sync for $accountName", e)
            }
        }
        if (syncedAccounts.isNotEmpty()) {
            // A few follow-up reloads while the sync lands. They can't retrigger the enable
            // (syncAttemptedCalendars) and loadCalendarEvents() is a no-op in background.
            viewModelScope.launch {
                listOf(1500L, 3000L, 6000L, 10000L).forEach { delayMs ->
                    delay(delayMs.milliseconds)
                    if (!_isForeground.value) return@launch
                    loadCalendarEvents()
                }
            }
        }
    }

    fun loadAvailableCalendars() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                return@launch
            }
            _availableCalendars.value = fetchAvailableCalendars(context)
        }
    }

    fun setShowCalendarSelectionDialog(show: Boolean) {
        if (show) loadAvailableCalendars()
        _showCalendarSelectionDialog.value = show
    }

    fun setVisibleCalendars(calendars: List<String>) {
        prefManager.visibleCalendars = calendars
        _visibleCalendars.value = calendars
        loadCalendarEvents()
    }

    fun toggleCalendarVisibility(calendarId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                return@launch
            }

            var available = _availableCalendars.value
            if (available.isEmpty()) {
                available = fetchAvailableCalendars(context)
                _availableCalendars.value = available
            }

            val allAvailable = available.map { it.id }
            val current = _visibleCalendars.value.toMutableList()

            val new = if (current.isEmpty()) {
                allAvailable.toMutableList().apply { remove(calendarId) }
            } else if (current.contains("__NONE__")) {
                mutableListOf(calendarId)
            } else {
                if (current.contains(calendarId)) {
                    current.remove(calendarId)
                    if (current.isEmpty()) mutableListOf("__NONE__") else current
                } else {
                    current.add(calendarId)
                    if (current.size >= allAvailable.size) mutableListOf() else current
                }
            }
            withContext(Dispatchers.Main) {
                setVisibleCalendars(new)
            }
        }
    }

    // ---------------------------------------------------------------------------------
    // Notification app filter
    // ---------------------------------------------------------------------------------

    fun setShowNotificationManagerDialog(show: Boolean) {
        _showNotificationManagerDialog.value = show
    }

    fun setVisibleNotificationApps(apps: List<String>) {
        prefManager.visibleNotificationApps = apps
        _visibleNotificationApps.value = apps
    }

    fun toggleNotificationAppVisibility(packageName: String) {
        val current = _visibleNotificationApps.value.toMutableList()
        val allAvailable = _apps.value.map { it.packageName }

        val new = if (current.isEmpty()) {
            // "Select All" is active, so we unselect the one clicked
            allAvailable.toMutableList().apply { remove(packageName) }
        } else if (current.contains("__NONE__")) {
            // "Clear All" is active, so we select the one clicked
            mutableListOf(packageName)
        } else {
            // Specific selection active
            if (current.contains(packageName)) {
                current.remove(packageName)
                if (current.isEmpty()) mutableListOf("__NONE__") else current
            } else {
                current.add(packageName)
                if (current.size >= allAvailable.size) mutableListOf() else current
            }
        }
        setVisibleNotificationApps(new)
    }

    private fun saveWidgets() {
        val layout = _widgets.value.joinToString(",") {
            val base = "${it.id}|${it.page}|${it.x}|${it.y}|${it.width}|${it.height}"
            if (it.type == "shortcut") {
                // Escape separators in label/intent
                val escapedIntent = it.shortcutIntent?.replace(",", "^")?.replace("|", "~") ?: ""
                val escapedLabel = it.shortcutLabel?.replace(",", "^")?.replace("|", "~") ?: ""
                "$base|shortcut|$escapedIntent|$escapedLabel|${it.shortcutIconRes ?: ""}"
            } else {
                "$base|widget"
            }
        }
        if (_isLandscape.value) {
            prefManager.widgetLayoutLandscape = layout
        } else {
            prefManager.widgetLayoutPortrait = layout
        }
    }
}