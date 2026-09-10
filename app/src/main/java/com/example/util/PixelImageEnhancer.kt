package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

object PixelImageEnhancer {

    /**
     * Enhances low-resolution GBA / retro pixel game screenshots:
     * 1. Upscales using crisp Nearest-Neighbor algorithm to prevent bilinear blur
     * 2. Boosts contrast and thresholding to make pixelated bitmap letters stand out
     * 3. Clarifies dialogue text against textured game backgrounds
     */
    fun enhancePixelText(source: Bitmap, targetMinDim: Int = 1080): Bitmap {
        // Step 1: Calculate crisp integer scale factor
        val minDim = minOf(source.width, source.height)
        val scale = if (minDim < targetMinDim) {
            val s = targetMinDim.toFloat() / minDim
            // Prefer clean integer scale (e.g. 2x, 3x, 4x) for retro pixel art
            Math.ceil(s.toDouble()).toInt().coerceIn(2, 6)
        } else {
            1
        }

        val targetWidth = source.width * scale
        val targetHeight = source.height * scale

        val enhanced = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enhanced)

        val paint = Paint().apply {
            // isFilterBitmap = false ensures NEAREST-NEIGHBOR scaling (preserves crisp pixel edges)
            isFilterBitmap = false
            isDither = false
        }

        // ColorMatrix: Increase contrast and slightly lift highlights to separate pixel text from dark box
        val contrast = 1.30f
        val brightness = 5f
        val colorMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

        canvas.drawBitmap(
            Bitmap.createScaledBitmap(source, targetWidth, targetHeight, false),
            0f, 0f, paint
        )

        return enhanced
    }
}
