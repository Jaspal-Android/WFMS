package com.atvantiq.wfms.ui.screens.attendance.approvals

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivityApprovalsBinding
import com.atvantiq.wfms.ui.screens.adapters.ApprovalsListAdapter
import com.atvantiq.wfms.ui.screens.adapters.MyProgressAdapter

class ApprovalsActivity : BaseActivity<ActivityApprovalsBinding,ApprovalsVM>() {

    private lateinit var approvalsListAdapter: ApprovalsListAdapter

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_approvals,ApprovalsVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
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