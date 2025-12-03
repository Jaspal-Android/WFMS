package com.atvantiq.wfms.ui.screens.login

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ActivityLoginBinding
import com.atvantiq.wfms.models.loginResponse.LoginResponse
import com.atvantiq.wfms.models.notification.UpdateNotificationTokenResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.DashboardActivity
import com.atvantiq.wfms.ui.screens.admin.SharedDashboardActivity
import com.atvantiq.wfms.ui.screens.forgotPassword.ForgotPasswordActivity
import com.atvantiq.wfms.utils.Utils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding, LoginVM>() {

    var lat: Double = 0.0
    var long: Double = 0.0
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_login, LoginVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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
            LoginClickEvents.ON_FETCH_CURRENT_LATITUDE_LONGITUDE_CLICKS -> getCurrentLatitudeLongitudePermissions()
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
                val user = it.data?.user
                viewModel.user = user
                FirebaseMessaging.getInstance().token
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val token = task.result
                            viewModel.sendNotificationToken(user?.userId.toString(), token)
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
        val role = viewModel.user?.role ?: ""
        val permissions = viewModel?.user?.permissions

        if (role.equals(ValConstants.ROLE_EMPLOYEE, ignoreCase = true)) {
            Utils.jumpActivityWithData(this, DashboardActivity::class.java,Bundle().apply {
                putParcelableArrayList(SharingKeys.ROLE_PERMISSIONS,permissions as ArrayList)
            })
        } else {
            Utils.jumpActivityWithData(this, SharedDashboardActivity::class.java,Bundle().apply {
                putParcelableArrayList(SharingKeys.ROLE_PERMISSIONS,permissions as ArrayList)
            })
        }
        finish()
    }

    private fun navigateToForgotPassword() {
        Utils.jumpActivity(this, ForgotPasswordActivity::class.java)
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLatitudeLongitudePermissions() {
        val permissions = getLocationRequiredPermissions()
        when {
            hasAllPermissions(permissions) -> {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    val latitude = location?.latitude
                    val longitude = location?.longitude
                    lat = latitude ?: 0.0
                    long = longitude ?: 0.0
                    if (latitude != null && longitude != null) {
                        Utils.getAddressFromLatLong(this, lat, long) { addressFromLatLon ->
                            runOnUiThread {
                                alertDialogShow(
                                    this,
                                    getString(R.string.current_location),
                                    "${getString(R.string.Latitude)}: $lat\n${getString(R.string.Longitude)}: $long\n\n${getString(R.string.Address)}: $addressFromLatLon"
                                )
                            }
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
                    alertDialogShow(this
                        , getString(R.string.alert), getString(R.string.unable_to_fetch_location))
                }
            }
            permissions.any { shouldShowRequestPermissionRationale(it) } -> showPermissionRationale()
            else -> permissionLauncherCurrentLatLon.launch(permissions)
        }
    }

    private fun getLocationRequiredPermissions(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return list.toTypedArray()
    }

    private fun hasAllPermissions(permissions: Array<String>): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private val permissionLauncherCurrentLatLon = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.all { it.value } -> {
                getCurrentLatitudeLongitudePermissions()
            }
            !permissions.any { shouldShowRequestPermissionRationale(it.key) } -> showPermissionDeniedPermanently()
            else -> showPermissionRationale()
        }
    }

    private fun showPermissionRationale() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.location_permission_rationale)
            .setPositiveButton(R.string.retry) { _, _ ->
                //permissionLauncherLocationTracking.launch(getRequiredPermissions())
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

}
