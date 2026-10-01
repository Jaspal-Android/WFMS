package com.atvantiq.wfms.ui.screens.admin.ui.siteApproval

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.base.PagedListUiState
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.ActivityWorkSitesApprovalBinding
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord
import com.atvantiq.wfms.models.attendance.attendanceDetails.day
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.WorkSubmissionsAdapter
import com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.workSites.WorkSitesActivity
import com.atvantiq.wfms.widgets.PaginationScrollListener
import dagger.hilt.android.AndroidEntryPoint

/** Work Approval: the month's employee-days, each opening that day's Work Sites. */
@AndroidEntryPoint
class WorkSitesApprovalActivity : BaseActivity<ActivityWorkSitesApprovalBinding, SiteApprovalVM>() {

    private var adapter: WorkSubmissionsAdapter? = null
    private var isProgressShown = false

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_work_sites_approval, SiteApprovalVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpToolbar()
        setUpList()
        binding.swipeRefreshLayout.setOnRefreshListener { viewModel.records.refresh() }
        viewModel.records.open()
    }

    private fun setUpToolbar() {
        binding.siteApprovalToolbar.toolbarTitle.text = getString(R.string.work_approval)
        binding.siteApprovalToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: SiteApprovalVM) {
        binding.vm = vm
        vm.month.observe(this) { month -> binding.monthTitle = month.label }
        vm.monthCount.observe(this) { count ->
            binding.monthSubtitle = count?.let { resources.getQuantityString(R.plurals.submissions_count, it, it) }
        }
        vm.records.state.observe(this) { state -> renderSubmissions(state) }
        vm.records.failure.observe(this) { failure ->
            if (!failure.consumeOnce()) return@observe
            handleFailure(failure)
        }
    }

    private fun setUpList() {
        adapter = WorkSubmissionsAdapter(onReview = ::openWorkSites)
        binding.rvSubmissions.adapter = adapter
        binding.rvSubmissions.addOnScrollListener(PaginationScrollListener { viewModel.records.loadNextPage() })
    }

    private fun renderSubmissions(state: PagedListUiState<AttendanceRecord>) {
        showFirstPageProgress(state.isLoadingFirstPage)
        if (!state.isLoadingFirstPage && !state.isRefreshing) binding.swipeRefreshLayout.isRefreshing = false
        adapter?.submitList(state.items)
        adapter?.showLoadingFooter(state.isLoadingMore)
        binding.emptyState.root.isVisible = state.isEmpty
    }

    private fun showFirstPageProgress(show: Boolean) {
        if (show == isProgressShown) return
        isProgressShown = show
        if (show) showProgress() else dismissProgress()
    }

    private fun handleFailure(failure: ApiState<AttendanceDetailListResponse>) {
        if (failure.status == Status.SUCCESS) {
            failure.response?.let { handleRejectedResponse(it.code, it.message) }
        } else {
            handleApiFailure(failure.throwable)
        }
    }

    private fun openWorkSites(record: AttendanceRecord) {
        val intent = Intent(this, WorkSitesActivity::class.java).apply {
            putExtra(SharingKeys.EMPLOYEE_ID, record.employee?.id.toString())
            putExtra(SharingKeys.DATE, record.day.orEmpty())
        }
        workSitesLauncher.launch(intent)
    }

    /** Work was approved or rejected on that day: its status comes back from the server. */
    private val workSitesLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) viewModel.records.refresh()
        }
}
