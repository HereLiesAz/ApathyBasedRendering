package com.hereliesaz.abr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // The void requires no action bar, no themes, no pretense.
        setContent {
            ApathyScreen(
                baseResId = R.drawable.base_reality,
                noiseResId = R.drawable.drip_noise,
                stencilResId = R.drawable.the_joke,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
