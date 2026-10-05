package com.atvantiq.wfms.ui.screens.admin.ui.attendanceApproval

import android.view.View
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.base.PagedListUiState
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.ActivityAttendanceApprovalBinding
import com.atvantiq.wfms.models.attendance.approve.AttendanceApproveResponse
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.AttendanceApprovalAdapter
import com.atvantiq.wfms.widgets.PaginationScrollListener
import dagger.hilt.android.AndroidEntryPoint
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

/**
 * Attendance Approval (spec 6.1): the month's records. "Mark attendance" opens the decision sheet,
 * Details the review screen; a decision updates its row in place.
 */
@AndroidEntryPoint
class AttendanceApprovalActivity : BaseActivity<ActivityAttendanceApprovalBinding, AttendanceApprovalVM>() {

    private var adapter: AttendanceApprovalAdapter? = null
    private var isProgressShown = false

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_attendance_approval, AttendanceApprovalVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        binding.toolbar.toolbarTitle.text = getString(R.string.attendance_approval)
        binding.toolbar.toolbarBackButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        setUpList()
        binding.swipeRefreshLayout.setOnRefreshListener { viewModel.records.refresh() }
        viewModel.records.open()
    }

    override fun subscribeToEvents(vm: AttendanceApprovalVM) {
        binding.vm = vm
        vm.month.observe(this) { month -> binding.monthTitle = month.label }
        vm.monthCount.observe(this) { count ->
            binding.monthSubtitle = count?.let { resources.getQuantityString(R.plurals.attendance_entries_count, it, it) }
        }
        vm.records.state.observe(this) { state -> renderRecords(state) }
        vm.records.failure.observe(this) { failure ->
            if (!failure.consumeOnce()) return@observe
            handleListFailure(failure)
        }
        vm.approveResponse.observe(this) { response ->
            if (!response.consumeOnce()) return@observe
            handleDecisionResult(response)
        }
    }

    private fun setUpList() {
        adapter = AttendanceApprovalAdapter(
            canMark = viewModel::canMark,
            onMark = ::openDecision,
            onDetails = ::openReview
        )
        binding.rvRecords.adapter = adapter
        binding.rvRecords.addOnScrollListener(PaginationScrollListener { viewModel.records.loadNextPage() })
    }

    private fun renderRecords(state: PagedListUiState<AttendanceRecord>) {
        showFirstPageProgress(state.isLoadingFirstPage)
        if (!state.isLoadingFirstPage && !state.isRefreshing) binding.swipeRefreshLayout.isRefreshing = false
        adapter?.submitList(state.items, state.changedPosition)
        adapter?.showLoadingFooter(state.isLoadingMore)
        binding.emptyState.root.isVisible = state.isEmpty
    }

    private fun showFirstPageProgress(show: Boolean) {
        if (show == isProgressShown) return
        isProgressShown = show
        if (show) showProgress() else dismissProgress()
    }

    private fun handleListFailure(failure: ApiState<AttendanceDetailListResponse>) {
        if (failure.status == Status.SUCCESS) {
            failure.response?.let { handleRejectedResponse(it.code, it.message) }
        } else {
            handleApiFailure(failure.throwable)
        }
    }

    private fun handleDecisionResult(response: ApiState<AttendanceApproveResponse>) {
        when (response.status) {
            Status.LOADING -> showProgress()
            Status.SUCCESS -> {
                dismissProgress()
                val answer = response.response
                if (answer?.success == true) {
                    showToast(this, answer.message ?: getString(R.string.attendance_decision_submitted))
                    (supportFragmentManager.findFragmentByTag(AttendanceDecisionBottomSheet.TAG) as? DialogFragment)?.dismiss()
                } else {
                    handleRejectedResponse(answer?.code, answer?.message)
                }
            }
            Status.ERROR -> {
                dismissProgress()
                handleApiFailure(response.throwable)
            }
        }
    }

    private fun openDecision(record: AttendanceRecord) {
        if (supportFragmentManager.findFragmentByTag(AttendanceDecisionBottomSheet.TAG) != null) return
        viewModel.startDecision(record)
        AttendanceDecisionBottomSheet().show(supportFragmentManager, AttendanceDecisionBottomSheet.TAG)
    }

    private fun openReview(record: AttendanceRecord) {
        reviewLauncher.launch(Intent(this, AttendanceReviewActivity::class.java).apply {
            putExtra(SharingKeys.attendanceRecord, record)
        })
    }

    /** A decision made on the review screen updates its row here too. */
    private val reviewLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data ?: return@registerForActivityResult
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val attendanceId = data.getLongExtra(SharingKeys.ATTENDANCE_ID, NO_ID)
            val status = data.getIntExtra(SharingKeys.ATTENDANCE_STATUS, NO_ID.toInt())
            if (attendanceId != NO_ID) viewModel.applyDecision(attendanceId, status)
        }

    private companion object {
        const val NO_ID = -1L
    }
}
