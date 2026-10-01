package com.atvantiq.wfms.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class MonthYearTest {

    @Test
    fun `steps across the year boundary both ways`() {
        assertEquals(MonthYear(12, 2025), MonthYear(1, 2026).previous())
        assertEquals(MonthYear(1, 2027), MonthYear(12, 2026).next())
        assertEquals(MonthYear(8, 2026), MonthYear(9, 2026).previous())
    }

    @Test
    fun `current month is 1-based`() {
        val calendar = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 15) }
        assertEquals(MonthYear(9, 2026), MonthYear.current(calendar))
    }
}
