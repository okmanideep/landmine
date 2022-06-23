package me.okmanideep.landmine

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay

@Composable
fun Watch(onClick: () -> Unit) {
    val activity = LocalContext.current as Activity

    if (AUTOMATE_CRASH) {
        LaunchedEffect(Unit) {
            delay(500)
            onClick()
        }
    }

    val lifecycle = LocalLifecycleOwner.current.observeAsState()

    if (lifecycle.value == Lifecycle.State.RESUMED) {
        DisposableEffect(activity) {
            activity.window.hideScreenDecorations()
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

            onDispose {
                activity.window.showScreenDecorations()
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                onClick()
            },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Click to go back", color = Color.White)
        }
    }
}

@Suppress("DEPRECATION")
fun Window.hideScreenDecorations() {
    // Touching anywhere on the screen causes the navigation bar to automatically reappear and remain visible when we use WindowInsetsController
    // to control the visibility of the navigation bar. Using decorView.systemUiVisibility to control it will not appear this issue.
    // Consider using WindowInsetsController to replace decorView.systemUiVisibility after we solve this issue.
    setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN
    )
    decorView.systemUiVisibility =
        (
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
}

@Suppress("DEPRECATION")
fun Window.showScreenDecorations() {
    decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
    clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
}

@Composable
fun LifecycleOwner.observeAsState(): State<Lifecycle.State> {
    val lifecycleOwner = this
    val state = remember { mutableStateOf(Lifecycle.State.INITIALIZED) }

    DisposableEffect(key1 = lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { lifecycleOwner, _ ->
            state.value = lifecycleOwner.lifecycle.currentState
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
    return state
}
