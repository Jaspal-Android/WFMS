package com.atvantiq.wfms.ui.screens.reimbursement.createClaim

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.repository.claims.IClaimRepo
import com.atvantiq.wfms.data.repository.creation.CreationRepo
import com.atvantiq.wfms.models.workSiteByDate.Data
import com.atvantiq.wfms.models.workSiteByDate.Site
import com.atvantiq.wfms.models.workSiteByDate.WorkSiteByDateResponse
import com.atvantiq.wfms.utils.Utils
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Create Claim offers the sites assigned on the chosen day (`GET /claim/sites/date`). */
@ExperimentalCoroutinesApi
class CreateClaimSitesByDateTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val claimRepo = mockk<IClaimRepo>()
    private lateinit var viewModel: CreateClaimVM

    private fun site(id: Long, name: String) = Site(
        circleCode = "HR", circleId = 970952557243L, circleName = "Haryana",
        projectId = 128739441916L, projectName = "TCTS-MH-CISCO-PEYTO",
        siteAddress = "Digwa Charkidadari Hrayana", siteId = id, siteName = name, workSiteId = id + 1
    )

    private fun sites(day: String, vararg list: Site) =
        WorkSiteByDateResponse(ValConstants.SUCCESS_CODE, Data(day, list.toList()), "Sites fetched successfully.", true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(Utils)
        every { Utils.isInternet(application) } returns true
        viewModel = CreateClaimVM(application, claimRepo, mockk<CreationRepo>(relaxed = true), SavedStateHandle())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun idle() = dispatcher.scheduler.advanceUntilIdle()

    @Test
    fun `choosing a day offers that day's sites`() {
        val digwa = site(131452377605L, "Digwa")
        coEvery { claimRepo.workSiteByDate("2026-10-02") } returns sites("2026-10-02", digwa)

        viewModel.onDateChosen("2026-10-02")
        idle()

        assertEquals("2026-10-02", viewModel.date.get())
        assertEquals(listOf(digwa), viewModel.singleSites)
    }

    @Test
    fun `a new day drops the site, project and circle picked for the old one`() {
        val digwa = site(131452377605L, "Digwa")
        coEvery { claimRepo.workSiteByDate(any()) } returns sites("2026-10-02", digwa)
        viewModel.onDateChosen("2026-10-02")
        idle()
        viewModel.onSingleSiteSelected(digwa)

        viewModel.onDateChosen("2026-10-03")

        assertNull(viewModel.selectedSingleSite)
        assertNull(viewModel.selectedProjectId)
        assertNull(viewModel.selectedCircleId)
        assertEquals(emptyList<Site>(), viewModel.singleSites)
    }

    @Test
    fun `a slow answer for an earlier day cannot replace the new day's sites`() {
        val old = site(1L, "Old day site")
        val current = site(2L, "Digwa")
        coEvery { claimRepo.workSiteByDate("2026-10-01") } coAnswers { delay(5_000); sites("2026-10-01", old) }
        coEvery { claimRepo.workSiteByDate("2026-10-02") } returns sites("2026-10-02", current)

        viewModel.onDateChosen("2026-10-01")
        viewModel.onDateChosen("2026-10-02")
        idle()

        assertEquals(listOf(current), viewModel.singleSites)
    }

    @Test
    fun `a rejected answer offers no sites`() {
        coEvery { claimRepo.workSiteByDate(any()) } returns WorkSiteByDateResponse(500, null, "boom", false)

        viewModel.onDateChosen("2026-10-02")
        idle()

        assertEquals(emptyList<Site>(), viewModel.singleSites)
    }
}
