package com.atvantiq.wfms.models.cab

data class CabRide(
    val id: String,
    val date: String,
    val circle: String,
    val site: String,
    val empName: String,
    val cabType: String,
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val startTime: String,
    val endTime: String
)
