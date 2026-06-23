package com.atvantiq.wfms.ui.screens.dashboard.tabs.myTargets

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.repository.budget.IBudgetRepo
import com.atvantiq.wfms.models.targets.MyTargetsResponse
import com.atvantiq.wfms.network.ApiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class MyTargetsVM @Inject constructor(
    application: Application,
    private val budgetRepo: IBudgetRepo
) : BaseViewModel(application) {

    val myTargetsResponse = MutableLiveData<ApiState<MyTargetsResponse>>()

    // Tracks the currently selected month/year for the picker UI
    var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)

    init {
        fetchMyTargets()
    }

    fun fetchMyTargets() {
        val monthParam = "%04d-%02d".format(selectedYear, selectedMonth)
        executeApiCall(
            apiCall = { budgetRepo.myTargets(monthParam) },
            liveData = myTargetsResponse
        )
    }
}
