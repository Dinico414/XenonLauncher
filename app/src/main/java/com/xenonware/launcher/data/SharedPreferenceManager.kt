package com.xenonware.launcher.data

import android.content.Context
import android.content.SharedPreferences

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

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
