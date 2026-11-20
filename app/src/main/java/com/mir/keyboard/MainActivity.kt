package com.mir.keyboard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0D1210"))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 60, 48, 60)
        }

        val tvBrand = TextView(this).apply {
            text = "M  I  R"
            textSize = 28f
            setTextColor(Color.parseColor("#00E676"))
            setPadding(0, 0, 0, 32)
        }

        val btnEnableIme = Button(this).apply {
            text = "1. Enable MIR Keyboard"
            setBackgroundColor(Color.parseColor("#00C853"))
            setTextColor(Color.BLACK)
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        }

        val btnSelectIme = Button(this).apply {
            text = "2. Select MIR Keyboard"
            setBackgroundColor(Color.parseColor("#1E2823"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
            }
        }

        val btnFloatingMenu = Button(this).apply {
            text = "3. Launch Floating Menu (M)"
            setBackgroundColor(Color.parseColor("#00C853"))
            setTextColor(Color.BLACK)
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } else {
                    startService(Intent(this@MainActivity, FloatingMenuService::class.java))
                    Toast.makeText(this@MainActivity, "MIR Floating Bubble Started!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val swBold = Switch(this).apply {
            text = "Bold Letters (Unicode)"
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(0, 32, 0, 24)
            isChecked = AppConfig.getBool(this@MainActivity, "bold_letters", false)
            setOnCheckedChangeListener { _, isChecked ->
                AppConfig.setBool(this@MainActivity, "bold_letters", isChecked)
            }
        }

        val etP1 = EditText(this).apply {
            hint = "Quick Phrase 1 (Default: Salam)"
            setText(AppConfig.getString(this@MainActivity, "phrase_1", "Salam"))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        val etP2 = EditText(this).apply {
            hint = "Quick Phrase 2 (Default: GG)"
            setText(AppConfig.getString(this@MainActivity, "phrase_2", "GG"))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        val etP3 = EditText(this).apply {
            hint = "Quick Phrase 3 (Default: MIR OP)"
            setText(AppConfig.getString(this@MainActivity, "phrase_3", "MIR OP"))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }

        val btnSavePhrases = Button(this).apply {
            text = "Save Custom Phrases"
            setBackgroundColor(Color.parseColor("#1E2823"))
            setTextColor(Color.parseColor("#00E676"))
            setOnClickListener {
                AppConfig.setString(this@MainActivity, "phrase_1", etP1.text.toString())
                AppConfig.setString(this@MainActivity, "phrase_2", etP2.text.toString())
                AppConfig.setString(this@MainActivity, "phrase_3", etP3.text.toString())
                Toast.makeText(this@MainActivity, "Phrases Saved!", Toast.LENGTH_SHORT).show()
            }
        }

        root.addView(tvBrand)
        root.addView(btnEnableIme)
        root.addView(btnSelectIme)
        root.addView(btnFloatingMenu)
        root.addView(swBold)
        root.addView(etP1)
        root.addView(etP2)
        root.addView(etP3)
        root.addView(btnSavePhrases)

        scroll.addView(root)
        setContentView(scroll)
    }
}
