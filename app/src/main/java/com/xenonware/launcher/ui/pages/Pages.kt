package com.xenonware.launcher.ui.pages

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MediaPage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Media Player Fullscreen", color = Color.White, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun MainHomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("12:45", fontSize = 80.sp, fontWeight = FontWeight.Light, color = Color.White)
        Text("Tuesday, October 24", fontSize = 20.sp, color = Color.White.copy(alpha = 0.8f))
        
        Spacer(Modifier.height(48.dp))
        
        Text("Notifications Placeholder", color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
fun WidgetPage() {
    val context = LocalContext.current
    val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
    val appWidgetHost = remember { AppWidgetHost(context, 1024) }
    val widgetIds = remember { mutableStateListOf<Int>() }

    val pickWidgetLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (appWidgetId != -1) {
                widgetIds.add(appWidgetId)
            }
        }
    }

    DisposableEffect(Unit) {
        appWidgetHost.startListening()
        onDispose {
            appWidgetHost.stopListening()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 64.dp, bottom = 120.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Widgets", color = Color.White, style = MaterialTheme.typography.headlineLarge)
        }
        
        items(widgetIds) { id ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val appWidgetInfo = appWidgetManager.getAppWidgetInfo(id)
                        appWidgetHost.createView(ctx, id, appWidgetInfo)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clickable {
                        val appWidgetId = appWidgetHost.allocateAppWidgetId()
                        val pickIntent = android.content.Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        pickWidgetLauncher.launch(pickIntent)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Add, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Add Widget", color = Color.White.copy(0.5f), fontSize = 14.sp)
                }
            }
        }
    }
}
