package com.atvantiq.wfms.ui.screens.dashboard.tabs.myDay

import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.atvantiq.wfms.R

/** Title, icon and tint of each kind of timeline entry (and its map pin). */
data class TimelineStyle(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    /** Theme attribute for the tint, or 0 when [tintColor] is used. */
    @AttrRes val tintAttr: Int = 0,
    @ColorRes val tintColor: Int = 0
) {
    companion object {
        fun of(kind: TimelineKind): TimelineStyle = when (kind) {
            TimelineKind.CHECK_IN -> TimelineStyle(R.string.timeline_started_day, R.drawable.ic_day_started, tintAttr = R.attr.wfmsColorPrimary)
            TimelineKind.WORK_START -> TimelineStyle(R.string.timeline_started_work, R.drawable.ic_work_started, tintColor = R.color.status_present_text)
            TimelineKind.WORK_COMPLETE -> TimelineStyle(R.string.timeline_completed_work, R.drawable.ic_work_completed, tintColor = R.color.status_present_text)
            TimelineKind.CHECK_OUT -> TimelineStyle(R.string.timeline_ended_day, R.drawable.ic_day_ended, tintAttr = R.attr.wfmsColorPrimary)
            TimelineKind.CLAIM -> TimelineStyle(R.string.timeline_submitted_claim, R.drawable.ic_tab_claims, tintAttr = R.attr.wfmsColorOnSurfaceVariant)
            TimelineKind.ACTIVITY -> TimelineStyle(R.string.activity, R.drawable.ic_activity_dot, tintAttr = R.attr.wfmsColorOnSurfaceVariant)
            TimelineKind.TRIP -> TimelineStyle(R.string.timeline_travel, R.drawable.ic_trip, tintAttr = R.attr.wfmsColorWarning)
        }
    }
}
