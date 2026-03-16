package com.hereliesaz.abr

import android.graphics.Bitmap
import android.opengl.GLES30
import android.opengl.GLUtils
import kotlin.random.Random

object TextureGenerator {
    fun generateChaos(size: Int = 64): Int {
        val textureHandle = IntArray(1)
        GLES30.glGenTextures(1, textureHandle, 0)
        
        if (textureHandle[0] == 0) {
            throw RuntimeException("The GPU refused to manifest procedural trauma.")
        }

        // A low-resolution canvas of raw, algorithmic static.
        // We rely on GLES30.GL_LINEAR to stretch this tiny block of mathematical noise 
        // into viscous, bleeding blobs across the void.
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        val seed = Random(42) // A deterministic tragedy. It will always bleed the same way.

        for (i in pixels.indices) {
            val intensity = seed.nextInt(256)
            pixels[i] = (0xFF shl 24) or (intensity shl 16) or (intensity shl 8) or intensity
        }
        
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandle[0])
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        
        // The chaos must tile infinitely to consume everything.
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)

        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()

        return textureHandle[0]
    }
}
