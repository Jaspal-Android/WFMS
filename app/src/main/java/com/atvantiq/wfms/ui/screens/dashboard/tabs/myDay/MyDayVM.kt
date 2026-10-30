package com.atvantiq.wfms.ui.screens.dashboard.tabs.myDay

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.repository.tracking.ITrackingRepo
import com.atvantiq.wfms.data.tracking.LocationEventQueue
import com.atvantiq.wfms.models.myDay.MyDayResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import javax.inject.Inject

/** What the My Day tab shows: the selected day, its content and the load state. */
data class MyDayState(
    /** 0 is today; up to [MyDayVM.MAX_DAYS_BACK]. */
    val daysBack: Int = 0,
    val content: MyDayUi? = null,
    val isLoading: Boolean = false,
    /** Server message (or the user-facing error) of the last failed load, shown as a banner. */
    val errorMessage: String? = null
) {
    val isToday: Boolean get() = daysBack == 0
    val canGoBack: Boolean get() = daysBack < MyDayVM.MAX_DAYS_BACK
    val canGoForward: Boolean get() = daysBack > 0
}

@HiltViewModel
class MyDayVM @Inject constructor(
    application: Application,
    private val trackingRepo: ITrackingRepo,
    private val locationEventQueue: LocationEventQueue
) : BaseViewModel(application) {

    val state = MutableLiveData(MyDayState())

    /** Raw responses, for the screen's session check (HTTP 401, or code 401 in the body). */
    val myDayResponse = MutableLiveData<ApiState<MyDayResponse>>()

    /** Clock for "today"; replaced in tests. */
    internal var now: () -> Long = { System.currentTimeMillis() }

    private val current get() = state.value ?: MyDayState()

    /** Loads the selected day. Reloads keep the old content on screen while they run. */
    fun load() {
        val daysBack = current.daysBack
        val isToday = daysBack == 0
        state.value = current.copy(isLoading = true, errorMessage = null)
        executeApiCall(
            apiCall = { trackingRepo.myDay(if (isToday) null else DateUtils.formatYmd(selectedDayMillis(daysBack))) },
            liveData = myDayResponse,
            onSuccess = { response ->
                state.value = if (response.code == ValConstants.SUCCESS_CODE) {
                    val queued = if (isToday) locationEventQueue.peekAll() else emptyList()
                    current.copy(content = MyDayContent.build(response.data, queued, isToday), isLoading = false)
                } else {
                    current.copy(isLoading = false, errorMessage = response.message)
                }
            },
            onError = { error -> state.value = current.copy(isLoading = false, errorMessage = error.message) },
            cancelPrevious = true
        )
    }

    fun previousDay() = moveTo(current.daysBack + 1)

    fun nextDay() = moveTo(current.daysBack - 1)

    /** Start of the shown day, for its heading. */
    fun selectedDayMillis(daysBack: Int = current.daysBack): Long =
        Calendar.getInstance().apply {
            timeInMillis = now()
            add(Calendar.DAY_OF_YEAR, -daysBack)
        }.timeInMillis

    private fun moveTo(daysBack: Int) {
        if (daysBack !in 0..MAX_DAYS_BACK || daysBack == current.daysBack) return
        state.value = current.copy(daysBack = daysBack)
        load()
    }

    companion object {
        /** The server answers 400 for days further back. */
        const val MAX_DAYS_BACK = 30
    }
}
