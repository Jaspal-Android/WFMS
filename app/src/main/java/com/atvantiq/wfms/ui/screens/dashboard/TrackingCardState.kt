package com.atvantiq.wfms.ui.screens.dashboard

import com.atvantiq.wfms.data.tracking.ShiftState

/** The dashboard tracking card's states (parity handoff, section 2), most urgent first. */
enum class TrackingCardState {
    /** Location services are off: Open Settings. */
    GPS_OFF,
    /** Location permission denied: Open Settings. */
    PERMISSION_DENIED,
    /** The service is starting up: no button. */
    STARTING,
    /** The employee paused: Resume. */
    PAUSED,
    /** Only foreground location: Fix ("Allow all the time"). */
    NEEDS_ALWAYS,
    /** Tracking: Pause 15 min. */
    ACTIVE;

    companion object {
        /**
         * The card's state, or null when it is hidden (no active day). Facts come from the
         * system (permissions, GPS) and from the shared [ShiftState].
         */
        fun resolve(
            isDayActive: Boolean,
            isTrackingStarted: Boolean,
            hasForegroundLocation: Boolean,
            hasBackgroundLocation: Boolean,
            isGpsOn: Boolean,
            shift: ShiftState,
            nowMillis: Long
        ): TrackingCardState? = when {
            !isDayActive -> null
            !isGpsOn -> GPS_OFF
            !hasForegroundLocation -> PERMISSION_DENIED
            !isTrackingStarted -> STARTING
            shift.isPaused(nowMillis) -> PAUSED
            !hasBackgroundLocation -> NEEDS_ALWAYS
            else -> ACTIVE
        }
    }
}
