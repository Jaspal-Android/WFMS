package com.atvantiq.wfms.ui.screens.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemCabListBinding
import com.atvantiq.wfms.models.cab.CabRide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

class CabFareListAdapter(private val cabRides: List<CabRide>,private val lifecycleOwner: LifecycleOwner) :
    RecyclerView.Adapter<CabFareListAdapter.MyTargetViewHolder>() {

    inner class MyTargetViewHolder(var binding: ItemCabListBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyTargetViewHolder {
        var infalter = LayoutInflater.from(parent.context)
        var binding: ItemCabListBinding =
            DataBindingUtil.inflate(infalter, R.layout.item_cab_list, parent, false)
        return MyTargetViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return 3
    }

    override fun onBindViewHolder(holder: MyTargetViewHolder, position: Int) {
        var cabRide = cabRides[position]
        holder.binding.item = cabRide
        holder.binding.executePendingBindings()

        val mapView: MapView = holder.binding.mapView
        mapView.onCreate(null)
        mapView.onResume() // Needed to get the map to display immediately
        mapView.getMapAsync { googleMap ->
            // Ensure MapsInitializer is initialized
            MapsInitializer.initialize(holder.binding.mapView.context)
            val startPoint = LatLng(cabRide.startLat, cabRide.startLng)
            val endPoint = LatLng(cabRide.endLat, cabRide.endLng)
            // Add markers
            googleMap.addMarker(MarkerOptions().position(startPoint).title("Start"))
            googleMap.addMarker(MarkerOptions().position(endPoint).title("End"))

            // Draw a straight line (polyline) between start and end
            googleMap.addPolyline(
                PolylineOptions()
                    .add(startPoint, endPoint)
                    .width(5f)
                    .color(android.graphics.Color.BLUE)
            )

            // Move camera to show both points
            val builder = com.google.android.gms.maps.model.LatLngBounds.Builder()
            builder.include(startPoint)
            builder.include(endPoint)
            val bounds = builder.build()
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        }

        // Handle MapView lifecycle
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                mapView.onDestroy()
            }

            override fun onPause(owner: LifecycleOwner) {
                mapView.onPause()
            }

            override fun onResume(owner: LifecycleOwner) {
                mapView.onResume()
            }
        })
    }
}