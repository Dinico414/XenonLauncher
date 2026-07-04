import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.BatteryManager
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
import kotlinx.coroutines.Dispatchers
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

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val prefManager = SharedPreferenceManager(application)

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "is_grid_layout" -> _isGridLayout.value = prefManager.isGridLayout
            "auto_focus_search" -> _autoFocusSearch.value = prefManager.autoFocusSearch
        }
    }

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _installingApps = MutableStateFlow<Map<Int, AppInfo>>(emptyMap())

    private val sessionCallback = object : PackageInstaller.SessionCallback() {
        override fun onCreated(sessionId: Int) = updateInstallingApps()
        override fun onBadgingChanged(sessionId: Int) = updateInstallingApps()
        override fun onActiveChanged(sessionId: Int, active: Boolean) = updateInstallingApps()
        override fun onProgressChanged(sessionId: Int, progress: Float) {
            val current = _installingApps.value.toMutableMap()
            val session = getApplication<Application>().packageManager.packageInstaller.getSessionInfo(sessionId)
            current[sessionId]?.let { oldApp ->
                val newLabel = getDisplayLabel(session) ?: oldApp.name
                if (oldApp.installProgress != progress || oldApp.name != newLabel) {
                    current[sessionId] = oldApp.copy(
                        installProgress = progress,
                        name = newLabel
                    )
                    _installingApps.value = current
                    combineApps()
                }
            }
        }
        override fun onFinished(sessionId: Int, success: Boolean) {
            val current = _installingApps.value.toMutableMap()
            current.remove(sessionId)
            _installingApps.value = current
            loadApps()
        }
    }

    private fun updateInstallingApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val installer = pm.packageInstaller
            val sessions = installer.allSessions
            val newInstallingApps = mutableMapOf<Int, AppInfo>()
            
            sessions.forEach { session ->
                val packageName = session.appPackageName ?: return@forEach
                val label = getDisplayLabel(session) ?: beautifyPackageName(packageName)
                
                val icon = session.appIcon?.let { BitmapDrawable(getApplication<Application>().resources, it) }
                
                newInstallingApps[session.sessionId] = AppInfo(
                    name = label,
                    packageName = packageName,
                    icon = icon,
                    installProgress = session.progress,
                    isInstalling = true
                )
            }
            _installingApps.value = newInstallingApps
            combineApps()
        }
    }

    private fun getDisplayLabel(session: PackageInstaller.SessionInfo?): String? {
        val rawLabel = session?.appLabel?.toString()?.trim()
        val packageName = session?.appPackageName ?: return null
        
        val statusKeywords = listOf(
            "install", "wird", "get", "herunter", "download", 
            "pending", "ausstehend", "wait", "laden"
        )
        
        val isStatus = rawLabel.isNullOrBlank() || statusKeywords.any { word -> 
            rawLabel.contains(word, ignoreCase = true) 
        } && rawLabel.length < 25
        
        return if (isStatus) beautifyPackageName(packageName) else rawLabel
    }

    private fun beautifyPackageName(packageName: String): String {
        val parts = packageName.split('.')
        if (parts.isEmpty()) return packageName
        
        val name = when {
            parts.size >= 3 && (parts.last().equals("android", ignoreCase = true) || parts.last().equals("mobile", ignoreCase = true)) -> parts[parts.size - 2]
            parts.size >= 2 && parts[0].equals("com", ignoreCase = true) -> parts[1]
            else -> parts.last()
        }

        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun combineApps() {
        val installed = _installedApps.value
        val installing = _installingApps.value.values.toList()
        
        // Remove installing apps if they are already in the installed list (just in case of overlap during finish)
        val filteredInstalling = installing.filter { installingApp ->
            installed.none { it.packageName == installingApp.packageName }
        }
        
        _apps.value = (installed + filteredInstalling).sortedBy { it.name.lowercase() }
    }

    private val _pinnedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val pinnedApps: StateFlow<List<AppInfo>> = _pinnedApps

    private val _isGridLayout = MutableStateFlow(prefManager.isGridLayout)
    val isGridLayout: StateFlow<Boolean> = _isGridLayout

    private val _autoFocusSearch = MutableStateFlow(prefManager.autoFocusSearch)
    val autoFocusSearch: StateFlow<Boolean> = _autoFocusSearch

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
        }
    }
    
    val mediaControllerManager = MediaControllerManager(application)
    val mediaState: MediaState get() = mediaControllerManager.mediaState

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    val notificationCount = com.xenonware.launcher.notification.NotificationManager.notificationCount

    private val _currentTime = MutableStateFlow(LocalDateTime.now())
    val currentTime: StateFlow<LocalDateTime> = _currentTime

    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState

    private val _batteryLevel = MutableStateFlow(1f)
    val batteryLevel: StateFlow<Float> = _batteryLevel

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

    init {
        prefManager.registerListener(preferenceListener)
        loadApps()
        startMediaUpdates()
        startTimeUpdates()
        startWeatherUpdates()

        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        application.registerReceiver(packageReceiver, packageFilter)
        application.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        val installer = application.packageManager.packageInstaller
        installer.registerSessionCallback(sessionCallback)
        updateInstallingApps()
    }

    override fun onCleared() {
        super.onCleared()
        prefManager.unregisterListener(preferenceListener)
        getApplication<Application>().unregisterReceiver(packageReceiver)
        getApplication<Application>().unregisterReceiver(batteryReceiver)
        getApplication<Application>().packageManager.packageInstaller.unregisterSessionCallback(sessionCallback)
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
            _installedApps.value = appList
            combineApps()

            // Restore pinned apps once the main list is loaded
            val savedPinnedPkgs = prefManager.pinnedApps
            _pinnedApps.value = savedPinnedPkgs.mapNotNull { pkg ->
                appList.find { it.packageName == pkg }
            }
        }
    }

    private fun savePinnedApps() {
        prefManager.pinnedApps = _pinnedApps.value.map { it.packageName }
    }

    fun launchApp(packageName: String) {
        val pm = getApplication<Application>().packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
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

    fun setAutoFocusSearch(enabled: Boolean) {
        _autoFocusSearch.value = enabled
        prefManager.autoFocusSearch = enabled
    }
}
