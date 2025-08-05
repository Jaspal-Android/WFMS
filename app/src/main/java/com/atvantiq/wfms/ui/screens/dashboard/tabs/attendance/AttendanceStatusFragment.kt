package com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.FragmentAttendanceStatusBinding
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.models.calendar.AttendanceDay
import com.atvantiq.wfms.models.calendar.AttendanceStatus
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.CalendarAdapter
import com.atvantiq.wfms.ui.screens.dashboard.DashboardViewModel
import com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance.detail.AttendanceDetailActivity
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.utils.Utils
import com.atvantiq.wfms.widgets.CalendarView
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


/**
 * A simple [Fragment] subclass.
 * Use the [AttendanceStatusFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

@AndroidEntryPoint
class AttendanceStatusFragment :
    BaseFragment<FragmentAttendanceStatusBinding, AttendanceStatusVM>() {

    private val communicationViewModel: AttendanceCommunicationViewModel by activityViewModels()


    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_attendance_status, AttendanceStatusVM::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        communicationViewModel.refreshCalendar.observe(viewLifecycleOwner) {
            refreshCalendar() // Your function to update calendar
        }
    }

    private fun refreshCalendar() {
        // Reload your calendar UI
        getAttendanceDetails()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        binding.customCalender.setCustomCalendarEventHandler(object :
            CalendarView.CustomCalendarEventHandler {
            override fun onDayClickListener(position: Int, day: AttendanceDay) {
                if (day.date.isNotEmpty() ) {
                    if(day.record != null){
                        Utils.jumpActivityWithData(requireContext(),
                            AttendanceDetailActivity::class.java,
                            Bundle().apply {
                                putParcelable(SharingKeys.attendanceRecord, day.record)
                            }
                        )
                    }else {
                        showToast(requireContext(), getString(R.string.no_attendance_data))
                    }
                }
            }

            override fun onPrevMonthClickListener(calendar: Calendar?) {
                calendar?.let {
                    binding.customCalender.clearAttendanceData()
                    val month = it.get(Calendar.MONTH) + 1 // Months are indexed from 0
                    val year =  it.get(Calendar.YEAR)
                    viewModel.getAttendanceDetails(month,year)
                }
            }

            override fun onNextMonthClickListener(calendar: Calendar?) {
                calendar?.let {
                    binding.customCalender.clearAttendanceData()
                    val month = it.get(Calendar.MONTH) + 1 // Months are indexed from 0
                    val year =  it.get(Calendar.YEAR)
                    viewModel.getAttendanceDetails(month,year)
                }
            }
        })

        getAttendanceDetails()
    }

    private fun getAttendanceDetails(){
        var time = DateUtils.getCurrentMonthAndYear()
        viewModel.getAttendanceDetails(time.first,time.second)
    }

    override fun subscribeToEvents(vm: AttendanceStatusVM) {

        vm.attendanceDetailsResponse.observe(viewLifecycleOwner) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    binding.showCalendarProgressBar = false
                    if (response.response?.code == 200) {
                        handleAttendanceDetailsResponse(response.response)
                    } else if (response.response?.code == 401) {
                        tokenExpiresAlert()
                    } else {
                        alertDialogShow(
                            requireContext(),
                            getString(R.string.alert),
                            response.response?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.ERROR -> {
                    binding.showCalendarProgressBar = false
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == 401) {
                            tokenExpiresAlert()
                        }
                    } else {
                        showToast(
                            requireContext(),
                            response.throwable?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    binding.showCalendarProgressBar = true
                }
            }
        }
    }

    private fun handleAttendanceDetailsResponse(response: AttendanceDetailListResponse) {
        if (response.data!=null) {
            if(!response.data.records.isNullOrEmpty()){
                val attendanceDays = response.data.records.map { detail ->
                    AttendanceDay(
                        date = DateUtils.formatApiDateToYMD(
                            detail.checkin.time
                        ).toString(),
                        status = AttendanceStatus.PRESENT
                        /*status = when (detail.status) {
                            "Present" -> AttendanceStatus.PRESENT
                            "Absent" -> AttendanceStatus.ABSENT
                            else -> AttendanceStatus.UNKNOWN
                        }*/
                       ,record = detail
                    )
                }
                binding.customCalender.setAttendanceData(attendanceDays)
            }
            else {
                showToast(requireContext(), getString(R.string.no_attendance_data))
            }
        } else {
            showToast(requireContext(), getString(R.string.no_attendance_data))
        }
    }

}