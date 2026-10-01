package com.atvantiq.wfms.ui.screens.vendor.viewAllActivities

import android.view.View
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseBindingActivity
import com.atvantiq.wfms.databinding.ActivityVendorDetailsBinding
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

class VendorDetailsActivity : BaseBindingActivity<ActivityVendorDetailsBinding>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_vendor_details)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        setupToolbar()
    }

    private fun setupToolbar(){
        binding.vendorDetailsToolbar.toolbarTitle.text = getString(R.string.details)
        binding.vendorDetailsToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

}