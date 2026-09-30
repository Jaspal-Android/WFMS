package com.atvantiq.wfms.ui.screens

import com.atvantiq.wfms.models.loginResponse.User
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class SplashRoutingTest {

    private fun user(role: String?): User = mockk(relaxed = true) { every { this@mockk.role } returns role }

    @Test
    fun `no saved token opens login`() {
        assertEquals(SplashTarget.LOGIN, SplashRouting.targetFor(null, user("Employee")))
        assertEquals(SplashTarget.LOGIN, SplashRouting.targetFor("", user("Employee")))
        assertEquals(SplashTarget.LOGIN, SplashRouting.targetFor("   ", user("Employee")))
    }

    @Test
    fun `a token without saved user data opens login instead of a broken dashboard`() {
        assertEquals(SplashTarget.LOGIN, SplashRouting.targetFor("token", null))
    }

    @Test
    fun `an employee opens the employee dashboard whatever the casing`() {
        assertEquals(SplashTarget.EMPLOYEE_DASHBOARD, SplashRouting.targetFor("token", user("Employee")))
        assertEquals(SplashTarget.EMPLOYEE_DASHBOARD, SplashRouting.targetFor("token", user("employee")))
        assertEquals(SplashTarget.EMPLOYEE_DASHBOARD, SplashRouting.targetFor("token", user("EMPLOYEE")))
    }

    @Test
    fun `every other role opens the shared admin dashboard`() {
        listOf("pm", "ops", "admin", "Manager").forEach {
            assertEquals(it, SplashTarget.ADMIN_DASHBOARD, SplashRouting.targetFor("token", user(it)))
        }
    }

    @Test
    fun `a user with no role is treated as a non-employee, as before`() {
        assertEquals(SplashTarget.ADMIN_DASHBOARD, SplashRouting.targetFor("token", user(null)))
    }
}
