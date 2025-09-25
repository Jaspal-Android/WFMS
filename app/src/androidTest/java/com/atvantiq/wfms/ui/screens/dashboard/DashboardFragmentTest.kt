package com.atvantiq.wfms.ui.screens.dashboard

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.atvantiq.wfms.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardFragmentTest {

    @Test
    fun testDashboardFragment_LaunchesAndDisplaysTabs() {
        FragmentScenario.launchInContainer(
            DashboardFragment::class.java,
            null,
            R.style.Theme_WFMS
        )
        // Check if tab titles are displayed
        onView(withText(R.string.attendance_status)).check(matches(isDisplayed()))
        onView(withText(R.string.my_targets)).check(matches(isDisplayed()))
        onView(withText(R.string.projects)).check(matches(isDisplayed()))
    }

    @Test
    fun testDashboardFragment_SlideButtonIsDisplayed() {
        FragmentScenario.launchInContainer(
            DashboardFragment::class.java,
            null,
            R.style.Theme_WFMS
        )
        // Check if the slide button is displayed
        onView(withId(R.id.slideStartDay)).check(matches(isDisplayed()))
    }
}
