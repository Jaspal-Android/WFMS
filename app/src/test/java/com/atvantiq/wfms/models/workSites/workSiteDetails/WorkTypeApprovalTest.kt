package com.atvantiq.wfms.models.workSites.workSiteDetails

import com.atvantiq.wfms.constants.StatusCodes
import com.atvantiq.wfms.models.workSites.workSites.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkTypeApprovalTest {

    private fun workType(
        id: Long,
        statusCode: Int?,
        pm: Int? = 0,
        ops: Int? = 0,
        admin: Int? = 0
    ) = WorkType(
        activities = null,
        admin = Admin(null, null, admin),
        employeeRemarks = null,
        id = id,
        name = "type-$id",
        ops = Ops(null, null, ops),
        pm = Pm(null, null, pm),
        status = Status(statusCode, null),
        workEndedAt = null,
        workStartedAt = null
    )

    @Test
    fun `a pm can act on an in-progress type nobody has actioned`() {
        assertTrue(
            workType(1, StatusCodes.WIP).isApprovableBy("PM")
        )
    }

    @Test
    fun `a pm cannot act on a type it already actioned`() {
        assertFalse(
            workType(1, StatusCodes.WIP, pm = 1).isApprovableBy("pm")
        )
    }

    @Test
    fun `a type that is neither in progress nor completed is never selectable`() {
        assertFalse(
            workType(1, StatusCodes.ACCEPTED).isApprovableBy("admin")
        )
        assertFalse(workType(1, null).isApprovableBy("admin"))
    }

    @Test
    fun `an ops user needs the ops and admin slots to be open`() {
        assertTrue(workType(1, StatusCodes.COMPLETED).isApprovableBy("ops"))
        assertFalse(
            workType(1, StatusCodes.COMPLETED, admin = 1).isApprovableBy("ops")
        )
    }

    @Test
    fun `an unknown role can never approve`() {
        assertFalse(
            workType(1, StatusCodes.WIP).isApprovableBy("employee")
        )
    }

    @Test
    fun `select all only picks the rows the admin could tick individually`() {
        val selectable = workType(1, StatusCodes.WIP)
        val alreadyActioned = workType(2, StatusCodes.WIP, pm = 1)
        val notStarted = workType(3, StatusCodes.ACCEPTED)

        val picked = listOf(selectable, alreadyActioned, notStarted).approvableBy("pm")

        assertEquals(listOf(selectable), picked)
    }

    @Test
    fun `select all picks nothing when no row is actionable`() {
        val picked = listOf(workType(1, StatusCodes.WIP, pm = 1), workType(2, StatusCodes.OPEN))
            .approvableBy("pm")

        assertTrue(picked.isEmpty())
    }
}
