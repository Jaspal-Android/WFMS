package com.atvantiq.wfms.models.targets

import com.google.gson.annotations.SerializedName

data class MyTargetsResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: MyTargetsData?
)

data class MyTargetsData(
    @SerializedName("employee") val employee: TargetEmployee?,
    @SerializedName("month") val month: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("revenue") val revenue: TargetRevenue?,
    @SerializedName("sites") val sites: TargetSites?,
    @SerializedName("active_days") val activeDays: TargetActiveDays?,
    @SerializedName("hours_worked") val hoursWorked: TargetHoursWorked?,
    @SerializedName("efficiency") val efficiency: String?
)

data class TargetEmployee(
    @SerializedName("id") val id: Long?,
    @SerializedName("name") val name: String?,
    @SerializedName("designation") val designation: String?,
    @SerializedName("reporting_manager") val reportingManager: String?
)

data class TargetRevenue(
    @SerializedName("target") val target: Double?,
    @SerializedName("achieved") val achieved: Double?,
    @SerializedName("variation") val variation: Double?,
    @SerializedName("achievement_percentage") val achievementPercentage: String?
)

data class TargetSites(
    @SerializedName("target") val target: Int?,
    @SerializedName("achieved") val achieved: Int?,
    @SerializedName("variation") val variation: Int?,
    @SerializedName("achievement_percentage") val achievementPercentage: String?
)

data class TargetActiveDays(
    @SerializedName("target") val target: Int?,
    @SerializedName("achieved") val achieved: Int?,
    @SerializedName("achievement_percentage") val achievementPercentage: String?
)

data class TargetHoursWorked(
    @SerializedName("target") val target: Double?,
    @SerializedName("achieved") val achieved: Double?,
    @SerializedName("achievement_percentage") val achievementPercentage: String?
)
