package com.atvantiq.wfms.ui.screens

import com.atvantiq.wfms.models.loginResponse.User
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    private val main = "android.intent.action.MAIN"
    private val launcher = setOf("android.intent.category.LAUNCHER")

    @Test
    fun `the launcher icon over an open app steps aside`() {
        assertTrue(SplashRouting.isRelaunchOverOpenTask(isTaskRoot = false, action = main, categories = launcher))
    }

    @Test
    fun `a cold start from the launcher routes as usual`() {
        assertFalse(SplashRouting.isRelaunchOverOpenTask(isTaskRoot = true, action = main, categories = launcher))
    }

    @Test
    fun `notification taps always route, even over an open app`() {
        assertFalse(SplashRouting.isRelaunchOverOpenTask(false, SplashActivity.ACTION_LOCATION_NOTIFICATION, null))
        assertFalse(SplashRouting.isRelaunchOverOpenTask(false, SplashActivity.ACTION_PUSH_NOTIFICATION, null))
        assertFalse(SplashRouting.isRelaunchOverOpenTask(false, main, emptySet()))
    }
}
