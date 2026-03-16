package com.hereliesaz.abr

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import kotlin.math.hypot

class ApathySurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs), SensorEventListener {

    private lateinit var renderer: ApathyRenderer

    private var lastX = 0f
    private var lastY = 0f
    private var lastTime = 0L

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

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

                val velocity = distance / dt
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

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_GRAVITY) {
            // X and Y physical forces. We ignore Z because the void is flat.
            val gx = event.values[0]
            val gy = event.values[1]
            
            queueEvent {
                renderer.setGravity(gx, gy)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // We do not care about accuracy. Apathy is imprecise by nature.
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        gravitySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }
}
