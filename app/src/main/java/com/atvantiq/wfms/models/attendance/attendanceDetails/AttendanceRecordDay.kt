package com.atvantiq.wfms.models.attendance.attendanceDetails

private const val YMD_LENGTH = 10

/** The record's day as yyyy-MM-dd: the start of `created_at`, falling back to the check-in time. */
val AttendanceRecord.day: String?
    get() = (createdAt ?: checkin?.time)?.takeIf { it.length >= YMD_LENGTH }?.take(YMD_LENGTH)
