package com.atvantiq.wfms.ui.screens.login

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.atvantiq.wfms.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @Test
    fun testLoginScreenElementsDisplayed() {
        ActivityScenario.launch<LoginActivity>(Intent(InstrumentationRegistry.getInstrumentation().targetContext, LoginActivity::class.java))
        onView(withId(R.id.phone_email_input)).check(matches(isDisplayed()))
        onView(withId(R.id.passwordEt)).check(matches(isDisplayed()))
        onView(withId(R.id.login_button)).check(matches(isDisplayed()))
        onView(withId(R.id.forgot_password)).check(matches(isDisplayed()))
    }

    @Test
    fun testEmptyUsernameShowsError() {
        ActivityScenario.launch<LoginActivity>(Intent(InstrumentationRegistry.getInstrumentation().targetContext, LoginActivity::class.java))
        onView(withId(R.id.login_button)).perform(click())
        onView(withId(R.id.phone_email_input)).check(matches(hasErrorText("Enter Username")))
    }

    @Test
    fun testEmptyPasswordShowsError() {
        ActivityScenario.launch<LoginActivity>(Intent(InstrumentationRegistry.getInstrumentation().targetContext, LoginActivity::class.java))
        onView(withId(R.id.phone_email_input)).perform(typeText("testuser"), closeSoftKeyboard())
        onView(withId(R.id.login_button)).perform(click())
        onView(withId(R.id.passwordEt)).check(matches(hasErrorText("Enter Password")))
    }

    @Test
    fun testPasswordToggleWorks() {
        ActivityScenario.launch<LoginActivity>(Intent(InstrumentationRegistry.getInstrumentation().targetContext, LoginActivity::class.java))
        onView(withId(R.id.password_toggle_bt)).perform(click())
    }

    @Test
    fun testNavigateToForgotPassword() {
        ActivityScenario.launch<LoginActivity>(Intent(InstrumentationRegistry.getInstrumentation().targetContext, LoginActivity::class.java))
        onView(withId(R.id.forgot_password)).perform(click())
    }
}
