package com.xenonware.launcher

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.xenonware.launcher.ui.AppDrawer
import com.xenonware.launcher.ui.DockPill
import com.xenonware.launcher.ui.components.WallpaperView
import com.xenonware.launcher.ui.pages.MainHomePage
import com.xenonware.launcher.ui.pages.MediaPage
import com.xenonware.launcher.ui.pages.WidgetPage
import com.xenonware.launcher.ui.theme.XenonLauncherTheme
import com.xenonware.launcher.viewmodel.LauncherViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            XenonLauncherTheme {
                val permissions = mutableListOf(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                )
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                    permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }

                val permissionsState = rememberMultiplePermissionsState(permissions = permissions)
                
                LaunchedEffect(Unit) {
                    permissionsState.launchMultiplePermissionRequest()
                }

                val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    android.Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                }

                val storagePermissionState = permissionsState.permissions.find { it.permission == storagePermission }
                val isStorageGranted = storagePermissionState?.status?.isGranted ?: false
                
                val apps by viewModel.apps.collectAsState()
                
                LauncherScreen(
                    apps = apps,
                    onAppClick = { viewModel.launchApp(it) },
                    onOpenSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    isStoragePermissionGranted = isStorageGranted
                )
            }
        }
    }
}

@Composable
fun LauncherScreen(
    apps: List<com.xenonware.launcher.model.AppInfo>,
    onAppClick: (String) -> Unit,
    onOpenSettings: () -> Unit,
    isStoragePermissionGranted: Boolean
) {
    val hazeState = rememberHazeState()
    val pagerState = rememberPagerState(initialPage = 1) { 3 }
    var isAppDrawerVisible by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // CONTENT SOURCE - Unified container for blur sampling
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            WallpaperView(isStoragePermissionGranted = isStoragePermissionGranted)
            
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

        // OVERLAYS - Peer of the source box
        DockPill(
            modifier = Modifier.align(Alignment.BottomCenter),
            apps = apps,
            onAppClick = onAppClick,
            onSettingsClick = onOpenSettings,
            onFabClick = { isAppDrawerVisible = true }
        )

        AnimatedVisibility(
            visible = isAppDrawerVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            AppDrawer(
                apps = apps,
                onAppClick = onAppClick,
                onDismiss = { isAppDrawerVisible = false },
            )
        }
    }
}
