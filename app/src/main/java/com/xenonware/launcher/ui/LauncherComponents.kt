package com.xenonware.launcher.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.xenonware.launcher.model.AppInfo

@Composable
fun DockPill(
    modifier: Modifier = Modifier,
    apps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onFabClick: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(1) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val dockColor = Color.Black.copy(alpha = 0.4f)
        Box(
            modifier = Modifier
                .height(72.dp)
                .weight(1f)
                .clip(CircleShape)
                .background(dockColor)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(if (currentPage == 0) 1.2f else 0.4f)
                        .animateContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentPage == 0) StatusSection() 
                    else IconButton(onClick = { currentPage = 0 }) {
                        Icon(Icons.Default.Info, null, tint = Color.White.copy(0.6f))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(if (currentPage == 1) 3f else 0.4f)
                        .animateContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentPage == 1) FixedAppSection(apps, onAppClick)
                    else IconButton(onClick = { currentPage = 1 }) {
                        Icon(Icons.Default.Apps, null, tint = Color.White.copy(0.6f))
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(if (currentPage == 2) 2f else 0.4f)
                        .animateContentSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentPage == 2) MediaSection()
                    else IconButton(onClick = { currentPage = 2 }) {
                        Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.6f))
                    }
                }

                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, null, tint = Color.White.copy(0.4f))
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        FloatingActionButton(
            onClick = onFabClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(Icons.Default.Add, "Open Apps", modifier = Modifier.size(32.dp), tint = Color.White)
        }
    }
}

@Composable
fun StatusSection() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("12:45", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Text("Tue, Oct 24", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WbSunny, null, tint = Color.Yellow, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("24°C", color = Color.White, fontSize = 14.sp)
        }
    }
}

@Composable
fun FixedAppSection(apps: List<AppInfo>, onAppClick: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(apps.take(8)) { app ->
            app.icon?.let { icon ->
                Image(
                    bitmap = icon.toBitmap().asImageBitmap(),
                    contentDescription = app.name,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable { onAppClick(app.packageName) },
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun MediaSection() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(36.dp), shape = CircleShape, color = Color.DarkGray) {
            Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.padding(9.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Song Title", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("Artist Name", color = Color.White.copy(0.7f), fontSize = 10.sp, maxLines = 1)
        }
        Row {
            IconButton(onClick = {}, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
            IconButton(onClick = {}, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Applications", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(apps) { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { 
                            onAppClick(app.packageName)
                            onDismiss()
                        }
                    ) {
                        app.icon?.let { icon ->
                            Image(
                                bitmap = icon.toBitmap().asImageBitmap(),
                                contentDescription = app.name,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            app.name, 
                            color = Color.White, 
                            fontSize = 12.sp, 
                            maxLines = 1,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
