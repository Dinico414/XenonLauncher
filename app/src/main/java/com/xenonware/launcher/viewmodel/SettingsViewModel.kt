package com.xenonware.launcher.viewmodel

import android.Manifest
import android.app.ActivityManager
import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Process
import android.provider.CalendarContract
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.xenon.mylibrary.res.LanguageOption
import com.xenon.mylibrary.res.ThemeSetting
import com.xenonware.launcher.R
import com.xenonware.launcher.data.SharedPreferenceManager
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.model.FabAction
import com.xenonware.launcher.ui.res.IconShape
import com.xenonware.launcher.util.AccessibilityUtils
import com.xenonware.launcher.util.generateCustomIcon
import com.xenonware.launcher.util.loadIconFromPack
import com.xenonware.launcher.util.normalizeIcon
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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

data class BackupInfo(
    val id: String, // Firestore document ID or local filename
    val timestamp: Long,
    val date: String,
    val time: String,
    val device: String,
    val data: String? = null // The actual backup JSON data (if local or already fetched)
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferenceManager = SharedPreferenceManager(application)
    val themeOptions = ThemeSetting.entries.toTypedArray()
    val themeFlags = themeOptions.map { it.nightModeFlag }.toTypedArray()

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps = _apps.asStateFlow()

    private val _currentThemeTitle = MutableStateFlow(
        themeOptions.getOrElse(sharedPreferenceManager.theme) { themeOptions.first { it.nightModeFlag == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM } }.title
    )
    val currentThemeTitle: StateFlow<String> = _currentThemeTitle.asStateFlow()

    private val _blackedOutModeEnabled = MutableStateFlow(sharedPreferenceManager.blackedOutModeEnabled)
    val blackedOutModeEnabled: StateFlow<Boolean> = _blackedOutModeEnabled.asStateFlow()

    private val _blurEnabled = MutableStateFlow(sharedPreferenceManager.blurEnabled)
    val blurEnabled: StateFlow<Boolean> = _blurEnabled.asStateFlow()

    private val _isGridLayout = MutableStateFlow(sharedPreferenceManager.isGridLayout)
    val isGridLayout: StateFlow<Boolean> = _isGridLayout.asStateFlow()

    private val _openKeyboard = MutableStateFlow(sharedPreferenceManager.openKeyboard)
    val openKeyboard: StateFlow<Boolean> = _openKeyboard.asStateFlow()

    private val _openKeyboardPortraitOnly = MutableStateFlow(sharedPreferenceManager.openKeyboardPortraitOnly)
    val openKeyboardPortraitOnly: StateFlow<Boolean> = _openKeyboardPortraitOnly.asStateFlow()

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

    private val _dockSafeDrawImePortraitOnly = MutableStateFlow(sharedPreferenceManager.dockSafeDrawImePortraitOnly)
    val dockSafeDrawImePortraitOnly: StateFlow<Boolean> = _dockSafeDrawImePortraitOnly.asStateFlow()

    private val _drawerIconShape = MutableStateFlow(IconShape.valueOf(sharedPreferenceManager.drawerIconShape))
    val drawerIconShape: StateFlow<IconShape> = _drawerIconShape.asStateFlow()

    private val _drawerIconShadow = MutableStateFlow(sharedPreferenceManager.drawerIconShadow)
    val drawerIconShadow: StateFlow<Boolean> = _drawerIconShadow.asStateFlow()

    private val _globalIconPack = MutableStateFlow(sharedPreferenceManager.globalIconPack)
    val globalIconPack: StateFlow<String?> = _globalIconPack.asStateFlow()

    private val _timeShortcut = MutableStateFlow(sharedPreferenceManager.timeShortcut)
    val timeShortcut: StateFlow<String> = _timeShortcut.asStateFlow()

    private val _dateShortcut = MutableStateFlow(sharedPreferenceManager.dateShortcut)
    val dateShortcut: StateFlow<String> = _dateShortcut.asStateFlow()

    private val _weatherShortcut = MutableStateFlow(sharedPreferenceManager.weatherShortcut)
    val weatherShortcut: StateFlow<String> = _weatherShortcut.asStateFlow()

    private val _visibleCalendars = MutableStateFlow(sharedPreferenceManager.visibleCalendars)
    val visibleCalendars: StateFlow<List<String>> = _visibleCalendars.asStateFlow()

    private val _availableCalendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
    val availableCalendars: StateFlow<List<CalendarInfo>> = _availableCalendars.asStateFlow()

    private val _showCalendarSelectionDialog = MutableStateFlow(false)
    val showCalendarSelectionDialog: StateFlow<Boolean> = _showCalendarSelectionDialog.asStateFlow()

    private val _showNotificationManagerDialog = MutableStateFlow(false)
    val showNotificationManagerDialog: StateFlow<Boolean> = _showNotificationManagerDialog.asStateFlow()

    private val _visibleNotificationApps = MutableStateFlow(sharedPreferenceManager.visibleNotificationApps)
    val visibleNotificationApps: StateFlow<List<String>> = _visibleNotificationApps.asStateFlow()

    private val _currentLanguage = MutableStateFlow(getCurrentLocaleDisplayName())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _availableLanguages = MutableStateFlow<List<LanguageOption>>(emptyList())
    val availableLanguages: StateFlow<List<LanguageOption>> = _availableLanguages.asStateFlow()

    private val _selectedLanguageTagInDialog = MutableStateFlow(getAppLocaleTag())
    val selectedLanguageTagInDialog: StateFlow<String> = _selectedLanguageTagInDialog.asStateFlow()

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

    private val _showGlobalIconPackDialog = MutableStateFlow(false)
    val showGlobalIconPackDialog: StateFlow<Boolean> = _showGlobalIconPackDialog.asStateFlow()

    private val _showVersionDialog = MutableStateFlow(false)
    val showVersionDialog: StateFlow<Boolean> = _showVersionDialog.asStateFlow()

    private val _showSignOutDialog = MutableStateFlow(false)
    val showSignOutDialog: StateFlow<Boolean> = _showSignOutDialog.asStateFlow()

    private val _showHiddenAppsDialog = MutableStateFlow(false)
    val showHiddenAppsDialog: StateFlow<Boolean> = _showHiddenAppsDialog.asStateFlow()

    private val _showBackupDialog = MutableStateFlow(false)
    val showBackupDialog: StateFlow<Boolean> = _showBackupDialog.asStateFlow()

    private val _backups = MutableStateFlow<List<BackupInfo>>(emptyList())
    val backups: StateFlow<List<BackupInfo>> = _backups.asStateFlow()

    private val _isSyncingBackups = MutableStateFlow(false)
    val isSyncingBackups: StateFlow<Boolean> = _isSyncingBackups.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _showFabConfigIsDoubleTap = MutableStateFlow<Boolean?>(null)
    val showFabConfigIsDoubleTap: StateFlow<Boolean?> = _showFabConfigIsDoubleTap.asStateFlow()

    private val _developerModeEnabled = MutableStateFlow(sharedPreferenceManager.developerModeEnabled)
    val developerModeEnabled: StateFlow<Boolean> = _developerModeEnabled.asStateFlow()

    private val _appLabelsEnabled = MutableStateFlow(sharedPreferenceManager.appLabelsEnabled)
    val appLabelsEnabled: StateFlow<Boolean> = _appLabelsEnabled.asStateFlow()

    private val _showClockAtAGlance = MutableStateFlow(sharedPreferenceManager.showClockAtAGlance)
    val showClockAtAGlance: StateFlow<Boolean> = _showClockAtAGlance.asStateFlow()

    private val _hideAtAGlance = MutableStateFlow(sharedPreferenceManager.hideAtAGlance)
    val hideAtAGlance: StateFlow<Boolean> = _hideAtAGlance.asStateFlow()

    private val _notificationIndicatorType = MutableStateFlow(sharedPreferenceManager.notificationIndicatorType)
    val notificationIndicatorType: StateFlow<Int> = _notificationIndicatorType.asStateFlow()

    private val _notificationMessageType = MutableStateFlow(sharedPreferenceManager.notificationMessageType)
    val notificationMessageType: StateFlow<Int> = _notificationMessageType.asStateFlow()

    private val _persistedThemeIndexFlow = MutableStateFlow(sharedPreferenceManager.theme)
    val persistedThemeIndex: StateFlow<Int> = _persistedThemeIndexFlow.asStateFlow()

    private val _fabDoubleTapAction = MutableStateFlow(FabAction.fromString(sharedPreferenceManager.fabDoubleTapAction))
    val fabDoubleTapAction: StateFlow<FabAction> = _fabDoubleTapAction.asStateFlow()

    private val _fabLongPressAction = MutableStateFlow(FabAction.fromString(sharedPreferenceManager.fabLongPressAction))
    val fabLongPressAction: StateFlow<FabAction> = _fabLongPressAction.asStateFlow()

    private val _fabDoubleTapValue = MutableStateFlow(sharedPreferenceManager.fabDoubleTapValue)
    val fabDoubleTapValue: StateFlow<String> = _fabDoubleTapValue.asStateFlow()

    private val _fabLongPressValue = MutableStateFlow(sharedPreferenceManager.fabLongPressValue)
    val fabLongPressValue: StateFlow<String> = _fabLongPressValue.asStateFlow()

    private val _dialogPreviewThemeIndex = MutableStateFlow(sharedPreferenceManager.theme)
    val dialogPreviewThemeIndex: StateFlow<Int> = _dialogPreviewThemeIndex.asStateFlow()

    private val _enableCoverTheme = MutableStateFlow(sharedPreferenceManager.coverThemeEnabled)
    val enableCoverTheme: StateFlow<Boolean> = _enableCoverTheme.asStateFlow()

    private val _isAccessibilityRestricted = MutableStateFlow(AccessibilityUtils.isAccessibilityRestricted(getApplication()))
    val isAccessibilityRestricted: StateFlow<Boolean> = _isAccessibilityRestricted.asStateFlow()

    private val _configShortcutType = MutableStateFlow<LauncherViewModel.ShortcutType?>(null)
    val configShortcutType: StateFlow<LauncherViewModel.ShortcutType?> = _configShortcutType.asStateFlow()

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

    init {
        viewModelScope.launch {
            activeNightModeFlag.collect { nightMode ->
                AppCompatDelegate.setDefaultNightMode(nightMode)
            }
        }
        viewModelScope.launch {
            _persistedThemeIndexFlow.collect { index ->
                _currentThemeTitle.value = themeOptions.getOrElse(index) { themeOptions.first { it.nightModeFlag == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM } }.title
            }
        }
        updateCurrentLanguage()
        prepareLanguageOptions()
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
            val globalPack = _globalIconPack.value
            val globalPackMap = globalPack?.let { com.xenonware.launcher.util.getIconPackMap(context, it) } ?: emptyMap()
            
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
                        // Apply global icon pack if available
                        val componentName = "ComponentInfo{${it.activityInfo.packageName}/${it.activityInfo.name}}"
                        val globalIconRes = globalPackMap[componentName]

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
                        icon = finalIcon,
                        label = finalLabel,
                        isCustomized = isCustomized
                    )
                } catch (_: Exception) {
                    null
                }
            }.sortedBy { it.label.lowercase() }
            _apps.value = appList
        }
    }

    fun onThemeSettingClicked() {
        _dialogPreviewThemeIndex.value = _persistedThemeIndexFlow.value
        _showThemeDialog.value = true
    }

    fun setBlackedOutEnabled(enabled: Boolean) {
        sharedPreferenceManager.blackedOutModeEnabled = enabled
        _blackedOutModeEnabled.value = enabled
    }

    fun setBlurEnabled(enabled: Boolean) {
        sharedPreferenceManager.blurEnabled = enabled
        _blurEnabled.value = enabled
    }

    fun setGridLayout(enabled: Boolean) {
        sharedPreferenceManager.isGridLayout = enabled
        _isGridLayout.value = enabled
    }

    fun setOpenKeyboard(enabled: Boolean) {
        sharedPreferenceManager.openKeyboard = enabled
        _openKeyboard.value = enabled
    }

    fun setOpenKeyboardPortraitOnly(enabled: Boolean) {
        sharedPreferenceManager.openKeyboardPortraitOnly = enabled
        _openKeyboardPortraitOnly.value = enabled
    }

    fun setAdvancedSearchEnabled(enabled: Boolean) {
        sharedPreferenceManager.advancedSearchEnabled = enabled
        _advancedSearchEnabled.value = enabled
    }

    fun setAppLabelsEnabled(enabled: Boolean) {
        sharedPreferenceManager.appLabelsEnabled = enabled
        _appLabelsEnabled.value = enabled
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

    fun hideApp(packageName: String) {
        val current = sharedPreferenceManager.hiddenApps.toMutableList()
        if (!current.contains(packageName)) {
            current.add(packageName)
            sharedPreferenceManager.hiddenApps = current
            _hiddenApps.value = current
        }
    }

    fun setNotificationBadgeType(type: Int) {
        sharedPreferenceManager.notificationBadgeType = type
        _notificationBadgeType.value = type
    }

    fun setDockSafeDrawIme(enabled: Boolean) {
        sharedPreferenceManager.dockSafeDrawIme = enabled
        _dockSafeDrawIme.value = enabled
    }

    fun setDockSafeDrawImePortraitOnly(enabled: Boolean) {
        sharedPreferenceManager.dockSafeDrawImePortraitOnly = enabled
        _dockSafeDrawImePortraitOnly.value = enabled
    }

    fun setShowClockAtAGlance(enabled: Boolean) {
        sharedPreferenceManager.showClockAtAGlance = enabled
        _showClockAtAGlance.value = enabled
    }

    fun setHideAtAGlance(enabled: Boolean) {
        sharedPreferenceManager.hideAtAGlance = enabled
        _hideAtAGlance.value = enabled
    }

    fun setNotificationIndicatorType(type: Int) {
        sharedPreferenceManager.notificationIndicatorType = type
        _notificationIndicatorType.value = type
    }

    fun setNotificationMessageType(type: Int) {
        sharedPreferenceManager.notificationMessageType = type
        _notificationMessageType.value = type
    }

    fun setDrawerIconShape(shape: IconShape) {
        sharedPreferenceManager.drawerIconShape = shape.name
        _drawerIconShape.value = shape
        loadApps()
    }

    fun setDrawerIconShadow(enabled: Boolean) {
        sharedPreferenceManager.drawerIconShadow = enabled
        _drawerIconShadow.value = enabled
    }

    fun setGlobalIconPack(packageName: String?) {
        sharedPreferenceManager.globalIconPack = packageName
        _globalIconPack.value = packageName
        _showGlobalIconPackDialog.value = false
        loadApps()
    }

    fun setShowGlobalIconPackDialog(show: Boolean) {
        _showGlobalIconPackDialog.value = show
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

    fun setVisibleCalendars(calendars: List<String>) {
        sharedPreferenceManager.visibleCalendars = calendars
        _visibleCalendars.value = calendars
    }

    fun toggleCalendarVisibility(calendarId: String) {
        val current = _visibleCalendars.value.toMutableList()
        val allAvailable = _availableCalendars.value.map { it.id }

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
        setVisibleCalendars(new)
    }

    fun setShowCalendarSelectionDialog(show: Boolean) {
        if (show) loadAvailableCalendars()
        _showCalendarSelectionDialog.value = show
    }

    fun setShowNotificationManagerDialog(show: Boolean) {
        _showNotificationManagerDialog.value = show
    }

    fun setVisibleNotificationApps(apps: List<String>) {
        sharedPreferenceManager.visibleNotificationApps = apps
        _visibleNotificationApps.value = apps
    }

    fun toggleNotificationAppVisibility(packageName: String) {
        val current = _visibleNotificationApps.value.toMutableList()
        val allAvailable = _apps.value.map { it.packageName }

        val new = if (current.isEmpty()) {
            allAvailable.toMutableList().apply { remove(packageName) }
        } else if (current.contains("__NONE__")) {
            mutableListOf(packageName)
        } else {
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

    private fun loadAvailableCalendars() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                return@launch
            }

            val calendars = mutableListOf<CalendarInfo>()
            val uri = CalendarContract.Calendars.CONTENT_URI
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.CALENDAR_COLOR,
                CalendarContract.Calendars.ACCOUNT_NAME
            )

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val colorIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
                val accountIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)

                while (cursor.moveToNext()) {
                    calendars.add(
                        CalendarInfo(
                            id = cursor.getString(idIdx),
                            name = cursor.getString(nameIdx) ?: "Unknown",
                            color = cursor.getInt(colorIdx),
                            accountName = cursor.getString(accountIdx) ?: ""
                        )
                    )
                }
            }
            _availableCalendars.value = calendars.sortedBy { it.name.lowercase() }
        }
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
        try {
            context.startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            _selectedLanguageTagInDialog.value = sharedPreferenceManager.languageTag.ifEmpty { getAppLocaleTag() }
            _showLanguageDialog.value = true
        }
    }

    fun onLanguageSelectedInDialog(localeTag: String) {
        _selectedLanguageTagInDialog.value = localeTag
    }

    fun applySelectedLanguage() {

        _showLanguageDialog.value = false
        updateCurrentLanguage()
    }

    fun dismissLanguageDialog() {
        _showLanguageDialog.value = false
        _selectedLanguageTagInDialog.value = getAppLocaleTag()
    }

    fun updateCurrentLanguage() {
        _currentLanguage.value = getCurrentLocaleDisplayName()
        _selectedLanguageTagInDialog.value = getAppLocaleTag()
    }

    private fun getCurrentLocaleDisplayName(): String {
        val application = getApplication<Application>()
        
        // Source 1: AppCompatDelegate override
        val locales = AppCompatDelegate.getApplicationLocales()
        if (!locales.isEmpty && locales[0] != null) {
            val l = locales[0]!!
            return l.getDisplayName(l).replaceFirstChar { it.uppercase() }
        }

        // Source 2: System Per-App Locale (Android 13+)
        val localeManager = application.getSystemService(LocaleManager::class.java)
        val appLocales = localeManager.applicationLocales
        if (!appLocales.isEmpty && appLocales[0] != null) {
            val l = appLocales[0]!!
            return l.getDisplayName(l).replaceFirstChar { it.uppercase() }
        }

        // Source 3: Legacy Manual Override
        val savedTag = sharedPreferenceManager.languageTag
        if (savedTag.isNotEmpty()) {
            val l = Locale.forLanguageTag(savedTag)
            return l.getDisplayName(l).replaceFirstChar { it.uppercase() }
        }

        // Final Fallback: System Default
        return application.getString(R.string.system_default)
    }

    private fun getAppLocaleTag(): String {
        val application = getApplication<Application>()
        val locales = AppCompatDelegate.getApplicationLocales()
        if (!locales.isEmpty) {
            return locales.toLanguageTags()
        }

        val localeManager = application.getSystemService(LocaleManager::class.java)
        val appLocales = localeManager.applicationLocales
        if (!appLocales.isEmpty) return appLocales.toLanguageTags()

        val saved = sharedPreferenceManager.languageTag
        if (saved.isNotEmpty()) return saved

        return ""
    }

    private fun prepareLanguageOptions() {
        val languages = mutableListOf(
            LanguageOption(getApplication<Application>().getString(R.string.system_default), "")
        )
        val en = Locale.forLanguageTag("en")
        languages.add(LanguageOption(en.getDisplayName(en).replaceFirstChar { it.uppercase() }, en.toLanguageTag()))
        val de = Locale.forLanguageTag("de")
        languages.add(LanguageOption(de.getDisplayName(de).replaceFirstChar { it.uppercase() }, de.toLanguageTag()))
        _availableLanguages.value = languages
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
            } catch (_: Exception) {
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
            sharedPreferenceManager.clearSettings()

            val defaultThemeIndex = 2
            _persistedThemeIndexFlow.value = defaultThemeIndex
            _dialogPreviewThemeIndex.value = defaultThemeIndex
            _blackedOutModeEnabled.value = sharedPreferenceManager.blackedOutModeEnabled
            _enableCoverTheme.value = sharedPreferenceManager.coverThemeEnabled
            refreshDeveloperModeState()

            _showResetSettingsDialog.value = false
            delay(1000.milliseconds)
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
                Toast.makeText(context, context.getString(R.string.already_in_developer_mode), Toast.LENGTH_SHORT).show()
            }
            return
        }

        infoTileTapCount++

        if (infoTileTapCount == 1) {
            singleTapJob = viewModelScope.launch {
                delay(tapTimeoutMillis.milliseconds)
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
                Toast.makeText(context, context.getString(R.string.developer_mode_enabled), Toast.LENGTH_LONG).show()
                infoTileTapCount = 0
            } else {
                val remaining = requiredTaps - infoTileTapCount
                Toast.makeText(context, context.getString(R.string.taps_remaining_to_developer, remaining), Toast.LENGTH_SHORT).show()
                resetTapsJob = viewModelScope.launch {
                    delay(multiTapCooldownMillis.milliseconds)
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

    fun refreshDeveloperModeState() {
        _developerModeEnabled.value = sharedPreferenceManager.developerModeEnabled
    }

    fun setCoverThemeEnabled(enabled: Boolean) {
        sharedPreferenceManager.coverThemeEnabled = enabled
        _enableCoverTheme.value = enabled
    }

    fun setFabAction(isDoubleTap: Boolean, action: FabAction, value: String = "") {
        if (isDoubleTap) {
            sharedPreferenceManager.fabDoubleTapAction = action.name
            sharedPreferenceManager.fabDoubleTapValue = value
            _fabDoubleTapAction.value = action
            _fabDoubleTapValue.value = value
        } else {
            sharedPreferenceManager.fabLongPressAction = action.name
            sharedPreferenceManager.fabLongPressValue = value
            _fabLongPressAction.value = action
            _fabLongPressValue.value = value
        }
    }

    fun setConfigShortcut(type: LauncherViewModel.ShortcutType?) {
        _configShortcutType.value = type
    }

    fun saveShortcut(type: LauncherViewModel.ShortcutType, value: String) {
        when (type) {
            LauncherViewModel.ShortcutType.TIME -> sharedPreferenceManager.timeShortcut = value
            LauncherViewModel.ShortcutType.DATE -> sharedPreferenceManager.dateShortcut = value
            LauncherViewModel.ShortcutType.WEATHER -> sharedPreferenceManager.weatherShortcut = value
        }
        _timeShortcut.value = sharedPreferenceManager.timeShortcut
        _dateShortcut.value = sharedPreferenceManager.dateShortcut
        _weatherShortcut.value = sharedPreferenceManager.weatherShortcut
    }

    fun setShowHiddenApps(show: Boolean) {
        _showHiddenAppsDialog.value = show
    }

    fun setShowBackupDialog(show: Boolean) {
        if (show) loadBackups()
        _showBackupDialog.value = show
    }

    fun loadBackups() {
        val user = auth.currentUser
        if (user == null) {
            _backups.value = emptyList()
            return
        }

        _isSyncingBackups.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Changed to 'notes' collection as per the user's reference to the Notes app structure
                val querySnapshot = firestore.collection("notes")
                    .document(user.uid)
                    .collection("backups")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val list = querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        val device = doc.getString("device") ?: "Unknown Device"
                        val data = doc.getString("data")
                        
                        val date = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
                        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
                        
                        BackupInfo(doc.id, timestamp, date, time, device, data)
                    } catch (_: Exception) {
                        null
                    }
                }
                _backups.value = list
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to load backups from Firestore", e)
                withContext(Dispatchers.Main) {
                    if (e.message?.contains("permission", ignoreCase = true) == true) {
                        Toast.makeText(getApplication(), "No cloud permission. Please check account.", Toast.LENGTH_LONG).show()
                    }
                }
            } finally {
                _isSyncingBackups.value = false
            }
        }
    }

    fun startBackup() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(getApplication(), "Please sign in first", Toast.LENGTH_SHORT).show()
            return
        }

        _isSyncingBackups.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = sharedPreferenceManager.getAllPreferences()
                val timestamp = System.currentTimeMillis()
                val device = android.os.Build.MODEL
                
                val dataJson = JSONObject()
                prefs.forEach { (k, v) -> dataJson.put(k, v) }
                val dataString = dataJson.toString()

                val backupDoc = hashMapOf(
                    "timestamp" to timestamp,
                    "device" to device,
                    "data" to dataString
                )

                firestore.collection("notes")
                    .document(user.uid)
                    .collection("backups")
                    .add(backupDoc)
                    .await()
                
                loadBackups()
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Backup saved to cloud", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to upload backup", e)
                withContext(Dispatchers.Main) {
                    val msg = if (e.message?.contains("permission", ignoreCase = true) == true) "No cloud permission" else "Upload failed: ${e.message}"
                    Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show()
                }
            } finally {
                _isSyncingBackups.value = false
            }
        }
    }

    fun restoreBackup(backup: BackupInfo) {
        if (backup.data == null) return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataJson = JSONObject(backup.data)
                
                val map = mutableMapOf<String, Any>()
                dataJson.keys().forEach { k ->
                    map[k] = dataJson.get(k)
                }
                
                sharedPreferenceManager.importPreferences(map)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Restored successfully", Toast.LENGTH_LONG).show()
                    delay(1000.milliseconds)
                    restartApplication(getApplication())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Restore failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun deleteBackup(backup: BackupInfo) {
        val user = auth.currentUser ?: return
        
        _isSyncingBackups.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                firestore.collection("notes")
                    .document(user.uid)
                    .collection("backups")
                    .document(backup.id)
                    .delete()
                    .await()
                
                loadBackups()
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to delete backup", e)
            } finally {
                _isSyncingBackups.value = false
            }
        }
    }

    fun setShowFabConfig(isDoubleTap: Boolean?) {
        _showFabConfigIsDoubleTap.value = isDoubleTap
    }

    fun isDefaultLauncher(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    fun openLauncherSelector(context: Context) {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val selectorIntent = Intent(Intent.ACTION_MAIN)
            selectorIntent.addCategory(Intent.CATEGORY_HOME)
            selectorIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(selectorIntent)
        }
    }

    fun openAccessibilitySettings(context: Context) {
        AccessibilityUtils.requestAccessibility(context)
    }

    fun updateAccessibilityRestriction() {
        _isAccessibilityRestricted.value = AccessibilityUtils.isAccessibilityRestricted(getApplication())
    }

    private fun restartApplication(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            Process.killProcess(Process.myPid())
        }
    }

    private fun openAppInfo(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
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

}
