package com.xenonware.launcher.data

import android.content.Context
import android.content.SharedPreferences
import com.xenonware.launcher.model.AppOverride
import org.json.JSONObject

class SharedPreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)

    var theme: Int
        get() = prefs.getInt("theme", 2) // 2: System
        set(value) = prefs.edit().putInt("theme", value).apply()

    var blurEnabled: Boolean
        get() = prefs.getBoolean("blur_enabled", true)
        set(value) = prefs.edit().putBoolean("blur_enabled", value).apply()

    var pinnedApps: List<String>
        get() = prefs.getString("pinned_apps", "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        set(value) = prefs.edit().putString("pinned_apps", value.joinToString(",")).apply()

    var isGridLayout: Boolean
        get() = prefs.getBoolean("is_grid_layout", true)
        set(value) = prefs.edit().putBoolean("is_grid_layout", value).apply()

    var openKeyboard: Boolean
        get() = prefs.getBoolean("open_keyboard", false)
        set(value) = prefs.edit().putBoolean("open_keyboard", value).apply()

    var appUsage: String
        get() = prefs.getString("app_usage", "") ?: ""
        set(value) = prefs.edit().putString("app_usage", value).apply()

    var widgetColumnsPortrait: Int
        get() = prefs.getInt("widget_columns_portrait", 4)
        set(value) = prefs.edit().putInt("widget_columns_portrait", value).apply()

    var widgetColumnsLandscape: Int
        get() = prefs.getInt("widget_columns_landscape", 6)
        set(value) = prefs.edit().putInt("widget_columns_landscape", value).apply()

    var widgetLayoutPortrait: String
        get() = prefs.getString("widget_layout_portrait", "") ?: ""
        set(value) = prefs.edit().putString("widget_layout_portrait", value).apply()

    var widgetLayoutLandscape: String
        get() = prefs.getString("widget_layout_landscape", "") ?: ""
        set(value) = prefs.edit().putString("widget_layout_landscape", value).apply()

    var advancedSearchEnabled: Boolean
        get() = prefs.getBoolean("advanced_search_enabled", true)
        set(value) = prefs.edit().putBoolean("advanced_search_enabled", value).apply()

    var notificationBadgeType: Int
        get() = prefs.getInt("notification_badge_type", 1) // 0: None, 1: Dot, 2: Number
        set(value) = prefs.edit().putInt("notification_badge_type", value).apply()

    var searchHistory: String
        get() = prefs.getString("search_history", "") ?: ""
        set(value) = prefs.edit().putString("search_history", value).apply()

    var dockSafeDrawIme: Boolean
        get() = prefs.getBoolean("dock_safedraw_ime", false)
        set(value) = prefs.edit().putBoolean("dock_safedraw_ime", value).apply()

    var timeShortcut: String
        get() = prefs.getString("time_shortcut", "") ?: ""
        set(value) = prefs.edit().putString("time_shortcut", value).apply()

    var dateShortcut: String
        get() = prefs.getString("date_shortcut", "") ?: ""
        set(value) = prefs.edit().putString("date_shortcut", value).apply()

    var weatherShortcut: String
        get() = prefs.getString("weather_shortcut", "") ?: ""
        set(value) = prefs.edit().putString("weather_shortcut", value).apply()

    var hiddenApps: List<String>
        get() = prefs.getString("hidden_apps", "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        set(value) = prefs.edit().putString("hidden_apps", value.joinToString(",")).apply()

    var showHiddenAppsInSearch: Boolean
        get() = prefs.getBoolean("show_hidden_apps_in_search", false)
        set(value) = prefs.edit().putBoolean("show_hidden_apps_in_search", value).apply()

    var drawerIconShape: String
        get() = prefs.getString("drawer_icon_shape", "Circle") ?: "Circle"
        set(value) = prefs.edit().putString("drawer_icon_shape", value).apply()

    var drawerIconShadow: Boolean
        get() = prefs.getBoolean("drawer_icon_shadow", false)
        set(value) = prefs.edit().putBoolean("drawer_icon_shadow", value).apply()

    var blackedOutModeEnabled: Boolean
        get() = prefs.getBoolean("blacked_out_mode_enabled", false)
        set(value) = prefs.edit().putBoolean("blacked_out_mode_enabled", value).apply()

    var developerModeEnabled: Boolean
        get() = prefs.getBoolean("developer_mode_enabled", false)
        set(value) = prefs.edit().putBoolean("developer_mode_enabled", value).apply()

    var isUserLoggedIn: Boolean
        get() = prefs.getBoolean("is_user_logged_in", false)
        set(value) = prefs.edit().putBoolean("is_user_logged_in", value).apply()

    var coverThemeEnabled: Boolean
        get() = prefs.getBoolean("cover_theme_enabled", false)
        set(value) = prefs.edit().putBoolean("cover_theme_enabled", value).apply()

    var coverDisplaySize: androidx.compose.ui.unit.IntSize
        get() {
            val dim1 = prefs.getInt("cover_display_dimension_1", 0)
            val dim2 = prefs.getInt("cover_display_dimension_2", 0)
            return androidx.compose.ui.unit.IntSize(dim1, dim2)
        }
        set(value) {
            prefs.edit().apply {
                putInt("cover_display_dimension_1", kotlin.math.min(value.width, value.height))
                putInt("cover_display_dimension_2", kotlin.math.max(value.width, value.height))
            }.apply()
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
        prefs.edit().putString("app_overrides", json.toString()).apply()
    }

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
