package com.mir.keyboard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent

class MirTapService : AccessibilityService() {

    private var cachedX = -1f
    private var cachedY = -1f

    private val tapReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val x = intent?.getFloatExtra("x", -1f) ?: -1f
            val y = intent?.getFloatExtra("y", -1f) ?: -1f
            if (x > 0f && y > 0f) {
                cachedX = x
                cachedY = y
            } else if (cachedX <= 0f) {
                val prefs = getSharedPreferences("mir_keyboard_prefs", Context.MODE_PRIVATE)
                val dm = resources.displayMetrics
                cachedX = prefs.getFloat("target_x", dm.widthPixels * 0.93f)
                cachedY = prefs.getFloat("target_y", dm.heightPixels * 0.69f)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && cachedX > 0f) {
                val path = Path().apply { moveTo(cachedX, cachedY) }
                val stroke = GestureDescription.StrokeDescription(path, 0L, 18L)
                val gesture = GestureDescription.Builder().addStroke(stroke).build()
                dispatchGesture(gesture, null, null)
            }
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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(tapReceiver) } catch (_: Exception) {}
    }
}
