package com.atvantiq.wfms.ui.screens.dashboard.tabs.myTargets

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentMyTargetsBinding
import com.atvantiq.wfms.ui.screens.adapters.MyTargetAdapter
import com.atvantiq.wfms.widgets.DividerItemDecoration


/**
 * A simple [Fragment] subclass.
 */

class MyTargetsFragment : BaseFragment<FragmentMyTargetsBinding,MyTargetsVM>() {

    private lateinit var targetAdapter:MyTargetAdapter

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_my_targets,MyTargetsVM::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initMyTargetList()
    }

    override fun subscribeToEvents(vm: MyTargetsVM) {

    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Intentionally empty.
    }

    private fun initMyTargetList(){
        targetAdapter  = MyTargetAdapter()
        binding.myTargetList.addItemDecoration(DividerItemDecoration(requireContext(),R.drawable.custom_divider))
        binding.myTargetList.layoutManager = LinearLayoutManager(requireContext())
        binding.myTargetList.adapter = targetAdapter
    }

}