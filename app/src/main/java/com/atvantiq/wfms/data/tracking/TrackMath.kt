package com.atvantiq.wfms.data.tracking

import com.google.gson.annotations.SerializedName
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** One recorded location, independent of where it came from (server trail or offline queue). */
data class TrackPoint(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("recordedAtMillis") val recordedAtMillis: Long,
    @SerializedName("accuracyMeters") val accuracyMeters: Double?
)

/**
 * Distance and moving time from a location trail, with the same filters as the server and iOS
 * (parity handoff, section 6), so the km a user sees on the device matches the server's.
 */
object TrackMath {

    /** Fixes less accurate than this are kept and uploaded but never counted or drawn. */
    const val MAX_ACCURACY_METERS = 100.0

    /** Faster than this from the previous kept point means a GPS jump. */
    private const val MAX_SPEED_KMH = 150.0

    /** Between two points faster than this, the employee was moving. */
    private const val MOVING_SPEED_KMH = 3.0

    private const val EARTH_RADIUS_KM = 6371.0
    private const val MILLIS_PER_HOUR = 3_600_000.0
    private const val MILLIS_PER_MINUTE = 60_000L

    /** Points that count: sorted by time, accurate enough, with coordinates, no GPS jumps. */
    fun countable(points: List<TrackPoint>): List<TrackPoint> {
        val kept = mutableListOf<TrackPoint>()
        points.sortedBy { it.recordedAtMillis }
            .filter { isCountable(it) }
            .forEach { point ->
                val previous = kept.lastOrNull()
                if (previous == null || !isJump(previous, point)) kept += point
            }
        return kept
    }

    /** Accurate enough and with real coordinates. */
    fun isCountable(point: TrackPoint): Boolean =
        point.hasCoordinates() && (point.accuracyMeters ?: 0.0) <= MAX_ACCURACY_METERS

    /** Faster than a vehicle could go from [previous]: a GPS jump. */
    fun isJump(previous: TrackPoint, point: TrackPoint): Boolean = speedKmh(previous, point) > MAX_SPEED_KMH

    /** Haversine distance over the countable points, rounded to one decimal km. */
    fun distanceKm(points: List<TrackPoint>): Double {
        val total = countable(points).zipWithNext { a, b -> haversineKm(a, b) }.sum()
        return (total * 10).roundToInt() / 10.0
    }

    /** Minutes spent between consecutive countable points while moving faster than 3 km/h. */
    fun movingMinutes(points: List<TrackPoint>): Int {
        val movingMillis = countable(points).zipWithNext()
            .filter { (a, b) -> speedKmh(a, b) > MOVING_SPEED_KMH }
            .sumOf { (a, b) -> b.recordedAtMillis - a.recordedAtMillis }
        return (movingMillis / MILLIS_PER_MINUTE).toInt()
    }

    fun haversineKm(a: TrackPoint, b: TrackPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(h))
    }

    private fun speedKmh(a: TrackPoint, b: TrackPoint): Double {
        val hours = (b.recordedAtMillis - a.recordedAtMillis) / MILLIS_PER_HOUR
        val km = haversineKm(a, b)
        return if (hours <= 0.0) (if (km == 0.0) 0.0 else Double.MAX_VALUE) else km / hours
    }

    /** (0, 0) is how a missing position arrives from the server. */
    private fun TrackPoint.hasCoordinates(): Boolean = !(latitude == 0.0 && longitude == 0.0)
}
