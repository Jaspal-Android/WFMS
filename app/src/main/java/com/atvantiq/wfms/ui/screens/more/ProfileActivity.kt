package com.atvantiq.wfms.ui.screens.more

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivityProfileBinding
import com.atvantiq.wfms.utils.isSessionLost
import dagger.hilt.android.AndroidEntryPoint
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

/** My Profile: the employee's personal and employment details from `GET /employee/me`. */
@AndroidEntryPoint
class ProfileActivity : BaseActivity<ActivityProfileBinding, ProfileVM>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_profile, ProfileVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        binding.main.applySystemBarsAndImePadding()
        binding.toolbar.toolbarTitle.text = getString(R.string.my_profile)
        binding.toolbar.toolbarBackButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        viewModel.refresh()
    }

    override fun subscribeToEvents(vm: ProfileVM) {
        vm.profile.observe(this) { profile -> binding.profile = profile }
        vm.profileResponse.observe(this) { response ->
            if (!response.consumeOnce()) return@observe
            // The cached profile stays on screen if the refresh fails; only a lost session matters.
            if (response.isSessionLost { it.code }) tokenExpiresAlert()
        }
    }
}
