package com.xenonware.launcher.util

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

class WidgetHostHolder(private val context: Context) {

    val host = InteractiveAppWidgetHost(context, HOST_ID)

    private val views = HashMap<Int, AppWidgetHostView>()
    private val appliedSizes = HashMap<Int, Pair<Int, Int>>()
    private var listening = false

    fun isCreated(appWidgetId: Int): Boolean = views.containsKey(appWidgetId)

    /** The live view for a widget, if it has been created (used for the drag ghost). */
    fun viewFor(appWidgetId: Int): View? = views[appWidgetId]

    /** Creates the view ahead of time without attaching it anywhere. */
    fun prewarm(appWidgetId: Int, info: AppWidgetProviderInfo?) {
        if (!views.containsKey(appWidgetId)) create(appWidgetId, info)
    }

    /**
     * Returns the cached view for [appWidgetId], creating it on first use. The view is detached
     * from any previous parent so it can be handed to a new AndroidView.
     */
    fun obtainView(appWidgetId: Int, info: AppWidgetProviderInfo?): AppWidgetHostView {
        val view = views[appWidgetId] ?: create(appWidgetId, info)
        (view.parent as? ViewGroup)?.removeView(view)
        return view
    }

    private fun create(appWidgetId: Int, info: AppWidgetProviderInfo?): AppWidgetHostView {
        val view = PerfLog.measure("widget.createView $appWidgetId", thresholdMs = 8) {
            host.createView(context, appWidgetId, info)
        }
        view.setPadding(0, 0, 0, 0)
        // Keeps content capture from walking every widget's view tree on each attach.
        view.importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
        views[appWidgetId] = view
        appliedSizes.remove(appWidgetId)
        return view
    }

    /**
     * Reports the widget's size to its provider, but only when it actually changed. Re-sending it
     * on every composition can make the provider push fresh RemoteViews.
     */
    fun applySize(appWidgetId: Int, view: AppWidgetHostView, widthDp: Int, heightDp: Int) {
        if (widthDp <= 0 || heightDp <= 0) return
        val target = widthDp to heightDp
        if (appliedSizes[appWidgetId] == target) return
        appliedSizes[appWidgetId] = target
        runCatching {
            view.updateAppWidgetSize(Bundle(), listOf(SizeF(widthDp.toFloat(), heightDp.toFloat())))
        }
    }

    fun allocateAppWidgetId(): Int = host.allocateAppWidgetId()

    /** Removes the widget permanently: drops its view and frees the id. */
    fun deleteWidget(appWidgetId: Int) {
        forget(appWidgetId)
        runCatching { host.deleteAppWidgetId(appWidgetId) }
    }

    /** Drops cached views for widgets that are no longer in the layout. */
    fun retainOnly(appWidgetIds: Set<Int>) {
        views.keys.filter { it !in appWidgetIds }.forEach(::forget)
    }

    private fun forget(appWidgetId: Int) {
        views.remove(appWidgetId)?.let { (it.parent as? ViewGroup)?.removeView(it) }
        appliedSizes.remove(appWidgetId)
    }

    fun startListening() {
        if (listening) return
        runCatching { host.startListening() }
        listening = true
    }

    fun stopListening() {
        if (!listening) return
        runCatching { host.stopListening() }
        listening = false
    }

    /** Stops listening and releases every view. Called when the owning composition goes away. */
    fun destroy() {
        stopListening()
        views.keys.toList().forEach(::forget)
    }

    companion object {
        const val HOST_ID = 1024
    }
}

/**
 * A [WidgetHostHolder] that lives as long as the calling composition and listens for widget
 * updates while the Activity is started, like Launcher3.
 */
@Composable
fun rememberWidgetHost(): WidgetHostHolder {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val holder = remember(context) { WidgetHostHolder(context) }

    DisposableEffect(holder, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> holder.startListening()
                Lifecycle.Event.ON_STOP -> holder.stopListening()
                else -> Unit
            }
        }
        // If the Activity is already started, the observer receives ON_START immediately.
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(holder) {
        onDispose { holder.destroy() }
    }

    return holder
}