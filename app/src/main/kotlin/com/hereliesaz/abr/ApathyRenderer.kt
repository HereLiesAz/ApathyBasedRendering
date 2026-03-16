package com.hereliesaz.abr

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ApathyRenderer(
    private val context: Context,
    private val baseResId: Int,
    private val noiseResId: Int,
    private val stencilResId: Int
) : GLSurfaceView.Renderer {

    private var shaderProgram: Int = 0
    private var timeUniformLocation: Int = 0
    private var startTime: Long = 0

    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var mvpMatrixHandle: Int = 0
    private var baseTextureHandle: Int = 0
    private var noiseTextureHandle: Int = 0
    private var stencilMaskHandle: Int = 0
    private var touchUVHandle: Int = 0

    private var baseTextureId: Int = 0
    private var noiseTextureId: Int = 0
    private var stencilTextureId: Int = 0

    private val vaoHandle = IntArray(1)
    private val vboHandle = IntArray(1)

    // The locus of control. Defaults to the dead center of existence.
    @Volatile private var touchU: Float = 0.5f
    @Volatile private var touchV: Float = 0.5f

    // The wall. A flat plane of existence for the paint to suffer on.
    // X, Y, Z, U, V
    private val geometryData = floatArrayOf(
        -1.0f,  1.0f, 0.0f,   0.0f, 0.0f, // Top left
        -1.0f, -1.0f, 0.0f,   0.0f, 1.0f, // Bottom left
         1.0f,  1.0f, 0.0f,   1.0f, 0.0f, // Top right
         1.0f, -1.0f, 0.0f,   1.0f, 1.0f  // Bottom right
    )

    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(geometryData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(geometryData)
        .apply { position(0) }

    fun setTouchUV(u: Float, v: Float) {
        touchU = u
        touchV = v
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)

        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        
        GLES30.glDisable(GLES30.GL_BLEND)

        shaderProgram = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        
        timeUniformLocation = GLES30.glGetUniformLocation(shaderProgram, "uTime")
        mvpMatrixHandle = GLES30.glGetUniformLocation(shaderProgram, "uMVPMatrix")
        baseTextureHandle = GLES30.glGetUniformLocation(shaderProgram, "uBaseTexture")
        noiseTextureHandle = GLES30.glGetUniformLocation(shaderProgram, "uNoiseTexture")
        stencilMaskHandle = GLES30.glGetUniformLocation(shaderProgram, "uStencilMask")
        touchUVHandle = GLES30.glGetUniformLocation(shaderProgram, "uTouchUV")

        // Pull the tragedy from the resources into the GPU.
        baseTextureId = TextureLoader.load(context, baseResId)
        noiseTextureId = TextureLoader.load(context, noiseResId)
        stencilTextureId = TextureLoader.load(context, stencilResId)

        // Manifest the physical limits of the canvas.
        GLES30.glGenVertexArrays(1, vaoHandle, 0)
        GLES30.glGenBuffers(1, vboHandle, 0)

        GLES30.glBindVertexArray(vaoHandle[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboHandle[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, geometryData.size * 4, vertexBuffer, GLES30.GL_STATIC_DRAW)

        // Position Attribute (Location 0, 3 floats)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 5 * 4, 0)
        GLES30.glEnableVertexAttribArray(0)

        // UV Attribute (Location 1, 2 floats)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 5 * 4, 3 * 4)
        GLES30.glEnableVertexAttribArray(1)

        GLES30.glBindVertexArray(0)

        startTime = SystemClock.uptimeMillis()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        
        val ratio: Float = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 10f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        GLES30.glUseProgram(shaderProgram)

        val time = (SystemClock.uptimeMillis() - startTime) / 1000.0f
        GLES30.glUniform1f(timeUniformLocation, time)
        
        // Feed the human intervention into the shader.
        GLES30.glUniform2f(touchUVHandle, touchU, touchV)

        // The camera stares into the void.
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        
        // The object exists, momentarily.
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vPMatrix, 0, modelMatrix, 0)
        
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        // Bind the textures. Force the fragmented reality down the pipeline.
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, baseTextureId)
        GLES30.glUniform1i(baseTextureHandle, 0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, noiseTextureId)
        GLES30.glUniform1i(noiseTextureHandle, 1)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, stencilTextureId)
        GLES30.glUniform1i(stencilMaskHandle, 2)

        // Execute the brutalist geometry.
        GLES30.glBindVertexArray(vaoHandle[0])
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        GLES30.glBindVertexArray(0)
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        return GLES30.glCreateProgram().also {
            GLES30.glAttachShader(it, vertexShader)
            GLES30.glAttachShader(it, fragmentShader)
            GLES30.glLinkProgram(it)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES30.glCreateShader(type).also { shader ->
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)
        }
    }

    companion object {
        private val VERTEX_SHADER = """
            #version 300 es
            layout(location = 0) in vec4 aPosition;
            layout(location = 1) in vec2 aUV;
            
            uniform mat4 uMVPMatrix; 
            
            out vec2 vUV;
            
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vUV = aUV;
            }
        """.trimIndent()

        private val FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;
            in vec2 vUV;
            
            uniform sampler2D uBaseTexture;
            uniform sampler2D uNoiseTexture;
            uniform sampler2D uStencilMask;
            uniform float uTime;
            uniform vec2 uTouchUV;
            
            out vec4 FragColor;
            
            void main() {
                // The wall bleeds.
                vec2 noiseUV = vUV * 5.0;
                float noiseVal = texture(uNoiseTexture, noiseUV).r;
                float dripFactor = noiseVal * (sin(uTime * 0.2) * 0.5 + 0.5) * 0.15;
                vec2 distortedUV = vUV + vec2(0.0, -dripFactor);
                
                // Offset the stencil by the touch coordinates. 
                // Scale by 3.0 to make it a decal rather than a full-screen inescapable truth.
                vec2 stencilUV = (vUV - uTouchUV) * 3.0 + 0.5;
                
                // Hard clamp the bounds so the stencil doesn't tile into infinity.
                float bounds = step(0.0, stencilUV.x) * step(stencilUV.x, 1.0) * step(0.0, stencilUV.y) * step(stencilUV.y, 1.0);
                
                float stencilVal = texture(uStencilMask, stencilUV).r * bounds;
                float binaryCut = step(0.5, stencilVal); 
                
                vec4 rawColor = texture(uBaseTexture, distortedUV);
                float luminance = dot(rawColor.rgb, vec3(0.299, 0.587, 0.114));
                
                // Multiply the void by the binary cut to slice out the stencil.
                FragColor = vec4(vec3(luminance) * binaryCut, rawColor.a * binaryCut);
            }
        """.trimIndent()
    }
}
