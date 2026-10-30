package com.atvantiq.wfms.ui.screens.dashboard.tabs.myDay

import com.atvantiq.wfms.data.tracking.QueuedLocationEvent
import com.atvantiq.wfms.data.tracking.TrackMath
import com.atvantiq.wfms.data.tracking.TrackPoint
import com.atvantiq.wfms.models.myDay.MyDayData
import com.atvantiq.wfms.models.myDay.MyDayPoint
import com.atvantiq.wfms.utils.DateUtils

enum class TimelineKind { CHECK_IN, WORK_START, WORK_COMPLETE, CHECK_OUT, CLAIM, ACTIVITY, TRIP }

/** One row of the My Day timeline: an event, or a trip between two places. */
data class TimelineEntry(
    val kind: TimelineKind,
    val atMillis: Long,
    val endMillis: Long? = null,
    val siteName: String? = null,
    val fromName: String? = null,
    val toName: String? = null,
    val distanceKm: Double? = null
)

data class GeoPoint(val latitude: Double, val longitude: Double)

data class MapPin(val kind: TimelineKind, val position: GeoPoint)

data class GeoBounds(val south: Double, val west: Double, val north: Double, val east: Double)

/** Everything the My Day tab shows for one day. */
data class MyDayUi(
    val distanceKm: Double,
    /** On-device estimate while unsynced points are waiting: shown with "≈". */
    val isDistanceEstimate: Boolean,
    val isShiftActive: Boolean,
    val checkInMillis: Long?,
    val checkOutMillis: Long?,
    val movingMinutes: Int?,
    val pointsRecorded: Int,
    val route: List<GeoPoint>,
    val pins: List<MapPin>,
    val checkInPosition: GeoPoint?,
    val timeline: List<TimelineEntry>,
    val isEmpty: Boolean
) {
    /** Everything the map should show, or null when there is nothing to place (map hidden). */
    val mapBounds: GeoBounds?
        get() = MyDayContent.paddedBounds(route + pins.map { it.position } + listOfNotNull(checkInPosition))
}

/** Builds [MyDayUi] from the server's day and, for today, the points still waiting to upload. */
object MyDayContent {

    /** Each side of the map gets this share of the content's span as margin. */
    private const val MAP_PADDING_RATIO = 0.4

    /** The map never zooms in tighter than this many degrees. */
    private const val MIN_MAP_SPAN_DEGREES = 0.01

    fun build(data: MyDayData?, queued: List<QueuedLocationEvent>, isToday: Boolean): MyDayUi {
        val serverPoints = data?.points.orEmpty().mapNotNull { it.toTrackPoint() }
        val serverTimes = serverPoints.map { it.recordedAtMillis }.toSet()
        val unsynced = if (isToday) {
            queued.map { TrackPoint(it.latitude, it.longitude, it.recordedAtMillis, it.accuracyMeters?.toDouble()) }
                .filterNot { it.recordedAtMillis in serverTimes }
        } else {
            emptyList()
        }
        val merged = (serverPoints + unsynced).sortedBy { it.recordedAtMillis }
        val summary = data?.summary
        val attendance = data?.attendance
        val checkInMillis = DateUtils.parseUtcIso(attendance?.checkInAt)
        val checkOutMillis = DateUtils.parseUtcIso(attendance?.checkOutAt)
        val timeline = timeline(data)

        return MyDayUi(
            distanceKm = if (unsynced.isNotEmpty()) TrackMath.distanceKm(merged) else summary?.distanceKm ?: 0.0,
            isDistanceEstimate = unsynced.isNotEmpty(),
            isShiftActive = checkInMillis != null && checkOutMillis == null,
            checkInMillis = checkInMillis,
            checkOutMillis = checkOutMillis,
            movingMinutes = summary?.movingMinutes,
            pointsRecorded = maxOf(summary?.pointsRecorded ?: 0, merged.size),
            route = merged
                .filter { (it.accuracyMeters ?: 0.0) <= TrackMath.MAX_ACCURACY_METERS }
                .map { GeoPoint(it.latitude, it.longitude) }
                .filter { it.isRecorded() },
            pins = data?.events.orEmpty().mapNotNull { event ->
                val lat = event.latitude ?: return@mapNotNull null
                val lon = event.longitude ?: return@mapNotNull null
                MapPin(kindOf(event.type), GeoPoint(lat, lon)).takeIf { it.position.isRecorded() }
            },
            checkInPosition = attendance?.let { a ->
                val lat = a.checkInLatitude ?: return@let null
                val lon = a.checkInLongitude ?: return@let null
                GeoPoint(lat, lon).takeIf { it.isRecorded() }
            },
            timeline = timeline,
            isEmpty = checkInMillis == null && merged.isEmpty() && timeline.isEmpty()
        )
    }

    /** Events and trips by time, oldest first; at the same instant events come first. */
    fun timeline(data: MyDayData?): List<TimelineEntry> {
        val events = data?.events.orEmpty().mapNotNull { event ->
            val at = DateUtils.parseUtcIso(event.at) ?: return@mapNotNull null
            TimelineEntry(kindOf(event.type), at, siteName = event.siteName)
        }
        val trips = data?.trips.orEmpty().mapNotNull { trip ->
            val start = DateUtils.parseUtcIso(trip.startAt) ?: return@mapNotNull null
            TimelineEntry(
                TimelineKind.TRIP, start,
                endMillis = DateUtils.parseUtcIso(trip.endAt),
                fromName = trip.fromName,
                toName = trip.toName,
                distanceKm = trip.distanceKm
            )
        }
        return (events + trips).sortedWith(
            compareBy<TimelineEntry> { it.atMillis }.thenBy { if (it.kind == TimelineKind.TRIP) 1 else 0 }
        )
    }

    /** An unknown type must not crash the screen: it is shown as a generic activity. */
    fun kindOf(type: String?): TimelineKind = when (type?.trim()?.uppercase()) {
        "CHECK_IN" -> TimelineKind.CHECK_IN
        "WORK_START" -> TimelineKind.WORK_START
        "WORK_COMPLETE" -> TimelineKind.WORK_COMPLETE
        "CHECK_OUT" -> TimelineKind.CHECK_OUT
        "CLAIM" -> TimelineKind.CLAIM
        else -> TimelineKind.ACTIVITY
    }

    /** Bounds around [points] with 40% padding and a minimum span, or null when empty. */
    fun paddedBounds(points: List<GeoPoint>): GeoBounds? {
        if (points.isEmpty()) return null
        val south = points.minOf { it.latitude }
        val north = points.maxOf { it.latitude }
        val west = points.minOf { it.longitude }
        val east = points.maxOf { it.longitude }
        val latPad = maxOf((north - south) * MAP_PADDING_RATIO, (MIN_MAP_SPAN_DEGREES - (north - south)) / 2)
        val lonPad = maxOf((east - west) * MAP_PADDING_RATIO, (MIN_MAP_SPAN_DEGREES - (east - west)) / 2)
        return GeoBounds(south - latPad, west - lonPad, north + latPad, east + lonPad)
    }

    private fun MyDayPoint.toTrackPoint(): TrackPoint? {
        val lat = latitude ?: return null
        val lon = longitude ?: return null
        val at = DateUtils.parseUtcIso(recordedAt) ?: return null
        return TrackPoint(lat, lon, at, accuracy)
    }

    /** (0, 0) means the position was not recorded. */
    private fun GeoPoint.isRecorded(): Boolean = !(latitude == 0.0 && longitude == 0.0)
}
