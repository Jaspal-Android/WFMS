package com.atvantiq.wfms.ui.screens.admin.ui.approvals

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord
import com.atvantiq.wfms.utils.MonthYear

/**
 * One month of attendance records for review, paged (`GET /attendance/details`). ◀ ▶ step the
 * month and reload from page 1.
 */
abstract class MonthlyAttendanceListVM(
    application: Application,
    protected val attendanceRepo: IAttendanceRepo
) : BaseViewModel(application) {

    /** The month on screen; opens on the current one. */
    val month = MutableLiveData(MonthYear.current())

    /** Records in [month] (`total_records`, which is per month; `total_count` covers all months). */
    val monthCount = MutableLiveData<Int?>()

    /** The month's records. Records with no employee can't be reviewed and are dropped. */
    val records = PagedList<AttendanceDetailListResponse, AttendanceRecord>(
        pageSize = ValConstants.APPROVAL_PAGE_SIZE,
        fetch = { page, pageSize ->
            val shown = month.value ?: MonthYear.current()
            attendanceRepo.attendanceForApproval(page, pageSize, shown.month, shown.year)
        },
        pageItems = { response ->
            if (response.code == ValConstants.SUCCESS_CODE) {
                monthCount.value = response.data?.totalRecords
                response.data?.records.orEmpty().filter { it.employee?.id != null }
            } else {
                null
            }
        }
    )

    fun showPreviousMonth() = showMonth(month.value?.previous())

    fun showNextMonth() = showMonth(month.value?.next())

    private fun showMonth(target: MonthYear?) {
        month.value = target ?: return
        monthCount.value = null
        records.reload()
    }
}
