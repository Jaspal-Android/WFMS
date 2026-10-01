package com.atvantiq.wfms.ui.screens.attendance

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.data.repository.work.IWorkRepo
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.models.work.workAssigned.Site
import com.atvantiq.wfms.models.work.workAssigned.WorkAssignedResponse
import com.atvantiq.wfms.utils.Utils
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The real Work Management fetch: typing a search or tapping a filter while an older request is
 * still running must leave the newest query's answer on screen.
 */
@ExperimentalCoroutinesApi
class SupersededListRequestsTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var workRepo: IWorkRepo
    private lateinit var viewModel: AttendanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val application = mockk<Application>(relaxed = true)
        workRepo = mockk(relaxed = true)
        mockkObject(Utils)
        every { Utils.isInternet(application) } returns true
        viewModel = AttendanceViewModel(application, workRepo, mockk(relaxed = true), mockk<SecurePrefMain>(relaxed = true))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `the answer to the latest search wins even when an older search answers last`() {
        val oldSite = mockk<Site>(relaxed = true)
        val newSite = mockk<Site>(relaxed = true)
        val oldAnswer = CompletableDeferred<WorkAssignedResponse>()
        coEvery { workRepo.workAssignedAll(1, 10, "ab", any()) } coAnswers { oldAnswer.await() }
        coEvery { workRepo.workAssignedAll(1, 10, "abc", any()) } returns answerWith(newSite)

        viewModel.searchQuery = "ab"
        viewModel.workList.reload()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.searchQuery = "abc"
        viewModel.workList.reload()
        testDispatcher.scheduler.advanceUntilIdle()

        oldAnswer.complete(answerWith(oldSite)) // the slow "ab" answer arrives after "abc"
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(newSite), viewModel.workList.state.value?.items)
    }

    private fun answerWith(site: Site): WorkAssignedResponse = mockk(relaxed = true) {
        every { code } returns ValConstants.SUCCESS_CODE
        every { data.results } returns listOf(site)
    }
}
