package com.atvantiq.wfms.ui.screens.attendance.addSignInActivity

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.atvantiq.wfms.data.repository.creation.ICreationRepo
import com.atvantiq.wfms.data.repository.work.IWorkRepo
import com.atvantiq.wfms.models.activity.ActivityData
import com.atvantiq.wfms.models.client.AddedBy
import com.atvantiq.wfms.models.client.Client
import com.atvantiq.wfms.models.type.TypeData
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Add Work keeps what the user picked when Android ends the process while they are in another app. */
@ExperimentalCoroutinesApi
class AddSignInRestoreTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val application = mockk<Application>(relaxed = true)
    private val creationRepo = mockk<ICreationRepo>(relaxed = true)
    private val workRepo = mockk<IWorkRepo>(relaxed = true)

    private val client = Client(
        addedBy = AddedBy(id = 1L, name = "Admin"), address = "Main St", alternateAddress = null,
        companyName = "Test Company", createdAt = "2024-06-01T10:00:00.000Z", displayName = "TestCo",
        gstNumber = "22AAAAA0000A1Z5", id = 1001L, isActive = 1, state = "Karnataka"
    )
    private val survey = ActivityData(1L, "Survey")
    private val typeA = TypeData(activities = null, id = 10L, name = "A")

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

    private fun viewModel(handle: SavedStateHandle) = AddSignInVM(application, creationRepo, workRepo, handle)

    private fun pickEverything(vm: AddSignInVM) {
        vm.selectedClient = client
        vm.selectedProjectId = 2L
        vm.selectedPoNumberId = 3L
        vm.selectedCircleId = 4L
        vm.selectedSiteId = 5L
        vm.selectedTypeIdList = arrayListOf(typeA)
        vm.activitySelection.setAvailable(10L, listOf(survey))
        vm.activitySelection.select(setOf(TypeActivityOption(10L, "A", survey)))
    }

    @Test
    fun `a recreated form keeps every pick`() {
        val handle = SavedStateHandle()
        pickEverything(viewModel(handle))

        val restored = viewModel(handle)

        assertEquals(client, restored.selectedClient)
        assertEquals(2L, restored.selectedProjectId)
        assertEquals(3L, restored.selectedPoNumberId)
        assertEquals(4L, restored.selectedCircleId)
        assertEquals(5L, restored.selectedSiteId)
        assertEquals(listOf(typeA), restored.selectedTypeIdList)
        assertEquals(setOf(1L), restored.activitySelection.selectedIds(10L))
    }

    @Test
    fun `a recreated form fetches the lists behind the pickers again`() {
        val handle = SavedStateHandle()
        pickEverything(viewModel(handle))

        viewModel(handle)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { creationRepo.projectListByClientId(1001L) }
        coVerify { creationRepo.poNumberListByProject(2L) }
        coVerify { creationRepo.circleByProject(2L) }
        coVerify { creationRepo.siteListByProject(2L) }
        coVerify { creationRepo.typeListByPo(3L) }
        coVerify { creationRepo.activityListByPoType(3L, 10L) }
    }

    @Test
    fun `clearing the types is saved, so they do not come back`() {
        val handle = SavedStateHandle()
        val first = viewModel(handle)
        pickEverything(first)

        first.clearTypes()

        val restored = viewModel(handle)
        assertEquals(emptyList<TypeData>(), restored.selectedTypeIdList)
        assertEquals(emptySet<Long>(), restored.activitySelection.selectedIds(10L))
    }

    @Test
    fun `a form that was never filled starts empty and fetches nothing`() {
        val vm = viewModel(SavedStateHandle())
        dispatcher.scheduler.advanceUntilIdle()

        assertNull(vm.selectedClient)
        assertNull(vm.selectedProjectId)
        coVerify(exactly = 0) { creationRepo.projectListByClientId(any()) }
        coVerify(exactly = 0) { creationRepo.typeListByPo(any()) }
    }
}
