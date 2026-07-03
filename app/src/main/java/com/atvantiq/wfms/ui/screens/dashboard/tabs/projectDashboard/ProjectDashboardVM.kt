package com.atvantiq.wfms.ui.screens.dashboard.tabs.projectDashboard

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.repository.budget.IBudgetRepo
import com.atvantiq.wfms.models.targets.MyProjectsResponse
import com.atvantiq.wfms.network.ApiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ProjectDashboardVM @Inject constructor(
    application: Application,
    private val budgetRepo: IBudgetRepo
) : BaseViewModel(application) {

    val myProjectsResponse = MutableLiveData<ApiState<MyProjectsResponse>>()

    var selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    var selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)

    init {
        fetchMyProjects()
    }

    fun fetchMyProjects() {
        val monthParam = "%04d-%02d".format(selectedYear, selectedMonth)
        executeApiCall(
            apiCall = { budgetRepo.myProjects(monthParam) },
            liveData = myProjectsResponse
        )
    }
}
