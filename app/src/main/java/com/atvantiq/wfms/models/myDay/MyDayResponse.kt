package com.atvantiq.wfms.models.myDay

import com.google.gson.annotations.SerializedName

// GET /geo-tracking/me/day. Keys are camelCase on the wire, so every field is named explicitly
// (the app's Gson policy is snake_case). Everything is nullable: Gson skips Kotlin defaults and a
// missing field must never crash the screen.

data class MyDayResponse(
    @SerializedName("code") val code: Int?,
    @SerializedName("message") val message: String?,
    @SerializedName("success") val success: Boolean?,
    @SerializedName("data") val data: MyDayData?
)

data class MyDayData(
    @SerializedName("date") val date: String?,
    @SerializedName("attendance") val attendance: MyDayAttendance?,
    @SerializedName("points") val points: List<MyDayPoint>?,
    @SerializedName("events") val events: List<MyDayEvent>?,
    @SerializedName("trips") val trips: List<MyDayTrip>?,
    @SerializedName("summary") val summary: MyDaySummary?
)

data class MyDayAttendance(
    @SerializedName("checkInAt") val checkInAt: String?,
    @SerializedName("checkOutAt") val checkOutAt: String?,
    @SerializedName("checkInLatitude") val checkInLatitude: Double?,
    @SerializedName("checkInLongitude") val checkInLongitude: Double?,
    @SerializedName("checkOutLatitude") val checkOutLatitude: Double?,
    @SerializedName("checkOutLongitude") val checkOutLongitude: Double?
)

data class MyDayPoint(
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("recordedAt") val recordedAt: String?,
    @SerializedName("accuracy") val accuracy: Double?
)

/** [type]: CHECK_IN, WORK_START, WORK_COMPLETE, CHECK_OUT, CLAIM; anything else is shown as Activity. */
data class MyDayEvent(
    @SerializedName("type") val type: String?,
    @SerializedName("at") val at: String?,
    @SerializedName("siteId") val siteId: String?,
    @SerializedName("siteName") val siteName: String?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?
)

data class MyDayTrip(
    @SerializedName("fromName") val fromName: String?,
    @SerializedName("toName") val toName: String?,
    @SerializedName("startAt") val startAt: String?,
    @SerializedName("endAt") val endAt: String?,
    @SerializedName("distanceKm") val distanceKm: Double?
)

data class MyDaySummary(
    @SerializedName("distanceKm") val distanceKm: Double?,
    @SerializedName("movingMinutes") val movingMinutes: Int?,
    @SerializedName("sitesVisited") val sitesVisited: Int?,
    @SerializedName("pointsRecorded") val pointsRecorded: Int?
)
