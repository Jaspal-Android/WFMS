package com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailData
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.models.attendance.attendanceDetails.Checkin
import com.atvantiq.wfms.models.attendance.attendanceDetails.Checkout
import com.atvantiq.wfms.models.attendance.attendanceDetails.Employee
import com.atvantiq.wfms.models.attendance.attendanceDetails.Record
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
class AttendanceStatusVMTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var attendanceRepo: IAttendanceRepo
    private lateinit var viewModel: AttendanceStatusVM
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        attendanceRepo = mockk(relaxed = true)
        mockkObject(Utils)
        every { Utils.isInternet(application) } returns true
        viewModel = AttendanceStatusVM(application, attendanceRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `getAttendanceDetails calls attendanceRepo and updates LiveData`() = runTest {

        val response = AttendanceDetailListResponse(
            code = 200,
            message = "Attendance records fetched successfully.",
            success = true,
            data = AttendanceDetailData(
                page = 1,
                pageSize = 10,
                totalCount = 16,
                totalRecords = 4,
                totalPages = 1,
                records = listOf(
                    Record(
                        id = 562030799601,
                        employee = Employee(
                            id = 12345,
                            code = "EMP001",
                            name = "John Doe"
                        ),
                        checkin = Checkin(
                            time = "2025-08-28T06:17:56.848415",
                            latitude = 30.7149134,
                            logitude = 76.7034208
                        ),
                        checkout = Checkout(
                            time = "2025-08-28T06:18:18.636806",
                            latitude = 30.7149842,
                            logitude = 76.7033593
                        ),
                        status = 2,
                        workHours = "00:00",
                        createdAt = "2025-08-28T06:17:56.848415",
                        canHrMarkAttendance = false,
                    )
                )
            )
        )
        coEvery { attendanceRepo.attendanceDetails(6, 2024) } returns response

        viewModel.getAttendanceDetails(6, 2024)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { attendanceRepo.attendanceDetails(6, 2024) }
        assertNotNull(viewModel.attendanceDetailsResponse.value)
        assertEquals(Status.SUCCESS, viewModel.attendanceDetailsResponse.value?.status)
        assertEquals(response, viewModel.attendanceDetailsResponse.value?.response)
    }
}
