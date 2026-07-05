package com.xenonware.launcher.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable? = null
)

data class WidgetItem(
    val id: Int,
    val page: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)
