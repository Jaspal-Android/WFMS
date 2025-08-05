package com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AttendanceCommunicationViewModel : ViewModel() {
    private val _refreshCalendar = MutableLiveData<Unit>()
    val refreshCalendar: LiveData<Unit> get() = _refreshCalendar

    fun triggerCalendarRefresh() {
        _refreshCalendar.value = Unit
    }
}