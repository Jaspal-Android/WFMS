package com.atvantiq.wfms.ui.screens.reimbursement.createClaim

import android.app.Application
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.models.reimbursement.DAExpense
import com.atvantiq.wfms.models.reimbursement.HotelExpense
import com.atvantiq.wfms.models.reimbursement.MultipleSite
import com.atvantiq.wfms.models.reimbursement.OtherExpense
import com.atvantiq.wfms.models.reimbursement.TravelExpense

class CreateClaimVM(application: Application) : AndroidViewModel(application) {

    var remarks = MutableLiveData<String>().apply { value = "" }
    var date = ObservableField<String>().apply { set("") }
    var purpose = ObservableField<String>().apply { set("") }
    var selectedSingleSite = MutableLiveData<String>().apply { value = "" }
    var selectedMultiSiteList = MutableLiveData<List<MultipleSite>>().apply { value = emptyList() }
    var travelingEntriesList = MutableLiveData<List<TravelExpense>>().apply { value = emptyList() }
    var daEntriesList = MutableLiveData<List<DAExpense>>().apply { value = emptyList() }
    var hotelEntriesList = MutableLiveData<List<HotelExpense>>().apply { value = emptyList() }
    var othersEntriesList = MutableLiveData<List<OtherExpense>>().apply { value = emptyList() }

    var clickEvents = MutableLiveData<CreateClaimClickEvents>()

    private fun postClickEvent(event: CreateClaimClickEvents) {
        clickEvents.value = event
    }

    fun onDateClick() = postClickEvent(CreateClaimClickEvents.ON_DATE_PICKER_CLICK)

    fun onSingleSiteClick() = postClickEvent(CreateClaimClickEvents.ON_SINGLE_SITE_CLICK)

    fun onMultiSiteClick() = postClickEvent(CreateClaimClickEvents.ON_MULTI_SITE_CLICK)

    fun onLocalClaimClick() = postClickEvent(CreateClaimClickEvents.ON_LOCAL_CLAIM_CLICK)

    fun onOutstationClaimClick() = postClickEvent(CreateClaimClickEvents.ON_OUTSTATION_CLAIM_CLICK)

    fun onAddMultipleSiteClick() = postClickEvent(CreateClaimClickEvents.ON_ADD_MULTIPLE_SITE_CLICK)

    fun onSiteDropdownClick() = postClickEvent(CreateClaimClickEvents.ON_SINGLE_SITE_DROPDOWN_CLICK)

    fun onAddTravelingExpenseClick() = postClickEvent(CreateClaimClickEvents.ON_ADD_TRAVELING_EXPENSE_CLICK)

    fun onAddDailyAllowanceClick() = postClickEvent(CreateClaimClickEvents.ON_ADD_DAILY_ALLOWANCE_CLICK)

    fun onAddHotelExpenseClick() = postClickEvent(CreateClaimClickEvents.ON_ADD_HOTEL_EXPENSE_CLICK)

    fun onAddOthersExpenseClick() = postClickEvent(CreateClaimClickEvents.ON_ADD_OTHERS_EXPENSE_CLICK)

    fun onCancelClick() = postClickEvent(CreateClaimClickEvents.ON_CANCEL_CLICK)

    fun onPurposeClick() = postClickEvent(CreateClaimClickEvents.ON_PURPOSE_CLICK)

    /*Handling single site*/
    fun  onSingleSiteSelected(siteId:String){
        selectedSingleSite.value = siteId
    }

    /*Handling multiple site*/
    fun addMultipleSite(site: MultipleSite) {
        val currentList = selectedMultiSiteList.value?.toMutableList() ?: mutableListOf()
        currentList.add(site)
        selectedMultiSiteList.value = currentList
    }
    /*Removing item from multiple site*/
    fun removeMultipleSite(site: MultipleSite) {
        val currentList = selectedMultiSiteList.value?.toMutableList() ?: mutableListOf()
        val iterator = currentList.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.siteId == site.siteId) {
                iterator.remove()
                break
            }
        }
        selectedMultiSiteList.value = currentList
    }

    fun clearSelectedMultiSites() {
        selectedMultiSiteList.value = emptyList()
    }

    fun clearSelectedSingleSite() {
        selectedSingleSite.value = ""
    }

    /* Add Travel Expense*/
    fun addTravelExpense(expense: TravelExpense) {
        val currentList = travelingEntriesList.value?.toMutableList() ?: mutableListOf()
        currentList.add(expense)
        travelingEntriesList.value = currentList
    }

    /*Remove item from travel expense*/
    fun removeTravelExpense(index: Int) {
        val currentList = travelingEntriesList.value?.toMutableList() ?: mutableListOf()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            travelingEntriesList.value = currentList
        }
    }

    /* Add DA Expense */
    fun addDAExpense(expense: DAExpense) {
        val currentList = daEntriesList.value?.toMutableList() ?: mutableListOf()
        currentList.add(expense)
        daEntriesList.value = currentList
    }

    /* remove item from DA Expense*/
    fun removeDAExpense(index: Int) {
        val currentList = daEntriesList.value?.toMutableList() ?: mutableListOf()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            daEntriesList.value = currentList
        }
    }

    /* Add Hotel Expense*/
    fun addHotelExpense(expense: HotelExpense) {
        val currentList = hotelEntriesList.value?.toMutableList() ?: mutableListOf()
        currentList.add(expense)
        hotelEntriesList.value = currentList
    }
    /*Remove item from hotel list*/
    fun removeHotelExpense(index: Int) {
        val currentList = hotelEntriesList.value?.toMutableList() ?: mutableListOf()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            hotelEntriesList.value = currentList
        }
    }

    /*Add other expense*/
    fun addOtherExpense(expense: OtherExpense) {
        val currentList = othersEntriesList.value?.toMutableList() ?: mutableListOf()
        currentList.add(expense)
        othersEntriesList.value = currentList
    }

    /*Remove item from the Other expense*/
    fun removeOtherExpense(index: Int) {
        val currentList = othersEntriesList.value?.toMutableList() ?: mutableListOf()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            othersEntriesList.value = currentList
        }
    }

}
