package com.hereliesaz.abr

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet

class ApathySurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    init {
        // Force the environment to acknowledge OpenGL ES 3.0.
        setEGLContextClientVersion(3)
    }
    
    fun manifest(baseResId: Int, noiseResId: Int, stencilResId: Int) {
        val renderer = ApathyRenderer(context, baseResId, noiseResId, stencilResId)
        
        // Bind the renderer to the lifecycle of this geometric void.
        setRenderer(renderer)

        // The void is never static. It continuously decays.
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
