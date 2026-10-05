package com.atvantiq.wfms.ui.screens.dashboard

import androidx.annotation.StringRes
import com.atvantiq.wfms.utils.permissions.openAppSettings
import com.atvantiq.wfms.utils.permissions.LocationPermissionDelegate
import com.atvantiq.wfms.utils.permissions.BackgroundLocationRequest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
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
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.DashboardTabs
import com.atvantiq.wfms.ui.screens.adapters.DashboardPagerAdapter
import com.atvantiq.wfms.ui.screens.announcements.AnnouncementsActivity
import com.atvantiq.wfms.ui.screens.attendance.applyLeave.ApplyLeaveActivity
import com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance.AttendanceCommunicationViewModel
import com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance.AttendanceStatusFragment
import com.atvantiq.wfms.ui.screens.dashboard.tabs.myTargets.MyTargetsFragment
import com.atvantiq.wfms.ui.screens.dashboard.tabs.projectDashboard.ProjectDashboardFragment
import com.atvantiq.wfms.ui.screens.more.ProfileVM
import com.atvantiq.wfms.utils.PermissionUtils
import com.atvantiq.wfms.utils.Utils
import com.atvantiq.wfms.utils.setAccessibleAction
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.ncorti.slidetoact.SlideToActView
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException
import com.atvantiq.wfms.utils.navigateToTab
import com.atvantiq.wfms.ui.screens.dashboard.tabs.myDay.MyDayFragment
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.location.LocationManagerCompat
import com.atvantiq.wfms.data.tracking.ShiftState
import com.atvantiq.wfms.utils.DateUtils

@AndroidEntryPoint
class DashboardFragment : BaseFragment<FragmentDashboardBinding, DashboardViewModel>() {

    private var isDayStarted = false
    private var attendanceActionInFlight = false
    // Location permission is asked automatically on resume while checked in; ask once per screen.
    private var trackingPermissionPrompted = false
    private var backgroundLocationPrompted = false

    // Every location request goes through a delegate, which shows the disclosure before the
    // system prompt and explains a denial the same way on every screen.
    private val dayLocationPermission = locationPermission(
        R.string.attendance_location_disclosure_title, R.string.attendance_location_disclosure_msg,
        includeNotifications = true, onGranted = ::manageDayStartEnd
    )
    private val trackingLocationPermission = locationPermission(
        R.string.attendance_location_disclosure_title, R.string.attendance_location_disclosure_msg,
        includeNotifications = true, onGranted = ::startTrackingAndAskForBackground
    )
    private val currentLocationPermission = locationPermission(
        R.string.share_current_location, R.string.share_location_msg,
        rationaleFirst = true, onGranted = ::getCurrentLatitudeLongitude
    )

    // Tracking runs as a location foreground service, so declining background access must not
    // undo it; the card is only refreshed.
    private val backgroundLocation = BackgroundLocationRequest(
        this, { requireContext() },
        R.string.background_location_usage, R.string.background_location_usage_msg
    ) { renderTrackingCard() }

    private fun locationPermission(
        @StringRes title: Int,
        @StringRes message: Int,
        includeNotifications: Boolean = false,
        rationaleFirst: Boolean = false,
        onGranted: () -> Unit
    ) = LocationPermissionDelegate(
        this, { requireContext() }, ::shouldShowRequestPermissionRationale,
        LocationPermissionDelegate.Config(
            LocationPermissionDelegate.Disclosure(title, message),
            includeNotifications = includeNotifications,
            rationaleFirst = rationaleFirst,
            onGranted = onGranted
        )
    )
    private var pendingCheckoutLocation: Pair<Double, Double>? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val communicationViewModel: AttendanceCommunicationViewModel by activityViewModels()

