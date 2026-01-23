package com.atvantiq.wfms.ui.screens.reimbursement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentReimbursementBinding
import com.atvantiq.wfms.ui.screens.adapters.AttendanceOptionsAdapter
import com.atvantiq.wfms.ui.screens.attendance.AttendanceClickEvents
import com.atvantiq.wfms.ui.screens.attendance.addSignInActivity.AddSignInActivity
import com.atvantiq.wfms.ui.screens.attendance.myProgress.MyProgressActivity
import com.atvantiq.wfms.ui.screens.attendance.signInDetails.SignInDetailActivity
import com.atvantiq.wfms.ui.screens.reimbursement.claimApprovals.ClaimApprovalsActivity
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.CreateClaimActivity
import com.atvantiq.wfms.ui.screens.reimbursement.myClaims.MyClaimsActivity
import com.atvantiq.wfms.utils.Utils

class ReimbursementFragment : BaseFragment<FragmentReimbursementBinding, ReimbursementViewModel>() {


    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_reimbursement, ReimbursementViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun subscribeToEvents(vm: ReimbursementViewModel) {
        binding.vm = vm
        vm.clickEvents.observe(viewLifecycleOwner) { event ->
            if (!isLifeCycleResumed()) return@observe
            handleClickEvents(event)

        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
    }

    private fun handleClickEvents(event: ReimbursementClickEvents) {
        when (event) {
            ReimbursementClickEvents.ON_CLICK_CREATE_CLAIM -> {
                val intent = Intent(requireContext(), CreateClaimActivity::class.java)
                createClaimLauncher.launch(intent)
            }
        }
    }

    private val createClaimLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {

            }
        }
}