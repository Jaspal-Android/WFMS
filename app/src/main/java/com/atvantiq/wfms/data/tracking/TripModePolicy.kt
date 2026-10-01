package com.atvantiq.wfms.data.tracking

/** How often and how precisely the service asks for location (parity handoff, section 6). */
enum class SamplingMode {
    /** Start of shift, or still for a while: balanced power, every 15 minutes. */
    REST,

    /** Moving: high accuracy, every ~10 s, at least 50 m apart. */
    TRIP
}

/**
 * The iOS `TripModePolicy`: sample sparsely while the employee is at rest and densely while they
 * travel, so the trail follows the road and the km match iOS and the server.
 *
 * - REST → TRIP once a fix is [TRIP_START_METERS] or more from where the employee last rested.
 * - TRIP → REST after [STILL_MILLIS] without moving more than [MOVE_METERS]. In trip mode a still
 *   phone sends no fixes (50 m filter), so the service also calls [onStillTimeout].
 *
 * Only fixes that count (accurate, with coordinates) move the policy.
 */
class TripModePolicy {

    var mode: SamplingMode = SamplingMode.REST
        private set

    /** Where the employee last rested; trip mode starts this far away. */
    private var restAnchor: TrackPoint? = null

    /** The last point that was a real move during a trip. */
    private var lastMove: TrackPoint? = null

    /** When the trip last moved, for the still timer; null outside trip mode. */
    val lastMoveMillis: Long? get() = lastMove?.recordedAtMillis.takeIf { mode == SamplingMode.TRIP }

    /** A new fix. Returns the mode to sample in from now on. */
    fun onFix(point: TrackPoint): SamplingMode {
        if (!TrackMath.isCountable(point)) return mode
        when (mode) {
            SamplingMode.REST -> {
                val anchor = restAnchor ?: point.also { restAnchor = it }
                if (metersBetween(anchor, point) >= TRIP_START_METERS) {
                    mode = SamplingMode.TRIP
                    lastMove = point
                }
            }
            SamplingMode.TRIP -> {
                val last = lastMove ?: point.also { lastMove = it }
                when {
                    metersBetween(last, point) > MOVE_METERS -> lastMove = point
                    point.recordedAtMillis - last.recordedAtMillis >= STILL_MILLIS -> rest(at = point)
                }
            }
        }
        return mode
    }

    /** No fix while in trip mode: back to rest once [STILL_MILLIS] have passed since the last move. */
    fun onStillTimeout(nowMillis: Long): SamplingMode {
        val last = lastMove
        if (mode == SamplingMode.TRIP && last != null && nowMillis - last.recordedAtMillis >= STILL_MILLIS) {
            rest(at = last)
        }
        return mode
    }

    private fun rest(at: TrackPoint) {
        mode = SamplingMode.REST
        restAnchor = at
        lastMove = null
    }

    private fun metersBetween(a: TrackPoint, b: TrackPoint): Double = TrackMath.haversineKm(a, b) * METERS_PER_KM

    companion object {
        const val TRIP_START_METERS = 200.0
        const val MOVE_METERS = 30.0
        const val STILL_MILLIS = 10 * 60 * 1000L

        /** In trip mode uploads are batched: this many points, or this long since the last upload. */
        const val TRIP_UPLOAD_BATCH = 10
        const val TRIP_UPLOAD_MAX_DELAY_MILLIS = 60 * 1000L

        private const val METERS_PER_KM = 1000.0

        /**
         * Whether to upload the queue now. At rest every fix goes straight up; while travelling
         * (a fix every few seconds) they are sent in small batches to spare battery and data.
         */
        fun shouldUpload(mode: SamplingMode, queued: Int, lastUploadMillis: Long, nowMillis: Long): Boolean =
            mode == SamplingMode.REST ||
                queued >= TRIP_UPLOAD_BATCH ||
                nowMillis - lastUploadMillis >= TRIP_UPLOAD_MAX_DELAY_MILLIS
    }
}
