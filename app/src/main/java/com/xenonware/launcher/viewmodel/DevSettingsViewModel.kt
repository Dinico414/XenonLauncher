package com.xenonware.launcher.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xenonware.launcher.R
import com.xenonware.launcher.data.SharedPreferenceManager
import com.xenonware.launcher.media.MediaControllerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
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
