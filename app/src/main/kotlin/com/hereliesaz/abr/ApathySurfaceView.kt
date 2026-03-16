package com.hereliesaz.abr

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class ApathySurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private val renderer: ApathyRenderer

    init {
        // Force the environment to acknowledge OpenGL ES 3.0.
        // Anything less is archaic and insufficient for our suffering.
        setEGLContextClientVersion(3)

        renderer = ApathyRenderer()

        // Bind the renderer to the lifecycle of this geometric void.
        setRenderer(renderer)

        // The void is never static. It continuously decays.
        // RENDERMODE_CONTINUOUSLY ensures the uTime uniform actually means something.
        renderMode = RENDERMODE_CONTINUOUSLY
    }
    
    override fun onPause() {
        super.onPause()
        // Pause the rendering thread. The apathy can wait.
    }

    override fun onResume() {
        super.onResume()
        // Resume the inevitable march of time and drip vectors.
    }
}
