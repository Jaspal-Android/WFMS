package com.atvantiq.wfms.ui.screens.attendance.myProgress.progressDetails

import android.view.View
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseBindingActivity
import com.atvantiq.wfms.databinding.ActivityProgressDetailsBinding
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

class ProgressDetailsActivity : BaseBindingActivity<ActivityProgressDetailsBinding>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_progress_details)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        setupToolbar()
    }

    private fun setupToolbar(){
        binding.progressDetailsToolbar.toolbarTitle.text = getString(R.string.details)
        binding.progressDetailsToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

}