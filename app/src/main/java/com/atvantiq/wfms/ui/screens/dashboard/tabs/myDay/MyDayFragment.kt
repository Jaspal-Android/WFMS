package com.atvantiq.wfms.ui.screens.dashboard.tabs.myDay

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentMyDayBinding
import com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance.AttendanceCommunicationViewModel
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.utils.MapMarkerIcons
import com.atvantiq.wfms.utils.isSessionLost
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint

/**
 * My Day: the signed-in employee's own day from `GET /geo-tracking/me/day` — a summary, the
 * route on a map and a timeline, for today or any of the last 30 days.
 */
@AndroidEntryPoint
class MyDayFragment : BaseFragment<FragmentMyDayBinding, MyDayVM>() {

    /** Start/End Day on the dashboard announces itself here; My Day then reloads. */
    private val attendanceEvents: AttendanceCommunicationViewModel by activityViewModels()

    private val timelineAdapter = MyDayTimelineAdapter()
    private var googleMap: GoogleMap? = null
    private var shownContent: MyDayUi? = null

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_my_day, MyDayVM::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvTimeline.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTimeline.adapter = timelineAdapter
        binding.btnPreviousDay.setOnClickListener { viewModel.previousDay() }
        binding.btnNextDay.setOnClickListener { viewModel.nextDay() }
        binding.rowCheckedIn.label = getString(R.string.checked_in)
        binding.rowCheckedOut.label = getString(R.string.checked_out)
        binding.rowTimeMoving.label = getString(R.string.time_moving)
        binding.rowLocations.label = getString(R.string.locations_recorded)

        binding.mapView.bindLifecycle(viewLifecycleOwner, null)
        binding.mapView.getMapAsync { map ->
            configure(map)
            googleMap = map
            shownContent?.let { drawMap(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload whenever the tab is opened.
        viewModel.load()
    }

    override fun onDestroyView() {
        googleMap = null
        shownContent = null
        super.onDestroyView()
    }

    override fun subscribeToEvents(vm: MyDayVM) {
        vm.state.observe(viewLifecycleOwner) { state -> render(state) }

        vm.myDayResponse.observe(viewLifecycleOwner) { response ->
            if (!response.consumeOnce()) return@observe
            if (response.isSessionLost { it.code }) tokenExpiresAlert()
        }

        attendanceEvents.refreshCalendar.observe(viewLifecycleOwner) {
            // The value replayed on subscribe arrives before RESUMED; onResume loads anyway.
            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) viewModel.load()
        }
    }

    private fun render(state: MyDayState) {
        binding.tvDay.text = DateUtils.formatDayLabel(viewModel.selectedDayMillis(state.daysBack))
        setEnabled(binding.btnPreviousDay, state.canGoBack)
        setEnabled(binding.btnNextDay, state.canGoForward)

        binding.tvError.text = state.errorMessage
        binding.tvError.visibility = if (state.errorMessage.isNullOrBlank()) View.GONE else View.VISIBLE

        val content = state.content
        binding.loadingCard.visibility = if (content == null && state.isLoading) View.VISIBLE else View.GONE
        binding.tvEmpty.visibility = if (content?.isEmpty == true) View.VISIBLE else View.GONE
        binding.tvEmpty.setText(if (state.isToday) R.string.my_day_empty_today else R.string.my_day_empty_past)
        binding.content.visibility = if (content != null && !content.isEmpty) View.VISIBLE else View.GONE
        binding.content.alpha = if (state.isLoading) RELOADING_ALPHA else 1f
        binding.reloadPill.visibility = if (content != null && state.isLoading) View.VISIBLE else View.GONE

        if (content != null && content !== shownContent) {
            shownContent = content
            bindSummary(content, state.isToday)
            timelineAdapter.submitList(content.timeline)
            binding.timelineCard.visibility = if (content.timeline.isEmpty()) View.GONE else View.VISIBLE
            drawMap(content)
        }
    }

    private fun bindSummary(content: MyDayUi, isToday: Boolean) {
        binding.tvDistanceCaption.setText(if (isToday) R.string.distance_today else R.string.distance_on_day)
        binding.tvDistance.text = getString(
            if (content.isDistanceEstimate) R.string.distance_value_estimate else R.string.distance_value,
            content.distanceKm
        )
        bindShiftStatus(content.isShiftActive)
        binding.rowCheckedIn.value = timeOrNotAvailable(content.checkInMillis)
        binding.rowCheckedOut.value = timeOrNotAvailable(content.checkOutMillis)
        binding.rowTimeMoving.value = content.movingMinutes?.let { getString(R.string.minutes_value, it) }
            ?: getString(R.string.not_available)
        binding.rowLocations.value = content.pointsRecorded.toString()
    }

    private fun bindShiftStatus(isActive: Boolean) {
        val color = MaterialColors.getColor(
            binding.tvShiftStatus,
            if (isActive) R.attr.wfmsColorAccent else R.attr.wfmsColorOnSurfaceVariant
        )
        binding.tvShiftStatus.setText(if (isActive) R.string.shift_active else R.string.shift_ended)
        binding.tvShiftStatus.setTextColor(color)
        binding.tvShiftStatus.setBackgroundResource(
            if (isActive) R.drawable.bg_pill_primary_soft else R.drawable.bg_pill_background
        )
        val icon = ContextCompat.getDrawable(
            requireContext(),
            if (isActive) R.drawable.rounded_location_on_24 else R.drawable.ic_check_circle_24
        )?.mutate()?.apply {
            val size = resources.getDimensionPixelSize(R.dimen.padding)
            setBounds(0, 0, size, size)
        }
        binding.tvShiftStatus.setCompoundDrawablesRelative(icon, null, null, null)
        TextViewCompat.setCompoundDrawableTintList(binding.tvShiftStatus, ColorStateList.valueOf(color))
    }

    private fun timeOrNotAvailable(millis: Long?): String =
        millis?.let { DateUtils.formatShortTime(it) } ?: getString(R.string.not_available)

    @SuppressLint("MissingPermission") // checked just above
    private fun configure(map: GoogleMap) {
        map.uiSettings.apply {
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
            isMapToolbarEnabled = false
            isCompassEnabled = true
            isMyLocationButtonEnabled = true
        }
        val hasLocation = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }
        if (hasLocation) map.isMyLocationEnabled = true
    }

