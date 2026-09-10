package com.example.service

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.GameTranslatorApp
import com.example.MainActivity
import com.example.R
import com.example.data.model.CharacterPronounConfig
import com.example.data.model.GameEra
import com.example.data.model.TranslationResponsePayload
import com.example.util.GameTextRecognizer
import com.example.util.ScreenCaptureManager
import com.example.util.TargetFrameManager
import com.example.util.TargetFrameRect
import com.example.util.UserPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Floating Overlay Service with:
 * 1. Floating Bubble (Tap = Translate Real Screen, Long-press = Aiming Target Box)
 * 2. Draggable & Freely Resizable Target Aiming Box with ↘ corner handles & presets
 * 3. Freely Movable & Resizable Subtitle Card with font-size controls
 */
class GameOverlayService : Service() {

    companion object {
        const val CHANNEL_ID = "game_translator_overlay_channel"
        const val NOTIFICATION_ID = 2024
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_START_WITH_PROJECTION = "ACTION_START_WITH_PROJECTION"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private val _serviceRunningState = MutableStateFlow(false)
        val serviceRunningState = _serviceRunningState.asStateFlow()

        var isServiceRunning = false
            private set
    }

    private var windowManager: WindowManager? = null

    // Floating Bubble
    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    // Target Aiming Frame (กรอบเล็งข้อความ)
    private var aimingFrameView: View? = null
    private var aimingFrameParams: WindowManager.LayoutParams? = null

    // Result Dialogue Card
    private var overlayCardView: View? = null
    private var cardParams: WindowManager.LayoutParams? = null
    private var isCardShowing = false
    private var currentFontSize = 16f

    // Persistent free position and dimensions for translation card
    private var savedCardX: Int? = null
    private var savedCardY: Int? = null
    private var savedCardWidth: Int? = null
    private var savedCardHeight: Int? = null

    // Auto-Translate Stability & Typewriter detection State
    private var autoTranslateJob: Job? = null
    private var activeTranslationJob: Job? = null
    private var candidateDialogueText: String = ""
    private var candidateLastChangeTime: Long = 0L
    private var lastTranslatedDialogue: String = ""
    private var lastRenderedThai: String = ""
    private var consecutiveEmptyTicks: Int = 0
    @Volatile
    private var isAutoTranslating = false

    // Card & Frame subviews
    private var cardResultsContainer: LinearLayout? = null
    private var cardLoadingBar: ProgressBar? = null
    private var cardStatusText: TextView? = null
    private var cardResultsScroll: ScrollView? = null
    private var cardAutoBtn: TextView? = null
    private var frameAutoBtn: TextView? = null
    private var cardBadge: TextView? = null

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()

