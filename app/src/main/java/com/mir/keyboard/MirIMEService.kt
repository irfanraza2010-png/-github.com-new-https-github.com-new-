package com.mir.keyboard

import android.graphics.Color
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
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#101513"))
            setPadding(8, 8, 8, 12)
        }

        val phraseRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 100)
        }

        val p1Text = AppConfig.getString(this, "phrase_1", "Salam")
        val p2Text = AppConfig.getString(this, "phrase_2", "GG")
        val p3Text = AppConfig.getString(this, "phrase_3", "MIR OP")

        listOf(p1Text, p2Text, p3Text).forEach { phrase ->
            val btn = Button(this).apply {
                text = phrase
                textSize = 12f
                isAllCaps = false
                setTextColor(Color.parseColor("#00E676"))
                setBackgroundColor(Color.parseColor("#1E2823"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    setMargins(4, 2, 4, 6)
                }
                setOnClickListener { sendPhrase(phrase) }
            }
            phraseRow.addView(btn)
        }
        root.addView(phraseRow)

        val qwertyContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val row1 = makeRow()
        val row2 = makeRow().apply { setPadding(24, 0, 24, 0) }
        val row3 = makeRow()

        addKeys(row1, listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"))
        addKeys(row2, listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"))
        addKeys(row3, listOf("z", "x", "c", "v", "b", "n", "m", "⌫"))

        qwertyContainer.addView(row1)
        qwertyContainer.addView(row2)
        qwertyContainer.addView(row3)

        val numpadContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        val nRow1 = makeRow()
        val nRow2 = makeRow()
        val nRow3 = makeRow()

        addKeys(nRow1, listOf("1", "2", "3", "4"))
        addKeys(nRow2, listOf("5", "6", "7", "8"))
        addKeys(nRow3, listOf("9", "0", ".", "⌫"))

        numpadContainer.addView(nRow1)
        numpadContainer.addView(nRow2)
        numpadContainer.addView(nRow3)

        root.addView(qwertyContainer)
        root.addView(numpadContainer)

        val bottomRow = makeRow()

        val btnToggle = Button(this).apply {
            text = "123"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#26322C"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.1f).apply {
                setMargins(4, 4, 4, 4)
            }
            setOnClickListener {
                isNumMode = !isNumMode
                qwertyContainer.visibility = if (isNumMode) View.GONE else View.VISIBLE
                numpadContainer.visibility = if (isNumMode) View.VISIBLE else View.GONE
                text = if (isNumMode) "ABC" else "123"
            }
        }

        val btnSpace = Button(this).apply {
            text = "M I R"
            setTextColor(Color.parseColor("#00E676"))
            setBackgroundColor(Color.parseColor("#1E2823"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 2.8f).apply {
                setMargins(4, 4, 4, 4)
            }
            setOnClickListener { processKey(" ") }
        }

        val btnSwitch = Button(this).apply {
            text = "🌐"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#26322C"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.9f).apply {
                setMargins(4, 4, 4, 4)
            }
            setOnClickListener {
                try { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() } catch (_: Exception) {}
            }
        }

        val btnSend = Button(this).apply {
            text = "➤"
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#00C853"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.2f).apply {
                setMargins(4, 4, 4, 4)
            }
            setOnClickListener { fireSend() }
        }

        bottomRow.addView(btnToggle)
        bottomRow.addView(btnSpace)
        bottomRow.addView(btnSwitch)
        bottomRow.addView(btnSend)
        root.addView(bottomRow)

        return root
    }

    private fun makeRow(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 125)
        }
    }

    private fun addKeys(row: LinearLayout, keys: List<String>) {
        for (key in keys) {
            val btn = Button(this).apply {
                text = key
                textSize = 15f
                isAllCaps = false
                setTextColor(Color.WHITE)
                setBackgroundColor(if (key == "⌫") Color.parseColor("#B71C1C") else Color.parseColor("#222C27"))
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    setMargins(3, 3, 3, 3)
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
