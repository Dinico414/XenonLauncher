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
}
