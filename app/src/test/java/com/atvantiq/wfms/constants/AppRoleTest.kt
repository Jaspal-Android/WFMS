package com.atvantiq.wfms.constants

import org.junit.Assert.assertEquals
import org.junit.Test

class AppRoleTest {

    @Test
    fun `PM is matched ignoring case, spaces, underscores and hyphens`() {
        listOf("PM", "pm", " Pm ", "P M", "p_m", "P-M").forEach { assertEquals(it, AppRole.PM, AppRole.from(it)) }
    }

    @Test
    fun `other roles are not PM`() {
        assertEquals(AppRole.EMPLOYEE, AppRole.from("Employee"))
        assertEquals(AppRole.OPS, AppRole.from("ops"))
        assertEquals(AppRole.ADMIN, AppRole.from("admin"))
        listOf("Super Admin", "HR", "DH", "Finance", "PMO", null, "").forEach {
            assertEquals(it.toString(), AppRole.OTHER, AppRole.from(it))
        }
    }
}
