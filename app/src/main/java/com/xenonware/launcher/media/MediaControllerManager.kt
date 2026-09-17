package com.xenonware.launcher.media

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.xenonware.launcher.notification.XenonNotificationService
import com.xenonware.launcher.util.PerfLog

data class MediaAction(
    val title: String,
    val icon: Drawable?,
    val actionIntent: PendingIntent?,
    val customAction: String? = null,
    /**
     * The icon pre-rendered for Compose, or null if the app provides none. Draw it with
     * `Image(bitmap, colorFilter = ColorFilter.tint(...))`; see MediaActionButton.
     */
    val iconBitmap: ImageBitmap? = null
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

class MediaControllerManager(context: Context) {
    // Use the application context: the companion below keeps a process-lifetime reference to
    // this manager, so holding an Activity/Service context here would leak it. The application
    // context lives as long as the process, so there is nothing to leak.
    private val context: Context = context.applicationContext
    companion object {
        private const val TAG = "MediaControllerManager"

        // Per-action filter and icon logging.
        private const val DEBUG = false

        // Upper bound for the per-package icon cache; it is also cleared on every package switch.
        private const val MAX_CACHED_ICONS = 64

        // Size the action icons are rendered at. Large enough for a 48 dp button on xxxhdpi.
        private const val ICON_SIZE_PX = 144

        // WeakReference, not a strong static field: this manager holds a Context, and a strong
        // static reference to it would leak that Context for the life of the process (the
        // StaticFieldLeak lint). The single live instance is created by the ViewModel and stays
        // reachable for as long as it is needed, so the weak reference is never collected early.
        private var _instanceRef: java.lang.ref.WeakReference<MediaControllerManager>? = null
        val instance: MediaControllerManager? get() = _instanceRef?.get()

        fun update() {
            instance?.updateActiveSession()
        }
    }

    init {
        _instanceRef = java.lang.ref.WeakReference(this)
    }

    private val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
    var mediaState by mutableStateOf(MediaState())
        private set

    var isPermissionGranted by mutableStateOf(false)
        private set

    private var activeController: MediaController? = null

    private class LoadedIcon(val drawable: Drawable, val bitmap: ImageBitmap?)

    // updateState() runs on the main thread for every playback and metadata callback. These caches
    // keep it from creating a package Context and reloading every action icon each time. They are
    // only valid for one package and are reset when the active package changes.
    private var cachedPackage: String? = null
    private var cachedPackageContext: Context? = null
    private var cachedThemedContext: Context? = null
    private val iconCache = HashMap<String, LoadedIcon?>()
    private val entryNameCache = HashMap<Int, String?>()

    // controller.metadata returns a freshly unparcelled Bitmap on every call. Reusing the previous
    // instance while the track is unchanged keeps MediaState equal, so the UI is not recomposed
    // (and the art not re-uploaded) just because a new Bitmap object arrived.
    private var lastArtSignature: String? = null
    private var lastAlbumArt: Bitmap? = null

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            try {
                updateState()
            } catch (e: Throwable) {
                Log.e(TAG, "Error in onPlaybackStateChanged", e)
            }
        }
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            try {
                updateState()
            } catch (e: Throwable) {
                Log.e(TAG, "Error in onMetadataChanged", e)
            }
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
            PerfLog.measure("media.getActiveSessions", thresholdMs = 8) {
                sessionManager.getActiveSessions(notificationListener)
            }
        } catch (_: SecurityException) {
            emptyList<MediaController>()
        } catch (_: Throwable) {
            emptyList<MediaController>()
        }

        // Pick the first active session or the one currently playing
        val newController = controllers.find { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()

        // Compare sessions, not packages: an app can replace its session, and the old
        // controller would then never report changes again.
        if (newController?.sessionToken != activeController?.sessionToken) {
            activeController?.unregisterCallback(callback)
            activeController = newController
            activeController?.registerCallback(callback)
        }
        updateState()
    }

    // --- Package context and icon loading ---------------------------------------------------

    private fun packageContextFor(pkg: String): Context? {
        if (pkg != cachedPackage) {
            cachedPackage = pkg
            iconCache.clear()
            entryNameCache.clear()
            val packageContext = try {
                PerfLog.measure("media.createPackageContext $pkg", thresholdMs = 8) {
                    context.createPackageContext(pkg, 0)
                }
            } catch (_: Exception) {
                null
            }
            cachedPackageContext = packageContext
            // The media app's own theme. Its action icons are usually vector drawables that
            // reference its theme attributes (?attr/...); without that theme they fail to load,
            // which is why these actions only showed their text before.
            cachedThemedContext = packageContext?.let { pc ->
                val themeRes = pc.applicationInfo?.theme ?: 0
                if (themeRes != 0) runCatching { ContextThemeWrapper(pc, themeRes) }.getOrNull() ?: pc
                else pc
            }
        }
        return cachedPackageContext
    }

    private fun cachedEntryName(packageContext: Context?, resId: Int): String? {
        if (packageContext == null || resId == 0) return null
        if (entryNameCache.containsKey(resId)) return entryNameCache[resId]
        return getResourceEntryName(packageContext, resId).also { entryNameCache[resId] = it }
    }

    private fun Drawable.toIconBitmap(): ImageBitmap? = runCatching {
        toBitmap(ICON_SIZE_PX, ICON_SIZE_PX).asImageBitmap()
    }.getOrNull()

    /** A drawable resource that belongs to the active media app ([pkg]). */
    private fun loadResourceIcon(pkg: String, resId: Int): LoadedIcon? {
        if (resId == 0) return null
        val key = "res|$resId"
        if (iconCache.containsKey(key)) return iconCache[key]

        val packageContext = packageContextFor(pkg)
        val themed = cachedThemedContext

        // Tried in order; each attempt is independent because any of them can throw.
        val drawable =
            // 1. The app's resources with the app's own theme: resolves its ?attr references.
            runCatching {
                themed?.let { ResourcesCompat.getDrawable(it.resources, resId, it.theme) }
            }.getOrNull()
            // 2. The framework path SystemUI uses for these icons.
                ?: runCatching {
                    Icon.createWithResource(pkg, resId).loadDrawable(context)
                }.getOrNull()
                // 3. No theme at all: unresolved attributes are skipped; we tint it anyway.
                ?: runCatching {
                    packageContext?.let { ResourcesCompat.getDrawable(it.resources, resId, null) }
                }.getOrNull()

        val loaded = drawable?.let { LoadedIcon(it, it.toIconBitmap()) }
        if (loaded == null) {
            Log.w(TAG, "Could not load action icon $resId from $pkg")
        }
        putIcon(key, loaded)
        return loaded
    }

    /** A notification action's Icon, which may point into another package or hold a bitmap. */
    private fun loadIconObject(pkg: String, icon: Icon?): LoadedIcon? {
        if (icon == null) return null
        if (icon.type == Icon.TYPE_RESOURCE) {
            val resPackage = runCatching { icon.resPackage }.getOrNull()
            if (resPackage.isNullOrEmpty() || resPackage == pkg) {
                return loadResourceIcon(pkg, icon.resId)
            }
        }

        // Bitmap/URI icons, or resources of another package. The Icon instance lives as long as
        // the posted notification, so its identity is a stable key.
        val key = "icon|${System.identityHashCode(icon)}"
        if (iconCache.containsKey(key)) return iconCache[key]
        val drawable = runCatching { icon.loadDrawable(cachedThemedContext ?: context) }.getOrNull()
            ?: runCatching { icon.loadDrawable(context) }.getOrNull()
        val loaded = drawable?.let { LoadedIcon(it, it.toIconBitmap()) }
        putIcon(key, loaded)
        return loaded
    }

    private fun putIcon(key: String, icon: LoadedIcon?) {
        if (iconCache.size >= MAX_CACHED_ICONS) iconCache.clear()
        iconCache[key] = icon
    }

    // --- State --------------------------------------------------------------------------------

    private fun resolveAlbumArt(metadata: MediaMetadata?, signature: String): Bitmap? {
        val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        val previous = lastAlbumArt
        if (art != null && previous != null && !previous.isRecycled &&
            signature == lastArtSignature &&
            art.width == previous.width && art.height == previous.height
        ) {
            return previous
        }
        lastArtSignature = signature
        lastAlbumArt = art
        return art
    }

    private fun updateState() {
        PerfLog.measure("media.updateState", thresholdMs = 8) { updateStateUnmeasured() }
    }

    private fun updateStateUnmeasured() {
        try {
            val controller = activeController
            if (controller == null) {
                lastArtSignature = null
                lastAlbumArt = null
                mediaState = MediaState()
                return
            }

            val metadata = controller.metadata
            val playbackState = controller.playbackState
            val pkg = controller.packageName
            val packageContext = packageContextFor(pkg)

            val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
            val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L

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

            val albumArt = resolveAlbumArt(
                metadata,
                signature = "$pkg|$title|$artist|$album|$duration|$albumArtUri"
            )

            val notification = XenonNotificationService.getNotificationForSession(controller.sessionToken)
                ?: XenonNotificationService.getInstance()?.safeActiveNotifications?.find { it.packageName == pkg }

            // 1. Custom actions from the PlaybackState (Spotify's like and shuffle live here)
            val customActions = playbackState?.customActions?.mapNotNull { ca ->
                val actionTitle = ca.name?.toString() ?: ""
                val actionId = ca.action
                val resourceName = cachedEntryName(packageContext, ca.icon)

                if (isStandardAction(actionTitle, actionId, resourceName)) return@mapNotNull null

                val loaded = loadResourceIcon(pkg, ca.icon)
                MediaAction(
                    title = actionTitle,
                    icon = loaded?.drawable,
                    actionIntent = null,
                    customAction = actionId,
                    iconBitmap = loaded?.bitmap
                )
            } ?: emptyList()

            // 2. Notification actions
            val compactActionIndices = notification?.notification?.extras
                ?.getIntArray(android.app.Notification.EXTRA_COMPACT_ACTIONS) ?: intArrayOf()
            val notificationActions = notification?.notification?.actions?.mapIndexedNotNull { index, action ->
                // Exclude if it's marked as a compact action (standard control)
                if (compactActionIndices.contains(index)) return@mapIndexedNotNull null

                val actionTitle = action.title?.toString() ?: ""
                val iconObj = action.getIcon()
                val resourceName = if (iconObj != null && iconObj.type == Icon.TYPE_RESOURCE &&
                    runCatching { iconObj.resPackage }.getOrNull() == pkg
                ) {
                    cachedEntryName(packageContext, iconObj.resId)
                } else {
                    resourceNameFromIcon(packageContext, iconObj)
                }

                // Fallback string/resource filter for safety
                if (isStandardAction(actionTitle, null, resourceName)) return@mapIndexedNotNull null

                // Older apps only set the deprecated int icon field.
                @Suppress("DEPRECATION")
                val legacyIconRes = action.icon
                val loaded = loadIconObject(pkg, iconObj) ?: loadResourceIcon(pkg, legacyIconRes)

                MediaAction(
                    title = actionTitle,
                    icon = loaded?.drawable,
                    actionIntent = action.actionIntent,
                    iconBitmap = loaded?.bitmap
                )
            } ?: emptyList()

            // The same action often appears in both lists. Keep the first one (custom actions
            // win), but if it has no icon, borrow the icon of its duplicate.
            val finalActions = (customActions + notificationActions)
                .groupBy { it.title.lowercase() }
                .values
                .map { group ->
                    val first = group.first()
                    if (first.iconBitmap != null) first
                    else group.firstOrNull { it.iconBitmap != null }
                        ?.let { withIcon -> first.copy(icon = withIcon.icon, iconBitmap = withIcon.iconBitmap) }
                        ?: first
                }

            if (DEBUG) {
                finalActions.forEach {
                    Log.d(TAG, "Action '${it.title}': icon=${it.iconBitmap != null} custom=${it.customAction}")
                }
            }

            // mutableStateOf uses structural equality, so an unchanged state (same bitmap and
            // drawable instances thanks to the caches above) does not trigger recomposition.
            mediaState = MediaState(
                title = title,
                artist = artist,
                isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING,
                packageName = pkg,
                albumArt = albumArt,
                albumArtUri = albumArtUri,
                position = playbackState?.position ?: 0L,
                duration = duration,
                actions = finalActions
            )
        } catch (e: Throwable) {
            Log.e(TAG, "Error in updateState", e)
        }
    }

    // --- Transport controls -------------------------------------------------------------------

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

    /** Runs a [MediaAction]: a custom session action, or the notification action's intent. */
    fun perform(action: MediaAction) {
        val custom = action.customAction
        if (custom != null) {
            sendCustomAction(custom)
        } else {
            runCatching { action.actionIntent?.send() }
                .onFailure { Log.w(TAG, "Media action '${action.title}' failed", it) }
        }
        // Like/shuffle state usually changes the icon; refresh right away instead of waiting for
        // the next poll.
        updateState()
    }

    fun dumpMediaState(): String {
        return try {
            val controller = activeController ?: return "No active media controller."
            val metadata = controller.metadata
            val playbackState = controller.playbackState
            val notification = XenonNotificationService.getNotificationForSession(controller.sessionToken)
                ?: XenonNotificationService.getInstance()?.safeActiveNotifications?.find { it.packageName == controller.packageName }
            val pkgCtx = try { context.createPackageContext(controller.packageName, 0) } catch (_: Exception) { null }

            val sb = StringBuilder()
            sb.append("Package: ${controller.packageName}\n")
            sb.append("Title: ${metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)}\n")
            sb.append("Artist: ${metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)}\n")
            sb.append("PlaybackState: ${playbackState?.state}\n")

            sb.append("\n[PlaybackState Custom Actions]\n")
            playbackState?.customActions?.forEach { ca ->
                val resourceName = getResourceEntryName(pkgCtx, ca.icon)
                sb.append("- Action: ${ca.action}, Name: ${ca.name}, IconRes: ${ca.icon} ($resourceName)\n")
            }

            sb.append("\n[Notification Actions]\n")
            val compactActionIndices = notification?.notification?.extras?.getIntArray(android.app.Notification.EXTRA_COMPACT_ACTIONS) ?: intArrayOf()
            sb.append("Compact Action Indices: ${compactActionIndices.joinToString()}\n")
            notification?.notification?.actions?.forEachIndexed { index, action ->
                val resourceName = resourceNameFromIcon(pkgCtx, action.getIcon())
                sb.append("- Index $index: Title: ${action.title}, Icon: ${action.getIcon()} ($resourceName), Intent: ${action.actionIntent != null}\n")
            }

            sb.append("\n[Resolved actions]\n")
            mediaState.actions.forEach {
                sb.append("- ${it.title}: icon=${it.iconBitmap != null}, custom=${it.customAction}\n")
            }

            val log = sb.toString()
            Log.d(TAG, "Media State Dump:\n$log")
            log
        } catch (e: Throwable) {
            "Error dumping media state: ${e.message}"
        }
    }

    /**
     * Best-effort resource-entry name for an Icon backed by a resource; null otherwise. Uses only
     * public Icon API, avoiding the deprecated Notification.Action.icon int field.
     */
    private fun resourceNameFromIcon(
        packageContext: Context?,
        icon: Icon?
    ): String? {
        if (packageContext == null || icon == null) return null
        if (icon.type != Icon.TYPE_RESOURCE) return null
        return try {
            getResourceEntryName(packageContext, icon.resId)
        } catch (_: Exception) {
            null
        }
    }

    private fun getResourceEntryName(packageContext: Context?, resId: Int): String? {
        if (packageContext == null || resId == 0) return null
        return try {
            packageContext.resources.getResourceEntryName(resId)
        } catch (_: Exception) {
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

        if (DEBUG) {
            if (isStandard) {
                Log.d(TAG, "Filtered standard action: title='$t', id='$id', res='$res'")
            } else {
                Log.d(TAG, "Found custom action: title='$t', id='$id', res='$res'")
            }
        }

        return isStandard || t == "x" || id == "x" || res == "x"
    }
}