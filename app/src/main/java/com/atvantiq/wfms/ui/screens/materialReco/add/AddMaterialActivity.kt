package com.atvantiq.wfms.ui.screens.materialReco.add

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivityAddMaterialBinding
import com.atvantiq.wfms.ui.screens.adapters.CustomAutoCompleteAdapter
import com.atvantiq.wfms.ui.screens.cab.CabClickEvents
import com.atvantiq.wfms.utils.DateUtils

class AddMaterialActivity : BaseActivity<ActivityAddMaterialBinding,AddMaterialViewModel>() {

    /*Local variables*/
    private var siteIdList = listOf("445213", "438803", "893438", "343554", "098787")
    private var materialList = listOf("Galvanized Steel", "M30 Grade Concrete", "Single-Mode Fiber Optic Cable", "Stainless Steel Brackets", "Copper Wire (6 AWG)")
    private val unitList = listOf("kg","meter","pieces","cubic meter")

    private lateinit var siteAdapter: CustomAutoCompleteAdapter
    private lateinit var materialAdapter: CustomAutoCompleteAdapter
    private lateinit var unitListAdapter: CustomAutoCompleteAdapter
    //---------------------------------------------------//


    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_add_material,AddMaterialViewModel::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpToolbar()
        initListeners()
        setSiteList()
        setMaterialList()
        setUnitList()
    }

    private fun setUpToolbar() {
        binding.addMaterialToolbar.toolbarTitle.text = getString(R.string.add_material)
        binding.addMaterialToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: AddMaterialViewModel) {
        binding.vm = vm
        vm.clickEvents.observe(this) {
            when (it) {
                AddMaterialClickEvents.ON_SAVE_CLICK -> {
                    finish()
                }
                AddMaterialClickEvents.ON_CANCEL_CLICK ->{
                    finish()
                }
            }
        }
    }

    private fun initListeners() {

        binding.etConsumedQuantity.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updatePending()
            }
        })
        binding.etTotalQuantity.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                updatePending()
            }
        })
        binding.siteEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.siteEt.showDropDown()
            }
        }
        binding.siteEt.setOnClickListener {
            binding.siteEt.showDropDown()

        }

        binding.materialEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.materialEt.showDropDown()
            }
        }
        binding.materialEt.setOnClickListener {
            binding.materialEt.showDropDown()

        }

        binding.unitEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.unitEt.showDropDown()
            }
        }
        binding.unitEt.setOnClickListener {
            binding.unitEt.showDropDown()

        }

        binding.etDate.setOnClickListener {
            DateUtils.onDateClick(this,object : DateUtils.DateCallBack{
                override fun onDateSelected(date: String, formatDate: String) {
                    binding.dateString = date
                }
            })
        }
    }

    private fun setSiteList() {
        siteAdapter =
            CustomAutoCompleteAdapter(this, android.R.layout.simple_dropdown_item_1line, siteIdList)
        binding.siteEt.setAdapter(siteAdapter)
    }

    private fun setMaterialList() {
        materialAdapter =
            CustomAutoCompleteAdapter(this, android.R.layout.simple_dropdown_item_1line, materialList)
        binding.materialEt.setAdapter(materialAdapter)
    }

    private fun setUnitList() {
        unitListAdapter =
            CustomAutoCompleteAdapter(this, android.R.layout.simple_dropdown_item_1line, unitList)
        binding.unitEt.setAdapter(unitListAdapter)
    }

    private fun updatePending() {
        val total = binding.etTotalQuantity.text.toString().toFloatOrNull() ?: 0f
        val consumed = binding.etConsumedQuantity.text.toString().toFloatOrNull() ?: 0f
        binding.etPendingQuantity.setText((total - consumed).toString())
    }
}