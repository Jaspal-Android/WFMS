package com.atvantiq.wfms.ui.screens.login

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivityLoginBinding
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.atvantiq.wfms.models.notification.UpdateNotificationTokenResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.DashboardActivity
import com.atvantiq.wfms.ui.screens.forgotPassword.ForgotPasswordActivity
import com.atvantiq.wfms.utils.Utils
import com.google.firebase.messaging.FirebaseMessaging
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.AndroidEntryPoint

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

        vm.clickEvents.observe(this, Observer { handleClickEvents(it, vm) })
        vm.errorHandler.observe(this, Observer { handleErrors(it) })
        vm.loginResponse.observe(this, Observer { handleLoginResponse(it) })
        vm.sendNotificationTokenResponse.observe(this, Observer { handleSendNotificationTokenResponse(it) })
    }

    private fun handleClickEvents(event: LoginClickEvents, vm: LoginVM) {
        when (event) {
            LoginClickEvents.ON_PASSWORD_TOGGLE -> handlePasswordToggle(vm)
            LoginClickEvents.ON_LOGIN_CLICK -> navigateToDashboard()
            LoginClickEvents.ON_FORGET_PASSWORD_CLICK -> navigateToForgotPassword()
        }
    }

    private fun handleErrors(error: LoginErrorHandler) {
        when (error) {
            LoginErrorHandler.EMPTY_USERNAME -> {
                binding.phoneEmailInput?.apply {
                    setError(getString(R.string.enter_username))
                    requestFocus()
                }
                shakeEditText(this, binding.phoneEmailInput)
            }
            LoginErrorHandler.EMPTY_PASSWORD -> {
                binding.passwordEt?.apply {
                    setError(getString(R.string.enter_password))
                    requestFocus()
                }
                shakeEditText(this, binding.passwordEt)
            }
        }
    }

    private fun handleLoginResponse(response: ApiState<LoginResponse>) {
        when (response.status) {
            Status.SUCCESS -> handleLoginSuccess(response)
            Status.LOADING -> showProgress()
            Status.ERROR -> {
                dismissProgress()
                alertDialogShow(this, getString(R.string.alert), response.throwable?.message.orEmpty())
            }
        }
    }

    private fun handleSendNotificationTokenResponse(response: ApiState<UpdateNotificationTokenResponse>) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                showToast(this, getString(R.string.login_success))
                navigateToDashboard()
            }
            Status.LOADING -> {showProgress()}
            Status.ERROR -> {
                dismissProgress()
                alertDialogShow(this, getString(R.string.alert), response.throwable?.message.orEmpty())
            }
        }
    }

    private fun handleLoginSuccess(response: ApiState<LoginResponse>) {
        dismissProgress()
        response.response?.let {
            if (it.code == 200 && it.success) {
                PrefMethods.saveUserToken(prefMain, it.data?.accessToken.orEmpty())
                PrefMethods.saveUserData(prefMain, it.data?.user)
                FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val token = task.result
                            viewModel.sendNotificationToken(it.data?.user?.userId.toString(), token)
                        } else {
                            Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                        }
                    }
            } else {
                alertDialogShow(this, getString(R.string.alert), it.message.orEmpty()) { dialog, _ ->
                    dialog.dismiss()
                }
            }
        }
    }

    private fun handlePasswordToggle(vm: LoginVM) {
        vm.isPasswordVisible = !vm.isPasswordVisible
        binding.isToggle = !vm.isPasswordVisible
        if (vm.isPasswordVisible) {
            Utils.showPassword(binding.passwordEt)
        } else {
            Utils.hidePassword(binding.passwordEt)
        }
    }

    private fun navigateToDashboard() {
        Utils.jumpActivity(this, DashboardActivity::class.java)
        finish()
    }

    private fun navigateToForgotPassword() {
        Utils.jumpActivity(this, ForgotPasswordActivity::class.java)
    }
}
