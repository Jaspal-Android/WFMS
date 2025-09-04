package com.atvantiq.wfms.ui.screens.attendance

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.data.repository.work.IWorkRepo
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.models.attendance.checkInStatus.Data
import com.atvantiq.wfms.models.work.acceptWork.AcceptWorkData
import com.atvantiq.wfms.models.work.acceptWork.AcceptWorkResponse
import com.atvantiq.wfms.models.work.assignedAll.Activity
import com.atvantiq.wfms.models.work.assignedAll.AssignedBy
import com.atvantiq.wfms.models.work.assignedAll.Circle
import com.atvantiq.wfms.models.work.assignedAll.Progres
import com.atvantiq.wfms.models.work.assignedAll.Project
import com.atvantiq.wfms.models.work.assignedAll.Site
import com.atvantiq.wfms.models.work.assignedAll.Type
import com.atvantiq.wfms.models.work.assignedAll.WorkAssignedAllResponse
import com.atvantiq.wfms.models.work.assignedAll.WorkRecord
import com.atvantiq.wfms.models.work.endWork.EndWorkData
import com.atvantiq.wfms.models.work.endWork.EndWorkResponse
import com.atvantiq.wfms.models.work.startWork.StartWorkData
import com.atvantiq.wfms.models.work.startWork.StartWorkResponse
import com.atvantiq.wfms.models.work.workDetail.WorkDetailResponse
import com.atvantiq.wfms.models.work.workDetailByDate.WorkDetailsByDateResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.utils.Utils
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@ExperimentalCoroutinesApi
class AttendanceViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var workRepo: IWorkRepo
    private lateinit var attendanceRepo: IAttendanceRepo
    private lateinit var viewModel: AttendanceViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        workRepo = mockk(relaxed = true)
        attendanceRepo = mockk(relaxed = true)

        mockkObject(Utils)
        every { Utils.isInternet(application) } returns true

        viewModel = AttendanceViewModel(application, workRepo, attendanceRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `onSignInClick sets clickEvents value`() {
        viewModel.onSignInClick()
        assertEquals(AttendanceClickEvents.ON_SIGN_IN_CLICK, viewModel.clickEvents.value)
    }

    @Test
    fun `onMyProgressClick sets clickEvents value`() {
        viewModel.onMyProgressClick()
        assertEquals(AttendanceClickEvents.ON_MY_PROGRESS_CLICK, viewModel.clickEvents.value)
    }

    @Test
    fun `onSignInDetailsClick sets clickEvents value`() {
        viewModel.onSignInDetailsClick()
        assertEquals(AttendanceClickEvents.ON_SIGN_IN_DETAILS_CLICK, viewModel.clickEvents.value)
    }

    @Test
    fun `startTracking sets isTracking true`() {
        viewModel.startTracking()
        assertEquals(true, viewModel.isTracking.value)
    }

    @Test
    fun `stopTracking sets isTracking false`() {
        viewModel.startTracking()
        viewModel.stopTracking()
        assertEquals(false, viewModel.isTracking.value)
    }

    @Test
    fun `getWorkAssignedAll calls workRepo and updates LiveData`() = runTest {
        val response = WorkAssignedAllResponse(
            code = 200,
            message = "Work assignments fetched successfully",
            success = true,
            data = com.atvantiq.wfms.models.work.assignedAll.Data(
                page = 1,
                pageSize = 10,
                totalCount = 18,
                totalRecords = 15,
                totalPages = 2,
                records = listOf(
                    WorkRecord(
                        id = 985273047249,
                        employee = null,
                        project = Project(
                            id = 57941758,
                            client = 68067649,
                            name = "Airtel-HR-5G-Rollout"
                        ),
                        site = Site(
                            id = 535937546585,
                            name = "ATCHD"
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
                        circle = Circle(
                            id = 37851209,
                            name = "Chandigarh"
                        ),
                        status = "COMPLETED",
                        assignedBy = AssignedBy(
                            id = 95034689,
                            name = "admin"
                        ),
                        progress = listOf(
                            Progres(
                                id = 373581294074,
                                startedAt = "2025-08-27T07:55:54.751320",
                                endedAt = "",
                                startLatitude = 30.7149142,
                                startLongitude = 76.7034246,
                                endedLatitude = null,
                                endedLongitude = null,
                                status = "WIP",
                                remarks = "",
                                photoPath = "static/start_work_photos/39580123/985273047249/373581294074_43bf5ff96bb04d9a8f8097889c01017e.jpg"
                            ),
                            Progres(
                                id = 726294930712,
                                startedAt = "",
                                endedAt = "2025-08-28T06:25:06.329064",
                                startLatitude = null,
                                startLongitude = null,
                                endedLatitude = 30.7149369,
                                endedLongitude = 76.703396,
                                status = "COMPLETED",
                                remarks = "",
                                photoPath = ""
                            )
                        ),
                        createdAt = "2025-08-20T07:34:11.416545",
                        updatedAt = "2025-08-20T07:34:11.416545"
                    )
                )
            )
        )

        coEvery { workRepo.workAssignedAll(any(), any()) } returns response

        viewModel.getWorkAssignedAll(1, 10)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { workRepo.workAssignedAll(1, 10) }
        assertNotNull(viewModel.workAssignedAllResponse.value)
        assertEquals(Status.SUCCESS, viewModel.workAssignedAllResponse.value?.status)
        assertEquals(response, viewModel.workAssignedAllResponse.value?.response)
    }

    @Test
    fun `workById calls workRepo and updates LiveData`() = runTest {
        val response = WorkDetailResponse(
            code = 200,
            message = "Work record fetched successfully",
            success = true,
            data = WorkRecord(
                id = 985273047249,
                employee = null,
                project = Project(
                    id = 57941758,
                    client = 68067649,
                    name = "Airtel-HR-5G-Rollout"
                ),
                site = Site(
                    id = 535937546585,
                    name = "ATCHD"
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
                circle = Circle(
                    id = 37851209,
                    name = "Chandigarh"
                ),
                status = "COMPLETED",
                assignedBy = AssignedBy(
                    id = 95034689,
                    name = "admin"
                ),
                progress = listOf(
                    Progres(
                        id = 373581294074,
                        startedAt = "2025-08-27T07:55:54.751320",
                        endedAt = "",
                        startLatitude = 30.7149142,
                        startLongitude = 76.7034246,
                        endedLatitude = null,
                        endedLongitude = null,
                        status = "WIP",
                        remarks = "",
                        photoPath = "static/start_work_photos/39580123/985273047249/373581294074_43bf5ff96bb04d9a8f8097889c01017e.jpg"
                    ),
                    Progres(
                        id = 726294930712,
                        startedAt = "",
                        endedAt = "2025-08-28T06:25:06.329064",
                        startLatitude = null,
                        startLongitude = null,
                        endedLatitude = 30.7149369,
                        endedLongitude = 76.703396,
                        status = "COMPLETED",
                        remarks = "",
                        photoPath = ""
                    )
                ),
                createdAt = "2025-08-20T07:34:11.416545",
                updatedAt = "2025-08-20T07:34:11.416545"
            )
        )
        coEvery { workRepo.workById(any()) } returns response

        viewModel.workById(123L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { workRepo.workById(123L) }
        assertNotNull(viewModel.workByIdResponse.value)
        assertEquals(Status.SUCCESS, viewModel.workByIdResponse.value?.status)
        assertEquals(response, viewModel.workByIdResponse.value?.response)
    }


    @Test
    fun `workDetailsByDate calls workRepo and updates LiveData`() = runTest {
        val response =
            WorkDetailsByDateResponse(
                code = 200,
                message = "Success",
                success = true,
                data = listOf(
                    WorkRecord(
                        id = 985273047249,
                        employee = null,
                        project = Project(
                            id = 57941758,
                            client = 68067649,
                            name = "Airtel-HR-5G-Rollout"
                        ),
                        site = Site(
                            id = 535937546585,
                            name = "ATCHD"
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
                        circle = Circle(
                            id = 37851209,
                            name = "Chandigarh"
                        ),
                        status = "COMPLETED",
                        assignedBy = AssignedBy(
                            id = 95034689,
                            name = "admin"
                        ),
                        progress = listOf(
                            Progres(
                                id = 373581294074,
                                startedAt = "2025-08-27T07:55:54.751320",
                                endedAt = "",
                                startLatitude = 30.7149142,
                                startLongitude = 76.7034246,
                                endedLatitude = null,
                                endedLongitude = null,
                                status = "WIP",
                                remarks = "",
                                photoPath = "static/start_work_photos/39580123/985273047249/373581294074_43bf5ff96bb04d9a8f8097889c01017e.jpg"
                            ),
                            Progres(
                                id = 726294930712,
                                startedAt = "",
                                endedAt = "2025-08-28T06:25:06.329064",
                                startLatitude = null,
                                startLongitude = null,
                                endedLatitude = 30.7149369,
                                endedLongitude = 76.703396,
                                status = "COMPLETED",
                                remarks = "",
                                photoPath = ""
                            )
                        ),
                        createdAt = "2025-08-20T07:34:11.416545",
                        updatedAt = "2025-08-20T07:34:11.416545"
                    )
                )
            )
        coEvery { workRepo.workDetailByDate(any()) } returns response

        viewModel.workDetailsByDate("2024-06-01")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { workRepo.workDetailByDate("2024-06-01") }
        assertNotNull(viewModel.workDetailsByDateResponse.value)
        assertEquals(Status.SUCCESS, viewModel.workDetailsByDateResponse.value?.status)
        assertEquals(response, viewModel.workDetailsByDateResponse.value?.response)
    }

    @Test
    fun `workAccept calls workRepo and updates LiveData and itemPosition`() = runTest {
        val response =
            AcceptWorkResponse(
                code = 200,
                message = "Task accepted successfully.",
                data = AcceptWorkData(
                    workId = 886000028313,
                    status = "ACCEPTED"
                ),
                success = true
            )
        coEvery { workRepo.workAccept(any()) } returns response

        viewModel.workAccept(123L, 2)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { workRepo.workAccept(123L) }
        assertNotNull(viewModel.workAcceptResponse.value)
        assertEquals(Status.SUCCESS, viewModel.workAcceptResponse.value?.status)
        assertEquals(response, viewModel.workAcceptResponse.value?.response)
        assertEquals(2, viewModel.itemPosition.value)
    }


    @Test
    fun `workStart calls workRepo and updates LiveData and itemPosition`() = runTest {
        val response = StartWorkResponse(
            code = 200,
            message = "Work started successfully.",
            data = StartWorkData(
                workId = 886000028313,
                progressId = 272050434462,
                status = "WIP",
                startedAt = "2025-09-03T07:53:45.250203",
                photoPath = "static/start_work_photos/39580123/886000028313/272050434462_1e6287930ad4445ebbfd14200f7464d8.png"
            ),
            success = true
        )

        coEvery { workRepo.workStart(any(), any(), any(), any()) } returns response

        // Use a temp file for photoPath
        val tempFile = kotlin.io.path.createTempFile().toFile()
        tempFile.writeText("fake image data")

        viewModel.workStart("123", "12.34", "56.78", tempFile.absolutePath, 1)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { workRepo.workStart(any(), any(), any(), any()) }
        assertNotNull(viewModel.workStartResponse.value)
        assertEquals(Status.SUCCESS, viewModel.workStartResponse.value?.status)
        assertEquals(response, viewModel.workStartResponse.value?.response)
        assertEquals(1, viewModel.itemPosition.value)

        tempFile.delete()
    }

    @Test
    fun `workEnd calls workRepo and updates LiveData and itemPosition`() = runTest {
        val response = EndWorkResponse(
            code = 200,
            message = "Work end recorded successfully.",
            data = EndWorkData(
                workId = 886000028313,
                progressId = 272050434462,
                status = "OPEN",
                endedAt = "2025-09-03T07:59:02.746040",
                endedLatitude = 30.7333,
                endedLongitude = 76.7794,
                remarks = "Done"
            ),
            success = true
        )

        coEvery { workRepo.workEnd(any()) } returns response

        viewModel.workEnd(123L, 12.34, 56.78, 1, "Done", 3)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { workRepo.workEnd(any()) }
        assertNotNull(viewModel.workEndResponse.value)
        assertEquals(Status.SUCCESS, viewModel.workEndResponse.value?.status)
        assertEquals(response, viewModel.workEndResponse.value?.response)
        assertEquals(3, viewModel.itemPosition.value)
    }


    @Test
    fun `checkInStatusAttendance calls attendanceRepo and updates LiveData`() = runTest {
        val response = CheckInStatusResponse(
            code = 400,
            message = "Completed attendance for today.",
            data = Data(
                checkedIn = true,
                checkedOut = true,
                checkinTime = "2025-09-02T06:14:05.615114",
                checkoutTime = "2025-09-02T06:14:50.510504",
                attendanceId = 222355621489
            ),
            success = false
        )
        coEvery { attendanceRepo.attendanceCheckInStatus() } returns response

        viewModel.checkInStatusAttendance()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { attendanceRepo.attendanceCheckInStatus() }
        assertNotNull(viewModel.attendanceCheckInStatusResponse.value)
        assertEquals(Status.SUCCESS, viewModel.attendanceCheckInStatusResponse.value?.status)
        assertEquals(response, viewModel.attendanceCheckInStatusResponse.value?.response)
    }
}
