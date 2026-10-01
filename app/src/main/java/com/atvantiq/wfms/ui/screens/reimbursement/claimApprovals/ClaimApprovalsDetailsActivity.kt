package com.atvantiq.wfms.ui.screens.reimbursement.claimApprovals

import android.view.View
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseBindingActivity
import com.atvantiq.wfms.databinding.ActivityClaimApprovalsDetailsBinding
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

class ClaimApprovalsDetailsActivity : BaseBindingActivity<ActivityClaimApprovalsDetailsBinding>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_claim_approvals_details)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        setupToolbar()
    }

    private fun setupToolbar(){
        binding.claimApprovalDetailToolbar.toolbarTitle.text = getString(R.string.details)
        binding.claimApprovalDetailToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

}