package com.atvantiq.wfms.ui.screens.reimbursement

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.data.repository.claims.IClaimRepo
import com.atvantiq.wfms.models.reimbursement.delete.DeleteClaimResponse
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.utils.NoInternetException
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ReimbursementViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var claimRepo: IClaimRepo
    private lateinit var viewModel: ReimbursementViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        claimRepo = mockk(relaxed = true)
        mockkObject(Utils)
        every { Utils.isInternet(application) } returns true
        coEvery { claimRepo.deleteClaim(any()) } returns
            DeleteClaimResponse(code = 200, message = "Claim deleted successfully.", success = true)
        viewModel = ReimbursementViewModel(application, claimRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `delete while offline reports no internet without calling the API`() = runTest {
        every { Utils.isInternet(application) } returns false

        viewModel.deleteClaim(42L)
        advanceUntilIdle()

        val state = viewModel.deleteClaimResponse.value!!
        assertEquals(Status.ERROR, state.status)
        assertTrue(state.throwable is NoInternetException)
        coVerify(exactly = 0) { claimRepo.deleteClaim(any()) }
    }

    @Test
    fun `delete works again after an offline attempt`() = runTest {
        every { Utils.isInternet(application) } returns false
        viewModel.deleteClaim(42L)
        advanceUntilIdle()

        every { Utils.isInternet(application) } returns true
        viewModel.deleteClaim(42L)
        advanceUntilIdle()

        coVerify(exactly = 1) { claimRepo.deleteClaim(42L) }
        assertEquals(Status.SUCCESS, viewModel.deleteClaimResponse.value!!.status)
    }

    @Test
    fun `a second tap while a delete is in flight is ignored`() = runTest {
        viewModel.deleteClaim(42L)
        viewModel.deleteClaim(42L)
        advanceUntilIdle()

        coVerify(exactly = 1) { claimRepo.deleteClaim(42L) }
    }
}
