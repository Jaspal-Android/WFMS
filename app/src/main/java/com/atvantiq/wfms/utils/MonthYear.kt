package com.atvantiq.wfms.utils

import java.util.Calendar

/** A calendar month, [month] 1-12, stepped by the ◀ ▶ month headers of the approval lists. */
data class MonthYear(val month: Int, val year: Int) {

    fun previous(): MonthYear = if (month == FIRST_MONTH) MonthYear(LAST_MONTH, year - 1) else MonthYear(month - 1, year)

    fun next(): MonthYear = if (month == LAST_MONTH) MonthYear(FIRST_MONTH, year + 1) else MonthYear(month + 1, year)

    /** "September 2026" */
    val label: String get() = DateUtils.formatMonthYear(month, year)

    companion object {
        private const val FIRST_MONTH = 1
        private const val LAST_MONTH = 12

        fun current(calendar: Calendar = Calendar.getInstance()): MonthYear =
            MonthYear(calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
    }
}
