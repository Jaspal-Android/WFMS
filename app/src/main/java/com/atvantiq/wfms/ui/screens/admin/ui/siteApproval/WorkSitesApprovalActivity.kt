package com.atvantiq.wfms.ui.screens.admin.ui.siteApproval

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ActivityWorkSitesApprovalBinding
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceDetailListResponse
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.AttendanceListAdapter
import com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.sites.WorkSitesActivity
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.utils.Utils
import retrofit2.HttpException

class WorkSitesApprovalActivity : BaseActivity<ActivityWorkSitesApprovalBinding, SiteApprovalVM>() {
    private var monthYear: Pair<Int, Int>? = null
    private var attendanceListAdapter: AttendanceListAdapter? = null

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_work_sites_approval, SiteApprovalVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpToolbarTitle()
        setUpAttendanceListAdapter()
        swipeRefresh()
        monthYear = DateUtils.getCurrentMonthAndYear()
        if (monthYear != null) {
            getAttendanceDetails()
        }

        binding.monthSelctorLayout.setOnClickListener {
            DateUtils.showMonthYearPickerDialog(
                this,
                monthYear?.first,
                monthYear?.second
            ) { selectedMonth, selectedYear ->
                Log.e(
                    "MonthYearPicker",
                    "Selected Month: $selectedMonth, Selected Year: $selectedYear"
                )
                monthYear = Pair(selectedMonth, selectedYear)
                getAttendanceDetails()
            }
        }
    }

    private fun setUpToolbarTitle() {
        binding.siteApprovalToolbar.toolbarTitle.text = getString(R.string.approve_site)
        binding.siteApprovalToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: SiteApprovalVM) {
        vm.attendanceDetailsResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> handleSuccessResponse(response.response)
                Status.ERROR -> handleErrorResponse(response.throwable)
                Status.LOADING -> showProgress()
            }
        }
    }

    private fun setUpAttendanceListAdapter() {
        if (attendanceListAdapter == null) {
            attendanceListAdapter = AttendanceListAdapter { employeeId, date ->
                Utils.jumpActivityWithData(
                    this,
                    WorkSitesActivity::class.java,
                    Bundle().apply {
                        putString(SharingKeys.EMPLOYEE_ID, employeeId)
                        putString(SharingKeys.DATE, date)
                    }
                )
            }
            binding.rvAttendance.adapter = attendanceListAdapter
        }
    }

    private fun getAttendanceDetails() {
        stopRefreshingData()
        if (monthYear != null) {
            if (monthYear?.first != null || monthYear?.second != null) {
                viewModel?.getAttendanceDetails(monthYear!!.first, monthYear!!.second)
            }
        }
    }

    private fun handleSuccessResponse(response: AttendanceDetailListResponse?) {
        dismissProgress()
        stopRefreshingData()
        when (response?.code) {
            ValConstants.SUCCESS_CODE -> {
                mainLayout()
                if (response.data?.records.isNullOrEmpty()) {
                    emptyDataLayout()
                }
                attendanceListAdapter?.submitData(response.data?.records ?: emptyList())
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
        getAttendanceDetails()
    }

    private fun stopRefreshingData() {
        if (binding.swipeRefreshLayout.isRefreshing) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun mainLayout() {
        binding.isEmptyAttendanceList = false
    }

    private fun emptyDataLayout() {
        binding.isEmptyAttendanceList = true
    }

}