package com.atvantiq.wfms.utils

import java.util.Calendar
import java.util.Locale

/** A calendar month, [month] 1-12, stepped by the ◀ ▶ month headers of the approval lists. */
data class MonthYear(val month: Int, val year: Int) {

    fun previous(): MonthYear = if (month == FIRST_MONTH) MonthYear(LAST_MONTH, year - 1) else MonthYear(month - 1, year)

    fun next(): MonthYear = if (month == LAST_MONTH) MonthYear(FIRST_MONTH, year + 1) else MonthYear(month + 1, year)

    /** "September 2026" */
    val label: String get() = DateUtils.formatMonthYear(month, year)

    /** The first day as yyyy-MM-dd, the API's date format. */
    val firstDay: String get() = formatDay(FIRST_DAY)

    /** The last day as yyyy-MM-dd. */
    val lastDay: String
        get() = formatDay(
            Calendar.getInstance().apply { set(year, month - 1, FIRST_DAY) }.getActualMaximum(Calendar.DAY_OF_MONTH)
        )

    private fun formatDay(day: Int): String = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)

    companion object {
        private const val FIRST_DAY = 1
        private const val FIRST_MONTH = 1
        private const val LAST_MONTH = 12

        fun current(calendar: Calendar = Calendar.getInstance()): MonthYear =
            MonthYear(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
    }
}
