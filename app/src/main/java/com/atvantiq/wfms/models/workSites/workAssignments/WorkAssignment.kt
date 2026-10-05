package com.atvantiq.wfms.models.workSites.workAssignments

import com.atvantiq.wfms.models.workSites.workSites.Circle
import com.atvantiq.wfms.models.workSites.workSites.Project
import com.atvantiq.wfms.models.workSites.workSites.Status
import com.google.gson.annotations.SerializedName

/** A work assignment on the Work Approval list (`GET /work/all`): one employee's visit to one site. */
data class WorkAssignment(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("employee")
    val employee: AssignmentEmployee?,
    @SerializedName("project")
    val project: Project?,
    @SerializedName("site")
    val site: AssignmentSite?,
    @SerializedName("circle")
    val circle: Circle?,
    @SerializedName("status")
    val status: Status?,
    @SerializedName("assigned_by")
    val assignedBy: AssignmentAssigner?,
    @SerializedName("visit_no")
    val visitNo: Int?,
    @SerializedName("progress")
    val progress: AssignmentProgress?,
    @SerializedName("created_at")
    val createdAt: String?
)

data class AssignmentEmployee(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("employee_code")
    val code: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("check_in")
    val checkIn: String?
)

/** [workSiteId] is the id the progress and approve calls take, not [id]. */
data class AssignmentSite(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("site_id")
    val siteCode: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("work_site_id")
    val workSiteId: Long?
)

data class AssignmentAssigner(
    @SerializedName("id")
    val id: Long?,
    @SerializedName("name")
    val name: String?
)

data class AssignmentProgress(
    @SerializedName("total")
    val total: Int?,
    @SerializedName("completed")
    val completed: Int?
)

/**
 * The day the progress call is asked about: the employee's check-in day, or the day the work was
 * assigned when they haven't checked in. The API's own date, as yyyy-MM-dd, without a time zone shift.
 */
val WorkAssignment.workDate: String?
    get() = (employee?.checkIn ?: createdAt)?.substringBefore('T')?.takeIf { DATE_ONLY.matches(it) }

private val DATE_ONLY = Regex("""\d{4}-\d{2}-\d{2}""")