    private fun drawMap(content: MyDayUi) {
        val bounds = content.mapBounds
        binding.mapCard.visibility = if (bounds == null) View.GONE else View.VISIBLE
        val map = googleMap ?: return
        map.clear()
        if (bounds == null) return

        if (content.route.isNotEmpty()) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(content.route.map { LatLng(it.latitude, it.longitude) })
                    .color(MaterialColors.getColor(binding.mapView, R.attr.wfmsColorPrimary))
                    .width(resources.getDimension(R.dimen.map_route_width))
            )
        }
        val ring = MaterialColors.getColor(binding.mapView, R.attr.wfmsColorSurface)
        content.pins.forEach { pin ->
            val style = TimelineStyle.of(pin.kind)
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(pin.position.latitude, pin.position.longitude))
                    .title(getString(style.title))
                    .icon(MapMarkerIcons.circle(requireContext(), style.icon, MyDayTimelineAdapter.tintOf(requireContext(), style), ring))
                    .anchor(PIN_ANCHOR, PIN_ANCHOR)
            )
        }
        // Bounds need a laid-out map.
        binding.mapView.post {
            if (googleMap !== map || binding.mapView.width == 0) return@post
            map.moveCamera(
                CameraUpdateFactory.newLatLngBounds(
                    LatLngBounds(LatLng(bounds.south, bounds.west), LatLng(bounds.north, bounds.east)), 0
                )
            )
        }
    }

    private fun setEnabled(button: View, enabled: Boolean) {
        button.isEnabled = enabled
        button.alpha = if (enabled) 1f else DISABLED_ALPHA
    }

    private companion object {
        /** Old content stays visible, dimmed, while a reload runs. */
        const val RELOADING_ALPHA = 0.58f
        const val DISABLED_ALPHA = 0.38f
        const val PIN_ANCHOR = 0.5f
    }
}
