package com.atvantiq.wfms.ui.screens.attendance.applyLeave

enum class ApplyLeaveErrorHandler {
    START_DATE_EMPTY,
    END_DATE_EMPTY,
    LEAVE_TYPE_EMPTY,
    LEAVE_REASON_EMPTY,
    START_DATE_AFTER_END_DATE,
    END_DATE_BEFORE_START_DATE
}