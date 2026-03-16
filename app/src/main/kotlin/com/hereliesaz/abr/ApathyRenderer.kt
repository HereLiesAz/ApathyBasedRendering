package com.hereliesaz.abr

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ApathyRenderer : GLSurfaceView.Renderer {

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

    // Abstract placeholders for reality. You'll need to generate these via GLUtils.texImage2D.
    private var baseTextureId: Int = 1
    private var noiseTextureId: Int = 2
    private var stencilTextureId: Int = 3

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

        // TODO: Bind VBOs and execute glDrawArrays to manifest the geometry.
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
            out vec4 FragColor;
            void main() {
                vec2 noiseUV = vUV * 5.0;
                float noiseVal = texture(uNoiseTexture, noiseUV).r;
                float dripFactor = noiseVal * (sin(uTime * 0.2) * 0.5 + 0.5) * 0.15;
                vec2 distortedUV = vUV + vec2(0.0, -dripFactor);
                float stencilVal = texture(uStencilMask, vUV).r;
                float binaryCut = step(0.5, stencilVal); 
                vec4 rawColor = texture(uBaseTexture, distortedUV);
                float luminance = dot(rawColor.rgb, vec3(0.299, 0.587, 0.114));
                FragColor = vec4(vec3(luminance) * binaryCut, rawColor.a * binaryCut);
            }
        """.trimIndent()
    }
}
