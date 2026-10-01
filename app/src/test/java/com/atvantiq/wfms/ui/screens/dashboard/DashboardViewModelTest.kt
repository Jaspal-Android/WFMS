package com.atvantiq.wfms.ui.screens.dashboard

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.atvantiq.wfms.data.prefs.PrefKeys
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.data.repository.auth.IAuthRepo
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.models.attendance.checkInStatus.Data
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import com.atvantiq.wfms.models.attendance.CheckInOutResponse
import com.atvantiq.wfms.models.attendance.CheckoutData
import com.atvantiq.wfms.models.circle.CircleData
import com.atvantiq.wfms.models.empDetail.AccessLevel
import com.atvantiq.wfms.models.empDetail.EmpData
import com.atvantiq.wfms.models.empDetail.EmpDetailResponse
import com.atvantiq.wfms.models.empDetail.Permission
import com.atvantiq.wfms.models.empDetail.ReportingManager
import com.atvantiq.wfms.models.loginResponse.OfficialLocation
import com.atvantiq.wfms.utils.Utils

@ExperimentalCoroutinesApi
class DashboardViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var attendanceRepo: IAttendanceRepo
    private lateinit var authRepo: IAuthRepo
    private lateinit var prefMain: SecurePrefMain
    private lateinit var viewModel: DashboardViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        attendanceRepo = mockk(relaxed = true)
        authRepo = mockk(relaxed = true)
        prefMain = mockk(relaxed = true)

        mockkObject(Utils)
        every { Utils.isInternet(application) } returns true

        viewModel = DashboardViewModel(application, attendanceRepo, authRepo, prefMain, mockk(relaxed = true))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `onAnnouncementsClicks sets clickEvents value`() {
        viewModel.onAnnouncementsClicks()
        assertEquals(DashboardClickEvents.onAnnouncementsClicks, viewModel.clickEvents.value)
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

    // --- Tracking must end with the attendance day, independent of any visible screen ---

    private fun statusResponse(code: Int, checkedIn: Boolean) = CheckInStatusResponse(
        code = code,
        message = "",
        data = Data(
            checkedIn = checkedIn,
            checkedOut = code == 400,
            checkinTime = "",
            checkoutTime = "",
            attendanceId = 1L
        ),
        success = code == 200
    )

    private fun checkOutResponse(code: Int) = CheckInOutResponse(
        code = code,
        message = "",
        success = code == 200,
        data = CheckoutData(attendanceId = 1L, dayProgress = false)
    )

    @Test
    fun `successful checkout stops tracking even when no screen observes the response`() = runTest {
        coEvery { attendanceRepo.attendanceCheckOutRequest(any()) } returns checkOutResponse(200)
        viewModel.startTracking()

        viewModel.checkOutAttendance(12.34, 56.78, true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.isTracking.value)
        verify { prefMain.put(PrefKeys.IS_TRACKING_ACTIVE, false) }
    }

    @Test
    fun `checkout blocked by no work for the day keeps tracking running`() = runTest {
        coEvery { attendanceRepo.attendanceCheckOutRequest(any()) } returns checkOutResponse(3001)
        viewModel.startTracking()

        viewModel.checkOutAttendance(12.34, 56.78, true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, viewModel.isTracking.value)
        verify(exactly = 0) { prefMain.put(PrefKeys.IS_TRACKING_ACTIVE, false) }
    }

    @Test
    fun `status not checked in stops tracking that is still marked active`() = runTest {
        every { prefMain.get(PrefKeys.IS_TRACKING_ACTIVE, false) } returns true
        coEvery { attendanceRepo.attendanceCheckInStatus() } returns statusResponse(200, checkedIn = false)

        viewModel.checkInStatusAttendance()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { prefMain.put(PrefKeys.IS_TRACKING_ACTIVE, false) }
    }

    @Test
    fun `status attendance already completed stops tracking that is still marked active`() = runTest {
        every { prefMain.get(PrefKeys.IS_TRACKING_ACTIVE, false) } returns true
        coEvery { attendanceRepo.attendanceCheckInStatus() } returns statusResponse(400, checkedIn = true)

        viewModel.checkInStatusAttendance()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { prefMain.put(PrefKeys.IS_TRACKING_ACTIVE, false) }
    }

    @Test
    fun `status checked in leaves tracking untouched`() = runTest {
        every { prefMain.get(PrefKeys.IS_TRACKING_ACTIVE, false) } returns true
        coEvery { attendanceRepo.attendanceCheckInStatus() } returns statusResponse(200, checkedIn = true)

        viewModel.checkInStatusAttendance()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { prefMain.put(PrefKeys.IS_TRACKING_ACTIVE, false) }
    }

    @Test
    fun `status not checked in does not rewrite prefs when tracking is already off`() = runTest {
        every { prefMain.get(PrefKeys.IS_TRACKING_ACTIVE, false) } returns false
        coEvery { attendanceRepo.attendanceCheckInStatus() } returns statusResponse(200, checkedIn = false)

        viewModel.checkInStatusAttendance()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { prefMain.put(PrefKeys.IS_TRACKING_ACTIVE, any<Boolean>()) }
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

        // Act
        viewModel.checkInStatusAttendance()
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { attendanceRepo.attendanceCheckInStatus() }
        assertNotNull(viewModel.attendanceCheckInStatusResponse.value)
        assertEquals(Status.SUCCESS, viewModel.attendanceCheckInStatusResponse.value?.status)
        assertEquals(response, viewModel.attendanceCheckInStatusResponse.value?.response)
    }

    @Test
    fun `checkInAttendance calls attendanceRepo and updates LiveData`() = runTest {
        val response = CheckInOutResponse(
            code = 200,
            message = "Attendance marked successfully for today.",
            success = true,
            data = CheckoutData(
                attendanceId = 222355621489,
                dayProgress = false
            )
        )
        coEvery { attendanceRepo.attendanceCheckInRequest(any()) } returns response

        viewModel.checkInAttendance(12.34, 56.78)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { attendanceRepo.attendanceCheckInRequest(any()) }
        assertNotNull(viewModel.attendanceCheckInResponse.value)
        assertEquals(Status.SUCCESS, viewModel.attendanceCheckInResponse.value?.status)
        assertEquals(response, viewModel.attendanceCheckInResponse.value?.response)
    }

        @Test
        fun `checkOutAttendance calls attendanceRepo and updates LiveData`() = runTest {
            val response = CheckInOutResponse(
                code = 200,
                message = "Attendance check-out marked successfully.",
                success = true,
                data  = CheckoutData(
                    attendanceId = 222355621489,
                    dayProgress = false
                )
            )
            coEvery { attendanceRepo.attendanceCheckOutRequest(any()) } returns response

            viewModel.checkOutAttendance(12.34, 56.78,true)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify(exactly = 1) { attendanceRepo.attendanceCheckOutRequest(any()) }
            assertNotNull(viewModel.attendanceCheckOutResponse.value)
            assertEquals(Status.SUCCESS, viewModel.attendanceCheckOutResponse.value?.status)
            assertEquals(response, viewModel.attendanceCheckOutResponse.value?.response)
        }

    @Test
    fun `getEmpDetails calls authRepo and updates LiveData`() = runTest {
        val response = EmpDetailResponse(
            code = 200,
            message = "Employee data fetched successfully",
            success = true,
            data = EmpData(
                employeeId = 39580123,
                employeeCode = "EMP/001",
                name = "Test Employee",
                shortName = "Test",
                dob = "2000-01-01",
                gender = "male",
                email = "employee@example.com",
                role = "Employee",
                permissions = listOf(
                    Permission(
                        featureId = 99923892,
                        featureName = "Self_Assign_Work",
                        accessLevels = listOf(
                            AccessLevel(accessId = 32875610, access = "Create"),
                            AccessLevel(accessId = 54328964, access = "View")
                        )
                    ),
                    Permission(
                        featureId = 87600650,
                        featureName = "Client",
                        accessLevels = listOf(
                            AccessLevel(accessId = 54328964, access = "View"),
                            AccessLevel(accessId = 54328964, access = "View"),
                            AccessLevel(accessId = 81459237, access = "Delete")
                        )
                    ),
                    Permission(
                        featureId = 94039589,
                        featureName = "purchase_order",
                        accessLevels = listOf(
                            AccessLevel(accessId = 54328964, access = "View")
                        )
                    ),
                    Permission(
                        featureId = 20569158,
                        featureName = "Project",
                        accessLevels = listOf(
                            AccessLevel(accessId = 54328964, access = "View")
                        )
                    ),
                    Permission(
                        featureId = 87632091,
                        featureName = "Type_Activity",
                        accessLevels = listOf(
                            AccessLevel(accessId = 54328964, access = "View")
                        )
                    ),
                    Permission(
                        featureId = 19503967,
                        featureName = "Site",
                        accessLevels = listOf(
                            AccessLevel(accessId = 54328964, access = "View")
                        )
                    ),
                    Permission(
                        featureId = 98432186,
                        featureName = "Assign work",
                        accessLevels = listOf(
                            AccessLevel(accessId = 54328964, access = "View")
                        )
                    )
                ),
                team = null,
                circle = listOf(CircleData(code = "CH", id = 1L, name = "chandigarh")),
                designation = "Eng",
                reportingManager = ReportingManager(
                    id = 45608697,
                    name = "string"
                ),
                dateOfJoining = "2023-11-01",
                officialLocation = OfficialLocation(
                    latitude = 28.6139,
                    longitude = 77.209,
                )
            )
        )
        coEvery { authRepo.empDetails() } returns response

        viewModel.getEmpDetails()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { authRepo.empDetails() }
        assertNotNull(viewModel.empDetailsResponse.value)
        assertEquals(Status.SUCCESS, viewModel.empDetailsResponse.value?.status)
        assertEquals(response, viewModel.empDetailsResponse.value?.response)
    }
}
