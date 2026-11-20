package com.mir.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout

class MirIMEService : InputMethodService() {

    private var isNumMode = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.layout_keyboard, null)

        val qwertyContainer = root.findViewById<LinearLayout>(R.id.qwertyContainer)
        val numpadContainer = root.findViewById<LinearLayout>(R.id.numpadContainer)
        val btnToggleMode = root.findViewById<Button>(R.id.btnToggleMode)
        val btnSpace = root.findViewById<Button>(R.id.btnSpace)
        val btnSwitchIme = root.findViewById<Button>(R.id.btnSwitchIme)
        val btnSend = root.findViewById<Button>(R.id.btnSend)

        val btnP1 = root.findViewById<Button>(R.id.btnPhrase1)
        val btnP2 = root.findViewById<Button>(R.id.btnPhrase2)
        val btnP3 = root.findViewById<Button>(R.id.btnPhrase3)

        btnP1.text = AppConfig.getString(this, "phrase_1", "Salam")
        btnP2.text = AppConfig.getString(this, "phrase_2", "GG")
        btnP3.text = AppConfig.getString(this, "phrase_3", "MIR OP")

        btnP1.setOnClickListener { sendPhrase(btnP1.text.toString()) }
        btnP2.setOnClickListener { sendPhrase(btnP2.text.toString()) }
        btnP3.setOnClickListener { sendPhrase(btnP3.text.toString()) }

        addKeys(root.findViewById(R.id.row1), listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"))
        addKeys(root.findViewById(R.id.row2), listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"))
        addKeys(root.findViewById(R.id.row3), listOf("z", "x", "c", "v", "b", "n", "m", "⌫"))

        addKeys(root.findViewById(R.id.numRow1), listOf("1", "2", "3", "4"))
        addKeys(root.findViewById(R.id.numRow2), listOf("5", "6", "7", "8"))
        addKeys(root.findViewById(R.id.numRow3), listOf("9", "0", ".", "⌫"))

        btnToggleMode.setOnClickListener {
            isNumMode = !isNumMode
            qwertyContainer.visibility = if (isNumMode) View.GONE else View.VISIBLE
            numpadContainer.visibility = if (isNumMode) View.VISIBLE else View.GONE
            btnToggleMode.text = if (isNumMode) "ABC" else "123"
        }

        btnSpace.setOnClickListener { processKey(" ") }
        btnSend.setOnClickListener { fireSend() }
        btnSwitchIme.setOnClickListener {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }

        return root
    }

    private fun addKeys(row: LinearLayout, keys: List<String>) {
        for (key in keys) {
            val btn = Button(this).apply {
                text = key
                textSize = 15f
                isAllCaps = false
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(if (key == "⌫") 0xFFB71C1C.toInt() else 0xFF222C27.toInt())
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    setMargins(3, 2, 3, 2)
                }
                setOnClickListener {
                    if (key == "⌫") {
                        currentInputConnection?.deleteSurroundingText(1, 0)
                    } else {
                        processKey(key)
                    }
                }
            }
            row.addView(btn)
        }
    }

    private fun processKey(rawKey: String) {
        val ic = currentInputConnection ?: return
        val useBold = AppConfig.getBool(this, "bold_letters", false)
        val outText = if (useBold) toBold(rawKey) else rawKey

        val iphoneDrop = AppConfig.getBool(this, "iphone_drop", false)
        val delay = AppConfig.getInt(this, "send_delay", 0).toLong()

        if (iphoneDrop && rawKey != " ") {
            repeat(3) { i ->
                handler.postDelayed({
                    ic.commitText(outText, 1)
                    fireSend()
                }, i * 35L)
            }
            return
        }

        ic.commitText(outText, 1)

        val autoAlpha = AppConfig.getBool(this, "auto_alpha", true)
        val autoNums = AppConfig.getBool(this, "auto_nums", false)
        val autoWords = AppConfig.getBool(this, "auto_words", false)

        val isAlpha = rawKey.length == 1 && rawKey[0].isLetter()
        val isNum = rawKey.length == 1 && rawKey[0].isDigit()
        val isSpace = rawKey == " "

        if ((autoAlpha && isAlpha) || (autoNums && isNum) || (autoWords && isSpace)) {
            if (delay > 0) {
                handler.postDelayed({ fireSend() }, delay)
            } else {
                fireSend()
            }
        }
    }

    private fun sendPhrase(phrase: String) {
        val ic = currentInputConnection ?: return
        val out = if (AppConfig.getBool(this, "bold_letters", false)) toBold(phrase) else phrase
        ic.commitText(out, 1)
        fireSend()
    }

    private fun fireSend() {
        val ic = currentInputConnection ?: return
        val actionId = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_UNSPECIFIED
        if (actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(actionId)
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun toBold(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            when (ch) {
                in 'a'..'z' -> sb.append(Character.toChars(0x1D5EE + (ch - 'a')))
                in 'A'..'Z' -> sb.append(Character.toChars(0x1D5D4 + (ch - 'A')))
                in '0'..'9' -> sb.append(Character.toChars(0x1D7EC + (ch - '0')))
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
