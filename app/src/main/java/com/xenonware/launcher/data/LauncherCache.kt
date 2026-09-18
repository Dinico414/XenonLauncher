package com.xenonware.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.viewmodel.CalendarEvent
import com.xenonware.launcher.viewmodel.WeatherState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.roundToInt

class LauncherCache(context: Context) {

    companion object {
        private const val TAG = "LauncherCache"
        private const val ICON_SIZE_DP = 64
    }

    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, "launcher_cache").apply { mkdirs() }
    private val iconDir = File(root, "icons").apply { mkdirs() }
    private val appsFile = File(root, "apps.json")
    private val weatherFile = File(root, "weather.json")
    private val calendarFile = File(root, "calendar.json")

    private val iconSizePx: Int =
        (ICON_SIZE_DP * appContext.resources.displayMetrics.density).roundToInt()

    data class CachedApp(
        val key: String,
        val stamp: String,
        val info: AppInfo,
    )

    data class CachedWeather(
        val state: WeatherState,
        val fetchedAt: Long,
        val unitKey: String,
    )

    fun rasterize(drawable: Drawable): Drawable {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null &&
                drawable.bitmap.width == iconSizePx && drawable.bitmap.height == iconSizePx &&
                drawable.bitmap.config == Bitmap.Config.ARGB_8888
            ) {
                drawable
            } else {
                wrap(drawable.toBitmap(iconSizePx, iconSizePx, Bitmap.Config.ARGB_8888))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not rasterize icon: ${e.message}")
            drawable
        }
    }

    private fun wrap(bitmap: Bitmap): BitmapDrawable {
        bitmap.density = appContext.resources.displayMetrics.densityDpi
        return bitmap.toDrawable(appContext.resources)
    }

    private fun iconFile(key: String): File {
        val safe = key.replace(Regex("[^A-Za-z0-9._-]"), "_").take(160)
        return File(iconDir, "${safe}_${Integer.toHexString(key.hashCode())}.png")
    }

    @Synchronized
    fun saveIcon(key: String, icon: Drawable?) {
        val bitmap = (icon as? BitmapDrawable)?.bitmap ?: return
        try {
            val target = iconFile(key)
            val tmp = File(iconDir, target.name + ".tmp")
            tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            if (!tmp.renameTo(target)) {
                target.delete()
                tmp.renameTo(target)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not save icon for $key: ${e.message}")
        }
    }

    @Synchronized
    fun deleteIcon(key: String) {
        iconFile(key).delete()
    }

    // ------------------------------------------------------------------ apps

    @Synchronized
    fun loadApps(): List<CachedApp> {
        if (!appsFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(appsFile.readText())
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val key = o.getString("key")
                val hasIcon = o.optBoolean("hasIcon", true)
                val icon = if (hasIcon) {
                    val f = iconFile(key)
                    if (f.exists()) BitmapFactory.decodeFile(f.path)?.let { wrap(it) } else null
                } else null
                // Icon file missing or broken: drop the entry so the next scan rebuilds it
                if (hasIcon && icon == null) return@mapNotNull null

                val name = o.getString("name")

                CachedApp(
                    key = key,
                    stamp = o.getString("stamp"),
                    info = AppInfo(
                        name = name,
                        packageName = o.getString("pkg"),
                        icon = icon,
                        label = o.optString("label", name),
                        isCustomized = o.optBoolean("customized", false),
                        color = if (o.has("color")) o.getInt("color") else null
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "App cache unreadable, ignoring: ${e.message}")
            emptyList()
        }
    }

    @Synchronized
    fun saveApps(apps: Collection<CachedApp>) {
        val arr = JSONArray()
        apps.forEach { entry ->
            arr.put(JSONObject().apply {
                put("key", entry.key)
                put("stamp", entry.stamp)
                put("pkg", entry.info.packageName)
                put("name", entry.info.name)
                put("label", entry.info.label)
                put("customized", entry.info.isCustomized)
                put("hasIcon", entry.info.icon != null)
                entry.info.color?.let { put("color", it) }
            })
        }
        writeAtomically(appsFile, arr.toString())
    }

    // ------------------------------------------------------------------ weather

    @Synchronized
    fun loadWeather(): CachedWeather? {
        if (!weatherFile.exists()) return null
        return try {
            val o = JSONObject(weatherFile.readText())
            CachedWeather(
                state = WeatherState(
                    temperature = o.getString("temperature"),
                    condition = o.getString("condition"),
                    maxTemp = o.nullableString("maxTemp"),
                    minTemp = o.nullableString("minTemp"),
                    dailyCondition = o.nullableString("dailyCondition")
                ),
                fetchedAt = o.getLong("fetchedAt"),
                unitKey = o.optString("unitKey", "")
            )
        } catch (_: Exception) {
            null
        }
    }

    @Synchronized
    fun saveWeather(state: WeatherState, fetchedAt: Long, unitKey: String) {
        val o = JSONObject().apply {
            put("temperature", state.temperature)
            put("condition", state.condition)
            state.maxTemp?.let { put("maxTemp", it) }
            state.minTemp?.let { put("minTemp", it) }
            state.dailyCondition?.let { put("dailyCondition", it) }
            put("fetchedAt", fetchedAt)
            put("unitKey", unitKey)
        }
        writeAtomically(weatherFile, o.toString())
    }

    // ------------------------------------------------------------------ calendar

    @Synchronized
    fun loadCalendarEvents(): List<CalendarEvent>? {
        if (!calendarFile.exists()) return null
        return try {
            val arr = JSONArray(calendarFile.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                CalendarEvent(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    startTime = o.getLong("start"),
                    endTime = o.getLong("end"),
                    location = o.nullableString("location"),
                    isAllDay = o.getBoolean("allDay"),
                    calendarId = o.getString("calendarId"),
                    color = if (o.has("color")) o.getInt("color") else null
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    @Synchronized
    fun saveCalendarEvents(events: List<CalendarEvent>) {
        val arr = JSONArray()
        events.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("start", e.startTime)
                put("end", e.endTime)
                e.location?.let { put("location", it) }
                put("allDay", e.isAllDay)
                put("calendarId", e.calendarId)
                e.color?.let { put("color", it) }
            })
        }
        writeAtomically(calendarFile, arr.toString())
    }

    // ------------------------------------------------------------------ helpers

    private fun writeAtomically(file: File, text: String) {
        try {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not write ${file.name}: ${e.message}")
        }
    }

    private fun JSONObject.nullableString(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null
}