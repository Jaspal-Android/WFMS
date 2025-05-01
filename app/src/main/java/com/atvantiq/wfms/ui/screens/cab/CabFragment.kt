package com.atvantiq.wfms.ui.screens.cab

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentCabBinding
import com.atvantiq.wfms.models.cab.CabRide
import com.atvantiq.wfms.ui.screens.adapters.CabFareListAdapter
import com.atvantiq.wfms.ui.screens.cab.add.AddCabFareActivity
import com.atvantiq.wfms.utils.Utils


class CabFragment : BaseFragment<FragmentCabBinding, CabViewModel>() {

    private lateinit var cabFareListAdapter: CabFareListAdapter

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_cab, CabViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun subscribeToEvents(vm: CabViewModel) {

    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val cabRides = listOf(
            CabRide("1", "04-03-205","PB","RM45454","Rahul-ATQ/8245","Monthly",30.7333,76.7794,30.6942,76.8606,"4:30 AM","8:40PM"),
            CabRide("1", "05-03-205","HR","HR45555","Rakesh-ATQ/12205","On-Call",30.7333,76.7794,30.9010,75.8573,"10:40 AM","6:00PM"),
            CabRide("1", "06-03-205","MP","MP78788","Mukul-ATQ/26587","Monthly",31.3260,75.5762,31.6340,74.8723,"6:30 AM","5:30PM"),
            // Add more rides as needed
        )
        setListeners()
        setCabListAllList(cabRides)
    }

    private fun setListeners(){
        binding.addCabText.setOnClickListener {
            Utils.jumpActivity(requireContext(),AddCabFareActivity::class.java)
        }
    }

    private fun setCabListAllList(cabRides: List<CabRide>) {
        cabFareListAdapter  = CabFareListAdapter(cabRides,viewLifecycleOwner)
        binding.cabFareList.layoutManager = LinearLayoutManager(requireContext())
        binding.cabFareList.adapter = cabFareListAdapter
    }

}