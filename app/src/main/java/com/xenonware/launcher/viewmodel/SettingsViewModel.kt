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
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.CalendarContract
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.xenon.mylibrary.res.ThemeSetting
import com.xenonware.launcher.R
import com.xenonware.launcher.accessibility.XenonAccessibilityService
import com.xenonware.launcher.data.SharedPreferenceManager
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.model.AppMenuItem
import com.xenonware.launcher.model.AppWidgetGroup
import com.xenonware.launcher.model.FabAction
import com.xenonware.launcher.model.WidgetPickerItemData
import com.xenonware.launcher.ui.res.IconShape
import com.xenonware.launcher.util.AccessibilityUtils
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

enum class FabConfigMode { NONE, SINGLE, DOUBLE, LONG, SWIPE_UP }

data class BackupInfo(
    val id: String,
    val timestamp: Long,
    val date: String,
    val time: String,
    val device: String,
    val data: String? = null,
)

data class PermissionStatus(
    val name: String,
    val isGranted: Boolean,
    val permission: String,
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

    private val _permissionsList = MutableStateFlow<List<PermissionStatus>>(emptyList())
    val permissionsList: StateFlow<List<PermissionStatus>> = _permissionsList.asStateFlow()

    private val _showPermissionsDialog = MutableStateFlow(false)
    val showPermissionsDialog: StateFlow<Boolean> = _showPermissionsDialog.asStateFlow()

    private val _installedShortcuts = MutableStateFlow<Map<AppWidgetGroup, List<WidgetPickerItemData>>>(emptyMap())
    val installedShortcuts: StateFlow<Map<AppWidgetGroup, List<WidgetPickerItemData>>> = _installedShortcuts.asStateFlow()

    fun setShowPermissionsDialog(show: Boolean) {
        _showPermissionsDialog.value = show
        if (show) {
            refreshPermissions()
        }
    }

    fun refreshPermissions() {
        val context = getApplication<Application>()
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()

        val newList = requestedPermissions.mapNotNull { permission ->
            try {
                val pInfo = packageManager.getPermissionInfo(permission, 0)
                // Filter for runtime permissions or common special ones
                val isRuntime = pInfo.protection == android.content.pm.PermissionInfo.PROTECTION_DANGEROUS
                val isSpecial = permission == Manifest.permission.MANAGE_EXTERNAL_STORAGE ||
                                permission == Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE ||
                                permission == Manifest.permission.BIND_ACCESSIBILITY_SERVICE

                if (isRuntime || isSpecial) {
                    val label = when (permission) {
                        Manifest.permission.READ_CONTACTS -> context.getString(R.string.contacts_access)
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> context.getString(R.string.location_access)
                        Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR -> context.getString(R.string.calendar_access)
                        Manifest.permission.POST_NOTIFICATIONS -> context.getString(R.string.post_notifications)
                        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_AUDIO -> context.getString(R.string.storage_access)
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE -> context.getString(R.string.all_files_access)
                        Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE -> context.getString(R.string.notification_access)
                        Manifest.permission.BIND_ACCESSIBILITY_SERVICE -> context.getString(R.string.accessibility_access)
                        else -> pInfo.loadLabel(packageManager).toString()
                    }

                    val isGranted = when (permission) {
                        Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                            Environment.isExternalStorageManager()
                        }
                        Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE -> {
                            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                            !TextUtils.isEmpty(flat) && flat.contains(context.packageName)
                        }
                        Manifest.permission.BIND_ACCESSIBILITY_SERVICE -> {
                            XenonAccessibilityService.instance != null
                        }
                        else -> context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
                    }

                    PermissionStatus(
                        name = label,
                        isGranted = isGranted,
                        permission = permission
                    )
                } else null
            } catch (_: Exception) {
                null
            }
        }.distinctBy { it.name }.sortedBy { it.name }

        _permissionsList.value = newList
    }

    fun openPermissionSettings(context: Context, permission: String) {
        val intent = when (permission) {
            Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
            Manifest.permission.POST_NOTIFICATIONS -> {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            }
            Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE -> {
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            }
            Manifest.permission.BIND_ACCESSIBILITY_SERVICE -> {
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            }
            else -> {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

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

    private val _showThemeDialog = MutableStateFlow(false)
    val showThemeDialog: StateFlow<Boolean> = _showThemeDialog.asStateFlow()

    private val _showClearDataDialog = MutableStateFlow(false)
    val showClearDataDialog: StateFlow<Boolean> = _showClearDataDialog.asStateFlow()

    private val _showResetSettingsDialog = MutableStateFlow(false)
    val showResetSettingsDialog: StateFlow<Boolean> = _showResetSettingsDialog.asStateFlow()

    private val _showCoverSelectionDialog = MutableStateFlow(false)
    val showCoverSelectionDialog: StateFlow<Boolean> = _showCoverSelectionDialog.asStateFlow()

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

    private val _notificationDeleteSinglePress = MutableStateFlow(sharedPreferenceManager.notificationDeleteSinglePress)
    val notificationDeleteSinglePress: StateFlow<Boolean> = _notificationDeleteSinglePress.asStateFlow()

    private val _notificationImageSizeFactor = MutableStateFlow(sharedPreferenceManager.notificationImageSizeFactor)
    val notificationImageSizeFactor: StateFlow<Float> = _notificationImageSizeFactor.asStateFlow()

    private val _notificationIndicatorType = MutableStateFlow(sharedPreferenceManager.notificationIndicatorType)
    val notificationIndicatorType: StateFlow<Int> = _notificationIndicatorType.asStateFlow()

    private val _notificationMessageType = MutableStateFlow(sharedPreferenceManager.notificationMessageType)
    val notificationMessageType: StateFlow<Int> = _notificationMessageType.asStateFlow()

    private val _tempUnit = MutableStateFlow(sharedPreferenceManager.tempUnit)
    val tempUnit: StateFlow<Int> = _tempUnit.asStateFlow()

    private val _fontType = MutableStateFlow(sharedPreferenceManager.fontType)
    val fontType: StateFlow<Int> = _fontType.asStateFlow()

    private val _mainFontType = MutableStateFlow(sharedPreferenceManager.mainFontType)
    val mainFontType: StateFlow<Int> = _mainFontType.asStateFlow()

    private val _robotoFlexSettings = MutableStateFlow(sharedPreferenceManager.robotoFlexSettings)
    val robotoFlexSettings: StateFlow<String> = _robotoFlexSettings.asStateFlow()

    private val _googleSansFlexSettings = MutableStateFlow(sharedPreferenceManager.googleSansFlexSettings)
    val googleSansFlexSettings: StateFlow<String> = _googleSansFlexSettings.asStateFlow()

    private val _appMenuOrder = MutableStateFlow(sharedPreferenceManager.appMenuOrder)
    val appMenuOrder: StateFlow<List<String>> = _appMenuOrder.asStateFlow()

    fun setAppMenuOrder(order: List<String>) {
        sharedPreferenceManager.appMenuOrder = order
        _appMenuOrder.value = order
    }

    fun resetAppMenuOrder() {
        val default = AppMenuItem.DEFAULT_ORDER.map { it.id }
        setAppMenuOrder(default)
    }

    private val _showFontConfigDialog = MutableStateFlow(false)
    val showFontConfigDialog: StateFlow<Boolean> = _showFontConfigDialog.asStateFlow()

    fun setShowFontConfigDialog(show: Boolean) {
        _showFontConfigDialog.value = show
    }

    private val _showAppMenuOrderDialog = MutableStateFlow(false)
    val showAppMenuOrderDialog: StateFlow<Boolean> = _showAppMenuOrderDialog.asStateFlow()

    fun setShowAppMenuOrderDialog(show: Boolean) {
        _showAppMenuOrderDialog.value = show
    }

    private val _showVisualizerConfigDialog = MutableStateFlow(false)
    val showVisualizerConfigDialog: StateFlow<Boolean> = _showVisualizerConfigDialog.asStateFlow()

    fun setShowVisualizerConfigDialog(show: Boolean) {
        _showVisualizerConfigDialog.value = show
    }

    private val _persistedThemeIndexFlow = MutableStateFlow(sharedPreferenceManager.theme)

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

    private val _fabSwipeUpAction = MutableStateFlow(FabAction.fromString(sharedPreferenceManager.fabSwipeUpAction))
    val fabSwipeUpAction: StateFlow<FabAction> = _fabSwipeUpAction.asStateFlow()

    private val _fabSwipeUpValue = MutableStateFlow(sharedPreferenceManager.fabSwipeUpValue)
    val fabSwipeUpValue: StateFlow<String> = _fabSwipeUpValue.asStateFlow()

    private val _dialogPreviewThemeIndex = MutableStateFlow(sharedPreferenceManager.theme)
    val dialogPreviewThemeIndex: StateFlow<Int> = _dialogPreviewThemeIndex.asStateFlow()

    val currentThemeIndex: StateFlow<Int> = combine(
        _persistedThemeIndexFlow,
        _dialogPreviewThemeIndex,
        _showThemeDialog
    ) { persistedIndex, previewIndex, isDialogShowing ->
        if (isDialogShowing) previewIndex else persistedIndex
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = sharedPreferenceManager.theme
    )

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
            "fab_swipe_up_action" -> _fabSwipeUpAction.value = FabAction.fromString(sharedPreferenceManager.fabSwipeUpAction)
            "fab_swipe_up_value" -> _fabSwipeUpValue.value = sharedPreferenceManager.fabSwipeUpValue
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
            "notification_delete_single_press" -> _notificationDeleteSinglePress.value = sharedPreferenceManager.notificationDeleteSinglePress
            "notification_image_size_factor" -> _notificationImageSizeFactor.value = sharedPreferenceManager.notificationImageSizeFactor
            "notification_indicator_type" -> _notificationIndicatorType.value = sharedPreferenceManager.notificationIndicatorType
            "notification_message_type" -> _notificationMessageType.value = sharedPreferenceManager.notificationMessageType
            "temp_unit" -> _tempUnit.value = sharedPreferenceManager.tempUnit
            "font_type" -> _fontType.value = sharedPreferenceManager.fontType
            "main_font_type" -> _mainFontType.value = sharedPreferenceManager.mainFontType
            "roboto_flex_settings" -> _robotoFlexSettings.value = sharedPreferenceManager.robotoFlexSettings
            "google_sans_flex_settings" -> _googleSansFlexSettings.value = sharedPreferenceManager.googleSansFlexSettings
            "app_menu_order" -> _appMenuOrder.value = sharedPreferenceManager.appMenuOrder
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
        loadApps()
        loadInstalledShortcuts()
    }

    private fun loadInstalledShortcuts() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val shortcutIntent = Intent(Intent.ACTION_CREATE_SHORTCUT)
            val shortcuts = pm.queryIntentActivities(shortcutIntent, 0)

            val allPackages = shortcuts.map { it.activityInfo.packageName }.toSet()

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

                val shortcutItems = shortcuts
                    .filter { it.activityInfo.packageName == pkg }
                    .map {
                        WidgetPickerItemData(
                            label = it.loadLabel(pm).toString(),
                            isWidget = false,
                            shortcutInfo = it
                        )
                    }

                AppWidgetGroup(appName, icon) to shortcutItems.sortedBy { it.label }
            }.filter { it.second.isNotEmpty() }.toMap().toSortedMap()

            _installedShortcuts.value = grouped
        }
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val resolvedApps = pm.queryIntentActivities(mainIntent, 0)
            val appList = resolvedApps.map { resolveInfo ->
                AppInfo(
                    name = resolveInfo.activityInfo.loadLabel(pm).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = resolveInfo.loadIcon(pm),
                    className = resolveInfo.activityInfo.name
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

    fun setNotificationDeleteSinglePress(enabled: Boolean) {
        sharedPreferenceManager.notificationDeleteSinglePress = enabled
        _notificationDeleteSinglePress.value = enabled
    }

    fun setNotificationImageSizeFactor(factor: Float) {
        sharedPreferenceManager.notificationImageSizeFactor = factor
        _notificationImageSizeFactor.value = factor
    }

    fun setNotificationIndicatorType(type: Int) {
        sharedPreferenceManager.notificationIndicatorType = type
        _notificationIndicatorType.value = type
    }

    fun setNotificationMessageType(type: Int) {
        sharedPreferenceManager.notificationMessageType = type
        _notificationMessageType.value = type
    }

    fun setTempUnit(unit: Int) {
        sharedPreferenceManager.tempUnit = unit
        _tempUnit.value = unit
    }

    fun setFontType(type: Int) {
        sharedPreferenceManager.fontType = type
        _fontType.value = type
    }

    fun setMainFontType(type: Int) {
        sharedPreferenceManager.mainFontType = type
        _mainFontType.value = type
    }

    fun setRobotoFlexSettings(settingsJson: String) {
        sharedPreferenceManager.robotoFlexSettings = settingsJson
        _robotoFlexSettings.value = settingsJson
    }

    fun setGoogleSansFlexSettings(settingsJson: String) {
        sharedPreferenceManager.googleSansFlexSettings = settingsJson
        _googleSansFlexSettings.value = settingsJson
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
            val context = getApplication<Application>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                _availableCalendars.value = emptyList()
                return@launch
            }

            val calendars = mutableListOf<CalendarInfo>()
            val contentResolver = context.contentResolver
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
        val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun updateCurrentLanguage() {
        _currentLanguage.value = getCurrentLocaleDisplayName()
    }

    fun getCurrentLocaleDisplayName(): String {
        val localeManager = getApplication<Application>().getSystemService(Context.LOCALE_SERVICE) as LocaleManager
        val locales = localeManager.applicationLocales
        return if (locales.isEmpty) {
            getApplication<Application>().getString(R.string.system_default)
        } else {
            val locale = locales.get(0)!!
            locale.getDisplayName(locale).replaceFirstChar { it.uppercase() }
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
        _appMenuOrder.value = AppMenuItem.DEFAULT_ORDER.map { it.id }
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
                delay(3000.milliseconds)
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
            delay(tapTimeoutMillis.milliseconds)
            _showVersionDialog.value = true
        }
    }

    fun dismissVersionDialog() {
        _showVersionDialog.value = false
    }

    fun openImpressum(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, "https://xenonware.com/impressum".toUri())
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

    fun setFabAction(configMode: FabConfigMode, action: FabAction, value: String = "") {
        when (configMode) {
            FabConfigMode.DOUBLE -> {
                sharedPreferenceManager.fabDoubleTapAction = action.name
                sharedPreferenceManager.fabDoubleTapValue = value
                _fabDoubleTapAction.value = action
                _fabDoubleTapValue.value = value
            }
            FabConfigMode.LONG -> {
                sharedPreferenceManager.fabLongPressAction = action.name
                sharedPreferenceManager.fabLongPressValue = value
                _fabLongPressAction.value = action
                _fabLongPressValue.value = value
            }
            FabConfigMode.SINGLE -> {
                sharedPreferenceManager.fabSingleTapAction = action.name
                sharedPreferenceManager.fabSingleTapValue = value
                _fabSingleTapAction.value = action
                _fabSingleTapValue.value = value
            }
            FabConfigMode.SWIPE_UP -> {
                sharedPreferenceManager.fabSwipeUpAction = action.name
                sharedPreferenceManager.fabSwipeUpValue = value
                _fabSwipeUpAction.value = action
                _fabSwipeUpValue.value = value
            }
            FabConfigMode.NONE -> {}
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
                        delay(1000.milliseconds)
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
        sharedPreferenceManager.unregisterListener(preferenceListener)
    }
}
