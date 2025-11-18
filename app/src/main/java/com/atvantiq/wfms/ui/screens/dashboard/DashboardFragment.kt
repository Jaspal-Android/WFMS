package com.atvantiq.wfms.ui.screens.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.atvantiq.wfms.ui.screens.adapters.MarqueeAdapter
import com.atvantiq.wfms.ui.screens.announcements.AnnouncementsActivity
import com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance.AttendanceCommunicationViewModel
import com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance.AttendanceStatusFragment
import com.atvantiq.wfms.ui.screens.dashboard.tabs.myTargets.MyTargetsFragment
import com.atvantiq.wfms.ui.screens.dashboard.tabs.projectDashboard.ProjectDashboardFragment
import com.atvantiq.wfms.utils.Utils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.ncorti.slidetoact.SlideToActView
import com.ssas.jibli.data.prefs.PrefMethods
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException

@AndroidEntryPoint
class DashboardFragment : BaseFragment<FragmentDashboardBinding, DashboardViewModel>() {

    private var isDayStarted = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var lat: Double = 0.0
    var long: Double = 0.0

    private val communicationViewModel: AttendanceCommunicationViewModel by activityViewModels()

    companion object {
        private const val GEOFENCE_RADIUS_METERS = 500
    }

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_dashboard, DashboardViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {
        // No-op
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                    getCurrentLatitudeLongitudePermissions()
                }
                DashboardClickEvents.OPEN_SITES_CLICK -> TODO()
                DashboardClickEvents.OPEN_SITES_APPROVALS_CLICK -> TODO()
                DashboardClickEvents.OPEN_CLAIM_APPROVALS_CLICK -> TODO()
                DashboardClickEvents.OPEN_PROFILE_CLICK -> TODO()
                DashboardClickEvents.LOGOUT_CLICK -> TODO()
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
                binding.appDashHeader.slideStartDay.setCompleted(false, true)
            }
        }

        vm.attendanceCheckOutResponse.observe(viewLifecycleOwner) { response ->
            if (isLifeCycleResumed()) {
                when (response.status) {
                    Status.SUCCESS -> handleCheckOutResponse(response.response)
                    Status.ERROR -> handleError(response.throwable, response.response?.message)
                    Status.LOADING -> showProgress()
                }
                binding.appDashHeader.slideStartDay.setCompleted(false, true)
            }
        }

        vm.attendanceCheckInStatusResponse.observe(viewLifecycleOwner) { response ->
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
        when (response?.code) {
            ValConstants.SUCCESS_CODE -> {
                isDayStarted = false
                updateSlideButton(false)
                viewModel.stopTracking()
            }
            ValConstants.UNAUTHORIZED_CODE -> tokenExpiresAlert()
            else -> alertDialogShow(requireContext(), getString(R.string.alert), response?.message ?: getString(R.string.something_went_wrong))
        }
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
            ValConstants.BAD_REQUEST_CODE -> alertDialogShow(requireContext(), getString(R.string.alert), response.message ?: getString(R.string.something_went_wrong))
            else -> handleCheckInStatusError(response?.message)
        }
    }

    private fun handleError(throwable: Throwable?, message: String?) {
        dismissProgress()
        if (throwable is HttpException && throwable.code() ==ValConstants.UNAUTHORIZED_CODE) {
            tokenExpiresAlert()
        } else {
            alertDialogShow(requireContext(), getString(R.string.alert), message ?: throwable?.message ?: getString(R.string.something_went_wrong))
        }
    }

    private fun handleCheckInStatusError(message: String?) {
        dismissProgress() // Ensure progress is dismissed before showing error dialog
        alertDialogShow(
            requireContext(),
            getString(R.string.alert),
            message ?: getString(R.string.something_went_wrong),
            getString(R.string.retry),
            DialogInterface.OnClickListener { _, _ -> checkInAttendanceStatus() },
            false
        )
    }

    private fun updateSlideButton(isStarted: Boolean) = with(binding.appDashHeader) {
        if (isStarted) {
            slideStartDay.text = getString(R.string.end_day)
            slideStartDay.outerColor = ContextCompat.getColor(requireContext(), R.color.red)
            slideStartDay.isReversed = true
        } else {
            slideStartDay.text = getString(R.string.start_day)
            slideStartDay.outerColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
            slideStartDay.isReversed = false
        }
    }

    @SuppressLint("MissingPermission")
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

    /*    fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            lat = location?.latitude ?: 0.0
            long = location?.longitude ?: 0.0
        }.addOnFailureListener {
            lat = 0.0
            long = 0.0
        }*/
        setupTabBar()
        setupSwipeButton()
        horizontalScrollTextView()
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

    private fun setupUserData(userData: EmpData?) {
        if (userData == null) return
        setGeofenceLocation(userData?.officialLocation?.latitude ?: 0.0, userData?.officialLocation?.longitude ?: 0.0)
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
                    val permissions = getRequiredPermissions()
                    when {
                        hasAllPermissions(permissions) -> manageDayStartEnd()
                        permissions.any { shouldShowRequestPermissionRationale(it) } -> {
                            binding.appDashHeader.slideStartDay.setCompleted(false, true)
                            showPermissionDeniedPermanently()
                        }
                        else -> {
                            binding.appDashHeader.slideStartDay.setCompleted(false, true)
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
                    binding.appDashHeader.slideStartDay.setCompleted(false, true)
                    alertDialogShow(requireContext(), getString(R.string.home_location_check))
                }
            }
        }
    }

    private fun setupTabBar() {
        val adapter = DashboardPagerAdapter(requireActivity()).apply {
            addFragment(AttendanceStatusFragment())
            addFragment(MyTargetsFragment())
            addFragment(ProjectDashboardFragment())
        }
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.attendance_status)
                1 -> getString(R.string.my_targets)
                2 -> getString(R.string.projects)
                else -> getString(R.string.attendance_status)
            }
        }.attach()
    }

    private fun horizontalScrollTextView() {
        val items = listOf(
            "New year celebrations are coming soon.",
            "Report files must be submitted before december",
            "Reimbursement forms are open now."
        )
        val adapter = MarqueeAdapter(items)
        binding.appDashHeader.marqueeRecyclerView.adapter = adapter
        binding.appDashHeader.marqueeRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
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
                        Utils.getAddressFromLatLong(requireContext(), lat, long) { addressFromLatLon ->
                            requireActivity().runOnUiThread {
                                alertDialogShow(
                                    requireContext(),
                                    getString(R.string.current_location),
                                    "${getString(R.string.Latitude)}: $lat\n${getString(R.string.Longitude)}: $long\n\n${getString(R.string.Address)}: $addressFromLatLon"
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
                    lat = 0.0
                    long = 0.0
                    alertDialogShow(requireContext(), getString(R.string.alert), getString(R.string.unable_to_fetch_location))
                }
            }
            permissions.any { shouldShowRequestPermissionRationale(it) } -> showPermissionRationale()
            else -> permissionLauncherCurrentLatLon.launch(permissions)
        }
    }
}
