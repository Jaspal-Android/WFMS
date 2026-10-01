package com.atvantiq.wfms.ui.screens

import com.atvantiq.wfms.constants.AppRole
import com.atvantiq.wfms.models.empDetail.AccessLevel
import com.atvantiq.wfms.models.empDetail.Permission
import com.atvantiq.wfms.ui.screens.DashboardTab.APPROVALS
import com.atvantiq.wfms.ui.screens.DashboardTab.CLAIMS
import com.atvantiq.wfms.ui.screens.DashboardTab.DASHBOARD
import com.atvantiq.wfms.ui.screens.DashboardTab.MORE
import com.atvantiq.wfms.ui.screens.DashboardTab.SITES
import com.atvantiq.wfms.ui.screens.DashboardTab.WORK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardTabsTest {

    private fun permission(feature: String, vararg access: String) = Permission(
        accessLevels = access.mapIndexed { i, level -> AccessLevel(access = level, accessId = i.toLong()) },
        featureId = 1,
        featureName = feature
    )

    private val siteView = listOf(permission("Site", "View"))

    @Test
    fun `roles are trimmed and matched case-insensitively`() {
        assertEquals(AppRole.EMPLOYEE, AppRole.from(" Employee "))
        assertEquals(AppRole.PM, AppRole.from("PM"))
        assertEquals(AppRole.OPS, AppRole.from("ops"))
        assertEquals(AppRole.ADMIN, AppRole.from("Admin"))
        assertEquals(AppRole.OTHER, AppRole.from("Manager"))
        assertEquals(AppRole.OTHER, AppRole.from(null))
    }

    @Test
    fun `only PM, OPS and admin approve`() {
        assertTrue(AppRole.PM.canApprove)
        assertTrue(AppRole.OPS.canApprove)
        assertTrue(AppRole.ADMIN.canApprove)
        assertFalse(AppRole.EMPLOYEE.canApprove)
        assertFalse(AppRole.OTHER.canApprove)
    }

    @Test
    fun `employees get Work and Claims, whatever their permissions`() {
        assertEquals(listOf(DASHBOARD, WORK, CLAIMS, MORE), DashboardTabs.forRole(AppRole.EMPLOYEE, siteView))
    }

    @Test
    fun `PM and admin with the Site permission get Sites and Approvals`() {
        assertEquals(listOf(DASHBOARD, SITES, APPROVALS, MORE), DashboardTabs.forRole(AppRole.PM, siteView))
        assertEquals(listOf(DASHBOARD, SITES, APPROVALS, MORE), DashboardTabs.forRole(AppRole.ADMIN, siteView))
    }

    @Test
    fun `without the Site permission the Sites tab is hidden, not shown and then failing`() {
        assertEquals(listOf(DASHBOARD, APPROVALS, MORE), DashboardTabs.forRole(AppRole.OPS, emptyList()))
        assertEquals(listOf(DASHBOARD, APPROVALS, MORE), DashboardTabs.forRole(AppRole.PM, null))
    }

    @Test
    fun `any other role gets Sites only, with no approvals`() {
        assertEquals(listOf(DASHBOARD, SITES, MORE), DashboardTabs.forRole(AppRole.OTHER, siteView))
        assertEquals(listOf(DASHBOARD, MORE), DashboardTabs.forRole(AppRole.OTHER, emptyList()))
    }

    @Test
    fun `Full Access implies View, and Create alone does not open Sites`() {
        assertTrue(SITES in DashboardTabs.forRole(AppRole.PM, listOf(permission(" site ", " full access "))))
        assertFalse(SITES in DashboardTabs.forRole(AppRole.PM, listOf(permission("Site", "Create"))))
        assertFalse(SITES in DashboardTabs.forRole(AppRole.PM, listOf(permission("Client", "View"))))
    }

    @Test
    fun `Enter Work Details opens Work, else Approvals, else Sites`() {
        assertEquals(WORK, DashboardTabs.workEntryTab(listOf(DASHBOARD, WORK, CLAIMS, MORE)))
        assertEquals(APPROVALS, DashboardTabs.workEntryTab(listOf(DASHBOARD, SITES, APPROVALS, MORE)))
        assertEquals(SITES, DashboardTabs.workEntryTab(listOf(DASHBOARD, SITES, MORE)))
        assertNull(DashboardTabs.workEntryTab(listOf(DASHBOARD, MORE)))
    }
}
