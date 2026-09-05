package com.xenonware.launcher.viewmodel

import android.Manifest
import android.app.ActivityManager
import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

enum class FabConfigMode { NONE, SINGLE, DOUBLE, LONG }

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

    private val _selectedLanguageTagInDialog = MutableStateFlow("")
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

    private val _showFabConfigMode = MutableStateFlow(FabConfigMode.NONE)
    val showFabConfigMode: StateFlow<FabConfigMode> = _showFabConfigMode.asStateFlow()

    private val _developerModeEnabled = MutableStateFlow(sharedPreferenceManager.developerModeEnabled)
    val developerModeEnabled: StateFlow<Boolean> = _developerModeEnabled.asStateFlow()

    private val _appLabelsEnabled = MutableStateFlow(sharedPreferenceManager.appLabelsEnabled)
    val appLabelsEnabled: StateFlow<Boolean> = _appLabelsEnabled.asStateFlow()

    private val _showClockAtAGlance = MutableStateFlow(sharedPreferenceManager.showClockAtAGlance)
    val showClockAtAGlance: StateFlow<Boolean> = _showClockAtAGlance.asStateFlow()

    private val _hideAtAGlance = MutableStateFlow(sharedPreferenceManager.hideAtAGlance)
    val hideAtAGlance: StateFlow<Boolean> = _hideAtAGlance.asStateFlow()

    private val _hideDockScrolling = MutableStateFlow(sharedPreferenceManager.hideDockScrolling)
    val hideDockScrolling: StateFlow<Boolean> = _hideDockScrolling.asStateFlow()

    private val _hideDockScrollingOnlySmall = MutableStateFlow(sharedPreferenceManager.hideDockScrollingOnlySmall)
    val hideDockScrollingOnlySmall: StateFlow<Boolean> = _hideDockScrollingOnlySmall.asStateFlow()

    private val _hideDockWidgets = MutableStateFlow(sharedPreferenceManager.hideDockWidgets)
    val hideDockWidgets: StateFlow<Boolean> = _hideDockWidgets.asStateFlow()

    private val _hideDockWidgetsLandscapeOnly = MutableStateFlow(sharedPreferenceManager.hideDockWidgetsLandscapeOnly)
    val hideDockWidgetsLandscapeOnly: StateFlow<Boolean> = _hideDockWidgetsLandscapeOnly.asStateFlow()

    private val _hideDockMedia = MutableStateFlow(sharedPreferenceManager.hideDockMedia)
    val hideDockMedia: StateFlow<Boolean> = _hideDockMedia.asStateFlow()

    private val _hideDockMediaLandscapeOnly = MutableStateFlow(sharedPreferenceManager.hideDockMediaLandscapeOnly)
    val hideDockMediaLandscapeOnly: StateFlow<Boolean> = _hideDockMediaLandscapeOnly.asStateFlow()

    private val _hideActionButton = MutableStateFlow(sharedPreferenceManager.hideActionButton)
    val hideActionButton: StateFlow<Boolean> = _hideActionButton.asStateFlow()

    private val _moveWebSearch = MutableStateFlow(sharedPreferenceManager.moveWebSearch)
    val moveWebSearch: StateFlow<Boolean> = _moveWebSearch.asStateFlow()

    private val _showMuteNotifications = MutableStateFlow(sharedPreferenceManager.showMuteNotifications)
    val showMuteNotifications: StateFlow<Boolean> = _showMuteNotifications.asStateFlow()

    private val _showPermanentNotifications = MutableStateFlow(sharedPreferenceManager.showPermanentNotifications)
    val showPermanentNotifications: StateFlow<Boolean> = _showPermanentNotifications.asStateFlow()

    private val _disableGrouping = MutableStateFlow(sharedPreferenceManager.disableGrouping)
    val disableGrouping: StateFlow<Boolean> = _disableGrouping.asStateFlow()

    private val _notificationIndicatorType = MutableStateFlow(sharedPreferenceManager.notificationIndicatorType)
    val notificationIndicatorType: StateFlow<Int> = _notificationIndicatorType.asStateFlow()

    private val _notificationMessageType = MutableStateFlow(sharedPreferenceManager.notificationMessageType)
    val notificationMessageType: StateFlow<Int> = _notificationMessageType.asStateFlow()

    private val _persistedThemeIndexFlow = MutableStateFlow(sharedPreferenceManager.theme)
    val persistedThemeIndex: StateFlow<Int> = _persistedThemeIndexFlow.asStateFlow()

    private val _fabSingleTapAction = MutableStateFlow(FabAction.fromString(sharedPreferenceManager.fabSingleTapAction))
    val fabSingleTapAction: StateFlow<FabAction> = _fabSingleTapAction.asStateFlow()

    private val _fabDoubleTapAction = MutableStateFlow(FabAction.fromString(sharedPreferenceManager.fabDoubleTapAction))
    val fabDoubleTapAction: StateFlow<FabAction> = _fabDoubleTapAction.asStateFlow()

    private val _fabLongPressAction = MutableStateFlow(FabAction.fromString(sharedPreferenceManager.fabLongPressAction))
    val fabLongPressAction: StateFlow<FabAction> = _fabLongPressAction.asStateFlow()

    private val _fabSingleTapValue = MutableStateFlow(sharedPreferenceManager.fabSingleTapValue)
    val fabSingleTapValue: StateFlow<String> = _fabSingleTapValue.asStateFlow()

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

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "blacked_out_mode_enabled" -> _blackedOutModeEnabled.value = sharedPreferenceManager.blackedOutModeEnabled
            "blur_enabled" -> _blurEnabled.value = sharedPreferenceManager.blurEnabled
            "is_grid_layout" -> _isGridLayout.value = sharedPreferenceManager.isGridLayout
            "open_keyboard" -> _openKeyboard.value = sharedPreferenceManager.openKeyboard
            "open_keyboard_portrait_only" -> _openKeyboardPortraitOnly.value = sharedPreferenceManager.openKeyboardPortraitOnly
            "advanced_search_enabled" -> _advancedSearchEnabled.value = sharedPreferenceManager.advancedSearchEnabled
            "app_labels_enabled" -> _appLabelsEnabled.value = sharedPreferenceManager.appLabelsEnabled
            "show_hidden_apps_in_search" -> _showHiddenAppsInSearch.value = sharedPreferenceManager.showHiddenAppsInSearch
            "notification_badge_type" -> _notificationBadgeType.value = sharedPreferenceManager.notificationBadgeType
            "dock_safedraw_ime" -> _dockSafeDrawIme.value = sharedPreferenceManager.dockSafeDrawIme
            "dock_safedraw_ime_portrait_only" -> _dockSafeDrawImePortraitOnly.value = sharedPreferenceManager.dockSafeDrawImePortraitOnly
            "drawer_icon_shape" -> _drawerIconShape.value = IconShape.valueOf(sharedPreferenceManager.drawerIconShape)
            "drawer_icon_shadow" -> _drawerIconShadow.value = sharedPreferenceManager.drawerIconShadow
            "global_icon_pack" -> _globalIconPack.value = sharedPreferenceManager.globalIconPack
            "time_shortcut" -> _timeShortcut.value = sharedPreferenceManager.timeShortcut
            "date_shortcut" -> _dateShortcut.value = sharedPreferenceManager.dateShortcut
            "weather_shortcut" -> _weatherShortcut.value = sharedPreferenceManager.weatherShortcut
            "visible_calendars" -> _visibleCalendars.value = sharedPreferenceManager.visibleCalendars
            "visible_notification_apps" -> _visibleNotificationApps.value = sharedPreferenceManager.visibleNotificationApps
            "theme" -> _persistedThemeIndexFlow.value = sharedPreferenceManager.theme
            "fab_single_tap_action" -> _fabSingleTapAction.value = FabAction.fromString(sharedPreferenceManager.fabSingleTapAction)
            "fab_double_tap_action" -> _fabDoubleTapAction.value = FabAction.fromString(sharedPreferenceManager.fabDoubleTapAction)
            "fab_long_press_action" -> _fabLongPressAction.value = FabAction.fromString(sharedPreferenceManager.fabLongPressAction)
            "fab_single_tap_value" -> _fabSingleTapValue.value = sharedPreferenceManager.fabSingleTapValue
            "fab_double_tap_value" -> _fabDoubleTapValue.value = sharedPreferenceManager.fabDoubleTapValue
            "fab_long_press_value" -> _fabLongPressValue.value = sharedPreferenceManager.fabLongPressValue
            "show_clock_at_a_glance" -> _showClockAtAGlance.value = sharedPreferenceManager.showClockAtAGlance
            "hide_at_a_glance" -> _hideAtAGlance.value = sharedPreferenceManager.hideAtAGlance
            "hide_dock_scrolling" -> _hideDockScrolling.value = sharedPreferenceManager.hideDockScrolling
            "hide_dock_scrolling_only_small" -> _hideDockScrollingOnlySmall.value = sharedPreferenceManager.hideDockScrollingOnlySmall
            "hide_dock_widgets" -> _hideDockWidgets.value = sharedPreferenceManager.hideDockWidgets
            "hide_dock_widgets_landscape_only" -> _hideDockWidgetsLandscapeOnly.value = sharedPreferenceManager.hideDockWidgetsLandscapeOnly
            "hide_dock_media" -> _hideDockMedia.value = sharedPreferenceManager.hideDockMedia
            "hide_dock_media_landscape_only" -> _hideDockMediaLandscapeOnly.value = sharedPreferenceManager.hideDockMediaLandscapeOnly
            "hide_action_button" -> _hideActionButton.value = sharedPreferenceManager.hideActionButton
            "move_web_search" -> _moveWebSearch.value = sharedPreferenceManager.moveWebSearch
            "show_mute_notifications" -> _showMuteNotifications.value = sharedPreferenceManager.showMuteNotifications
            "show_permanent_notifications" -> _showPermanentNotifications.value = sharedPreferenceManager.showPermanentNotifications
            "disable_grouping" -> _disableGrouping.value = sharedPreferenceManager.disableGrouping
            "notification_indicator_type" -> _notificationIndicatorType.value = sharedPreferenceManager.notificationIndicatorType
            "notification_message_type" -> _notificationMessageType.value = sharedPreferenceManager.notificationMessageType
        }
    }

    init {
        sharedPreferenceManager.registerListener(preferenceListener)
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
            val pm = getApplication<Application>().packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val resolvedApps = pm.queryIntentActivities(mainIntent, 0)
            val appList = resolvedApps.map { resolveInfo ->
                AppInfo(
                    name = resolveInfo.loadLabel(pm).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = resolveInfo.loadIcon(pm)
                )
            }.sortedBy { it.label.lowercase() }
            _apps.value = appList
        }
    }

    fun onThemeSettingClicked() {
        _dialogPreviewThemeIndex.value = sharedPreferenceManager.theme
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
        val hidden = sharedPreferenceManager.hiddenApps.toMutableList()
        hidden.remove(packageName)
        sharedPreferenceManager.hiddenApps = hidden
        _hiddenApps.value = hidden
    }

    fun hideApp(packageName: String) {
        val hidden = sharedPreferenceManager.hiddenApps.toMutableList()
        if (!hidden.contains(packageName)) {
            hidden.add(packageName)
            sharedPreferenceManager.hiddenApps = hidden
            _hiddenApps.value = hidden
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

    fun setHideDockScrolling(enabled: Boolean) {
        sharedPreferenceManager.hideDockScrolling = enabled
        _hideDockScrolling.value = enabled
    }

    fun setHideDockScrollingOnlySmall(enabled: Boolean) {
        sharedPreferenceManager.hideDockScrollingOnlySmall = enabled
        _hideDockScrollingOnlySmall.value = enabled
    }

    fun setHideDockWidgets(enabled: Boolean) {
        sharedPreferenceManager.hideDockWidgets = enabled
        _hideDockWidgets.value = enabled
    }

    fun setHideDockWidgetsLandscapeOnly(enabled: Boolean) {
        sharedPreferenceManager.hideDockWidgetsLandscapeOnly = enabled
        _hideDockWidgetsLandscapeOnly.value = enabled
    }

    fun setHideDockMedia(enabled: Boolean) {
        sharedPreferenceManager.hideDockMedia = enabled
        _hideDockMedia.value = enabled
    }

    fun setHideDockMediaLandscapeOnly(enabled: Boolean) {
        sharedPreferenceManager.hideDockMediaLandscapeOnly = enabled
        _hideDockMediaLandscapeOnly.value = enabled
    }

    fun setHideActionButton(enabled: Boolean) {
        sharedPreferenceManager.hideActionButton = enabled
        _hideActionButton.value = enabled
    }

    fun setMoveWebSearch(enabled: Boolean) {
        sharedPreferenceManager.moveWebSearch = enabled
        _moveWebSearch.value = enabled
    }

    fun setShowMuteNotifications(enabled: Boolean) {
        sharedPreferenceManager.showMuteNotifications = enabled
        _showMuteNotifications.value = enabled
    }

    fun setShowPermanentNotifications(enabled: Boolean) {
        sharedPreferenceManager.showPermanentNotifications = enabled
        _showPermanentNotifications.value = enabled
    }

    fun setDisableGrouping(enabled: Boolean) {
        sharedPreferenceManager.disableGrouping = enabled
        _disableGrouping.value = enabled
    }

    fun setNotificationIndicatorType(type: Int) {
        sharedPreferenceManager.notificationIndicatorType = type
        _notificationIndicatorType.value = type
    }

    fun setNotificationMessageType(type: Int) {
        sharedPreferenceManager.notificationMessageType = type
        _notificationMessageType.value = type
    }

    fun setFabSingleTapAction(action: FabAction) {
        sharedPreferenceManager.fabSingleTapAction = action.name
        _fabSingleTapAction.value = action
    }

    fun setFabDoubleTapAction(action: FabAction) {
        sharedPreferenceManager.fabDoubleTapAction = action.name
        _fabDoubleTapAction.value = action
    }

    fun setFabLongPressAction(action: FabAction) {
        sharedPreferenceManager.fabLongPressAction = action.name
        _fabLongPressAction.value = action
    }

    fun setFabSingleTapValue(value: String) {
        sharedPreferenceManager.fabSingleTapValue = value
        _fabSingleTapValue.value = value
    }

    fun setFabDoubleTapValue(value: String) {
        sharedPreferenceManager.fabDoubleTapValue = value
        _fabDoubleTapValue.value = value
    }

    fun setFabLongPressValue(value: String) {
        sharedPreferenceManager.fabLongPressValue = value
        _fabLongPressValue.value = value
    }

    fun setDrawerIconShape(shape: IconShape) {
        sharedPreferenceManager.drawerIconShape = shape.name
        _drawerIconShape.value = shape
    }

    fun setDrawerIconShadow(enabled: Boolean) {
        sharedPreferenceManager.drawerIconShadow = enabled
        _drawerIconShadow.value = enabled
    }

    fun setGlobalIconPack(packageName: String?) {
        sharedPreferenceManager.globalIconPack = packageName
        _globalIconPack.value = packageName
        _showGlobalIconPackDialog.value = false
    }

    fun setShowGlobalIconPackDialog(show: Boolean) {
        _showGlobalIconPackDialog.value = show
    }

    fun getInstalledIconPacks(): List<ResolveInfo> {
        val pm = getApplication<Application>().packageManager
        val intent = Intent("org.adw.launcher.THEMES")
        val adw = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
        val intent2 = Intent("com.gau.go.launcherex.theme")
        val go = pm.queryIntentActivities(intent2, PackageManager.GET_META_DATA)
        val intent3 = Intent("com.fede.launcher.THEME_ICONPACK")
        val launcherPro = pm.queryIntentActivities(intent3, PackageManager.GET_META_DATA)
        
        return (adw + go + launcherPro).distinctBy { it.activityInfo.packageName }
    }

    fun setVisibleCalendars(calendars: List<String>) {
        sharedPreferenceManager.visibleCalendars = calendars
        _visibleCalendars.value = calendars
    }

    fun toggleCalendarVisibility(calendarId: String) {
        val current = _visibleCalendars.value.toMutableList()
        if (current.isEmpty()) {
            // "Select all" mode. To toggle one off, we need to list all others.
            // But usually this means start with empty (all) and add specific IDs.
            current.add(calendarId)
        } else if (current.contains("__NONE__")) {
            current.clear()
            current.add(calendarId)
        } else {
            if (current.contains(calendarId)) {
                current.remove(calendarId)
                if (current.isEmpty()) current.add("__NONE__")
            } else {
                current.add(calendarId)
            }
        }
        setVisibleCalendars(current)
    }

    fun setShowCalendarSelectionDialog(show: Boolean) {
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
        if (current.contains(packageName)) {
            current.remove(packageName)
            if (current.isEmpty()) current.add("__NONE__")
        } else {
            if (current.contains("__NONE__")) current.remove("__NONE__")
            current.add(packageName)
        }
        setVisibleNotificationApps(current)
    }

    fun loadAvailableCalendars() {
        viewModelScope.launch(Dispatchers.IO) {
            val calendars = mutableListOf<CalendarInfo>()
            val contentResolver = getApplication<Application>().contentResolver
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
                contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    while (cursor.moveToNext()) {
                        calendars.add(
                            CalendarInfo(
                                id = cursor.getString(0),
                                name = cursor.getString(1),
                                color = cursor.getInt(2),
                                accountName = cursor.getString(3),
                                syncEvents = cursor.getInt(4) != 0,
                                visible = cursor.getInt(5) != 0,
                                accountType = cursor.getString(6)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Failed to query calendars", e)
            }
            _availableCalendars.value = calendars.sortedBy { it.name.lowercase() }
        }
    }

    fun onThemeOptionSelectedInDialog(index: Int) {
        _dialogPreviewThemeIndex.value = index
    }

    fun applySelectedTheme() {
        val index = _dialogPreviewThemeIndex.value
        sharedPreferenceManager.theme = index
        _persistedThemeIndexFlow.value = index
        _showThemeDialog.value = false
    }

    fun dismissThemeDialog() {
        _showThemeDialog.value = false
        // Reset preview index for next time
        _dialogPreviewThemeIndex.value = sharedPreferenceManager.theme
    }

    fun onCoverThemeClicked() {
        _showCoverSelectionDialog.value = true
    }

    fun dismissCoverThemeDialog() {
        _showCoverSelectionDialog.value = false
    }

    fun saveCoverDisplayMetrics(size: IntSize) {
        sharedPreferenceManager.coverDisplaySize = size
        sharedPreferenceManager.coverThemeEnabled = true
        _enableCoverTheme.value = true
        _showCoverSelectionDialog.value = false
    }

    fun applyCoverTheme(size: IntSize): Boolean {
        return sharedPreferenceManager.isCoverThemeApplied(size)
    }

    fun onLanguageSettingClicked(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as LocaleManager
            val currentLocales = localeManager.applicationLocales
            _selectedLanguageTagInDialog.value = if (currentLocales.isEmpty) "" else currentLocales.toLanguageTags()
        } else {
            _selectedLanguageTagInDialog.value = sharedPreferenceManager.languageTag
        }
        _showLanguageDialog.value = true
    }

    fun onLanguageSelectedInDialog(tag: String) {
        _selectedLanguageTagInDialog.value = tag
    }

    fun applySelectedLanguage() {
        val tag = _selectedLanguageTagInDialog.value
        sharedPreferenceManager.languageTag = tag
        _showLanguageDialog.value = false
        // The activity will recreate itself and apply the new locale in attachBaseContext
    }

    fun dismissLanguageDialog() {
        _showLanguageDialog.value = false
    }

    fun updateCurrentLanguage() {
        _currentLanguage.value = getCurrentLocaleDisplayName()
    }

    fun getCurrentLocaleDisplayName(): String {
        val tag = sharedPreferenceManager.languageTag
        return if (tag.isEmpty()) {
            getApplication<Application>().getString(R.string.system_default)
        } else {
            Locale.forLanguageTag(tag).getDisplayName(Locale.forLanguageTag(tag))
                .replaceFirstChar { it.uppercase() }
        }
    }

    fun getAppLocaleTag(): String {
        return sharedPreferenceManager.languageTag
    }

    fun prepareLanguageOptions() {
        viewModelScope.launch(Dispatchers.IO) {
            val options = listOf(
                LanguageOption("", getApplication<Application>().getString(R.string.system_default)),
                LanguageOption("en", "English"),
                LanguageOption("de", "Deutsch"),
            )
            _availableLanguages.value = options
        }
    }

    fun onClearDataClicked() {
        _showClearDataDialog.value = true
    }

    fun confirmClearData() {
        val context = getApplication<Application>()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.clearApplicationUserData()
        _showClearDataDialog.value = false
    }

    fun dismissClearDataDialog() {
        _showClearDataDialog.value = false
    }

    fun onResetSettingsClicked() {
        _showResetSettingsDialog.value = true
    }

    fun confirmResetSettings() {
        sharedPreferenceManager.clearSettings()
        _showResetSettingsDialog.value = false
        // Re-initialize StateFlows with default values
        _blackedOutModeEnabled.value = false
        _blurEnabled.value = true
        _isGridLayout.value = true
        _openKeyboard.value = false
        _advancedSearchEnabled.value = true
        _appLabelsEnabled.value = true
        _drawerIconShape.value = IconShape.Circle
        _drawerIconShadow.value = false
        // Re-start app to apply all resets
        restartApplication(getApplication())
    }

    fun dismissResetSettingsDialog() {
        _showResetSettingsDialog.value = false
    }

    fun onInfoTileClicked() {
        infoTileTapCount++
        if (infoTileTapCount >= requiredTaps) {
            val now = System.currentTimeMillis()
            if (now - lastMultiTapTime < multiTapCooldownMillis) {
                // Cooldown to prevent spamming
                return
            }
            lastMultiTapTime = now

            if (sharedPreferenceManager.developerModeEnabled) {
                currentToast?.cancel()
                currentToast = Toast.makeText(getApplication(), "Developer mode is already enabled", Toast.LENGTH_SHORT)
                currentToast?.show()
            } else {
                sharedPreferenceManager.developerModeEnabled = true
                _developerModeEnabled.value = true
                currentToast?.cancel()
                currentToast = Toast.makeText(getApplication(), "You are now a developer!", Toast.LENGTH_SHORT)
                currentToast?.show()
            }
            infoTileTapCount = 0
            resetTapsJob?.cancel()
        } else {
            resetTapsJob?.cancel()
            resetTapsJob = viewModelScope.launch {
                delay(3000)
                infoTileTapCount = 0
            }

            if (infoTileTapCount > 3) {
                val remaining = requiredTaps - infoTileTapCount
                currentToast?.cancel()
                currentToast = Toast.makeText(getApplication(), "You are now $remaining steps away from being a developer", Toast.LENGTH_SHORT)
                currentToast?.show()
            }
        }

        // Normal single tap action
        singleTapJob?.cancel()
        singleTapJob = viewModelScope.launch {
            delay(tapTimeoutMillis)
            _showVersionDialog.value = true
        }
    }

    fun dismissVersionDialog() {
        _showVersionDialog.value = false
    }

    fun openImpressum(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://xenonware.com/impressum"))
        context.startActivity(intent)
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

    fun setFabAction(isDoubleTap: Boolean?, action: FabAction, value: String = "") {
        when (isDoubleTap) {
            true -> {
                sharedPreferenceManager.fabDoubleTapAction = action.name
                sharedPreferenceManager.fabDoubleTapValue = value
                _fabDoubleTapAction.value = action
                _fabDoubleTapValue.value = value
            }
            false -> {
                sharedPreferenceManager.fabLongPressAction = action.name
                sharedPreferenceManager.fabLongPressValue = value
                _fabLongPressAction.value = action
                _fabLongPressValue.value = value
            }
            null -> {
                sharedPreferenceManager.fabSingleTapAction = action.name
                sharedPreferenceManager.fabSingleTapValue = value
                _fabSingleTapAction.value = action
                _fabSingleTapValue.value = value
            }
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
        _configShortcutType.value = null
    }

    fun setShowHiddenApps(show: Boolean) {
        _showHiddenAppsDialog.value = show
    }

    fun setShowBackupDialog(show: Boolean) {
        _showBackupDialog.value = show
        if (show) loadBackups()
    }

    fun loadBackups() {
        viewModelScope.launch {
            _isSyncingBackups.value = true
            val backups = mutableListOf<BackupInfo>()
            val user = auth.currentUser
            if (user != null) {
                try {
                    val snapshot = firestore.collection("users")
                        .document(user.uid)
                        .collection("backups")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .get()
                        .await()
                    
                    snapshot.documents.forEach { doc ->
                        backups.add(BackupInfo(
                            id = doc.id,
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            date = doc.getString("date") ?: "",
                            time = doc.getString("time") ?: "",
                            device = doc.getString("device") ?: "Unknown"
                        ))
                    }
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Failed to load backups", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Failed to load backups: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            _backups.value = backups
            _isSyncingBackups.value = false
        }
    }

    fun startBackup() {
        viewModelScope.launch {
            _isSyncingBackups.value = true
            val user = auth.currentUser
            if (user != null) {
                try {
                    val data = JSONObject()
                    sharedPreferenceManager.getAllPreferences().forEach { (key, value) ->
                        when (value) {
                            is Set<*> -> {
                                val array = JSONArray()
                                value.forEach { array.put(it) }
                                data.put(key, array)
                            }
                            else -> data.put(key, value)
                        }
                    }

                    val now = Date()
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    
                    val backup = hashMapOf<String, Any>(
                        "timestamp" to now.time,
                        "date" to dateFormat.format(now),
                        "time" to timeFormat.format(now),
                        "device" to Build.MODEL,
                        "data" to data.toString()
                    )

                    Log.d("SettingsViewModel", "Attempting backup for user: ${user.uid}")
                    
                    firestore.collection("users")
                        .document(user.uid)
                        .collection("backups")
                        .add(backup)
                        .await()
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Backup created successfully", Toast.LENGTH_SHORT).show()
                    }
                    loadBackups()
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Backup failed for UID: ${user.uid}", e)
                    withContext(Dispatchers.Main) {
                        if (e.message?.contains("permission") == true) {
                            Toast.makeText(getApplication(), "Cloud error: Insufficient permissions. Please sign out and sign in again.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(getApplication(), "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Please sign in first", Toast.LENGTH_SHORT).show()
                }
            }
            _isSyncingBackups.value = false
        }
    }

    fun restoreBackup(backupInfo: BackupInfo) {
        viewModelScope.launch {
            _isSyncingBackups.value = true
            val user = auth.currentUser
            if (user != null) {
                try {
                    val doc = firestore.collection("users")
                        .document(user.uid)
                        .collection("backups")
                        .document(backupInfo.id)
                        .get()
                        .await()
                    
                    val dataJson = doc.getString("data")
                    if (dataJson != null) {
                        val json = JSONObject(dataJson)
                        val map = mutableMapOf<String, Any>()
                        json.keys().forEach { key ->
                            val value = json.get(key)
                            if (value is JSONArray) {
                                val set = mutableSetOf<String>()
                                for (i in 0 until value.length()) {
                                    set.add(value.getString(i))
                                }
                                map[key] = set
                            } else {
                                map[key] = value
                            }
                        }
                        sharedPreferenceManager.importPreferences(map)
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(getApplication(), "Settings restored. Restarting...", Toast.LENGTH_SHORT).show()
                        }
                        delay(1000)
                        restartApplication(getApplication())
                    }
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Restore failed", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            _isSyncingBackups.value = false
        }
    }

    fun deleteBackup(backupInfo: BackupInfo) {
        viewModelScope.launch {
            _isSyncingBackups.value = true
            val user = auth.currentUser
            if (user != null) {
                try {
                    firestore.collection("users")
                        .document(user.uid)
                        .collection("backups")
                        .document(backupInfo.id)
                        .delete()
                        .await()
                    loadBackups()
                } catch (e: Exception) {
                    Log.e("SettingsViewModel", "Delete failed", e)
                }
            }
            _isSyncingBackups.value = false
        }
    }

    fun setShowFabConfig(mode: FabConfigMode) {
        _showFabConfigMode.value = mode
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

    override fun onCleared() {
        super.onCleared()
        sharedPreferenceManager.unregisterListener(preferenceListener)
    }
}
