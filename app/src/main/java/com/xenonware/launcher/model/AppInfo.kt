package com.xenonware.launcher.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable? = null,
    val installProgress: Float? = null,
    val isInstalling: Boolean = false
)
