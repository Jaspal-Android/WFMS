package com.atvantiq.wfms.ui.screens.reimbursement.createClaim.addTravelDetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.atvantiq.wfms.models.reimbursement.TravelExpense
import com.atvantiq.wfms.ui.screens.attendance.AttendanceClickEvents
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.CreateClaimClickEvents

class AddTravelDetailViewModel(application: Application) : AndroidViewModel(application) {

    val travelMode = MutableLiveData<String>().apply { value = "" }
    val travelAmount = MutableLiveData<String>().apply { value = "" }
    val travelingWith= MutableLiveData<String>().apply { value = "" }
    val fromLocation = MutableLiveData<String>().apply { value = "" }
    val toLocation = MutableLiveData<String>().apply { value = "" }
    val attachmentPath = MutableLiveData<String>().apply { value = "" }

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


    fun createTravelDetail(): TravelExpense {
       /* return TravelExpense(
            mode = travelMode.value ?: "",
            amount = travelAmount.value ?: "",
            travelingWith = travelingWith.value ?: "",
            from = fromLocation.value ?: "",
            to = toLocation.value ?: "",
            receiptAttachment = attachmentPath.value ?: ""
        )*/

        return TravelExpense(
            mode = "Bus",
            amount = "200",
            travelingWith = "Rahul",
            from = "Chandigarh",
            to = "Mohali",
            receiptAttachment = attachmentPath.value ?: ""
        )
    }
}