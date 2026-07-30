package com.xenonware.launcher.viewmodel

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xenonware.launcher.data.SharedPreferenceManager
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.util.generateCustomIcon
import com.xenonware.launcher.util.loadIconFromPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LayoutType {
    COVER, SMALL, COMPACT, MEDIUM, EXPANDED
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferenceManager = SharedPreferenceManager(application)
    val themeOptions = com.xenon.mylibrary.res.ThemeSetting.entries.toTypedArray()
    val themeFlags = themeOptions.map { it.nightModeFlag }.toTypedArray()

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps = _apps.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val pm = context.packageManager
            val launcherPackage = context.packageName
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            
            val overrides = sharedPreferenceManager.getAppOverrides()
            val currentShape = _drawerIconShape.value
            
            val resolvedInfos = pm.queryIntentActivities(intent, 0)
            val appList = resolvedInfos.mapNotNull {
                val pkgName = it.activityInfo.packageName
                if (pkgName == launcherPackage) return@mapNotNull null

                try {
                    val originalLabel = it.loadLabel(pm).toString()
                    val originalIcon = it.loadIcon(pm)
                    
                    val override = overrides[pkgName]
                    var finalLabel = originalLabel
                    var finalIcon: Drawable? = null
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
                        finalIcon = com.xenonware.launcher.util.normalizeIcon(context, originalIcon)
                    }

                    AppInfo(
                        name = originalLabel,
                        packageName = pkgName,
                        icon = finalIcon,
                        label = finalLabel,
                        isCustomized = isCustomized
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedBy { it.label.lowercase() }
            _apps.value = appList
        }
    }

    private val _currentThemeTitle = MutableStateFlow(
        themeOptions.getOrElse(sharedPreferenceManager.theme) { themeOptions.last() }.title
    )
    val currentThemeTitle: StateFlow<String> = _currentThemeTitle.asStateFlow()

    private val _blackedOutModeEnabled = MutableStateFlow(sharedPreferenceManager.blurEnabled)
    val blackedOutModeEnabled: StateFlow<Boolean> = _blackedOutModeEnabled.asStateFlow()

    private val _isGridLayout = MutableStateFlow(sharedPreferenceManager.isGridLayout)
    val isGridLayout: StateFlow<Boolean> = _isGridLayout.asStateFlow()

    private val _openKeyboard = MutableStateFlow(sharedPreferenceManager.openKeyboard)
    val openKeyboard: StateFlow<Boolean> = _openKeyboard.asStateFlow()

    private val _advancedSearchEnabled = MutableStateFlow(sharedPreferenceManager.advancedSearchEnabled)
    val advancedSearchEnabled: StateFlow<Boolean> = _advancedSearchEnabled.asStateFlow()

    private val _showHiddenAppsInSearch = MutableStateFlow(sharedPreferenceManager.showHiddenAppsInSearch)
    val showHiddenAppsInSearch: StateFlow<Boolean> = _showHiddenAppsInSearch.asStateFlow()

    private val _hiddenApps = MutableStateFlow(sharedPreferenceManager.hiddenApps)
    val hiddenApps: StateFlow<List<String>> = _hiddenApps.asStateFlow()

    private val _notificationBadgeType = MutableStateFlow(sharedPreferenceManager.notificationBadgeType)
    val notificationBadgeType: StateFlow<Int> = _notificationBadgeType.asStateFlow()

    private val _dockSafeDrawIme = MutableStateFlow(sharedPreferenceManager.dockSafeDrawIme)
    val dockSafeDrawIme: StateFlow<Boolean> = _dockSafeDrawIme.asStateFlow()

    private val _drawerIconShape = MutableStateFlow(com.xenonware.launcher.ui.res.IconShape.valueOf(sharedPreferenceManager.drawerIconShape))
    val drawerIconShape: StateFlow<com.xenonware.launcher.ui.res.IconShape> = _drawerIconShape.asStateFlow()

    private val _drawerIconShadow = MutableStateFlow(sharedPreferenceManager.drawerIconShadow)
    val drawerIconShadow: StateFlow<Boolean> = _drawerIconShadow.asStateFlow()

    private val _timeShortcut = MutableStateFlow(sharedPreferenceManager.timeShortcut)
    val timeShortcut: StateFlow<String> = _timeShortcut.asStateFlow()

    private val _dateShortcut = MutableStateFlow(sharedPreferenceManager.dateShortcut)
    val dateShortcut: StateFlow<String> = _dateShortcut.asStateFlow()

    private val _weatherShortcut = MutableStateFlow(sharedPreferenceManager.weatherShortcut)
    val weatherShortcut: StateFlow<String> = _weatherShortcut.asStateFlow()

    private val _hasWallpaperAccess = MutableStateFlow(checkWallpaperAccess())
    val hasWallpaperAccess: StateFlow<Boolean> = _hasWallpaperAccess.asStateFlow()

    private val _currentLanguage = MutableStateFlow("English")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _showThemeDialog = MutableStateFlow(false)
    val showThemeDialog: StateFlow<Boolean> = _showThemeDialog.asStateFlow()

    private val _showClearDataDialog = MutableStateFlow(false)
    val showClearDataDialog: StateFlow<Boolean> = _showClearDataDialog.asStateFlow()

    private val _showResetSettingsDialog = MutableStateFlow(false)
    val showResetSettingsDialog: StateFlow<Boolean> = _showResetSettingsDialog.asStateFlow()

    private val _showCoverSelectionDialog = MutableStateFlow(false)
    val showCoverSelectionDialog: StateFlow<Boolean> = _showCoverSelectionDialog.asStateFlow()

    private val _showLanguageDialog = MutableStateFlow(false)
    val showLanguageDialog: StateFlow<Boolean> = _showLanguageDialog.asStateFlow()

    private val _showVersionDialog = MutableStateFlow(false)
    val showVersionDialog: StateFlow<Boolean> = _showVersionDialog.asStateFlow()

    private val _showSignOutDialog = MutableStateFlow(false)
    val showSignOutDialog: StateFlow<Boolean> = _showSignOutDialog.asStateFlow()

    private val _showDeveloperOptions = MutableStateFlow(false)
    val showDeveloperOptions: StateFlow<Boolean> = _showDeveloperOptions.asStateFlow()

    private val _developerModeEnabled = MutableStateFlow(sharedPreferenceManager.developerModeEnabled)
    val developerModeEnabled: StateFlow<Boolean> = _developerModeEnabled.asStateFlow()

    private val _persistedThemeIndexFlow = MutableStateFlow(sharedPreferenceManager.theme)
    val persistedThemeIndex: StateFlow<Int> = _persistedThemeIndexFlow.asStateFlow()

    private val _dialogPreviewThemeIndex = MutableStateFlow(sharedPreferenceManager.theme)
    val dialogPreviewThemeIndex: StateFlow<Int> = _dialogPreviewThemeIndex.asStateFlow()

    private val _enableCoverTheme = MutableStateFlow(sharedPreferenceManager.coverThemeEnabled)
    val enableCoverTheme: StateFlow<Boolean> = _enableCoverTheme.asStateFlow()

    private var infoTileTapCount = 0
    private var singleTapJob: Job? = null
    private var resetTapsJob: Job? = null
    private val requiredTaps = 7
    private val tapTimeoutMillis = 500L
    private var lastMultiTapTime: Long = 0
    private val multiTapCooldownMillis = 500L
    private var currentToast: Toast? = null

    val activeNightModeFlag: StateFlow<Int> = combine(
        _persistedThemeIndexFlow,
        _dialogPreviewThemeIndex,
        _showThemeDialog
    ) { persistedIndex, previewIndex, isDialogShowing ->
        val themeIndexToUse = if (isDialogShowing) previewIndex else persistedIndex
        themeFlags.getOrElse(themeIndexToUse) { AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = themeFlags.getOrElse(sharedPreferenceManager.theme) { AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM }
    )

    private fun checkWallpaperAccess(): Boolean {
        val context = getApplication<Application>()
        return when {
            true -> {
                Environment.isExternalStorageManager()
            }
            true -> {
                context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED ||
                        Environment.isExternalStorageManager()
            }
            else -> {
                context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun onThemeSettingClicked() {
        _dialogPreviewThemeIndex.value = _persistedThemeIndexFlow.value
        _showThemeDialog.value = true
    }

    fun setBlackedOutEnabled(enabled: Boolean) {
        sharedPreferenceManager.blurEnabled = enabled
        _blackedOutModeEnabled.value = enabled
    }

    fun setGridLayout(enabled: Boolean) {
        sharedPreferenceManager.isGridLayout = enabled
        _isGridLayout.value = enabled
    }

    fun setOpenKeyboard(enabled: Boolean) {
        sharedPreferenceManager.openKeyboard = enabled
        _openKeyboard.value = enabled
    }

    fun setAdvancedSearchEnabled(enabled: Boolean) {
        sharedPreferenceManager.advancedSearchEnabled = enabled
        _advancedSearchEnabled.value = enabled
    }

    fun setShowHiddenAppsInSearch(enabled: Boolean) {
        sharedPreferenceManager.showHiddenAppsInSearch = enabled
        _showHiddenAppsInSearch.value = enabled
    }

    fun unhideApp(packageName: String) {
        val current = sharedPreferenceManager.hiddenApps.toMutableList()
        current.remove(packageName)
        sharedPreferenceManager.hiddenApps = current
        _hiddenApps.value = current
    }

    fun setNotificationBadgeType(type: Int) {
        sharedPreferenceManager.notificationBadgeType = type
        _notificationBadgeType.value = type
    }

    fun setDockSafeDrawIme(enabled: Boolean) {
        sharedPreferenceManager.dockSafeDrawIme = enabled
        _dockSafeDrawIme.value = enabled
    }

    fun setDrawerIconShape(shape: com.xenonware.launcher.ui.res.IconShape) {
        sharedPreferenceManager.drawerIconShape = shape.name
        _drawerIconShape.value = shape
        loadApps()
    }

    fun setDrawerIconShadow(enabled: Boolean) {
        sharedPreferenceManager.drawerIconShadow = enabled
        _drawerIconShadow.value = enabled
    }

    fun setTimeShortcut(value: String) {
        sharedPreferenceManager.timeShortcut = value
        _timeShortcut.value = value
    }

    fun setDateShortcut(value: String) {
        sharedPreferenceManager.dateShortcut = value
        _dateShortcut.value = value
    }

    fun setWeatherShortcut(value: String) {
        sharedPreferenceManager.weatherShortcut = value
        _weatherShortcut.value = value
    }

    fun onThemeOptionSelectedInDialog(index: Int) {
        if (index >= 0 && index < themeOptions.size) {
            _dialogPreviewThemeIndex.value = index
            _persistedThemeIndexFlow.value = index
        }
    }

    fun applySelectedTheme() {
        val indexToApply = _dialogPreviewThemeIndex.value
        if (indexToApply >= 0 && indexToApply < themeOptions.size) {
            sharedPreferenceManager.theme = indexToApply
            _persistedThemeIndexFlow.value = indexToApply
            _currentThemeTitle.value = themeOptions[indexToApply].title
        }
        _showThemeDialog.value = false
    }

    fun dismissThemeDialog() {
        _showThemeDialog.value = false
        _dialogPreviewThemeIndex.value = sharedPreferenceManager.theme
        _persistedThemeIndexFlow.value = sharedPreferenceManager.theme
    }

    fun onCoverThemeClicked() {
        _showCoverSelectionDialog.value = true
    }

    fun dismissCoverThemeDialog() {
        _showCoverSelectionDialog.value = false
    }

    fun saveCoverDisplayMetrics(displaySize: IntSize) {
        sharedPreferenceManager.coverDisplaySize = displaySize
        _enableCoverTheme.value = true
        sharedPreferenceManager.coverThemeEnabled = true
        _showCoverSelectionDialog.value = false
    }

    fun applyCoverTheme(displaySize: IntSize): Boolean {
        return sharedPreferenceManager.isCoverThemeApplied(displaySize)
    }

    fun onLanguageSettingClicked(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                context.startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) {
                _showLanguageDialog.value = true
            }
        } else {
            _showLanguageDialog.value = true
        }
    }

    fun dismissLanguageDialog() {
        _showLanguageDialog.value = false
    }

    fun onClearDataClicked() {
        _showClearDataDialog.value = true
    }

    fun confirmClearData() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                @Suppress("DEPRECATION")
                val success = activityManager.clearApplicationUserData()
                if (success) {
                    restartApplication(context)
                } else {
                    openAppInfo(context)
                }
            } catch (e: Exception) {
                openAppInfo(context)
            } finally {
                _showClearDataDialog.value = false
            }
        }
    }

    fun dismissClearDataDialog() {
        _showClearDataDialog.value = false
    }

    fun onResetSettingsClicked() {
        _showResetSettingsDialog.value = true
    }

    fun confirmResetSettings() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            sharedPreferenceManager.theme = 2
            sharedPreferenceManager.blurEnabled = true
            sharedPreferenceManager.isGridLayout = true
            sharedPreferenceManager.openKeyboard = false
            sharedPreferenceManager.advancedSearchEnabled = true
            sharedPreferenceManager.notificationBadgeType = 1
            sharedPreferenceManager.dockSafeDrawIme = false
            sharedPreferenceManager.drawerIconShape = "Circle"
            sharedPreferenceManager.drawerIconShadow = false
            sharedPreferenceManager.timeShortcut = ""
            sharedPreferenceManager.dateShortcut = ""
            sharedPreferenceManager.weatherShortcut = ""
            sharedPreferenceManager.hiddenApps = emptyList()
            sharedPreferenceManager.showHiddenAppsInSearch = false
            sharedPreferenceManager.coverThemeEnabled = false
            sharedPreferenceManager.developerModeEnabled = false

            _persistedThemeIndexFlow.value = 2
            _dialogPreviewThemeIndex.value = 2
            _blackedOutModeEnabled.value = true
            _isGridLayout.value = true
            _openKeyboard.value = false
            _advancedSearchEnabled.value = true
            _notificationBadgeType.value = 1
            _dockSafeDrawIme.value = false
            _drawerIconShape.value = com.xenonware.launcher.ui.res.IconShape.Circle
            _drawerIconShadow.value = false
            _timeShortcut.value = ""
            _dateShortcut.value = ""
            _weatherShortcut.value = ""
            _hiddenApps.value = emptyList()
            _showHiddenAppsInSearch.value = false
            _enableCoverTheme.value = false
            _developerModeEnabled.value = false

            _showResetSettingsDialog.value = false
            delay(1000)
            restartApplication(context)
        }
    }

    fun dismissResetSettingsDialog() {
        _showResetSettingsDialog.value = false
    }

    fun onInfoTileClicked() {
        val context = getApplication<Application>()
        currentToast?.cancel()
        singleTapJob?.cancel()
        resetTapsJob?.cancel()

        val currentTime = System.currentTimeMillis()

        if (currentTime - lastMultiTapTime < multiTapCooldownMillis) {
            if (_developerModeEnabled.value) {
                Toast.makeText(context, "Already in developer mode", Toast.LENGTH_SHORT).show()
            }
            return
        }

        infoTileTapCount++

        if (infoTileTapCount == 1) {
            singleTapJob = viewModelScope.launch {
                delay(tapTimeoutMillis)
                _showVersionDialog.value = true
                infoTileTapCount = 0
            }
        } else {
            lastMultiTapTime = currentTime
            if (_developerModeEnabled.value) {
                infoTileTapCount = 0
                return
            }
            if (infoTileTapCount >= requiredTaps) {
                sharedPreferenceManager.developerModeEnabled = true
                _developerModeEnabled.value = true
                Toast.makeText(context, "Developer mode enabled", Toast.LENGTH_LONG).show()
                infoTileTapCount = 0
            } else {
                val remaining = requiredTaps - infoTileTapCount
                Toast.makeText(context, "$remaining taps to developer", Toast.LENGTH_SHORT).show()
                resetTapsJob = viewModelScope.launch {
                    delay(multiTapCooldownMillis)
                    infoTileTapCount = 0
                }
            }
        }
    }

    fun dismissVersionDialog() {
        _showVersionDialog.value = false
    }

    fun openImpressum(context: Context) {
        Toast.makeText(context, "Xenon Launcher by XenonWare", Toast.LENGTH_LONG).show()
    }

    fun onSignOutClicked() {
        _showSignOutDialog.value = true
    }

    fun dismissSignOutDialog() {
        _showSignOutDialog.value = false
    }

    fun showDeveloperOptions() {
        _showDeveloperOptions.value = true
    }

    fun dismissDeveloperOptions() {
        _showDeveloperOptions.value = false
    }

    private fun restartApplication(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun openAppInfo(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun refreshDeveloperModeState() {
        _developerModeEnabled.value = sharedPreferenceManager.developerModeEnabled
    }

    fun setCoverThemeEnabled(enabled: Boolean) {
        sharedPreferenceManager.coverThemeEnabled = enabled
        _enableCoverTheme.value = enabled
    }

    class SettingsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettingsViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    fun requestWallpaperAccess(context: android.content.Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val genericIntent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
            }
        }
    }

    fun refreshWallpaperAccess() {
        _hasWallpaperAccess.value = checkWallpaperAccess()
    }
}
