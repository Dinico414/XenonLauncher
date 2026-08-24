package com.xenonware.launcher.media

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.xenonware.launcher.notification.XenonNotificationService

data class MediaAction(
    val title: String,
    val icon: Drawable?,
    val actionIntent: PendingIntent?,
    val customAction: String? = null
)

data class MediaState(
    val title: String? = null,
    val artist: String? = null,
    val isPlaying: Boolean = false,
    val packageName: String? = null,
    val albumArt: Bitmap? = null,
    val albumArtUri: String? = null,
    val position: Long = 0L,
    val duration: Long = 0L,
    val actions: List<MediaAction> = emptyList()
)

class MediaControllerManager(private val context: Context) {
    companion object {
        private var _instance: MediaControllerManager? = null
        val instance: MediaControllerManager? get() = _instance

        fun update() {
            _instance?.updateActiveSession()
        }
    }

    init {
        _instance = this
    }

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

        val notificationListener = ComponentName(context, XenonNotificationService::class.java)
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
            val playbackState = controller.playbackState

            val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            
            // Standard URIs
            var albumArtUri = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI)
            
            // Spotify specific fallback
            if (albumArtUri == null || albumArtUri.startsWith("content://com.spotify")) {
                val spotifyUri = metadata?.getString("com.spotify.music.extra.ART_HTTPS_URI")
                if (spotifyUri != null) {
                    albumArtUri = spotifyUri
                }
            }

            val notification = XenonNotificationService.getNotificationForSession(controller.sessionToken)
                ?: XenonNotificationService.getInstance()?.activeNotifications?.find { it.packageName == controller.packageName }
            
            val serviceContext = XenonNotificationService.getInstance()
            val packageContext = try {
                context.createPackageContext(controller.packageName, 0)
            } catch (e: Exception) {
                null
            }

            // 1. Try CustomActions from PlaybackState
            val customActions = playbackState?.customActions?.mapNotNull { ca ->
                val title = ca.name?.toString() ?: ""
                val actionId = ca.action
                val resourceName = getResourceEntryName(packageContext, ca.icon)
                
                if (isStandardAction(title, actionId, resourceName)) return@mapNotNull null

                val icon = if (packageContext != null && ca.icon != 0) {
                    try { packageContext.getDrawable(ca.icon) } catch (e: Exception) { null }
                } else null

                MediaAction(
                    title = title,
                    icon = icon,
                    actionIntent = null,
                    customAction = actionId
                )
            } ?: emptyList()

            // 2. Try Notification Actions
            val compactActionIndices = notification?.notification?.extras?.getIntArray(android.app.Notification.EXTRA_COMPACT_ACTIONS) ?: intArrayOf()
            val notificationActions = notification?.notification?.actions?.mapIndexedNotNull { index, action ->
                val title = action.title?.toString() ?: ""
                val iconRes = action.icon // Use the direct field which is the resource ID
                val resourceName = getResourceEntryName(packageContext, iconRes)
                
                // Exclude if it's marked as a compact action (standard control)
                if (compactActionIndices.contains(index)) return@mapIndexedNotNull null
                
                // Fallback string/resource filter for safety
                if (isStandardAction(title, null, resourceName)) return@mapIndexedNotNull null

                val icon = try {
                    val iconObj = action.getIcon()
                    if (iconObj != null) {
                        iconObj.loadDrawable(packageContext ?: context) ?: iconObj.loadDrawable(serviceContext ?: context)
                    } else if (action.icon != 0) {
                        packageContext?.getDrawable(action.icon)
                    } else null
                } catch (e: Exception) {
                    action.getIcon()?.loadDrawable(serviceContext ?: context)
                }

                MediaAction(
                    title = title,
                    icon = icon,
                    actionIntent = action.actionIntent
                )
            } ?: emptyList()

            val finalActions = (customActions + notificationActions).distinctBy { it.title.lowercase() }

            mediaState = MediaState(
                title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
                artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
                isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
                packageName = controller.packageName,
                albumArt = albumArt,
                albumArtUri = albumArtUri,
                position = playbackState?.position ?: 0L,
                duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
                actions = finalActions
            )
        } else {
            mediaState = MediaState()
        }
    }

    fun seekTo(position: Long) {
        activeController?.transportControls?.seekTo(position)
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

    fun sendCustomAction(action: String) {
        activeController?.transportControls?.sendCustomAction(action, null)
    }

    fun dumpMediaState(): String {
        val controller = activeController ?: return "No active media controller."
        val metadata = controller.metadata
        val playbackState = controller.playbackState
        val notification = XenonNotificationService.getNotificationForSession(controller.sessionToken)
            ?: XenonNotificationService.getInstance()?.activeNotifications?.find { it.packageName == controller.packageName }

        val sb = StringBuilder()
        sb.append("Package: ${controller.packageName}\n")
        sb.append("Title: ${metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)}\n")
        sb.append("Artist: ${metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)}\n")
        sb.append("PlaybackState: ${playbackState?.state}\n")

        sb.append("\n[PlaybackState Custom Actions]\n")
        playbackState?.customActions?.forEach { ca ->
            val resourceName = getResourceEntryName(try { context.createPackageContext(controller.packageName, 0) } catch (e: Exception) { null }, ca.icon)
            sb.append("- Action: ${ca.action}, Name: ${ca.name}, IconRes: ${ca.icon} ($resourceName)\n")
        }

        sb.append("\n[Notification Actions]\n")
        val compactActionIndices = notification?.notification?.extras?.getIntArray(android.app.Notification.EXTRA_COMPACT_ACTIONS) ?: intArrayOf()
        sb.append("Compact Action Indices: ${compactActionIndices.joinToString()}\n")
        notification?.notification?.actions?.forEachIndexed { index, action ->
            val resourceName = getResourceEntryName(try { context.createPackageContext(controller.packageName, 0) } catch (e: Exception) { null }, action.icon)
            sb.append("- Index $index: Title: ${action.title}, IconRes: ${action.icon} ($resourceName), Intent: ${action.actionIntent != null}\n")
        }

        val log = sb.toString()
        android.util.Log.d("MediaControllerManager", "Media State Dump:\n$log")
        return log
    }

    private fun getResourceEntryName(packageContext: Context?, resId: Int): String? {
        if (packageContext == null || resId == 0) return null
        return try {
            packageContext.resources.getResourceEntryName(resId)
        } catch (e: Exception) {
            null
        }
    }

    private fun isStandardAction(title: String, actionId: String? = null, resourceName: String? = null): Boolean {
        val t = title.lowercase().trim()
        val id = actionId?.lowercase() ?: ""
        val res = resourceName?.lowercase() ?: ""
        
        if (t.isBlank() && id.isBlank() && res.isBlank()) return true
        
        // These terms are almost always used in English in the internal IDs or resource names, 
        // even if the visible title is German/other.
        val standardKeywords = listOf(
            "play", "pause", "next", "prev", "previous", "skip", "stop",
            "close", "dismiss", "exit", "cancel", "clear"
        )
        
        val isStandard = standardKeywords.any { t.contains(it) } ||
                (id.isNotEmpty() && standardKeywords.any { id.contains(it) }) ||
                (res.isNotEmpty() && standardKeywords.any { res.contains(it) })
        
        // Log it so we can see exactly why YouTube is still showing these buttons
        if (isStandard) {
            android.util.Log.d("MediaControllerManager", "Filtered standard action: title='$t', id='$id', res='$res'")
        } else {
            android.util.Log.d("MediaControllerManager", "Found custom action: title='$t', id='$id', res='$res'")
        }
        
        return isStandard || t == "x" || id == "x" || res == "x"
    }
}
