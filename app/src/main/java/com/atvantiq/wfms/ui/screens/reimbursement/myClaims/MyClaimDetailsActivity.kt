package com.atvantiq.wfms.ui.screens.reimbursement.myClaims

import android.view.View
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseBindingActivity
import com.atvantiq.wfms.databinding.ActivityMyClaimDetailsBinding
import com.atvantiq.wfms.databinding.ActivityProgressDetailsBinding
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

class MyClaimDetailsActivity : BaseBindingActivity<ActivityMyClaimDetailsBinding>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_my_claim_details)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        setupToolbar()
    }

    private fun setupToolbar(){
        binding.myClaimsDetailsToolbar.toolbarTitle.text = getString(R.string.details)
        binding.myClaimsDetailsToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}