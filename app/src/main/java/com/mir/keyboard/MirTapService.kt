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

    private val tapReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val prefs = getSharedPreferences("mir_keyboard_prefs", Context.MODE_PRIVATE)
            val x = prefs.getFloat("target_x", -1f)
            val y = prefs.getFloat("target_y", -1f)
            if (x > 0f && y > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                performTap(x, y)
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

    private fun performTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 25L)
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
