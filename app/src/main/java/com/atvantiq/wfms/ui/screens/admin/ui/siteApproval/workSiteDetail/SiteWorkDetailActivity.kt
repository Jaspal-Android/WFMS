package com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.workSiteDetail

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.constants.StatusCodes
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ActivitySiteWorkDetailBinding
import com.atvantiq.wfms.models.work.workDetail.WorkDetailData
import com.atvantiq.wfms.models.work.workDetail.WorkDetailResponse
import com.atvantiq.wfms.models.workSites.approve.ApproveWorkSiteTypeResponse
import com.atvantiq.wfms.models.workSites.workSiteDetails.Data
import com.atvantiq.wfms.models.workSites.workSiteDetails.WorkSiteDetailResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.WorkTypeAdapterAdmin
import com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.SiteApprovalVM
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException

@AndroidEntryPoint
class SiteWorkDetailActivity : BaseActivity<ActivitySiteWorkDetailBinding, SiteApprovalVM>() {

    private var workSiteId: Long? = null
    private var employeeId: String = ""
    private var date: String = ""
    private var employeeRole:String = ""

    private var itemPosition: Int = -1
    private var itemTypeAdapter: WorkTypeAdapterAdmin? = null

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(
            R.layout.activity_site_work_detail,
            SiteApprovalVM::class.java
        )

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        getUserDetails()
        setToolbar()
        initListeners()
        setupWokTypeRecyclerView()
        setupSelectAllCheckbox()
        fetchIntentData()
    }

    private fun setToolbar() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.tvBack.text = getString(R.string.details)
    }

    private fun initListeners() {
        binding.btnApprove.setOnClickListener {
            approveRejectWorkSiteType(StatusCodes.APPROVE)
        }
        binding.btnReject.setOnClickListener {
            approveRejectWorkSiteType(StatusCodes.REJECT)
        }
    }

    private fun getUserDetails() {
        var userData = PrefMethods.getUserData(prefMain)
        employeeRole = userData?.role?:""
    }

    private fun approveRejectWorkSiteType(status: Int){
        viewModel.approveRejectWorkSite(
            workSiteId ?: -1,
            employeeId.toLongOrNull() ?: -1, // Type ID can be set as needed
            status,
            if(status ==1){getString(R.string.approved_by)+" "+employeeRole}else{getString(R.string.rejected_by)+" "+employeeRole},
            itemTypeAdapter?.getSelectedTypes()
        )
    }

    private fun fetchIntentData() {
        itemPosition = intent.getIntExtra(SharingKeys.WORK_POSITION, -1)
        workSiteId = intent.getLongExtra(SharingKeys.WORK_ID, -1)
        employeeId = intent.getStringExtra(SharingKeys.EMPLOYEE_ID) ?: ""
        date = intent.getStringExtra(SharingKeys.WORK_DATE) ?: ""
        viewModel.itemPosition.value = itemPosition
        getWorkSiteDetails()
    }

    private fun getWorkSiteDetails() {
        viewModel.getWorkSiteDetails(workSiteId ?: -1, employeeId, date)
    }

    private fun setupWokTypeRecyclerView() {
        itemTypeAdapter = WorkTypeAdapterAdmin(employeeRole) { allSelected ->
            binding.cbSelectAllWorkTypes.setOnCheckedChangeListener(null)
            binding.cbSelectAllWorkTypes.isChecked = allSelected.isNullOrEmpty().not()
            binding.hideButtons = allSelected.isNullOrEmpty()
            binding.cbSelectAllWorkTypes.setOnCheckedChangeListener { _, isChecked ->
                itemTypeAdapter?.setAllSelected(isChecked)
            }
        }
        binding.rvWorkTypes.apply {
            layoutManager = LinearLayoutManager(this@SiteWorkDetailActivity)
            adapter = itemTypeAdapter
        }
    }

    private fun setupSelectAllCheckbox() {
        binding.cbSelectAllWorkTypes.setOnCheckedChangeListener { _, isChecked ->
            itemTypeAdapter?.setAllSelected(isChecked)
        }
    }

    private fun setupUI(record: Data?) {
        binding.tvProject.text = record?.project?.name ?: getString(R.string.not_available)
        binding.tvCircle.text = record?.circle?.name ?: getString(R.string.not_available)
        binding.tvSiteName.text = record?.site?.name ?: getString(R.string.not_available)
        binding.tvSiteCode.text = record?.site?.siteId ?: getString(R.string.not_available)
        binding.siteStatusInteger = record?.site?.status?.code ?: -1

        if (record?.workType?.isNullOrEmpty() == true) {
            binding.showSelectAll = false
        } else {
            val hasOpenWorkType = record?.workType?.any { it.status?.code == StatusCodes.WIP || it.status?.code == StatusCodes.COMPLETED }
            var eligibleToApprove = false
            when (employeeRole.lowercase()) {
                ValConstants.ROLE_PM.lowercase() -> {
                    if (record?.workType?.any { it.pm?.status == 0 && it.admin?.status == 0 } == true) {
                        eligibleToApprove = true
                    }
                }
                ValConstants.ROLE_OPS.lowercase() -> {
                    if (record?.workType?.any { it.ops?.status == 0 && it.admin?.status == 0 } == true) {
                        eligibleToApprove = true
                    }
                }
                ValConstants.ROLE_Admin.lowercase() -> {
                    if( record?.workType?.any { it.admin?.status == 0 } == true) {
                        eligibleToApprove = true
                    }
                }
            }
            if (hasOpenWorkType == true && eligibleToApprove) {
                binding.showSelectAll = true
            }
        }
        itemTypeAdapter?.setData(record?.workType ?: emptyList(), false)
    }


    override fun subscribeToEvents(vm: SiteApprovalVM) {
        vm.workSiteDetails.observe(this) { response ->
            handleWorkSiteDetails(response)
        }

        vm.approveWorkSiteResponse.observe(this) { response ->
            handleWorkSiteStatusResponse(
                response,
                R.string.work_site_approval_successful
            )
        }
    }

    private fun handleWorkSiteDetails(response: ApiState<WorkSiteDetailResponse>) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                response.response?.let {
                    if (it.code == 200) {
                        setupUI(it.data)
                    } else {
                        handleErrorResponse(it.code, it.message)
                    }
                }
            }

            Status.ERROR -> handleError(response.throwable)
            Status.LOADING -> showProgress()
        }

    }


    private fun handleWorkSiteStatusResponse(
        response: ApiState<ApproveWorkSiteTypeResponse>,
        successMessage: Int,
    ) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                response.response?.let {
                    if (it.code == 200) {
                        showToast(this, it.message ?: getString(successMessage))
                        getWorkSiteDetails()
                        setResult(RESULT_OK)
                    } else {
                        handleErrorResponse(it.code, it.message)
                    }
                }
            }

            Status.ERROR -> handleError(response.throwable)
            Status.LOADING -> showProgress()
        }
    }

    private fun handleStatusUpdateResponse(data: WorkDetailData?) {
        //setupUI(data)
        val resultIntent = Intent().apply {
            putExtra(SharingKeys.WORK_POSITION, viewModel.itemPosition.value)
            putExtra(
                SharingKeys.UPDATED_STATUS,
                data?.status?.code
            ) // Add any other updated data as needed
        }
        setResult(RESULT_OK, resultIntent)
    }

    private fun handleErrorResponse(code: Int?, message: String?) {
        if (code == 401) tokenExpiresAlert() else alertDialogShow(
            this,
            getString(R.string.alert),
            message ?: getString(R.string.something_went_wrong)
        )
    }

    private fun handleError(throwable: Throwable?) {
        dismissProgress()
        if (throwable is HttpException && throwable.code() == 401) {
            tokenExpiresAlert()
        } else {
            showToast(this, throwable?.message ?: getString(R.string.something_went_wrong))
        }
    }
}