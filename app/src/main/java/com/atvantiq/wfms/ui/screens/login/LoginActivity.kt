package com.atvantiq.wfms.ui.screens.login

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ActivityLoginBinding
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.atvantiq.wfms.models.loginWithOTP.RequestOtpResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.DashboardActivity
import com.atvantiq.wfms.ui.screens.forgotPassword.ForgotPasswordActivity
import com.atvantiq.wfms.ui.screens.forgotPassword.dialog.GetOTPBottomSheetDialog
import com.atvantiq.wfms.ui.screens.login.withOtp.RequestOtpBottomSheet
import com.atvantiq.wfms.utils.Utils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding, LoginVM>() {

    var lat: Double = 0.0
    var long: Double = 0.0
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var getOtpBottomSheet: GetOTPBottomSheetDialog? = null

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_login, LoginVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        blockScreenCapture()
        enableEdgeToEdge()
        binding.main.applySystemBarsAndImePadding()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Done on the password keyboard logs in, so the keyboard never has to be closed first.
        binding.passwordEt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_DONE) return@setOnEditorActionListener false
            viewModel.onSubmitLoginClick()
            true
        }

    }

    override fun subscribeToEvents(vm: LoginVM) {
        binding.vm = vm
        vm.clickEvents.observe(this, Observer { handleClickEvents(it, vm) })
        vm.errorHandler.observe(this, Observer { handleErrors(it) })
        vm.loginResponse.observe(this, Observer { handleLoginResponse(it) })
        vm.loginCompleted.observe(this, Observer { completed -> if (completed == true) onLoginCompleted() })
        vm.requestOtpResponse.observe(this, Observer { handleRequestOtpResponse(it) })
    }

    private fun handleClickEvents(event: LoginClickEvents, vm: LoginVM) {
        when (event) {
            LoginClickEvents.ON_PASSWORD_TOGGLE -> handlePasswordToggle(vm)
            LoginClickEvents.ON_LOGIN_CLICK -> navigateToDashboard()
            LoginClickEvents.ON_FORGET_PASSWORD_CLICK -> navigateToForgotPassword()
            LoginClickEvents.ON_FETCH_CURRENT_LATITUDE_LONGITUDE_CLICKS -> startLocationPermission()
            LoginClickEvents.ON_LOGIN_WITH_OTP_CLICK -> requestOtp()
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
                alertDialogShow(
                    this,
                    getString(R.string.alert),
                    response.throwable?.message.orEmpty()
                )
            }
        }
    }

    private fun handleLoginSuccess(response: ApiState<LoginResponse>) {
        // Accepted: the ViewModel saves the session and registers the push token; the progress
        // dialog stays up until loginCompleted.
        if (viewModel.isAccepted(response.response)) return
        dismissProgress()
        response.response?.let {
            alertDialogShow(
                this,
                getString(R.string.alert),
                it.message.orEmpty()
            ) { dialog, _ ->
                dialog.dismiss()
            }
        }
    }

    private fun onLoginCompleted() {
        getOtpBottomSheet?.dismiss()
        getOtpBottomSheet = null
        dismissProgress()
        showToast(this, getString(R.string.login_success))
        navigateToDashboard()
    }

    private fun handleRequestOtpResponse(response: ApiState<RequestOtpResponse>) {
        when (response.status) {
            Status.SUCCESS -> {
                handleRequestOtpSuccess(response)
            }

            Status.LOADING -> {
                showProgress()
            }

            Status.ERROR -> {
                dismissProgress()
                alertDialogShow(
                    this,
                    getString(R.string.alert),
                    response.throwable?.message.orEmpty()
                )
            }
        }
    }

    private fun handleRequestOtpSuccess(response: ApiState<RequestOtpResponse>) {
        dismissProgress()
        response.response?.let {
            if (it.code == ValConstants.SUCCESS_CODE && it.success) {
                showToast(this, it.message.orEmpty())
                getOtpBottomSheet = GetOTPBottomSheetDialog().apply {
                    onSubmitOTP = { otp -> viewModel.verifyLoginWithOtp(otp) }
                    onResendOTP = { viewModel.requestLoginWithOtp() }
                }
                getOtpBottomSheet?.show(supportFragmentManager, "GetOTPBottomSheetDialog")
            } else {
                alertDialogShow(
                    this,
                    getString(R.string.alert),
                    it.message.orEmpty()
                ) { dialog, _ ->
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

    /** Every role opens the same dashboard, which picks its tabs from the role. */
    private fun navigateToDashboard() {
        Utils.jumpActivity(this, DashboardActivity::class.java)
        finish()
    }

    private fun navigateToForgotPassword() {
        Utils.jumpActivity(this, ForgotPasswordActivity::class.java)
    }

    /**
     * Only foreground (fine/coarse) location is needed here. Play's Prominent Disclosure policy
     * applies on every Android version, so the disclosure always precedes the system prompt.
     */
    private fun startLocationPermission() {
        if (hasAllPermissions(getForegroundLocationPermissions())) {
            startLocationPermissionFlow()
            return
        }
        Utils.showLocationDisclosureDialog(
            this,
            getString(R.string.share_current_location),
            getString(R.string.share_location_msg),
            onContinue = { startLocationPermissionFlow() }
        )
    }

    private fun startLocationPermissionFlow() {
        val foreground = getForegroundLocationPermissions()
        when {
            hasAllPermissions(foreground) -> getCurrentLatitudeLongitude()
            foreground.any { shouldShowRequestPermissionRationale(it) } -> showPermissionRationale()
            else -> permissionLauncherForeground.launch(foreground)
        }
    }

    private fun getForegroundLocationPermissions(): Array<String> =
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

    @SuppressLint("MissingPermission")
    private fun getCurrentLatitudeLongitude() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val latitude = location?.latitude
            val longitude = location?.longitude
            lat = latitude ?: 0.0
            long = longitude ?: 0.0
            if (latitude != null && longitude != null) {
                Utils.getAddressFromLatLong(this, lat, long) { addressFromLatLon ->
                    alertDialogShow(
                        this,
                        getString(R.string.current_location),
                        getString(R.string.current_location_details, lat.toString(), long.toString(), addressFromLatLon)
                    )
                }
            } else {
                alertDialogShow(
                    this,
                    getString(R.string.alert),
                    getString(R.string.unable_to_fetch_location)
                )
            }
        }.addOnFailureListener {
            lat = 0.0
            long = 0.0
            alertDialogShow(
                this,
                getString(R.string.alert),
                getString(R.string.unable_to_fetch_location)
            )
        }
    }

    private fun hasAllPermissions(permissions: Array<String>): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private val permissionLauncherForeground = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        when {
            result.all { it.value } -> getCurrentLatitudeLongitude()
            !result.any { shouldShowRequestPermissionRationale(it.key) } -> showPermissionDeniedPermanently()
            else -> showPermissionRationale()
        }
    }

    private fun showPermissionRationale() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.location_permission_rationale)
            .setPositiveButton(R.string.retry) { _, _ ->
                openApplicationSettings()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPermissionDeniedPermanently() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_denied)
            .setMessage(R.string.permission_denied_permanently)
            .setPositiveButton(R.string.open_settings) { _, _ -> openApplicationSettings() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun openApplicationSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = android.net.Uri.fromParts("package", this.packageName, null)
        startActivity(intent)
    }

    private fun requestOtp() {
        val requestOtpBottomSheet = RequestOtpBottomSheet().apply {
            onSubmitEmail = { email ->
                viewModel.userEmailId.set(email)
                viewModel.requestLoginWithOtp()
            }
        }
        requestOtpBottomSheet.show(supportFragmentManager, "RequestOtpBottomSheet")
    }

}
