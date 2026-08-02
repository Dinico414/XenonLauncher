package com.xenonware.launcher.ui.layouts.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import com.xenonware.launcher.presentation.sign_in.GoogleAuthUiClient
import com.xenonware.launcher.presentation.sign_in.SignInState
import com.xenonware.launcher.viewmodel.LayoutType
import com.xenonware.launcher.viewmodel.SettingsViewModel

@Composable
fun SettingsLayout(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel,
    isLandscape: Boolean,
    layoutType: LayoutType,
    onNavigateToDeveloperOptions: () -> Unit,
    modifier: Modifier = Modifier,
    state: SignInState,
    googleAuthUiClient: GoogleAuthUiClient,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onConfirmSignOut: () -> Unit,
    appSize: IntSize,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (layoutType) {
            LayoutType.COVER -> {
                CoverSettings(
                    onNavigateBack = onNavigateBack,
                    viewModel = viewModel,
                    onNavigateToDeveloperOptions = onNavigateToDeveloperOptions,
                    state = state,
                    googleAuthUiClient = googleAuthUiClient,
                    onSignInClick = onSignInClick,
                    onSignOutClick = onSignOutClick,
                    onConfirmSignOut = onConfirmSignOut
                )
            }

            else -> {
                DefaultSettings(
                    onNavigateBack = onNavigateBack,
                    viewModel = viewModel,
                    layoutType = layoutType,
                    isLandscape = isLandscape,
                    onNavigateToDeveloperOptions = onNavigateToDeveloperOptions,
                    state = state,
                    googleAuthUiClient = googleAuthUiClient,
                    onSignInClick = onSignInClick,
                    onSignOutClick = onSignOutClick,
                    onConfirmSignOut = onConfirmSignOut,
                    appSize = appSize
                )
            }
        }
    }
}
