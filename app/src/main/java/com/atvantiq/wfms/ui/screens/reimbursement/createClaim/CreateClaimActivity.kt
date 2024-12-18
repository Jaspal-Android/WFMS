package com.atvantiq.wfms.ui.screens.reimbursement.createClaim

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivityCreateClaimBinding
import com.atvantiq.wfms.ui.screens.adapters.CustomAutoCompleteAdapter
import com.atvantiq.wfms.utils.DateUtils

class CreateClaimActivity : BaseActivity<ActivityCreateClaimBinding,CreateClaimVM>() {

    /*Local variables*/
    private var circleList = listOf("CG", "HP", "HR", "MH", "PB", "RJ", "DL", "UPE")
    private val projectList =
        listOf("DEPL-CG-BSNL-TOWER", "DEPL-CG-BSNL-EnodeB", "DEMO-PROJECT", "DEV-ENV-PROJECT")
    private var siteIdList = listOf("445213", "438803", "893438", "343554", "098787")
    private var expenseTypeList = listOf("Public Transport", "Airfare", "Hotel Stays", "Business Meals", "Internet Charges")
    private var travellingWithList = listOf("Akhil-0012","Pankaj-LO012","Swati-KK56","Sandeep-BM90", )

    private lateinit var circleAdapter: CustomAutoCompleteAdapter
    private lateinit var projectAdapter: CustomAutoCompleteAdapter
    private lateinit var siteAdapter: CustomAutoCompleteAdapter
    private lateinit var expenseTypeAdapter:CustomAutoCompleteAdapter
    private lateinit var travellingWithAdapter:CustomAutoCompleteAdapter
    //---------------------------------------------------//

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_create_claim,CreateClaimVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpToolbar()
        setListeners()
        setCircleList()
        setProjectList()
        setSiteList()
        setExpenseTypeList()
        setTravellingWithList()
    }

    private fun setUpToolbar(){
        binding.createClaimsToolbar.toolbarTitle.text = getString(R.string.create_claim)
        binding.createClaimsToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setListeners(){
        binding.etDate.setOnClickListener {
            showDatePicker()
        }
        binding.circleEt.setOnClickListener {
            binding.circleEt.showDropDown()

        }
        binding.projectEt.setOnClickListener {
            binding.projectEt.showDropDown()

        }
        binding.siteEt.setOnClickListener {
            binding.siteEt.showDropDown()

        }
        binding.expenseTypeEt.setOnClickListener {
            binding.expenseTypeEt.showDropDown()

        }
        binding.travellingWithEt.setOnClickListener {
            binding.travellingWithEt.showDropDown()

        }

    }

    private fun setCircleList() {
        circleAdapter =
            CustomAutoCompleteAdapter(this, android.R.layout.simple_dropdown_item_1line, circleList)
        binding.circleEt.setAdapter(circleAdapter)
    }

    private fun setProjectList() {
        projectAdapter = CustomAutoCompleteAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            projectList
        )
        binding.projectEt.setAdapter(projectAdapter)
    }

    private fun setSiteList() {
        siteAdapter =
            CustomAutoCompleteAdapter(this, android.R.layout.simple_dropdown_item_1line, siteIdList)
        binding.siteEt.setAdapter(siteAdapter)
    }

    private fun setExpenseTypeList() {
        expenseTypeAdapter =
            CustomAutoCompleteAdapter(this, android.R.layout.simple_dropdown_item_1line, expenseTypeList)
        binding.expenseTypeEt.setAdapter(expenseTypeAdapter)
    }

    private fun setTravellingWithList() {
        travellingWithAdapter =
            CustomAutoCompleteAdapter(this, android.R.layout.simple_dropdown_item_1line, travellingWithList)
        binding.travellingWithEt.setAdapter(travellingWithAdapter)
    }

    private fun showDatePicker(){
        DateUtils.onDateClickWithLimit(this,object :DateUtils.DateCallBack{
            override fun onDateSelected(date: String, formatDate: String) {
                binding.dateString = date
            }
        },false)
    }

    override fun subscribeToEvents(vm: CreateClaimVM) {

    }
}