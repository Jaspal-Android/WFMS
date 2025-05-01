package com.atvantiq.wfms.ui.screens.vendor.loginDetails

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.base.BaseBindingActivity
import com.atvantiq.wfms.databinding.ActivityVendorDetailsBinding
import com.atvantiq.wfms.databinding.ActivityVendorLoginDetailsBinding

class VendorLoginDetailsActivity : BaseBindingActivity<ActivityVendorLoginDetailsBinding>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_vendor_login_details)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupToolbar()
    }

    private fun setupToolbar(){
        binding.vendorLoginDetailToolbar.toolbarTitle.text= getString(R.string.details)
        binding.vendorLoginDetailToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

}