package com.xenonware.launcher.model

import android.appwidget.AppWidgetProviderInfo
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import java.util.UUID

data class WidgetPickerItemData(
    val label: String,
    val isWidget: Boolean,
    val widgetInfo: AppWidgetProviderInfo? = null,
    val shortcutInfo: ResolveInfo? = null,
    val id: String = UUID.randomUUID().toString()
)

data class AppWidgetGroup(
    val appName: String,
    val icon: Drawable?
) : Comparable<AppWidgetGroup> {
    override fun compareTo(other: AppWidgetGroup): Int = appName.compareTo(other.appName)
}
