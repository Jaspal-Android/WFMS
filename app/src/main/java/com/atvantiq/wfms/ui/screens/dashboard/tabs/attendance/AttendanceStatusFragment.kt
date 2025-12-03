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
import com.atvantiq.wfms.constants.ValConstants
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
        // No-op
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        communicationViewModel.refreshCalendar.observe(viewLifecycleOwner) {
            refreshCalendar()
        }
    }

    private fun refreshCalendar() {
        getAttendanceDetails()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        binding.customCalender.setCustomCalendarEventHandler(calendarEventHandler)
        getAttendanceDetails()
    }

    private val calendarEventHandler = object : CalendarView.CustomCalendarEventHandler {
        override fun onDayClickListener(position: Int, day: AttendanceDay) {
            if (day.date.isNotEmpty()) {
                day.record?.let {
                    Utils.jumpActivityWithData(requireContext(),
                        AttendanceDetailActivity::class.java,
                        Bundle().apply { putParcelable(SharingKeys.attendanceRecord, it) }
                    )
                } ?: showToast(requireContext(), getString(R.string.no_attendance_data))
            }
        }

        override fun onPrevMonthClickListener(calendar: Calendar?) {
            calendar?.let {
                updateCalendarForMonth(it)
            }
        }

        override fun onNextMonthClickListener(calendar: Calendar?) {
            calendar?.let {
                updateCalendarForMonth(it)
            }
        }

        override fun attendanceSummaryResult(statusCounts: Map<String, Int>, noApiDays: Int) {
            Log.d("CalendarView", "Status Counts: $statusCounts")
            Log.d("CalendarView", "Days without API data: $noApiDays")
            showAttendanceSummary(statusCounts, noApiDays)
        }
    }

    private fun updateCalendarForMonth(calendar: Calendar) = with(binding) {
        customCalender.clearAttendanceData()
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        totalDaysText.text = getTotalDaysInMonth(month, year).toString()
        resetAttendanceSummary()
        viewModel.getAttendanceDetails(month, year)
    }

    private fun getTotalDaysInMonth(month: Int, year: Int): Int =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)

    private fun getAttendanceDetails() {
        val (month, year) = DateUtils.getCurrentMonthAndYear()
        binding.totalDaysText.text = getTotalDaysInMonth(month, year).toString()
        viewModel.getAttendanceDetails(month, year)
    }

    override fun subscribeToEvents(vm: AttendanceStatusVM) {
        vm.attendanceDetailsResponse.observe(viewLifecycleOwner) { response ->
            binding.showCalendarProgressBar = response.status == Status.LOADING
            when (response.status) {
                Status.SUCCESS -> handleSuccessResponse(response.response)
                Status.ERROR -> handleErrorResponse(response.throwable)
                Status.LOADING -> Unit
            }
        }
    }

    private fun handleSuccessResponse(response: AttendanceDetailListResponse?) {
        when (response?.code) {
            ValConstants.SUCCESS_CODE -> handleAttendanceDetailsResponse(response)
            ValConstants.UNAUTHORIZED_CODE -> tokenExpiresAlert()
            else -> alertDialogShow(
                requireContext(),
                getString(R.string.alert),
                response?.message ?: getString(R.string.something_went_wrong)
            )
        }
    }

    private fun handleErrorResponse(throwable: Throwable?) {
        if (throwable is HttpException && throwable.code() == ValConstants.UNAUTHORIZED_CODE) {
            tokenExpiresAlert()
        } else {
            showToast(
                requireContext(),
                throwable?.message ?: getString(R.string.something_went_wrong)
            )
        }
    }

    private fun handleAttendanceDetailsResponse(response: AttendanceDetailListResponse) {
        val records = response.data?.records
        if (!records.isNullOrEmpty()) {
            val attendanceDays = records.map { detail ->
                AttendanceDay(
                    date = DateUtils.formatApiDateToYMD(detail.checkin.time).toString(),
                    status = mapStatus(detail.status),
                    record = detail
                )
            }
            binding.customCalender.setAttendanceData(attendanceDays)
        } else {
            resetAttendanceSummary()
            showToast(requireContext(), getString(R.string.no_attendance_data))
        }
    }

    private fun mapStatus(status: Int): String = when (status) {
        0 -> AttendanceStatus.NO_ACTION
        1 -> AttendanceStatus.PRESENT
        2 -> AttendanceStatus.ABSENT
        3 -> AttendanceStatus.LEAVE
        4 -> AttendanceStatus.IDLE
        5 -> AttendanceStatus.HOLIDAY
        6 -> AttendanceStatus.WORK_OFF
        else -> AttendanceStatus.NO_ACTION
    }

    private fun resetAttendanceSummary() = with(binding) {
        binding.presentText.text = "0"
        binding.absentText.text = "0"
        binding.leaveText.text = "0"
        binding.idleText.text = "0"
        binding.holidayText.text = "0"
        binding.workOffText.text = "0"
        binding.noActionText.text = "0"
        binding.naText.text = "0"
    }

    private fun showAttendanceSummary(statusCounts: Map<String, Int>, noApiDays: Int) = with(binding) {
        binding.presentText.text = statusCounts[AttendanceStatus.PRESENT]?.toString() ?: "0"
        binding.absentText.text = statusCounts[AttendanceStatus.ABSENT]?.toString() ?: "0"
        binding.leaveText.text = statusCounts[AttendanceStatus.LEAVE]?.toString() ?: "0"
        binding.idleText.text = statusCounts[AttendanceStatus.IDLE]?.toString() ?: "0"
        binding.holidayText.text = statusCounts[AttendanceStatus.HOLIDAY]?.toString() ?: "0"
        binding.workOffText.text = statusCounts[AttendanceStatus.WORK_OFF]?.toString() ?: "0"
        binding.noActionText.text = statusCounts[AttendanceStatus.NO_ACTION]?.toString() ?: "0"
        naText.text = noApiDays.toString()
    }
}