    /** The shell's profile and tabs, refreshed from `GET /employee/me` by the activity. */
    private val profileViewModel: ProfileVM by activityViewModels()

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
        binding.appDashHeader.slideStartDay.let { it.setAccessibleAction(it.text) }
    }

    override fun subscribeToEvents(vm: DashboardViewModel) {
        binding.vm = vm

        profileViewModel.profile.observe(viewLifecycleOwner) { profile -> setupUserData(profile) }

        // Pause / Resume from the card or the notification, and the end of a pause.
        vm.shiftState.observe(viewLifecycleOwner) { renderTrackingCard() }

        vm.clickEvents.observe(viewLifecycleOwner) {
            if (!isLifeCycleResumed()) return@observe
            when (it) {
                DashboardClickEvents.onAnnouncementsClicks -> Utils.jumpActivity(requireContext(), AnnouncementsActivity::class.java)

                DashboardClickEvents.onFetchCurrentLatitudeLongitudeClicks -> {
                    currentLocationPermission.request()
                }
                DashboardClickEvents.APPLY_LEAVE_CLICK -> {
                    Utils.jumpActivity(requireContext(), ApplyLeaveActivity::class.java)
                }
                null -> Unit
            }
        }

        // Attendance results are consumed once instead of being gated on RESUMED: a result that
        // lands while the screen is only STARTED (e.g. behind a permission dialog) must still be
        // handled, otherwise tracking keeps running after checkout and the slider stays locked.
        vm.attendanceCheckInResponse.observe(viewLifecycleOwner) { response ->
            if (!response.consumeOnce()) return@observe
            when (response.status) {
                Status.SUCCESS -> handleCheckInResponse(response.response)
                Status.ERROR -> handleError(response.throwable, response.response?.message)
                Status.LOADING -> showProgress()
            }
        }

        vm.attendanceCheckOutResponse.observe(viewLifecycleOwner) { response ->
            if (!response.consumeOnce()) return@observe
            when (response.status) {
                Status.SUCCESS -> handleCheckOutResponse(response.response)
                Status.ERROR -> handleError(response.throwable, response.response?.message)
                Status.LOADING -> showProgress()
            }
        }

        vm.attendanceCheckInStatusResponse.observe(viewLifecycleOwner) { response ->
            if (!response.consumeOnce()) return@observe
            when (response.status) {
                Status.SUCCESS -> handleCheckInStatusResponse(response.response)
                Status.ERROR -> handleCheckInStatusError(response.response?.message,response.throwable)
                Status.LOADING -> showProgress()
            }
        }

        vm.attendanceRemarksResponse.observe(viewLifecycleOwner) { response ->
            if (!response.consumeOnce()) return@observe
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

    override fun onResume() {
        super.onResume()
        checkInAttendanceStatus()
        // Permissions or Location Services may have changed in Settings.
        renderTrackingCard()
    }

    override fun onDestroyView() {
        view?.removeCallbacks(trackingCardRefresh)
        super.onDestroyView()
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
            else -> handleRejectedResponse(response?.code, response?.message)
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
            else -> handleRejectedResponse(response?.code, response?.message)
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
                // Work for employees; Approvals, or Sites, for admins.
                DashboardTabs.workEntryTab(profileViewModel.tabs.value.orEmpty())
                    ?.let { findNavController().navigateToTab(it.destinationId) }
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
        slideStartDay.setAccessibleAction(slideStartDay.text)
        renderTrackingCard()
    }

    /** Re-renders the card when a pause runs out while the dashboard is open. */
    private val trackingCardRefresh = Runnable { renderTrackingCard() }

    /**
     * The tracking card under the name row: whether the shift is being tracked, with one action.
     * Shown only while a day is active; End Day always works, whatever the card shows.
     */
    private fun renderTrackingCard() {
        val root = view ?: return
        val shift = viewModel.shiftState.value ?: ShiftState()
        val now = System.currentTimeMillis()
        val state = TrackingCardState.resolve(
            isDayActive = isDayStarted,
            isTrackingStarted = viewModel.isTrackingStarted,
            hasForegroundLocation = PermissionUtils.hasLocationPermissions(requireContext()),
            hasBackgroundLocation = PermissionUtils.hasBackgroundLocationPermission(requireContext()),
            isGpsOn = isLocationServiceOn(),
            shift = shift,
            nowMillis = now
        )
        root.removeCallbacks(trackingCardRefresh)
        shift.pausedUntilMillis?.takeIf { state == TrackingCardState.PAUSED }?.let { until ->
            root.postDelayed(trackingCardRefresh, (until - now).coerceAtLeast(0L))
        }
        with(binding.appDashHeader) {
            trackingCard.visibility = if (state == null) View.GONE else View.VISIBLE
            if (state == null) return
            val context = requireContext()
            val accent = MaterialColors.getColor(trackingCard, R.attr.wfmsColorAccent)
            val error = MaterialColors.getColor(trackingCard, R.attr.wfmsColorError)
            val white = ContextCompat.getColor(context, R.color.white)
            when (state) {
                TrackingCardState.ACTIVE -> {
                    trackingCard.setBackgroundResource(R.drawable.bg_card_outlined)
                    setTrackingIcon(R.drawable.ic_tracking_navigation, MaterialColors.getColor(trackingCard, R.attr.wfmsColorPrimary), white)
                    tvTrackingMessage.text = getString(R.string.tracking_active_since, shortTime(shift.checkInMillis))
                }
                TrackingCardState.PAUSED -> {
                    trackingCard.setBackgroundResource(R.drawable.bg_tracking_card_paused)
                    setTrackingIcon(R.drawable.ic_tracking_pause, MaterialColors.getColor(trackingCard, R.attr.wfmsColorWarning), white)
                    tvTrackingMessage.text = getString(R.string.tracking_paused_until, shortTime(shift.pausedUntilMillis))
                }
                TrackingCardState.STARTING -> {
                    trackingCard.setBackgroundResource(R.drawable.bg_card_outlined)
                    setTrackingIcon(R.drawable.rounded_location_on_24, Color.TRANSPARENT, accent)
                    tvTrackingMessage.setText(R.string.tracking_starting)
                }
                TrackingCardState.NEEDS_ALWAYS, TrackingCardState.PERMISSION_DENIED, TrackingCardState.GPS_OFF -> {
                    trackingCard.setBackgroundResource(R.drawable.bg_tracking_card_alert)
                    setTrackingIcon(R.drawable.ic_tracking_warning, ContextCompat.getColor(context, R.color.error_soft), error)
                    tvTrackingMessage.setText(
                        when (state) {
                            TrackingCardState.NEEDS_ALWAYS -> R.string.tracking_needs_always
                            TrackingCardState.PERMISSION_DENIED -> R.string.tracking_permission_denied
                            else -> R.string.tracking_gps_off
                        }
                    )
                }
            }
            btnTrackingPause.visibility = if (state == TrackingCardState.ACTIVE) View.VISIBLE else View.GONE
            btnTrackingPause.setOnClickListener { viewModel.pauseTracking() }
            val action: Pair<Int, () -> Unit>? = when (state) {
                TrackingCardState.PAUSED -> R.string.resume to { viewModel.resumeTracking() }
                TrackingCardState.NEEDS_ALWAYS -> R.string.fix to { requestBackgroundLocation() }
                TrackingCardState.PERMISSION_DENIED -> R.string.open_settings to { requireContext().openAppSettings() }
                TrackingCardState.GPS_OFF -> R.string.open_settings to {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                else -> null
            }
            btnTrackingAction.visibility = if (action == null) View.GONE else View.VISIBLE
            action?.let { (label, onClick) ->
                btnTrackingAction.setText(label)
                btnTrackingAction.setIconResource(if (state == TrackingCardState.PAUSED) R.drawable.ic_tracking_play else 0)
                btnTrackingAction.setOnClickListener { onClick() }
            }
        }
    }

    private fun setTrackingIcon(icon: Int, circleColor: Int, iconColor: Int) = with(binding.appDashHeader.ivTrackingIcon) {
        setImageResource(icon)
        backgroundTintList = ColorStateList.valueOf(circleColor)
        imageTintList = ColorStateList.valueOf(iconColor)
    }

    private fun shortTime(millis: Long?): String =
        millis?.let { DateUtils.formatShortTime(it) } ?: getString(R.string.not_available)

    private fun isLocationServiceOn(): Boolean =
        ContextCompat.getSystemService(requireContext(), LocationManager::class.java)
            ?.let { LocationManagerCompat.isLocationEnabled(it) } ?: true


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
                if (Utils.isUsableAttendanceLocation(cachedLocation)) {
                    whenAttached { onResult(cachedLocation) }
                    return@addOnSuccessListener
                }

                val cancellationTokenSource = CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { freshLocation ->
                    whenAttached { onResult(freshLocation) }
                }.addOnFailureListener {
                    whenAttached { onResult(null) }
                }
            }.addOnFailureListener {
                whenAttached {
                    resetAttendanceAction()
                    onResult(null)
                }
            }
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
                    if (PermissionUtils.hasLocationPermissions(requireContext())) {
                        manageDayStartEnd()
                    } else {
                        binding.appDashHeader.slideStartDay.setCompleted(false, true)
                        dayLocationPermission.request()
                    }
                }
            }
    }

    private fun setupTabBar() {
        val pages = listOf(
            DashboardPagerAdapter.Page(key = "attendance") { AttendanceStatusFragment() },
            DashboardPagerAdapter.Page(key = "myDay") { MyDayFragment() },
            DashboardPagerAdapter.Page(key = "targets") { MyTargetsFragment() },
            DashboardPagerAdapter.Page(key = "projects") { ProjectDashboardFragment() }
        )
        val titles = listOf(R.string.attendance, R.string.my_day, R.string.my_targets, R.string.projects)
        binding.viewPager.adapter = DashboardPagerAdapter(requireActivity(), pages)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = getString(titles[position])
        }.attach()
    }


                    // Play's Prominent Disclosure policy: the system prompt is only ever shown right after the
    // in-app disclosure, never on its own.
    private fun checkPermissionForLiveLocation() {
        when {
            PermissionUtils.hasLocationPermissions(requireContext()) -> startTrackingAndAskForBackground()
            trackingPermissionPrompted -> Unit
            else -> {
                trackingPermissionPrompted = true
                trackingLocationPermission.request()
            }
        }
    }

    private fun startTrackingAndAskForBackground() {
        viewModel.startTracking()
        renderTrackingCard()
        requestBackgroundLocationIfNeeded()
    }

    // Android 11+ ignores background location when it is requested together with foreground
    // location, so it is asked for separately, once per screen, after its own disclosure.
    private fun requestBackgroundLocationIfNeeded() {
        if (backgroundLocationPrompted || PermissionUtils.hasBackgroundLocationPermission(requireContext())) return
        backgroundLocationPrompted = true
        requestBackgroundLocation()
    }

    /** Disclosure, then the "Allow all the time" request (also the card's Fix button). */
    private fun requestBackgroundLocation() = backgroundLocation.request()

    @SuppressLint("MissingPermission")
    private fun getCurrentLatitudeLongitude() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            whenAttached { context ->
                val latitude = location?.latitude
                val longitude = location?.longitude
                if (latitude != null && longitude != null) {
                    // The address arrives on the main thread, possibly after the screen is gone.
                    Utils.getAddressFromLatLong(context, latitude, longitude) { addressFromLatLon ->
                        whenAttached {
                            alertDialogShow(
                                it,
                                getString(R.string.current_location),
                                getString(R.string.current_location_details, latitude.toString(), longitude.toString(), addressFromLatLon)
                            )
                        }
                    }
                } else {
                    alertDialogShow(context, getString(R.string.alert), getString(R.string.unable_to_fetch_location))
                }
            }
        }.addOnFailureListener {
            whenAttached { alertDialogShow(it, getString(R.string.alert), getString(R.string.unable_to_fetch_location)) }
        }
    }
}
