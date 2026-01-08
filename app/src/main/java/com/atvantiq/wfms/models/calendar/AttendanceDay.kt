package com.atvantiq.wfms.models.calendar

import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord

data class AttendanceDay(val date: String, val status: String,var record:AttendanceRecord? = null) {
    override fun toString(): String {
        return "AttendanceDay(date='$date', status=$status, record=$record)"
    }
}