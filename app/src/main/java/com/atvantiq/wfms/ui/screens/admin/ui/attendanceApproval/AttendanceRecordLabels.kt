package com.atvantiq.wfms.ui.screens.admin.ui.attendanceApproval

import android.content.Context
import com.atvantiq.wfms.R
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord
import com.atvantiq.wfms.models.attendance.attendanceDetails.day
import com.atvantiq.wfms.utils.DateUtils

/** "9:35 AM – 4:43 PM", with "Not available" for a missing time. */
fun AttendanceRecord.checkInOutLabel(context: Context): String {
    val missing = context.getString(R.string.not_available_value)
    return context.getString(
        R.string.time_range_format,
        DateUtils.formatIsoShortTime(checkin?.time) ?: missing,
        DateUtils.formatIsoShortTime(checkout?.time) ?: missing
    )
}

/** The record's day as "23 Sep 2026". */
val AttendanceRecord.dayLabel: String?
    get() = DateUtils.formatYmdLabel(day)
