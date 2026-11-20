package com.mir.keyboard

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
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
    private lateinit var floatView: View

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatView = LayoutInflater.from(this).inflate(R.layout.layout_floating_menu, null)

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

        val menuCard = floatView.findViewById<LinearLayout>(R.id.menuCard)
        val btnBubble = floatView.findViewById<Button>(R.id.btnBubble)
        val swAutoNums = floatView.findViewById<Switch>(R.id.swAutoNums)
        val swAutoAlpha = floatView.findViewById<Switch>(R.id.swAutoAlpha)
        val swAutoWords = floatView.findViewById<Switch>(R.id.swAutoWords)
        val swIphoneDrop = floatView.findViewById<Switch>(R.id.swIphoneDrop)
        val tvDelayLabel = floatView.findViewById<TextView>(R.id.tvDelayLabel)
        val sbDelay = floatView.findViewById<SeekBar>(R.id.sbDelay)

        swAutoNums.isChecked = AppConfig.getBool(this, "auto_nums", false)
        swAutoAlpha.isChecked = AppConfig.getBool(this, "auto_alpha", true)
        swAutoWords.isChecked = AppConfig.getBool(this, "auto_words", false)
        swIphoneDrop.isChecked = AppConfig.getBool(this, "iphone_drop", false)
        val savedDelay = AppConfig.getInt(this, "send_delay", 0)
        sbDelay.progress = savedDelay
        tvDelayLabel.text = "Send Delay: $savedDelay ms"

        swAutoNums.setOnCheckedChangeListener { _, isChecked -> AppConfig.setBool(this, "auto_nums", isChecked) }
        swAutoAlpha.setOnCheckedChangeListener { _, isChecked -> AppConfig.setBool(this, "auto_alpha", isChecked) }
        swAutoWords.setOnCheckedChangeListener { _, isChecked -> AppConfig.setBool(this, "auto_words", isChecked) }
        swIphoneDrop.setOnCheckedChangeListener { _, isChecked -> AppConfig.setBool(this, "iphone_drop", isChecked) }

        sbDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvDelayLabel.text = "Send Delay: $progress ms"
                AppConfig.setInt(this@FloatingMenuService, "send_delay", progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

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
        if (::floatView.isInitialized) windowManager.removeView(floatView)
    }
}
