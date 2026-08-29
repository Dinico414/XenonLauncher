package com.xenonware.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.xenon.mylibrary.res.AnimatedGradientBackground
import com.xenon.mylibrary.theme.QuicksandTitleVariable
import com.xenon.mylibrary.theme.XenonTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class BootWelcomeActivity : ComponentActivity() {
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
                val alpha = remember { Animatable(0f) }
                val backgroundAlpha = remember { Animatable(0f) }

                LaunchedEffect(Unit) {
                    // Fade in background blobs
                    backgroundAlpha.animateTo(1f, tween(1000))
                    // Fade in "Welcome" text
                    alpha.animateTo(1f, tween(1000))
                    
                    delay(2000.milliseconds)
                    
                    // Fade everything away
                    backgroundAlpha.animateTo(0f, tween(1000))
                    alpha.animateTo(0f, tween(1000))
                    
                    finish()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Blobs background
                    Box(modifier = Modifier.fillMaxSize().alpha(backgroundAlpha.value)) {
                        AnimatedGradientBackground(modifier = Modifier.fillMaxSize()) {}
                    }
                    
                    // Welcome text
                    Text(
                        text = stringResource(R.string.welcome),
                        modifier = Modifier.align(Alignment.Center).alpha(alpha.value),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = QuicksandTitleVariable,
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
}
