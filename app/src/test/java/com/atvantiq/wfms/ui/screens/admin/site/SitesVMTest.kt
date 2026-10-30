package com.atvantiq.wfms.ui.screens.admin.site

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.constants.StatusCodes
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.repository.creation.ICreationRepo
import com.atvantiq.wfms.models.site.allSites.AllSiteData
import com.atvantiq.wfms.models.site.allSites.Site
import com.atvantiq.wfms.models.site.allSites.SitesListAllResponse
import com.atvantiq.wfms.ui.screens.admin.ui.site.SitesEventClicks
import com.atvantiq.wfms.ui.screens.admin.ui.site.SitesVM
import com.atvantiq.wfms.utils.Utils
import io.mockk.coEvery
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SitesVMTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val creationRepo = mockk<ICreationRepo>()

    private fun response(code: Int, sites: List<Site>) = SitesListAllResponse(
        code, AllSiteData(1, ValConstants.DEFAULT_PAGE_SIZE, sites, sites.size, 1, sites.size), "msg", code == ValConstants.SUCCESS_CODE
    )

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

    @Test
    fun `only active sites are requested`() {
        val sites = listOf(mockk<Site>(), mockk<Site>())
        coEvery { creationRepo.siteListAll(1, ValConstants.DEFAULT_PAGE_SIZE, StatusCodes.SITE_ACTIVE) } returns
            response(ValConstants.SUCCESS_CODE, sites)
        val vm = SitesVM(application, creationRepo)

        vm.sites.open()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(sites, vm.sites.state.value!!.items)
        coVerify { creationRepo.siteListAll(1, ValConstants.DEFAULT_PAGE_SIZE, StatusCodes.SITE_ACTIVE) }
    }

    @Test
    fun `a rejected answer shows no sites`() {
        coEvery { creationRepo.siteListAll(any(), any(), any()) } returns response(500, listOf(mockk()))
        val vm = SitesVM(application, creationRepo)

        vm.sites.open()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList<Site>(), vm.sites.state.value!!.items)
    }

    @Test
    fun `tapping Add Site posts the click event`() {
        val vm = SitesVM(application, creationRepo)

        vm.onAddSiteClick()

        assertEquals(SitesEventClicks.ON_ADD_STIE_CLICK, vm.clickEvents.value)
    }
}
