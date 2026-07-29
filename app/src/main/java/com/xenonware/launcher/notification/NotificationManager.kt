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
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import androidx.core.graphics.drawable.toDrawable
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
    val senderIcon: Drawable? = null,
    val mediaImage: Drawable? = null,
    val contentIntent: PendingIntent? = null,
    val actions: List<LauncherNotificationAction> = emptyList()
)

data class LauncherNotificationAction(
    val title: String,
    val actionIntent: PendingIntent?,
    val remoteInput: RemoteInput? = null
)

object NotificationManager {
    private val _notificationCount = MutableStateFlow(0)
    val notificationCount: StateFlow<Int> = _notificationCount

    private val _notifications = MutableStateFlow<List<LauncherNotification>>(emptyList())
    val notifications: StateFlow<List<LauncherNotification>> = _notifications


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

            val filtered = activeNotifications.filter { sbn ->
                val ranking = Ranking()
                val hasRanking = rankingMap?.getRanking(sbn.key, ranking) ?: false
                val notification = sbn.notification

                // 1. Core System Filters
                if (sbn.packageName == ownPackageName) return@filter false
                if (sbn.isOngoing) return@filter false

                // 1.5 Media Filter - Exclude media playback notifications
                val isTransport = notification.category == Notification.CATEGORY_TRANSPORT
                val hasMediaSession = notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
                val isMediaStyle = notification.extras.getString(Notification.EXTRA_TEMPLATE)?.contains("MediaStyle") == true
                
                // YouTube, Twitter, etc. often have media we want to show even if they use MediaSession/MediaStyle
                // BUT the user specifically wants to exclude YouTube playback controls
                val isSocialOrVideo = sbn.packageName.contains("twitter") ||
                                     sbn.packageName.contains("x.android") ||
                                     sbn.packageName.contains("instagram")
                
                if ((isTransport || hasMediaSession || isMediaStyle) && !isSocialOrVideo) return@filter false
                
                // 2. Ranking/Importance Filters
                if (hasRanking) {
                    // IMPORTANCE_MIN = 1, IMPORTANCE_LOW = 2, IMPORTANCE_DEFAULT = 3
                    // Show anything that isn't MIN importance (min is usually completely hidden/silent)
                    if (ranking.importance <= 1) return@filter false
                    if (ranking.isSuspended) return@filter false
                }

                // 3. Content Filters
                val extras = notification.extras
                
                val title = extras.getCharSequence("android.title")
                val text = extras.getCharSequence("android.text")
                val bigText = extras.getCharSequence("android.bigText")
                val messages = extras.get("android.messages")
                
                // Permissive content check
                if (title.isNullOrBlank() && text.isNullOrBlank() && bigText.isNullOrBlank() && messages == null) {
                    return@filter false
                }

                true
            }

            // 4. Improved Grouping Logic
            // Instead of grouping by package, group by the actual notification group key.
            // This ensures that if an app has multiple different groups (e.g. different email accounts),
            // we handle summaries correctly for each group.
            val groupedByGroup = filtered.groupBy { it.groupKey ?: (it.packageName + it.id) }
            
