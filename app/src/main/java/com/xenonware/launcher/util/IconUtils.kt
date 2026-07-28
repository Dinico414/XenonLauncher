package com.xenonware.launcher.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/**
 * Normalizes an app icon by ensuring it's square and has a background if needed.
 * For Adaptive Icons, it zooms into the "safe zone" (the central 72dp of the 108dp asset)
 * to make the icon appear normal sized while remaining square.
 * 
 * High quality flags are used to prevent pixelation.
 */
fun normalizeIcon(context: Context, drawable: Drawable?): Drawable? {
    if (drawable == null) return null
    
    // Get the device's standard launcher icon size for the best quality/memory balance
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    val size = activityManager.launcherLargeIconSize
    
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Set draw filter for high quality scaling (Anti-alias + Filtering)
    canvas.drawFilter = PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    
    // Always start with a white background
    canvas.drawColor(Color.WHITE)
    
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
    
    return BitmapDrawable(context.resources, bitmap)
}
