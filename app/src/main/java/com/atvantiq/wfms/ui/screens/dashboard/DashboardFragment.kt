package com.atvantiq.wfms.ui.screens.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentDashboardBinding
import com.atvantiq.wfms.models.loginResponse.User
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

    //private val GEOFENCE_LAT = 30.7046486
    private val GEOFENCE_LAT = 30.7149242

    //37.4220936
    private val GEOFENCE_LON = 76.7033762

    //private val GEOFENCE_LON = 76.7178726
    //-122.083922
    private val GEOFENCE_RADIUS_METERS = 500

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var lat: Double = 0.0
    var long: Double = 0.0

    private val communicationViewModel: AttendanceCommunicationViewModel by activityViewModels()

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_dashboard, DashboardViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun subscribeToEvents(vm: DashboardViewModel) {
        binding.vm = vm
        setupUserData()
        checkInAttendanceStatus()

        vm.clickEvents.observe(viewLifecycleOwner) {
            if (!isLifeCycleResumed()) return@observe
            when (it) {
                DashboardClickEvents.onAnnouncementsClicks -> {
                    Utils.jumpActivity(requireContext(), AnnouncementsActivity::class.java)
                }
            }

        }

        vm.attendanceCheckInResponse.observe(viewLifecycleOwner) { response ->
            if (isLifeCycleResumed()) {
                when (response.status) {
                    Status.SUCCESS -> {
                        dismissProgress()
                        if (response.response?.code == 200) {
                            isDayStarted = true
                            binding.appDashHeader.slideStartDay.text = getString(R.string.end_day)
                            binding.appDashHeader.slideStartDay.outerColor =
                                (ContextCompat.getColor(requireContext(), R.color.red))
                            binding.appDashHeader.slideStartDay.isReversed = true
                            communicationViewModel.triggerCalendarRefresh()
                            checkPermissionForLiveLocation()
                        } else if(response.response?.code == 401){
                            tokenExpiresAlert()
                        }else {
                            alertDialogShow(
                                requireContext(),
                                getString(R.string.alert),
                                response.response?.message
                                    ?: getString(R.string.something_went_wrong)
                            )
                        }
                        binding.appDashHeader.slideStartDay.setCompleted(false, true)
                    }

                    Status.ERROR -> {
                        dismissProgress()
                        isDayStarted = false
                        binding.appDashHeader.slideStartDay.setCompleted(false, true)

                        val throwable = response.throwable
                        if (throwable is HttpException) {
                            if (throwable.code() == 401) {
                                tokenExpiresAlert()
                            }
                        } else {
                            alertDialogShow(
                                requireContext(),
                                getString(R.string.alert),
                                response?.throwable?.message.toString()
                            )
                        }
                    }

                    Status.LOADING -> {
                        showProgress()
                    }
                }
            }

        }

        vm.attendanceCheckOutResponse.observe(viewLifecycleOwner) { response ->
            if (isLifeCycleResumed()) {
                when (response.status) {
                    Status.SUCCESS -> {
                        dismissProgress()
                        if (response.response?.code == 200) {
                            isDayStarted = false
                            binding.appDashHeader.slideStartDay.text = getString(R.string.start_day)
                            binding.appDashHeader.slideStartDay.outerColor =
                                (ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark))
                            binding.appDashHeader.slideStartDay.isReversed = false
                            viewModel.stopTracking()
                        }else if(response.response?.code == 401){
                            tokenExpiresAlert()
                        } else {
                            alertDialogShow(
                                requireContext(),
                                getString(R.string.alert),
                                response.response?.message
                                    ?: getString(R.string.something_went_wrong)
                            )
                        }
                        binding.appDashHeader.slideStartDay.setCompleted(false, true)
                    }

                    Status.ERROR -> {
                        dismissProgress()
                        isDayStarted = true
                        binding.appDashHeader.slideStartDay.setCompleted(false, true)

                        val throwable = response.throwable
                        if (throwable is HttpException) {
                            if (throwable.code() == 401) {
                                tokenExpiresAlert()
                            }
                        } else {
                            alertDialogShow(
                                requireContext(),
                                getString(R.string.alert),
                                response?.throwable?.message.toString()
                            )
                        }
                    }

                    Status.LOADING -> {
                        showProgress()
                    }
                }
            }
        }

        vm.attendanceCheckInStatusResponse.observe(viewLifecycleOwner) { response ->
            if(isLifeCycleResumed()){
                when (response.status) {
                    Status.SUCCESS -> {
                        dismissProgress()
                        if (response.response?.code == 200) {
                            if (response.response?.data?.checkedIn == true) {
                                isDayStarted = true
                                binding.appDashHeader.slideStartDay.text = getString(R.string.end_day)
                                binding.appDashHeader.slideStartDay.outerColor =
                                    (ContextCompat.getColor(requireContext(), R.color.red))
                                binding.appDashHeader.slideStartDay.isReversed = true
                            } else {
                                isDayStarted = false
                                binding.appDashHeader.slideStartDay.text = getString(R.string.start_day)
                                binding.appDashHeader.slideStartDay.outerColor =
                                    (ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark))
                                binding.appDashHeader.slideStartDay.isReversed = false
                            }
                        }else if(response.response?.code == 401){
                            tokenExpiresAlert()
                        } else if(response.response?.code == 400){
                            alertDialogShow(
                                requireContext(),
                                getString(R.string.alert),
                                response.response?.message ?: getString(R.string.something_went_wrong)
                            )
                        } else {
                            handleCheckInStatusError(response.response?.message)
                        }
                    }

                    Status.ERROR -> {
                        dismissProgress()
                        val throwable = response.throwable
                        if (throwable is HttpException) {
                            if (throwable.code() == 401) {
                                tokenExpiresAlert()
                            }
                        } else {
                            handleCheckInStatusError(response.response?.message)
                        }
                    }

                    Status.LOADING -> {
                        showProgress()
                    }
                }
            }
        }


    }

    @SuppressLint("MissingPermission")
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                lat = location.latitude
                long = location.longitude
            } else {
                lat = 0.0
                long = 0.0
            }
        }.addOnFailureListener {
            lat = 0.0
            long = 0.0
        }

        setupTabBar()
        setupSwipeButton()
        horizontalScrollTextView()
    }

    private fun checkInAttendanceStatus() {
        viewModel.checkInStatusAttendance()
    }

    private fun handleCheckInStatusError(message: String?) {
        alertDialogShow(
            requireContext(),
            getString(R.string.alert),
            message ?: getString(R.string.something_went_wrong),
            getString(R.string.retry),
            DialogInterface.OnClickListener { dialog, which ->
                checkInAttendanceStatus()
            }, false
        )
    }

    @SuppressLint("MissingPermission")
    private fun isWithinGeofence(onResult: (Boolean) -> Unit) {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location == null) {
                lat = 0.0
                long = 0.0
                onResult(false)
            } else {
                lat = location.latitude
                long = location.longitude
                val distance = FloatArray(1)
                Location.distanceBetween(
                    location!!.latitude, location!!.longitude,
                    GEOFENCE_LAT, GEOFENCE_LON,
                    distance
                )
                onResult(distance[0] <= GEOFENCE_RADIUS_METERS)
            }
        }.addOnFailureListener {
            lat = 0.0
            long = 0.0
            onResult(false)
        }
    }

    private fun setupUserData() {
        var userData: User? = PrefMethods.getUserData(prefMain) ?: return
        binding.appDashHeader.userData = userData
    }

    /**
     * Sets up the swipe button to start or end the day.
     * The button's text, color, and state change based on whether the day is started or ended.
     */
    private fun setupSwipeButton() {
        binding.appDashHeader.slideStartDay.onSlideCompleteListener =
            object : SlideToActView.OnSlideCompleteListener {
                override fun onSlideComplete(view: SlideToActView) {
                    val permissions = getRequiredPermissions()
                    when {
                        hasAllPermissions(permissions) -> {
                            manageDayStartEnd()
                        }
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
                if (location == null) {
                    lat = 0.0
                    long = 0.0
                } else {
                    lat = location.latitude
                    long = location.longitude
                    viewModel.checkOutAttendance(lat, long)
                }

            }.addOnFailureListener {
                lat = 0.0
                long = 0.0
            }
        } else {
            // Logic to start the day
            isWithinGeofence { isWithinGeofence ->
                if (isWithinGeofence) {
                    checkPermissionsAndUpdateGeofence()
                } else {
                    binding.appDashHeader.slideStartDay.setCompleted(false, true)
                    alertDialogShow(requireContext(), "Start day only from the home location")
                }
            }
        }
    }

    private fun setupTabBar() {
        val adapter = DashboardPagerAdapter(requireActivity())
        adapter.addFragment(AttendanceStatusFragment())
        adapter.addFragment(MyTargetsFragment())
        adapter.addFragment(ProjectDashboardFragment())
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
        val layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.appDashHeader.marqueeRecyclerView.layoutManager = layoutManager
        /*val handler = Handler(Looper.getMainLooper())
        val scrollRunnable = object : Runnable {
            override fun run() {
                val currentPosition = layoutManager.findFirstVisibleItemPosition()
                val nextPosition = (currentPosition + 1) % items.size
                binding.appDashHeader.marqueeRecyclerView.smoothScrollToPosition(nextPosition)
                handler.postDelayed(this, 2000) // Adjust delay as needed
            }
        }
        handler.post(scrollRunnable)*/
    }


    private val permissionLauncherLocationTracking = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.all { it.value } -> {
                viewModel.startTracking()
            }

            !permissions.any { shouldShowRequestPermissionRationale(it.key) } -> {
                showPermissionDeniedPermanently()
            }

            else -> {
                showPermissionRationale()
            }
        }
    }

    private val permissionLauncherGeofencing = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.all { it.value } -> {
                viewModel.checkInAttendance(lat, long)
                //manageDayStartEnd()
            }

            !permissions.any { shouldShowRequestPermissionRationale(it.key) } -> {
                showPermissionDeniedPermanently()
            }

            else -> {
                showPermissionRationale()
            }
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
            hasAllPermissions(permissions) -> {
                viewModel.startTracking()
            }

            permissions.any { shouldShowRequestPermissionRationale(it) } -> {
                showPermissionRationale()
            }

            else -> {
                permissionLauncherLocationTracking.launch(permissions)
            }
        }
    }

    private fun checkPermissionsAndUpdateGeofence() {
        val permissions = getRequiredPermissions()
        if (hasAllPermissions(permissions)) {
            viewModel.checkInAttendance(lat, long)
            //manageDayStartEnd()
        } else {
            permissionLauncherGeofencing.launch(permissions)
        }
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
            ContextCompat.checkSelfPermission(
                requireContext(),
                it
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun showPermissionRationale() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.permission_required)
            .setMessage(R.string.location_permission_rationale)
            .setPositiveButton(R.string.retry) { _, _ ->
                // Launch permission request after showing rationale
                permissionLauncherLocationTracking.launch(getRequiredPermissions())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPermissionDeniedPermanently() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.permission_denied)
            .setMessage(R.string.permission_denied_permanently)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                openApplicationSettings()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }


    /*
    * Runtime permission request for location tracking.
    * */

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.all { it.value } -> {
                manageDayStartEnd()
            }
            permissions.any { shouldShowRequestPermissionRationale(it.key) } -> {
                showPermissionDeniedPermanently()
            }
            else -> {
                showPermissionDeniedPermanently()
            }
        }
    }

}