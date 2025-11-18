package com.atvantiq.wfms.ui.screens.admin

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ActivitySharedDashboardBinding
import com.atvantiq.wfms.models.attendance.CheckInOutResponse
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.models.empDetail.EmpDetailResponse
import com.atvantiq.wfms.models.loginResponse.User
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.admin.ui.site.SitesActivity
import com.atvantiq.wfms.ui.screens.admin.ui.siteApproval.WorkSitesApprovalActivity
import com.atvantiq.wfms.ui.screens.dashboard.DashboardClickEvents
import com.atvantiq.wfms.ui.screens.dashboard.DashboardViewModel
import com.atvantiq.wfms.ui.screens.login.LoginActivity
import com.atvantiq.wfms.utils.Utils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging
import com.ncorti.slidetoact.SlideToActView
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException

@AndroidEntryPoint
class SharedDashboardActivity : BaseActivity<ActivitySharedDashboardBinding,DashboardViewModel>() {

    private var isDayStarted = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var lat: Double = 0.0
    var long: Double = 0.0
    companion object {
        private const val GEOFENCE_RADIUS_METERS = 500
    }

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_shared_dashboard, DashboardViewModel::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        var userData = PrefMethods.getUserData(prefMain)
        viewModel.getEmpDetails()
        setupHeaderData(userData)
        setupSwipeButton()
    }

    private fun setupHeaderData(userData: User?) {
        if (userData == null)
            return
        setGeofenceLocation(userData.officialLocation.latitude ?: 0.0, userData.officialLocation.longitude ?: 0.0)
        binding.userNameString = (userData.firstName ?: "") + " " + (userData.lastName ?: "")
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

    override fun subscribeToEvents(vm: DashboardViewModel) {
        binding.vm = vm
        vm.clickEvents.observe(this) { event ->
            when (event) {
                DashboardClickEvents.OPEN_SITES_CLICK -> {
                    Utils.jumpActivity(this, SitesActivity::class.java)
                }
                DashboardClickEvents.OPEN_SITES_APPROVALS_CLICK -> {
                    Utils.jumpActivity(this, WorkSitesApprovalActivity::class.java)
                }
                DashboardClickEvents.OPEN_CLAIM_APPROVALS_CLICK -> {

                }
                DashboardClickEvents.OPEN_PROFILE_CLICK -> {

                }

                DashboardClickEvents.LOGOUT_CLICK ->{
                    logoutUser()
                }

                DashboardClickEvents.onAnnouncementsClicks -> {
                }
                DashboardClickEvents.onFetchCurrentLatitudeLongitudeClicks -> {
                    getCurrentLatitudeLongitudePermissions()
                }
            }
        }

        vm.empDetailsResponse.observe(this) { response ->
            if (isLifeCycleResumed()) {
                when (response.status) {
                    Status.SUCCESS -> handleEmpDetailsResponse(response.response)
                    Status.ERROR -> handleError(response.throwable, response.response?.message)
                    Status.LOADING -> { /* showProgress() if needed */ }
                }
            }
        }

        vm.attendanceCheckInResponse.observe(this) { response ->
            if (isLifeCycleResumed()) {
                when (response.status) {
                    Status.SUCCESS -> handleCheckInResponse(response.response)
                    Status.ERROR -> handleError(response.throwable, response.response?.message)
                    Status.LOADING -> showProgress()
                }
                binding.slideStartDay.setCompleted(false, true)
            }
        }

        vm.attendanceCheckOutResponse.observe(this) { response ->
            if (isLifeCycleResumed()) {
                when (response.status) {
                    Status.SUCCESS -> handleCheckOutResponse(response.response)
                    Status.ERROR -> handleError(response.throwable, response.response?.message)
                    Status.LOADING -> showProgress()
                }
                binding.slideStartDay.setCompleted(false, true)
            }
        }

        vm.attendanceCheckInStatusResponse.observe(this) { response ->
            if (isLifeCycleResumed()){
                when (response.status) {
                    Status.SUCCESS -> handleCheckInStatusResponse(response.response)
                    Status.ERROR -> handleCheckInStatusError(response.response?.message)
                    Status.LOADING -> {
                        showProgress()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkInAttendanceStatus()
    }

    private fun handleEmpDetailsResponse(empDetailResponse: EmpDetailResponse?) {
        dismissProgress()
        when (empDetailResponse?.code) {
            ValConstants.SUCCESS_CODE -> {
                setGeofenceLocation(empDetailResponse.data?.officialLocation?.latitude ?: 0.0,
                    empDetailResponse.data?.officialLocation?.longitude ?: 0.0)
            }
            ValConstants.UNAUTHORIZED_CODE -> tokenExpiresAlert()
            else -> alertDialogShow(this, getString(R.string.alert), empDetailResponse?.message ?: getString(R.string.something_went_wrong))
        }
    }

    private fun handleCheckInResponse(response: CheckInOutResponse?) = runOnUiThread {
        dismissProgress()
        when (response?.code) {
            ValConstants.SUCCESS_CODE -> {
                isDayStarted = true
                updateSlideButton(true)
                checkPermissionForLiveLocation()
            }
            ValConstants.UNAUTHORIZED_CODE -> tokenExpiresAlert()
            else -> alertDialogShow(this, getString(R.string.alert), response?.message ?: getString(
                R.string.something_went_wrong))
        }
    }

    private fun handleCheckOutResponse(response: CheckInOutResponse?) = runOnUiThread {
        dismissProgress()
        when (response?.code) {
            ValConstants.SUCCESS_CODE -> {
                isDayStarted = false
                updateSlideButton(false)
                viewModel.stopTracking()
            }
            ValConstants.UNAUTHORIZED_CODE -> tokenExpiresAlert()
            else -> alertDialogShow(this, getString(R.string.alert), response?.message ?: getString(
                R.string.something_went_wrong))
        }
    }

    private fun handleCheckInStatusResponse(response: CheckInStatusResponse?) = runOnUiThread {
        dismissProgress()
        when (response?.code) {
            ValConstants.SUCCESS_CODE -> {
                isDayStarted = response.data?.checkedIn == true
                updateSlideButton(isDayStarted)
                if(isDayStarted) checkPermissionForLiveLocation()
            }
            ValConstants.UNAUTHORIZED_CODE -> tokenExpiresAlert()
            ValConstants.BAD_REQUEST_CODE -> alertDialogShow(this, getString(R.string.alert), response.message
                ?: getString(R.string.something_went_wrong))
            else -> handleCheckInStatusError(response?.message)
        }
    }

    private fun handleError(throwable: Throwable?, message: String?) {
        dismissProgress()
        if (throwable is HttpException && throwable.code() ==ValConstants.UNAUTHORIZED_CODE) {
            tokenExpiresAlert()
        } else {
            alertDialogShow(this, getString(R.string.alert), message ?: throwable?.message ?: getString(R.string.something_went_wrong))
        }
    }

    private fun handleCheckInStatusError(message: String?) {
        dismissProgress() // Ensure progress is dismissed before showing error dialog
        alertDialogShow(
            this,
            getString(R.string.alert),
            message ?: getString(R.string.something_went_wrong),
            getString(R.string.retry),
            DialogInterface.OnClickListener { _, _ -> checkInAttendanceStatus() },
        )
    }

    private fun updateSlideButton(isStarted: Boolean) = runOnUiThread {
        if (isStarted) {
            binding.slideStartDay.text = getString(R.string.end_day)
            binding.slideStartDay.outerColor = ContextCompat.getColor(this, R.color.red)
            binding.slideStartDay.isReversed = true
        } else {
            binding.slideStartDay.text = getString(R.string.start_day)
            binding.slideStartDay.outerColor = ContextCompat.getColor(this, R.color.colorPrimaryDark)
            binding.slideStartDay.isReversed = false
        }
    }

    private fun checkInAttendanceStatus() {
        viewModel.checkInStatusAttendance()
    }

    @SuppressLint("MissingPermission")
    private fun isWithinGeofence(onResult: (Boolean) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            lat = location?.latitude ?: 0.0
            long = location?.longitude ?: 0.0
            val distance = FloatArray(1)
            viewModel.GEOFENCE_LAT.get()?.let {
                viewModel.GEOFENCE_LON.get()?.let { it1 ->
                    Location.distanceBetween(
                        lat, long,
                        it, it1,
                        distance
                    )
                }
            }
            onResult(distance[0] <= GEOFENCE_RADIUS_METERS)
        }.addOnFailureListener {
            lat = 0.0
            long = 0.0
            onResult(false)
        }
    }

    private fun setGeofenceLocation(lat: Double, lon: Double) {
        viewModel.GEOFENCE_LAT.set(lat)
        viewModel.GEOFENCE_LON.set(lon)
    }

    private fun setupSwipeButton() {
        binding.slideStartDay.onSlideCompleteListener =
            object : SlideToActView.OnSlideCompleteListener {
                override fun onSlideComplete(view: SlideToActView) {
                    val permissions = getRequiredPermissions()
                    when {
                        hasAllPermissions(permissions) -> manageDayStartEnd()
                        permissions.any { shouldShowRequestPermissionRationale(it) } -> {
                            binding.slideStartDay.setCompleted(false, true)
                            showPermissionDeniedPermanently()
                        }
                        else -> {
                            binding.slideStartDay.setCompleted(false, true)
                            permissionLauncher.launch(permissions)
                        }
                    }
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun manageDayStartEnd() {
        if (isDayStarted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                lat = location?.latitude ?: 0.0
                long = location?.longitude ?: 0.0
                viewModel.checkOutAttendance(lat, long)
            }.addOnFailureListener {
                lat = 0.0
                long = 0.0
            }
        } else {
            isWithinGeofence { isWithin ->
                if (isWithin) {
                    checkPermissionsAndUpdateGeofence()
                } else {
                    binding.slideStartDay.setCompleted(false, true)
                    alertDialogShow(this, getString(R.string.home_location_check))
                }
            }
        }
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

    private val permissionLauncherLocationTracking = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.all { it.value } -> viewModel.startTracking()
            !permissions.any { shouldShowRequestPermissionRationale(it.key) } -> showPermissionDeniedPermanently()
            else -> showPermissionRationale()
        }
    }

    private val permissionLauncherGeofencing = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.all { it.value } -> viewModel.checkInAttendance(lat, long)
            !permissions.any { shouldShowRequestPermissionRationale(it.key) } -> showPermissionDeniedPermanently()
            else -> showPermissionRationale()
        }
    }

    private fun openApplicationSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = android.net.Uri.fromParts("package", this.packageName, null)
        startActivity(intent)
    }

    private fun checkPermissionForLiveLocation() {
        val permissions = getRequiredPermissions()
        when {
            hasAllPermissions(permissions) -> viewModel.startTracking()
            permissions.any { shouldShowRequestPermissionRationale(it) } -> showPermissionRationale()
            else -> permissionLauncherLocationTracking.launch(permissions)
        }
    }

    private fun checkPermissionsAndUpdateGeofence() {
        val permissions = getRequiredPermissions()
        if (hasAllPermissions(permissions)) {
            viewModel.checkInAttendance(lat, long)
        } else {
            permissionLauncherGeofencing.launch(permissions)
        }
    }

    private fun getLocationRequiredPermissions(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return list.toTypedArray()
    }

    private fun getRequiredPermissions(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            list.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.toTypedArray()
    }

    private fun hasAllPermissions(permissions: Array<String>): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
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

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.all { it.value } -> manageDayStartEnd()
            permissions.any { shouldShowRequestPermissionRationale(it.key) } -> showPermissionDeniedPermanently()
            else -> showPermissionDeniedPermanently()
        }
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
                            this.runOnUiThread {
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
                    alertDialogShow(this, getString(R.string.alert), getString(R.string.unable_to_fetch_location))
                }
            }
            permissions.any { shouldShowRequestPermissionRationale(it) } -> showPermissionRationale()
            else -> permissionLauncherCurrentLatLon.launch(permissions)
        }
    }

}