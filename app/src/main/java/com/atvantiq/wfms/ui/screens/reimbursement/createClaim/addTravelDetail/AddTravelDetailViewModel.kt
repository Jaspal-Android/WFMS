package com.atvantiq.wfms.ui.screens.reimbursement.createClaim.addTravelDetail

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.repository.claims.IClaimRepo
import com.atvantiq.wfms.data.repository.creation.CreationRepo
import com.atvantiq.wfms.models.allProjects.Project
import com.atvantiq.wfms.models.empoyeeByCircle.Data
import com.atvantiq.wfms.models.empoyeeByCircle.EmployeeByCircleResponse
import com.atvantiq.wfms.models.reimbursement.TravelExpense
import com.atvantiq.wfms.models.reimbursement.TravelModeOption
import com.atvantiq.wfms.models.workSiteByDate.WorkSiteByDateResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.ui.screens.attendance.AttendanceClickEvents
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.CreateClaimClickEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddTravelDetailViewModel @Inject constructor(
    application: Application,
    private val claimRepo: IClaimRepo,
) : BaseViewModel(application) {

    val travelAmount = ObservableField<String>().apply { set("") }
    val fromLocation = ObservableField<String>().apply { set("") }
    val toLocation = ObservableField<String>().apply { set("") }
    val attachmentPath = ObservableField<String>().apply { set("") }
    var selectedTravelMode = ObservableField<TravelModeOption>().apply { set(null) }
    val selectedTravelModeValue = ObservableField<String>().apply { set("") }

    var selectedEmployee =  ObservableField<Data>().apply { set(null) }

    var employeesByCircle: List<Data> = ArrayList()

    val clickEvents = MutableLiveData<AddTravelingClickEvents>()

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

    fun createTravelDetail(): TravelExpense {
        return TravelExpense(
            mode = selectedTravelMode.get(),
            amount = travelAmount.get().toString(),
            travelingWith = if (selectedEmployee.get() != null) listOf(selectedEmployee.get()!!) else emptyList(),
            from = fromLocation.get().toString(),
            to = toLocation.get().toString(),
            receiptAttachments = if (!attachmentPath.get().isNullOrEmpty()) listOf(attachmentPath.get()!!) else emptyList()
        )
    }
}