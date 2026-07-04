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

    var autoFocusSearch: Boolean
        get() = prefs.getBoolean("auto_focus_search", false)
        set(value) = prefs.edit().putBoolean("auto_focus_search", value).apply()

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
