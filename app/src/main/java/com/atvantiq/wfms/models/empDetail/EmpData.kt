package com.atvantiq.wfms.models.empDetail


import com.atvantiq.wfms.models.circle.CircleData
import com.atvantiq.wfms.models.loginResponse.OfficialLocation
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

data class EmpData(
    @SerializedName("circle")
    @JsonAdapter(CircleListDeserializer::class)
    val circle: List<CircleData>? = null,
    @SerializedName("date_of_joining")
    val dateOfJoining: String,
    @SerializedName("designation")
    val designation: String,
    @SerializedName("dob")
    val dob: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("employee_code")
    val employeeCode: String,
    @SerializedName("employee_id")
    val employeeId: Long,
    @SerializedName("gender")
    val gender: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("official_location")
    val officialLocation: OfficialLocation?,
    @SerializedName("permissions")
    val permissions: List<Permission>,
    @SerializedName("reporting_manager")
    val reportingManager: ReportingManager?,
    @SerializedName("role")
    val role: String,
    @SerializedName("short_name")
    val shortName: String,
    @SerializedName("team")
    val team: Any?
) {
    /**
     * Circle codes joined for display, e.g. "CHD" or "CHD, PB".
     * Computed (no backing field) so Gson never serialises it into the cached payload.
     */
    val circleDisplay: String
        get() = circle.orEmpty()
            .map { it.code }
            .filter { it.isNotBlank() }
            .joinToString(", ")
}