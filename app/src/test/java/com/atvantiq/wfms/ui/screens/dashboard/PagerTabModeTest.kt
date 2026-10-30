package com.atvantiq.wfms.ui.screens.dashboard

import com.google.android.material.tabs.TabLayout
import org.junit.Assert.assertEquals
import org.junit.Test

class PagerTabModeTest {

    @Test
    fun `normal and slightly larger text keeps the four equal tabs`() {
        assertEquals(TabLayout.MODE_FIXED, PagerTabMode.forFontScale(0.85f))
        assertEquals(TabLayout.MODE_FIXED, PagerTabMode.forFontScale(1.0f))
        assertEquals(TabLayout.MODE_FIXED, PagerTabMode.forFontScale(1.15f))
        assertEquals(TabLayout.MODE_FIXED, PagerTabMode.forFontScale(PagerTabMode.MAX_FONT_SCALE_FOR_EQUAL_TABS))
    }

    @Test
    fun `larger text scrolls the tabs instead of breaking a label`() {
        assertEquals(TabLayout.MODE_SCROLLABLE, PagerTabMode.forFontScale(1.3f))
        assertEquals(TabLayout.MODE_SCROLLABLE, PagerTabMode.forFontScale(1.5f))
        assertEquals(TabLayout.MODE_SCROLLABLE, PagerTabMode.forFontScale(2.0f))
    }
}
