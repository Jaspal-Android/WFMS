package com.atvantiq.wfms.ui.screens.dashboard

import com.google.android.material.tabs.TabLayout

/**
 * How the Dashboard's four top tabs (Attendance, My Day, Targets, Projects) are laid out.
 * Four equal tabs only have room for the labels while the text is close to its normal size; with a
 * larger system font "Attendance" would break in the middle of the word. So the tabs stay equal
 * and fixed at normal sizes, and scroll, each as wide as its label, when the font is larger.
 */
object PagerTabMode {

    /** Above this font scale the longest label no longer fits in a quarter of the width. */
    const val MAX_FONT_SCALE_FOR_EQUAL_TABS = 1.25f

    fun forFontScale(fontScale: Float): Int =
        if (fontScale > MAX_FONT_SCALE_FOR_EQUAL_TABS) TabLayout.MODE_SCROLLABLE else TabLayout.MODE_FIXED
}
