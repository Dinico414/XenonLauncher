package com.xenonware.launcher.viewmodel

import android.Manifest
import android.accounts.Account
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.xenonware.launcher.data.SharedPreferenceManager
import com.xenonware.launcher.media.MediaControllerManager
import com.xenonware.launcher.media.MediaState
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.model.AppOverride
import com.xenonware.launcher.model.SearchResult
import com.xenonware.launcher.model.WidgetItem
import com.xenonware.launcher.notification.NotificationManager
import com.xenonware.launcher.notification.XenonNotificationService
import com.xenonware.launcher.ui.res.IconShape
import com.xenonware.launcher.util.generateCustomIcon
import com.xenonware.launcher.util.loadIconFromPack
import com.xenonware.launcher.util.matches
import com.xenonware.launcher.util.matchesSearch
import com.xenonware.launcher.util.normalizeIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

data class WeatherState(
    val temperature: String = "24°C",
    val condition: String = "Sunny"
)

data class CalendarInfo(
    val id: String,
    val name: String,
    val color: Int,
    val accountName: String,
    /** False when the provider holds no events for this calendar, no matter what we query. */
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
    val calendarId: String
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "LauncherViewModel"

        /**
         * Logs every relevant row plus, when a calendar filter is active, exactly which
         * events that filter is hiding. Flip to false once the calendar list looks right.
         */
        const val DEBUG_CALENDAR = true

        /**
         * Timed events that already ended earlier today still count as "today".
         * Set to false to go back to only showing running/upcoming events.
         */
        const val INCLUDE_PAST_EVENTS_TODAY = true

        const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }

    private val prefManager = SharedPreferenceManager(application)

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "is_grid_layout" -> _isGridLayout.value = prefManager.isGridLayout
            "notification_badge_type" -> _notificationBadgeType.value = prefManager.notificationBadgeType
            "open_keyboard" -> _openKeyboard.value = prefManager.openKeyboard
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
                loadApps() // Reload icons when shape changes
            }
            "drawer_icon_shadow" -> _drawerIconShadow.value = prefManager.drawerIconShadow
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

    private val _blurEnabled = MutableStateFlow(prefManager.blurEnabled)
    val blurEnabled: StateFlow<Boolean> = _blurEnabled

    private val _pinnedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val pinnedApps: StateFlow<List<AppInfo>> = _pinnedApps

    private val _isGridLayout = MutableStateFlow(prefManager.isGridLayout)
    val isGridLayout: StateFlow<Boolean> = _isGridLayout

    private val _notificationBadgeType = MutableStateFlow(prefManager.notificationBadgeType)
    val notificationBadgeType: StateFlow<Int> = _notificationBadgeType

    private val _openKeyboard = MutableStateFlow(prefManager.openKeyboard)
    val openKeyboard: StateFlow<Boolean> = _openKeyboard

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

    data class WidgetPickerItemData(
        val label: String,
        val isWidget: Boolean,
        val widgetInfo: AppWidgetProviderInfo? = null,
        val shortcutInfo: ResolveInfo? = null,
        val id: String = UUID.randomUUID().toString()
    )

    private val _advancedSearchEnabled = MutableStateFlow(prefManager.advancedSearchEnabled)
    val advancedSearchEnabled: StateFlow<Boolean> = _advancedSearchEnabled

    private val _dockSafeDrawIme = MutableStateFlow(prefManager.dockSafeDrawIme)
    val dockSafeDrawIme: StateFlow<Boolean> = _dockSafeDrawIme

    private val _dockSafeDrawImePortraitOnly = MutableStateFlow(prefManager.dockSafeDrawImePortraitOnly)
    val dockSafeDrawImePortraitOnly: StateFlow<Boolean> = _dockSafeDrawImePortraitOnly

    private val _searchHistory = MutableStateFlow(loadSearchHistory())
    val searchHistory: StateFlow<List<String>> = _searchHistory

    private val _configShortcutType = MutableStateFlow<ShortcutType?>(null)
    val configShortcutType: StateFlow<ShortcutType?> = _configShortcutType

    private val _theme = MutableStateFlow(prefManager.theme)
    val theme: StateFlow<Int> = _theme

    private val _isAppDrawerVisible = MutableStateFlow(false)
    val isAppDrawerVisible: StateFlow<Boolean> = _isAppDrawerVisible

    fun setAppDrawerVisible(visible: Boolean) {
        _isAppDrawerVisible.value = visible
    }

    private val _blackedOutModeEnabled = MutableStateFlow(prefManager.blackedOutModeEnabled)
    val blackedOutModeEnabled: StateFlow<Boolean> = _blackedOutModeEnabled

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

    data class AppWidgetGroup(
        val appName: String,
        val icon: Drawable?
    ) : Comparable<AppWidgetGroup> {
        override fun compareTo(other: AppWidgetGroup): Int = appName.compareTo(other.appName)
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadApps()
        }
    }

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

    // Key of the notification whose inline reply is open, or null. Lives here so the
    // dock can freeze its IME padding and LauncherScreen can close it when the app
    // drawer opens.
    private val _replyingNotificationKey = MutableStateFlow<String?>(null)
    val replyingNotificationKey: StateFlow<String?> = _replyingNotificationKey

    fun setReplyingNotification(key: String?) {
        _replyingNotificationKey.value = key
    }

    private val _currentTime = MutableStateFlow(LocalDateTime.now())
    val currentTime: StateFlow<LocalDateTime> = _currentTime

    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState

    private val _batteryLevel = MutableStateFlow(1f)
    val batteryLevel: StateFlow<Float> = _batteryLevel

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging

    private val _calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val calendarEvents: StateFlow<List<CalendarEvent>> = _calendarEvents

    private val _availableCalendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
    val availableCalendars: StateFlow<List<CalendarInfo>> = _availableCalendars

    private val _showCalendarSelectionDialog = MutableStateFlow(false)
    val showCalendarSelectionDialog: StateFlow<Boolean> = _showCalendarSelectionDialog

    private val _visibleCalendars = MutableStateFlow(prefManager.visibleCalendars)
    val visibleCalendars: StateFlow<List<String>> = _visibleCalendars

    /**
     * Calendars the user has ticked that the provider will never return events for,
     * because Google isn't syncing them to this device. Surface these in the calendar
     * picker with a "not synced" hint, otherwise the tick looks like it did something.
     */
    private val _unsyncedSelectedCalendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
    val unsyncedSelectedCalendars: StateFlow<List<CalendarInfo>> = _unsyncedSelectedCalendars

    /** Opens the system calendar sync settings so the user can enable the missing calendars. */
    fun openCalendarSyncSettings() {
        val context = getApplication<Application>()
        val calendarPackages = listOf("com.google.android.calendar", "com.android.calendar")
        for (pkg in calendarPackages) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
        }
        try {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_SYNC_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {}
    }

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
            delay(100.milliseconds) // Wait for drawer animation or system transition
            _navigationEvents.emit(1)
        }
    }

    fun updateNextAlarm() {
        val am = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        _nextAlarm.value = am.nextAlarmClock
    }

    val activeTimers = NotificationManager.notifications.map { list ->
        val timers = list.filter { it.isTimer }
        if (timers.isNotEmpty()) {
            Log.d(TAG, "Exposing ${timers.size} active timers to UI")
        }
        timers
    }

    val activeStopwatches = NotificationManager.notifications.map { list ->
        val stopwatches = list.filter { it.isStopwatch }
        if (stopwatches.isNotEmpty()) {
            Log.d(TAG, "Exposing ${stopwatches.size} active stopwatches to UI")
        }
        stopwatches
    }

    private val alarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateNextAlarm()
        }
    }

    val timeFormatter: DateTimeFormatter? = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter: DateTimeFormatter? = DateTimeFormatter.ofPattern("EEE, MMM d")

    private var calendarObserver: ContentObserver? = null

    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadCalendarEvents()
        }
    }

    init {
        prefManager.registerListener(preferenceListener)
        loadApps()
        loadWidgets()
        loadInstalledWidgets()
        startMediaUpdates()
        startTimeUpdates()
        startWeatherUpdates()
        loadAvailableCalendars()
        loadCalendarEvents()
        startCalendarUpdates()

        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        application.registerReceiver(packageReceiver, packageFilter)
        application.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val timeFilter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        application.registerReceiver(timeTickReceiver, timeFilter)

        application.registerReceiver(alarmReceiver, IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED))
        updateNextAlarm()

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
    }

    override fun onCleared() {
        super.onCleared()
        prefManager.unregisterListener(preferenceListener)
        getApplication<Application>().unregisterReceiver(packageReceiver)
        getApplication<Application>().unregisterReceiver(batteryReceiver)
        try {
            getApplication<Application>().unregisterReceiver(timeTickReceiver)
        } catch (_: Exception) {}
        try {
            getApplication<Application>().unregisterReceiver(alarmReceiver)
        } catch (_: Exception) {}
        calendarObserver?.let {
            try {
                getApplication<Application>().contentResolver.unregisterContentObserver(it)
            } catch (_: Exception) {}
        }
    }

    private fun startWeatherUpdates() {
        viewModelScope.launch {
            while (true) {
                val gotReading = updateWeatherOnce()
                // If we couldn't get a fix/reading yet, retry soon; otherwise refresh every 15 min.
                delay((if (gotReading) 900_000L else 60_000L).milliseconds)
            }
        }
    }

    private suspend fun updateWeatherOnce(): Boolean {
        val location = getDeviceLocation()
        return withContext(Dispatchers.IO) {
            try {
                // With coordinates wttr.in reports your exact location, like Google Weather.
                // Without them, it falls back to IP-based geolocation (less accurate).
                val locationPath = location?.let { "/${it.latitude},${it.longitude}" } ?: ""
                val url = URL("https://wttr.in$locationPath?format=%t;%C")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val text = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                if (text.isNotEmpty() && text.contains(";")) {
                    val parts = text.split(";")
                    if (parts.size >= 2) {
                        _weatherState.value = WeatherState(
                            temperature = parts[0].trim(),
                            condition = parts[1].trim()
                        )
                        return@withContext true
                    }
                }
                false
            } catch (_: Exception) {
                false
            }
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
        if (!hasFine && !hasCoarse) return null // no permission -> wttr.in uses IP fallback

        val priority = if (hasFine) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }

        return suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(priority, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        if (cont.isActive) cont.resume(location)
                    } else {
                        // Fresh fix unavailable (e.g. just booted) -> fall back to last known.
                        fusedLocationClient.lastLocation
                            .addOnSuccessListener { last -> if (cont.isActive) cont.resume(last) }
                            .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                    }
                }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
            cont.invokeOnCancellation { cts.cancel() }
        }
    }

    private fun startTimeUpdates() {
        viewModelScope.launch {
            while (true) {
                _currentTime.value = LocalDateTime.now()
                delay(1000L.milliseconds)
            }
        }
    }

    private fun startMediaUpdates() {
        viewModelScope.launch {
            while (true) {
                mediaControllerManager.updateActiveSession()
                delay(1000.milliseconds)
            }
        }
    }

    fun togglePlayPause() = mediaControllerManager.togglePlayPause()
    fun skipNext() = mediaControllerManager.skipNext()
    fun skipPrevious() = mediaControllerManager.skipPrevious()
    fun seekTo(position: Long) = mediaControllerManager.seekTo(position)
    fun sendCustomAction(action: String) = mediaControllerManager.sendCustomAction(action)

    fun openMediaApp() {
        val pkg = mediaState.packageName
        val context = getApplication<Application>()
        if (!pkg.isNullOrEmpty()) {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
        }

        try {
            val audioIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType("content://media/external/audio/media".toUri(), "audio/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooserIntent = Intent.createChooser(audioIntent, "SELECT AUDIO SOURCE").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
        } catch (_: Exception) {
        }
    }

    val isMediaPermissionGranted: Boolean get() = mediaControllerManager.isPermissionGranted

    fun openNotificationAccessSettings() {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val pm = context.packageManager
            val launcherPackage = context.packageName
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val overrides = prefManager.getAppOverrides()
            val currentShape = _drawerIconShape.value

            val resolvedInfos = pm.queryIntentActivities(intent, 0)
            val appList = resolvedInfos.mapNotNull { it ->
                val pkgName = it.activityInfo.packageName
                if (pkgName == launcherPackage) return@mapNotNull null

                try {
                    val originalLabel = it.loadLabel(pm).toString()
                    val originalIcon = it.loadIcon(pm)

                    val override = overrides[pkgName]
                    var finalLabel = originalLabel
                    var finalIcon: Drawable?
                    var isCustomized = false

                    if (override != null) {
                        isCustomized = true
                        override.customName?.let { finalLabel = it }

                        val baseIcon = if (override.iconPackPackage != null && override.iconResourceName != null) {
                            loadIconFromPack(context, override.iconPackPackage, override.iconResourceName) ?: originalIcon
                        } else {
                            originalIcon
                        }

                        finalIcon = generateCustomIcon(context, baseIcon, override, currentShape)
                    } else {
                        finalIcon = normalizeIcon(context, originalIcon)
                    }

                    AppInfo(
                        name = originalLabel,
                        packageName = pkgName,
                        icon = finalIcon,
                        label = finalLabel,
                        isCustomized = isCustomized
                    )
                } catch (_: Exception) {
                    null
                }
            }.sortedBy { it.label.lowercase() }
            _allApps.value = appList
            _apps.value = appList.filter { it.packageName !in _hiddenApps.value }

            // Restore pinned apps once the main list is loaded
            val savedPinnedPkgs = prefManager.pinnedApps
            _pinnedApps.value = savedPinnedPkgs.mapNotNull { pkg ->
                appList.find { it.packageName == pkg }
            }
            loadRecentlyOpened()
        }
    }

    fun updateAppOverride(packageName: String, override: AppOverride) {
        prefManager.saveAppOverride(packageName, override)
        loadApps()
    }

    fun resetAppOverride(packageName: String) {
        prefManager.resetAppOverride(packageName)
        loadApps()
    }

    fun getAppOverride(packageName: String): AppOverride? {
        return prefManager.getAppOverrides()[packageName]
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
        loadApps()
    }

    fun unhideApp(packageName: String) {
        val current = _hiddenApps.value.toMutableSet()
        current.remove(packageName)
        _hiddenApps.value = current
        prefManager.hiddenApps = current.toList()
        loadApps()
    }

    private fun recordLaunch(packageName: String) {
        val now = System.currentTimeMillis()
        val usageStr = prefManager.appUsage
        val entries = usageStr.split(",").filter { it.isNotEmpty() }.toMutableList()
        entries.add("$packageName|$now")

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

        _recentlyOpened.value = recentApps.mapNotNull { pkg ->
            _apps.value.find { it.packageName == pkg }
        }
    }

    private fun savePinnedApps() {
        prefManager.pinnedApps = _pinnedApps.value.map { it.packageName }
    }

    fun launchApp(packageName: String) {
        val pm = getApplication<Application>().packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            recordLaunch(packageName)
            getApplication<Application>().startActivity(launchIntent)
        }
    }

    fun pinApp(packageName: String, atIndex: Int = -1) {
        val app = _apps.value.find { it.packageName == packageName } ?: return
        val currentPinned = _pinnedApps.value.toMutableList()

        val alreadyPinned = currentPinned.any { it.packageName == packageName }
        if (!alreadyPinned && currentPinned.size >= 6) return

        // Remove if already exists to avoid duplicates
        currentPinned.removeAll { it.packageName == packageName }

        if (atIndex == -1 || atIndex >= currentPinned.size) {
            currentPinned.add(app)
        } else {
            currentPinned.add(atIndex.coerceAtLeast(0), app)
        }
        _pinnedApps.value = currentPinned
        savePinnedApps()
    }

    fun unpinApp(packageName: String) {
        _pinnedApps.value = _pinnedApps.value.filter { it.packageName != packageName }
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

    private fun loadSearchHistory(): List<String> {
        return prefManager.searchHistory.split(",").filter { it.isNotEmpty() }
    }

    fun addToSearchHistory(query: String) {
        val current = loadSearchHistory().toMutableList()
        current.remove(query)
        current.add(0, query)
        val limited = current.take(10)
        _searchHistory.value = limited
        prefManager.searchHistory = limited.joinToString(",")
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

                // 3. Search Files
                results.addAll(searchFiles(query))

                // 4. Web Search and Website suggestions
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
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )
        val selection = null
        val selectionArgs = null

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

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
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns._ID
        )
        val selection = null
        val selectionArgs = null

        context.contentResolver.query(externalUri, projection, selection, selectionArgs, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dataIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            val mimeIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
            val idIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)

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

    private fun setupCalendarObserver() {
        if (calendarObserver == null) {
            val context = getApplication<Application>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            calendarObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    Log.d(TAG, "Calendar content changed (uri=$uri); reloading events")
                    loadCalendarEvents()
                }
            }
            try {
                context.contentResolver.registerContentObserver(
                    CalendarContract.CONTENT_URI,
                    true,
                    calendarObserver!!
                )
                context.contentResolver.registerContentObserver(
                    CalendarContract.Events.CONTENT_URI,
                    true,
                    calendarObserver!!
                )
                context.contentResolver.registerContentObserver(
                    CalendarContract.Instances.CONTENT_URI,
                    true,
                    calendarObserver!!
                )
                context.contentResolver.registerContentObserver(
                    CalendarContract.Calendars.CONTENT_URI,
                    true,
                    calendarObserver!!
                )
            } catch (e: Exception) {
                Log.e(TAG, "Could not register calendar observers", e)
            }
        }
    }

    private fun startCalendarUpdates() {
        setupCalendarObserver()
        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                loadCalendarEvents()
            }
        }
    }

    // ---------------------------------------------------------------------------------
    // Calendar loading
    // ---------------------------------------------------------------------------------

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
        // Timed events used to be compared against `now`, which silently dropped every
        // event that had already finished today while all-day events survived.
        return if (isAllDay || INCLUDE_PAST_EVENTS_TODAY) {
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
     * logged instead of aborting the whole query — previously one bad row threw out of
     * the loop and, because the sort order puts all-day events first, left you with only
     * the all-day results.
     */
    private fun readEvents(
        context: Context,
        uri: Uri,
        projection: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String,
        bounds: DayBounds,
        tz: TimeZone
    ): List<CalendarEvent> {
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

                if (listOf(idIdx, titleIdx, startIdx, endIdx, allDayIdx, calIdIdx).any { it < 0 }) {
                    Log.e(TAG, "Calendar cursor is missing expected columns; aborting read")
                    return emptyList()
                }

                while (cursor.moveToNext()) {
                    try {
                        val event = CalendarEvent(
                            id = cursor.getLong(idIdx),
                            title = cursor.getString(titleIdx) ?: "No Title",
                            startTime = cursor.getLong(startIdx),
                            endTime = cursor.getLong(endIdx),
                            location = if (locIdx >= 0) cursor.getString(locIdx) else null,
                            isAllDay = cursor.getInt(allDayIdx) != 0,
                            calendarId = cursor.getString(calIdIdx) ?: ""
                        )

                        if (DEBUG_CALENDAR) {
                            Log.d(
                                TAG,
                                "raw row: '${event.title}' cal=${event.calendarId} " +
                                        "allDay=${event.isAllDay} start=${event.startTime} end=${event.endTime}"
                            )
                        }

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
        val baseSelection = "${CalendarContract.Events.DELETED} = 0 AND ${CalendarContract.Events.RRULE} IS NULL"
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
            CalendarContract.Events.CALENDAR_ID
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

                if (listOf(idIdx, titleIdx, startIdx, allDayIdx, calIdIdx).any { it < 0 }) {
                    return emptyList()
                }

                while (cursor.moveToNext()) {
                    try {
                        val startTime = cursor.getLong(startIdx)
                        val endTime = if (endIdx >= 0 && !cursor.isNull(endIdx)) cursor.getLong(endIdx) else startTime
                        val event = CalendarEvent(
                            id = cursor.getLong(idIdx),
                            title = cursor.getString(titleIdx) ?: "No Title",
                            startTime = startTime,
                            endTime = endTime,
                            location = if (locIdx >= 0) cursor.getString(locIdx) else null,
                            isAllDay = cursor.getInt(allDayIdx) != 0,
                            calendarId = cursor.getString(calIdIdx) ?: ""
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

    private fun logDatabaseTruth(context: Context, bounds: DayBounds) {
        val searchStart = bounds.endOfToday
        val searchEnd = bounds.endOfTomorrow + DAY_MILLIS
        Log.d(TAG, "--- DATABASE TRUTH: Checking all events between ${searchStart} and ${searchEnd} ---")
        
        try {
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.CALENDAR_ID,
                CalendarContract.Events.DELETED
            )
            // Query Events table directly for tomorrow/day after
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?",
                arrayOf(searchStart.toString(), searchEnd.toString()),
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { c ->
                Log.d(TAG, "Events found in raw database for this window: ${c.count}")
                while (c.moveToNext()) {
                    Log.d(TAG, "   DB_ROW: '${c.getString(1)}' cal=${c.getString(3)} start=${c.getLong(2)} deleted=${c.getInt(4)}")
                }
            }

            // Also check Instances table for same window
            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, searchStart)
            ContentUris.appendId(builder, searchEnd)
            context.contentResolver.query(
                builder.build(),
                arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.CALENDAR_ID),
                null, null, null
            )?.use { c ->
                Log.d(TAG, "Instances expanded by OS for this window: ${c.count}")
                while (c.moveToNext()) {
                    Log.d(TAG, "   OS_INSTANCE: '${c.getString(0)}' cal=${c.getString(2)} start=${c.getLong(1)}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging database truth", e)
        }
    }

    /**
     * Answers "are the events even on the device?" — independent of any filtering this
     * class does. Logs the sync/visible flags per calendar, every row in the Events
     * table from yesterday onward, and the raw Instances count per calendar with no
     * selection applied. An event present in Events but absent from Instances means the
     * provider's instance expansion is stale (clear Calendar Storage + resync). An event
     * absent from both is either unsynced or not a calendar event at all (Google Tasks,
     * Reminders and Birthdays live in a private provider CalendarContract cannot read).
     */
    private fun logCalendarDiagnostics(context: Context, instancesUri: Uri, bounds: DayBounds) {
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                    CalendarContract.Calendars.VISIBLE,
                    CalendarContract.Calendars.SYNC_EVENTS
                ), null, null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    Log.d(
                        TAG,
                        "cal ${c.getString(0)} '${c.getString(1)}' " +
                                "visible=${c.getInt(2)} syncEvents=${c.getInt(3)}"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not read Calendars sync flags", e)
        }

        try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(
                    CalendarContract.Events.CALENDAR_ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.ALL_DAY,
                    CalendarContract.Events.RRULE,
                    CalendarContract.Events.DELETED
                ),
                "${CalendarContract.Events.DTSTART} > ?",
                arrayOf((bounds.startOfToday - DAY_MILLIS).toString()),
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { c ->
                Log.d(TAG, "Events table rows from yesterday onward: ${c.count}")
                while (c.moveToNext()) {
                    Log.d(
                        TAG,
                        "  event cal=${c.getString(0)} '${c.getString(1)}' " +
                                "start=${c.getLong(2)} end=${c.getLong(3)} " +
                                "allDay=${c.getInt(4)} rrule=${c.getString(5)} deleted=${c.getInt(6)}"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not read Events table", e)
        }

        try {
            Log.d(TAG, "running as user=${android.os.Process.myUserHandle()}")
            val counts = sortedMapOf<String, Int>()
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(CalendarContract.Events.CALENDAR_ID),
                "${CalendarContract.Events.DELETED} = 0", null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: "?"
                    counts[id] = (counts[id] ?: 0) + 1
                }
            }
            Log.d(TAG, "TOTAL Events rows by calendarId (no date filter): $counts")
        } catch (e: Exception) {
            Log.e(TAG, "Could not count all Events", e)
        }

        // Everything stored for the calendars that are supposedly missing, ignoring dates.
        for (calId in listOf("8", "13", "14")) {
            try {
                context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    arrayOf(
                        CalendarContract.Events.TITLE,
                        CalendarContract.Events.DTSTART,
                        CalendarContract.Events.RRULE,
                        CalendarContract.Events.ALL_DAY,
                        CalendarContract.Events.DELETED
                    ),
                    "${CalendarContract.Events.CALENDAR_ID} = ?",
                    arrayOf(calId),
                    "${CalendarContract.Events.DTSTART} DESC LIMIT 25"
                )?.use { c ->
                    Log.d(TAG, "cal $calId Events rows (all dates): ${c.count}")
                    while (c.moveToNext()) {
                        Log.d(
                            TAG,
                            "   cal$calId '${c.getString(0)}' start=${c.getLong(1)} " +
                                    "rrule=${c.getString(2)} allDay=${c.getInt(3)} deleted=${c.getInt(4)}"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not read Events for calendar $calId", e)
            }
        }

        // Instances across a much wider window, in case the day boundaries are the problem.
        try {
            val wide = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(wide, bounds.startOfToday - 7 * DAY_MILLIS)
            ContentUris.appendId(wide, bounds.startOfToday + 7 * DAY_MILLIS)
            val counts = sortedMapOf<String, Int>()
            context.contentResolver.query(
                wide.build(),
                arrayOf(CalendarContract.Instances.CALENDAR_ID),
                null, null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: "?"
                    counts[id] = (counts[id] ?: 0) + 1
                }
            }
            Log.d(TAG, "Instances +/-7 days by calendarId: $counts")
        } catch (e: Exception) {
            Log.e(TAG, "Could not count wide Instances", e)
        }

        try {
            val counts = sortedMapOf<String, Int>()
            context.contentResolver.query(
                instancesUri,
                arrayOf(CalendarContract.Instances.CALENDAR_ID),
                null, null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: "?"
                    counts[id] = (counts[id] ?: 0) + 1
                }
            }
            Log.d(TAG, "Instances in window by calendarId (no selection): $counts")
        } catch (e: Exception) {
            Log.e(TAG, "Could not count Instances", e)
        }
    }

    fun loadCalendarEvents() {
        viewModelScope.launch(Dispatchers.IO) {
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
                _calendarEvents.value = emptyList()
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

            if (DEBUG_CALENDAR) {
                Log.d(TAG, "calendar filter=$visibleCalendars available=$availableIds tz=${tz.id}")
            }

            // Keep the picker's copy fresh so it can render the sync state.
            _availableCalendars.value = availableCalendars

            val effectiveSelection = if (visibleCalendars.isEmpty()) availableIds else visibleCalendars.toSet()
            val unsynced = availableCalendars.filter { it.id in effectiveSelection && (!it.syncEvents || !it.visible) }
            _unsyncedSelectedCalendars.value = unsynced
            if (unsynced.isNotEmpty()) {
                Log.w(
                    TAG,
                    "Selected but NOT synced to device — attempting automatic sync enable: " +
                            unsynced.joinToString { "${it.id}:'${it.name}' (${it.accountName})" }
                )
                enableSyncForCalendars(context, unsynced)
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
                CalendarContract.Instances.CALENDAR_ID
            )

            var selection: String? = null
            var selectionArgs: Array<String>? = null

            if (visibleCalendars.isNotEmpty()) {
                val placeholders = visibleCalendars.joinToString(",") { "?" }
                selection = "${CalendarContract.Instances.CALENDAR_ID} IN ($placeholders)"
                selectionArgs = visibleCalendars.toTypedArray()
            }

            val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

            if (DEBUG_CALENDAR) {
                logCalendarDiagnostics(context, uri, bounds)
                logDatabaseTruth(context, bounds)
            }

            var events = readEvents(context, uri, projection, selection, selectionArgs, sortOrder, bounds, tz)

            // Also check Events table directly for any non-recurring events that Instances might have missed
            val directEvents = readNonRecurringEvents(context, selection, selectionArgs, bounds, tz)
            val existingIds = events.map { it.id }.toSet()
            val supplemental = directEvents.filter { it.id !in existingIds }
            if (supplemental.isNotEmpty()) {
                if (DEBUG_CALENDAR) {
                    Log.d(TAG, "Added ${supplemental.size} supplemental events directly from Events table: ${supplemental.map { it.title }}")
                }
                events = events + supplemental
            }

            // Diagnostic: show exactly what the calendar filter is hiding. The old code only
            // ran an unfiltered fallback when the filtered result was completely empty, so a
            // filter that hid only the timed events (leaving the all-day ones) went unnoticed.
            if (selection != null && (DEBUG_CALENDAR || events.isEmpty())) {
                val unfiltered = readEvents(context, uri, projection, null, null, sortOrder, bounds, tz)
                val hidden = unfiltered.filter { u -> events.none { it.id == u.id && it.startTime == u.startTime } }

                if (hidden.isNotEmpty()) {
                    Log.w(TAG, "Filter $visibleCalendars is hiding ${hidden.size} otherwise-relevant events:")
                    hidden.forEach {
                        Log.w(TAG, "   hidden: '${it.title}' calendarId=${it.calendarId} allDay=${it.isAllDay}")
                    }
                }

                // Keep the original safety net: if the filter matched nothing at all, show everything.
                if (events.isEmpty() && unfiltered.isNotEmpty()) {
                    Log.w(TAG, "Filtered query returned 0 events; falling back to unfiltered results")
                    events = unfiltered
                }
            }

            val sortedEvents = events
                .sortedWith(
                    compareBy<CalendarEvent> { rankOf(it, bounds, tz) }
                        .thenBy { it.localStart(tz) }
                        .thenBy { it.localEnd(tz) }
                )
                .take(25)

            Log.d(TAG, "Loaded ${sortedEvents.size} calendar events (relevant raw: ${events.size})")
            _calendarEvents.value = sortedEvents
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
                    val name = cursor.getString(nameIdx) ?: "Unknown"
                    val color = cursor.getInt(colorIdx)
                    val accountName = cursor.getString(accountIdx) ?: ""
                    val syncEvents = syncIdx < 0 || cursor.getInt(syncIdx) != 0
                    val visible = visibleIdx < 0 || cursor.getInt(visibleIdx) != 0
                    val accountType = if (typeIdx >= 0) cursor.getString(typeIdx) ?: "" else ""
                    if (DEBUG_CALENDAR) {
                        Log.d(TAG, "Found calendar: id=$id, name=$name, account=$accountName, sync=$syncEvents")
                    }
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
                    putBoolean(ContentResolver.SYNC_EXTRAS_FORCE, true)
                }
                ContentResolver.requestSync(account, CalendarContract.AUTHORITY, bundle)
                Log.d(TAG, "Force requested calendar sync for account: $accountName")
            } catch (e: Exception) {
                Log.e(TAG, "Could not request sync for $accountName", e)
            }
        }
        if (syncedAccounts.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                listOf(1500L, 3000L, 6000L, 10000L).forEach { delayMs ->
                    delay(delayMs)
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