package com.atvantiq.wfms.models.calendar

import com.atvantiq.wfms.models.attendance.attendanceDetails.Record

data class AttendanceDay(val date: String, val status: AttendanceStatus,var record:Record? = null) {
    override fun toString(): String {
        return "AttendanceDay(date='$date', status=$status, record=$record)"
    }
}