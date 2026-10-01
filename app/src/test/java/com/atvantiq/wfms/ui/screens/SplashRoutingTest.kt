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
    fun `every role opens the same dashboard, which picks its own tabs`() {
        listOf("Employee", "employee", "pm", "ops", "admin", "Manager", null).forEach {
            assertEquals(it, SplashTarget.DASHBOARD, SplashRouting.targetFor("token", user(it)))
        }
    }
}
