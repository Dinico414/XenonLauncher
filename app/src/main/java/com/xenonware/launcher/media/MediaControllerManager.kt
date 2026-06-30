package com.xenonware.launcher.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.provider.Settings
import android.text.TextUtils

data class MediaState(
    val title: String? = null,
    val artist: String? = null,
    val isPlaying: Boolean = false,
    val packageName: String? = null,
    val albumArt: Bitmap? = null
)

class MediaControllerManager(private val context: Context) {
    private val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    var mediaState by mutableStateOf(MediaState())
        private set
    
    var isPermissionGranted by mutableStateOf(false)
        private set

    private var activeController: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateState()
        }
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateState()
        }
    }

    fun updatePermissionStatus() {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        isPermissionGranted = if (!TextUtils.isEmpty(flat)) {
            flat.split(":").any {
                val cn = ComponentName.unflattenFromString(it)
                cn != null && TextUtils.equals(packageName, cn.packageName)
            }
        } else false
    }

    fun updateActiveSession() {
        updatePermissionStatus()
        if (!isPermissionGranted) return

        val notificationListener = ComponentName(context, com.xenonware.launcher.notification.XenonNotificationService::class.java)
        val controllers = try {
            sessionManager.getActiveSessions(notificationListener)
        } catch (e: SecurityException) {
            emptyList<MediaController>()
        }
        
        // Pick the first active session or the one currently playing
        val newController = controllers.find { it.playbackState?.state == PlaybackState.STATE_PLAYING } 
            ?: controllers.firstOrNull()

        if (newController?.packageName != activeController?.packageName) {
            activeController?.unregisterCallback(callback)
            activeController = newController
            activeController?.registerCallback(callback)
        }
        updateState()
    }

    private fun updateState() {
        val controller = activeController
        if (controller != null) {
            val metadata = controller.metadata
            mediaState = MediaState(
                title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
                artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
                isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING,
                packageName = controller.packageName,
                albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            )
        } else {
            mediaState = MediaState()
        }
    }

    fun togglePlayPause() {
        val controller = activeController ?: return
        if (mediaState.isPlaying) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun skipNext() {
        activeController?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }
}
