package com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.sites

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.databinding.ActivityWorkSitesBinding
import com.atvantiq.wfms.models.workSites.workSites.WorkSitesResponse
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.AttendanceListAdapter
import com.atvantiq.wfms.ui.screens.adapters.WorkSitesAdapter
import com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.SiteApprovalVM
import com.atvantiq.wfms.utils.Utils
import com.ssas.jibli.data.prefs.PrefMethods
import retrofit2.HttpException
import javax.inject.Inject

class WorkSitesActivity : BaseActivity<ActivityWorkSitesBinding, SiteApprovalVM>() {


    private var workSiteAdapter: WorkSitesAdapter? = null
    private var employeeId: String = ""
    private var employeeRole:String = ""

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_work_sites, SiteApprovalVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        getUserDetails()
        setUpToolbarTitle()
        setUpAttendanceListAdapter()
        swipeRefresh()
        getWorkSites()
    }
    private fun getUserDetails() {
        var userData = PrefMethods.getUserData(prefMain)
        employeeRole = userData?.role?:""
    }

    private fun getWorkSites(){
        intent?.let {
            employeeId = it.getStringExtra(SharingKeys.EMPLOYEE_ID)?:""
            val date = it.getStringExtra(SharingKeys.DATE)?:""
            viewModel.getWorkSites(employeeId,date)
        }
    }

    private fun setUpToolbarTitle() {
        binding.siteApprovalToolbar.toolbarTitle.text = getString(R.string.approve_site)
        binding.siteApprovalToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: SiteApprovalVM) {
        vm.workSites.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> handleSuccessResponse(response.response)
                Status.ERROR -> handleErrorResponse(response.throwable)
                Status.LOADING -> showProgress()
            }
        }

        vm.approveWorkSiteResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    dismissProgress()
                    if(response.response?.code == ValConstants.SUCCESS_CODE){
                        showToast(this, response.response?.message.toString() )
                        getWorkSites()
                    }else{
                        alertDialogShow(
                            this, getString(R.string.alert),
                            response.response?.message ?: getString(R.string.something_went_wrong))
                    }
                }
                Status.ERROR -> {
                    dismissProgress()
                    showToast(
                        this,
                        response.throwable?.message ?: getString(R.string.something_went_wrong)
                    )
                }
                Status.LOADING -> showProgress()
            }
        }
    }

    private fun setUpAttendanceListAdapter() {
        if (workSiteAdapter == null) {
            workSiteAdapter = WorkSitesAdapter(
                this, employeeRole,
                onSiteApprovedReject = { status, workSite ->
                   viewModel.approveRejectWorkSite(
                       siteWorkId = workSite.id,
                       employeeId = employeeId.toLong(),
                       status = status,
                       remarks = ""
                   )
                }
            )
            binding.rvWorkSites.adapter = workSiteAdapter
        }
    }

    private fun handleSuccessResponse(response: WorkSitesResponse?) {
        dismissProgress()
        stopRefreshingData()
        when (response?.code) {
            ValConstants.SUCCESS_CODE -> {
                if (response.data?.workSites.isNullOrEmpty()) {
                    emptyDataLayout()
                } else {
                    mainLayout()
                }
                workSiteAdapter?.submitData(response.data?.workSites ?: emptyList())
            }

            ValConstants.UNAUTHORIZED_CODE -> {
                tokenExpiresAlert()
                emptyDataLayout()
            }
            else -> {
                alertDialogShow(
                    this, getString(R.string.alert),
                    response?.message ?: getString(R.string.something_went_wrong)
                )
                emptyDataLayout()
            }
        }
    }

    private fun handleErrorResponse(throwable: Throwable?) {
        dismissProgress()
        stopRefreshingData()
        emptyDataLayout()
        if (throwable is HttpException && throwable.code() == ValConstants.UNAUTHORIZED_CODE) {
            tokenExpiresAlert()
        } else {
            showToast(this, throwable?.message ?: getString(R.string.something_went_wrong))
        }
    }

    private fun swipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            startRefreshingData()
        }
    }

    private fun startRefreshingData() {
        getWorkSites()
    }

    private fun stopRefreshingData() {
        if (binding.swipeRefreshLayout.isRefreshing) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun mainLayout() {
        binding.isEmptyWorkSites = false
    }

    private fun emptyDataLayout() {
        binding.isEmptyWorkSites = true
    }
}