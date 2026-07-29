package com.xenonware.launcher.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable? = null,
    val label: String = name,
    val isCustomized: Boolean = false
)

data class WidgetItem(
    val id: Int,
    val page: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val type: String = "widget", // "widget" or "shortcut"
    val shortcutIntent: String? = null,
    val shortcutLabel: String? = null,
    val shortcutIconRes: String? = null // package:name
)
