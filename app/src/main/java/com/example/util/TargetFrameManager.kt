package com.example.util

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TargetFrameRect(
    val leftRatio: Float = 0.05f,   // 5% from left
    val topRatio: Float = 0.65f,    // 65% from top (dialogue usually at bottom)
    val widthRatio: Float = 0.90f,  // 90% screen width
    val heightRatio: Float = 0.28f  // 28% screen height
)

object TargetFrameManager {
    // Current target frame (null means full screen capture)
    private val _targetFrame = MutableStateFlow<TargetFrameRect?>(TargetFrameRect())
    val targetFrame = _targetFrame.asStateFlow()

    private val _isAimingFrameVisible = MutableStateFlow(false)
    val isAimingFrameVisible = _isAimingFrameVisible.asStateFlow()

    fun setTargetFrame(rect: TargetFrameRect?) {
        _targetFrame.value = rect
    }

    fun setAimingFrameVisible(visible: Boolean) {
        _isAimingFrameVisible.value = visible
    }

    fun hasTargetFrame(): Boolean = _targetFrame.value != null

    /**
     * Crops a full-screen screenshot to the user-defined aiming frame
     */
    fun cropToTarget(bitmap: Bitmap): Bitmap {
        val frame = _targetFrame.value ?: return bitmap
        return try {
            val left = (frame.leftRatio * bitmap.width).toInt().coerceIn(0, (bitmap.width - 20).coerceAtLeast(0))
            val top = (frame.topRatio * bitmap.height).toInt().coerceIn(0, (bitmap.height - 20).coerceAtLeast(0))
            val width = (frame.widthRatio * bitmap.width).toInt().coerceIn(20, bitmap.width - left)
            val height = (frame.heightRatio * bitmap.height).toInt().coerceIn(20, bitmap.height - top)

            if (width <= 0 || height <= 0) return bitmap
            Bitmap.createBitmap(bitmap, left, top, width, height)
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }
}
