package com.atvantiq.wfms.ui.screens.materialReco

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.databinding.FragmentPurchaseBinding
import com.atvantiq.wfms.models.material.MaterialRecord
import com.atvantiq.wfms.ui.screens.adapters.MaterialAdapter
import com.atvantiq.wfms.ui.screens.materialReco.add.AddMaterialActivity
import com.atvantiq.wfms.utils.Utils


class PurchaseFragment : BaseFragment<FragmentPurchaseBinding,MaterialViewModel>() {

    private val materialList = listOf(
        MaterialRecord("10-Apr-2025", "JIO 4G", "Galvanized Steel (IS2062 Grade E410)", "kg", 10000f, 8000f),
        MaterialRecord("12-Apr-2025", "Ericssion Fiber", "M30 Grade Concrete", "cubic meter", 50f, 40f),
        MaterialRecord("08-Apr-2025", "Cisco 5G", "Single-Mode Fiber Optic Cable", "meter", 2000f, 1500f),
        MaterialRecord("14-Apr-2025", "Vodafone Towers", "Stainless Steel Brackets", "pieces", 100f, 80f),
        MaterialRecord("09-Apr-2025", "Jio Rooftops", "Copper Wire (6 AWG)", "meter", 500f, 400f)
    )

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_purchase,MaterialViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        initListeners()
        inflateMaterialList()
    }

    private fun initListeners(){
        binding.addMaterialText.setOnClickListener {
            Utils.jumpActivity(requireContext(), AddMaterialActivity::class.java)
        }
    }

    private fun inflateMaterialList(){
        binding.materialList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = MaterialAdapter(materialList)
        }
    }

    override fun subscribeToEvents(vm: MaterialViewModel) {

    }

}