        serviceScope.launch {
            UserPreferencesManager.isAutoTranslateEnabled.collect { enabled ->
                if (enabled) {
                    startAutoTranslateLoop()
                } else {
                    stopAutoTranslateLoop()
                }
                updateAllAutoButtons(enabled)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("GameOverlayService", "Orientation changed: ${newConfig.orientation}")
        ScreenCaptureManager.checkAndRefreshDisplay(this)
        adjustViewsForOrientation()
    }

    private fun adjustViewsForOrientation() {
        val metrics = resources.displayMetrics
        val sw = metrics.widthPixels
        val sh = metrics.heightPixels

        // 1. Aiming Frame: readjust based on current ratios
        aimingFrameParams?.let { params ->
            aimingFrameView?.let { view ->
                val rect = TargetFrameManager.targetFrame.value ?: TargetFrameRect()
                params.x = (rect.leftRatio * sw).toInt().coerceIn(0, (sw - 100).coerceAtLeast(0))
                params.y = (rect.topRatio * sh).toInt().coerceIn(0, (sh - 80).coerceAtLeast(0))
                params.width = (rect.widthRatio * sw).toInt().coerceIn(150, (sw - params.x).coerceAtLeast(150))
                params.height = (rect.heightRatio * sh).toInt().coerceIn(60, (sh - params.y).coerceAtLeast(60))
                try {
                    windowManager?.updateViewLayout(view, params)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 2. Floating Bubble: clamp inside new screen boundaries
        bubbleParams?.let { bp ->
            bubbleView?.let { bv ->
                val sizeDp = (58 * metrics.density).toInt()
                bp.x = bp.x.coerceIn(10, (sw - sizeDp - 10).coerceAtLeast(10))
                bp.y = bp.y.coerceIn(10, (sh - sizeDp - 10).coerceAtLeast(10))
                try {
                    windowManager?.updateViewLayout(bv, bp)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 3. Translation Card: adjust width to 92% of new screen and clamp Y
        cardParams?.let { cp ->
            overlayCardView?.let { cv ->
                cp.width = (sw * 0.92f).toInt()
                cp.x = (sw * 0.04f).toInt()
                cp.y = cp.y.coerceIn(20, (sh - 120).coerceAtLeast(20))
                try {
                    windowManager?.updateViewLayout(cv, cp)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                isServiceRunning = false
                _serviceRunningState.value = false
                stopAutoTranslateLoop()
                ScreenCaptureManager.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                isServiceRunning = true
                _serviceRunningState.value = true
                try {
                    val notification = createNotification()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }

                    // Process MediaProjection token if attached
                    val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
                    val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent?.getParcelableExtra(EXTRA_RESULT_DATA)
                    }

                    if (resultCode == Activity.RESULT_OK && resultData != null) {
                        ScreenCaptureManager.init(applicationContext, resultCode, resultData)
                        ScreenCaptureManager.checkAndRefreshDisplay(this)
                        Log.d("GameOverlayService", "MediaProjection successfully initialized in service!")
                    }

                    setupBubbleView()

                    if (UserPreferencesManager.isAutoTranslateEnabled.value) {
                        startAutoTranslateLoop()
                    }
                } catch (e: Exception) {
                    Log.e("GameOverlayService", "Failed to start foreground service: ${e.message}", e)
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Game Translator Bubble Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating translation bubble over games"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Game Translator ทำงานอยู่")
            .setContentText("แตะปุ่มเพื่อแปล • กดค้างเพื่อเปิดกรอบเล็งข้อความเกม")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBubbleView() {
        if (bubbleView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 350
        }

        val sizeDp = (58 * resources.displayMetrics.density).toInt()
        val bubbleFrame = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(sizeDp, sizeDp)

            // High polish glowing radial gradient
            val backgroundDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(
                    Color.parseColor("#0284C7"), // Cyan
                    Color.parseColor("#1E1B4B")  // Deep Indigo
                )
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = sizeDp.toFloat()
                setStroke((2 * resources.displayMetrics.density).toInt(), Color.parseColor("#38BDF8"))
            }
            background = backgroundDrawable
            elevation = 16f
        }

        val icon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setColorFilter(Color.WHITE)
            val padding = (14 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        bubbleFrame.addView(icon)

        // Drag, Tap & Long-Press Handling
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var hasMoved = false
        var isLongPressTriggered = false

        val longPressRunnable = Runnable {
            if (!hasMoved) {
                isLongPressTriggered = true
                triggerVibration()
                toggleAimingFrameView()
            }
        }

        bubbleFrame.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams?.x ?: 0
                    initialY = bubbleParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    hasMoved = false
                    isLongPressTriggered = false
                    mainHandler.postDelayed(longPressRunnable, 450)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                        hasMoved = true
                        mainHandler.removeCallbacks(longPressRunnable)
                    }
                    if (!isLongPressTriggered) {
                        bubbleParams?.x = initialX + dx
                        bubbleParams?.y = initialY + dy
                        windowManager?.updateViewLayout(bubbleFrame, bubbleParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    mainHandler.removeCallbacks(longPressRunnable)
                    if (!isLongPressTriggered) {
                        if (!hasMoved) {
                            // User tapped the bubble!
                            triggerVibration()
                            onBubbleClicked()
                        } else {
                            // Snap to nearest screen edge
                            val screenWidth = resources.displayMetrics.widthPixels
                            val finalX = if ((bubbleParams?.x ?: 0) < screenWidth / 2) 20 else screenWidth - sizeDp - 20
                            bubbleParams?.x = finalX
                            windowManager?.updateViewLayout(bubbleFrame, bubbleParams)
                        }
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    mainHandler.removeCallbacks(longPressRunnable)
                    true
                }
                else -> false
            }
        }

        bubbleView = bubbleFrame
        try {
            windowManager?.addView(bubbleView, bubbleParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(35)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration permission exceptions
        }
    }

    // ==========================================
    // 🎯 TARGET AIMING FRAME (กรอบเล็งข้อความเกม)
    // ==========================================
    private fun toggleAimingFrameView() {
        if (aimingFrameView != null) {
            hideAimingFrameView()
        } else {
            showAimingFrameView()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showAimingFrameView() {
        if (aimingFrameView != null) return

        val (screenWidth, screenHeight) = getRealScreenSize()
        val metrics = resources.displayMetrics

        val currentRect = TargetFrameManager.targetFrame.value ?: TargetFrameRect()
        val frameX = (currentRect.leftRatio * screenWidth).toInt()
        val frameY = (currentRect.topRatio * screenHeight).toInt()
        val frameW = (currentRect.widthRatio * screenWidth).toInt().coerceAtLeast((140 * metrics.density).toInt())
        val frameH = (currentRect.heightRatio * screenHeight).toInt().coerceAtLeast((60 * metrics.density).toInt())

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            frameW,
            frameH,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = frameX
            y = frameY
        }
        aimingFrameParams = params

        // Root FrameLayout allowing overlays of corner & edge handles
        val rootFrame = FrameLayout(this)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val borderBg = GradientDrawable().apply {
                setColor(Color.parseColor("#220284C7")) // 13% translucent cyan/sky
                cornerRadius = 14 * metrics.density
                setStroke((3.5f * metrics.density).toInt(), Color.parseColor("#F59E0B")) // Glowing Amber
            }
            background = borderBg
            val p = (6 * metrics.density).toInt()
            setPadding(p, p, p, p)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        rootFrame.addView(container)

        // Header bar with Move handle, live size, lock, and clear buttons
        val headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#EB0F172A"))
                cornerRadius = 8 * metrics.density
            }
            background = bg
            val hp = (6 * metrics.density).toInt()
            setPadding(hp * 2, hp, hp * 2, hp)
        }

        val title = TextView(this).apply {
            text = "⠿ 🎯 กรอบเล็ง"
            setTextColor(Color.parseColor("#F59E0B"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
        }
        headerBar.addView(title)

        val sizeIndicator = TextView(this).apply {
            text = " ${params.width}x${params.height}"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 10f
            typeface = Typeface.MONOSPACE
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val p = (4 * metrics.density).toInt()
            setPadding(p, 0, 0, 0)
        }
        headerBar.addView(sizeIndicator)

        val lockBtn = Button(this).apply {
            text = "✓ ล็อค"
            textSize = 11f
            setTextColor(Color.WHITE)
            val btnBg = GradientDrawable().apply {
                setColor(Color.parseColor("#059669")) // Emerald
                cornerRadius = 6 * metrics.density
            }
            background = btnBg
            val bp = (4 * metrics.density).toInt()
            setPadding(bp * 2, bp, bp * 2, bp)
            setOnClickListener {
                saveCurrentTargetRect(params.x, params.y, params.width, params.height)
                hideAimingFrameView()
                Toast.makeText(this@GameOverlayService, "🎯 บันทึกกรอบแล้ว! แตะบับเบิลเพื่อแปลเฉพาะจุดนี้", Toast.LENGTH_SHORT).show()
            }
        }
        headerBar.addView(lockBtn)

        val autoFrameBtn = TextView(this).apply {
            val isAuto = UserPreferencesManager.isAutoTranslateEnabled.value
            text = if (isAuto) " ⚡ ออโต้: ON " else " ⚡ ออโต้: OFF "
            setTextColor(if (isAuto) Color.parseColor("#10B981") else Color.parseColor("#94A3B8"))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            val p = (4 * metrics.density).toInt()
            setPadding(p, 0, p, 0)
            val bgDrawable = GradientDrawable().apply {
                setColor(if (isAuto) Color.parseColor("#1E3A2F") else Color.parseColor("#1E293B"))
                cornerRadius = 6 * metrics.density
                setStroke((1 * metrics.density).toInt(), if (isAuto) Color.parseColor("#10B981") else Color.parseColor("#475569"))
            }
            background = bgDrawable
            setOnClickListener {
                val newState = !UserPreferencesManager.isAutoTranslateEnabled.value
                UserPreferencesManager.setAutoTranslateEnabled(newState)
                Toast.makeText(this@GameOverlayService, if (newState) "⚡ เปิดแปลอัตโนมัติ" else "ปิดแปลอัตโนมัติ", Toast.LENGTH_SHORT).show()
            }
        }
        frameAutoBtn = autoFrameBtn
        headerBar.addView(autoFrameBtn)

        val closeBtn = TextView(this).apply {
            text = "  ✕ ทั้งจอ  "
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                TargetFrameManager.setTargetFrame(null) // Reset to Full screen mode
                hideAimingFrameView()
                Toast.makeText(this@GameOverlayService, "ปิดกรอบเล็ง (จะจับภาพทั้งหน้าจอ)", Toast.LENGTH_SHORT).show()
            }
        }
        headerBar.addView(closeBtn)
        container.addView(headerBar)

        // Center Hint & Drag Area
        val centerGuide = TextView(this).apply {
            text = "ลากย้ายกรอบได้อิสระ • ลากมุม ◢ ขวาล่างเพื่อย่อขยายขนาด"
            setTextColor(Color.parseColor("#E2E8F0"))
            textSize = 11f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        container.addView(centerGuide)

        // Bottom Controls Bar (Quick presets & resize step buttons)
        val controlBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val bg = GradientDrawable().apply {
                setColor(Color.parseColor("#E60F172A"))
                cornerRadius = 8 * metrics.density
            }
            background = bg
            val cp = (4 * metrics.density).toInt()
            setPadding(cp * 2, cp, cp * 2, cp)
        }

        fun updateLayoutAndSave() {
            windowManager?.updateViewLayout(rootFrame, params)
            sizeIndicator.text = " ${params.width}x${params.height}"
            saveCurrentTargetRect(params.x, params.y, params.width, params.height)
        }

        val bottomPreset = TextView(this).apply {
            text = "💬 ล่างจอ"
            textSize = 10f
            setTextColor(Color.parseColor("#38BDF8"))
            val p = (4 * metrics.density).toInt()
            setPadding(p, p, p, p)
            setOnClickListener {
                params.x = (0.04f * screenWidth).toInt()
                params.y = (0.64f * screenHeight).toInt()
                params.width = (0.92f * screenWidth).toInt()
                params.height = (0.28f * screenHeight).toInt()
                updateLayoutAndSave()
            }
        }
        controlBar.addView(bottomPreset)

        val centerPreset = TextView(this).apply {
            text = "🎯 กลางจอ"
            textSize = 10f
            setTextColor(Color.parseColor("#38BDF8"))
            val p = (4 * metrics.density).toInt()
            setPadding(p, p, p, p)
            setOnClickListener {
                params.x = (0.08f * screenWidth).toInt()
                params.y = (0.40f * screenHeight).toInt()
                params.width = (0.84f * screenWidth).toInt()
                params.height = (0.22f * screenHeight).toInt()
                updateLayoutAndSave()
            }
        }
        controlBar.addView(centerPreset)

        val fullWidthPreset = TextView(this).apply {
            text = "📱 เต็มกว้าง"
            textSize = 10f
            setTextColor(Color.parseColor("#38BDF8"))
            val p = (4 * metrics.density).toInt()
            setPadding(p, p, p, p)
            setOnClickListener {
                params.x = (0.03f * screenWidth).toInt()
                params.width = (0.94f * screenWidth).toInt()
                updateLayoutAndSave()
            }
        }
        controlBar.addView(fullWidthPreset)

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
        controlBar.addView(spacer)

        val minusW = TextView(this).apply {
            text = "◀ ย่อ "
            textSize = 10f
            setTextColor(Color.WHITE)
            setOnClickListener {
                params.width = (params.width - 40 * metrics.density).toInt().coerceAtLeast((120 * metrics.density).toInt())
                updateLayoutAndSave()
            }
        }
        controlBar.addView(minusW)

        val plusW = TextView(this).apply {
            text = " ขยาย ▶ "
            textSize = 10f
            setTextColor(Color.WHITE)
            setOnClickListener {
                params.width = (params.width + 40 * metrics.density).toInt().coerceAtMost(screenWidth - params.x)
                updateLayoutAndSave()
            }
        }
        controlBar.addView(plusW)

        val minusH = TextView(this).apply {
            text = " ➖ สูง "
            textSize = 10f
            setTextColor(Color.WHITE)
            setOnClickListener {
                params.height = (params.height - 30 * metrics.density).toInt().coerceAtLeast((70 * metrics.density).toInt())
                updateLayoutAndSave()
            }
        }
        controlBar.addView(minusH)

        val plusH = TextView(this).apply {
            text = " ➕ สูง "
            textSize = 10f
            setTextColor(Color.WHITE)
            setOnClickListener {
                params.height = (params.height + 30 * metrics.density).toInt().coerceAtMost((screenHeight * 0.85f).toInt())
                updateLayoutAndSave()
            }
        }
        controlBar.addView(plusH)

        container.addView(controlBar)

        // Drag anywhere on frame (header & center) to reposition
        var initMoveX = 0
        var initMoveY = 0
        var startMoveTouchX = 0f
        var startMoveTouchY = 0f

        val moveTouchListener = View.OnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    initMoveX = params.x
                    initMoveY = params.y
                    startMoveTouchX = ev.rawX
                    startMoveTouchY = ev.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = (initMoveX + (ev.rawX - startMoveTouchX)).toInt()
                    params.y = (initMoveY + (ev.rawY - startMoveTouchY)).toInt()
                    windowManager?.updateViewLayout(rootFrame, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    saveTargetRectFromFrame()
                    true
                }
                else -> false
            }
        }
        headerBar.setOnTouchListener(moveTouchListener)
        centerGuide.setOnTouchListener(moveTouchListener)

        // ↘ FREELY RESIZABLE CORNER HANDLE AT BOTTOM-RIGHT
        val resizeCornerHandle = TextView(this).apply {
            text = "◢"
            textSize = 24f
            setTextColor(Color.parseColor("#F59E0B")) // Amber
            gravity = Gravity.BOTTOM or Gravity.END
            val handleSize = (50 * metrics.density).toInt()
            val lp = FrameLayout.LayoutParams(handleSize, handleSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
            }
            layoutParams = lp
            val p = (4 * metrics.density).toInt()
            setPadding(0, 0, p, p)
        }

        var startW = 0
        var startH = 0
        var startResizeX = 0f
        var startResizeY = 0f

        resizeCornerHandle.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startW = params.width
                    startH = params.height
                    startResizeX = ev.rawX
                    startResizeY = ev.rawY
                    triggerVibration()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dw = (ev.rawX - startResizeX).toInt()
                    val dh = (ev.rawY - startResizeY).toInt()
                    val newW = (startW + dw).coerceIn((120 * metrics.density).toInt(), screenWidth - params.x)
                    val newH = (startH + dh).coerceIn((60 * metrics.density).toInt(), screenHeight - params.y)
                    params.width = newW
                    params.height = newH
                    windowManager?.updateViewLayout(rootFrame, params)
                    sizeIndicator.text = " ${newW}x${newH}"
                    saveTargetRectFromFrame()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    saveTargetRectFromFrame()
                    true
                }
                else -> false
            }
        }
        rootFrame.addView(resizeCornerHandle)

        aimingFrameView = rootFrame
        try {
            windowManager?.addView(aimingFrameView, params)
            TargetFrameManager.setAimingFrameVisible(true)
            rootFrame.post { saveTargetRectFromFrame() }
            Toast.makeText(this, "🎯 กรอบเล็งเปิดแล้ว: ลากย้ายตำแหน่ง หรือลากมุม ◢ เพื่อย่อขยาย", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getRealScreenSize(): Pair<Int, Int> {
        val wm = getSystemService(WINDOW_SERVICE) as? WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm?.defaultDisplay?.getRealMetrics(metrics) ?: return Pair(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels)
        return Pair(metrics.widthPixels, metrics.heightPixels)
    }

    private fun saveTargetRectFromFrame() {
        val frame = aimingFrameView ?: return
        val (realW, realH) = getRealScreenSize()
        val loc = IntArray(2)
        frame.getLocationOnScreen(loc)
        val x = loc[0].coerceAtLeast(0)
        val y = loc[1].coerceAtLeast(0)
        val w = frame.width.coerceAtLeast(40)
        val h = frame.height.coerceAtLeast(40)

        val rect = TargetFrameRect(
            leftRatio = (x.toFloat() / realW.toFloat()).coerceIn(0f, 0.98f),
            topRatio = (y.toFloat() / realH.toFloat()).coerceIn(0f, 0.98f),
            widthRatio = (w.toFloat() / realW.toFloat()).coerceIn(0.02f, 1f),
            heightRatio = (h.toFloat() / realH.toFloat()).coerceIn(0.02f, 1f)
        )
        TargetFrameManager.setTargetFrame(rect)
        candidateDialogueText = ""
        lastTranslatedDialogue = "" // reset cache so newly framed dialogue triggers translation immediately
    }

    private fun saveCurrentTargetRect(x: Int, y: Int, w: Int, h: Int) {
        val (realW, realH) = getRealScreenSize()
        val rect = TargetFrameRect(
            leftRatio = (x.toFloat() / realW.toFloat()).coerceIn(0f, 0.98f),
            topRatio = (y.toFloat() / realH.toFloat()).coerceIn(0f, 0.98f),
            widthRatio = (w.toFloat() / realW.toFloat()).coerceIn(0.02f, 1f),
            heightRatio = (h.toFloat() / realH.toFloat()).coerceIn(0.02f, 1f)
        )
        TargetFrameManager.setTargetFrame(rect)
        candidateDialogueText = ""
        lastTranslatedDialogue = "" // reset cache so newly framed dialogue triggers translation immediately
    }

    private fun hideAimingFrameView() {
        if (aimingFrameView != null) {
            try {
                windowManager?.removeView(aimingFrameView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            aimingFrameView = null
            aimingFrameParams = null
            frameAutoBtn = null
            TargetFrameManager.setAimingFrameVisible(false)
        }
    }

    // ==========================================
    // 💬 ULTRA-CLEAN DIALOGUE TRANSLATION CARD (FREELY MOVABLE & RESIZABLE)
    // ==========================================
    private fun onBubbleClicked() {
        if (isCardShowing) {
            dismissOverlayCard()
            return
        }
        showTranslationOverlayCard()
    }

    private fun updateDynamicFontSize() {
        val cardW = cardParams?.width ?: return
        val cardH = cardParams?.height ?: return
        val density = resources.displayMetrics.density

        // Compute proportional font size based on card dimensions
        val baseSpFromWidth = (cardW / (density * 22f))
        val baseSpFromHeight = if (cardH > 0) (cardH / (density * 8f)) else baseSpFromWidth
        currentFontSize = minOf(baseSpFromWidth, baseSpFromHeight).coerceIn(12f, 26f)

        cardResultsContainer?.let { container ->
            for (i in 0 until container.childCount) {
                val child = container.getChildAt(i)
                if (child is TextView) {
                    child.textSize = currentFontSize
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showTranslationOverlayCard(autoTriggered: Boolean = false) {
        if (overlayCardView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val (screenWidth, screenHeight) = getRealScreenSize()
        val metrics = resources.displayMetrics
        val density = metrics.density

        val initialW = savedCardWidth ?: (screenWidth * 0.90f).toInt()
        val initialH = savedCardHeight ?: (screenHeight * 0.22f).toInt()
        val initialX = savedCardX ?: (screenWidth * 0.05f).toInt()
        val initialY = savedCardY ?: (screenHeight * 0.68f).toInt()

        cardParams = WindowManager.LayoutParams(
            initialW,
            initialH,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }

        val rootCardFrame = FrameLayout(this)

        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                setColor(Color.argb(225, 15, 23, 42)) // 88% Slate 900 translucent
                cornerRadius = 16 * density
                setStroke((1.5f * density).toInt(), Color.parseColor("#38BDF8"))
            }
            background = bg
            elevation = 20f
            val pad = (8 * density).toInt()
            setPadding(pad, (4 * density).toInt(), pad, pad)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        rootCardFrame.addView(cardLayout)

        // Minimal Header bar: Drag handle, Auto toggle, and Close button
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val hp = (2 * density).toInt()
            setPadding(0, hp, 0, hp)
        }

        val dragGrip = TextView(this).apply {
            text = "⠿ "
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            val p = (4 * density).toInt()
            setPadding(p, 0, p, 0)
        }
        headerRow.addView(dragGrip)

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
        headerRow.addView(spacer)

        // Minimalist Auto toggle
        val autoCardBtn = TextView(this).apply {
            val isAuto = UserPreferencesManager.isAutoTranslateEnabled.value
            text = if (isAuto) " ⚡ Auto " else " ⚡ Off "
            setTextColor(if (isAuto) Color.parseColor("#10B981") else Color.parseColor("#94A3B8"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            val p = (3 * density).toInt()
            setPadding(p, 0, p, 0)
            val bgDrawable = GradientDrawable().apply {
                setColor(if (isAuto) Color.parseColor("#1E3A2F") else Color.parseColor("#1E293B"))
                cornerRadius = 5 * density
                setStroke((1 * density).toInt(), if (isAuto) Color.parseColor("#10B981") else Color.parseColor("#475569"))
            }
            background = bgDrawable
            setOnClickListener {
                val newState = !UserPreferencesManager.isAutoTranslateEnabled.value
                UserPreferencesManager.setAutoTranslateEnabled(newState)
                Toast.makeText(this@GameOverlayService, if (newState) "⚡ เปิดแปลอัตโนมัติ" else "ปิดแปลอัตโนมัติ", Toast.LENGTH_SHORT).show()
            }
        }
        cardAutoBtn = autoCardBtn
        headerRow.addView(autoCardBtn)

        val closeBtn = TextView(this).apply {
            text = "  ✕"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            val p = (4 * density).toInt()
            setPadding(p, 0, p, 0)
            setOnClickListener { dismissOverlayCard() }
        }
        headerRow.addView(closeBtn)
        cardLayout.addView(headerRow)

        // Freely drag Translation Card anywhere on screen
        var initCardX = 0
        var initCardY = 0
        var startCardTouchX = 0f
        var startCardTouchY = 0f

        val cardMoveListener = View.OnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    initCardX = cardParams?.x ?: 0
                    initCardY = cardParams?.y ?: 0
                    startCardTouchX = ev.rawX
                    startCardTouchY = ev.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val newX = (initCardX + (ev.rawX - startCardTouchX)).toInt()
                    val newY = (initCardY + (ev.rawY - startCardTouchY)).toInt()
                    cardParams?.x = newX
                    cardParams?.y = newY
                    savedCardX = newX
                    savedCardY = newY
                    windowManager?.updateViewLayout(rootCardFrame, cardParams)
                    true
                }
                else -> false
            }
        }
        headerRow.setOnTouchListener(cardMoveListener)
        dragGrip.setOnTouchListener(cardMoveListener)

        // Loading Progress Bar (subtle)
        val loadingBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = View.GONE
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (4 * density).toInt()
            ).apply {
                topMargin = (2 * density).toInt()
                bottomMargin = (2 * density).toInt()
            }
            layoutParams = lp
        }
        cardLoadingBar = loadingBar
        cardLayout.addView(loadingBar)

        // Scrollable clean results container (Displays ONLY translated dialogue)
        val resultsScroll = ScrollView(this).apply {
            isFillViewport = true
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            layoutParams = lp
        }
        cardResultsScroll = resultsScroll

        val resultsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        cardResultsContainer = resultsContainer
        resultsScroll.addView(resultsContainer)
        cardLayout.addView(resultsScroll)

        // ↘ FREELY RESIZABLE CORNER FOR TRANSLATION CARD (WIDTH & HEIGHT)
        val cardResizeHandle = TextView(this).apply {
            text = "◢"
            textSize = 22f
            setTextColor(Color.parseColor("#38BDF8"))
            gravity = Gravity.BOTTOM or Gravity.END
            val handleSize = (44 * density).toInt()
            val lp = FrameLayout.LayoutParams(handleSize, handleSize).apply {
                gravity = Gravity.BOTTOM or Gravity.END
            }
            layoutParams = lp
            val p = (2 * density).toInt()
            setPadding(0, 0, p, p)
        }

        var startCardW = 0
        var startCardH = 0
        var startCardResizeX = 0f
        var startCardResizeY = 0f

        cardResizeHandle.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startCardW = cardParams?.width ?: initialW
                    startCardH = cardParams?.height ?: initialH
                    startCardResizeX = ev.rawX
                    startCardResizeY = ev.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dw = (ev.rawX - startCardResizeX).toInt()
                    val dh = (ev.rawY - startCardResizeY).toInt()
                    val (realW, realH) = getRealScreenSize()
                    val newW = (startCardW + dw).coerceIn((140 * density).toInt(), realW - (cardParams?.x ?: 0))
                    val newH = (startCardH + dh).coerceIn((50 * density).toInt(), realH - (cardParams?.y ?: 0))
                    cardParams?.width = newW
                    cardParams?.height = newH
                    savedCardWidth = newW
                    savedCardHeight = newH
                    windowManager?.updateViewLayout(rootCardFrame, cardParams)
                    updateDynamicFontSize()
                    true
                }
                else -> false
            }
        }
        rootCardFrame.addView(cardResizeHandle)

        overlayCardView = rootCardFrame
        isCardShowing = true

        try {
            windowManager?.addView(overlayCardView, cardParams)
            rootCardFrame.post { updateDynamicFontSize() }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!autoTriggered) {
            executeTranslationProcess(null, isAuto = false)
        }
    }

    private fun renderDialogueResults(payload: TranslationResponsePayload, isAuto: Boolean, currentGame: String) {
        if (payload.translations.isEmpty()) {
            if (!isAuto) {
                cardResultsContainer?.removeAllViews()
                val emptyView = TextView(this@GameOverlayService).apply {
                    text = "ไม่พบตัวอักษรในบริเวณนี้\n(ลองลากกรอบเล็งไปครอบกล่องบทสนทนาในเกม $currentGame)"
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setPadding(0, 10, 0, 10)
                }
                cardResultsContainer?.addView(emptyView)
            }
            return
        }

        // Flatten dialogues into continuous sentences without artificial line-breaks from OCR blocks
        val combinedThai = payload.translations.joinToString(" ") { block ->
            val speaker = if (!block.speaker.isNullOrBlank() && block.speaker.lowercase() != "null") "${block.speaker}: " else ""
            val cleanText = block.translatedText.replace("\r", " ").replace("\n", " ").replace(Regex("\\s+"), " ").trim()
            "$speaker$cleanText"
        }.replace(Regex("\\s+"), " ").trim()

        // If identical to what is already on screen, avoid redraw flicker!
        if (combinedThai.isNotBlank() && combinedThai == lastRenderedThai) {
            cardLoadingBar?.visibility = View.GONE
            return
        }
        lastRenderedThai = combinedThai

        // Render CLEAN continuous dialogue text across a smooth full-width layout
        cardResultsContainer?.removeAllViews()

        // Group by speaker or present as long continuous stream
        val spannableBuilder = SpannableStringBuilder()

        payload.translations.forEachIndexed { index, block ->
            val speakerName = block.speaker?.trim()
            val cleanThaiText = block.translatedText
                .replace("\r", " ")
                .replace("\n", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (cleanThaiText.isBlank()) return@forEachIndexed

            if (index > 0) {
                spannableBuilder.append(" ")
            }

            val prefix = if (!speakerName.isNullOrBlank() && speakerName.lowercase() != "null") {
                "$speakerName: "
            } else ""

            val startIdx = spannableBuilder.length
            spannableBuilder.append(prefix)
            if (prefix.isNotEmpty()) {
                spannableBuilder.setSpan(
                    ForegroundColorSpan(Color.parseColor("#F59E0B")),
                    startIdx,
                    startIdx + prefix.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannableBuilder.setSpan(
                    StyleSpan(Typeface.BOLD),
                    startIdx,
                    startIdx + prefix.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            val textStart = spannableBuilder.length
            spannableBuilder.append(cleanThaiText)
            spannableBuilder.setSpan(
                ForegroundColorSpan(Color.parseColor("#F8FAFC")),
                textStart,
                spannableBuilder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val dialogueView = TextView(this@GameOverlayService).apply {
            text = spannableBuilder
            textSize = currentFontSize
            setLineSpacing(4 * resources.displayMetrics.density, 1.28f)
            val pad = (4 * resources.displayMetrics.density).toInt()
            setPadding(0, pad, 0, pad)
        }
        cardResultsContainer?.addView(dialogueView)

        cardLoadingBar?.visibility = View.GONE
        updateDynamicFontSize()
    }

    private fun executeTranslationProcess(providedBitmap: Bitmap?, isAuto: Boolean) {
        val metrics = resources.displayMetrics

        if (!isAuto) {
            cardLoadingBar?.visibility = View.VISIBLE
            cardStatusText?.visibility = View.GONE
            cardResultsScroll?.visibility = View.VISIBLE
        } else {
            // In auto mode, only show loading spinner if card is currently empty to prevent distracting flash
            if ((cardResultsContainer?.childCount ?: 0) == 0) {
                cardLoadingBar?.visibility = View.VISIBLE
            }
        }

        // Cancel previous in-flight translation job to prevent race conditions
        activeTranslationJob?.cancel()
        activeTranslationJob = serviceScope.launch {
            try {
                // 1. Acquire Bitmap with instant capture fallback
                val bitmapToTranslate: Bitmap = if (providedBitmap != null) {
                    providedBitmap
                } else {
                    val fullScreenshot = if (ScreenCaptureManager.hasProjection) {
                        ScreenCaptureManager.captureLatestScreenshot()
                            ?: ScreenCaptureManager.acquireScreenshotWithWait(150)
                    } else null

                    if (fullScreenshot == null && !ScreenCaptureManager.hasProjection) {
                        if (!isAuto) {
                            withContext(Dispatchers.Main) {
                                cardLoadingBar?.visibility = View.GONE
                                cardStatusText?.visibility = View.GONE
                                cardResultsScroll?.visibility = View.VISIBLE
                                cardResultsContainer?.removeAllViews()

                                val warnBox = LinearLayout(this@GameOverlayService).apply {
                                    orientation = LinearLayout.VERTICAL
                                    gravity = Gravity.CENTER
                                    val p = (10 * metrics.density).toInt()
                                    setPadding(p, p, p, p)
                                }

                                val warnView = TextView(this@GameOverlayService).apply {
                                    text = "📌 ยังไม่พบสิทธิ์จับภาพหน้าจอเกม"
                                    setTextColor(Color.parseColor("#FCD34D"))
                                    textSize = 14f
                                    typeface = Typeface.DEFAULT_BOLD
                                    gravity = Gravity.CENTER
                                }
                                warnBox.addView(warnView)

                                val openPermissionBtn = Button(this@GameOverlayService).apply {
                                    text = "⚡ แตะเพื่อเปิดสิทธิ์ 'บันทึกหน้าจอ' ทันที"
                                    textSize = 12f
                                    setTextColor(Color.WHITE)
                                    val btnBg = GradientDrawable().apply {
                                        setColor(Color.parseColor("#D97706"))
                                        cornerRadius = 8 * metrics.density
                                    }
                                    background = btnBg
                                    val bp = (6 * metrics.density).toInt()
                                    setPadding(bp * 2, bp, bp * 2, bp)
                                    val lp = LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                    ).apply {
                                        topMargin = (8 * metrics.density).toInt()
                                    }
                                    layoutParams = lp
                                    setOnClickListener {
                                        val appIntent = Intent(this@GameOverlayService, MainActivity::class.java).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                            putExtra(MainActivity.EXTRA_REQUEST_CAPTURE, true)
                                        }
                                        startActivity(appIntent)
                                        dismissOverlayCard()
                                    }
                                }
                                warnBox.addView(openPermissionBtn)
                                cardResultsContainer?.addView(warnBox)
                            }
                        }
                        return@launch
                    }

                    if (fullScreenshot != null) {
                        if (TargetFrameManager.hasTargetFrame()) {
                            TargetFrameManager.cropToTarget(fullScreenshot)
                        } else {
                            fullScreenshot
                        }
                    } else {
                        Bitmap.createBitmap(800, 450, Bitmap.Config.ARGB_8888)
                    }
                }

                val currentGame = UserPreferencesManager.gameTitle.value.ifBlank { "Pokémon" }
                val currentEra = if (currentGame.contains("pokemon", ignoreCase = true) ||
                    currentGame.contains("โปเกมอน", ignoreCase = true) ||
                    currentGame.contains("emerald", ignoreCase = true) ||
                    currentGame.contains("gba", ignoreCase = true)
                ) {
                    GameEra.RETRO_PIXEL_GBA
                } else {
                    UserPreferencesManager.gameEra.value
                }

                val repo = GameTranslatorApp.instance.repository
                val result = repo.translateGameScreen(
                    bitmap = bitmapToTranslate,
                    gameTitle = currentGame,
                    era = currentEra,
                    pronounConfig = CharacterPronounConfig("ฉัน", "เธอ", "สำนวนเกม"),
                    isPixelEnhanceEnabled = (currentEra == GameEra.RETRO_PIXEL_GBA),
                    provider = UserPreferencesManager.selectedProvider.value,
                    onInstantPreview = if (isAuto) null else { previewPayload ->
                        withContext(Dispatchers.Main) {
                            cardLoadingBar?.visibility = View.VISIBLE
                            cardStatusText?.visibility = View.GONE
                            cardResultsScroll?.visibility = View.VISIBLE
                            renderDialogueResults(previewPayload, isAuto = isAuto, currentGame = currentGame)
                        }
                    }
                )

                result.onSuccess { payload ->
                    withContext(Dispatchers.Main) {
                        cardLoadingBar?.visibility = View.GONE
                        cardStatusText?.visibility = View.GONE
                        cardResultsScroll?.visibility = View.VISIBLE
                        renderDialogueResults(payload, isAuto = isAuto, currentGame = currentGame)
                    }
                }.onFailure { err ->
                    withContext(Dispatchers.Main) {
                        cardLoadingBar?.visibility = View.GONE
                        cardStatusText?.visibility = View.GONE
                        if (!isAuto && (cardResultsContainer?.childCount ?: 0) == 0) {
                            cardResultsScroll?.visibility = View.VISIBLE
                            cardResultsContainer?.removeAllViews()

                            val errorView = TextView(this@GameOverlayService).apply {
                                text = "การแปลขัดข้อง: ${err.message ?: "โปรดตรวจสอบ API Key"}"
                                setTextColor(Color.parseColor("#F87171"))
                                textSize = 13f
                                gravity = Gravity.CENTER
                                setPadding(0, 10, 0, 10)
                            }
                            cardResultsContainer?.addView(errorView)
                        }
                    }
                }
            } finally {
                isAutoTranslating = false
            }
        }
    }

    private fun startAutoTranslateLoop() {
        if (autoTranslateJob?.isActive == true) return
        autoTranslateJob = serviceScope.launch {
            Log.d("GameOverlayService", "Auto-translate background loop started")
            while (isActive && isServiceRunning && UserPreferencesManager.isAutoTranslateEnabled.value) {
                delay(250) // Responsive polling to monitor typewriter animation and screen changes

                if (!ScreenCaptureManager.hasProjection || isAutoTranslating) {
                    continue
                }

                performAutoTranslateTick()
            }
        }
    }

    private fun stopAutoTranslateLoop() {
        autoTranslateJob?.cancel()
        autoTranslateJob = null
        activeTranslationJob?.cancel()
        activeTranslationJob = null
        candidateDialogueText = ""
        candidateLastChangeTime = 0L
        isAutoTranslating = false
    }

    private suspend fun performAutoTranslateTick() {
        if (isAutoTranslating) return
        try {
            val fullScreenshot = ScreenCaptureManager.captureLatestScreenshot() ?: return
            val bitmapToAnalyze = if (TargetFrameManager.hasTargetFrame()) {
                TargetFrameManager.cropToTarget(fullScreenshot)
            } else {
                fullScreenshot
            }

            val ocrBlocks = GameTextRecognizer.recognizeGameText(bitmapToAnalyze)
            if (ocrBlocks.isEmpty()) {
                consecutiveEmptyTicks++
                if (consecutiveEmptyTicks >= 4) {
                    // Screen has cleared dialogue box
                    candidateDialogueText = ""
                    candidateLastChangeTime = 0L
                    lastTranslatedDialogue = ""
                    if (isCardShowing) {
                        withContext(Dispatchers.Main) {
                            dismissOverlayCard()
                        }
                    }
                }
                return
            }

            // Text detected on screen
            consecutiveEmptyTicks = 0

            // Assemble OCR blocks in natural reading order
            val sortedBlocks = ocrBlocks.sortedBy { it.boundingBox?.top ?: 0 }
            val rawCombined = sortedBlocks
                .map { it.text.trim() }
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .replace(Regex("\\s+"), " ")
                .trim()

            if (!isGenuineDialogue(rawCombined)) {
                return
            }

            val cleanCurrent = cleanDialogueForComparison(rawCombined)
            val cleanLastTranslated = cleanDialogueForComparison(lastTranslatedDialogue)

            // 1. If this exact dialogue is already translated and showing, do nothing!
            if (cleanLastTranslated.isNotEmpty() && isSameDialogue(rawCombined, lastTranslatedDialogue)) {
                return
            }

            val now = System.currentTimeMillis()
            val cleanCandidate = cleanDialogueForComparison(candidateDialogueText)

            // 2. Typewriter Effect Detection: Is text still being typed out?
            if (cleanCandidate.isEmpty() || cleanCurrent != cleanCandidate) {
                // Text changed or is actively growing
                candidateDialogueText = rawCombined
                candidateLastChangeTime = now
                // Wait for typewriter animation to finish
                return
            }

            // 3. Text has not changed between ticks (cleanCurrent == cleanCandidate)
            val stableDuration = now - candidateLastChangeTime

            // Check for end-of-dialogue indicators (Pokemon triangle cursor 🔻/▼ or sentence punctuation)
            val hasTerminalMarker = rawCombined.contains(Regex("[🔻▼▶►>]")) ||
                rawCombined.endsWith(".") || rawCombined.endsWith("!") || rawCombined.endsWith("?") ||
                rawCombined.endsWith("\"") || rawCombined.endsWith("”") || rawCombined.endsWith("…")

            // Wait 500ms if terminal marker is present, or 800ms for plain text without marker to ensure 100% calm
            val requiredStability = if (hasTerminalMarker) 500L else 800L

            if (stableDuration < requiredStability) {
                // Still waiting for sentence to be fully typed out and calm
                return
            }

            // 4. Text is fully STABLE and COMPLETE!
            if (isSameDialogue(rawCombined, lastTranslatedDialogue)) {
                return
            }

            Log.d("GameOverlayService", "Auto-translate triggered for stable complete dialogue: $rawCombined")
            lastTranslatedDialogue = rawCombined
            isAutoTranslating = true

            withContext(Dispatchers.Main) {
                if (overlayCardView == null) {
                    showTranslationOverlayCard(autoTriggered = true)
                }
                executeTranslationProcess(bitmapToAnalyze, isAuto = true)
            }
        } catch (e: Exception) {
            Log.e("GameOverlayService", "Error in performAutoTranslateTick: ${e.message}")
            isAutoTranslating = false
        }
    }

    private fun isGenuineDialogue(text: String): Boolean {
        if (text.isBlank() || text.length < 3) return false

        var letterCount = 0
        var digitOrSymbolCount = 0
        for (ch in text) {
            when {
                ch.isLetter() -> letterCount++
                ch.isWhitespace() -> {}
                ch.isDigit() -> digitOrSymbolCount++
                else -> digitOrSymbolCount++
            }
        }

        // Must have at least 3 genuine letters
        if (letterCount < 3) return false

        // HUD / stats filter: if mostly digits and symbols (e.g. "120/120 HP Lv.5"), it's not dialogue
        if (letterCount < digitOrSymbolCount) return false

        val upper = text.trim().uppercase()
        val ignoredSingleWords = setOf("START", "SELECT", "OPTION", "OPTIONS", "PAUSE", "MENU", "EXIT", "RESUME", "AUTO", "SKIP")
        if (ignoredSingleWords.contains(upper)) return false

        return true
    }

    private fun cleanDialogueForComparison(text: String): String {
        return text
            .replace(Regex("[🔻▼▶►>_~.…\\-\\:\\;\\,\\.\\!\\?\\'\\\"\\(\\)\\[\\]\\{\\}]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
    }

    private fun isSameDialogue(text1: String, text2: String): Boolean {
        val c1 = cleanDialogueForComparison(text1)
        val c2 = cleanDialogueForComparison(text2)
        if (c1.isEmpty() || c2.isEmpty()) return false
        if (c1 == c2) return true

        // Fuzzy edit distance check (ignore small OCR misrecognitions)
        val dist = computeLevenshteinDistance(c1, c2)
        val maxLen = Math.max(c1.length, c2.length)
        val similarity = 1.0 - (dist.toDouble() / maxLen.toDouble())
        return similarity >= 0.85
    }

    private fun computeLevenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun updateAllAutoButtons(enabled: Boolean) {
        val autoText = if (enabled) " ⚡ ออโต้: ON " else " ⚡ ออโต้: OFF "
        val textColor = if (enabled) Color.parseColor("#10B981") else Color.parseColor("#94A3B8")
        val bgDrawable = GradientDrawable().apply {
            setColor(if (enabled) Color.parseColor("#1E3A2F") else Color.parseColor("#1E293B"))
            cornerRadius = 6 * resources.displayMetrics.density
            setStroke((1 * resources.displayMetrics.density).toInt(), if (enabled) Color.parseColor("#10B981") else Color.parseColor("#475569"))
        }

        cardAutoBtn?.let { btn ->
            btn.text = autoText
            btn.setTextColor(textColor)
            btn.background = bgDrawable
        }

        frameAutoBtn?.let { btn ->
            btn.text = autoText
            btn.setTextColor(textColor)
            btn.background = bgDrawable
        }
    }

    private fun dismissOverlayCard() {
        if (overlayCardView != null && isCardShowing) {
            try {
                windowManager?.removeView(overlayCardView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayCardView = null
            cardResultsContainer = null
            cardLoadingBar = null
            cardStatusText = null
            cardResultsScroll = null
            cardAutoBtn = null
            cardBadge = null
            isCardShowing = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        stopAutoTranslateLoop()
        dismissOverlayCard()
        hideAimingFrameView()
        if (bubbleView != null) {
            try {
                windowManager?.removeView(bubbleView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bubbleView = null
        }
        serviceScope.cancel()
    }
}
