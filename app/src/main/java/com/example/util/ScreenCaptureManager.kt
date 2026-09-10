package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ScreenCaptureManager {
    private const val TAG = "ScreenCaptureManager"

    private var appContext: Context? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    private var displayListener: DisplayManager.DisplayListener? = null

    private var screenWidth = 1080
    private var screenHeight = 1920
    private var screenDensity = 320

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    @Volatile
    private var latestBitmap: Bitmap? = null

    val hasProjection: Boolean
        get() = mediaProjection != null

    @SuppressLint("WrongConstant")
    fun init(context: Context, resultCode: Int, data: Intent) {
        try {
            stop()
            appContext = context.applicationContext

            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)

            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
            screenDensity = metrics.densityDpi

            val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = mpm.getMediaProjection(resultCode, data)
            if (projection == null) {
                Log.e(TAG, "MediaProjection was null")
                _isReady.value = false
                return
            }
            mediaProjection = projection

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                projection.registerCallback(object : MediaProjection.Callback() {
                    override fun onStop() {
                        super.onStop()
                        _isReady.value = false
                        stop()
                    }
                }, handler)
            }

            setupVirtualDisplay()

            // Register DisplayListener to react immediately to orientation changes (Portrait <-> Landscape)
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val listener = object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) {}
                override fun onDisplayRemoved(displayId: Int) {}
                override fun onDisplayChanged(displayId: Int) {
                    appContext?.let { ctx -> checkAndRefreshDisplay(ctx) }
                }
            }
            displayListener = listener
            dm?.registerDisplayListener(listener, handler)

            _isReady.value = true
            Log.d(TAG, "MediaProjection initialized: ${screenWidth}x${screenHeight}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MediaProjection: ${e.message}", e)
            _isReady.value = false
        }
    }

    fun checkAndRefreshDisplay(context: Context) {
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)

            val curW = metrics.widthPixels
            val curH = metrics.heightPixels
            val curDensity = metrics.densityDpi

            if (curW != screenWidth || curH != screenHeight) {
                Log.d(TAG, "Orientation/display changed from ${screenWidth}x${screenHeight} to ${curW}x${curH}")
                screenWidth = curW
                screenHeight = curH
                screenDensity = curDensity
                reconfigureVirtualDisplay()
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkAndRefreshDisplay error: ${e.message}")
        }
    }

    private fun reconfigureVirtualDisplay() {
        if (mediaProjection == null) return
        try {
            val oldDisplay = virtualDisplay
            val oldReader = imageReader

            virtualDisplay = null
            imageReader = null

            oldDisplay?.release()
            oldReader?.close()

            synchronized(this) {
                latestBitmap?.recycle()
                latestBitmap = null
            }

            setupVirtualDisplay()
            Log.d(TAG, "VirtualDisplay successfully reconfigured for ${screenWidth}x${screenHeight}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reconfigure VirtualDisplay: ${e.message}", e)
        }
    }

    private fun setupVirtualDisplay() {
        val reader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        reader.setOnImageAvailableListener({ ir ->
            try {
                val image = ir.acquireLatestImage() ?: return@setOnImageAvailableListener
                val bmp = imageToBitmap(image)
                image.close()

                if (bmp != null) {
                    synchronized(this) {
                        val old = latestBitmap
                        latestBitmap = bmp
                        if (old != null && old != bmp && !old.isRecycled) {
                            old.recycle()
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore transient frame skips
            }
        }, handler)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "GameTranslatorCapture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            handler
        )
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val fullWidth = image.width + rowPadding / pixelStride
            val bitmap = Bitmap.createBitmap(fullWidth, image.height, Bitmap.Config.ARGB_8888)
            buffer.rewind()
            bitmap.copyPixelsFromBuffer(buffer)

            if (rowPadding > 0) {
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
                if (cropped != bitmap) {
                    bitmap.recycle()
                }
                cropped
            } else {
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image to bitmap: ${e.message}")
            null
        }
    }

    fun captureLatestScreenshot(): Bitmap? {
        appContext?.let { checkAndRefreshDisplay(it) }

        synchronized(this) {
            latestBitmap?.let { bmp ->
                if (!bmp.isRecycled) {
                    return bmp.copy(Bitmap.Config.ARGB_8888, false)
                }
            }
        }
        return try {
            val img = imageReader?.acquireLatestImage() ?: imageReader?.acquireNextImage()
            img?.use {
                val bmp = imageToBitmap(it)
                if (bmp != null) {
                    synchronized(this) {
                        val old = latestBitmap
                        latestBitmap = bmp.copy(Bitmap.Config.ARGB_8888, false)
                        if (old != null && old != latestBitmap && !old.isRecycled) {
                            old.recycle()
                        }
                    }
                }
                bmp
            }
        } catch (e: Exception) {
            Log.e(TAG, "Direct capture failed: ${e.message}")
            null
        }
    }

    suspend fun acquireScreenshotWithWait(timeoutMs: Long = 800): Bitmap? {
        appContext?.let { checkAndRefreshDisplay(it) }
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val shot = captureLatestScreenshot()
            if (shot != null) return shot
            delay(50)
        }
        return captureLatestScreenshot()
    }

    fun stop() {
        try {
            _isReady.value = false
            displayListener?.let { listener ->
                val dm = appContext?.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                dm?.unregisterDisplayListener(listener)
                displayListener = null
            }
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection?.stop()
            mediaProjection = null
            synchronized(this) {
                latestBitmap?.recycle()
                latestBitmap = null
            }
            appContext = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaProjection: ${e.message}")
        }
    }
}
