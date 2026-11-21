package com.mir.keyboard

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class FloatingMenuService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatView: LinearLayout
    private lateinit var targetView: TextView
    private lateinit var tParams: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())

    private val passThroughReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (::targetView.isInitialized && ::tParams.isInitialized) {
                tParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                try { windowManager.updateViewLayout(targetView, tParams) } catch (_: Exception) {}

                handler.postDelayed({
                    tParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    try { windowManager.updateViewLayout(targetView, tParams) } catch (_: Exception) {}
                }, 120L)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val prefs = getSharedPreferences("mir_keyboard_prefs", Context.MODE_PRIVATE)
        val dm = resources.displayMetrics

        val filter = IntentFilter("com.mir.keyboard.ACTION_TAP_SEND")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(passThroughReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(passThroughReceiver, filter)
        }

        floatView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val menuCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F2141A17"))
            setPadding(32, 24, 32, 24)
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(540, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val tvTitle = TextView(this).apply {
            text = "M  I  R"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#00E676"))
            setPadding(0, 0, 0, 12)
        }

        val btnEnableTap = Button(this).apply {
            text = "Enable Auto-Tap Service"
            textSize = 12f
            setBackgroundColor(Color.parseColor("#00C853"))
            setTextColor(Color.BLACK)
            setOnClickListener {
                try {
                    val i = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(i)
                } catch (e: Exception) {
                    Toast.makeText(this@FloatingMenuService, "Settings > Accessibility > MIR Auto-Send Tapper", Toast.LENGTH_LONG).show()
                }
            }
        }

        val swAutoNums = Switch(this).apply {
            text = "Auto Nums"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("auto_nums", false)
            setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("auto_nums", isChecked).apply() }
        }

        val swAutoAlpha = Switch(this).apply {
            text = "Auto Alpha"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("auto_alpha", true)
            setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("auto_alpha", isChecked).apply() }
        }

        val swAutoWords = Switch(this).apply {
            text = "Auto Words"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("auto_words", false)
            setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("auto_words", isChecked).apply() }
        }

        val swIphoneDrop = Switch(this).apply {
            text = "iPhone drop (Burst)"
            setTextColor(Color.parseColor("#00E676"))
            isChecked = prefs.getBoolean("iphone_drop", false)
            setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("iphone_drop", isChecked).apply() }
        }

        val savedDelay = prefs.getInt("send_delay", 0)
        val tvDelayLabel = TextView(this).apply {
            text = "Send Delay: $savedDelay ms"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 12, 0, 4)
        }

        val sbDelay = SeekBar(this).apply {
            max = 500
            progress = savedDelay
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    tvDelayLabel.text = "Send Delay: $progress ms"
                    prefs.edit().putInt("send_delay", progress).apply()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        menuCard.addView(tvTitle)
        menuCard.addView(btnEnableTap)
        menuCard.addView(swAutoNums)
        menuCard.addView(swAutoAlpha)
        menuCard.addView(swAutoWords)
        menuCard.addView(swIphoneDrop)
        menuCard.addView(tvDelayLabel)
        menuCard.addView(sbDelay)

        val btnBubble = Button(this).apply {
            text = "M"
            textSize = 18f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#00C853"))
            layoutParams = LinearLayout.LayoutParams(120, 120).apply {
                setMargins(12, 0, 0, 0)
            }
        }

        floatView.addView(menuCard)
        floatView.addView(btnBubble)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 250
        }

        windowManager.addView(floatView, params)

        var initX = 0
        var initY = 0
        var initTouchX = 0f
        var initTouchY = 0f

        btnBubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x
                    initY = params.y
                    initTouchX = event.rawX
                    initTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initX - (event.rawX - initTouchX).toInt()
                    params.y = initY + (event.rawY - initTouchY).toInt()
                    windowManager.updateViewLayout(floatView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (Math.abs(event.rawX - initTouchX) < 10 && Math.abs(event.rawY - initTouchY) < 10) {
                        menuCard.visibility = if (menuCard.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                    }
                    true
                }
                else -> false
            }
        }

        val defaultX = (dm.widthPixels * 0.90f).toInt()
        val defaultY = (dm.heightPixels * 0.67f).toInt()
        prefs.edit().putFloat("target_x", defaultX + 45f).putFloat("target_y", defaultY + 45f).apply()

        targetView = TextView(this).apply {
            text = "🎯"
            textSize = 20f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#8800C853"))
        }

        tParams = WindowManager.LayoutParams(
            90, 90,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = defaultX
            y = defaultY
        }

        windowManager.addView(targetView, tParams)

        var tInitX = 0
        var tInitY = 0
        var tTouchX = 0f
        var tTouchY = 0f

        targetView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    tInitX = tParams.x
                    tInitY = tParams.y
                    tTouchX = event.rawX
                    tTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    tParams.x = tInitX + (event.rawX - tTouchX).toInt()
                    tParams.y = tInitY + (event.rawY - tTouchY).toInt()
                    windowManager.updateViewLayout(targetView, tParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val loc = IntArray(2)
                    targetView.getLocationOnScreen(loc)
                    val centerX = loc[0] + (targetView.width / 2f)
                    val centerY = loc[1] + (targetView.height / 2f)
                    prefs.edit().putFloat("target_x", centerX).putFloat("target_y", centerY).apply()
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(passThroughReceiver) } catch (_: Exception) {}
        if (::floatView.isInitialized) {
            try { windowManager.removeView(floatView) } catch (_: Exception) {}
        }
        if (::targetView.isInitialized) {
            try { windowManager.removeView(targetView) } catch (_: Exception) {}
        }
    }
}
