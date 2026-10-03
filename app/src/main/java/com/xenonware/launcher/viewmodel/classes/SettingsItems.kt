package com.xenonware.launcher.viewmodel.classes

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.xenon.mylibrary.R
import com.xenon.mylibrary.res.SettingsGoogleTile
import com.xenon.mylibrary.res.SettingsSwitchMenuTile
import com.xenon.mylibrary.res.SettingsSwitchTile
import com.xenon.mylibrary.res.SettingsTile
import com.xenon.mylibrary.theme.LayoutType
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.values.ExtraLargeSpacing
import com.xenon.mylibrary.values.ExtraLargerCornerRadius
import com.xenon.mylibrary.values.LargestPadding
import com.xenon.mylibrary.values.NoCornerRadius
import com.xenon.mylibrary.values.NoSpacing
import com.xenon.mylibrary.values.SmallCornerRadius
import com.xenon.mylibrary.values.SmallerSpacer
import com.xenonware.launcher.CustomizationActivity
import com.xenonware.launcher.R.string
import com.xenonware.launcher.presentation.sign_in.SignInState
import com.xenonware.launcher.viewmodel.LauncherViewModel
import com.xenonware.launcher.viewmodel.SettingsViewModel

@Composable
fun SettingsItems(
    viewModel: SettingsViewModel,
    currentThemeTitle: String,
    applyCoverTheme: Boolean,
    coverThemeEnabled: Boolean,
    currentLanguage: String,
    appVersion: String,
    layoutType: LayoutType = LayoutType.COMPACT,
    innerGroupRadius: Dp = SmallCornerRadius,
    outerGroupRadius: Dp = ExtraLargerCornerRadius,
    innerGroupSpacing: Dp = SmallerSpacer,
    outerGroupSpacing: Dp = ExtraLargeSpacing,
    tileBackgroundColor: Color = colorScheme.surfaceBright,
    tileContentColor: Color = colorScheme.onSurface,
    tileSubtitleColor: Color = colorScheme.onSurfaceVariant,
    tileShapeOverride: Shape? = null,
    switchColorsOverride: SwitchColors? = null,
    useGroupStyling: Boolean = true,
    state: SignInState,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onShowHiddenApps: () -> Unit,
    onNavigateToDeveloperOptions: () -> Unit,
    onConfigShortcut: (LauncherViewModel.ShortcutType) -> Unit,
    mainContextFont: FontFamily = QuicksandTitleVariable,
    subContextFont: FontFamily? = null,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updateAccessibilityRestriction()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    val blackedOutEnabled by viewModel.blackedOutModeEnabled.collectAsState()
    val blurEnabled by viewModel.blurEnabled.collectAsState()
    val developerModeEnabled by viewModel.developerModeEnabled.collectAsState()
    val isAccessibilityRestricted by viewModel.isAccessibilityRestricted.collectAsState()
    val timeShortcut by viewModel.timeShortcut.collectAsState()
    val dateShortcut by viewModel.dateShortcut.collectAsState()
    val weatherShortcut by viewModel.weatherShortcut.collectAsState()
    
    val userData = state.userData

    val actualInnerGroupRadius = if (useGroupStyling) innerGroupRadius else NoSpacing
    val actualOuterGroupRadius = if (useGroupStyling) outerGroupRadius else NoSpacing
    val actualInnerGroupSpacing = if (useGroupStyling) innerGroupSpacing else NoSpacing
    val actualOuterGroupSpacing = if (useGroupStyling) outerGroupSpacing else NoSpacing

    val defaultSwitchColors = SwitchDefaults.colors()

    val topShape = if (useGroupStyling) RoundedCornerShape(
        bottomStart = actualInnerGroupRadius,
        bottomEnd = actualInnerGroupRadius,
        topStart = actualOuterGroupRadius,
        topEnd = actualOuterGroupRadius
    ) else RoundedCornerShape(NoCornerRadius)

    val middleShape = if (useGroupStyling) RoundedCornerShape(
        topStart = actualInnerGroupRadius,
        topEnd = actualInnerGroupRadius,
        bottomStart = actualInnerGroupRadius,
        bottomEnd = actualInnerGroupRadius
    ) else RoundedCornerShape(NoCornerRadius)

    val bottomShape = if (useGroupStyling) RoundedCornerShape(
        topStart = actualInnerGroupRadius,
        topEnd = actualInnerGroupRadius,
        bottomStart = actualOuterGroupRadius,
        bottomEnd = actualOuterGroupRadius
    ) else RoundedCornerShape(NoCornerRadius)

    val standaloneShape = if (useGroupStyling) RoundedCornerShape(actualOuterGroupRadius)
    else RoundedCornerShape(NoCornerRadius)

    LaunchedEffect(key1 = state.signInError) {
        state.signInError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        }
    }


    // --- ACCOUNT ---
    SettingsGoogleTile(
        title = if (state.isSignInSuccessful) userData?.username ?: "Signed in" else stringResource(string.sign_in_with_google),
        subtitle = if (state.isSignInSuccessful) userData?.email else null,
        profilePictureUrl = userData?.profilePictureUrl,
        noAccIcon = painterResource(R.drawable.ic_default_icon),
        isSignedIn = state.isSignInSuccessful,
        onClick = if (state.isSignInSuccessful) onSignOutClick else onSignInClick,
        shape = tileShapeOverride ?: standaloneShape,
        backgroundColor = Color.Transparent,
        contentColor = tileContentColor,
        subtitleColor = tileSubtitleColor,
        mainContextFont = mainContextFont,
        subContextFont = subContextFont ?: mainContextFont,
        iconContentDescription = stringResource(string.profile_picture)
    )
    Spacer(Modifier.height(actualOuterGroupSpacing))

    // --- permission ---
    val isDefault = viewModel.isDefaultLauncher(context)
    if (!isDefault) {
        SettingsTile(
            title = stringResource(string.default_home),
            subtitle = stringResource(string.set_as_default_launcher),
            onClick = { viewModel.openLauncherSelector(context) },
            icon = { Icon(painterResource(R.drawable.ic_home), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = stringResource(string.accessibility_access),
            subtitle = if (isAccessibilityRestricted)
                stringResource(string.accessibility_restricted_description)
            else
                stringResource(string.accessibility_access_description),
            onClick = { viewModel.openAccessibilitySettings(context) },
            icon = {  Icon(painterResource(R.drawable.ic_accessibility), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: middleShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = stringResource(string.permissions),
            subtitle = stringResource(string.permissions_summary),
            onClick = { viewModel.setShowPermissionsDialog(true) },
            icon = { Icon(painterResource(R.drawable.ic_toggle), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont
        )
        Spacer(Modifier.height(actualOuterGroupSpacing))
    } else {
        SettingsTile(
            title = stringResource(string.accessibility_access),
            subtitle = if (isAccessibilityRestricted)
                stringResource(string.accessibility_restricted_description)
            else
                stringResource(string.accessibility_access_description),
            onClick = { viewModel.openAccessibilitySettings(context) },
            icon = {  Icon(painterResource(R.drawable.ic_accessibility), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = stringResource(string.permissions),
            subtitle = stringResource(string.permissions_summary),
            onClick = { viewModel.setShowPermissionsDialog(true) },
            icon = { Icon(painterResource(R.drawable.ic_toggle), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont
        )
        Spacer(Modifier.height(actualOuterGroupSpacing))
    }

    // --- theming ---
    Column {
        SettingsTile(
            title = stringResource(string.theme),
            subtitle = "${stringResource(string.current)} $currentThemeTitle",
            onClick = { viewModel.onThemeSettingClicked() },
            icon = { Icon(painterResource(R.drawable.ic_themes), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsSwitchTile(
            title = stringResource(string.blacked_out),
            subtitle = stringResource(string.blacked_out_description),
            checked = blackedOutEnabled,
            onCheckedChange = { viewModel.setBlackedOutEnabled(it) },
            onClick = { viewModel.setBlackedOutEnabled(!blackedOutEnabled) },
            icon = { Icon(painterResource(R.drawable.ic_blacked_out), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: middleShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont,
            switchColors = switchColorsOverride ?: defaultSwitchColors
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsSwitchTile(
            title = stringResource(string.blur_effect),
            subtitle = stringResource(string.enable_glass_haze),
            checked = blurEnabled,
            onCheckedChange = { viewModel.setBlurEnabled(it) },
            onClick = { viewModel.setBlurEnabled(!blurEnabled) },
            icon = { Icon(painterResource(R.drawable.ic_blur), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: middleShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont,
            switchColors = switchColorsOverride ?: defaultSwitchColors
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsSwitchMenuTile(
            title = stringResource(string.cover_screen_mode),
            subtitle = "${stringResource(string.selected_cover_screen)}\n(${if (applyCoverTheme) stringResource(string.active) else stringResource(string.inactive)})",
            checked = coverThemeEnabled,
            onCheckedChange = { viewModel.setCoverThemeEnabled(it) },
            onClick = { viewModel.onCoverThemeClicked() },
            icon = { Icon(painterResource(R.drawable.ic_cover_screen), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont,
            switchColors = switchColorsOverride ?: defaultSwitchColors
        )
    }
    Spacer(Modifier.height(actualOuterGroupSpacing))

    // --- Language ---
    SettingsTile(
        title = stringResource(string.language),
        subtitle = "${stringResource(string.current)} $currentLanguage",
        onClick = { viewModel.onLanguageSettingClicked(context) },
        icon = { Icon(painterResource(R.drawable.ic_language), null, tint = tileSubtitleColor) },
        shape = tileShapeOverride ?: standaloneShape,
        backgroundColor = tileBackgroundColor,
        contentColor = tileContentColor,
        subtitleColor = tileSubtitleColor,
        mainContextFont = mainContextFont,
        subContextFont = subContextFont
    )
    Spacer(Modifier.height(actualOuterGroupSpacing))

    // --- At a Glance & Notification Manager ---
    Column {
        SettingsTile(
            title = stringResource(string.at_a_glance),
            subtitle = stringResource(string.at_a_glance_description),
            onClick = { viewModel.setShowCalendarSelectionDialog(true) },
            icon = { Icon(painterResource(R.drawable.ic_at_a_glance), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = stringResource(string.notification_manager),
            subtitle = stringResource(string.notification_manager_description),
            onClick = { viewModel.setShowNotificationManagerDialog(true) },
            icon = { Icon(painterResource(R.drawable.ic_notification_manager), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont
        )
    }
    Spacer(Modifier.height(actualOuterGroupSpacing))

    // --- Customization ---
    SettingsTile(
        title = stringResource(string.customization),
        subtitle = stringResource(string.customization_description),
        onClick = {
            val intent = Intent(context, CustomizationActivity::class.java)
            context.startActivity(intent)
        },
        icon = { Icon(painterResource(R.drawable.ic_tune), null, tint = tileSubtitleColor) },
        shape = tileShapeOverride ?: standaloneShape,
        backgroundColor = tileBackgroundColor,
        contentColor = tileContentColor,
        subtitleColor = tileSubtitleColor,
        mainContextFont = mainContextFont,
        subContextFont = subContextFont
    )
    Spacer(Modifier.height(actualOuterGroupSpacing))

    // --- shortcuts ---
    Column {
        SettingsTile(
            title = stringResource(string.time_shortcut),
            subtitle = timeShortcut.ifEmpty { stringResource(string.not_set) },
            onClick = { onConfigShortcut(LauncherViewModel.ShortcutType.TIME) },
            icon = {  Icon(painterResource(R.drawable.ic_time), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = stringResource(string.date_shortcut),
            subtitle = dateShortcut.ifEmpty { stringResource(string.not_set) },
            onClick = { onConfigShortcut(LauncherViewModel.ShortcutType.DATE) },
            icon = {  Icon(painterResource(R.drawable.ic_date), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: middleShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = stringResource(string.weather_shortcut),
            subtitle = weatherShortcut.ifEmpty { stringResource(string.not_set) },
            onClick = { onConfigShortcut(LauncherViewModel.ShortcutType.WEATHER) },
            icon = {  Icon(painterResource(R.drawable.ic_weater), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont
        )
    }
    Spacer(Modifier.height(actualOuterGroupSpacing))

    // --- system ---
    Column {
        SettingsTile(
            title = stringResource(string.backup_and_restore),
            subtitle = stringResource(string.backup_and_restore_description),
            onClick = { viewModel.setShowBackupDialog(true) },
            icon = { Icon(painterResource(R.drawable.ic_backup), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: topShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = stringResource(string.clear_data),
            subtitle = stringResource(string.clear_data_description),
            onClick = { viewModel.onClearDataClicked(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
            icon = { Icon(painterResource(R.drawable.ic_reset), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: middleShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = stringResource(string.reset_settings),
            subtitle = stringResource(string.reset_all_settings_description),
            onClick = { viewModel.onResetSettingsClicked(); haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
            icon = { Icon(painterResource(R.drawable.ic_reset_settings), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: middleShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont
        )
        Spacer(Modifier.height(actualInnerGroupSpacing))
        Box(contentAlignment = Alignment.CenterEnd) {
            SettingsTile(
                title = stringResource(string.privacy_policy_title),
                subtitle = stringResource(string.privacy_policy_description),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, "https://xenonware.com/privacy_policy_launcher".toUri())
                    context.startActivity(intent)
                },
                icon = { Icon(painterResource(R.drawable.ic_privacy), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: middleShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = tileSubtitleColor,
                modifier = Modifier.padding(end = LargestPadding)
            )
        }
        Spacer(Modifier.height(actualInnerGroupSpacing))
        SettingsTile(
            title = stringResource(string.version),
            subtitle = "v $appVersion" + if (developerModeEnabled) " (${stringResource(string.developer)})" else "",
            onClick = { viewModel.onInfoTileClicked() },
            onLongClick = { viewModel.openImpressum(context) },
            icon = { Icon(painterResource(R.drawable.ic_info), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: bottomShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont
        )
        Spacer(Modifier.height(actualOuterGroupSpacing))
        Box(contentAlignment = Alignment.CenterEnd) {
            SettingsTile(
                title = stringResource(string.buy_me_a_coffee),
                subtitle = stringResource(string.buy_me_a_coffee_description),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, "https://www.buymeacoffee.com/xenonware".toUri())
                    context.startActivity(intent)
                },
                icon = { Icon(painterResource(R.drawable.ic_buy_me_a_coffee), null, tint = tileSubtitleColor) },
                shape = tileShapeOverride ?: standaloneShape,
                backgroundColor = tileBackgroundColor,
                contentColor = tileContentColor,
                subtitleColor = tileSubtitleColor,
                mainContextFont = mainContextFont,
                subContextFont = subContextFont
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = tileSubtitleColor,
                modifier = Modifier.padding(end = LargestPadding)
            )
        }
    }

    // --- dev ---
    if (developerModeEnabled) {
        Spacer(Modifier.height(actualOuterGroupSpacing))
        SettingsTile(
            title = stringResource(string.developer_options_title),
            subtitle = stringResource(string.dev_settings_description),
            onClick = onNavigateToDeveloperOptions,
            icon = { Icon(painterResource(R.drawable.ic_developer), null, tint = tileSubtitleColor) },
            shape = tileShapeOverride ?: standaloneShape,
            backgroundColor = tileBackgroundColor,
            contentColor = tileContentColor,
            subtitleColor = tileSubtitleColor,
            mainContextFont = mainContextFont,
            subContextFont = subContextFont
        )
    }
}
