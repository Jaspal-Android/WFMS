package com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.workSiteDetail

import android.view.View
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.AppRole
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.constants.StatusCodes
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ActivitySiteWorkDetailBinding
import com.atvantiq.wfms.models.workSites.approve.ApproveWorkSiteTypeResponse
import com.atvantiq.wfms.models.workSites.workSiteDetails.Data
import com.atvantiq.wfms.models.workSites.workSiteDetails.WorkSiteDetailResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.WorkTypeAdapterAdmin
import com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.SiteApprovalVM
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.AndroidEntryPoint
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

/**
 * Site Work Detail (spec 7.3): the work types at one site, where the role approves or rejects the
 * ones still waiting for it. RESULT_OK tells Work Approval that something changed.
 */
@AndroidEntryPoint
class SiteWorkDetailActivity : BaseActivity<ActivitySiteWorkDetailBinding, SiteApprovalVM>() {

    private var workSiteId: Long = NO_ID
    private var employeeId: String = ""
    private var date: String = ""
    private lateinit var role: AppRole
    private var itemTypeAdapter: WorkTypeAdapterAdmin? = null

    /** The role as the work-type rules spell it. */
    private val roleKey: String get() = role.approverTag.orEmpty()

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_site_work_detail, SiteApprovalVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        role = PrefMethods.getAppRole(prefMain)
        binding.toolbar.toolbarTitle.text = getString(R.string.site_work_detail)
        binding.toolbar.toolbarBackButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        setUpWorkTypes()
        binding.btnSelectAll.setOnClickListener { itemTypeAdapter?.toggleSelectAll() }
        binding.btnApprove.setOnClickListener { decide(StatusCodes.APPROVE) }
        binding.btnReject.setOnClickListener { decide(StatusCodes.REJECT) }

        workSiteId = intent.getLongExtra(SharingKeys.WORK_ID, NO_ID)
        employeeId = intent.getStringExtra(SharingKeys.EMPLOYEE_ID).orEmpty()
        date = intent.getStringExtra(SharingKeys.WORK_DATE).orEmpty()
        loadWorkSite()
    }

    private fun loadWorkSite() = viewModel.getWorkSiteDetails(workSiteId, employeeId, date)

    private fun setUpWorkTypes() {
        itemTypeAdapter = WorkTypeAdapterAdmin(roleKey) { selected -> binding.hasSelection = selected.isNotEmpty() }
        binding.rvWorkTypes.adapter = itemTypeAdapter
    }

    /** Approve (1) or reject (2) every ticked type; remarks "Approved by pm" / "Rejected by pm". */
    private fun decide(status: Int) {
        val selected = itemTypeAdapter?.getSelectedTypes().orEmpty()
        val employee = employeeId.toLongOrNull()
        when {
            selected.isEmpty() -> return showToast(this, getString(R.string.select_eligible_work_type))
            employee == null || employee <= 0 || workSiteId <= 0 -> return showToast(this, getString(R.string.employee_id_invalid))
        }
        val tag = roleKey.lowercase()
        val remarks = getString(if (status == StatusCodes.APPROVE) R.string.approved_by_format else R.string.rejected_by_format, tag)
        viewModel.approveRejectWorkSite(workSiteId, employee ?: return, status, remarks, selected)
    }

    private fun render(data: Data?) {
        binding.site = data
        val types = data?.workType.orEmpty()
        val waiting = viewModel.hasApprovableTypes(roleKey, types)
        binding.showSelectAll = waiting
        binding.emptyMessage = when {
            data == null -> getString(R.string.work_progress_unavailable)
            types.isEmpty() -> getString(R.string.no_work_types_found)
            !role.canApprove -> getString(R.string.cannot_approve_work_types)
            !waiting -> getString(R.string.no_work_types_waiting)
            else -> null
        }
        itemTypeAdapter?.setData(types)
    }

    override fun subscribeToEvents(vm: SiteApprovalVM) {
        vm.workSiteDetails.observe(this) { response -> handleWorkSiteDetails(response) }
        vm.approveWorkSiteResponse.observe(this) { response -> handleDecision(response) }
    }

    private fun handleWorkSiteDetails(response: ApiState<WorkSiteDetailResponse>) {
        when (response.status) {
            Status.LOADING -> showProgress()
            Status.SUCCESS -> {
                dismissProgress()
                val answer = response.response
                if (answer?.code == ValConstants.SUCCESS_CODE) render(answer.data) else {
                    render(null)
                    handleRejectedResponse(answer?.code, answer?.message)
                }
            }
            Status.ERROR -> {
                dismissProgress()
                render(null)
                handleApiFailure(response.throwable)
            }
        }
    }

    /** After a decision the server's steps are reloaded and the selection cleared. */
    private fun handleDecision(response: ApiState<ApproveWorkSiteTypeResponse>) {
        if (!response.consumeOnce()) return
        when (response.status) {
            Status.LOADING -> showProgress()
            Status.SUCCESS -> {
                dismissProgress()
                val answer = response.response
                if (answer?.code == ValConstants.SUCCESS_CODE) {
                    showToast(this, answer.message ?: getString(R.string.work_site_approval_successful))
                    setResult(RESULT_OK)
                    loadWorkSite()
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

    private companion object {
        const val NO_ID = -1L
    }
}
