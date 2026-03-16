package com.hereliesaz.abr

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun ApathyScreen(
    baseResId: Int,
    noiseResId: Int,
    stencilResId: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Instantiate the geometric void exactly once. 
    // Recomposition should not reset the trauma.
    val apathyView = remember {
        ApathySurfaceView(context).apply {
            manifest(baseResId, noiseResId, stencilResId)
        }
    }

    // The rendering thread must sleep when the user looks away.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> apathyView.onResume()
                Lifecycle.Event.ON_PAUSE -> apathyView.onPause()
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Wrap the old world in the new.
    AndroidView(
        factory = { apathyView },
        modifier = modifier
    )
}
