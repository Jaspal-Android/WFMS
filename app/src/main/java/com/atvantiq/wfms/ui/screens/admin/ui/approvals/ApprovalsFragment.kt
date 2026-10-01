package com.atvantiq.wfms.ui.screens.admin.ui.approvals

import android.os.Bundle
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentApprovalsBinding
import com.atvantiq.wfms.ui.screens.admin.ui.attendanceApproval.AttendanceApprovalActivity
import com.atvantiq.wfms.ui.screens.admin.ui.claimApproval.ClaimApprovalActivity
import com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.WorkSitesApprovalActivity
import com.atvantiq.wfms.utils.Utils
import dagger.hilt.android.AndroidEntryPoint

/** Approvals tab (PM, OPS and admin): opens each approval category full screen. */
@AndroidEntryPoint
class ApprovalsFragment : BaseFragment<FragmentApprovalsBinding, ApprovalsVM>() {

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_approvals, ApprovalsVM::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {
    }

    override fun subscribeToEvents(vm: ApprovalsVM) {
        binding.vm = vm
        vm.clickEvents.observe(viewLifecycleOwner) { event ->
            if (!isLifeCycleResumed()) return@observe
            when (event) {
                ApprovalsClickEvents.ATTENDANCE_APPROVAL ->
                    Utils.jumpActivity(requireContext(), AttendanceApprovalActivity::class.java)
                ApprovalsClickEvents.WORK_APPROVAL ->
                    Utils.jumpActivity(requireContext(), WorkSitesApprovalActivity::class.java)
                ApprovalsClickEvents.CLAIMS_APPROVAL ->
                    Utils.jumpActivity(requireContext(), ClaimApprovalActivity::class.java)
            }
        }
    }
}
