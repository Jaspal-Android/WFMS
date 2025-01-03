package com.atvantiq.wfms.ui.screens.reimbursement.claimApprovals

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivityClaimApprovalsBinding
import com.atvantiq.wfms.ui.screens.adapters.ClaimApprovalsListAdapter
import com.atvantiq.wfms.ui.screens.adapters.MyClaimsListAdapter
import com.atvantiq.wfms.ui.screens.reimbursement.myClaims.MyClaimsVM
import com.atvantiq.wfms.utils.Utils
import com.atvantiq.wfms.widgets.DividerItemDecoration

class ClaimApprovalsActivity : BaseActivity<ActivityClaimApprovalsBinding,ClaimApprovalsVM>() {

    private lateinit var claimApprovalsListAdapter: ClaimApprovalsListAdapter

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_claim_approvals,ClaimApprovalsVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpToolbar()
        setClaimApprovalsList()
    }

    private fun setUpToolbar(){
        binding.claimApprovalsToolbar.toolbarTitle.text = getString(R.string.pending_approvals_pm)
        binding.claimApprovalsToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: ClaimApprovalsVM) {

    }

    private fun setClaimApprovalsList(){
        claimApprovalsListAdapter  = ClaimApprovalsListAdapter{
            Utils.jumpActivity(this,ClaimApprovalsDetailsActivity::class.java)
        }
        binding.claimApprovalsList.addItemDecoration(DividerItemDecoration(this,R.drawable.custom_divider))
        binding.claimApprovalsList.layoutManager = LinearLayoutManager(this)
        binding.claimApprovalsList.adapter = claimApprovalsListAdapter
    }
}