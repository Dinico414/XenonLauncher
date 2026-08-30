package com.xenonware.launcher.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import com.xenonware.launcher.model.AppOverride
import com.xenonware.launcher.ui.res.IconShape

/**
 * Normalizes an app icon by ensuring its square and has a background if needed.
 * For Adaptive Icons, it zooms into the "safe zone" (the central 72dp of the 108dp asset)
 * to make the icon appear normal-sized while remaining square.
 * 
 * High quality flags are used to prevent pixelation.
 */
fun normalizeIcon(context: Context, drawable: Drawable?): Drawable? {
    if (drawable == null) return null
    
    // Get the device's standard launcher icon size for the best quality/memory balance
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    val size = activityManager.launcherLargeIconSize
    
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    
    // Draw filter for high quality scaling
    canvas.drawFilter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    
    if (drawable is AdaptiveIconDrawable) {
        val scale = 1.5f
        val offset = (size * (scale - 1f) / 2f).toInt()
        val bounds = Rect(-offset, -offset, size + offset, size + offset)
        
        drawable.background?.let {
            it.bounds = bounds
            if (it is BitmapDrawable) it.isFilterBitmap = true
            it.draw(canvas)
        }
        drawable.foreground?.let {
            it.bounds = bounds
            if (it is BitmapDrawable) it.isFilterBitmap = true
            it.draw(canvas)
        }
    } else {
        // For legacy icons, draw at full size
        drawable.setBounds(0, 0, size, size)
        if (drawable is BitmapDrawable) drawable.isFilterBitmap = true
        drawable.draw(canvas)
    }
    
    return bitmap.toDrawable(context.resources)
}

fun generateCustomIcon(
    context: Context,
    baseDrawable: Drawable?,
    override: AppOverride,
    shape: IconShape,
): Drawable? {
    if (baseDrawable == null) return null

    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    val size = activityManager.launcherLargeIconSize
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    canvas.drawFilter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // 1. Draw Background if provided
    override.backgroundColor?.let { canvas.drawColor(it) }

    // 2. Draw Icon with Zoom
    val zoom = override.zoom.coerceIn(0.1f, 5.0f)
    val borderOffset = override.borderWidth * (size / 100f) // scale border width relative to icon size
    val availableSize = size - (borderOffset * 2)
    val scaledSize = availableSize * zoom
    
    val left = (size - scaledSize) / 2f
    val top = (size - scaledSize) / 2f
    val right = left + scaledSize
    val bottom = top + scaledSize
    
    val bounds = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    
    if (baseDrawable is AdaptiveIconDrawable) {
        val adaptiveScale = 1.5f // standard adaptive icon scale factor
        val adaptiveOffset = (scaledSize * (adaptiveScale - 1f) / 2f).toInt()
        val adaptiveBounds = Rect(
            bounds.left - adaptiveOffset,
            bounds.top - adaptiveOffset,
            bounds.right + adaptiveOffset,
            bounds.bottom + adaptiveOffset
        )
        
        // Draw original background layer if it exists
        baseDrawable.background?.let {
            it.bounds = adaptiveBounds
            if (it is BitmapDrawable) it.isFilterBitmap = true
            it.draw(canvas)
        }
        
        baseDrawable.foreground?.let {
            it.bounds = adaptiveBounds
            if (it is BitmapDrawable) it.isFilterBitmap = true
            it.draw(canvas)
        }
    } else {
        baseDrawable.bounds = bounds
        if (baseDrawable is BitmapDrawable) baseDrawable.isFilterBitmap = true
        baseDrawable.draw(canvas)
    }

    // 3. Apply Shape Clipping and Border
    val resultBitmap = createBitmap(size, size)
    val resultCanvas = Canvas(resultBitmap)
    resultCanvas.drawFilter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val path = shape.getAndroidPath(size.toFloat(), size.toFloat())
    resultCanvas.clipPath(path)
    resultCanvas.drawBitmap(bitmap, 0f, 0f, null)

    // Draw Border
    if (override.borderWidth > 0 && override.borderColor != null) {
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        borderPaint.style = Paint.Style.STROKE
        borderPaint.color = override.borderColor
        borderPaint.strokeWidth = override.borderWidth * (size / 100f) * 2f // multiply by 2 because stroke is centered
        resultCanvas.drawPath(path, borderPaint)
    }

    return resultBitmap.toDrawable(context.resources)
}

fun loadIconFromPack(context: Context, packageName: String, resourceName: String): Drawable? {
    return try {
        val res = context.packageManager.getResourcesForApplication(packageName)
        val id = res.getIdentifier(resourceName, "drawable", packageName)
        if (id != 0) res.getDrawable(id, null) else null
    } catch (_: Exception) {
        null
    }
}

fun getAllIconPackIcons(context: Context, packageName: String): List<String> {
    val icons = mutableListOf<String>()
    try {
        val res = context.packageManager.getResourcesForApplication(packageName)
        
        // Try drawable.xml first (standard for pickers)
        var resourceId = res.getIdentifier("drawable", "xml", packageName)
        if (resourceId == 0) {
            // Try appfilter.xml as fallback
            resourceId = res.getIdentifier("appfilter", "xml", packageName)
        }
        
        if (resourceId != 0) {
            val parser = res.getXml(resourceId)
            var eventType = parser.eventType
            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                    val name = parser.name
                    if (name == "item") {
                        val drawableName = parser.getAttributeValue(null, "drawable")
                        if (!drawableName.isNullOrEmpty()) {
                            icons.add(drawableName)
                        }
                    }
                }
                eventType = parser.next()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return icons.distinct()
}

fun getIconPackMap(context: Context, packageName: String): Map<String, String> {
    val map = mutableMapOf<String, String>()
    try {
        val res = context.packageManager.getResourcesForApplication(packageName)
        val resourceId = res.getIdentifier("appfilter", "xml", packageName)
        if (resourceId != 0) {
            val parser = res.getXml(resourceId)
            var eventType = parser.eventType
            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (component != null && drawable != null) {
                        map[component] = drawable
                    }
                }
                eventType = parser.next()
            }
        }
    } catch (_: Exception) {}
    return map
}
