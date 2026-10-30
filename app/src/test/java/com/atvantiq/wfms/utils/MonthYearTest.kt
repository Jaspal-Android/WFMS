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

    @Test
    fun `the first and last day are the API's yyyy-MM-dd dates`() {
        assertEquals("2026-09-01", MonthYear(9, 2026).firstDay)
        assertEquals("2026-09-30", MonthYear(9, 2026).lastDay)
        assertEquals("2026-12-31", MonthYear(12, 2026).lastDay)
    }

    @Test
    fun `February ends on the 28th, or the 29th in a leap year`() {
        assertEquals("2027-02-28", MonthYear(2, 2027).lastDay)
        assertEquals("2028-02-29", MonthYear(2, 2028).lastDay)
    }
}
