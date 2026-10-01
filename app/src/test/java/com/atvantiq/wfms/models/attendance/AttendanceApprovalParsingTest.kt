package com.atvantiq.wfms.models.attendance

import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.constants.ApprovalTextStatus
import com.atvantiq.wfms.models.attendance.attendanceDetails.day
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Real Dev responses of GET /attendance/details (PM test account, September 2026), trimmed. */
class AttendanceApprovalParsingTest {

    private val gson = GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create()

    private fun parse(statusJson: String) = gson.fromJson(
        """{ "code": 200, "message": "Attendance records fetched successfully.", "success": true,
          "data": { "page": 1, "page_size": 25, "total_records": 3, "total_pages": 1, "total_count": 6,
            "records": [ {
              "id": 345633198668,
              "employee": { "id": 248957380444, "name": "Jaspal", "code": "JAXX029",
                            "reporting_manager": { "id": 903375758915, "name": "Pm" } },
              "checkin":  { "time": "2026-09-23T04:05:17.540282Z", "latitude": 30.7150626, "logitude": 76.7032882 },
              "checkout": { "time": null, "latitude": 0.0, "logitude": 0.0 },
              "status": $statusJson,
              "work_hours": "", "created_at": "2026-09-23T04:05:17.540282Z",
              "employee_remarks": null, "can_hr_mark_attendance": false,
              "action": "Submitted by employee", "approval_status": null, "logs": {},
              "site": null, "project": { "id": 163264443387, "name": "shivam testing project" }, "circle": null
            } ] } }""",
        AttendanceDetailListResponse::class.java
    )

    @Test
    fun `status is read whether it arrives as an object or a number`() {
        assertEquals(0, parse("""{ "code": 0, "label": "SUBMITTED" }""").data?.records?.single()?.status)
        assertEquals(4, parse("4").data?.records?.single()?.status)
        assertNull(parse("null").data?.records?.single()?.status)
    }

    @Test
    fun `the month count is total_records, not the all-months total_count`() {
        assertEquals(3, parse("0").data?.totalRecords)
    }

    @Test
    fun `site, project and circle, the misspelt logitude and the record's day`() {
        val record = parse("0").data!!.records!!.single()
        assertEquals("shivam testing project", record.project?.name)
        assertNull(record.site)
        assertEquals(76.7032882, record.checkin?.logitude!!, 0.0)
        assertEquals("2026-09-23", record.day)
    }

    @Test
    fun `work chip follows the action text`() {
        assertEquals(ApprovalTextStatus.APPROVED, ApprovalTextStatus.from("Approved by PM"))
        assertEquals(ApprovalTextStatus.REJECTED, ApprovalTextStatus.from("Rejected by OPS"))
        assertEquals(ApprovalTextStatus.SUBMITTED, ApprovalTextStatus.from("Submitted by employee"))
        assertEquals(ApprovalTextStatus.PENDING, ApprovalTextStatus.from("PENDING"))
        assertEquals(ApprovalTextStatus.OTHER, ApprovalTextStatus.from("Marked by HR"))
        assertEquals(ApprovalTextStatus.OTHER, ApprovalTextStatus.from(null))
    }
}
