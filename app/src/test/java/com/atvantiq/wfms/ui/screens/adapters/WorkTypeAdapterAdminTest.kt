package com.atvantiq.wfms.ui.screens.adapters

import com.atvantiq.wfms.constants.StatusCodes
import com.atvantiq.wfms.models.workSites.workSiteDetails.Admin
import com.atvantiq.wfms.models.workSites.workSiteDetails.Ops
import com.atvantiq.wfms.models.workSites.workSiteDetails.Pm
import com.atvantiq.wfms.models.workSites.workSiteDetails.WorkType
import com.atvantiq.wfms.models.workSites.workSites.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkTypeAdapterAdminTest {

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
            WorkTypeAdapterAdmin.isSelectable("PM", workType(1, StatusCodes.WIP))
        )
    }

    @Test
    fun `a pm cannot act on a type it already actioned`() {
        assertFalse(
            WorkTypeAdapterAdmin.isSelectable("pm", workType(1, StatusCodes.WIP, pm = 1))
        )
    }

    @Test
    fun `a type that is neither in progress nor completed is never selectable`() {
        assertFalse(
            WorkTypeAdapterAdmin.isSelectable("admin", workType(1, StatusCodes.ACCEPTED))
        )
        assertFalse(WorkTypeAdapterAdmin.isSelectable("admin", workType(1, null)))
    }

    @Test
    fun `an ops user needs the ops and admin slots to be open`() {
        assertTrue(WorkTypeAdapterAdmin.isSelectable("ops", workType(1, StatusCodes.COMPLETED)))
        assertFalse(
            WorkTypeAdapterAdmin.isSelectable("ops", workType(1, StatusCodes.COMPLETED, admin = 1))
        )
    }

    @Test
    fun `an unknown role can never approve`() {
        assertFalse(
            WorkTypeAdapterAdmin.isSelectable("employee", workType(1, StatusCodes.WIP))
        )
    }

    @Test
    fun `select all only picks the rows the admin could tick individually`() {
        val selectable = workType(1, StatusCodes.WIP)
        val alreadyActioned = workType(2, StatusCodes.WIP, pm = 1)
        val notStarted = workType(3, StatusCodes.ACCEPTED)

        val picked = WorkTypeAdapterAdmin.selectableTypes(
            "pm", listOf(selectable, alreadyActioned, notStarted)
        )

        assertEquals(listOf(selectable), picked)
    }

    @Test
    fun `select all picks nothing when no row is actionable`() {
        val picked = WorkTypeAdapterAdmin.selectableTypes(
            "pm", listOf(workType(1, StatusCodes.WIP, pm = 1), workType(2, StatusCodes.OPEN))
        )

        assertTrue(picked.isEmpty())
    }
}
