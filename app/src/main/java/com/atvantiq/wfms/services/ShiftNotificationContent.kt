package com.atvantiq.wfms.services

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.atvantiq.wfms.R
import com.atvantiq.wfms.data.tracking.ShiftState

/**
 * What the shift notification says: active (since, locations, km) or paused (resumes at), and
 * which action it offers. Times are passed in already formatted so this stays free of Android.
 */
data class ShiftNotificationContent(
    val isPaused: Boolean,
    @StringRes val title: Int,
    @DrawableRes val smallIcon: Int,
    @StringRes val actionTitle: Int,
    @DrawableRes val actionIcon: Int,
    /** Start of the elapsed-time chronometer: the check-in, or null when unknown. */
    val chronometerBaseMillis: Long?,
    val locations: Int,
    val distanceKm: Double,
    val pausedUntilMillis: Long?
) {
    companion object {
        fun of(state: ShiftState, nowMillis: Long): ShiftNotificationContent {
            val paused = state.isPaused(nowMillis)
            return ShiftNotificationContent(
                isPaused = paused,
                title = if (paused) R.string.shift_notification_paused_title else R.string.shift_notification_active_title,
                smallIcon = if (paused) R.drawable.ic_tracking_pause else R.drawable.ic_tracking_navigation,
                actionTitle = if (paused) R.string.resume else R.string.pause_15_min,
                actionIcon = if (paused) R.drawable.ic_tracking_play else R.drawable.ic_tracking_pause,
                chronometerBaseMillis = state.checkInMillis,
                locations = state.tally.locations,
                distanceKm = state.tally.distanceKm,
                pausedUntilMillis = state.pausedUntilMillis.takeIf { paused }
            )
        }
    }
}
