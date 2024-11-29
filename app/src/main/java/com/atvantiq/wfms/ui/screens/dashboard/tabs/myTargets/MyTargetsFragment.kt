package com.atvantiq.wfms.ui.screens.dashboard.tabs.myTargets

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentMyTargetsBinding
import com.atvantiq.wfms.ui.screens.adapters.MyTargetAdapter


/**
 * A simple [Fragment] subclass.
 * Use the [MyTargetsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class MyTargetsFragment : BaseFragment<FragmentMyTargetsBinding,MyTargetsVM>() {

    private lateinit var targetAdapter:MyTargetAdapter

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_my_targets,MyTargetsVM::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun subscribeToEvents(vm: MyTargetsVM) {

    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        initMyTargetList()
    }

    private fun initMyTargetList(){
        targetAdapter  = MyTargetAdapter()
        binding.myTargetList.layoutManager = LinearLayoutManager(requireContext())
        binding.myTargetList.adapter = targetAdapter
    }

}