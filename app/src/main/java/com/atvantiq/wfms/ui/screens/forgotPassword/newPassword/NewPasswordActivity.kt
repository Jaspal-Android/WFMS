package com.atvantiq.wfms.ui.screens.forgotPassword.newPassword

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivityNewPasswordBinding
import com.atvantiq.wfms.ui.screens.forgotPassword.newPassword.vm.CreatePassClickEvents
import com.atvantiq.wfms.ui.screens.forgotPassword.newPassword.vm.CreatePassErrorHandler
import com.atvantiq.wfms.ui.screens.forgotPassword.newPassword.vm.CreatePasswordVM
import com.atvantiq.wfms.utils.Utils

class NewPasswordActivity : BaseActivity<ActivityNewPasswordBinding, CreatePasswordVM>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_new_password, CreatePasswordVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initToolbar()
    }

    private fun initToolbar(){
        binding.newPassToolbar.toolbarTitle.text = "Create Password"
        binding.newPassToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: CreatePasswordVM) {
        binding.vm = vm

        vm.errorHandler.observe(this, Observer {
            when(it){
                CreatePassErrorHandler.EMPTY_PASSWORD -> {
                    binding?.passwordEt?.error = getString(R.string.enter_password)
                    binding?.passwordEt?.requestFocus()
                }
                CreatePassErrorHandler.EMPTY_CONFIRM_PASSWORD -> {
                    binding?.confirmPasswordEt?.error = getString(R.string.enter_confirm_password)
                    binding?.confirmPasswordEt?.requestFocus()
                }
                CreatePassErrorHandler.MISMATCH_PASSWORD -> {
                    binding?.confirmPasswordEt?.error = getString(R.string.password_mismatch_error)
                    binding?.confirmPasswordEt?.requestFocus()
                }
            }
        })

        vm.clickEvents.observe(this, Observer {
            when(it){
                CreatePassClickEvents.ON_PASSWORD_TOGGLE_CLICK -> {
                    handlePasswordToggle(vm)
                }
                CreatePassClickEvents.ON_CONFIRM_PASSWORD_TOGGLE_CLICK -> {
                    handleConfirmPasswordToggle(vm)
                }
                else -> {}
            }
        })
    }

    private fun handlePasswordToggle(vm: CreatePasswordVM) {
        if (vm.isPasswordVisible) {
            Utils.hidePassword(binding?.passwordEt!!)
            binding?.isToggle = true
            vm.isPasswordVisible = false
        } else {
            Utils.showPassword(binding?.passwordEt!!)
            binding?.isToggle = false
            vm.isPasswordVisible = true
        }
    }

    private fun handleConfirmPasswordToggle(vm: CreatePasswordVM) {
        if (vm.isConfirmPasswordVisible) {
            Utils.hidePassword(binding?.confirmPasswordEt!!)
            binding?.isConfirmToggle = true
            vm.isConfirmPasswordVisible = false
        } else {
            Utils.showPassword(binding?.confirmPasswordEt!!)
            binding?.isConfirmToggle = false
            vm.isConfirmPasswordVisible = true
        }
    }

}