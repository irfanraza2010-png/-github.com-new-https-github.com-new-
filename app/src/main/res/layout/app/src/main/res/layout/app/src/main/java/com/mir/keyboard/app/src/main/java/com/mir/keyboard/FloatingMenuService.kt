package com.mir.keyboard

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class FloatingMenuService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatView: LinearLayout

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

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
            setPadding(0, 0, 0, 16)
        }

        val swAutoNums = Switch(this).apply {
            text = "Auto Nums"
            setTextColor(Color.WHITE)
            isChecked = AppConfig.getBool(this@FloatingMenuService, "auto_nums", false)
            setOnCheckedChangeListener { _, isChecked -> AppConfig.setBool(this@FloatingMenuService, "auto_nums", isChecked) }
        }

        val swAutoAlpha = Switch(this).apply {
            text = "Auto Alpha"
            setTextColor(Color.WHITE)
            isChecked = AppConfig.getBool(this@FloatingMenuService, "auto_alpha", true)
            setOnCheckedChangeListener { _, isChecked -> AppConfig.setBool(this@FloatingMenuService, "auto_alpha", isChecked) }
        }

        val swAutoWords = Switch(this).apply {
            text = "Auto Words"
            setTextColor(Color.WHITE)
            isChecked = AppConfig.getBool(this@FloatingMenuService, "auto_words", false)
            setOnCheckedChangeListener { _, isChecked -> AppConfig.setBool(this@FloatingMenuService, "auto_words", isChecked) }
        }

        val swIphoneDrop = Switch(this).apply {
            text = "iPhone drop (Burst)"
            setTextColor(Color.parseColor("#00E676"))
            isChecked = AppConfig.getBool(this@FloatingMenuService, "iphone_drop", false)
            setOnCheckedChangeListener { _, isChecked -> AppConfig.setBool(this@FloatingMenuService, "iphone_drop", isChecked) }
        }

        val savedDelay = AppConfig.getInt(this, "send_delay", 0)
        val tvDelayLabel = TextView(this).apply {
            text = "Send Delay: $savedDelay ms"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 16, 0, 4)
        }

        val sbDelay = SeekBar(this).apply {
            max = 500
            progress = savedDelay
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    tvDelayLabel.text = "Send Delay: $progress ms"
                    AppConfig.setInt(this@FloatingMenuService, "send_delay", progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        menuCard.addView(tvTitle)
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
            layoutParams = LinearLayout.LayoutParams(130, 130).apply {
                setMargins(16, 0, 0, 0)
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
            gravity = Gravity.CENTER_VERTICAL or Gravity.END
            x = 16
            y = 0
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
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatView.isInitialized) {
            try { windowManager.removeView(floatView) } catch (_: Exception) {}
        }
    }
}
