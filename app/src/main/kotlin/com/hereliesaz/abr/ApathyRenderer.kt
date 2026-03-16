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

    private var stampProgram: Int = 0
    private var compositeProgram: Int = 0
    private var startTime: Long = 0

    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    // Stamp Uniforms
    private var stampMvpHandle: Int = 0
    private var stampStencilHandle: Int = 0
    private var stampTouchUVHandle: Int = 0
    private var stampOpacityHandle: Int = 0

    // Composite Uniforms
    private var compMvpHandle: Int = 0
    private var compTimeHandle: Int = 0
    private var compBaseHandle: Int = 0
    private var compNoiseHandle: Int = 0
    private var compAccumHandle: Int = 0
    private var compGravityHandle: Int = 0

    // Textures
    private var baseTextureId: Int = 0
    private var noiseTextureId: Int = 0
    private var stencilTextureId: Int = 0

    // FBO Memory
    private val fboHandle = IntArray(1)
    private val accumTextureHandle = IntArray(1)

    private val vaoHandle = IntArray(1)
    private val vboHandle = IntArray(1)

    @Volatile private var touchU: Float = 0.5f
    @Volatile private var touchV: Float = 0.5f
    @Volatile private var touchOpacity: Float = 1.0f
    @Volatile private var isTouching: Boolean = false
    
    // Default to falling straight down in standard portrait mode.
    @Volatile private var gravX: Float = 0.0f
    @Volatile private var gravY: Float = 9.8f 

    private val geometryData = floatArrayOf(
        -1.0f,  1.0f, 0.0f,   0.0f, 0.0f,
        -1.0f, -1.0f, 0.0f,   0.0f, 1.0f,
         1.0f,  1.0f, 0.0f,   1.0f, 0.0f,
         1.0f, -1.0f, 0.0f,   1.0f, 1.0f
    )

    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(geometryData.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(geometryData)
        .apply { position(0) }

    fun setTouchData(u: Float, v: Float, opacity: Float) {
        touchU = u
        touchV = v
        touchOpacity = opacity
    }

    fun setTouchState(state: Boolean) {
        isTouching = state
    }

    fun setGravity(x: Float, y: Float) {
        gravX = x
        gravY = y
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)

        stampProgram = createProgram(VERTEX_SHADER, STAMP_FRAGMENT_SHADER)
        stampMvpHandle = GLES30.glGetUniformLocation(stampProgram, "uMVPMatrix")
        stampStencilHandle = GLES30.glGetUniformLocation(stampProgram, "uStencilMask")
        stampTouchUVHandle = GLES30.glGetUniformLocation(stampProgram, "uTouchUV")
        stampOpacityHandle = GLES30.glGetUniformLocation(stampProgram, "uBrushOpacity")

        compositeProgram = createProgram(VERTEX_SHADER, COMPOSITE_FRAGMENT_SHADER)
        compMvpHandle = GLES30.glGetUniformLocation(compositeProgram, "uMVPMatrix")
        compTimeHandle = GLES30.glGetUniformLocation(compositeProgram, "uTime")
        compBaseHandle = GLES30.glGetUniformLocation(compositeProgram, "uBaseTexture")
        compNoiseHandle = GLES30.glGetUniformLocation(compositeProgram, "uNoiseTexture")
        compAccumHandle = GLES30.glGetUniformLocation(compositeProgram, "uAccumulationTexture")
        compGravityHandle = GLES30.glGetUniformLocation(compositeProgram, "uGravity")

        baseTextureId = TextureLoader.load(context, baseResId)
        stencilTextureId = TextureLoader.load(context, stencilResId)
        noiseTextureId = TextureGenerator.generateChaos()

        GLES30.glGenVertexArrays(1, vaoHandle, 0)
        GLES30.glGenBuffers(1, vboHandle, 0)

        GLES30.glBindVertexArray(vaoHandle[0])
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboHandle[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, geometryData.size * 4, vertexBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 5 * 4, 0)
        GLES30.glEnableVertexAttribArray(0)

        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 5 * 4, 3 * 4)
        GLES30.glEnableVertexAttribArray(1)

        GLES30.glBindVertexArray(0)

        startTime = SystemClock.uptimeMillis()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        
        val ratio: Float = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 10f)

        if (fboHandle[0] != 0) {
            GLES30.glDeleteFramebuffers(1, fboHandle, 0)
            GLES30.glDeleteTextures(1, accumTextureHandle, 0)
        }

        GLES30.glGenTextures(1, accumTextureHandle, 0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, accumTextureHandle[0])
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA8, width, height, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        GLES30.glGenFramebuffers(1, fboHandle, 0)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboHandle[0])
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, accumTextureHandle[0], 0)

        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vPMatrix, 0, modelMatrix, 0)

        // PASS 1: Accumulate the Vandalism
        if (isTouching) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboHandle[0])
            
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_ONE, GLES30.GL_ONE)

            GLES30.glUseProgram(stampProgram)
            GLES30.glUniformMatrix4fv(stampMvpHandle, 1, false, mvpMatrix, 0)
            GLES30.glUniform2f(stampTouchUVHandle, touchU, touchV)
            GLES30.glUniform1f(stampOpacityHandle, touchOpacity)

            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, stencilTextureId)
            GLES30.glUniform1i(stampStencilHandle, 0)

            GLES30.glBindVertexArray(vaoHandle[0])
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
            
            GLES30.glDisable(GLES30.GL_BLEND)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        }

        // PASS 2: Render the Decaying Reality
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        GLES30.glUseProgram(compositeProgram)

        val time = (SystemClock.uptimeMillis() - startTime) / 1000.0f
        GLES30.glUniform1f(compTimeHandle, time)
        GLES30.glUniformMatrix4fv(compMvpHandle, 1, false, mvpMatrix, 0)
        
        // Pass the physical pull to the GPU
        GLES30.glUniform2f(compGravityHandle, gravX, gravY)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, baseTextureId)
        GLES30.glUniform1i(compBaseHandle, 0)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, noiseTextureId)
        GLES30.glUniform1i(compNoiseHandle, 1)

        GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, accumTextureHandle[0])
        GLES30.glUniform1i(compAccumHandle, 2)

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

        private val STAMP_FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;
            in vec2 vUV;
            
            uniform sampler2D uStencilMask;
            uniform vec2 uTouchUV;
            uniform float uBrushOpacity;
            out vec4 FragColor;
            
            void main() {
                vec2 stencilUV = (vUV - uTouchUV) * 3.0 + 0.5;
                float bounds = step(0.0, stencilUV.x) * step(stencilUV.x, 1.0) * step(0.0, stencilUV.y) * step(stencilUV.y, 1.0);
                float stencilVal = texture(uStencilMask, stencilUV).r * bounds;
                
                FragColor = vec4(stencilVal * uBrushOpacity);
            }
        """.trimIndent()

        private val COMPOSITE_FRAGMENT_SHADER = """
            #version 300 es
            precision highp float;
            in vec2 vUV;
            
            uniform sampler2D uBaseTexture;
            uniform sampler2D uNoiseTexture;
            uniform sampler2D uAccumulationTexture;
            uniform float uTime;
            uniform vec2 uGravity;
            
            out vec4 FragColor;
            
            void main() {
                // Determine the direction of despair based on the physical device orientation.
                // OpenGL UV origin is bottom-left, physical screen origin is top-left.
                // The sensor returns negative Y when the device is upright, so we invert it.
                // It returns positive X when tilted left, so we invert X to drag the UVs the right way.
                vec2 gravityDir = normalize(vec2(-uGravity.x, -uGravity.y));
                
                vec2 noiseUV = vUV * 5.0;
                float noiseVal = texture(uNoiseTexture, noiseUV).r;
                float dripFactor = noiseVal * (sin(uTime * 0.2) * 0.5 + 0.5) * 0.15;
                
                // Distort reality along the gravity vector.
                vec2 distortedUV = vUV + (gravityDir * dripFactor);
                
                float accumulatedVal = texture(uAccumulationTexture, vUV).r;
                float binaryCut = step(0.5, accumulatedVal); 
                
                vec4 rawColor = texture(uBaseTexture, distortedUV);
                float luminance = dot(rawColor.rgb, vec3(0.299, 0.587, 0.114));
                
                FragColor = vec4(vec3(luminance) * binaryCut, rawColor.a * binaryCut);
            }
        """.trimIndent()
    }
}
