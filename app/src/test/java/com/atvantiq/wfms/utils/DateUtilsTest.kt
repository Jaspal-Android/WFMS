package com.atvantiq.wfms.utils

import org.junit.Assert.*
import org.junit.Test

class DateUtilsTest {

    @Test
    fun testGetCurrentDateFormat() {
        val date = DateUtils.getCurrentDate()
        // Should match dd-MM-yyyy
        assertTrue(date.matches(Regex("\\d{2}-\\d{2}-\\d{4}")))
    }

    @Test
    fun testGetCurrentTimeFormat() {
        val time = DateUtils.getCurrentTime()
        // Accepts hh:mm AM/PM (always uppercase, exactly two letters)
        assertTrue(time.matches(Regex("\\d{2}:\\d{2} [AP]M")))
    }

    @Test
    fun testFormatDateValid() {
        val input = "2024-06-01 12:34:56.789"
        val expected = "01/06/2024"
        assertEquals(expected, DateUtils.formatDate(input))
    }

    @Test
    fun testFormatDateInvalid() {
        assertEquals("", DateUtils.formatDate(""))
        assertEquals("", DateUtils.formatDate("invalid"))
    }

    @Test
    fun testGetCurrentMonthAndYear() {
        val (month, year) = DateUtils.getCurrentMonthAndYear()
        assertTrue(month in 1..12)
        assertTrue(year > 2000)
    }

    @Test
    fun testConvertFrom24Valid() {
        assertEquals("01:05 AM", DateUtils.convertFrom24("01:05:00"))
        assertEquals("12:00 PM", DateUtils.convertFrom24("12:00:00"))
        assertEquals("03:30 PM", DateUtils.convertFrom24("15:30:00"))
    }

    @Test
    fun testConvertFrom24Invalid() {
        assertEquals("", DateUtils.convertFrom24("invalid"))
        assertEquals("", DateUtils.convertFrom24(""))
    }

    @Test
    fun testFormatApiDateToYMDValid() {
        val input = "2024-06-01T12:34:56.789Z"
        val expected = "2024-06-01"
        assertEquals(expected, DateUtils.formatApiDateToYMD(input))
    }

    @Test
    fun testFormatApiDateToYMDInvalid() {
        assertNull(DateUtils.formatApiDateToYMD("invalid"))
        assertNull(DateUtils.formatApiDateToYMD(null))
    }

    @Test
    fun testFormatApiDateToTimeAndDateValid() {
        val input = "2024-06-01T08:00:00.000Z"
        val result = DateUtils.formatApiDateToTimeAndDate(input)
        // Should match "hh:mm a  01-06-2024"
        assertNotNull(result)
        assertTrue(result!!.endsWith("01-06-2024"))
        assertTrue(result.matches(Regex("\\d{2}:\\d{2} [apAP][mM]  01-06-2024")))
    }

    @Test
    fun testFormatApiDateToTimeAndDateInvalid() {
        assertNull(DateUtils.formatApiDateToTimeAndDate("invalid"))
        assertNull(DateUtils.formatApiDateToTimeAndDate(null))
    }
}
