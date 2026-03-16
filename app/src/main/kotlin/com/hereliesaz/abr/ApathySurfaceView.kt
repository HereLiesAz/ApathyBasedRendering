package com.hereliesaz.abr

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent

class ApathySurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private lateinit var renderer: ApathyRenderer

    init {
        // Force the environment to acknowledge OpenGL ES 3.0.
        setEGLContextClientVersion(3)
    }
    
    fun manifest(baseResId: Int, noiseResId: Int, stencilResId: Int) {
        renderer = ApathyRenderer(context, baseResId, noiseResId, stencilResId)
        
        // Bind the renderer to the lifecycle of this geometric void.
        setRenderer(renderer)

        // The void is never static. It continuously decays.
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            // The Android view system and OpenGL fundamentally disagree on which way is down.
            // Normalize the physical touch into UV space (0.0 to 1.0) and invert the Y-axis.
            val u = event.x / width
            val v = 1.0f - (event.y / height)

            // You cannot touch the void directly. You must queue your intentions for the GL thread.
            queueEvent {
                renderer.setTouchUV(u, v)
            }
            return true
        }
        return super.onTouchEvent(event)
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
