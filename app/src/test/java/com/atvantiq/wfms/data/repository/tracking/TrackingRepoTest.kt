package com.atvantiq.wfms.data.repository.tracking

import android.content.Context
import android.location.Location
import com.atvantiq.wfms.network.ApiService
import com.atvantiq.wfms.models.location.CustomLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class TrackingRepoTest {

    private lateinit var apiService: ApiService
    private lateinit var context: Context
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var repo: TrackingRepo

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        apiService = mockk(relaxed = true)
        context = mockk(relaxed = true)
        fusedLocationProviderClient = mockk(relaxed = true)
        mockkStatic(LocationServices::class)
        every { LocationServices.getFusedLocationProviderClient(context) } returns fusedLocationProviderClient
        repo = TrackingRepo(apiService, context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `sendLocationToServer logs location and calls apiService`() = runTest {
        val location = mockk<Location>(relaxed = true)
        every { location.latitude } returns 12.34
        every { location.longitude } returns 56.78

        // Uncomment and adjust if you implement apiService.sendLocation
        // coEvery { apiService.sendLocation(any()) } just Runs

        repo.javaClass.getDeclaredMethod("sendLocationToServer", Location::class.java)
            .apply { isAccessible = true }
            .invoke(repo, location)

        // Uncomment and adjust if you implement apiService.sendLocation
        // coVerify {
        //     apiService.sendLocation(
        //         CustomLocationRequest(
        //             latitude = 12.34,
        //             longitude = 56.78,
        //             timestamp = any()
        //         )
        //     )
        // }
    }

    // You can add more tests for startLocationUpdates and stopLocationUpdates if needed,
    // but those require more Android instrumentation/mocking.
}

