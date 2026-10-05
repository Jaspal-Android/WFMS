package com.atvantiq.wfms.ui.screens.admin.ui.siteApproval

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.repository.atten.IAttendanceRepo
import com.atvantiq.wfms.models.workSites.workAssignments.WorkAssignment
import com.atvantiq.wfms.models.workSites.workAssignments.WorkAssignmentsResponse
import com.atvantiq.wfms.utils.MonthYear
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Work Approval list (`GET /work/all`): the work assignments in one month, paged. ◀ ▶ step the
 * month; the search runs on the keyboard's Search key. Either reloads from page 1.
 */
@HiltViewModel
class WorkApprovalVM @Inject constructor(
    application: Application,
    private val attendanceRepo: IAttendanceRepo
) : BaseViewModel(application) {

    /** The month on screen; opens on the current one. */
    val month = MutableLiveData(MonthYear.current())

    /** The search last run; the field's own text only applies once Search is pressed. */
    var appliedSearch: String = ""
        private set

    /** Assignments matching the month and search (`total_records`). */
    val assignmentCount = MutableLiveData<Int?>()

    /** Assignments with no site to open can't be reviewed and are dropped. */
    val assignments = PagedList<WorkAssignmentsResponse, WorkAssignment>(
        pageSize = ValConstants.APPROVAL_PAGE_SIZE,
        fetch = { page, pageSize ->
            val shown = month.value ?: MonthYear.current()
            attendanceRepo.workAssignments(
                page,
                pageSize,
                shown.firstDay,
                shown.lastDay,
                appliedSearch.ifEmpty { null }
            )
        },
        pageItems = { response ->
            if (response.code == ValConstants.SUCCESS_CODE) {
                assignmentCount.value = response.data?.totalRecords
                response.data?.records.orEmpty().filter { it.site?.workSiteId != null && it.employee?.id != null }
            } else {
                null
            }
        }
    )

    fun showPreviousMonth() = showMonth(month.value?.previous())

    fun showNextMonth() = showMonth(month.value?.next())

    private fun showMonth(target: MonthYear?) {
        month.value = target ?: return
        assignmentCount.value = null
        assignments.reload()
    }

    /** Runs [query] from page 1; the same query again only refreshes. */
    fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed == appliedSearch) {
            assignments.refresh()
            return
        }
        appliedSearch = trimmed
        assignmentCount.value = null
        assignments.reload()
    }
}
