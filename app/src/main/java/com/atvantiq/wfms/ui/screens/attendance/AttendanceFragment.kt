package com.atvantiq.wfms.ui.screens.attendance

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import com.atvantiq.wfms.BuildConfig
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentAttendanceBinding
import com.atvantiq.wfms.ui.screens.attendance.addSignInActivity.AddSignInActivity
import com.atvantiq.wfms.ui.screens.attendance.myProgress.MyProgressActivity
import com.atvantiq.wfms.ui.screens.attendance.signInDetails.SignInDetailActivity
import com.atvantiq.wfms.utils.Utils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class AttendanceFragment : BaseFragment<FragmentAttendanceBinding, AttendanceViewModel>() {

    private lateinit var googleMap: GoogleMap
    private val chandigarh = LatLng(30.7333, 76.7794)
    private val panchkula = LatLng(30.6942, 76.8606)

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
            //drawRoute(chandigarh,panchkula)
        }
    }

    private fun moveCameraToLocation() {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(chandigarh, 12f))
    }

    @SuppressLint("MissingPermission")
    private fun mapSettings() {
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isRotateGesturesEnabled = true
    }

    private fun drawRoute(origin: LatLng, destination: LatLng) {
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${destination.latitude},${destination.longitude}" +
                "&mode=driving&key=${BuildConfig.GOOGLE_MAPS_API_KEY}"

        Log.e("DIRECTIONS_API", url)

        // Use coroutine or AsyncTask to fetch JSON
        CoroutineScope(Dispatchers.IO).launch {
            val data = URL(url).readText()
            val jsonObject = JSONObject(data)

            val routes = jsonObject.getJSONArray("routes")
            if (routes.length() > 0) {
                val points = ArrayList<LatLng>()
                val legs = routes.getJSONObject(0).getJSONArray("legs")
                val steps = legs.getJSONObject(0).getJSONArray("steps")

                for (i in 0 until steps.length()) {
                    val polyline =
                        steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                    points.addAll(PolyUtil.decode(polyline))
                }

                withContext(Dispatchers.Main) {
                    googleMap.addPolyline(
                        PolylineOptions()
                            .addAll(points)
                            .width(10f)
                            .color(Color.BLUE)
                    )

                    googleMap.addMarker(MarkerOptions().position(origin).title("Origin"))
                    googleMap.addMarker(MarkerOptions().position(destination).title("Destination"))
                }
            }
        }
    }

}