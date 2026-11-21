package com.mir.keyboard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class MirTapService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    private val tapReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val prefs = getSharedPreferences("mir_keyboard_prefs", Context.MODE_PRIVATE)
            val dm = resources.displayMetrics
            val x = prefs.getFloat("target_x", dm.widthPixels * 0.93f)
            val y = prefs.getFloat("target_y", dm.heightPixels * 0.69f)

            // 55ms wait taake targetView pass-through ho jaye aur Yalla Ludo ka send button active ho jaye
            handler.postDelayed({
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    performTap(x, y)
                }
            }, 55L)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val filter = IntentFilter("com.mir.keyboard.ACTION_TAP_SEND")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(tapReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(tapReceiver, filter)
        }
    }

    private fun performTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        // Unity game engine ke liye 55ms tap duration zaroori hai
        val stroke = GestureDescription.StrokeDescription(path, 0L, 55L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(tapReceiver) } catch (_: Exception) {}
    }
}
