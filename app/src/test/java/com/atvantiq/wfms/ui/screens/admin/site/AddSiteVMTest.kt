package com.atvantiq.wfms.ui.screens.admin.site

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.data.repository.creation.ICreationRepo
import com.atvantiq.wfms.models.circle.CircleData
import com.atvantiq.wfms.models.client.Client
import com.atvantiq.wfms.models.project.ProjectData
import com.atvantiq.wfms.ui.screens.admin.ui.site.addSite.AddSiteVM
import com.atvantiq.wfms.utils.Utils
import com.google.gson.JsonObject
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AddSiteVMTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val creationRepo = mockk<ICreationRepo>(relaxed = true)
    private lateinit var viewModel: AddSiteVM

    private fun client(id: Long, name: String): Client = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { companyName } returns name
    }

    private fun project(id: Long, name: String): ProjectData = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.name } returns name
    }

    private fun circle(id: Long, name: String): CircleData = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { this@mockk.name } returns name
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(Utils)
        every { Utils.isInternet(application) } returns true
        viewModel = AddSiteVM(application, creationRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun fillSite() {
        viewModel.selectClient(client(1, "Jio"))
        viewModel.selectProject(project(12, "jio_4g"))
        viewModel.selectCircle(circle(4, "Chandigarh"))
        viewModel.siteId.set("MH-0042")
        viewModel.siteName.set("Testing 6")
        viewModel.siteAddress.set("Sector 17")
    }

    @Test
    fun `a new client clears the project and circle picked for the previous one`() {
        fillSite()

        viewModel.selectClient(client(2, "Airtel"))

        assertEquals("Airtel", viewModel.clientName.get())
        assertNull(viewModel.projectName.get())
        assertNull(viewModel.circleName.get())
        assertNull(viewModel.selectedProjectId)
        assertNull(viewModel.selectedCircleId)
    }

    @Test
    fun `a new project clears the circle`() {
        fillSite()

        viewModel.selectProject(project(13, "jio_5g"))

        assertNull(viewModel.circleName.get())
        assertNull(viewModel.selectedCircleId)
    }

    @Test
    fun `Create Site is enabled only once the circle and required fields are filled`() {
        assertFalse(viewModel.canCreate.get())
        fillSite()
        assertTrue(viewModel.canCreate.get())

        viewModel.siteAddress.set("   ")
        assertFalse(viewModel.canCreate.get())
    }

    @Test
    fun `empty coordinates are left out, filled ones sent as numbers`() {
        val bodies = mutableListOf<JsonObject>()
        coEvery { creationRepo.createSite(any()) } answers {
            bodies += firstArg<JsonObject>(); mockk(relaxed = true)
        }
        fillSite()

        viewModel.onSaveClick()
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.siteLatitude.set(" 30.71 ")
        viewModel.siteLongitude.set("76.70")
        viewModel.onSaveClick()
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(bodies[0].has("latitude"))
        assertFalse(bodies[0].has("longitude"))
        assertEquals(12L, bodies[0].get("project_id").asLong)
        assertEquals(4L, bodies[0].get("circle_id").asLong)
        assertEquals(30.71, bodies[1].get("latitude").asDouble, 0.0)
        assertEquals(76.70, bodies[1].get("longitude").asDouble, 0.0)
    }

    @Test
    fun `no message on a fresh form, then the first failing check once filling starts`() {
        assertNull(viewModel.validationError.get())

        viewModel.selectClient(client(1, "Jio"))

        assertEquals(com.atvantiq.wfms.R.string.add_site_select_project, viewModel.validationError.get())
    }

    @Test
    fun `an out-of-range latitude keeps Create Site greyed and sends nothing`() {
        fillSite()
        viewModel.siteLatitude.set("91")

        assertFalse(viewModel.canCreate.get())
        assertEquals(com.atvantiq.wfms.R.string.add_site_invalid_latitude, viewModel.validationError.get())
        viewModel.onSaveClick()
        dispatcher.scheduler.advanceUntilIdle()
        io.mockk.coVerify(exactly = 0) { creationRepo.createSite(any()) }
    }
}
