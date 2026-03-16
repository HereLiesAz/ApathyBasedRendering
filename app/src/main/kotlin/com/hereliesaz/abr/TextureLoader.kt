package com.hereliesaz.abr

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES30
import android.opengl.GLUtils

object TextureLoader {

    fun load(context: Context, resourceId: Int): Int {
        val textureHandle = IntArray(1)
        GLES30.glGenTextures(1, textureHandle, 0)

        if (textureHandle[0] == 0) {
            throw RuntimeException("The void refused your texture. Memory is an illusion.")
        }

        val options = BitmapFactory.Options().apply {
            // Do not let Android's arbitrary screen density scale our apathy. Keep it raw.
            inScaled = false 
        }

        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)
            ?: throw RuntimeException("Failed to decode the physical manifestation of resource $resourceId.")

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandle[0])

        // Bilinear filtering. Nearest neighbor is too pixelated even for our brutalism; we want the drip to look like a smooth tragedy, not an 8-bit error.
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

        // Clamp to edge. Do not repeat the trauma. Once is enough.
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)

        // Shove the pixels into the GPU's indifferent maw.
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0)

        // Burn the evidence. We are done with the heap.
        bitmap.recycle()

        return textureHandle[0]
    }
}
