private fun fireSend() {
        val ic = currentInputConnection ?: return

        // 1. Unity / Yalla Ludo ke liye Action Done aur Send dono fire karein
        val actionId = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        if (actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(actionId)
        }
        ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
        ic.performEditorAction(EditorInfo.IME_ACTION_SEND)

        // 2. KeyEvent DOWN aur UP ke darmiyan 15ms gap (Unity Engine ke liye zaroori)
        val downTime = SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0))

        handler.postDelayed({
            val upTime = SystemClock.uptimeMillis()
            currentInputConnection?.sendKeyEvent(KeyEvent(downTime, upTime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0))
            // Backup: Kuch Unity builds mein DPAD_CENTER ya newline se send hota hai
            currentInputConnection?.sendKeyEvent(KeyEvent(upTime, upTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0))
            currentInputConnection?.sendKeyEvent(KeyEvent(upTime, upTime + 10, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER, 0))
        }, 15L)
    }
