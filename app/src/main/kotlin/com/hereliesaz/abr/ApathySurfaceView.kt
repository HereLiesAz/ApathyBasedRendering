package com.hereliesaz.abr

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import kotlin.math.hypot

class ApathySurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    private lateinit var renderer: ApathyRenderer

    private var lastX = 0f
    private var lastY = 0f
    private var lastTime = 0L

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
        val currentTime = SystemClock.uptimeMillis()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                lastTime = currentTime
                
                queueEvent {
                    // Initial contact. Maximum trauma.
                    renderer.setTouchData(u, v, 1.0f)
                    renderer.setTouchState(true)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dt = (currentTime - lastTime).coerceAtLeast(1).toFloat()
                val dx = event.x - lastX
                val dy = event.y - lastY
                val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()

                // Velocity in pixels per millisecond.
                val velocity = distance / dt

                // The faster you run, the less of a mark you leave.
                // Map a speed of 5.0+ px/ms to near 0 opacity, and 0.0 px/ms to 1.0 opacity.
                val normalizedVelocity = (velocity / 5.0f).coerceIn(0.01f, 1.0f)
                val opacity = 1.0f - normalizedVelocity

                lastX = event.x
                lastY = event.y
                lastTime = currentTime

                queueEvent {
                    renderer.setTouchData(u, v, opacity)
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
