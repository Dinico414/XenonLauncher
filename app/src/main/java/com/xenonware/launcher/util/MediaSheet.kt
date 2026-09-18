package com.xenonware.launcher.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.xenonware.launcher.media.MediaState

fun openMediaApp(context: Context, mediaState: MediaState) {
    val packageName = mediaState.packageName
    if (!packageName.isNullOrEmpty()) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            context.startActivity(intent)
            return
        }
    }

    try {
        val audioIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                "content://media/external/audio/media".toUri(),
                "audio/*"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooserIntent = Intent.createChooser(audioIntent, "SELECT AUDIO SOURCE").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val knownAudioApps = listOf(
                "com.spotify.music",
                "com.qobuz.music",
                "com.apple.android.music",
                "au.com.shiftyjelly.pocketcasts",
                "de.danoeh.antennapod",
                "com.google.android.apps.youtube.music",
                "com.soundcloud.android",
                "com.tidal.wave",
                "com.deezer.android",
                "com.amazon.mp3"
            )
            val extraIntents = knownAudioApps.mapNotNull { pkg ->
                context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }.toTypedArray()
            if (extraIntents.isNotEmpty()) {
                putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents)
            }
        }
        try {
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            android.util.Log.w("WinampPanel", "Failed to launch audio chooser", e)
        }
    } catch (_: Exception) {
    }
}