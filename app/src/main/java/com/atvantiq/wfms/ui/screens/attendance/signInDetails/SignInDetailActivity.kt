package com.atvantiq.wfms.ui.screens.attendance.signInDetails

import android.view.View
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseBindingActivity
import com.atvantiq.wfms.databinding.ActivitySignInDetailBinding
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

class SignInDetailActivity : BaseBindingActivity<ActivitySignInDetailBinding>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_sign_in_detail)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        setToolbar()
    }

    private fun setToolbar(){
        binding.singInDetailsToolbar.toolbarTitle.text = getString(R.string.details)
        binding.singInDetailsToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

}