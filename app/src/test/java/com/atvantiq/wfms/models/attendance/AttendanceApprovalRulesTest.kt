package com.atvantiq.wfms.models.attendance

import com.atvantiq.wfms.constants.AppRole
import com.atvantiq.wfms.models.attendance.attendanceDetails.ADMIN
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceApprovalStatus
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord
import com.atvantiq.wfms.models.attendance.attendanceDetails.Logs
import com.atvantiq.wfms.models.attendance.attendanceDetails.OPS
import com.atvantiq.wfms.models.attendance.attendanceDetails.PM
import com.atvantiq.wfms.models.attendance.attendanceDetails.canBeMarkedBy
import com.atvantiq.wfms.models.attendance.attendanceDetails.initialDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Who can mark an attendance record (spec 6.1). */
class AttendanceApprovalRulesTest {

    private fun record(
        id: Long? = 1L,
        action: String? = "Submitted by employee",
        logs: Logs? = Logs(null, null, null),
        status: Int? = 0
    ) = AttendanceRecord(
        action = action, approvalStatus = null, canHrMarkAttendance = false, checkin = null, checkout = null,
        createdAt = null, employee = null, employeeRemarks = null, id = id, logs = logs, status = status, workHours = ""
    )

    @Test
    fun `a submitted record can be marked by every approver, never by others`() {
        assertTrue(record().canBeMarkedBy(AppRole.PM))
        assertTrue(record().canBeMarkedBy(AppRole.OPS))
        assertTrue(record().canBeMarkedBy(AppRole.ADMIN))
        assertFalse(record().canBeMarkedBy(AppRole.EMPLOYEE))
        assertFalse(record().canBeMarkedBy(AppRole.OTHER))
    }

    @Test
    fun `a record without an id can't be marked`() {
        assertFalse(record(id = null).canBeMarkedBy(AppRole.PM))
    }

    @Test
    fun `the role's own approved log closes it for that role only`() {
        val pmApproved = record(logs = Logs(aDMIN = null, oPS = null, pM = PM(isApproved = true, remarks = null, status = 1)))
        assertFalse(pmApproved.canBeMarkedBy(AppRole.PM))
        assertTrue(pmApproved.canBeMarkedBy(AppRole.OPS))

        val opsApproved = record(logs = Logs(aDMIN = null, oPS = OPS(true, null, 1), pM = null))
        assertFalse(opsApproved.canBeMarkedBy(AppRole.OPS))

        val adminApproved = record(logs = Logs(aDMIN = ADMIN(true, null, 1), oPS = null, pM = null))
        assertFalse(adminApproved.canBeMarkedBy(AppRole.ADMIN))
        assertTrue(adminApproved.canBeMarkedBy(AppRole.PM))
    }

    @Test
    fun `an action already "by {role}" closes it, ignoring case`() {
        assertFalse(record(action = "Approved by PM").canBeMarkedBy(AppRole.PM))
        assertFalse(record(action = "approved BY pm").canBeMarkedBy(AppRole.PM))
        assertTrue(record(action = "Approved by PM").canBeMarkedBy(AppRole.OPS))
        assertFalse(record(action = "Approved by ADMIN").canBeMarkedBy(AppRole.ADMIN))
    }

    @Test
    fun `a decision starts from the record's status, or Present when there is none`() {
        assertEquals(AttendanceApprovalStatus.IDLE, record(status = 4).initialDecision)
        assertEquals(AttendanceApprovalStatus.PRESENT, record(status = 0).initialDecision)
        assertEquals(AttendanceApprovalStatus.PRESENT, record(status = null).initialDecision)
    }

    @Test
    fun `the grid offers the eight statuses in order, codes 1 to 8`() {
        assertEquals((1..8).toList(), AttendanceApprovalStatus.decisions.map { it.code })
        assertEquals(AttendanceApprovalStatus.SUBMITTED, AttendanceApprovalStatus.from(99))
    }
}
