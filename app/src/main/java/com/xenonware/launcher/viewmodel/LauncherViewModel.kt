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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
            "widget_columns_normal", "widget_columns_wide" -> {
                _widgetColumns.value = if (_isWide.value) prefManager.widgetColumnsWide else prefManager.widgetColumnsNormal
            }
            "widget_layout_normal", "widget_layout_wide" -> loadWidgets()
            "advanced_search_enabled" -> _advancedSearchEnabled.value = prefManager.advancedSearchEnabled
            "search_history" -> _searchHistory.value = loadSearchHistory()
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

    private val _isWide = MutableStateFlow(false)
    val isWide: StateFlow<Boolean> = _isWide

    private val _widgetColumns = MutableStateFlow(prefManager.widgetColumnsNormal)
    val widgetColumns: StateFlow<Int> = _widgetColumns

    private val _widgets = MutableStateFlow<List<com.xenonware.launcher.model.WidgetItem>>(emptyList())
    val widgets: StateFlow<List<com.xenonware.launcher.model.WidgetItem>> = _widgets

    private val _installedWidgets = MutableStateFlow<Map<AppWidgetGroup, List<android.appwidget.AppWidgetProviderInfo>>>(emptyMap())
    val installedWidgets: StateFlow<Map<AppWidgetGroup, List<android.appwidget.AppWidgetProviderInfo>>> = _installedWidgets

    private val _advancedSearchEnabled = MutableStateFlow(prefManager.advancedSearchEnabled)
    val advancedSearchEnabled: StateFlow<Boolean> = _advancedSearchEnabled

    private val _searchHistory = MutableStateFlow<List<String>>(loadSearchHistory())
    val searchHistory: StateFlow<List<String>> = _searchHistory

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
        com.xenonware.launcher.notification.XenonNotificationService.dismissNotification(key)
    }

    fun dismissAllNotifications() {
        com.xenonware.launcher.notification.XenonNotificationService.dismissAllNotifications()
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
            val appList = resolvedInfos.map {
                AppInfo(
                    name = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(pm)
                )
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

    fun setIsWide(wide: Boolean) {
        if (_isWide.value != wide) {
            _isWide.value = wide
            _widgetColumns.value = if (wide) prefManager.widgetColumnsWide else prefManager.widgetColumnsNormal
            loadWidgets()
        }
    }

    fun setWidgetColumns(cols: Int) {
        _widgetColumns.value = cols
        if (_isWide.value) {
            prefManager.widgetColumnsWide = cols
        } else {
            prefManager.widgetColumnsNormal = cols
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
            val appResults = _apps.value.filter { it.name.contains(query, ignoreCase = true) }
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
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

            while (cursor.moveToNext() && results.size < 20) {
                val id = cursor.getString(idIdx)
                val name = cursor.getString(nameIdx)
                val number = cursor.getString(numberIdx)
                val photoUriStr = cursor.getString(photoIdx)
                results.add(SearchResult.Contact(id, name, number, photoUriStr?.let { Uri.parse(it) }))
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
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        context.contentResolver.query(externalUri, projection, selection, selectionArgs, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dataIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            val mimeIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
            val idIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)

            while (cursor.moveToNext() && results.size < 20) {
                val name = cursor.getString(nameIdx)
                val path = cursor.getString(dataIdx)
                val mimeType = cursor.getString(mimeIdx) ?: "application/octet-stream"

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
            
            val grouped = providers.groupBy { it.provider.packageName }
                .map { (pkg, widgets) ->
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
                    AppWidgetGroup(appName, icon) to widgets
                }
                .toMap()
                .toSortedMap()
            
            _installedWidgets.value = grouped
        }
    }

    private fun loadWidgets() {
        val layout = if (_isWide.value) prefManager.widgetLayoutWide else prefManager.widgetLayoutNormal
        if (layout.isEmpty()) {
            _widgets.value = emptyList()
            return
        }
        val items = layout.split(",").mapNotNull {
            val parts = it.split("|")
            if (parts.size == 6) {
                com.xenonware.launcher.model.WidgetItem(
                    parts[0].toIntOrNull() ?: -1,
                    parts[1].toIntOrNull() ?: 0,
                    parts[2].toIntOrNull() ?: 0,
                    parts[3].toIntOrNull() ?: 0,
                    parts[4].toIntOrNull() ?: 1,
                    parts[5].toIntOrNull() ?: 1
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
        }.filter { it.id != -1 }
        _widgets.value = items
    }

    fun addWidget(id: Int, page: Int, x: Int, y: Int, w: Int, h: Int) {
        val current = _widgets.value.toMutableList()
        current.add(com.xenonware.launcher.model.WidgetItem(id, page, x, y, w, h))
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
            current[index] = com.xenonware.launcher.model.WidgetItem(id, page, x, y, w, h)
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
                    events.add(
                        CalendarEvent(
                            title = cursor.getString(titleIdx),
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
        val layout = _widgets.value.joinToString(",") { "${it.id}|${it.page}|${it.x}|${it.y}|${it.width}|${it.height}" }
        if (_isWide.value) {
            prefManager.widgetLayoutWide = layout
        } else {
            prefManager.widgetLayoutNormal = layout
        }
    }
}
