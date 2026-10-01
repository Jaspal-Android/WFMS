package com.atvantiq.wfms.ui.screens.vendor.loginDetails

import android.view.View
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.base.BaseBindingActivity
import com.atvantiq.wfms.databinding.ActivityVendorDetailsBinding
import com.atvantiq.wfms.databinding.ActivityVendorLoginDetailsBinding
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

class VendorLoginDetailsActivity : BaseBindingActivity<ActivityVendorLoginDetailsBinding>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_vendor_login_details)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        setupToolbar()
    }

    private fun setupToolbar(){
        binding.vendorLoginDetailToolbar.toolbarTitle.text= getString(R.string.details)
        binding.vendorLoginDetailToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

}