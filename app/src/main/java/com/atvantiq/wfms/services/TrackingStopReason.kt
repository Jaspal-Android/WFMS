package com.atvantiq.wfms.services

import androidx.annotation.StringRes
import com.atvantiq.wfms.R

/** Why the tracking service ended itself, and what the user is told about it. */
enum class TrackingStopReason(@StringRes val textRes: Int) {
    PERMISSION_LOST(R.string.tracking_stopped_permission),
    SESSION_EXPIRED(R.string.tracking_stopped_session),
    ERROR(R.string.tracking_stopped_error)
}
