package com.atvantiq.wfms.models.location

data class CustomLocationRequest(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)