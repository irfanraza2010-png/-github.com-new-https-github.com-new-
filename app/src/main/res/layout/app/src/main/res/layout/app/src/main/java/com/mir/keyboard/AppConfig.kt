package com.mir.keyboard

import android.content.Context

object AppConfig {
    private const val PREF_NAME = "mir_keyboard_prefs"

    fun getBool(context: Context, key: String, def: Boolean = false): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getBoolean(key, def)
    }

    fun setBool(context: Context, key: String, value: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply()
    }

    fun getInt(context: Context, key: String, def: Int = 0): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getInt(key, def)
    }

    fun setInt(context: Context, key: String, value: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putInt(key, value).apply()
    }

    fun getString(context: Context, key: String, def: String = ""): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).getString(key, def) ?: def
    }

    fun setString(context: Context, key: String, value: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString(key, value).apply()
    }
}
