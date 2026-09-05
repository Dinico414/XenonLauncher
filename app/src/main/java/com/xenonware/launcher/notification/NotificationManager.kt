package com.xenonware.launcher.notification

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.graphics.drawable.toDrawable
import com.xenonware.launcher.ui.res.notification.ChronoDetector
import com.xenonware.launcher.ui.res.notification.ChronoKind
import com.xenonware.launcher.ui.res.notification.ChronoState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class LauncherNotification(
    val key: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val postTime: Long,
    val isMessaging: Boolean = false,
    val icon: Drawable? = null,
    val iconKey: String? = null,
    val senderIcon: Drawable? = null,
    val mediaImage: Drawable? = null,
    val contentIntent: PendingIntent? = null,
    val actions: List<LauncherNotificationAction> = emptyList(),
    val chrono: ChronoState = ChronoState.NONE,
    val isMuted: Boolean = false,
    val isOngoing: Boolean = false,
) {
    // Convenience accessors so existing call sites keep compiling.
    val isTimer: Boolean get() = chrono.kind == ChronoKind.TIMER
    val isStopwatch: Boolean get() = chrono.kind == ChronoKind.STOPWATCH
}

data class LauncherNotificationAction(
    val title: String,
    val actionIntent: PendingIntent?,
    val remoteInput: RemoteInput? = null
)

object NotificationManager {
    private const val TAG = "NotificationManager"

    private val _notificationCount = MutableStateFlow(0)
    val notificationCount: StateFlow<Int> = _notificationCount

    private val _notifications = MutableStateFlow<List<LauncherNotification>>(emptyList())
    val notifications: StateFlow<List<LauncherNotification>> = _notifications

    var visibleApps: Set<String>? = null
    var showMuteNotifications: Boolean = false
    var showPermanentNotifications: Boolean = false
    var disableGrouping: Boolean = false

    fun removeNotificationOptimistically(key: String) {
        val current = _notifications.value
        val updated = current.filter { it.key != key }
        if (updated.size != current.size) {
            _notifications.value = updated
            _notificationCount.value = updated.size
        }
    }

    fun removeNotificationsByPackageOptimistically(packageName: String) {
        val current = _notifications.value
        val updated = current.filter { it.packageName != packageName }
        if (updated.size != current.size) {
            _notifications.value = updated
            _notificationCount.value = updated.size
        }
    }

    fun removeAllNotificationsOptimistically() {
        _notifications.value = emptyList()
        _notificationCount.value = 0
    }

