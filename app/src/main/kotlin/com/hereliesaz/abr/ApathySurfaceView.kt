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
        setEGLContextClientVersion(3)
    }
    
    fun manifest(baseResId: Int, noiseResId: Int, stencilResId: Int) {
        renderer = ApathyRenderer(context, baseResId, noiseResId, stencilResId)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val u = event.x / width
        val v = 1.0f - (event.y / height)

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                queueEvent {
                    renderer.setTouchUV(u, v)
                    renderer.setTouchState(true)
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                queueEvent {
                    renderer.setTouchState(false)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }
}
