package com.atvantiq.wfms.ui.screens.admin

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivitySharedDashboardBinding
import com.atvantiq.wfms.models.loginResponse.User
import com.atvantiq.wfms.ui.screens.admin.ui.site.SitesActivity
import com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.WorkSitesApprovalActivity
import com.atvantiq.wfms.ui.screens.login.LoginActivity
import com.atvantiq.wfms.utils.Utils
import com.google.firebase.messaging.FirebaseMessaging
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SharedDashboardActivity : BaseActivity<ActivitySharedDashboardBinding,SharedDashboardVM>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_shared_dashboard, SharedDashboardVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        var userData = PrefMethods.getUserData(prefMain)
        setupHeaderData(userData)
    }

    private fun setupHeaderData(userData: User?) {
        if (userData == null)
            return
        binding.tvUserName.text = (userData.firstName ?: "") + " " + (userData.lastName ?: "")
        binding.tvUserEmail.text = userData?.email ?: ""
        binding.tvUserRole.text = userData?.role ?: ""
    }

    private fun logoutUser(){
        alertDialogShow(this,
            getString(R.string.logout),
            getString(R.string.logout_confirmation),
            getString(R.string.yes),
            { dialog, which ->
                dialog.dismiss()
                performLogout()
            },
            { dialog, which ->
                dialog.dismiss()
            })
    }

    private fun performLogout() {
        FirebaseMessaging.getInstance().deleteToken()
        prefMain.deleteAll()
        Utils.jumpActivity(this, LoginActivity::class.java)
        finish()
    }

    override fun subscribeToEvents(vm: SharedDashboardVM) {
        binding.vm = vm
        vm.clickEvents.observe(this) { event ->
            when (event) {
                SharedDashClickEvents.OPEN_SITES_CLICK -> {
                    Utils.jumpActivity(this, SitesActivity::class.java)
                }
                SharedDashClickEvents.OPEN_SITES_APPROVALS_CLICK -> {
                    Utils.jumpActivity(this, WorkSitesApprovalActivity::class.java)
                }
                SharedDashClickEvents.OPEN_CLAIM_APPROVALS_CLICK -> {

                }
                SharedDashClickEvents.OPEN_PROFILE_CLICK -> {

                }

                SharedDashClickEvents.LOGOUT_CLICK ->{
                    logoutUser()
                }
            }
        }
    }

}