    fun updateFromNotifications(
        context: Context,
        activeNotifications: Array<StatusBarNotification>?,
        rankingMap: RankingMap?,
        ownPackageName: String
    ) {
        try {
            if (activeNotifications == null) {
                _notificationCount.value = 0
                _notifications.value = emptyList()
                return
            }

            // ---- ONE detection pass. Everything downstream reads this map. ----
            val chronoByKey = HashMap<String, ChronoState>(activeNotifications.size)
            activeNotifications.forEach { sbn ->
                val state = ChronoDetector.detect(context, sbn)
                chronoByKey[sbn.key] = state

                if (ChronoDetector.looksLikeClockApp(sbn.packageName)) {
                    // Full field dump for anything clock-ish, whether detected or not.
                    ChronoDetector.dump(sbn)
                }
            }

            val dropped = HashMap<String, String>()

            val filtered = activeNotifications.filter { sbn ->
                val notification = sbn.notification
                val extras = notification.extras
                val chrono = chronoByKey[sbn.key] ?: ChronoState.NONE
                val isTimeRelated = chrono.isTimeRelated

                fun drop(reason: String): Boolean {
                    dropped[sbn.key] = reason
                    return false
                }

                // 0. App filter (time-related notifications bypass it)
                val apps = visibleApps
                if (!apps.isNullOrEmpty() && !isTimeRelated) {
                    if (apps.contains("__NONE__")) return@filter drop("app filter: __NONE__")
                    if (!apps.contains(sbn.packageName)) return@filter drop("app filter: not in visibleApps")
                }

                // 1. Core system filters
                if (sbn.packageName == ownPackageName) return@filter drop("own package")
                if (sbn.isOngoing && !isTimeRelated && !showPermanentNotifications) return@filter drop("ongoing && !timeRelated")

                // 2. Media filter
                val isTransport = notification.category == Notification.CATEGORY_TRANSPORT
                val hasMediaSession = extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
                val isMediaStyle =
                    extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true
                val isSocialOrVideo = sbn.packageName.contains("twitter") ||
                        sbn.packageName.contains("x.android") ||
                        sbn.packageName.contains("instagram")

                if ((isTransport || hasMediaSession || isMediaStyle) &&
                    !isSocialOrVideo && !isTimeRelated
                ) return@filter drop("media filter")

                // 3. Ranking / importance
                val ranking = Ranking()
                if (rankingMap?.getRanking(sbn.key, ranking) == true) {
                    if (ranking.importance <= 2 && !isTimeRelated && !showMuteNotifications) {
                        return@filter drop("importance=${ranking.importance}")
                    }
                    if (ranking.isSuspended) return@filter drop("suspended")
                }

                // 4. Content
                val title = extras.getCharSequence("android.title")
                val text = extras.getCharSequence("android.text")
                val bigText = extras.getCharSequence("android.bigText")
                val messages = extras.get("android.messages")

                if (title.isNullOrBlank() && text.isNullOrBlank() &&
                    bigText.isNullOrBlank() && messages == null && !isTimeRelated
                ) return@filter drop("no content")

                true
            }

            // 5. Grouping: prefer children over the group summary, per group key.
            val finalNotifications = if (disableGrouping) {
                filtered
            } else {
                val groupedByGroup = filtered.groupBy { it.groupKey ?: (it.packageName + it.id) }
                groupedByGroup.flatMap { (_, sbnList) ->
                    val summaries =
                        sbnList.filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0 }
                    val children =
                        sbnList.filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
                    children.ifEmpty { summaries }
                }
            }

            _notifications.value = finalNotifications.map { sbn ->
                val extras = sbn.notification.extras
                val chrono = chronoByKey[sbn.key] ?: ChronoState.NONE

                val isYouTube = sbn.packageName == "com.google.android.youtube"
                val isSocial = sbn.packageName.contains("twitter") ||
                        sbn.packageName.contains("x.android") ||
                        sbn.packageName.contains("instagram")
                val isWeather = sbn.packageName.contains("googlequicksearchbox")
                val isAliExpress = sbn.packageName.contains("aliexpress")

                val title = extras.getCharSequence("android.title")?.toString() ?: ""

                var body = extras.getCharSequence("android.text")?.toString()
                    ?: extras.getCharSequence("android.bigText")?.toString()
                    ?: ""

                val template = extras.getString(Notification.EXTRA_TEMPLATE)
                val isMessaging = template?.contains("MessagingStyle") == true
                val isSummary =
                    (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

                if (isSummary || body.isBlank() ||
                    body.contains("new messages", ignoreCase = true) ||
                    body.contains("nachrichten", ignoreCase = true)
                ) {
                    @Suppress("DEPRECATION")
                    val messages = extras.get("android.messages") as? Array<*>
                    if (!messages.isNullOrEmpty()) {
                        val lastMessage = messages.last() as? Bundle
                        val messageText = lastMessage?.getCharSequence("text")
                        if (messageText != null) body = messageText.toString()
                    } else {
                        val lines = extras.getCharSequenceArray("android.textLines")
                        if (!lines.isNullOrEmpty()) body = lines.last().toString()
                    }
                }

                // ---- media / avatar extraction ----
                val largeIcon = sbn.notification.getLargeIcon()?.loadDrawable(context)

                @Suppress("DEPRECATION")
                val picture = (
                        extras.getParcelable("android.picture", Bitmap::class.java)
                        )?.toDrawable(context.resources)

                val pictureIcon =
                    extras.getParcelable("android.pictureIcon", Icon::class.java)
                        ?.loadDrawable(context)

                val largeIconBig =
                    extras.getParcelable("android.largeIcon.big", Icon::class.java)
                        ?.loadDrawable(context)

                @Suppress("DEPRECATION")
                val bigPicture = (
                        extras.getParcelable("android.bigPicture", Bitmap::class.java)
                        )?.toDrawable(context.resources)

                var messagingImage: Drawable? = null
                if (isMessaging) {
                    val messages = extras.getParcelableArray("android.messages")
                    if (!messages.isNullOrEmpty()) {
                        for (i in messages.indices.reversed()) {
                            val m = messages[i] as? Bundle ?: continue
                            val uri =
                                m.getParcelable("dataUri", Uri::class.java)
                            if (uri != null && m.getString("dataMimeType")
                                    ?.startsWith("image/") == true
                            ) {
                                try {
                                    context.contentResolver.openInputStream(uri)?.use {
                                        val bmp = BitmapFactory.decodeStream(it)
                                        if (bmp != null) {
                                            messagingImage = bmp.toDrawable(context.resources)
                                        }
                                    }
                                } catch (_: Exception) {
                                }
                                if (messagingImage != null) break
                            }
                        }
                    }
                }

                val finalSenderIcon: Drawable?
                val finalMediaImage: Drawable?

                if (isYouTube) {
                    finalMediaImage = pictureIcon ?: picture ?: bigPicture ?: largeIconBig ?: largeIcon
                    finalSenderIcon = null
                } else if (isWeather || isAliExpress) {
                    finalSenderIcon = largeIconBig ?: largeIcon
                    finalMediaImage = pictureIcon ?: picture ?: bigPicture
                } else if (isMessaging || isSocial) {
                    finalSenderIcon = largeIconBig ?: largeIcon
                    finalMediaImage = messagingImage ?: pictureIcon ?: picture ?: bigPicture
                } else {
                    if (pictureIcon != null || picture != null || bigPicture != null) {
                        finalMediaImage = pictureIcon ?: picture ?: bigPicture
                        finalSenderIcon = largeIconBig ?: largeIcon
                    } else {
                        finalMediaImage = largeIconBig ?: largeIcon
                        finalSenderIcon = null
                    }
                }

                val smallIcon = sbn.notification.smallIcon
                val iconKey = when (smallIcon?.type) {
                    Icon.TYPE_RESOURCE -> "res:${smallIcon.resPackage}:${smallIcon.resId}"
                    Icon.TYPE_URI -> "uri:${smallIcon.uri}"
                    Icon.TYPE_BITMAP -> "bmp:${smallIcon.hashCode()}"
                    Icon.TYPE_DATA -> "data:${smallIcon.hashCode()}"
                    else -> null
                }

                val ranking = Ranking()
                val isMuted = if (rankingMap?.getRanking(sbn.key, ranking) == true) {
                    ranking.importance <= 2
                } else false
                
                Log.d(TAG, "Notification ${sbn.key}: importance=${ranking.importance}, isMuted=$isMuted")

                LauncherNotification(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    title = title,
                    text = body,
                    postTime = sbn.postTime,
                    isMessaging = isMessaging,
                    icon = smallIcon?.loadDrawable(context),
                    iconKey = iconKey,
                    senderIcon = finalSenderIcon,
                    mediaImage = finalMediaImage,
                    contentIntent = sbn.notification.contentIntent,
                    actions = sbn.notification.actions?.map { action ->
                        LauncherNotificationAction(
                            title = action.title.toString(),
                            actionIntent = action.actionIntent,
                            remoteInput = action.remoteInputs?.firstOrNull()
                        )
                    } ?: emptyList(),
                    chrono = chrono,
                    isMuted = isMuted,
                    isOngoing = sbn.isOngoing
                )
            }.sortedByDescending { it.postTime }

            _notificationCount.value = _notifications.value.size

        } catch (e: Throwable) {
            Log.e(TAG, "updateFromNotifications failed", e)
        }
    }
}