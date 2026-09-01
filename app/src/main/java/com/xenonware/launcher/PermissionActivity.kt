package com.xenonware.launcher

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.xenon.mylibrary.activity.BasePermissionActivity
import com.xenon.mylibrary.res.AnimatedGradientBackground
import com.xenon.mylibrary.res.PermissionScreen
import com.xenon.mylibrary.theme.XenonTheme
import com.xenon.mylibrary.utils.PermissionItem
import com.xenonware.launcher.accessibility.XenonAccessibilityService
import com.xenonware.launcher.data.SharedPreferenceManager
import com.xenonware.launcher.util.AccessibilityUtils

class PermissionActivity : BasePermissionActivity() {

    private val sharedPreferenceManager by lazy { SharedPreferenceManager(this) }
    private val currentPermissionName = mutableStateOf("")
    private val refreshTrigger = mutableStateOf(0)

    override fun onResume() {
        super.onResume()
        refreshTrigger.value++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            XenonTheme(
                darkTheme = isSystemInDarkTheme(),
                useBlackedOutDarkTheme = false,
                isCoverMode = false,
                dynamicColor = true
            ) {
                AnimatedGradientBackground(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    var showGuide by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Transparent
                        ) {
                            val trigger by refreshTrigger
                            val permissions = remember(trigger) { getPermissions() }
                            PermissionScreen(
                                permissions = permissions,
                                isFirstLaunch = isFirstLaunch(),
                                grantButtonText = if (currentPermissionName.value == getString(R.string.default_home)) getString(R.string.set_as_home) else getString(R.string.grant_permission),
                                onFinish = { onPermissionsFinished() }
                            )
                        }

                        if (currentPermissionName.value == getString(R.string.accessibility_access)) {
                            if (showGuide) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { showGuide = false }
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (showGuide) {
                                        AccessibilityUtils.openAppInfo(context)
                                    } else {
                                        showGuide = true
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 48.dp, end = 16.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Info,
                                    contentDescription = "Accessibility Guide",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            AnimatedVisibility(
                                visible = showGuide,
                                enter = fadeIn(),
                                exit = fadeOut(),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 100.dp)
                                    .padding(horizontal = 32.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.accessibility_guide),
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
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
                val enabled = Settings.Secure.getString(it.contentResolver, "enabled_notification_listeners")
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
                val enabledServices = Settings.Secure.getString(it.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                val granted = enabledServices?.contains(expectedComponentName.flattenToString()) == true
                if (!granted) currentPermissionName.value = getString(R.string.accessibility_access)
                granted
            },
            request = {
                AccessibilityUtils.requestAccessibility(it)
            }
        ))

        // Location Access
        add(PermissionItem(
            name = getString(R.string.location_access),
            description = getString(R.string.location_access_description),
            isGranted = {
                val granted = ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (!granted) currentPermissionName.value = getString(R.string.location_access)
                granted
            },
            request = {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), 101)
            }
        ))

        // Calendar Access
        add(PermissionItem(
            name = getString(R.string.calendar_access),
            description = getString(R.string.calendar_access_description),
            isGranted = {
                val granted = ContextCompat.checkSelfPermission(it, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                if (!granted) currentPermissionName.value = getString(R.string.calendar_access)
                granted
            },
            request = {
                requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR), 102)
            }
        ))

        // Storage/Media Access
        add(PermissionItem(
            name = getString(R.string.storage_access),
            description = getString(R.string.storage_access_description),
            isGranted = {
                val granted = ContextCompat.checkSelfPermission(it, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                if (!granted) currentPermissionName.value = getString(R.string.storage_access)
                granted
            },
            request = {
                requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_AUDIO), 103)
            }
        ))

        // Contacts Access
        add(PermissionItem(
            name = getString(R.string.contacts_access),
            description = getString(R.string.contacts_access_description),
            isGranted = {
                val granted = ContextCompat.checkSelfPermission(it, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                if (!granted) currentPermissionName.value = getString(R.string.contacts_access)
                granted
            },
            request = {
                requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 104)
            }
        ))

        // Post Notifications
        add(PermissionItem(
            name = getString(R.string.post_notifications),
            description = getString(R.string.post_notifications_description),
            isGranted = {
                val granted = ContextCompat.checkSelfPermission(it, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (!granted) currentPermissionName.value = getString(R.string.post_notifications)
                granted
            },
            request = {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 105)
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
