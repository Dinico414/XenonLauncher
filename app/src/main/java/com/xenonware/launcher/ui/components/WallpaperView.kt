package com.xenonware.launcher.ui.components

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun canReadRealWallpaper(context: Context): Boolean {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            Environment.isExternalStorageManager()
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED ||
                    Environment.isExternalStorageManager()
        }
        else -> {
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}

@Composable
fun WallpaperView(
    modifier: Modifier = Modifier,
    isStoragePermissionGranted: Boolean = false
) {
    val context = LocalContext.current
    val wallpaperManager = remember { WallpaperManager.getInstance(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasAccess by remember { mutableStateOf(canReadRealWallpaper(context)) }
    var wallpaperVersion by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { wallpaperVersion++ }
        }
        @Suppress("DEPRECATION")
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_WALLPAPER_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hasAccess = canReadRealWallpaper(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val wallpaperBitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = isStoragePermissionGranted,
        key2 = wallpaperVersion,
        key3 = hasAccess
    ) {
        value = withContext(Dispatchers.IO) {
            if (hasAccess) {
                runCatching { wallpaperManager.peekDrawable()?.toBitmapSafely() }.getOrNull()
            } else null
        }
    }

    val bitmap = wallpaperBitmap
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        // Transparent fallback to let system wallpaper show through
        Box(modifier = modifier.fillMaxSize())
    }
}

private fun Drawable.toBitmapSafely(): Bitmap {
    val w = if (intrinsicWidth > 0) intrinsicWidth else 1080
    val h = if (intrinsicHeight > 0) intrinsicHeight else 1920
    return toBitmap(w, h)
}
