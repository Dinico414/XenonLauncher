package com.xenonware.launcher.util

import android.appwidget.AppWidgetProviderInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Bridges the widget configuration result from MainActivity.onActivityResult to WidgetPage.
 *
 * AppWidgetHost.startAppWidgetConfigureActivityForResult() uses the classic
 * startActivityForResult, so the result can't go through an ActivityResultLauncher.
 */
object WidgetConfig {
    const val REQUEST_CONFIGURE = 0x0C01   // first setup while adding
    const val REQUEST_RECONFIGURE = 0x0C02 // "Widget settings" on an existing widget

    data class Result(val requestCode: Int, val resultCode: Int)

    // A StateFlow (not a SharedFlow) so the result is kept even if WidgetPage
    // isn't composed at the moment it arrives. WidgetPage consumes it.
    private val _results = MutableStateFlow<Result?>(null)
    val results: StateFlow<Result?> = _results

    fun deliver(requestCode: Int, resultCode: Int) {
        _results.value = Result(requestCode, resultCode)
    }

    fun consume() {
        _results.value = null
    }
}

/** The setup screen must run before the widget is placed. */
fun AppWidgetProviderInfo.needsConfigOnAdd(): Boolean =
    configure != null &&
            (widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) == 0

/**
 * The provider says its setup screen can be reopened on an existing widget.
 * To also offer it for older widgets that never set the flag, return `configure != null`.
 */
fun AppWidgetProviderInfo.isReconfigurable(): Boolean =
    configure != null &&
            (widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE) != 0