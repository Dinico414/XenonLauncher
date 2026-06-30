package com.xenonware.launcher.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xenonware.launcher.model.AppInfo
import kotlinx.coroutines.Dispatchers
import com.xenonware.launcher.media.MediaControllerManager
import com.xenonware.launcher.media.MediaState
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WeatherState(
    val temperature: String = "24°C",
    val condition: String = "Sunny"
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps
    
    val mediaControllerManager = MediaControllerManager(application)
    val mediaState: MediaState get() = mediaControllerManager.mediaState

    val notificationCount = com.xenonware.launcher.notification.NotificationManager.notificationCount

    private val _currentTime = MutableStateFlow(LocalDateTime.now())
    val currentTime: StateFlow<LocalDateTime> = _currentTime

    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

    init {
        loadApps()
        startMediaUpdates()
        startTimeUpdates()
        startWeatherUpdates()
    }

    private fun startWeatherUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    // Simple weather fetch from wttr.in (text-based for simplicity)
                    val url = java.net.URL("https://wttr.in?format=%t")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    
                    val text = connection.inputStream.bufferedReader().use { it.readText() }.trim()
                    if (text.isNotEmpty() && (text.contains("+") || text.contains("-"))) {
                        _weatherState.value = WeatherState(temperature = text)
                    }
                } catch (e: Exception) {
                    // Keep previous state on error
                }
                delay(1800000) // Update every 30 minutes
            }
        }
    }

    private fun startTimeUpdates() {
        viewModelScope.launch {
            while (true) {
                _currentTime.value = LocalDateTime.now()
                delay(60000) // Update every minute
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
        }
    }

    fun launchApp(packageName: String) {
        val pm = getApplication<Application>().packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            getApplication<Application>().startActivity(launchIntent)
        }
    }
}
