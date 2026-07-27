package com.xenonware.launcher.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.BatteryManager
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Patterns
import android.util.Size
import androidx.core.content.ContextCompat
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
import com.xenonware.launcher.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import com.xenonware.launcher.util.matches
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.xenonware.launcher.util.matches
import com.xenonware.launcher.util.matchesSearch
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume

data class WeatherState(
    val temperature: String = "24°C",
    val condition: String = "Sunny"
)

data class CalendarEvent(
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val location: String?
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
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
        }
    }

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

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

    private val _widgets = MutableStateFlow<List<com.xenonware.launcher.model.WidgetItem>>(emptyList())
    val widgets: StateFlow<List<com.xenonware.launcher.model.WidgetItem>> = _widgets

    private val _installedWidgets = MutableStateFlow<Map<AppWidgetGroup, List<WidgetPickerItemData>>>(emptyMap())
    val installedWidgets: StateFlow<Map<AppWidgetGroup, List<WidgetPickerItemData>>> = _installedWidgets

    data class WidgetPickerItemData(
        val label: String,
        val isWidget: Boolean,
        val widgetInfo: android.appwidget.AppWidgetProviderInfo? = null,
        val shortcutInfo: android.content.pm.ResolveInfo? = null,
        val id: String = java.util.UUID.randomUUID().toString()
    )

    private val _advancedSearchEnabled = MutableStateFlow(prefManager.advancedSearchEnabled)
    val advancedSearchEnabled: StateFlow<Boolean> = _advancedSearchEnabled

    private val _dockSafeDrawIme = MutableStateFlow(prefManager.dockSafeDrawIme)
    val dockSafeDrawIme: StateFlow<Boolean> = _dockSafeDrawIme

    private val _searchHistory = MutableStateFlow<List<String>>(loadSearchHistory())
    val searchHistory: StateFlow<List<String>> = _searchHistory

    private val _configShortcutType = MutableStateFlow<ShortcutType?>(null)
    val configShortcutType: StateFlow<ShortcutType?> = _configShortcutType

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
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
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
        val icon: android.graphics.drawable.Drawable?
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

    val notificationCount = com.xenonware.launcher.notification.NotificationManager.notificationCount
    val notifications = com.xenonware.launcher.notification.NotificationManager.notifications

    fun dismissNotification(key: String) {
        com.xenonware.launcher.notification.NotificationManager.removeNotificationOptimistically(key)
        com.xenonware.launcher.notification.XenonNotificationService.dismissNotification(key)
    }

    fun dismissAllNotifications() {
        com.xenonware.launcher.notification.NotificationManager.removeAllNotificationsOptimistically()
        com.xenonware.launcher.notification.XenonNotificationService.dismissAllNotifications()
    }

    fun dismissNotificationsByPackage(packageName: String) {
        com.xenonware.launcher.notification.NotificationManager.removeNotificationsByPackageOptimistically(packageName)
        com.xenonware.launcher.notification.XenonNotificationService.dismissNotificationsByPackage(packageName)
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

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

    init {
        prefManager.registerListener(preferenceListener)
        loadApps()
        loadWidgets()
        loadInstalledWidgets()
        startMediaUpdates()
        startTimeUpdates()
        startWeatherUpdates()
        loadCalendarEvents()

        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        application.registerReceiver(packageReceiver, packageFilter)
        application.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onCleared() {
        super.onCleared()
        prefManager.unregisterListener(preferenceListener)
        getApplication<Application>().unregisterReceiver(packageReceiver)
        getApplication<Application>().unregisterReceiver(batteryReceiver)
    }

    private fun startWeatherUpdates() {
        viewModelScope.launch {
            while (true) {
                val gotReading = updateWeatherOnce()
                // If we couldn't get a fix/reading yet, retry soon; otherwise refresh every 15 min.
                delay(if (gotReading) 900_000L else 60_000L)
            }
        }
    }

    private suspend fun updateWeatherOnce(): Boolean {
        val location = getDeviceLocation()
        return withContext(Dispatchers.IO) {
            try {
                // With coordinates wttr.in reports your exact location, like Google Weather.
                // Without them it falls back to IP-based geolocation (less accurate).
                val locationPath = location?.let { "/${it.latitude},${it.longitude}" } ?: ""
                val url = java.net.URL("https://wttr.in$locationPath?format=%t;%C")
                val connection = url.openConnection() as java.net.HttpURLConnection
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
            } catch (e: Exception) {
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
                val second = LocalDateTime.now().second
                delay((60 - second) * 1000L + 500L)
            }
        }
    }

    private fun startMediaUpdates() {
        viewModelScope.launch {
            while (true) {
                mediaControllerManager.updateActiveSession()
                delay(1000)
            }
        }
    }

    fun togglePlayPause() = mediaControllerManager.togglePlayPause()
    fun skipNext() = mediaControllerManager.skipNext()
    fun skipPrevious() = mediaControllerManager.skipPrevious()
    fun seekTo(position: Long) = mediaControllerManager.seekTo(position)
    
    fun openMediaApp() {
        val pkg = mediaState.packageName ?: return
        val pm = getApplication<Application>().packageManager
        val intent = pm.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(intent)
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
            val pm = getApplication<Application>().packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedInfos = pm.queryIntentActivities(intent, 0)
            val appList = resolvedInfos.mapNotNull {
                try {
                    AppInfo(
                        name = it.loadLabel(pm).toString(),
                        packageName = it.activityInfo.packageName,
                        icon = it.loadIcon(pm)
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.name.lowercase() }
            _apps.value = appList

            // Restore pinned apps once the main list is loaded
            val savedPinnedPkgs = prefManager.pinnedApps
            _pinnedApps.value = savedPinnedPkgs.mapNotNull { pkg ->
                appList.find { it.packageName == pkg }
            }
            loadRecentlyOpened()
        }
    }

    private fun recordLaunch(packageName: String) {
        val now = System.currentTimeMillis()
        val usageStr = prefManager.appUsage
        val entries = usageStr.split(",").filter { it.isNotEmpty() }.toMutableList()
        entries.add("$packageName|$now")
        
        val oneDayAgo = now - 24 * 60 * 60 * 1000
        val filteredEntries = entries.filter { 
            val parts = it.split("|")
            parts.size == 2 && parts[1].toLongOrNull() ?: 0L > oneDayAgo
        }
        
        prefManager.appUsage = filteredEntries.joinToString(",")
        loadRecentlyOpened()
    }

    private fun loadRecentlyOpened() {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - 24 * 60 * 60 * 1000
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
            val appResults = _apps.value.filter { it.matches(query) }
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
                    results.add(SearchResult.Contact(id, name, number, photoUriStr?.let { Uri.parse(it) }))
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
                if (path != null && java.io.File(path).isDirectory) continue

                val id = cursor.getLong(idIdx)
                val uri = Uri.withAppendedPath(externalUri, id.toString())

                var preview: Bitmap? = null
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    try {
                        preview = context.contentResolver.loadThumbnail(uri, Size(128, 128), null)
                    } catch (e: Exception) {}
                }

                results.add(SearchResult.File(name, path, uri, mimeType, preview))
            }
        }
        return results
    }

    private fun loadInstalledWidgets() {
        viewModelScope.launch(Dispatchers.IO) {
            val manager = android.appwidget.AppWidgetManager.getInstance(getApplication())
            val pm = getApplication<Application>().packageManager
            
            val providers = manager.installedProviders
            val shortcutIntent = Intent(Intent.ACTION_CREATE_SHORTCUT)
            val shortcuts = pm.queryIntentActivities(shortcutIntent, 0)
            
            val allPackages = (providers.map { it.provider.packageName } + shortcuts.map { it.activityInfo.packageName }).toSet()
            
            val grouped = allPackages.map { pkg ->
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (e: Exception) {
                    pkg
                }
                val icon = try {
                    pm.getApplicationIcon(pkg)
                } catch (e: Exception) {
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

                com.xenonware.launcher.model.WidgetItem(
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
                com.xenonware.launcher.model.WidgetItem(
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
        current.add(com.xenonware.launcher.model.WidgetItem(id, page, x, y, w, h))
        _widgets.value = current
        saveWidgets()
    }

    fun addShortcut(page: Int, x: Int, y: Int, w: Int, h: Int, label: String, intent: String, iconRes: String?, iconBitmap: android.graphics.Bitmap? = null) {
        val current = _widgets.value.toMutableList()
        // Generate a unique ID that isn't -1 or -2
        val id = (current.map { it.id }.minOrNull() ?: 0).coerceAtMost(0) - 100
        
        var finalIconRes = iconRes
        if (iconBitmap != null) {
            try {
                val context = getApplication<Application>()
                val fileName = "shortcut_icon_${System.currentTimeMillis()}.png"
                context.openFileOutput(fileName, Context.MODE_PRIVATE).use { 
                    iconBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                finalIconRes = "file:$fileName"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        current.add(com.xenonware.launcher.model.WidgetItem(
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
        ))
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

    fun loadCalendarEvents() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                return@launch
            }

            val events = mutableListOf<CalendarEvent>()
            val uri = android.provider.CalendarContract.Events.CONTENT_URI
            val projection = arrayOf(
                android.provider.CalendarContract.Events.TITLE,
                android.provider.CalendarContract.Events.DTSTART,
                android.provider.CalendarContract.Events.DTEND,
                android.provider.CalendarContract.Events.EVENT_LOCATION
            )
            val now = System.currentTimeMillis()
            val tomorrow = now + 24 * 60 * 60 * 1000
            val selection = "${android.provider.CalendarContract.Events.DTSTART} >= ? AND ${android.provider.CalendarContract.Events.DTSTART} <= ?"
            val selectionArgs = arrayOf(now.toString(), tomorrow.toString())
            val sortOrder = "${android.provider.CalendarContract.Events.DTSTART} ASC"

            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val titleIdx = cursor.getColumnIndex(android.provider.CalendarContract.Events.TITLE)
                val startIdx = cursor.getColumnIndex(android.provider.CalendarContract.Events.DTSTART)
                val endIdx = cursor.getColumnIndex(android.provider.CalendarContract.Events.DTEND)
                val locIdx = cursor.getColumnIndex(android.provider.CalendarContract.Events.EVENT_LOCATION)

                while (cursor.moveToNext() && events.size < 5) {
                    val title = cursor.getString(titleIdx) ?: "No Title"
                    events.add(
                        CalendarEvent(
                            title = title,
                            startTime = cursor.getLong(startIdx),
                            endTime = cursor.getLong(endIdx),
                            location = cursor.getString(locIdx)
                        )
                    )
                }
            }
            _calendarEvents.value = events
        }
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
