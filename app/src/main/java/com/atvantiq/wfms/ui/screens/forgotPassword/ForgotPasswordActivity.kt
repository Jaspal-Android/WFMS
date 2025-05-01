package com.atvantiq.wfms.ui.screens.forgotPassword

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.atvantiq.wfms.ui.screens.forgotPassword.dialog.GetOTPBottomSheetDialog
import com.atvantiq.wfms.ui.screens.forgotPassword.newPassword.NewPasswordActivity
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivityForgotPasswordBinding
import com.atvantiq.wfms.ui.screens.forgotPassword.vm.ForgotPassClickEvents
import com.atvantiq.wfms.ui.screens.forgotPassword.vm.ForgotPassErrorHandler
import com.atvantiq.wfms.ui.screens.forgotPassword.vm.ForgotPasswordVM
import com.atvantiq.wfms.utils.Utils

class ForgotPasswordActivity : BaseActivity<ActivityForgotPasswordBinding, ForgotPasswordVM>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_forgot_password, ForgotPasswordVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initToolbar()
    }

    private fun initToolbar(){
        binding.toolbar.toolbarTitle.text = getString(R.string.forgotYourPassword)
        binding.toolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: ForgotPasswordVM) {
        binding.vm = vm

        vm.errorHandler.observe(this, Observer {
            when(it){
                ForgotPassErrorHandler.EMPTY_EMAIL_ADDRESS
                -> {
                    binding.phoneNumberEt.error = getString(R.string.enter_email)
                    binding.phoneNumberEt.requestFocus()
                    shakeEditText(this,binding.phoneNumberEt)
                }
            }
        })

        vm.clickEvents.observe(this, Observer {
            when(it){
                ForgotPassClickEvents.ON_RESET_PASSWORD_CLICK -> {
                    val otpBottomSheet = GetOTPBottomSheetDialog(){
                        Utils.jumpActivity(this, NewPasswordActivity::class.java)
                        finish()
                    }
                    otpBottomSheet.show(supportFragmentManager, "OtpBottomSheetDialog")
                }
                ForgotPassClickEvents.ON_BACK_TO_LOGIN_CLICK -> {
                    finish()
                }
                else -> {

                }
            }
        })
    }
}