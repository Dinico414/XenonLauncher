package com.xenonware.launcher.ui.layouts.main

import android.Manifest
import android.content.Intent
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenonware.launcher.model.AppInfo
import com.xenonware.launcher.model.SearchResult
import com.xenonware.launcher.ui.res.AllAppsDivider
import com.xenonware.launcher.ui.res.AppEditDialog
import com.xenonware.launcher.ui.res.MenuItem
import com.xenonware.launcher.ui.res.XenonDropDown
import com.xenonware.launcher.ui.res.XenonSingleChoiceButtonGroup
import com.xenonware.launcher.ui.res.notification.NotificationBadge
import com.xenonware.launcher.ui.res.search.SearchHistoryItem
import com.xenonware.launcher.ui.res.search.SearchResultItem
import com.xenonware.launcher.util.LocalDragDropState
import com.xenonware.launcher.util.matches
import com.xenonware.launcher.viewmodel.LauncherViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


enum class SearchType {
    Apps, Contacts, Files, Web
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AppDrawer(
    viewModel: LauncherViewModel,
    apps: List<AppInfo>,
    recentlyOpened: List<AppInfo>,
    containerColor: Color,
    onAppClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onDismiss: () -> Unit,
    onPinApp: (String, Int) -> Unit = { _, _ -> },
    isGridLayout: Boolean = true,
    onToggleLayout: () -> Unit = {},
    openKeyboard: Boolean = false,
    onToggleOpenKeyboard: () -> Unit = {},
    onProgress: (Float) -> Unit = {},
) {
    val dragDropState = LocalDragDropState.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 640
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptic = LocalHapticFeedback.current

    val hazeState = remember { HazeState() }
    val focusRequester = remember { FocusRequester() }
    val searchInteractionSource = remember { MutableInteractionSource() }
    val isSearchPressed by searchInteractionSource.collectIsPressedAsState()

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    val searchResults by viewModel.searchResults.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()

    val notifications by viewModel.notifications.collectAsState()
    val badgeType by viewModel.notificationBadgeType.collectAsState()

    val groupedNotifications = remember(notifications) {
        notifications.groupBy { it.packageName }
    }
    
    var appToEdit by remember { mutableStateOf<AppInfo?>(null) }
    val advancedSearchEnabled by viewModel.advancedSearchEnabled.collectAsState()
    val showHiddenAppsInSearch by viewModel.showHiddenAppsInSearch.collectAsState()
    val hiddenApps by viewModel.hiddenApps.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val iconShape by viewModel.drawerIconShape.collectAsState()
    val showShadow by viewModel.drawerIconShadow.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(advancedSearchEnabled) {
        if (advancedSearchEnabled) {
            val permissions = mutableListOf<String>()
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.READ_CONTACTS)

            permissionLauncher.launch(permissions.toTypedArray())

            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = "package:${context.packageName}".toUri()
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    context.startActivity(intent)
                }
            }
        }
    }

    var selectedSearchType by remember { mutableStateOf(SearchType.Apps) }

    LaunchedEffect(advancedSearchEnabled) {
        if (!advancedSearchEnabled) {
            selectedSearchType = SearchType.Apps
        }
    }

    var isSearchFocused by remember { mutableStateOf(false) }
    var searchResultPressOffset by remember { mutableStateOf(Offset.Zero) }
    var isSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(isSearchFocused, searchQuery, selectedSearchType, openKeyboard, isSearchPressed) {
        if (selectedSearchType != SearchType.Apps ||
            searchQuery.isNotEmpty() ||
            isSearchPressed ||
            (!openKeyboard && isSearchFocused)
        ) {
            isSearchActive = true
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var searchResultMenuApp by remember { mutableStateOf<AppInfo?>(null) }
    var appMenuInfo by remember { mutableStateOf<Pair<AppInfo, Offset>?>(null) }
    var barHeightPx by remember { mutableIntStateOf(0) }
    var searchBarHeightPx by remember { mutableIntStateOf(0) }
    var searchBarAnchor by remember { mutableStateOf(Offset.Zero) }

    val recentCount = if (isWideScreen) 6 else 4
    val recentApps = remember(recentlyOpened) { recentlyOpened.take(recentCount) }

    val isAtTop by remember(isGridLayout) {
        derivedStateOf {
            if (isGridLayout) {
                gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0
            } else {
                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            }
        }
    }

    // Autofocus search when scrolled to top
    LaunchedEffect(isAtTop, openKeyboard) {
        if (openKeyboard && isAtTop) {
            focusRequester.requestFocus()
            delay(100.milliseconds)
            keyboardController?.show()
        }
    }

    // Unfocus and hide keyboard when scrolling down if search is empty
    LaunchedEffect(isAtTop) {
        if (!isAtTop && searchQuery.isEmpty()) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    LaunchedEffect(searchQuery) {
        viewModel.performSearch(searchQuery)
    }

    val filteredResults = remember(searchResults, selectedSearchType, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else searchResults.filter { result ->
            when (selectedSearchType) {
                SearchType.Apps -> result is SearchResult.App
                SearchType.Contacts -> result is SearchResult.Contact
                SearchType.Files -> result is SearchResult.File
                SearchType.Web -> result is SearchResult.Web
            }
        }
    }

    val filteredApps = remember(apps, allApps, searchQuery, showHiddenAppsInSearch) {
        if (searchQuery.isBlank()) apps
        else {
            val source = if (showHiddenAppsInSearch) allApps else apps
            source.filter { it.matches(searchQuery) }
        }
    }

    fun handleSearchResultClick(result: SearchResult) {
        when (result) {
            is SearchResult.App -> {
                onAppClick(result.appInfo.packageName)
                onDismiss()
            }
            is SearchResult.Contact -> {
                val intent = Intent(Intent.ACTION_DIAL, "tel:${result.phoneNumber}".toUri())
                context.startActivity(intent)
                onDismiss()
            }
            is SearchResult.File -> {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(result.uri, result.mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Open File"))
                onDismiss()
            }
            is SearchResult.Web -> {
                val url = if (result.isUrl) {
                    if (result.query.startsWith("http")) result.query else "https://${result.query}"
                } else {
                    "https://www.google.com/search?q=${result.query}"
                }
                viewModel.addToSearchHistory(result.query)
                context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                onDismiss()
            }
        }
    }

    fun closeSearchOrDismiss() {
        if (isSearchFocused || searchQuery.isNotEmpty() || isSearchActive) {
            searchQuery = ""
            selectedSearchType = SearchType.Apps
            isSearchActive = false
            focusManager.clearFocus()
            keyboardController?.hide()
        } else {
            onDismiss()
        }
    }

    val onUninstallApp: (String) -> Unit = { packageName ->
        val intent = Intent(Intent.ACTION_DELETE, "package:$packageName".toUri())
        context.startActivity(intent)
    }

    val onAppInfo: (String) -> Unit = { packageName ->
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri())
        context.startActivity(intent)
    }

    val onHideApp: (String) -> Unit = { packageName ->
        viewModel.hideApp(packageName)
    }

    val onUnhideApp: (String) -> Unit = { packageName ->
        viewModel.unhideApp(packageName)
    }


    val scope = rememberCoroutineScope()
    val backProgress = remember { Animatable(0f) }
    val searchBackProgress = remember { Animatable(0f) }
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    val isSearchUIActive = isSearchFocused || searchQuery.isNotEmpty() || isSearchActive

    // 1. Search Back Handler: Handles dismissing search/categories
    PredictiveBackHandler(enabled = isSearchUIActive) { progress ->
        try {
            progress.collect { backEvent ->
                val eased = FastOutSlowInEasing.transform(backEvent.progress)
                searchBackProgress.snapTo(eased)
            }
            // Committed: Close search
            scope.launch {
                // Simultaneously start the layout shrink and finish the fade
                isSearchActive = false
                searchQuery = ""
                selectedSearchType = SearchType.Apps
                focusManager.clearFocus()
                keyboardController?.hide()
                
                // Animate to 1.0 (fully dismissed) to sync with the current gesture progress
                searchBackProgress.animateTo(1f, tween(300))
                // Reset for next time
                searchBackProgress.snapTo(0f)
            }
        } catch (_: CancellationException) {
            scope.launch { searchBackProgress.animateTo(0f, tween(220)) }
        }
    }

    // 2. Drawer Back Handler: Handles dismissing the whole drawer
    PredictiveBackHandler(enabled = !isSearchUIActive) { progress ->
        try {
            progress.collect { backEvent ->
                val eased = FastOutSlowInEasing.transform(backEvent.progress)
                backProgress.snapTo(eased)
            }
            // Committed: Close drawer
            currentOnDismiss()
        } catch (_: CancellationException) {
            scope.launch { backProgress.animateTo(0f, tween(220)) }
        }
    }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var sheetHeightPx by remember { mutableIntStateOf(0) }

    val currentProgress = remember(offsetY, sheetHeightPx, backProgress.value) {
        val dragProgress =
            if (sheetHeightPx > 0) (1f - (offsetY / sheetHeightPx)).coerceIn(0f, 1f) else 1f
        dragProgress * (1f - backProgress.value)
    }

    LaunchedEffect(currentProgress) {
        onProgress(currentProgress)
    }

    val dismissThresholdPx = with(density) { 120.dp.toPx() }
    val flingDismissVelocity = 1200f

    val sheetDragConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && offsetY > 0f) {
                    val consume = available.y.coerceAtLeast(-offsetY)
                    offsetY += consume
                    return Offset(0f, consume)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    offsetY += available.y
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offsetY > 0f) {
                    val target = if (sheetHeightPx > 0) sheetHeightPx.toFloat() else offsetY + 1000f
                    if (offsetY > dismissThresholdPx || available.y > flingDismissVelocity) {
                        animate(offsetY, target, animationSpec = tween(220)) { v, _ -> offsetY = v }
                        currentOnDismiss()
                    } else {
                        animate(offsetY, 0f) { v, _ -> offsetY = v }
                    }
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    val p = backProgress.value
                    val s = 1f - 0.08f * p
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    translationY = offsetY + 24.dp.toPx() * p
                }
                .onSizeChanged { sheetHeightPx = it.height }
                .statusBarsPadding()
                .then(
                    if (isWideScreen) {
                        Modifier.padding(horizontal = 56.dp).widthIn(max = 640.dp).fillMaxHeight()
                    } else {
                        Modifier.fillMaxSize()
                    }
                )
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = containerColor,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(Modifier.height(16.dp))

                // Drag handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            colorScheme.onSurfaceVariant.copy(alpha = 0.6f), CircleShape
                        )
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(16.dp))

                // Bar floats over the scrolling list; the list is hazed where it passes behind it.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .graphicsLayer(clip = false)
                ) {
                    val animatedBarHeight by animateFloatAsState(
                        targetValue = barHeightPx.toFloat(),
                        animationSpec = tween(500),
                        label = "barHeight"
                    )
                    val contentTopPadding = with(density) { animatedBarHeight.toDp() } + 16.dp

                    if (isGridLayout && selectedSearchType == SearchType.Apps) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (isWideScreen) 6 else 4),
                            state = gridState,
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(sheetDragConnection)
                                .hazeSource(hazeState)
                                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)),
                            contentPadding = PaddingValues(
                                top = contentTopPadding, bottom = 120.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box {
                                    this@Column.AnimatedVisibility(
                                        visible = searchQuery.isEmpty(),
                                        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { -it } + expandVertically(animationSpec = tween(300), clip = false),
                                        exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(600)) { -it } + shrinkVertically(animationSpec = tween(600), clip = false),
                                        modifier = Modifier.graphicsLayer(clip = false)
                                    ) {
                                        if (recentApps.isNotEmpty()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(
                                                        16.dp
                                                    )
                                                ) {
                                                    recentApps.forEach { app ->
                                                        Box(
                                                            modifier = Modifier.weight(1f),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                                AppDrawerGridLayout(
                                                                    app = app,
                                                                    notificationCount = groupedNotifications[app.packageName]?.size ?: 0,
                                                                    badgeType = badgeType,
                                                                    onAppClick = onAppClick,
                                                                    onDismiss = onDismiss,
                                                                    onPinApp = onPinApp,
                                                                    dragDropState = dragDropState,
                                                                    onLongPress = { appMenuInfo = app to it },
                                                                    iconShape = iconShape,
                                                                    showShadow = showShadow
                                                                )
                                                        }
                                                    }
                                                    repeat(recentCount - recentApps.size) {
                                                        Spacer(Modifier.weight(1f))
                                                    }
                                                }

                                                AllAppsDivider()
                                            }
                                        }
                                    }
                                }
                            }

                            items(filteredApps) { app ->
                                AppDrawerGridLayout(
                                    app = app,
                                    notificationCount = groupedNotifications[app.packageName]?.size ?: 0,
                                    badgeType = badgeType,
                                    onAppClick = onAppClick,
                                    onDismiss = onDismiss,
                                    onPinApp = onPinApp,
                                    dragDropState = dragDropState,
                                    onLongPress = { appMenuInfo = app to it },
                                    iconShape = iconShape,
                                    showShadow = showShadow
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(sheetDragConnection)
                                .hazeSource(hazeState)
                                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)),
                            contentPadding = PaddingValues(
                                top = contentTopPadding, bottom = 120.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedSearchType == SearchType.Apps) {
                                item {
                                    Box {
                                        this@Column.AnimatedVisibility(
                                            visible = searchQuery.isEmpty(),
                                            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)) { -it } + expandVertically(animationSpec = tween(300), clip = false),
                                            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300)) { -it } + shrinkVertically(animationSpec = tween(300), clip = false),
                                            modifier = Modifier.graphicsLayer(clip = false)
                                        ) {
                                            if (recentApps.isNotEmpty()) {
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            16.dp
                                                        )
                                                    ) {
                                                        recentApps.forEach { app ->
                                                            Box(
                                                                modifier = Modifier.weight(1f),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                AppDrawerGridLayout(
                                                                    app = app,
                                                                    notificationCount = groupedNotifications[app.packageName]?.size ?: 0,
                                                                    badgeType = badgeType,
                                                                    onAppClick = onAppClick,
                                                                    onDismiss = onDismiss,
                                                                    onPinApp = onPinApp,
                                                                    dragDropState = dragDropState,
                                                                    onLongPress = { appMenuInfo = app to it },
                                                                    iconShape = iconShape,
                                                                    showShadow = showShadow
                                                                )
                                                            }
                                                        }
                                                        repeat(recentCount - recentApps.size) {
                                                            Spacer(Modifier.weight(1f))
                                                        }
                                                    }

                                                    AllAppsDivider(
                                                        modifier = Modifier.padding(
                                                            bottom = 20.dp
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                items(filteredApps) { app ->
                                    var itemPos by remember { mutableStateOf(Offset.Zero) }
                                    var pressOffset by remember { mutableStateOf(Offset.Zero) }
                                    var isActualDrag by remember { mutableStateOf(false) }

                                    val viewConfiguration = LocalViewConfiguration.current
                                    val customViewConfiguration = remember(viewConfiguration) {
                                        object : ViewConfiguration by viewConfiguration {
                                            override val touchSlop: Float
                                                get() = viewConfiguration.touchSlop * 3f
                                        }
                                    }

                                    CompositionLocalProvider(LocalViewConfiguration provides customViewConfiguration) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .onGloballyPositioned { itemPos = it.positionInRoot() }
                                                .pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onTap = {
                                                            onAppClick(app.packageName)
                                                            onDismiss()
                                                        })
                                                }
                                                .pointerInput(Unit) {
                                                    var totalDragDistance = 0f
                                                    detectDragGesturesAfterLongPress(onDragStart = { offset ->
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        isActualDrag = false
                                                        pressOffset = offset
                                                        totalDragDistance = 0f
                                                    }, onDrag = { change, dragAmount ->
                                                        totalDragDistance += dragAmount.getDistance()
                                                        val threshold = with(density) { 24.dp.toPx() }

                                                        if (totalDragDistance > threshold && !isActualDrag) {
                                                            isActualDrag = true
                                                            dragDropState.startDrag(
                                                                app, itemPos + pressOffset
                                                            )
                                                        }
                                                        
                                                        if (isActualDrag) {
                                                            change.consume()
                                                            dragDropState.dragOffset += dragAmount

                                                            if (dragDropState.dockBounds.contains(
                                                                    dragDropState.dragOffset
                                                                )
                                                            ) {
                                                                val relativeX =
                                                                    dragDropState.dragOffset.x - dragDropState.dockBounds.left
                                                                val itemWidth =
                                                                    with(density) { 52.dp.toPx() }
                                                                dragDropState.targetIndex =
                                                                    (relativeX / itemWidth).toInt()
                                                                        .coerceIn(0, 100)
                                                            } else {
                                                                dragDropState.targetIndex = -1
                                                            }
                                                        }
                                                    }, onDragEnd = {
                                                        if (isActualDrag) {
                                                            val finalPos = dragDropState.dragOffset
                                                            val verticalDist =
                                                                if (finalPos.y < dragDropState.dockBounds.top) {
                                                                    dragDropState.dockBounds.top - finalPos.y
                                                                } else if (finalPos.y > dragDropState.dockBounds.bottom) {
                                                                    finalPos.y - dragDropState.dockBounds.bottom
                                                                } else 0f

                                                            val hitThreshold =
                                                                with(density) { 80.dp.toPx() }

                                                            if (dragDropState.dockBounds.contains(
                                                                    finalPos
                                                                ) || verticalDist < hitThreshold
                                                            ) {
                                                                onPinApp(
                                                                    app.packageName,
                                                                    dragDropState.targetIndex
                                                                )
                                                            }
                                                        } else {
                                                            appMenuInfo = app to (itemPos + pressOffset)
                                                        }
                                                        dragDropState.stopDrag()
                                                    }, onDragCancel = { dragDropState.stopDrag() })
                                                }
                                                .padding(horizontal = 8.dp, vertical = 8.dp)) {
                                            Box(contentAlignment = Alignment.TopEnd) {
                                                app.icon?.let { icon ->
                                                    val shape = iconShape.getShape()
                                                    Image(
                                                        bitmap = icon.toBitmap().asImageBitmap(),
                                                        contentDescription = app.label,
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .then(if (showShadow) Modifier.shadow(4.dp, shape) else Modifier)
                                                            .clip(shape)
                                                    )
                                                }
                                                NotificationBadge(
                                                    count = groupedNotifications[app.packageName]?.size ?: 0,
                                                    badgeType = badgeType,
                                                    appIcon = app.icon,
                                                    modifier = Modifier.offset(x = 2.dp, y = (-2).dp)
                                                )
                                            }
                                            Spacer(Modifier.width(16.dp))
                                            Text(
                                                app.label,
                                                color = colorScheme.onSurface,
                                                fontSize = 16.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Text(
                                        text = selectedSearchType.name,
                                        style = typography.titleMedium,
                                        color = colorScheme.onSurface,
                                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 16.dp)
                                    )
                                }

                                if (selectedSearchType == SearchType.Web) {
                                    if (searchQuery.isNotEmpty()) {
                                        items(filteredResults) { result ->
                                            SearchResultItem(
                                                result = result,
                                                onClick = { handleSearchResultClick(it) },
                                                iconShape = iconShape,
                                                showShadow = showShadow
                                            )
                                        }
                                    }
                                    if (searchHistory.isNotEmpty()) {
                                        item {
                                            Column {
                                                Text(
                                                    "Search History",
                                                    style = typography.labelMedium,
                                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                                )
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(20.dp))
                                                        .background(colorScheme.surfaceContainer.copy(alpha = 0.8f))
                                                ) {
                                                    searchHistory.forEachIndexed { index, history ->
                                                        SearchHistoryItem(history) {
                                                            searchQuery = it
                                                            viewModel.performSearch(it)
                                                        }
                                                        if (index < searchHistory.size - 1) {
                                                            HorizontalDivider(
                                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                                color = colorScheme.onSurface.copy(alpha = 0.05f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    items(filteredResults) { result ->
                                        SearchResultItem(
                                            result = result,
                                            onClick = { handleSearchResultClick(it) },
                                            onLongClick = { it, offset ->
                                                if (it is SearchResult.App) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    searchResultPressOffset = offset
                                                    searchResultMenuApp = it.appInfo
                                                }
                                            },
                                            iconShape = iconShape,
                                            showShadow = showShadow
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (filteredApps.isEmpty() && searchQuery.isNotBlank() && selectedSearchType == SearchType.Apps) {
                        Text(
                            "No apps found",
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    searchResultMenuApp?.let { app ->
                        val isHidden = app.packageName in hiddenApps
                        
                        XenonDropDown(
                            expanded = searchResultMenuApp != null,
                            onDismissRequest = { searchResultMenuApp = null },
                            items = listOf(
                                MenuItem(
                                    text = "Uninstall",
                                    onClick = { onUninstallApp(app.packageName) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                    textColor = colorScheme.error,
                                    containerColor = colorScheme.error.copy(alpha = 0.15f)
                                ),
                                MenuItem(
                                    text = "App Info",
                                    onClick = { onAppInfo(app.packageName) },
                                    leadingIcon = { Icon(Icons.Rounded.Info, null) }
                                ),
                                MenuItem(
                                    text = "Edit",
                                    onClick = { appToEdit = app },
                                    leadingIcon = { Icon(Icons.Rounded.Edit, null) }
                                ),
                                MenuItem(
                                    text = if (isHidden) "Unhide" else "Hide",
                                    onClick = {
                                        if (isHidden) onUnhideApp(app.packageName)
                                        else onHideApp(app.packageName)
                                    },
                                    leadingIcon = { Icon(if (isHidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, null) }
                                )
                            ),
                            hazeState = hazeState,
                            offsetX = with(density) { searchResultPressOffset.x.toDp() },
                            offsetY = with(density) { searchResultPressOffset.y.toDp() },
                            anchorPos = Offset.Zero,
                            alignment = Alignment.Center
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = tween(300))
                            .onSizeChanged { barHeightPx = it.height }
                            .graphicsLayer(clip = false)
                    ) {
                        this@Column.AnimatedVisibility(
                            visible = isSearchActive && advancedSearchEnabled,
                            enter = slideInVertically(animationSpec = tween(300, 500)) { -it } + expandVertically(
                                expandFrom = Alignment.Top,
                                animationSpec = tween(200),
                                clip = false
                            ) + fadeIn(animationSpec = tween(300, 500)),
                            exit = slideOutVertically(animationSpec = tween(300)) { -it } + shrinkVertically(
                                shrinkTowards = Alignment.Top,
                                animationSpec = tween(300),
                                clip = false
                            ) + fadeOut(animationSpec = tween(300)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = with(density) { searchBarHeightPx.toDp() })
                                .zIndex(0f)
                                .graphicsLayer {
                                    // Progressive back gesture support for search categories
                                    translationY = -searchBackProgress.value * 20.dp.toPx()
                                    alpha = 1f - searchBackProgress.value
                                }
                                .graphicsLayer(clip = false)
                        ) {
                            CompositionLocalProvider(LocalTextStyle provides typography.labelMedium) {
                                XenonSingleChoiceButtonGroup(
                                    options = SearchType.entries,
                                    selectedOption = selectedSearchType,
                                    onOptionSelect = { selectedSearchType = it },
                                    label = { it.name },
                                    icon = { _, _ -> },
                                    buttonHeight = 36.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .zIndex(1f)
                                .fillMaxWidth()
                                .onSizeChanged { searchBarHeightPx = it.height }
                                .onGloballyPositioned { coords ->
                                    searchBarAnchor = coords.positionInRoot().let { pos ->
                                        Offset(pos.x + coords.size.width, pos.y + coords.size.height)
                                    }
                                }
                                .clip(RoundedCornerShape(100f))
                                .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin())
                                .background(colorScheme.surfaceContainer.copy(alpha = 0.4f))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    isSearchActive = true
                                    focusRequester.requestFocus()
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { closeSearchOrDismiss() }, Modifier.padding(4.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    tint = colorScheme.onSurface,
                                    contentDescription = if (isSearchFocused || searchQuery.isNotEmpty()) "Close search" else "Close"
                                )
                            }

                            val textStyle = typography.titleLarge.merge(
                                TextStyle(
                                    fontFamily = QuicksandTitleVariable,
                                    textAlign = TextAlign.Center,
                                    color = colorScheme.onSurface
                                )
                            )

                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { isSearchFocused = it.isFocused },
                                interactionSource = searchInteractionSource,
                                singleLine = true,
                                textStyle = textStyle,
                                cursorBrush = SolidColor(colorScheme.primary),
                                decorationBox = { innerTextField ->
                                    Box(
                                        Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
                                    ) {
                                        if (searchQuery.isEmpty() && !isSearchFocused) {
                                            Text(
                                                text = "Search",
                                                style = textStyle,
                                                color = colorScheme.onSurface.copy(alpha = 0.6f),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        innerTextField()
                                    }
                                })

                            Box {
                                IconButton(
                                    onClick = { showMenu = !showMenu },
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.MoreVert,
                                        tint = colorScheme.onSurface,
                                        contentDescription = "More options"
                                    )
                                }
                                XenonDropDown(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    items = listOf(
                                        MenuItem(
                                        text = if (isGridLayout) "List view" else "Grid view",
                                        onClick = onToggleLayout,
                                        dismissOnClick = true,
                                        leadingIcon = {
                                            Icon(
                                                if (isGridLayout) Icons.AutoMirrored.Rounded.ViewList
                                                else Icons.Rounded.GridView,
                                                contentDescription = "Toggle layout"
                                            )
                                        }),
                                        MenuItem(
                                        text = "Show Keyboard",
                                        onClick = onToggleOpenKeyboard,
                                        dismissOnClick = false,
                                        leadingIcon = {
                                            Icon(
                                                if (openKeyboard) Icons.Rounded.Visibility
                                                else Icons.Rounded.VisibilityOff,
                                                contentDescription = null
                                            )
                                        }),
                                        MenuItem(
                                            text = "Advanced Search",
                                            onClick = { viewModel.setAdvancedSearchEnabled(!advancedSearchEnabled) },
                                            dismissOnClick = false,
                                            leadingIcon = { Icon(if (advancedSearchEnabled) Icons.Rounded.Search else Icons.Rounded.SearchOff, null) }
                                        ),
                                        MenuItem(
                                        text = "Settings",
                                        onClick = { onSettingsClick() },
                                        dismissOnClick = true,
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.Settings,
                                                contentDescription = "Settings"
                                            )
                                        })
                                    ),
                                    hazeState = hazeState,
                                    anchorPos = searchBarAnchor,
                                    offsetY = 8.dp,
                                    alignment = Alignment.TopEnd
                                )
                            }
                        }
                    }
                }
            }
        }
        
        appMenuInfo?.let { (app, offset) ->
            XenonDropDown(
                expanded = true,
                onDismissRequest = { appMenuInfo = null },
                items = listOf(
                    MenuItem(
                        text = "Uninstall",
                        onClick = { onUninstallApp(app.packageName) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                        textColor = colorScheme.error,
                        containerColor = colorScheme.error.copy(alpha = 0.25f)
                    ),
                    MenuItem(
                        text = "App Info",
                        onClick = { onAppInfo(app.packageName) },
                        leadingIcon = { Icon(Icons.Rounded.Info, null) }
                    ),
                    MenuItem(
                        text = "Edit",
                        onClick = { appToEdit = app },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) }
                    ),
                    MenuItem(
                        text = if (app.packageName in hiddenApps) "Unhide" else "Hide",
                        onClick = {
                            if (app.packageName in hiddenApps) onUnhideApp(app.packageName)
                            else onHideApp(app.packageName)
                        },
                        leadingIcon = {
                            Icon(
                                if (app.packageName in hiddenApps) Icons.Rounded.Visibility
                                else Icons.Rounded.VisibilityOff, null
                            )
                        }
                    )
                ),
                hazeState = hazeState,
                offsetX = with(density) { offset.x.toDp() },
                offsetY = with(density) { offset.y.toDp() },
                anchorPos = Offset.Zero,
                alignment = Alignment.Center
            )
        }
        
        appToEdit?.let { app ->
            AppEditDialog(
                app = app,
                viewModel = viewModel,
                onDismiss = { appToEdit = null }
            )
        }
    }
}
