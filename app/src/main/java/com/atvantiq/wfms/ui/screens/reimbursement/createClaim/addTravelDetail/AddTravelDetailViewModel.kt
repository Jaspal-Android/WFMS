package com.atvantiq.wfms.ui.screens.reimbursement.createClaim.addTravelDetail

import android.app.Application
import com.atvantiq.wfms.base.LiveEvent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.base.savedField
import com.atvantiq.wfms.data.repository.claims.IClaimRepo
import com.atvantiq.wfms.models.empoyeeByCircle.Data
import com.atvantiq.wfms.models.empoyeeByCircle.EmployeeByCircleResponse
import com.atvantiq.wfms.models.reimbursement.TravelExpense
import com.atvantiq.wfms.models.reimbursement.TravelModeOption
import com.atvantiq.wfms.network.ApiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddTravelDetailViewModel @Inject constructor(
    application: Application,
    private val claimRepo: IClaimRepo,
    state: SavedStateHandle
) : BaseViewModel(application) {

    // Kept in the SavedStateHandle, so the entry survives Android ending the app's process while
    // the user is in the camera or another app.
    val travelAmount = state.savedField("amount", "")
    val fromLocation = state.savedField("from", "")
    val toLocation = state.savedField("to", "")
    val attachmentPath = state.savedField("receipt", "")
    val selectedTravelMode = state.savedField<TravelModeOption?>("mode", null)
    val selectedTravelModeValue = state.savedField("modeLabel", "")

    val selectedEmployee = state.savedField<Data?>("travelingWith", null)

    var employeesByCircle: List<Data> = ArrayList()

    val clickEvents = LiveEvent<AddTravelingClickEvents>()
    val errorHandler = LiveEvent<AddTravelDetailsErrorHandler>()

    private fun postClickEvent(event: AddTravelingClickEvents) {
        clickEvents.value = event
    }

    fun onTravelModeClick() {
        postClickEvent(AddTravelingClickEvents.SELECT_TRAVEL_MODE)
    }

    fun onTravelingWithClick() {
        postClickEvent(AddTravelingClickEvents.SELECT_TRAVELING_WITH)
    }

    fun onAttachmentClick() {
        postClickEvent(AddTravelingClickEvents.ATTACHMENT_CLICK)
    }

    fun onDoneClick() {
        postClickEvent(AddTravelingClickEvents.ON_DONE_CLICK)
    }

    fun onCancelClick() {
        postClickEvent(AddTravelingClickEvents.ON_CANCEL_CLICK)
    }

    /*Employee by circle API Call*/
    var employeeByCircleResponse = MutableLiveData<ApiState<EmployeeByCircleResponse>>()
    fun employeeByCircle(circleCode: String) {
        executeApiCall(
            apiCall = { claimRepo.employeeByCircle(circleCode) },
            liveData = employeeByCircleResponse,
        )
    }

    fun validateTravelDetailOrPostError(): Boolean {
        val mode = selectedTravelMode.get()
        val amount = travelAmount.get().orEmpty().trim()
        val from = fromLocation.get().orEmpty().trim()
        val to = toLocation.get().orEmpty().trim()
        val parsedAmount = amount.toDoubleOrNull()

        if (mode == null) {
            errorHandler.value = AddTravelDetailsErrorHandler.ON_EMPTY_TRAVEL_MODE
            return false
        }
        if (amount.isEmpty() || parsedAmount == null || parsedAmount <= 0.0) {
            errorHandler.value = AddTravelDetailsErrorHandler.ON_EMPTY_TRAVEL_AMOUNT
            return false
        }
        if (from.isEmpty()) {
            errorHandler.value = AddTravelDetailsErrorHandler.ON_EMPTY_FROM_LOCATION
            return false
        }
        if (to.isEmpty()) {
            errorHandler.value = AddTravelDetailsErrorHandler.ON_EMPTY_TO_LOCATION
            return false
        }
        return true
    }


    fun createTravelDetail(): TravelExpense {
        return TravelExpense(
            mode = selectedTravelMode.get(),
            amount = travelAmount.get().orEmpty().trim(),
            travelingWith = if (selectedEmployee.get() != null) listOf(selectedEmployee.get()!!) else emptyList(),
            from = fromLocation.get().orEmpty().trim(),
            to = toLocation.get().orEmpty().trim(),
            receiptAttachments = if (!attachmentPath.get().isNullOrEmpty()) listOf(attachmentPath.get()!!) else emptyList()
        )
    }
}
