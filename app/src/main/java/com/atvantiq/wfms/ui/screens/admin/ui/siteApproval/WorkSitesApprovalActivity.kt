package com.atvantiq.wfms.ui.screens.admin.ui.siteApproval

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.base.PagedListUiState
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.ActivityWorkSitesApprovalBinding
import com.atvantiq.wfms.models.workSites.workAssignments.WorkAssignment
import com.atvantiq.wfms.models.workSites.workAssignments.WorkAssignmentsResponse
import com.atvantiq.wfms.models.workSites.workAssignments.workDate
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.WorkAssignmentsAdapter
import com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.workSiteDetail.SiteWorkDetailActivity
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding
import com.atvantiq.wfms.widgets.PaginationScrollListener
import dagger.hilt.android.AndroidEntryPoint

/** Work Approval: the month's work assignments, each opening its Site Work Detail. */
@AndroidEntryPoint
class WorkSitesApprovalActivity : BaseActivity<ActivityWorkSitesApprovalBinding, WorkApprovalVM>() {

    private var adapter: WorkAssignmentsAdapter? = null
    private var isProgressShown = false

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_work_sites_approval, WorkApprovalVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        setUpToolbar()
        setUpSearch()
        setUpList()
        binding.swipeRefreshLayout.setOnRefreshListener { viewModel.assignments.refresh() }
        binding.errorState.onRetry = View.OnClickListener { viewModel.assignments.refresh() }
        viewModel.assignments.open()
    }

    private fun setUpToolbar() {
        binding.siteApprovalToolbar.toolbarTitle.text = getString(R.string.work_approval)
        binding.siteApprovalToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setUpSearch() {
        with(binding.searchBar) {
            etSearch.setText(viewModel.appliedSearch)
            ivClearSearch.isVisible = viewModel.appliedSearch.isNotEmpty()
            etSearch.doAfterTextChanged { ivClearSearch.isVisible = !it.isNullOrEmpty() }
            etSearch.setOnEditorActionListener { view, actionId, _ ->
                if (actionId != EditorInfo.IME_ACTION_SEARCH) return@setOnEditorActionListener false
                hideSoftKeyboard(this@WorkSitesApprovalActivity)
                viewModel.search(view.text.toString())
                true
            }
            ivClearSearch.setOnClickListener {
                etSearch.setText("")
                viewModel.search("")
            }
        }
    }

    override fun subscribeToEvents(vm: WorkApprovalVM) {
        binding.vm = vm
        vm.month.observe(this) { month -> binding.monthTitle = month.label }
        vm.assignmentCount.observe(this) { count ->
            binding.monthSubtitle = count?.let { resources.getQuantityString(R.plurals.work_assignments_count, it, it) }
        }
        vm.assignments.state.observe(this) { state -> renderAssignments(state) }
        vm.assignments.failure.observe(this) { failure ->
            if (!failure.consumeOnce()) return@observe
            handleFailure(failure)
        }
    }

    private fun setUpList() {
        adapter = WorkAssignmentsAdapter(onReview = ::openSiteWorkDetail)
        binding.rvAssignments.adapter = adapter
        binding.rvAssignments.addOnScrollListener(PaginationScrollListener { viewModel.assignments.loadNextPage() })
    }

    private fun renderAssignments(state: PagedListUiState<WorkAssignment>) {
        showFirstPageProgress(state.isLoadingFirstPage)
        if (!state.isLoadingFirstPage && !state.isRefreshing) binding.swipeRefreshLayout.isRefreshing = false
        adapter?.submitList(state.items, state.changedPosition)
        adapter?.showLoadingFooter(state.isLoadingMore)
        binding.emptyState.root.isVisible = state.isEmpty
        binding.errorState.root.isVisible = state.isFirstPageFailed
    }

    private fun showFirstPageProgress(show: Boolean) {
        if (show == isProgressShown) return
        isProgressShown = show
        if (show) showProgress() else dismissProgress()
    }

    private fun handleFailure(failure: ApiState<WorkAssignmentsResponse>) {
        if (failure.status == Status.SUCCESS) {
            failure.response?.let { handleRejectedResponse(it.code, it.message) }
        } else {
            handleApiFailure(failure.throwable)
        }
    }

    private fun openSiteWorkDetail(assignment: WorkAssignment) {
        val workSiteId = assignment.site?.workSiteId
        val employeeId = assignment.employee?.id
        val date = assignment.workDate
        if (workSiteId == null || employeeId == null || date == null) {
            return showToast(this, getString(R.string.work_progress_unavailable))
        }
        detailLauncher.launch(Intent(this, SiteWorkDetailActivity::class.java).apply {
            putExtra(SharingKeys.WORK_ID, workSiteId)
            putExtra(SharingKeys.EMPLOYEE_ID, employeeId.toString())
            putExtra(SharingKeys.WORK_DATE, date)
        })
    }

    /** Work was approved or rejected there: its status comes back from the server. */
    private val detailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) viewModel.assignments.refresh()
        }
}