            val finalNotifications = groupedByGroup.flatMap { (_, sbnList) ->
                val summaries = sbnList.filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0 }
                val children = sbnList.filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }

                // Prefer individual notifications
                children.ifEmpty {
                    summaries // Fallback to summary if no children (rare but possible)
                }
            }

            _notifications.value = finalNotifications.map { sbn ->
                val extras = sbn.notification.extras
                
                // Determine app categories early for logic and logging
                val isYouTube = sbn.packageName == "com.google.android.youtube"
                val isSocial = sbn.packageName.contains("twitter") || sbn.packageName.contains("x.android") || sbn.packageName.contains("instagram")
                val isWeather = sbn.packageName.contains("googlequicksearchbox")
                val isAliExpress = sbn.packageName.contains("aliexpress")

                // Extracting title
                val title = extras.getCharSequence("android.title")?.toString() ?: ""
                
                // Extracting text body
                var body = extras.getCharSequence("android.text")?.toString()
                    ?: extras.getCharSequence("android.bigText")?.toString()
                    ?: ""
                
                // 5. Advanced text extraction for MessagingStyle/InboxStyle
                val template = extras.getString(Notification.EXTRA_TEMPLATE)
                val isMessaging = template?.contains("MessagingStyle") == true
                val isSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
                
                if (isSummary || body.isBlank() || body.contains("new messages", ignoreCase = true) || body.contains("nachrichten", ignoreCase = true)) {
                    // Try MessagingStyle messages
                    @Suppress("DEPRECATION")
                    val messages = extras.get("android.messages") as? Array<*>
                    if (!messages.isNullOrEmpty()) {
                        val lastMessage = messages.last() as? Bundle
                        val messageText = lastMessage?.getCharSequence("text")
                        if (messageText != null) {
                            body = messageText.toString()
                        }
                    } else {
                        // Try InboxStyle lines
                        val lines = extras.getCharSequenceArray("android.textLines")
                        if (!lines.isNullOrEmpty()) {
                            body = lines.last().toString()
                        }
                    }
                }

                // 6. Media extraction (Images/Thumbnails)
                val largeIcon = sbn.notification.getLargeIcon()?.loadDrawable(context)
                
                @Suppress("DEPRECATION")
                val picture = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable("android.picture", Bitmap::class.java)
                } else {
                    extras.getParcelable("android.picture") as? Bitmap
                })?.toDrawable(context.resources)

                val pictureIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable("android.pictureIcon", Icon::class.java)
                } else {
                    extras.getParcelable("android.pictureIcon") as? Icon
                }?.loadDrawable(context)

                val largeIconBig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable("android.largeIcon.big", Icon::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    extras.getParcelable("android.largeIcon.big") as? Icon
                }?.loadDrawable(context)

                @Suppress("DEPRECATION")
                val bigPicture = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras.getParcelable("android.bigPicture", Bitmap::class.java)
                } else {
                    extras.getParcelable("android.bigPicture") as? Bitmap
                })?.toDrawable(context.resources)

                // 7. MessagingStyle image extraction (WhatsApp Fix)
                var messagingImage: Drawable? = null
                if (isMessaging) {
                    val messages = extras.getParcelableArray("android.messages")
                    if (!messages.isNullOrEmpty()) {
                        for (i in messages.indices.reversed()) {
                            val m = messages[i] as? Bundle ?: continue
                            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                m.getParcelable("dataUri", Uri::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                m.getParcelable("dataUri") as? Uri
                            }
                            

                            if (uri != null && m.getString("dataMimeType")?.startsWith("image/") == true) {
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
                
                // Determine what is a profile pic vs a media thumbnail
                var finalSenderIcon: Drawable?
                var finalMediaImage: Drawable?

                if (isYouTube) {
                    // YouTube: Prioritize anything that looks like a thumbnail
                    // Sometimes YouTube puts it in custom keys, but let's stick to standard for now and log the others.
                    finalMediaImage = pictureIcon ?: picture ?: bigPicture ?: largeIconBig ?: largeIcon
                    finalSenderIcon = null 
                } else if (isWeather || isAliExpress) {
                    // User requested these behave like profile pictures
                    finalSenderIcon = largeIconBig ?: largeIcon
                    finalMediaImage = pictureIcon ?: picture ?: bigPicture
                } else if (isMessaging || isSocial) {
                    // Standard messaging/social: largeIcon is the person, picture is the content
                    finalSenderIcon = largeIconBig ?: largeIcon
                    finalMediaImage = messagingImage ?: pictureIcon ?: picture ?: bigPicture
                } else {
                    // Fallback for other apps
                    if (pictureIcon != null || picture != null || bigPicture != null) {
                        finalMediaImage = pictureIcon ?: picture ?: bigPicture
                        finalSenderIcon = largeIconBig ?: largeIcon
                    } else {
                        finalMediaImage = largeIconBig ?: largeIcon
                        finalSenderIcon = null
                    }
                }

                LauncherNotification(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    title = title,
                    text = body,
                    postTime = sbn.postTime,
                    isMessaging = isMessaging,
                    icon = sbn.notification.smallIcon?.loadDrawable(context),
                    senderIcon = finalSenderIcon,
                    mediaImage = finalMediaImage,
                    contentIntent = sbn.notification.contentIntent,
                    actions = sbn.notification.actions?.map { action ->
                        LauncherNotificationAction(
                            title = action.title.toString(),
                            actionIntent = action.actionIntent,
                            remoteInput = action.remoteInputs?.firstOrNull()
                        )
                    } ?: emptyList()
                )
            }.sortedByDescending { it.postTime }
            
            _notificationCount.value = _notifications.value.size
        } catch (_: Exception) {
        }
    }
}
