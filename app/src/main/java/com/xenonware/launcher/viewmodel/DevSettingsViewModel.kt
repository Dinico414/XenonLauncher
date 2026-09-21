@file:Suppress("unused")

package com.xenonware.launcher.viewmodel

import android.app.Application
import android.app.Notification
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xenonware.launcher.PermissionActivity
import com.xenonware.launcher.R
import com.xenonware.launcher.data.SharedPreferenceManager
import com.xenonware.launcher.media.MediaControllerManager
import com.xenonware.launcher.notification.NotificationManager
import com.xenonware.launcher.notification.XenonNotificationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.lang.reflect.Array
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DevSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferenceManager = SharedPreferenceManager(application)

    private val _devModeToggleState = MutableStateFlow(sharedPreferenceManager.developerModeEnabled)
    val devModeToggleState: StateFlow<Boolean> = _devModeToggleState.asStateFlow()

    private val _crashLogExists = MutableStateFlow(false)
    val crashLogExists: StateFlow<Boolean> = _crashLogExists.asStateFlow()

    init {
        updateCrashLogStatus()
    }

    fun setDeveloperModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sharedPreferenceManager.developerModeEnabled = enabled
            _devModeToggleState.value = enabled
        }
    }

    fun restartSetup() {
        sharedPreferenceManager.isFirstLaunch = true
        val context = getApplication<Application>()
        val intent = Intent(context, PermissionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
    }

    private fun getCrashLogFile(): File {
        return File(getApplication<Application>().getExternalFilesDir(null), "crash_log.txt")
    }

    fun updateCrashLogStatus() {
        _crashLogExists.value = getCrashLogFile().exists()
    }

    fun readCrashLog(): String {
        val file = getCrashLogFile()
        return if (file.exists()) file.readText() else getApplication<Application>().getString(R.string.no_crash_log_found)
    }

    fun clearCrashLog() {
        val file = getCrashLogFile()
        val context = getApplication<Application>()
        if (file.exists()) {
            file.delete()
            updateCrashLogStatus()
            Toast.makeText(context, context.getString(R.string.crash_log_cleared), Toast.LENGTH_SHORT).show()
        }
    }

    fun shareCrashLog() {
        val context = getApplication<Application>()
        val log = readCrashLog()
        if (log == context.getString(R.string.no_crash_log_found)) return

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.crash_log_subject))
            putExtra(Intent.EXTRA_TEXT, log)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_crash_log)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun contactDeveloper() {
        val context = getApplication<Application>()
        updateCrashLogStatus()
        val log = readCrashLog()
        val hasLog = _crashLogExists.value
        val logFile = getCrashLogFile()
        
        val dateTime = if (hasLog && logFile.exists()) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(logFile.lastModified()))
        } else {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        }
        
        val subject = "Crashlog XenonLauncher - $dateTime"
        val body = if (hasLog && log != context.getString(R.string.no_crash_log_found)) log else "No crash log found."

        // Encode parameters into the URI - this is the most reliable way to fill subject and body in Gmail/Outlook
        val mailtoUri = "mailto:dinico.kustom@gmail.com" +
                "?subject=${Uri.encode(subject)}" +
                "&body=${Uri.encode(body)}"

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = mailtoUri.toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(emailIntent)
        } catch (_: Exception) {
            // Fallback: If the URI is too long or fails, use ACTION_SEND with a chooser
            try {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("dinico.kustom@gmail.com"))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Send Email...").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (_: Exception) {
                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun dumpMediaControls(): String {
        val manager = MediaControllerManager.instance
        return manager?.dumpMediaState() ?: getApplication<Application>().getString(R.string.media_manager_not_found)
    }

    fun dumpNotifications(): String {
        val service = XenonNotificationService.getInstance()
        val active = service?.safeActiveNotifications
        if (active.isNullOrEmpty()) return "No active notifications found in XenonNotificationService."

        val processed = NotificationManager.notifications.value

        val sb = StringBuilder()
        sb.append("--- NOTIFICATION DUMP (${active.size} items) ---\n")
        
        active.forEachIndexed { index, sbn ->
            val launcherNotif = processed.find { it.key == sbn.key }
            sb.append("\n[$index] Package: ${sbn.packageName}\n")
            sb.append("Key: ${sbn.key}\n")
            sb.append("ID: ${sbn.id}, Tag: ${sbn.tag}\n")
            sb.append("Post Time: ${sbn.postTime}\n")
            sb.append("Ongoing: ${sbn.isOngoing}, Clearable: ${sbn.isClearable}\n")
            
            val n = sbn.notification
            sb.append("Category: ${n.category}, Channel: ${n.channelId}, Template: ${n.extras.getString(
                Notification.EXTRA_TEMPLATE)}\n")
            
            sb.append("Flags: ${n.flags} (")
            if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) sb.append("GROUP_SUMMARY ")
            if ((n.flags and Notification.FLAG_ONGOING_EVENT) != 0) sb.append("ONGOING ")
            if ((n.flags and Notification.FLAG_NO_CLEAR) != 0) sb.append("NO_CLEAR ")
            if ((n.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0) sb.append("FOREGROUND_SERVICE ")
            sb.append(")\n")

            sb.append("Extras (Recursive):\n")
            dumpBundle(n.extras, sb, 2)

            sb.append("Actions:\n")
            n.actions?.forEachIndexed { aIndex, action ->
                sb.append("  - Action $aIndex: Title: ${action.title}, Intent: ${action.actionIntent != null}\n")
                sb.append("    Semantic Action: ${action.semanticAction}\n")
                if (action.extras != null && !action.extras.isEmpty) {
                    sb.append("    Action Extras:\n")
                    dumpBundle(action.extras, sb, 6)
                }
                action.remoteInputs?.forEach { ri ->
                    sb.append("    RemoteInput: Label=${ri.label}, ResultKey=${ri.resultKey}\n")
                }
            }
            
            sb.append("Icons:\n")
            sb.append("  - Small Icon: ${n.smallIcon}\n")
            sb.append("  - Large Icon: ${n.getLargeIcon()}\n")

            if (n.contentView != null) sb.append("Has contentView (RemoteViews)\n")
            if (n.bigContentView != null) sb.append("Has bigContentView (RemoteViews)\n")
            if (n.headsUpContentView != null) sb.append("Has headsUpContentView (RemoteViews)\n")
            
            sb.append("------------------------------------------\n")
        }

        return sb.toString()
    }

    private fun dumpBundle(bundle: Bundle, sb: StringBuilder, indent: Int) {
        val pad = " ".repeat(indent)
        try {
            val keys = bundle.keySet()
            keys.forEach { key ->
                try {
                    val value = bundle.get(key)
                    when {
                        value is Bundle -> {
                            sb.append("$pad- $key: Bundle (size=${value.size()})\n")
                            dumpBundle(value, sb, indent + 2)
                        }
                        value != null && value.javaClass.isArray -> {
                            val length = Array.getLength(value)
                            sb.append("$pad- $key: Array (length=$length)\n")
                            for (i in 0 until length) {
                                val item = Array.get(value, i)
                                if (item is Bundle) {
                                    sb.append("$pad  [$i]: Bundle\n")
                                    dumpBundle(item, sb, indent + 4)
                                } else {
                                    sb.append("$pad  [$i]: $item\n")
                                }
                            }
                        }
                        value is Bitmap -> sb.append("$pad- $key: Bitmap(${value.width}x${value.height}, ${value.byteCount} bytes)\n")
                        value is Icon -> sb.append("$pad- $key: Icon($value)\n")
                        else -> sb.append("$pad- $key: $value\n")
                    }
                } catch (e: Exception) {
                    sb.append("$pad- $key: [Error getting value: ${e.message}]\n")
                }
            }
        } catch (e: Exception) {
            sb.append("$pad- [Error dumping bundle keys: ${e.message}]\n")
        }
    }

    fun copyToClipboard(text: String) {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Xenon Debug Dump", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun triggerExampleDevActionThatRequiresRestart() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            Toast.makeText(
                context,
                context.getString(R.string.restart_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
