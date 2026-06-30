package com.xenonware.launcher

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.xenonware.launcher.ui.AppDrawer
import com.xenonware.launcher.ui.DockPill
import com.xenonware.launcher.ui.pages.MainHomePage
import com.xenonware.launcher.ui.pages.MediaPage
import com.xenonware.launcher.ui.pages.WidgetPage
import com.xenonware.launcher.ui.theme.XenonLauncherTheme
import com.xenonware.launcher.util.WindowBlurBehind
import com.xenonware.launcher.util.rememberBlurAvailable
import com.xenonware.launcher.viewmodel.LauncherViewModel
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)

        setContent {
            XenonLauncherTheme {
                val permissions = mutableListOf(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                    permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
                }

                val permissionsState = rememberMultiplePermissionsState(permissions = permissions)

                LaunchedEffect(Unit) {
                    permissionsState.launchMultiplePermissionRequest()
                }

                val apps by viewModel.apps.collectAsState()
                val currentTime by viewModel.currentTime.collectAsState()
                val weatherState by viewModel.weatherState.collectAsState()
                val notificationCount by viewModel.notificationCount.collectAsState()
                val batteryLevel by viewModel.batteryLevel.collectAsState()

                LauncherScreen(
                    viewModel = viewModel,
                    apps = apps,
                    currentTime = currentTime.format(viewModel.timeFormatter),
                    currentDate = currentTime.format(viewModel.dateFormatter),
                    weatherTemp = weatherState.temperature,
                    weatherCondition = weatherState.condition,
                    notificationCount = notificationCount,
                    batteryLevel = batteryLevel,
                    onAppClick = { viewModel.launchApp(it) },
                    onOpenSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    apps: List<com.xenonware.launcher.model.AppInfo>,
    currentTime: String,
    currentDate: String,
    weatherTemp: String,
    weatherCondition: String,
    notificationCount: Int,
    batteryLevel: Float,
    onAppClick: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val hazeState = rememberHazeState()
    val pagerState = rememberPagerState(initialPage = 1) { 3 }
    var isAppDrawerVisible by remember { mutableStateOf(false) }

    val contentBlur by animateFloatAsState(
        targetValue = if (isAppDrawerVisible) 20f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "contentBlur"
    )

    val blurAvailable = rememberBlurAvailable()

    WindowBlurBehind(
        targetRadiusPx = if (isAppDrawerVisible) 30 else 0,
        durationMillis = 200
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = contentBlur.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    when (page) {
                        0 -> MediaPage()
                        1 -> MainHomePage()
                        2 -> WidgetPage()
                    }
                }
            }

            // APP LIST
            AnimatedVisibility(
                visible = isAppDrawerVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                AppDrawer(
                    apps = apps,
                    containerColor = if (blurAvailable) {
                        val lerp = if (isSystemInDarkTheme()) 0.5f else 0.15f
                        lerp(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f), Color.Black.copy(alpha = 0.2f), lerp)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f)
                    },
                    onAppClick = onAppClick,
                    onSettingsClick = onOpenSettings,
                    onDismiss = { isAppDrawerVisible = false }
                )
            }
        }

        // DOCK LAYER
        DockPill(
            modifier = Modifier.align(Alignment.BottomCenter),
            apps = apps,
            mediaState = viewModel.mediaState,
            isMediaPermissionGranted = viewModel.isMediaPermissionGranted,
            notificationCount = notificationCount,
            currentTime = currentTime,
            currentDate = currentDate,
            weatherTemp = weatherTemp,
            weatherCondition = weatherCondition,
            onAppClick = onAppClick,
            onSettingsClick = onOpenSettings,
            onFabClick = { isAppDrawerVisible = !isAppDrawerVisible },
            onMediaPlayPause = { viewModel.togglePlayPause() },
            onMediaSkipNext = { viewModel.skipNext() },
            onOpenMediaPermission = { viewModel.openNotificationAccessSettings() },
            isAppDrawerVisible = isAppDrawerVisible,
            hazeState = hazeState,
            progress = batteryLevel
        )
    }
}