package com.atvantiq.wfms.ui.screens.reimbursement.createClaim.addTravelDetail

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.atvantiq.wfms.data.repository.claims.IClaimRepo
import com.atvantiq.wfms.models.empoyeeByCircle.Data
import com.atvantiq.wfms.models.reimbursement.TravelModeOption
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** A travel entry keeps what was typed and picked when Android ends the process while the user is in the camera. */
class AddTravelDetailRestoreTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val application = mockk<Application>(relaxed = true)
    private val claimRepo = mockk<IClaimRepo>(relaxed = true)

    private val bus = TravelModeOption("Bus", "bus")
    private val jaspal = Data(code = "ATQ/PB/1289", email = null, id = 7L, name = "Jaspal")

    private fun viewModel(handle: SavedStateHandle) = AddTravelDetailViewModel(application, claimRepo, handle)

    @Test
    fun `a recreated entry starts from what was saved`() {
        val handle = SavedStateHandle()
        with(viewModel(handle)) {
            selectedTravelMode.set(bus)
            selectedTravelModeValue.set(bus.label)
            travelAmount.set("250")
            selectedEmployee.set(jaspal)
            fromLocation.set("Sector 18")
            toLocation.set("Mohali")
            attachmentPath.set("/cache/receipt.jpg")
        }

        val restored = viewModel(handle)

        assertEquals(bus, restored.selectedTravelMode.get())
        assertEquals("Bus", restored.selectedTravelModeValue.get())
        assertEquals("250", restored.travelAmount.get())
        assertEquals(jaspal, restored.selectedEmployee.get())
        assertEquals("Sector 18", restored.fromLocation.get())
        assertEquals("Mohali", restored.toLocation.get())
        assertEquals("/cache/receipt.jpg", restored.attachmentPath.get())
    }

    @Test
    fun `the restored entry is what gets submitted`() {
        val handle = SavedStateHandle()
        with(viewModel(handle)) {
            selectedTravelMode.set(bus)
            travelAmount.set("250")
            selectedEmployee.set(jaspal)
            fromLocation.set("Sector 18")
            toLocation.set("Mohali")
            attachmentPath.set("/cache/receipt.jpg")
        }

        val restored = viewModel(handle)

        assertEquals(true, restored.validateTravelDetailOrPostError())
        val entry = restored.createTravelDetail()
        assertEquals(bus, entry.mode)
        assertEquals("250", entry.amount)
        assertEquals(listOf(jaspal), entry.travelingWith)
        assertEquals(listOf("/cache/receipt.jpg"), entry.receiptAttachments)
    }

    @Test
    fun `a new entry starts empty`() {
        val vm = viewModel(SavedStateHandle())

        assertNull(vm.selectedTravelMode.get())
        assertNull(vm.selectedEmployee.get())
        assertEquals("", vm.travelAmount.get())
        assertEquals("", vm.attachmentPath.get())
    }
}
