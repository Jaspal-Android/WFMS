package com.atvantiq.wfms.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Keeps this (root) view clear of the system bars and of the keyboard. Since targetSdk 35 every
 * screen is drawn edge to edge and `adjustResize` no longer makes room for the keyboard, so the
 * bottom padding is the taller of the navigation bar and the keyboard: the screen's scroll view
 * shrinks above the keyboard and the focused field scrolls into view.
 */
fun View.applySystemBarsAndImePadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
        insets
    }
}
