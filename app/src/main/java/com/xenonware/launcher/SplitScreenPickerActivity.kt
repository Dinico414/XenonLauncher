package com.xenonware.launcher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.xenonware.launcher.data.SharedPreferenceManager
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.ui.res.IconShape
import com.xenonware.launcher.ui.theme.FontAxes
import com.xenonware.launcher.ui.theme.FontType
import com.xenonware.launcher.ui.theme.XenonTheme
import com.xenonware.launcher.ui.theme.createCustomFontFamily
import com.xenonware.launcher.ui.theme.mainFontFamily
import com.xenonware.launcher.util.matches
import com.xenonware.launcher.viewmodel.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SplitScreenPickerActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_FIRST_PACKAGE = "first_package"
        private const val STATE_FIRST_LAUNCHED = "first_launched"

        fun intent(context: Context, firstPackage: String?): Intent =
            Intent(context, SplitScreenPickerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                if (firstPackage != null) putExtra(EXTRA_FIRST_PACKAGE, firstPackage)
            }
    }

    private var firstAppLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        firstAppLaunched = savedInstanceState?.getBoolean(STATE_FIRST_LAUNCHED) ?: false

        val prefs = SharedPreferenceManager(application)
        val iconShape = runCatching { IconShape.valueOf(prefs.drawerIconShape) }.getOrNull()
        val showLabels = prefs.appLabelsEnabled
        val hiddenApps = prefs.hiddenApps.toSet()

        setContent {
            val fontType = prefs.fontType
            val mainFontType = prefs.mainFontType
            val robotoSettings = prefs.robotoFlexSettings
            val googleSansSettings = prefs.googleSansFlexSettings

            val customFontFamily = remember(fontType, robotoSettings, googleSansSettings) {
                createCustomFontFamily(
                    fontType = FontType.fromId(fontType), robotoSettings = FontAxes.parseSettings(
                        robotoSettings, FontAxes.ROBOTO_FLEX_AXES
                    ), googleSansSettings = FontAxes.parseSettings(
                        googleSansSettings, FontAxes.GOOGLE_SANS_AXES
                    )
                )
            }

            val customMainFontFamily = remember(mainFontType, robotoSettings, googleSansSettings) {
                createCustomFontFamily(
                    fontType = FontType.fromId(mainFontType),
                    robotoSettings = FontAxes.parseSettings(
                        robotoSettings, FontAxes.ROBOTO_FLEX_AXES
                    ),
                    googleSansSettings = FontAxes.parseSettings(
                        googleSansSettings, FontAxes.GOOGLE_SANS_AXES
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
                SplitScreenPicker(
                    iconShape = iconShape,
                    showLabels = showLabels,
                    hiddenApps = hiddenApps,
                    onAppClick = ::launchSecondApp,
                    onClose = ::finish
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val firstPackage = intent.getStringExtra(EXTRA_FIRST_PACKAGE)
        if (firstPackage != null && !firstAppLaunched) {
            firstAppLaunched = true
            window.decorView.post { launchFirstApp(firstPackage) }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_FIRST_LAUNCHED, firstAppLaunched)
    }

    override fun onStop() {
        super.onStop()
        if (firstAppLaunched && !isInMultiWindowMode && !isChangingConfigurations) {
            finish()
        }
    }

    private fun launchFirstApp(packageName: String) {
        val launch = if (packageName.contains("/")) {
            val parts = packageName.split("/")
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(parts[0], parts[1])
            }
        } else {
            packageManager.getLaunchIntentForPackage(packageName)
        }
        if (launch == null) {
            finish()
            return
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        runCatching { startActivity(launch) }.onFailure { finish() }
    }

    private fun launchSecondApp(packageName: String) {
        val launch = if (packageName.contains("/")) {
            val parts = packageName.split("/")
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(parts[0], parts[1])
            }
        } else {
            packageManager.getLaunchIntentForPackage(packageName)
        } ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(launch) }
        finish()
    }
}

@Composable
private fun SplitScreenPicker(
    iconShape: IconShape?,
    showLabels: Boolean,
    hiddenApps: Set<String>,
    onAppClick: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val sharedApps by LauncherViewModel.launchableApps.collectAsState()
    var fallbackApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(sharedApps.isEmpty()) {
        if (sharedApps.isEmpty() && fallbackApps.isEmpty()) {
            fallbackApps = loadAppsFallback(context, hiddenApps)
        }
    }

    val apps = sharedApps.ifEmpty { fallbackApps }
    var query by remember { mutableStateOf("") }
    val shown = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { it.matches(query) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.split_screen_pick_app),
            style = typography.titleMedium.copy(fontFamily = mainFontFamily),
            color = colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(colorScheme.surfaceContainer),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose, modifier = Modifier.padding(4.dp)) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.split_screen_close),
                    tint = colorScheme.onSurface
                )
            }
            val textStyle = typography.titleLarge.merge(
                TextStyle(
                    fontFamily = mainFontFamily,
                    textAlign = TextAlign.Center,
                    color = colorScheme.onSurface
                )
            )
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 56.dp),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search),
                                style = textStyle,
                                color = colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        inner()
                    }
                })
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 76.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (showLabels) 16.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            items(items = shown, key = { app: AppInfo -> "${app.packageName}/${app.className}" }) { app: AppInfo ->
                PickerAppItem(
                    app = app,
                    iconShape = iconShape,
                    showLabel = showLabels,
                    onClick = { onAppClick(if (app.className.isNotEmpty()) "${app.packageName}/${app.className}" else app.packageName) })
            }
        }
    }
}

@Composable
private fun PickerAppItem(
    app: AppInfo,
    iconShape: IconShape?,
    showLabel: Boolean,
    onClick: () -> Unit,
) {
    val bitmap = remember(app) { app.icon?.toBitmap()?.asImageBitmap() }
    val shape = iconShape?.getShape() ?: CircleShape

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = app.label,
                modifier = Modifier
                    .size(52.dp)
                    .clip(shape)
            )
        } else {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(shape)
                    .background(colorScheme.surfaceContainerHigh)
            )
        }
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = app.label,
                style = typography.labelMedium,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

private suspend fun loadAppsFallback(context: Context, hiddenApps: Set<String>): List<AppInfo> =
    withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(query, 0).asSequence().map { it.activityInfo.packageName to it }
            .filter { (pkg, _) -> pkg != context.packageName && pkg !in hiddenApps }
            .mapNotNull { (pkg, info) ->
                runCatching {
                    val label = info.activityInfo.loadLabel(pm).toString()
                    AppInfo(
                        name = label,
                        packageName = pkg,
                        icon = info.loadIcon(pm),
                        label = label,
                        isCustomized = false,
                        className = info.activityInfo.name
                    )
                }.getOrNull()
            }.sortedBy { it.label.lowercase() }.toList()
    }