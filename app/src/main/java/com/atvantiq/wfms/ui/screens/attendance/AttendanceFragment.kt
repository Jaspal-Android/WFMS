package com.atvantiq.wfms.ui.screens.attendance

import android.annotation.SuppressLint
import android.os.Bundle
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentAttendanceBinding
import com.atvantiq.wfms.ui.screens.attendance.addSignInActivity.AddSignInActivity
import com.atvantiq.wfms.ui.screens.attendance.approvals.ApprovalsActivity
import com.atvantiq.wfms.ui.screens.attendance.myProgress.MyProgressActivity
import com.atvantiq.wfms.ui.screens.attendance.signInDetails.SignInDetailActivity
import com.atvantiq.wfms.utils.Utils
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class AttendanceFragment : BaseFragment<FragmentAttendanceBinding, AttendanceViewModel>(){

    private lateinit var googleMap: GoogleMap

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_attendance, AttendanceViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun subscribeToEvents(vm: AttendanceViewModel) {

    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        setListeners()
        initGoogleMaps()
    }

    private fun setListeners() {
        binding.signInText.setOnClickListener {
            Utils.jumpActivity(requireContext(), AddSignInActivity::class.java)
        }
        binding.myProgressText.setOnClickListener {
            Utils.jumpActivity(requireContext(), MyProgressActivity::class.java)
        }
        binding.btLogedProfile.setOnClickListener {
            Utils.jumpActivity(requireContext(), SignInDetailActivity::class.java)
        }
    }

    private fun initGoogleMaps() {
        var mapFragment: SupportMapFragment = childFragmentManager.findFragmentById(R.id.trackingMap) as SupportMapFragment
        mapFragment.getMapAsync {
            googleMap = it
            mapSettings()
            moveCameraToLocation()
        }
    }

    private fun moveCameraToLocation(){
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(30.741482,76.768066),16f))
    }

    @SuppressLint("MissingPermission")
    private fun mapSettings(){
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isRotateGesturesEnabled = true
    }

}