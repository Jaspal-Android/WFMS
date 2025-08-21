package com.atvantiq.wfms.ui.screens.login

import android.content.DialogInterface
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.data.prefs.PrefMain
import com.atvantiq.wfms.data.prefs.SecurePrefMain
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.databinding.ActivityLoginBinding
import com.atvantiq.wfms.ui.screens.DashboardActivity
import com.atvantiq.wfms.ui.screens.forgotPassword.ForgotPasswordActivity
import com.atvantiq.wfms.utils.Utils
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding, LoginVM>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_login, LoginVM::class.java)


    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    override fun subscribeToEvents(vm: LoginVM) {
        binding.vm = vm

        vm.clickEvents.observe(this, Observer {
            when(it){
                LoginClickEvents.ON_PASSWORD_TOGGLE -> {
                    handlePasswordToggle(vm)
                }
                LoginClickEvents.ON_LOGIN_CLICK -> {
                    Utils.jumpActivity(this, DashboardActivity::class.java)
                    finish()
                }
                LoginClickEvents.ON_FORGET_PASSWORD_CLICK -> {
                    Utils.jumpActivity(this, ForgotPasswordActivity::class.java)
                }
            }
        })

        vm.errorHandler.observe(this, Observer {
            when(it){
                LoginErrorHandler.EMPTY_USERNAME -> {
                    binding?.phoneEmailInput?.error = getString(R.string.enter_username)
                    binding.phoneEmailInput?.requestFocus()
                    shakeEditText(this,binding.phoneEmailInput)
                }

                LoginErrorHandler.EMPTY_PASSWORD -> {
                    binding?.passwordEt?.error = getString(R.string.enter_password)
                    binding?.passwordEt?.requestFocus()
                    shakeEditText(this,binding.passwordEt)
                }
            }
        })

        vm.loginResponse.observe(this@LoginActivity, Observer {
            when(it.status){
                Status.SUCCESS -> {
                    dismissProgress()
                    if(it.response?.code == 200 && it?.response?.success == true){
                        showToast(this,getString(R.string.login_success))
                        PrefMethods.saveUserToken(prefMain,it?.response?.data?.accessToken?:"")
                        PrefMethods.saveUserData(prefMain,it?.response?.data?.user)
                        Utils.jumpActivity(this,DashboardActivity::class.java)
                        finish()
                    }else{
                        alertDialogShow(this,getString(R.string.alert),it?.response?.message.toString()) { dialog, which ->
                            dialog.dismiss()
                        }
                    }
                }
                Status.LOADING -> {
                   showProgress()
                }
                Status.ERROR ->{
                    dismissProgress()
                    alertDialogShow(this,getString(R.string.alert),it?.throwable?.message.toString())
                }
            }
        })
    }

    private fun handlePasswordToggle(vm: LoginVM) {
        if (vm.isPasswordVisible) {
            Utils.hidePassword(binding?.passwordEt)
            binding?.isToggle = true
            vm.isPasswordVisible = false
        } else {
            Utils.showPassword(binding?.passwordEt)
            binding?.isToggle = false
            vm.isPasswordVisible = true
        }
    }

}