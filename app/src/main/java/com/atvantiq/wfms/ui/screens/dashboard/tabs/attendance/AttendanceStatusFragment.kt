package com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.constants.AttendanceStatus
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.FragmentAttendanceStatusBinding
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.models.calendar.AttendanceDay
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance.detail.AttendanceDetailActivity
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.utils.Utils
import com.atvantiq.wfms.widgets.CalendarView
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException
import java.util.Calendar


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
                if (day.date.isNotEmpty()) {
                    if (day.record != null) {
                        Utils.jumpActivityWithData(requireContext(),
                            AttendanceDetailActivity::class.java,
                            Bundle().apply {
                                putParcelable(SharingKeys.attendanceRecord, day.record)
                            }
                        )
                    } else {
                        showToast(requireContext(), getString(R.string.no_attendance_data))
                    }
                }
            }

            override fun onPrevMonthClickListener(calendar: Calendar?) {
                calendar?.let {
                    binding.customCalender.clearAttendanceData()
                    val month = it.get(Calendar.MONTH) + 1 // Months are indexed from 0
                    val year = it.get(Calendar.YEAR)
                    val totalDays = getTotalDaysInMonth(month, year)
                    binding.totalDaysText.text = totalDays.toString()
                    resetAttendanceSummary()
                    viewModel.getAttendanceDetails(month, year)
                }
            }

            override fun onNextMonthClickListener(calendar: Calendar?) {
                calendar?.let {
                    binding.customCalender.clearAttendanceData()
                    val month = it.get(Calendar.MONTH) + 1 // Months are indexed from 0
                    val year = it.get(Calendar.YEAR)
                    val totalDays = getTotalDaysInMonth(month, year) // Get total days in the month
                    binding.totalDaysText.text = totalDays.toString()
                    resetAttendanceSummary()
                    viewModel.getAttendanceDetails(month, year)
                }
            }

            override fun attendanceSummaryResult(statusCounts: Map<String, Int>, noApiDays: Int) {
                // Log or handle the results
                Log.d("CalendarView", "Status Counts: $statusCounts")
                Log.d("CalendarView", "Days without API data: ${noApiDays}")
                showAttendanceSummary(statusCounts, noApiDays)
            }
        })

        getAttendanceDetails()
    }

    private fun getTotalDaysInMonth(month: Int, year: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1) // Months are indexed from 0
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun getAttendanceDetails() {
        var time = DateUtils.getCurrentMonthAndYear()
        binding.totalDaysText.text = getTotalDaysInMonth(time.first, time.second).toString()
        viewModel.getAttendanceDetails(time.first, time.second)
    }

    override fun subscribeToEvents(vm: AttendanceStatusVM) {

        vm.attendanceDetailsResponse.observe(viewLifecycleOwner) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    binding.showCalendarProgressBar = false
                    when (response.response?.code) {
                        200 -> {
                            handleAttendanceDetailsResponse(response.response)
                        }

                        401 -> {
                            tokenExpiresAlert()
                        }

                        else -> {
                            alertDialogShow(
                                requireContext(),
                                getString(R.string.alert),
                                response.response?.message
                                    ?: getString(R.string.something_went_wrong)
                            )
                        }
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
        if (response.data != null) {
            if (!response.data.records.isNullOrEmpty()) {
                val attendanceDays = response.data.records.map { detail ->
                    AttendanceDay(
                        date = DateUtils.formatApiDateToYMD(detail.checkin.time).toString(),
                        status = when (detail.status) {
                            0 -> AttendanceStatus.NO_ACTION
                            1 -> AttendanceStatus.PRESENT
                            2 -> AttendanceStatus.ABSENT
                            3 -> AttendanceStatus.LEAVE
                            4 -> AttendanceStatus.IDLE
                            5 -> AttendanceStatus.HOLIDAY
                            6 -> AttendanceStatus.WORK_OFF
                            else -> AttendanceStatus.NO_ACTION
                        },
                        record = detail
                    )
                }
                // Calculate the count of each status
                //val statusCounts = attendanceDays.groupingBy { it.status }.eachCount()
                // showAttendanceSummary(statusCounts)
                binding.customCalender.setAttendanceData(attendanceDays)
            } else {
                resetAttendanceSummary() // Reset counts when no attendance data is found
                showToast(requireContext(), getString(R.string.no_attendance_data))
            }
        } else {
            resetAttendanceSummary() // Reset counts when no attendance data is found
            showToast(requireContext(), getString(R.string.no_attendance_data))
        }
    }

    // Method to reset attendance summary counts to 0
    private fun resetAttendanceSummary() {
        binding.presentText.text = "0"
        binding.absentText.text = "0"
        binding.leaveText.text = "0"
        binding.idleText.text = "0"
        binding.holidayText.text = "0"
        binding.workOffText.text = "0"
        binding.naText.text = "0"
    }

    private fun showAttendanceSummary(statusCounts: Map<String, Int>, noApiDays: Int) {
        binding.presentText.text = statusCounts[AttendanceStatus.PRESENT]?.toString() ?: "0"
        binding.absentText.text = statusCounts[AttendanceStatus.ABSENT]?.toString() ?: "0"
        binding.leaveText.text = statusCounts[AttendanceStatus.LEAVE]?.toString() ?: "0"
        binding.idleText.text = statusCounts[AttendanceStatus.IDLE]?.toString() ?: "0"
        binding.holidayText.text = statusCounts[AttendanceStatus.HOLIDAY]?.toString() ?: "0"
        binding.workOffText.text = statusCounts[AttendanceStatus.WORK_OFF]?.toString() ?: "0"
        binding.naText.text = noApiDays.toString()
    }
}
