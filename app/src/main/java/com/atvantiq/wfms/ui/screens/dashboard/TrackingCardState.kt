package com.atvantiq.wfms.ui.screens.dashboard

import com.atvantiq.wfms.data.tracking.ShiftState

/** The dashboard tracking card's states (parity handoff, section 2), most urgent first. */
enum class TrackingCardState {
    /** Location services are off: Open Settings. */
    GPS_OFF,
    /** Location permission denied: Open Settings. */
    PERMISSION_DENIED,
    /** Only "Approximate" location (Android 12+): Fix (ask Android to switch to precise). */
    NEEDS_PRECISE,
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
            location: LocationAccess,
            shift: ShiftState,
            nowMillis: Long
        ): TrackingCardState? = when {
            !isDayActive -> null
            !location.servicesOn -> GPS_OFF
            !location.foreground -> PERMISSION_DENIED
            !location.precise -> NEEDS_PRECISE
            !isTrackingStarted -> STARTING
            shift.isPaused(nowMillis) -> PAUSED
            !location.background -> NEEDS_ALWAYS
            else -> ACTIVE
        }
    }
}

/**
 * Whether the app can get a location: Location Services on, any access while in use
 * ([foreground], approximate counts), the precise one, and "Allow all the time".
 */
data class LocationAccess(
    val servicesOn: Boolean = true,
    val foreground: Boolean = true,
    val precise: Boolean = true,
    val background: Boolean = true
)
