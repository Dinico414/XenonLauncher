package com.xenonware.launcher

import android.Manifest.permission
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.provider.Settings.Secure
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.xenon.mylibrary.activity.BasePermissionActivity
import com.xenon.mylibrary.res.AnimatedGradientBackground
import com.xenon.mylibrary.res.PermissionScreen
import com.xenon.mylibrary.res.XenonIcon
import com.xenon.mylibrary.utils.PermissionItem
import com.xenonware.launcher.accessibility.XenonAccessibilityService
import com.xenonware.launcher.data.SharedPreferenceManager
import com.xenonware.launcher.ui.theme.FontAxes
import com.xenonware.launcher.ui.theme.FontType
import com.xenonware.launcher.ui.theme.XenonTheme
import com.xenonware.launcher.ui.theme.createCustomFontFamily
import com.xenonware.launcher.util.AccessibilityUtils

class PermissionActivity : BasePermissionActivity() {

    private val sharedPreferenceManager by lazy { SharedPreferenceManager(this) }
    private val currentPermissionName = mutableStateOf("")
    private val refreshTrigger = mutableIntStateOf(0)

    override fun onResume() {
        super.onResume()
        refreshTrigger.intValue++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val fontType = sharedPreferenceManager.fontType
            val mainFontType = sharedPreferenceManager.mainFontType
            val robotoSettings = sharedPreferenceManager.robotoFlexSettings
            val googleSansSettings = sharedPreferenceManager.googleSansFlexSettings

            val customFontFamily = remember(fontType, robotoSettings, googleSansSettings) {
                createCustomFontFamily(
                    fontType = FontType.fromId(fontType),
                    robotoSettings = FontAxes.parseSettings(
                        robotoSettings,
                        FontAxes.ROBOTO_FLEX_AXES
                    ),
                    googleSansSettings = FontAxes.parseSettings(
                        googleSansSettings,
                        FontAxes.GOOGLE_SANS_AXES
                    )
                )
            }

            val customMainFontFamily = remember(mainFontType, robotoSettings, googleSansSettings) {
                createCustomFontFamily(
                    fontType = FontType.fromId(mainFontType),
                    robotoSettings = FontAxes.parseSettings(
                        robotoSettings,
                        FontAxes.ROBOTO_FLEX_AXES
                    ),
                    googleSansSettings = FontAxes.parseSettings(
                        googleSansSettings,
                        FontAxes.GOOGLE_SANS_AXES
                    )
                )
            }

            XenonTheme(
                darkTheme = isSystemInDarkTheme(),
                useBlackedOutDarkTheme = false,
                isCoverMode = false,
                dynamicColor = true,
                fontFamily = customFontFamily,
                mainContextFont = customMainFontFamily
            ) {
                AnimatedGradientBackground(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        val trigger by refreshTrigger
                        val permissions = remember(trigger) { getPermissions() }
                        PermissionScreen(
                            permissions = permissions,
                            isFirstLaunch = isFirstLaunch(),
                            grantButtonText = if (currentPermissionName.value == getString(R.string.default_home)) {
                                getString(R.string.set_as_home)
                            } else {
                                getString(R.string.grant_permission)
                            },
                            skipIcon = XenonIcon { modifier ->
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = modifier.size(24.dp)
                                )
                            },
                            onFinish = { onPermissionsFinished() },
                            mainContextFont = customMainFontFamily,
                            subContextFont = customFontFamily
                        )
                    }
                }
            }
        }
    }

    override fun isFirstLaunch(): Boolean = sharedPreferenceManager.isFirstLaunch

    override fun getPermissions(): List<PermissionItem> = buildList {
        // Notification Access
        add(PermissionItem(
            name = getString(R.string.notification_access),
            description = getString(R.string.notification_access_description),
            isGranted = {
                val enabled = Secure.getString(it.contentResolver, "enabled_notification_listeners")
                val granted = enabled?.contains(it.packageName) == true
                if (!granted) currentPermissionName.value = getString(R.string.notification_access)
                granted
            },
            request = {
                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            }
        ))

        // Accessibility Access
        add(PermissionItem(
            name = getString(R.string.accessibility_access),
            description = if (AccessibilityUtils.isAccessibilityRestricted(this@PermissionActivity))
                getString(R.string.accessibility_restricted_description)
            else
                getString(R.string.accessibility_access_description),
            isGranted = {
                val expectedComponentName = ComponentName(it, XenonAccessibilityService::class.java)
                val enabledServices = Secure.getString(it.contentResolver, Secure.ENABLED_ACCESSIBILITY_SERVICES)
                val granted = enabledServices?.contains(expectedComponentName.flattenToString()) == true
                if (!granted) currentPermissionName.value = getString(R.string.accessibility_access)
                granted
            },
            request = {
                AccessibilityUtils.requestAccessibility(it)
            },
            guideText = getString(R.string.accessibility_guide),
            onInfoClick = {
                AccessibilityUtils.openAppInfo(it)
            }
        ))

        // Location Access
        add(PermissionItem(
            name = getString(R.string.location_access),
            description = getString(R.string.location_access_description),
            isGranted = {
                val granted = ContextCompat.checkSelfPermission(it, permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (!granted) currentPermissionName.value = getString(R.string.location_access)
                granted
            },
            request = {
                requestPermissions(arrayOf(permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION), 101)
            }
        ))

        // Calendar Access
        add(PermissionItem(
            name = getString(R.string.calendar_access),
            description = getString(R.string.calendar_access_description),
            isGranted = {
                val granted = ContextCompat.checkSelfPermission(it, permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                if (!granted) currentPermissionName.value = getString(R.string.calendar_access)
                granted
            },
            request = {
                requestPermissions(arrayOf(permission.READ_CALENDAR, permission.WRITE_CALENDAR), 102)
            }
        ))

        // Storage/Media Access
        add(PermissionItem(
            name = getString(R.string.storage_access),
            description = getString(R.string.storage_access_description),
            isGranted = {
                val granted = ContextCompat.checkSelfPermission(it, permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                if (!granted) currentPermissionName.value = getString(R.string.storage_access)
                granted
            },
            request = {
                requestPermissions(arrayOf(permission.READ_MEDIA_IMAGES, permission.READ_MEDIA_AUDIO), 103)
            }
        ))

        // Contacts Access
        add(PermissionItem(
            name = getString(R.string.contacts_access),
            description = getString(R.string.contacts_access_description),
            isGranted = {
                val granted = ContextCompat.checkSelfPermission(it, permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                if (!granted) currentPermissionName.value = getString(R.string.contacts_access)
                granted
            },
            request = {
                requestPermissions(arrayOf(permission.READ_CONTACTS), 104)
            }
        ))

        // Post Notifications
        add(PermissionItem(
            name = getString(R.string.post_notifications),
            description = getString(R.string.post_notifications_description),
            isGranted = {
                val granted = ContextCompat.checkSelfPermission(it, permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (!granted) currentPermissionName.value = getString(R.string.post_notifications)
                granted
            },
            request = {
                requestPermissions(arrayOf(permission.POST_NOTIFICATIONS), 105)
            }
        ))

        // All Files Access
        add(PermissionItem(
            name = getString(R.string.all_files_access),
            description = getString(R.string.all_files_access_description),
            isGranted = {
                val granted = Environment.isExternalStorageManager()
                if (!granted) currentPermissionName.value = getString(R.string.all_files_access)
                granted
            },
            request = {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = "package:${it.packageName}".toUri()
                }
                startActivity(intent)
            }
        ))

        // Default Launcher
        add(PermissionItem(
            name = getString(R.string.default_home),
            description = getString(R.string.set_as_default_launcher),
            isGranted = { context ->
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                val granted = resolveInfo?.activityInfo?.packageName == context.packageName
                if (!granted) currentPermissionName.value = getString(R.string.default_home)
                granted
            },
            request = { context ->
                val intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_HOME)
                val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                val isAlreadyDefault = resolveInfo?.activityInfo?.packageName == context.packageName

                if (!isAlreadyDefault) {
                    val homeSettingsIntent = Intent(Settings.ACTION_HOME_SETTINGS)
                    homeSettingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(homeSettingsIntent)
                    } catch (_: Exception) {
                        val selectorIntent = Intent(Intent.ACTION_MAIN)
                        selectorIntent.addCategory(Intent.CATEGORY_HOME)
                        selectorIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(selectorIntent)
                    }
                }
            }
        ))
    }

    override fun onPermissionsFinished() {
        if (sharedPreferenceManager.isFirstLaunch) {
            startActivity(Intent(this, WelcomeActivity::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
