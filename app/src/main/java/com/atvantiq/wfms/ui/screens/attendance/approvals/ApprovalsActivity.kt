package com.atvantiq.wfms.ui.screens.attendance.approvals

import android.view.View
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivityApprovalsBinding
import com.atvantiq.wfms.ui.screens.adapters.ApprovalsListAdapter
import com.atvantiq.wfms.ui.screens.adapters.MyProgressAdapter
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

class ApprovalsActivity : BaseActivity<ActivityApprovalsBinding,ApprovalsVM>() {

    private lateinit var approvalsListAdapter: ApprovalsListAdapter

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_approvals,ApprovalsVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        setToolbar()
        setApprovalsList()
    }

    private fun setToolbar(){
        binding.approvalsToolbar.toolbarTitle.text = getString(R.string.approvals)
        binding.approvalsToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: ApprovalsVM) {

    }

    private fun setApprovalsList(){
        approvalsListAdapter  = ApprovalsListAdapter()
        binding.approvalsList.layoutManager = LinearLayoutManager(this)
        binding.approvalsList.adapter = approvalsListAdapter
    }
}