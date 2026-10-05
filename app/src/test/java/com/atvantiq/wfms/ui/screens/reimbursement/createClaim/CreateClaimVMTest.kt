package com.atvantiq.wfms.ui.screens.reimbursement.createClaim

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.atvantiq.wfms.data.repository.creation.CreationRepo
import com.atvantiq.wfms.data.repository.claims.IClaimRepo
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CreateClaimVMTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: CreateClaimVM

    @Before
    fun setUp() {
        viewModel = CreateClaimVM(
            mockk<Application>(relaxed = true),
            mockk<IClaimRepo>(relaxed = true),
            mockk<CreationRepo>(relaxed = true),
            SavedStateHandle()
        )
    }

    @Test
    fun `a created claim is saved only on 201`() {
        assertTrue(viewModel.isClaimSaved(201))
        assertFalse(viewModel.isClaimSaved(200))
        assertFalse(viewModel.isClaimSaved(400))
        assertFalse(viewModel.isClaimSaved(null))
    }

    @Test
    fun `an edited claim is saved on 200 or 201`() {
        viewModel.isEditMode.set(true)

        assertTrue(viewModel.isClaimSaved(200))
        assertTrue(viewModel.isClaimSaved(201))
        assertFalse(viewModel.isClaimSaved(400))
    }

    @Test
    fun `submit stays locked after the claim is saved so a dismissed dialog cannot resend it`() {
        viewModel.isSubmitting.set(true)

        viewModel.onSubmitResponse(201)

        assertEquals(true, viewModel.isSubmitting.get())
    }

    @Test
    fun `submit is released when the server rejects the claim`() {
        viewModel.isSubmitting.set(true)

        viewModel.onSubmitResponse(400)

        assertEquals(false, viewModel.isSubmitting.get())
    }

    @Test
    fun `submit is released when the response has no code`() {
        viewModel.isSubmitting.set(true)

        viewModel.onSubmitResponse(null)

        assertEquals(false, viewModel.isSubmitting.get())
    }
}
