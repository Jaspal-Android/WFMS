package com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.workSites

import android.view.View
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ActivityWorkSitesBinding
import com.atvantiq.wfms.models.workSites.workSites.WorkSite
import com.atvantiq.wfms.models.workSites.workSites.WorkSitesResponse
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.WorkSitesAdapter
import com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.SiteApprovalVM
import com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.workSiteDetail.SiteWorkDetailActivity
import com.atvantiq.wfms.utils.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

/**
 * Work Sites: the sites one employee worked on one day. Returns RESULT_OK when work was approved
 * or rejected on any of them, so Work Approval refreshes that day's status.
 */
@AndroidEntryPoint
class WorkSitesActivity : BaseActivity<ActivityWorkSitesBinding, SiteApprovalVM>() {

    private var workSiteAdapter: WorkSitesAdapter? = null
    private var employeeId: String = ""
    private var date: String = ""

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_work_sites, SiteApprovalVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        employeeId = intent?.getStringExtra(SharingKeys.EMPLOYEE_ID).orEmpty()
        date = intent?.getStringExtra(SharingKeys.DATE).orEmpty()
        binding.dateLabel = DateUtils.formatYmdLabel(date)
        binding.emptyState.root.isVisible = false
        setUpToolbarTitle()
        setUpWorkSitesList()
        binding.swipeRefreshLayout.setOnRefreshListener { getWorkSites() }
        getWorkSites()
    }

    private fun getWorkSites() {
        viewModel.getWorkSites(employeeId, date)
    }

    private fun setUpToolbarTitle() {
        binding.siteApprovalToolbar.toolbarTitle.text = getString(R.string.work_sites_title)
        binding.siteApprovalToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: SiteApprovalVM) {
        vm.workSites.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> handleSuccessResponse(response.response)
                Status.ERROR -> {
                    stopLoading()
                    handleApiFailure(response.throwable)
                }
                Status.LOADING -> if (!binding.swipeRefreshLayout.isRefreshing) showProgress()
            }
        }
    }

    private fun setUpWorkSitesList() {
        workSiteAdapter = WorkSitesAdapter(onTapSite = ::launchWorkSiteDetails)
        binding.rvWorkSites.adapter = workSiteAdapter
    }

    private fun handleSuccessResponse(response: WorkSitesResponse?) {
        stopLoading()
        if (response?.code != ValConstants.SUCCESS_CODE) {
            handleRejectedResponse(response?.code, response?.message)
            return
        }
        val sites = response.data?.workSites.orEmpty()
        binding.employee = response.data?.employee
        workSiteAdapter?.submitList(sites)
        binding.emptyState.root.isVisible = sites.isEmpty()
    }

    private fun stopLoading() {
        dismissProgress()
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private val siteWorkDetailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                setResult(Activity.RESULT_OK)
                getWorkSites()
            }
        }

    private fun launchWorkSiteDetails(position: Int, item: WorkSite) {
        val intent = Intent(this, SiteWorkDetailActivity::class.java).apply {
            putExtra(SharingKeys.WORK_POSITION, position)
            putExtra(SharingKeys.WORK_ID, item.id)
            putExtra(SharingKeys.EMPLOYEE_ID, employeeId)
            putExtra(SharingKeys.WORK_DATE, date)
        }
        siteWorkDetailLauncher.launch(intent)
    }
}
