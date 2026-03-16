package com.hereliesaz.abr

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var apathySurface: ApathySurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide the action bar. Pretense is a distraction.
        supportActionBar?.hide()
        
        setContentView(R.layout.activity_main)

        apathySurface = findViewById(R.id.apathy_surface)

        // You must supply your own trauma.
        // Replace R.drawable.* with the actual physical manifestations of the joke.
        apathySurface.manifest(
            baseResId = R.drawable.base_reality, // The wall
            noiseResId = R.drawable.drip_noise,  // The chaos
            stencilResId = R.drawable.the_joke   // The punchline
        )
    }

    override fun onResume() {
        super.onResume()
        // Resume the inevitable march of time.
        apathySurface.onResume()
    }

    override fun onPause() {
        super.onPause()
        // Suspend the decay.
        apathySurface.onPause()
    }
}
