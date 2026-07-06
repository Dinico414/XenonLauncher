package com.xenonware.launcher.viewmodel

import android.app.Application
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import com.xenonware.launcher.data.SharedPreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferenceManager = SharedPreferenceManager(application)

    private val _currentThemeTitle = MutableStateFlow(
        when(sharedPreferenceManager.theme) {
            0 -> "Light"
            1 -> "Dark"
            else -> "System"
        }
    )
    val currentThemeTitle = _currentThemeTitle.asStateFlow()

    private val _blackedOutModeEnabled = MutableStateFlow(sharedPreferenceManager.blurEnabled)
    val blackedOutModeEnabled = _blackedOutModeEnabled.asStateFlow()

    private val _isGridLayout = MutableStateFlow(sharedPreferenceManager.isGridLayout)
    val isGridLayout = _isGridLayout.asStateFlow()

    private val _openKeyboard = MutableStateFlow(sharedPreferenceManager.openKeyboard)
    val openKeyboard = _openKeyboard.asStateFlow()

    private val _advancedSearchEnabled = MutableStateFlow(sharedPreferenceManager.advancedSearchEnabled)
    val advancedSearchEnabled = _advancedSearchEnabled.asStateFlow()

    private val _hasWallpaperAccess = MutableStateFlow(checkWallpaperAccess())
    val hasWallpaperAccess = _hasWallpaperAccess.asStateFlow()

    private val _currentLanguage = MutableStateFlow("English")
    val currentLanguage = _currentLanguage.asStateFlow()

    private fun checkWallpaperAccess(): Boolean {
        val context = getApplication<Application>()
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED ||
                        Environment.isExternalStorageManager()
            }
            else -> {
                context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun onThemeSettingClicked() { /* TODO */ }
    
    fun setBlackedOutEnabled(enabled: Boolean) { 
        sharedPreferenceManager.blurEnabled = enabled
        _blackedOutModeEnabled.value = enabled 
    }

    fun setGridLayout(enabled: Boolean) {
        sharedPreferenceManager.isGridLayout = enabled
        _isGridLayout.value = enabled
    }

    fun setOpenKeyboard(enabled: Boolean) {
        sharedPreferenceManager.openKeyboard = enabled
        _openKeyboard.value = enabled
    }

    fun setAdvancedSearchEnabled(enabled: Boolean) {
        sharedPreferenceManager.advancedSearchEnabled = enabled
        _advancedSearchEnabled.value = enabled
    }

    fun onLanguageSettingClicked() { /* TODO */ }
    fun onClearDataClicked() { /* TODO */ }
    fun onResetSettingsClicked() { /* TODO */ }
    fun onSignOutClicked() { /* TODO */ }

    fun requestWallpaperAccess(context: android.content.Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val genericIntent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(genericIntent)
            }
        }
    }

    fun refreshWallpaperAccess() {
        _hasWallpaperAccess.value = checkWallpaperAccess()
    }
}
