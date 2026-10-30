package com.atvantiq.wfms.ui.screens.reimbursement.createClaim

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.atvantiq.wfms.data.repository.claims.IClaimRepo
import com.atvantiq.wfms.data.repository.creation.CreationRepo
import com.atvantiq.wfms.models.reimbursement.DAExpense
import com.atvantiq.wfms.models.reimbursement.HotelExpense
import com.atvantiq.wfms.models.reimbursement.OtherExpense
import com.atvantiq.wfms.models.reimbursement.TravelExpense
import com.atvantiq.wfms.models.site.SiteData
import com.atvantiq.wfms.models.workSiteByDate.Site
import com.atvantiq.wfms.utils.Utils
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** A long claim survives Android ending the process while the user is in the camera or another app. */
@ExperimentalCoroutinesApi
class CreateClaimRestoreTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val claimRepo = mockk<IClaimRepo>(relaxed = true)
    private val creationRepo = mockk<CreationRepo>(relaxed = true)

    private val travel = TravelExpense(
        mode = null, amount = "120", from = "Chandigarh", to = "Mohali",
        receiptAttachments = listOf("/cache/receipt_1.jpg")
    )
    private val da = DAExpense(entryId = "da-1", amount = "300", receiptAttachments = listOf("/cache/da.jpg"))
    private val hotel = HotelExpense(entryId = "h-1", amount = "2500")
    private val other = OtherExpense(entryId = "o-1", category = "Parking", amount = "50")
    private val site = Site(
        circleCode = "PB", circleId = 7L, circleName = "Punjab", projectId = 12L, projectName = "jio_4g",
        siteAddress = "Sector 17", siteId = 99L, siteName = "Tower 99", workSiteId = 5L
    )
    private val pickedSite = SiteData(id = 3L, name = "Site 3", siteId = "S-3", po = null)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(Utils)
        every { Utils.isInternet(application) } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun viewModel(handle: SavedStateHandle) = CreateClaimVM(application, claimRepo, creationRepo, handle)

    @Test
    fun `a recreated claim keeps what was typed and picked`() {
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            date.set("2026-10-05")
            purpose.set("Client visit")
            remarks.value = "Left at 9"
            isOutstationExpense.set(true)
            onSingleSiteSelected(site)
            singleSites = listOf(site)
        }

        val restored = viewModel(handle)

        assertEquals("2026-10-05", restored.date.get())
        assertEquals("Client visit", restored.purpose.get())
        assertEquals("Left at 9", restored.remarks.value)
        assertEquals(true, restored.isOutstationExpense.get())
        assertEquals(site, restored.selectedSingleSite)
        assertEquals(12L, restored.selectedProjectId)
        assertEquals(7L, restored.selectedCircleId)
        assertEquals("PB", restored.selectedCircleCode)
        assertEquals(listOf(site), restored.singleSites)
    }

    @Test
    fun `a recreated claim keeps every added expense and picked site`() {
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            travelingEntriesList.value = listOf(travel)
            daEntriesList.value = listOf(da)
            hotelEntriesList.value = listOf(hotel)
            othersEntriesList.value = listOf(other)
            selectedSitesIdList.value = listOf(pickedSite)
        }

        val restored = viewModel(handle)

        assertEquals(listOf(travel), restored.travelingEntriesList.value)
        assertEquals(listOf(da), restored.daEntriesList.value)
        assertEquals(listOf(hotel), restored.hotelEntriesList.value)
        assertEquals(listOf(other), restored.othersEntriesList.value)
        assertEquals(listOf(pickedSite), restored.selectedSitesIdList.value)
    }

    @Test
    fun `removing an expense is saved, so it does not come back`() {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        first.travelingEntriesList.value = listOf(travel)

        first.travelingEntriesList.value = emptyList()

        assertEquals(emptyList<TravelExpense>(), viewModel(handle).travelingEntriesList.value)
    }

    @Test
    fun `a multi-site claim fetches the picked project's sites again`() {
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            isMultiSite.set(true)
            selectedProjectId = 12L
        }

        viewModel(handle)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { creationRepo.siteListByProject(12L) }
    }

    @Test
    fun `a claim that was never filled starts empty and fetches nothing`() {
        val vm = viewModel(SavedStateHandle())
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals("", vm.date.get())
        assertFalse(vm.isMultiSite.get() == true)
        assertNull(vm.selectedSingleSite)
        assertEquals(emptyList<TravelExpense>(), vm.travelingEntriesList.value)
        coVerify(exactly = 0) { creationRepo.siteListByProject(any()) }
    }
}
