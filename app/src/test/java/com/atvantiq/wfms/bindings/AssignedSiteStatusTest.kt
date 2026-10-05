package com.atvantiq.wfms.bindings

import android.content.Context
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.StatusCodes
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

/** The status pill on the employee's Work list and task detail. */
class AssignedSiteStatusTest {

    private val context = mockk<Context>(relaxed = true)
    private val pill = mockk<TextView>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.getColor(any(), any()) } returns 0
        every { pill.context } returns context
        every { context.getString(R.string.in_progress) } returns "In Progress"
        every { context.getString(R.string.pending) } returns "Pending"
        every { context.getString(R.string.access_issue) } returns "Access Issue"
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `work in progress reads In Progress, matching the Active filter and the approval screens`() {
        UtilStatusBindings.assignedSiteStatus(pill, StatusCodes.WIP)

        verify { pill.text = "In Progress" }
        verify(exactly = 0) { pill.text = "Pending" }
    }

    @Test
    fun `an access issue gets its own label instead of keeping a recycled row's text`() {
        UtilStatusBindings.assignedSiteStatus(pill, StatusCodes.ACCESS_ISSUE)

        verify { pill.text = "Access Issue" }
    }
}
