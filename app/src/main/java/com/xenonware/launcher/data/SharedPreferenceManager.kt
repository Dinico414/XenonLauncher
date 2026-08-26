package com.xenonware.launcher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.xenonware.launcher.model.AppOverride
import org.json.JSONObject

class SharedPreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

    val themeFlag = arrayOf(
        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO,
        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES,
        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    )

    var theme: Int
        get() = prefs.getInt("theme", 2) // 2: System
        set(value) = prefs.edit { putInt("theme", value) }

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean("is_first_launch", true)
        set(value) = prefs.edit { putBoolean("is_first_launch", value) }

    var blurEnabled: Boolean
        get() = prefs.getBoolean("blur_enabled", true)
        set(value) = prefs.edit { putBoolean("blur_enabled", value) }

    var pinnedApps: List<String>
        get() = prefs.getString("pinned_apps", "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        set(value) = prefs.edit { putString("pinned_apps", value.joinToString(",")) }

    var isGridLayout: Boolean
        get() = prefs.getBoolean("is_grid_layout", true)
        set(value) = prefs.edit { putBoolean("is_grid_layout", value) }

    var openKeyboard: Boolean
        get() = prefs.getBoolean("open_keyboard", false)
        set(value) = prefs.edit { putBoolean("open_keyboard", value) }

    var openKeyboardPortraitOnly: Boolean
        get() = prefs.getBoolean("open_keyboard_portrait_only", false)
        set(value) = prefs.edit { putBoolean("open_keyboard_portrait_only", value) }

    var appUsage: String
        get() = prefs.getString("app_usage", "") ?: ""
        set(value) = prefs.edit { putString("app_usage", value) }

    var widgetColumnsPortrait: Int
        get() = prefs.getInt("widget_columns_portrait", 4)
        set(value) = prefs.edit { putInt("widget_columns_portrait", value) }

    var widgetColumnsLandscape: Int
        get() = prefs.getInt("widget_columns_landscape", 6)
        set(value) = prefs.edit { putInt("widget_columns_landscape", value) }

    var widgetLayoutPortrait: String
        get() = prefs.getString("widget_layout_portrait", "") ?: ""
        set(value) = prefs.edit { putString("widget_layout_portrait", value) }

    var widgetLayoutLandscape: String
        get() = prefs.getString("widget_layout_landscape", "") ?: ""
        set(value) = prefs.edit { putString("widget_layout_landscape", value) }

    var advancedSearchEnabled: Boolean
        get() = prefs.getBoolean("advanced_search_enabled", true)
        set(value) = prefs.edit { putBoolean("advanced_search_enabled", value) }

    var notificationBadgeType: Int
        get() = prefs.getInt("notification_badge_type", 1) // 0: None, 1: Dot, 2: Number
        set(value) = prefs.edit { putInt("notification_badge_type", value) }

    var searchHistory: String
        get() = prefs.getString("search_history", "") ?: ""
        set(value) = prefs.edit { putString("search_history", value) }

    var dockSafeDrawIme: Boolean
        get() = prefs.getBoolean("dock_safedraw_ime", false)
        set(value) = prefs.edit { putBoolean("dock_safedraw_ime", value) }

    var dockSafeDrawImePortraitOnly: Boolean
        get() = prefs.getBoolean("dock_safedraw_ime_portrait_only", false)
        set(value) = prefs.edit { putBoolean("dock_safedraw_ime_portrait_only", value) }

    var timeShortcut: String
        get() = prefs.getString("time_shortcut", "") ?: ""
        set(value) = prefs.edit { putString("time_shortcut", value) }

    var dateShortcut: String
        get() = prefs.getString("date_shortcut", "") ?: ""
        set(value) = prefs.edit { putString("date_shortcut", value) }

    var weatherShortcut: String
        get() = prefs.getString("weather_shortcut", "") ?: ""
        set(value) = prefs.edit { putString("weather_shortcut", value) }

    var hiddenApps: List<String>
        get() = prefs.getString("hidden_apps", "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        set(value) = prefs.edit { putString("hidden_apps", value.joinToString(",")) }

    var visibleCalendars: List<String>
        get() = prefs.getString("visible_calendars", "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        set(value) = prefs.edit { putString("visible_calendars", value.joinToString(",")) }

    var visibleNotificationApps: List<String>
        get() = prefs.getString("visible_notification_apps", "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        set(value) = prefs.edit { putString("visible_notification_apps", value.joinToString(",")) }

    var showHiddenAppsInSearch: Boolean
        get() = prefs.getBoolean("show_hidden_apps_in_search", false)
        set(value) = prefs.edit { putBoolean("show_hidden_apps_in_search", value) }

    var drawerIconShape: String
        get() = prefs.getString("drawer_icon_shape", "Circle") ?: "Circle"
        set(value) = prefs.edit { putString("drawer_icon_shape", value) }

    var drawerIconShadow: Boolean
        get() = prefs.getBoolean("drawer_icon_shadow", false)
        set(value) = prefs.edit { putBoolean("drawer_icon_shadow", value) }

    var blackedOutModeEnabled: Boolean
        get() = prefs.getBoolean("blacked_out_mode_enabled", false)
        set(value) = prefs.edit { putBoolean("blacked_out_mode_enabled", value) }

    var developerModeEnabled: Boolean
        get() = prefs.getBoolean("developer_mode_enabled", false)
        set(value) = prefs.edit { putBoolean("developer_mode_enabled", value) }

    var appLabelsEnabled: Boolean
        get() = prefs.getBoolean("app_labels_enabled", true)
        set(value) = prefs.edit { putBoolean("app_labels_enabled", value) }

    var isUserLoggedIn: Boolean
        get() = prefs.getBoolean("is_user_logged_in", false)
        set(value) = prefs.edit { putBoolean("is_user_logged_in", value) }

    var coverThemeEnabled: Boolean
        get() = prefs.getBoolean("cover_theme_enabled", false)
        set(value) = prefs.edit { putBoolean("cover_theme_enabled", value) }

    var coverDisplaySize: androidx.compose.ui.unit.IntSize
        get() {
            val dim1 = prefs.getInt("cover_display_dimension_1", 0)
            val dim2 = prefs.getInt("cover_display_dimension_2", 0)
            return androidx.compose.ui.unit.IntSize(dim1, dim2)
        }
        set(value) {
            prefs.edit {
                putInt("cover_display_dimension_1", kotlin.math.min(value.width, value.height))
                putInt("cover_display_dimension_2", kotlin.math.max(value.width, value.height))
            }
        }

    fun isCoverThemeApplied(currentDisplaySize: androidx.compose.ui.unit.IntSize): Boolean {
        if (!coverThemeEnabled) return false
        val storedDimension1 = prefs.getInt("cover_display_dimension_1", 0)
        val storedDimension2 = prefs.getInt("cover_display_dimension_2", 0)
        if (storedDimension1 == 0 || storedDimension2 == 0) return false
        val currentMatchesStoredOrder =
            (currentDisplaySize.width == storedDimension1 && currentDisplaySize.height == storedDimension2)
        val currentMatchesSwappedOrder =
            (currentDisplaySize.width == storedDimension2 && currentDisplaySize.height == storedDimension1)

        return currentMatchesStoredOrder || currentMatchesSwappedOrder
    }

    fun getAppOverrides(): Map<String, AppOverride> {
        val jsonStr = prefs.getString("app_overrides", "{}") ?: "{}"
        val json = JSONObject(jsonStr)
        val map = mutableMapOf<String, AppOverride>()
        json.keys().forEach { pkg ->
            val obj = json.getJSONObject(pkg)
            map[pkg] = AppOverride(
                customName = if (obj.has("name")) obj.getString("name") else null,
                iconPackPackage = if (obj.has("iconPack")) obj.getString("iconPack") else null,
                iconResourceName = if (obj.has("iconRes")) obj.getString("iconRes") else null,
                zoom = if (obj.has("zoom")) obj.getDouble("zoom").toFloat() else 1.0f,
                backgroundColor = if (obj.has("bgColor")) obj.getInt("bgColor") else null,
                borderColor = if (obj.has("borderColor")) obj.getInt("borderColor") else null,
                borderWidth = if (obj.has("borderWidth")) obj.getDouble("borderWidth").toFloat() else 0f
            )
        }
        return map
    }

    fun saveAppOverride(packageName: String, override: AppOverride) {
        val overrides = getAppOverrides().toMutableMap()
        overrides[packageName] = override
        saveAllOverrides(overrides)
    }

    fun resetAppOverride(packageName: String) {
        val overrides = getAppOverrides().toMutableMap()
        overrides.remove(packageName)
        saveAllOverrides(overrides)
    }

    private fun saveAllOverrides(overrides: Map<String, AppOverride>) {
        val json = JSONObject()
        overrides.forEach { (pkg, override) ->
            val obj = JSONObject()
            override.customName?.let { obj.put("name", it) }
            override.iconPackPackage?.let { obj.put("iconPack", it) }
            override.iconResourceName?.let { obj.put("iconRes", it) }
            obj.put("zoom", override.zoom.toDouble())
            override.backgroundColor?.let { obj.put("bgColor", it) }
            override.borderColor?.let { obj.put("borderColor", it) }
            obj.put("borderWidth", override.borderWidth.toDouble())
            json.put(pkg, obj)
        }
        prefs.edit { putString("app_overrides", json.toString()) }
    }

    fun clearSettings() {
        prefs.edit {
            remove("theme")
            remove("blacked_out_mode_enabled")
            remove("blur_enabled")
            remove("is_grid_layout")
            remove("open_keyboard")
            remove("advanced_search_enabled")
            remove("notification_badge_type")
            remove("dock_safedraw_ime")
            remove("dock_safedraw_ime_portrait_only")
            remove("drawer_icon_shape")
            remove("drawer_icon_shadow")
            remove("time_shortcut")
            remove("date_shortcut")
            remove("weather_shortcut")
            remove("hidden_apps")
            remove("show_hidden_apps_in_search")
            remove("cover_theme_enabled")
            remove("developer_mode_enabled")
            remove("app_labels_enabled")
            remove("cover_display_dimension_1")
            remove("cover_display_dimension_2")
        }
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
