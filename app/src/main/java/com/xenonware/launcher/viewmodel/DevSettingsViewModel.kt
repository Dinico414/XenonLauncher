package com.xenonware.launcher.viewmodel

import android.app.Application
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xenonware.launcher.data.SharedPreferenceManager
import com.xenonware.launcher.media.MediaControllerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

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
        return if (file.exists()) file.readText() else "No crash log found."
    }

    fun clearCrashLog() {
        val file = getCrashLogFile()
        if (file.exists()) {
            file.delete()
            updateCrashLogStatus()
            Toast.makeText(getApplication(), "Crash log cleared.", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareCrashLog() {
        val log = readCrashLog()
        if (log == "No crash log found.") return

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Xenon Launcher Crash Log")
            putExtra(Intent.EXTRA_TEXT, log)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        getApplication<Application>().startActivity(Intent.createChooser(intent, "Share Crash Log").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun dumpMediaControls(): String {
        val manager = MediaControllerManager.instance
        return manager?.dumpMediaState() ?: "MediaControllerManager instance not found."
    }

    fun triggerExampleDevActionThatRequiresRestart() {
        viewModelScope.launch {
            Toast.makeText(
                getApplication(),
                "To apply changes, restart the app.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
