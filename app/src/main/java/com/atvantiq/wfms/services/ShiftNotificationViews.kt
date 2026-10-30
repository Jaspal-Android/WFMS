package com.atvantiq.wfms.services

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.atvantiq.wfms.R
import java.util.Locale

/**
 * The shift notification drawn as the iOS Live Activity card: glyph, status, a live elapsed timer,
 * "Since … · N locations" (or "Resumes at …") and km. The system draws these views, so colours come
 * in resolved: [accent] for the km figure, [warning] for the paused glyph.
 */
class ShiftNotificationViews(
    private val context: Context,
    @ColorInt private val accent: Int,
    @ColorInt private val warning: Int
) {

    /** One row under the system header: timer, status, km. */
    fun collapsed(content: ShiftNotificationContent, nowMillis: Long): RemoteViews =
        base(R.layout.notification_shift_collapsed, content, nowMillis).apply {
            setTextViewText(R.id.shiftStatus, context.getString(statusRes(content)))
        }

    /** The full card, shown when the notification is expanded and on the lock screen. */
    fun expanded(content: ShiftNotificationContent, nowMillis: Long, detail: String): RemoteViews =
        base(R.layout.notification_shift_expanded, content, nowMillis).apply {
            setTextViewText(R.id.shiftTitle, context.getString(content.title))
            setTextViewText(R.id.shiftDetail, detail)
            setImageViewResource(R.id.shiftGlyph, content.smallIcon)
            setInt(R.id.shiftGlyph, "setColorFilter", if (content.isPaused) warning else textColor())
        }

    private fun base(@LayoutRes layout: Int, content: ShiftNotificationContent, nowMillis: Long): RemoteViews =
        RemoteViews(context.packageName, layout).apply {
            setTextViewText(R.id.shiftDistance, String.format(Locale.getDefault(), DISTANCE_FORMAT, content.distanceKm))
            setTextColor(R.id.shiftDistance, accent)
            val checkIn = content.chronometerBaseMillis
            if (checkIn == null) {
                setViewVisibility(R.id.shiftElapsed, View.GONE)
            } else {
                // The shift clock keeps running through a pause, as on iOS; it is only dimmed.
                setViewVisibility(R.id.shiftElapsed, View.VISIBLE)
                setChronometer(R.id.shiftElapsed, elapsedBase(checkIn, nowMillis, SystemClock.elapsedRealtime()), null, true)
                setTextColor(R.id.shiftElapsed, if (content.isPaused) secondaryTextColor() else textColor())
            }
        }

    private fun statusRes(content: ShiftNotificationContent): Int =
        if (content.isPaused) R.string.shift_card_status_paused else R.string.shift_card_status_active

    private fun textColor(): Int = ContextCompat.getColor(context, R.color.shift_card_text)

    private fun secondaryTextColor(): Int = ContextCompat.getColor(context, R.color.shift_card_text_secondary)

    companion object {
        private const val DISTANCE_FORMAT = "%.1f"

        /**
         * A [android.widget.Chronometer] counts from an `elapsedRealtime` base, not a wall-clock
         * time: the base that makes it show the time since [checkInMillis].
         */
        fun elapsedBase(checkInMillis: Long, nowMillis: Long, elapsedRealtimeMillis: Long): Long =
            elapsedRealtimeMillis - (nowMillis - checkInMillis).coerceAtLeast(0L)
    }
}
