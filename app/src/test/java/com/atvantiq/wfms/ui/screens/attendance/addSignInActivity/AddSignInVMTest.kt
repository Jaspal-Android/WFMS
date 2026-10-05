package com.atvantiq.wfms.ui.screens.attendance.addSignInActivity

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.atvantiq.wfms.data.repository.creation.ICreationRepo
import com.atvantiq.wfms.data.repository.work.IWorkRepo
import com.atvantiq.wfms.models.activity.ActivityData
import com.atvantiq.wfms.models.client.Client
import com.atvantiq.wfms.models.client.ClientListResponse
import com.atvantiq.wfms.models.project.ProjectListByClientResponse
import com.atvantiq.wfms.models.po.PoListByProjectResponse
import com.atvantiq.wfms.models.circle.CircleListByProjectResponse
import com.atvantiq.wfms.models.site.SiteListByProjectResponse
import com.atvantiq.wfms.models.activity.ActivityListByProjectTypeResponse
import com.atvantiq.wfms.models.circle.CircleData
import com.atvantiq.wfms.models.client.AddedBy
import com.atvantiq.wfms.models.client.ClientData
import com.atvantiq.wfms.models.po.PoData
import com.atvantiq.wfms.models.project.ProjectData
import com.atvantiq.wfms.models.site.SiteData
import com.atvantiq.wfms.models.work.assignedAll.Circle
import com.atvantiq.wfms.models.work.assignedAll.Project
import com.atvantiq.wfms.models.work.selfAssign.Activity
import com.atvantiq.wfms.models.work.selfAssign.AssignedBy
import com.atvantiq.wfms.models.work.selfAssign.Details
import com.atvantiq.wfms.models.work.selfAssign.Employee
import com.atvantiq.wfms.models.work.selfAssign.Result
import com.atvantiq.wfms.models.work.selfAssign.SelfAssignData
import com.atvantiq.wfms.models.work.selfAssign.SelfAssignResponse
import com.atvantiq.wfms.models.work.selfAssign.Site
import com.atvantiq.wfms.models.work.selfAssign.Type
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.utils.Utils
import com.google.gson.JsonObject
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class AddSignInVMTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var creationRepo: ICreationRepo
    private lateinit var workRepo: IWorkRepo
    private lateinit var viewModel: AddSignInVM
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        creationRepo = mockk(relaxed = true)
        workRepo = mockk(relaxed = true)
        mockkObject(Utils)
        every { Utils.isInternet(application) } returns true
        viewModel = AddSignInVM(application, creationRepo, workRepo, SavedStateHandle())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

  /*  @Test
    fun `onCameraClick sets clickEvents value`() {
        viewModel.onCameraClick()
        assertEquals(AddSignInClickEvents.ON_CAMERA_CLICK, viewModel.clickEvents.value)
    }*/

    @Test
    fun `onCancelClick sets clickEvents value`() {
        viewModel.onCancelClick()
        assertEquals(AddSignInClickEvents.ON_CANCEL_CLICK, viewModel.clickEvents.value)
    }

   /* @Test
    fun `validateAssignTaskFields returns false and sets errorHandler for missing fields`() {
        // All fields null
        viewModel.selectedClient = null
        assertFalse(viewModel.validateAssignTaskFields())
        assertEquals(AssignTaskError.ON_CLIENT_ERROR, viewModel.errorHandler.value)

        viewModel.selectedClient = Client(
            addedBy = AddedBy(id = 1L, name = "Admin"),
            address = "123 Main Street",
            alternateAddress = "456 Side Street",
            companyName = "Test Company Pvt Ltd",
            createdAt = "2024-06-01T10:00:00.000Z",
            displayName = "TestCo",
            gstNumber = "22AAAAA0000A1Z5",
            id = 1001L,
            isActive = 1,
            state = "Karnataka"
        )
        viewModel.selectedProjectId = null
        assertFalse(viewModel.validateAssignTaskFields())
        assertEquals(AssignTaskError.ON_PROJECT_ERROR, viewModel.errorHandler.value)

        viewModel.selectedProjectId = 1L
        viewModel.selectedPoNumberId = null
        assertFalse(viewModel.validateAssignTaskFields())
        assertEquals(AssignTaskError.ON_PO_NUMBER_ERROR, viewModel.errorHandler.value)

        viewModel.selectedPoNumberId = 1L
        viewModel.selectedCircleId = null
        assertFalse(viewModel.validateAssignTaskFields())
        assertEquals(AssignTaskError.ON_CIRCLE_ERROR, viewModel.errorHandler.value)

        viewModel.selectedCircleId = 1L
        viewModel.selectedSiteId = null
        assertFalse(viewModel.validateAssignTaskFields())
        assertEquals(AssignTaskError.ON_SITE_ERROR, viewModel.errorHandler.value)

        viewModel.selectedSiteId = 1L
        viewModel.selectedTypeIdList = arrayListOf()
        assertFalse(viewModel.validateAssignTaskFields())
        assertEquals(AssignTaskError.ON_TYPE_ERROR, viewModel.errorHandler.value)

        viewModel.selectedTypeIdList = arrayListOf(1L)
        viewModel.selectedActivityIdList = arrayListOf()
        assertFalse(viewModel.validateAssignTaskFields())
        assertEquals(AssignTaskError.ON_ACTIVITY_ERROR, viewModel.errorHandler.value)
    }
*/
/*    @Test
    fun `validateAssignTaskFields returns true when all fields are set`() {
        viewModel.selectedClient = Client(
            addedBy = AddedBy(id = 1L, name = "Admin"),
            address = "123 Main Street",
            alternateAddress = "456 Side Street",
            companyName = "Test Company Pvt Ltd",
            createdAt = "2024-06-01T10:00:00.000Z",
            displayName = "TestCo",
            gstNumber = "22AAAAA0000A1Z5",
            id = 1001L,
            isActive = 1,
            state = "Karnataka"
        )
        viewModel.selectedProjectId = 1L
        viewModel.selectedPoNumberId = 2L
        viewModel.selectedCircleId = 3L
        viewModel.selectedSiteId = 4L
        viewModel.selectedTypeIdList = arrayListOf(5L)
        viewModel.selectedActivityIdList = arrayListOf(6L)
        assertTrue(viewModel.validateAssignTaskFields())
    }*/

    @Test
    fun `getClientList calls creationRepo and updates LiveData`() = runTest {
        val response = ClientListResponse(
            code = 200,
            message = "Clients fetched successfully.",
            success = true,
            data = ClientData(
                page = 1,
                pageSize = 10,
                totalCount = 6,
                totalRecords = 6,
                totalPages = 1,
                clients = listOf(
                    Client(
                        id = 384640712673,
                        companyName = "Lotus Biscoff ultra pro max",
                        displayName = "LB",
                        gstNumber = "07ABCDE1234F2Z0",
                        state = "Punjab",
                        address = "Tim cook",
                        alternateAddress = "string",
                        isActive = 0,
                        addedBy = AddedBy(id = 95034689, name = "admin"),
                        createdAt = "2025-08-27T07:21:45.980686"
                    )
                )
            )
        )
        coEvery { creationRepo.clientList() } returns response

        viewModel.getClientList()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { creationRepo.clientList() }
        assertNotNull(viewModel.clientListResponse.value)
        assertEquals(Status.SUCCESS, viewModel.clientListResponse.value?.status)
        assertEquals(response, viewModel.clientListResponse.value?.response)
    }

    @Test
    fun `getProjectListByClientId calls creationRepo and updates LiveData`() = runTest {
        val response = ProjectListByClientResponse(
            code = 200,
            message = "Projects fetched successfully.",
            success = true,
            data = listOf(
                ProjectData(
                    id = 478044733426,
                    name = "Jio-5g"
                )
            )
        )
        coEvery { creationRepo.projectListByClientId(any()) } returns response

        viewModel.getProjectListByClientId(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { creationRepo.projectListByClientId(1L) }
        assertNotNull(viewModel.projectListByClientResponse.value)
        assertEquals(Status.SUCCESS, viewModel.projectListByClientResponse.value?.status)
        assertEquals(response, viewModel.projectListByClientResponse.value?.response)
    }

    @Test
    fun `getPoNumberListByProject calls creationRepo and updates LiveData`() = runTest {
        val response =  PoListByProjectResponse(
            code = 200,
            message = "Purchase orders fetched successfully.",
            success = true,
            data = listOf(
                PoData(
                    id = 673299769695,
                    poNumber = "0003",
                    poDate = "2025-08-29",
                    clientId = 980369557118,
                    projectId = 858077505915,
                    quantity = 1,
                    validFrom = "2025-08-21",
                    validTo = "2025-08-21",
                    location = "Mohali",
                    createdAt = "2025-08-29T07:26:47.601369",
                    updatedAt = "2025-08-29T07:26:47.601369"
                )
            )
        )
        coEvery { creationRepo.poNumberListByProject(any()) } returns response

        viewModel.getPoNumberListByProject(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { creationRepo.poNumberListByProject(1L) }
        assertNotNull(viewModel.poNumberListByProjectResponse.value)
        assertEquals(Status.SUCCESS, viewModel.poNumberListByProjectResponse.value?.status)
        assertEquals(response, viewModel.poNumberListByProjectResponse.value?.response)
    }

    @Test
    fun `getCircleListByProject calls creationRepo and updates LiveData`() = runTest {
        val response = CircleListByProjectResponse(
            code = 200,
            message = "Circles fetched successfully.",
            success = true,
            data = listOf(
                CircleData(
                    id = 37851209,
                    code = "CH",
                    name = "Chandigarh"
                )
            )
        )
        coEvery { creationRepo.circleByProject(any()) } returns response

        viewModel.getCircleListByProject(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { creationRepo.circleByProject(1L) }
        assertNotNull(viewModel.circleListByProjectResponse.value)
        assertEquals(Status.SUCCESS, viewModel.circleListByProjectResponse.value?.status)
        assertEquals(response, viewModel.circleListByProjectResponse.value?.response)
    }

   /* @Test
    fun `getSiteListByProject calls creationRepo and updates LiveData`() = runTest {
        val response = SiteListByProjectResponse(
            code = 200,
            message = "Sites fetched successfully.",
            success = true,
            data = listOf(
                SiteData(
                    id = 535937546585,
                    siteId = "AT00010",
                    name = "ATCHD",

                )
            )
        )
        coEvery { creationRepo.siteListByProject(any()) } returns response

        viewModel.getSiteListByProject(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { creationRepo.siteListByProject(1L) }
        assertNotNull(viewModel.siteListByProjectResponse.value)
        assertEquals(Status.SUCCESS, viewModel.siteListByProjectResponse.value?.status)
        assertEquals(response, viewModel.siteListByProjectResponse.value?.response)
    }*/

/*
    @Test
    fun `getTypeListByPo calls creationRepo and updates LiveData`() = runTest {
        val response = TypeListByProjectResponse(
            code = 200,
            message = "Types fetched successfully.",
            success = true,
            data = listOf(
                TypeData(
                    id = 829040919323,
                    name = "phone"
                )
            )
        )
        coEvery { creationRepo.typeListByPo(any()) } returns response

        viewModel.getTypeListByPo(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { creationRepo.typeListByPo(1L) }
        assertNotNull(viewModel.typeListByProjectResponse.value)
        assertEquals(Status.SUCCESS, viewModel.typeListByProjectResponse.value?.status)
        assertEquals(response, viewModel.typeListByProjectResponse.value?.response)
    }
*/

    @Test
    fun `onTypesSelected loads activities for every selected type`() = runTest {
        coEvery { creationRepo.activityListByPoType(1L, 10L) } returns activityResponse(ActivityData(1L, "Survey"))
        coEvery { creationRepo.activityListByPoType(1L, 20L) } returns activityResponse(ActivityData(2L, "Install"))
        viewModel.selectedPoNumberId = 1L

        viewModel.onTypesSelected(listOf(type(10L, "A"), type(20L, "B")))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { creationRepo.activityListByPoType(1L, 10L) }
        coVerify(exactly = 1) { creationRepo.activityListByPoType(1L, 20L) }
        assertEquals(Status.SUCCESS, viewModel.typeActivitiesResponse.value?.status)
        assertEquals(
            listOf("Survey", "Install"),
            viewModel.activitySelection.options(viewModel.selectedTypeKeys()).map { it.activity.name }
        )
    }

    @Test
    fun `onTypesSelected keeps picks of types that stay selected and does not refetch them`() = runTest {
        coEvery { creationRepo.activityListByPoType(1L, 10L) } returns activityResponse(ActivityData(1L, "Survey"))
        coEvery { creationRepo.activityListByPoType(1L, 20L) } returns activityResponse(ActivityData(2L, "Install"))
        viewModel.selectedPoNumberId = 1L
        viewModel.onTypesSelected(listOf(type(10L, "A")))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.activitySelection.select(viewModel.activitySelection.options(viewModel.selectedTypeKeys()).toSet())

        viewModel.onTypesSelected(listOf(type(10L, "A"), type(20L, "B")))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { creationRepo.activityListByPoType(1L, 10L) }
        assertEquals(setOf(1L), viewModel.activitySelection.selectedIds(10L))
    }

    @Test
    fun `activities that arrive after the type was deselected are ignored`() = runTest {
        coEvery { creationRepo.activityListByPoType(1L, 10L) } returns activityResponse(ActivityData(1L, "Survey"))
        viewModel.selectedPoNumberId = 1L
        viewModel.onTypesSelected(listOf(type(10L, "A")))

        viewModel.onTypesSelected(emptyList())
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.activitySelection.options(listOf(10L to "A")).isEmpty())
    }

    @Test
    fun `validateAssignTaskFields requires selected activity`() {
        viewModel.selectedClient = Client(
            id = 1L,
            companyName = "Client",
            displayName = "Client",
            gstNumber = "GST",
            state = "State",
            address = "Address",
            alternateAddress = "Alt",
            isActive = 1,
            addedBy = AddedBy(id = 1L, name = "Admin"),
            createdAt = "2026-01-01"
        )
        viewModel.selectedProjectId = 2L
        viewModel.selectedPoNumberId = 3L
        viewModel.selectedCircleId = 4L
        viewModel.selectedSiteId = 5L
        viewModel.selectedTypeIdList = arrayListOf(
            com.atvantiq.wfms.models.type.TypeData(
                activities = emptyList(),
                id = 6L,
                name = "Type"
            )
        )

        assertFalse(viewModel.validateAssignTaskFields())
        assertEquals(AssignTaskError.ON_ACTIVITY_ERROR, viewModel.errorHandler.value)
    }

    @Test
    fun `validateAssignTaskFields requires an activity for every selected type`() {
        fillCommonFields()
        viewModel.selectedTypeIdList = arrayListOf(type(10L, "A"), type(20L, "B"))
        viewModel.activitySelection.setAvailable(10L, listOf(ActivityData(1L, "Survey")))
        viewModel.activitySelection.setAvailable(20L, listOf(ActivityData(2L, "Install")))
        viewModel.activitySelection.select(setOf(TypeActivityOption(10L, "A", ActivityData(1L, "Survey"))))

        assertFalse(viewModel.validateAssignTaskFields())
        assertEquals(AssignTaskError.ON_ACTIVITY_ERROR, viewModel.errorHandler.value)
    }

    @Test
    fun `onSaveClick sends selected activity ids in self assign payload`() = runTest {
        val response = mockk<SelfAssignResponse>(relaxed = true)
        val params = slot<JsonObject>()
        coEvery { workRepo.workSelfAssign(capture(params)) } returns response
        fillCommonFields()
        viewModel.selectedTypeIdList = arrayListOf(type(66L, "Type"))
        viewModel.activitySelection.setAvailable(
            66L, listOf(ActivityData(77L, "One"), ActivityData(88L, "Two"), ActivityData(99L, "Not selected"))
        )
        viewModel.activitySelection.select(
            setOf(
                TypeActivityOption(66L, "Type", ActivityData(77L, "One")),
                TypeActivityOption(66L, "Type", ActivityData(88L, "Two"))
            )
        )

        viewModel.onSaveClick()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { workRepo.workSelfAssign(any()) }
        val activityArray = params.captured
            .getAsJsonArray("type")
            .first()
            .asJsonObject
            .getAsJsonArray("activity")
        assertEquals(listOf(77L, 88L), activityArray.map { it.asJsonObject.get("id").asLong })
    }

    @Test
    fun `onSaveClick sends each type only its own activities`() = runTest {
        val params = slot<JsonObject>()
        coEvery { workRepo.workSelfAssign(capture(params)) } returns mockk<SelfAssignResponse>(relaxed = true)
        fillCommonFields()
        viewModel.selectedTypeIdList = arrayListOf(type(10L, "A"), type(20L, "B"))
        viewModel.activitySelection.setAvailable(10L, listOf(ActivityData(1L, "Survey"), ActivityData(3L, "Audit")))
        viewModel.activitySelection.setAvailable(20L, listOf(ActivityData(2L, "Install")))
        viewModel.activitySelection.select(
            setOf(
                TypeActivityOption(10L, "A", ActivityData(1L, "Survey")),
                TypeActivityOption(20L, "B", ActivityData(2L, "Install"))
            )
        )

        viewModel.onSaveClick()
        testDispatcher.scheduler.advanceUntilIdle()

        val byType = params.captured.getAsJsonArray("type").associate { typeJson ->
            val obj = typeJson.asJsonObject
            obj.get("id").asLong to obj.getAsJsonArray("activity").map { it.asJsonObject.get("id").asLong }
        }
        assertEquals(mapOf(10L to listOf(1L), 20L to listOf(2L)), byType)
    }

   /* @Test
    fun `onSaveClick calls workRepo and updates LiveData when validation passes`() = runTest {
        val response = SelfAssignResponse(
            code = 200,
            message = "1 assignment created successfully.",
            success = true,
            data = SelfAssignData(
                client = com.atvantiq.wfms.models.work.selfAssign.Client(id = 68067649, name = "Airtel"),
                project = Project(id = 57941758, name = "Airtel-HR-5G-Rollout", client =68067649),
                circle = Circle(id = 37851209, name = "Chandigarh", code = "CH"),
                results = listOf(
                    Result(
                        message = "Work assigned for Employee 39580123, Site 535937546585, Type 62653421, Activity 45508443.",
                        details = Details(
                            id = 370222601935,
                            employee = Employee(
                                id = 39580123,
                                name = "Happy",
                                site = listOf(
                                    Site(
                                        id = 535937546585,
                                        siteId = "AT00010",
                                        name = "ATCHD"
                                    )
                                ),
                                type = listOf(
                                    Type(
                                        id = 62653421,
                                        name = "Type: Site Survey",
                                        activity = listOf(
                                            Activity(
                                                id = 45508443,
                                                name = "Identify existing 4G tower locations"
                                            )
                                        )
                                    )
                                ),
                                status = "ACCEPTED",
                                assignedBy = AssignedBy(
                                    id = 39580123,
                                    name = "Happy"
                                ),
                                createdAt = "2025-04-21T19:10:05"
                            )
                        )
                    )
                )
            )
        )
        coEvery { workRepo.workSelfAssign(any()) } returns response

        viewModel.selectedClient = Client(
            id = 384640712673,
            companyName = "Lotus Biscoff ultra pro max",
            displayName = "LB",
            gstNumber = "07ABCDE1234F2Z0",
            state = "Punjab",
            address = "Tim cook",
            alternateAddress = "string",
            isActive = 0,
            addedBy = AddedBy(id = 95034689, name = "admin"),
            createdAt = "2025-08-27T07:21:45.980686"
        )
        viewModel.selectedProjectId = 1L
        viewModel.selectedPoNumberId = 2L
        viewModel.selectedCircleId = 3L
        viewModel.selectedSiteId = 4L
        viewModel.selectedTypeIdList = arrayListOf(5L)
        viewModel.selectedActivityIdList = arrayListOf(6L)

        viewModel.onSaveClick()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { workRepo.workSelfAssign(any()) }
        assertNotNull(viewModel.workAssignedResponse.value)
        assertEquals(Status.SUCCESS, viewModel.workAssignedResponse.value?.status)
        assertEquals(response, viewModel.workAssignedResponse.value?.response)
    }*/

    private fun type(id: Long, name: String) =
        com.atvantiq.wfms.models.type.TypeData(activities = emptyList(), id = id, name = name)

    private fun activityResponse(vararg activities: ActivityData) = ActivityListByProjectTypeResponse(
        code = 200,
        message = "Activities fetched successfully.",
        success = true,
        data = activities.toList()
    )

    private fun fillCommonFields() {
        viewModel.selectedClient = Client(
            id = 11L,
            companyName = "Client",
            displayName = "Client",
            gstNumber = "GST",
            state = "State",
            address = "Address",
            alternateAddress = "Alt",
            isActive = 1,
            addedBy = AddedBy(id = 1L, name = "Admin"),
            createdAt = "2026-01-01"
        )
        viewModel.selectedProjectId = 22L
        viewModel.selectedPoNumberId = 33L
        viewModel.selectedCircleId = 44L
        viewModel.selectedSiteId = 55L
    }
}
