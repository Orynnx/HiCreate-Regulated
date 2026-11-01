package com.orynnx.hicreatehook.ui

import android.content.Context
import android.os.Build

enum class UiStyle {
    MIUIX,
    GLASMIO,
}

/** The single user-visible preference: which component system renders the status page. */
object AppearanceStore {

    private const val PREFS = "appearance"
    private const val KEY_STYLE = "ui_style"

    fun load(context: Context): UiStyle {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_STYLE, UiStyle.MIUIX.name)
        val style = runCatching { UiStyle.valueOf(value ?: UiStyle.MIUIX.name) }
            .getOrDefault(UiStyle.MIUIX)
        return if (style == UiStyle.GLASMIO && Build.VERSION.SDK_INT < 33) {
            UiStyle.MIUIX
        } else {
            style
        }
    }

    fun save(context: Context, style: UiStyle) {
        val safe = if (style == UiStyle.GLASMIO && Build.VERSION.SDK_INT < 33) {
            UiStyle.MIUIX
        } else {
            style
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STYLE, safe.name)
            .apply()
    }
}
