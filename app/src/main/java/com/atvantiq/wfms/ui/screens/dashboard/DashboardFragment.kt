package com.atvantiq.wfms.ui.screens.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.FragmentDashboardBinding
import com.atvantiq.wfms.models.attendance.CheckInOutResponse
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.models.empDetail.EmpData
import com.atvantiq.wfms.models.empDetail.EmpDetailResponse
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.DashboardPagerAdapter
import com.atvantiq.wfms.ui.screens.announcements.AnnouncementsActivity
import com.atvantiq.wfms.ui.screens.attendance.applyLeave.ApplyLeaveActivity
import com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance.AttendanceCommunicationViewModel
import com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance.AttendanceStatusFragment
import com.atvantiq.wfms.ui.screens.dashboard.tabs.myTargets.MyTargetsFragment
import com.atvantiq.wfms.ui.screens.dashboard.tabs.projectDashboard.ProjectDashboardFragment
import com.atvantiq.wfms.utils.Utils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ncorti.slidetoact.SlideToActView
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException

@AndroidEntryPoint
class DashboardFragment : BaseFragment<FragmentDashboardBinding, DashboardViewModel>() {

    private var isDayStarted = false
    private var attendanceActionInFlight = false
    private var pendingCheckoutLocation: Pair<Double, Double>? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val communicationViewModel: AttendanceCommunicationViewModel by activityViewModels()

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_dashboard, DashboardViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val startColor = MaterialColors.getColor(view, R.attr.wfmsColorPrimaryDark)
        val endColor   = MaterialColors.getColor(view, R.attr.wfmsColorGradientEnd)
        binding.appDashHeader.root.background = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(startColor, endColor)
        )
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        setupTabBar()
        setupSwipeButton()

        PrefMethods.getEmpDetailResponse(prefMain)?.let {
            setupUserData(it)
        } ?: viewModel.getEmpDetails()
    }

    override fun subscribeToEvents(vm: DashboardViewModel) {
        binding.vm = vm

        vm.clickEvents.observe(viewLifecycleOwner) {
            if (!isLifeCycleResumed()) return@observe
            when (it) {
                DashboardClickEvents.onAnnouncementsClicks -> Utils.jumpActivity(requireContext(), AnnouncementsActivity::class.java)

                DashboardClickEvents.onFetchCurrentLatitudeLongitudeClicks -> {
                    startCurrentLocationPermissionFlow()
                }
                DashboardClickEvents.OPEN_SITES_CLICK,
                DashboardClickEvents.OPEN_SITES_APPROVALS_CLICK,
                DashboardClickEvents.OPEN_CLAIM_APPROVALS_CLICK,
                DashboardClickEvents.OPEN_PROFILE_CLICK,
                DashboardClickEvents.CHANGE_THEME_CLICK -> {
                    showToast(requireContext(), getString(R.string.under_development))
                }
                DashboardClickEvents.LOGOUT_CLICK -> logoutUser()
                DashboardClickEvents.APPLY_LEAVE_CLICK -> {
                    Utils.jumpActivity(requireContext(), ApplyLeaveActivity::class.java)
                }
                null -> Unit
            }
        }

        vm.empDetailsResponse.observe(viewLifecycleOwner) { response ->
            if (isLifeCycleResumed()) {
                when (response.status) {
                    Status.SUCCESS -> handleEmpDetailsResponse(response.response)
                    Status.ERROR -> handleError(response.throwable, response.response?.message)
                    Status.LOADING -> { /* showProgress() if needed */ }
                }
            }
        }

        vm.attendanceCheckInResponse.observe(viewLifecycleOwner) { response ->
            if (isLifeCycleResumed()) {
                when (response.status) {
                    Status.SUCCESS -> handleCheckInResponse(response.response)
                    Status.ERROR -> handleError(response.throwable, response.response?.message)
                    Status.LOADING -> showProgress()
                }
            }
        }

        vm.attendanceCheckOutResponse.observe(viewLifecycleOwner) { response ->
            if (isLifeCycleResumed()) {
                when (response.status) {
                    Status.SUCCESS -> handleCheckOutResponse(response.response)
                    Status.ERROR -> handleError(response.throwable, response.response?.message)
                    Status.LOADING -> showProgress()
                }
            }
        }

        vm.attendanceCheckInStatusResponse.observe(viewLifecycleOwner) { response ->
            if (isLifeCycleResumed()){
                when (response.status) {
                    Status.SUCCESS -> handleCheckInStatusResponse(response.response)
                    Status.ERROR -> handleCheckInStatusError(response.response?.message,response.throwable)
                    Status.LOADING -> {
                        showProgress()
                    }
                }
            }
        }

        vm.attendanceRemarksResponse.observe(viewLifecycleOwner) { response ->
            if (isLifeCycleResumed()) {
                when (response.status) {
                    Status.SUCCESS -> {
                        dismissProgress()
                        if (response.response?.code == ValConstants.SUCCESS_CODE) {
                            val location = pendingCheckoutLocation
                            if (location != null) {
                                attendanceActionInFlight = true
                                viewModel.checkOutAttendance(location.first, location.second, true)
                            } else {
                                checkInAttendanceStatus()
                            }
                        }
                        showToast(requireContext(), response.response?.message ?: getString(R.string.something_went_wrong))
                    }
                    Status.ERROR -> handleError(response.throwable, response.response?.message)
                    Status.LOADING -> showProgress()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkInAttendanceStatus()
    }

    private fun logoutUser() {
        alertDialogShow(
            requireContext(),
            getString(R.string.logout),
            getString(R.string.logout_confirmation),
            getString(R.string.yes),
            { dialog, _ ->
                dialog.dismiss()
                performLogout()
            },
            { dialog, _ -> dialog.dismiss() }
        )
    }

    private fun handleEmpDetailsResponse(empDetailResponse: EmpDetailResponse?) {
        dismissProgress() // Ensure progress is dismissed before showing any message
        when (empDetailResponse?.code) {
            ValConstants.SUCCESS_CODE -> {
                PrefMethods.saveEmpDetailResponse(prefMain, empDetailResponse.data)
                setupUserData(empDetailResponse.data)
            }
            ValConstants.UNAUTHORIZED_CODE -> tokenExpiresAlert()
            else -> alertDialogShow(requireContext(), getString(R.string.alert), empDetailResponse?.message ?: getString(R.string.something_went_wrong))
        }
    }

    private fun handleCheckInResponse(response: CheckInOutResponse?) = with(binding.appDashHeader) {
        dismissProgress()
        resetAttendanceAction()
        when (response?.code) {
            ValConstants.SUCCESS_CODE -> {
                isDayStarted = true
                updateSlideButton(true)
                communicationViewModel.triggerCalendarRefresh()
                checkPermissionForLiveLocation()
            }
            ValConstants.UNAUTHORIZED_CODE -> tokenExpiresAlert()
            else -> alertDialogShow(requireContext(), getString(R.string.alert), response?.message ?: getString(R.string.something_went_wrong))
        }
    }

    private fun handleCheckOutResponse(response: CheckInOutResponse?) = with(binding.appDashHeader) {
        dismissProgress()
        resetAttendanceAction()
        when (response?.code) {
            ValConstants.SUCCESS_CODE -> {
              pendingCheckoutLocation = null
              performCheckOut()
              communicationViewModel.triggerCalendarRefresh()
            }
            3001 ->{
                handleNoWorkForDay(response.data?.attendanceId)
            }
            ValConstants.UNAUTHORIZED_CODE -> tokenExpiresAlert()
            else -> alertDialogShow(requireContext(), getString(R.string.alert), response?.message ?: getString(R.string.something_went_wrong))
        }
    }

    private fun performCheckOut(){
        isDayStarted = false
        updateSlideButton(false)
        viewModel.stopTracking()
    }

    private fun handleCheckInStatusResponse(response: CheckInStatusResponse?) = with(binding.appDashHeader) {
        dismissProgress()
        when (response?.code) {
            ValConstants.SUCCESS_CODE -> {
                isDayStarted = response.data?.checkedIn == true
                updateSlideButton(isDayStarted)
                if(isDayStarted) checkPermissionForLiveLocation()
            }
            ValConstants.UNAUTHORIZED_CODE -> tokenExpiresAlert()
            ValConstants.BAD_REQUEST_CODE -> alertDialogShow(requireContext(), getString(R.string.alert), response.message)
            else -> handleCheckInStatusError(response?.message, null)
        }
    }

    private fun handleError(throwable: Throwable?, message: String?) {
        dismissProgress()
        resetAttendanceAction()
        if (throwable is HttpException && throwable.code() ==ValConstants.UNAUTHORIZED_CODE) {
            tokenExpiresAlert()
        } else {
            alertDialogShow(requireContext(), getString(R.string.alert), message ?: throwable?.message ?: getString(R.string.something_went_wrong))
        }
    }

    private fun handleCheckInStatusError(message: String?, throwable: Throwable?) {
        dismissProgress() // Ensure progress is dismissed before showing error dialog
        if (throwable is HttpException) {
            when (throwable.code()) {
                ValConstants.UNAUTHORIZED_CODE -> tokenExpiresAlert()
                ValConstants.SERVER_ERROR_CODE -> alertDialogShow(
                    requireContext(),
                    throwable.message ?: getString(R.string.something_went_wrong)
                )
                else -> alertDialogShow(
                    requireContext(),
                    getString(R.string.alert),
                    message ?: getString(R.string.something_went_wrong),
                    getString(R.string.retry),
                    DialogInterface.OnClickListener { _, _ -> checkInAttendanceStatus() },
                    false
                )
            }
        } else {
            alertDialogShow(
                requireContext(),
                getString(R.string.alert),
                message ?: getString(R.string.something_went_wrong),
                getString(R.string.retry),
                DialogInterface.OnClickListener { _, _ -> checkInAttendanceStatus() },
                false
            )
        }
    }

    private fun handleNoWorkForDay(attendanceId: Long?) {
        alertDialogShow(requireContext(),
            getString(R.string.no_work_started_title),
            getString(R.string.no_work_started_message),
            getString(R.string.enter_work_details),
            DialogInterface.OnClickListener { dialog, _ ->
                dialog.dismiss()
                findNavController().navigate(R.id.nav_attendance)
            },
            getString(R.string.mark_idle),
            DialogInterface.OnClickListener { _, _ ->
                showRemarksDialog(attendanceId ?: 0L)
            }
        )
    }

    private fun showRemarksDialog(attendanceId: Long) {
        val remarksBottomSheet = AttendanceRemarksBottomSheet.newInstance().apply {
            onSubmitDetails = { remarks ->
                viewModel.setAttendanceEmpRemarks(attendanceId, remarks)
            }
        }
        remarksBottomSheet.show(parentFragmentManager, "AttendanceRemarksBottomSheet")
    }

    private fun updateSlideButton(isStarted: Boolean) = with(binding.appDashHeader) {
        if (isStarted) {
            slideStartDay.text = getString(R.string.end_day)
            slideStartDay.outerColor = ContextCompat.getColor(requireContext(), R.color.red)
            slideStartDay.isReversed = true
        } else {
            slideStartDay.text = getString(R.string.start_day)
            slideStartDay.outerColor = MaterialColors.getColor(slideStartDay, R.attr.wfmsColorPrimary)
            slideStartDay.isReversed = false
        }
    }


    private fun checkInAttendanceStatus() {
        viewModel.checkInStatusAttendance()
    }

    private fun resetAttendanceAction() {
        attendanceActionInFlight = false
        binding.appDashHeader.slideStartDay.setCompleted(false, true)
    }

    @SuppressLint("MissingPermission")
    private fun manageDayStartEnd() {
        if (!isDeviceLocationEnabled()) {
            resetAttendanceAction()
            showLocationDisabledDialog()
            return
        }
        getAttendanceActionLocation { location ->
                if (location == null) {
                    resetAttendanceAction()
                    alertDialogShow(
                        requireContext(),
                        getString(R.string.alert),
                        getString(R.string.unable_to_fetch_location)
                    )
                    return@getAttendanceActionLocation
                }

                val lat = location.latitude
                val lon = location.longitude

                attendanceActionInFlight = true
                if (isDayStarted) {
                    pendingCheckoutLocation = lat to lon
                    viewModel.checkOutAttendance(lat, lon, true)
                } else {
                    pendingCheckoutLocation = null
                    viewModel.checkInAttendance(lat, lon)
                }
        }
    }

    private fun isDeviceLocationEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showLocationDisabledDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.permission_required)
            .setMessage(R.string.location_permission_rationale)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun getAttendanceActionLocation(onResult: (Location?) -> Unit) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { cachedLocation ->
                if (isUsableAttendanceLocation(cachedLocation)) {
                    onResult(cachedLocation)
                    return@addOnSuccessListener
                }

                val cancellationTokenSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { freshLocation ->
                    onResult(freshLocation)
                }.addOnFailureListener {
                    onResult(null)
                }
            }.addOnFailureListener {
                resetAttendanceAction()
                onResult(null)
            }
    }

    private fun isUsableAttendanceLocation(location: Location?): Boolean {
        if (location == null) return false
        val maxAgeMillis = 2 * 60 * 1000L
        val maxAccuracyMeters = 100f
        val ageMillis = System.currentTimeMillis() - location.time
        return ageMillis in 0..maxAgeMillis && location.accuracy <= maxAccuracyMeters
    }

    private fun setupUserData(userData: EmpData?) {
        if (userData == null) return
        setGeofenceLocation(userData.officialLocation?.latitude ?: 0.0, userData.officialLocation?.longitude ?: 0.0)
        binding.appDashHeader.userData = userData
    }

    private fun setGeofenceLocation(lat: Double, lon: Double) {
        viewModel.GEOFENCE_LAT.set(lat)
        viewModel.GEOFENCE_LON.set(lon)
    }

    private fun setupSwipeButton() {
        binding.appDashHeader.slideStartDay.onSlideCompleteListener =
            object : SlideToActView.OnSlideCompleteListener {
                override fun onSlideComplete(view: SlideToActView) {
                    if (attendanceActionInFlight) {
                        binding.appDashHeader.slideStartDay.setCompleted(false, true)
                        return
                    }
                    val permissions = getRequiredPermissions()
                    when {
                        hasAllPermissions(permissions) -> manageDayStartEnd()
                        permissions.any { shouldShowRequestPermissionRationale(it) } -> {
                            binding.appDashHeader.slideStartDay.setCompleted(false, true)
                            Utils.showBackgroundLocationDisclosureDialog(requireContext(),getString(R.string.background_location_usage),getString(R.string.background_location_usage_msg)) {
                                permissionLauncher.launch(permissions)
                            }
                        }
                        else -> {
                            binding.appDashHeader.slideStartDay.setCompleted(false, true)
                            Utils.showBackgroundLocationDisclosureDialog(requireContext(),getString(R.string.background_location_usage),getString(R.string.background_location_usage_msg)) {
                                permissionLauncher.launch(permissions)
                            }
                        }
                    }
                }
            }
    }

    private fun setupTabBar() {
        val pages = listOf(
            DashboardPagerAdapter.Page(key = "attendance") { AttendanceStatusFragment() },
            DashboardPagerAdapter.Page(key = "targets") { MyTargetsFragment() },
            DashboardPagerAdapter.Page(key = "projects") { ProjectDashboardFragment() }
        )
        binding.viewPager.adapter = DashboardPagerAdapter(requireActivity(), pages)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.attendance)
                1 -> getString(R.string.my_targets)
                2 -> getString(R.string.projects)
                else -> getString(R.string.attendance)
            }
        }.attach()
    }


    private val permissionLauncherCurrentLocation = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.all { it.value } -> {
                getCurrentLatitudeLongitude()
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


    private fun openApplicationSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = android.net.Uri.fromParts("package", requireContext().packageName, null)
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

    private fun getForegroundRequiredPermissions(): Array<String> {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return list.toTypedArray()
    }

    private fun getRequiredPermissions(): Array<String> {
        val list = getForegroundRequiredPermissions().toMutableList()

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
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

    private fun showPermissionRationale() {
        MaterialAlertDialogBuilder(requireContext())
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
        MaterialAlertDialogBuilder(requireContext())
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

    private fun startCurrentLocationPermissionFlow() {
        val foreground = getForegroundRequiredPermissions()
        when {
            hasAllPermissions(foreground) -> {
                getCurrentLatitudeLongitude()
            }
            foreground.any { shouldShowRequestPermissionRationale(it) } -> showPermissionRationale()
            else -> {
                Utils.showBackgroundLocationDisclosureDialog(requireContext(),getString(R.string.share_current_location),getString(R.string.share_location_msg)) {
                    permissionLauncherCurrentLocation.launch(foreground)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLatitudeLongitude() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            val latitude = location?.latitude
            val longitude = location?.longitude
            val lat = latitude ?: 0.0
            val lon = longitude ?: 0.0
            if (latitude != null && longitude != null) {
                Utils.getAddressFromLatLong(requireContext(), lat, lon) { addressFromLatLon ->
                    requireActivity().runOnUiThread {
                        alertDialogShow(
                            requireContext(),
                            getString(R.string.current_location),
                            "${getString(R.string.Latitude)}: $lat\n${getString(R.string.Longitude)}: $lon\n\n${getString(R.string.Address)}: $addressFromLatLon"
                        )
                    }
                }
            } else {
                alertDialogShow(
                    requireContext(),
                    getString(R.string.alert),
                    getString(R.string.unable_to_fetch_location)
                )
            }
        }.addOnFailureListener {
            alertDialogShow(requireContext(), getString(R.string.alert), getString(R.string.unable_to_fetch_location))
        }
    }
}
