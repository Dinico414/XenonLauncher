package com.xenonware.launcher.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xenonware.launcher.data.SharedPreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DevSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferenceManager = SharedPreferenceManager(application)

    private val _devModeToggleState = MutableStateFlow(sharedPreferenceManager.developerModeEnabled)
    val devModeToggleState: StateFlow<Boolean> = _devModeToggleState.asStateFlow()

    fun setDeveloperModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sharedPreferenceManager.developerModeEnabled = enabled
            _devModeToggleState.value = enabled
        }
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
