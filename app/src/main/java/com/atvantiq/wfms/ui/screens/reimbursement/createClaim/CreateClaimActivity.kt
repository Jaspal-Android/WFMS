package com.atvantiq.wfms.ui.screens.reimbursement.createClaim

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.ActivityCreateClaimBinding
import com.atvantiq.wfms.models.reimbursement.DAExpense
import com.atvantiq.wfms.models.reimbursement.HotelExpense
import com.atvantiq.wfms.models.reimbursement.MultipleSite
import com.atvantiq.wfms.models.reimbursement.OtherExpense
import com.atvantiq.wfms.models.reimbursement.TravelExpense
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.adapters.AddDaExpenseEntriesAdapter
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.adapters.SelectedHotelEntriesAdapter
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.adapters.SelectedOtherEntriesAdapter
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.adapters.SelectedSitesAdapter
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.adapters.SelectedTravelingEntriesAdapter
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.addSiteDetail.AddMutilSiteDetailsActivity
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.addTravelDetail.AddTravelingDetailActivity
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.dialogs.EnterDaBottomSheet
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.dialogs.EnterOthersBottomSheet
import com.atvantiq.wfms.utils.DateUtils

class CreateClaimActivity : BaseActivity<ActivityCreateClaimBinding, CreateClaimVM>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_create_claim, CreateClaimVM::class.java)

    private lateinit var selectedSitesAdapter: SelectedSitesAdapter
    private lateinit var selectedTravelingEntriesAdapter: SelectedTravelingEntriesAdapter
    private lateinit var selectedDaEntriesAdapter:AddDaExpenseEntriesAdapter
    private lateinit var selectedHotelEntriesAdapter: SelectedHotelEntriesAdapter
    private lateinit var selectedOthersEntriesAdapter: SelectedOtherEntriesAdapter

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpToolbar()
        setUpSelectedSitesRecycler()
        setUpSelectedTravelingEntriesRecycler()
        setUpSelectedDAEntriesRecycler()
        setUpSelectedHotelEntriesRecycler()
        setUpSelectedOthersEntriesRecycler()
        setListeners()
    }

    private fun setUpToolbar() {
        binding.createReimbursementToolbar.toolbarTitle.text = getString(R.string.create_claim)
        binding.createReimbursementToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setUpSelectedSitesRecycler() {
        selectedSitesAdapter = SelectedSitesAdapter { site ->
            viewModel.removeMultipleSite(site)
        }
        binding.rvSelectedSites.layoutManager = LinearLayoutManager(this)
        binding.rvSelectedSites.adapter = selectedSitesAdapter
    }

    private fun setUpSelectedTravelingEntriesRecycler() {
        selectedTravelingEntriesAdapter = SelectedTravelingEntriesAdapter { position ->
            viewModel.removeTravelExpense(position)
        }
        binding.rvTravelEntries.layoutManager = LinearLayoutManager(this)
        binding.rvTravelEntries.adapter = selectedTravelingEntriesAdapter
    }

    private fun setUpSelectedDAEntriesRecycler() {
        selectedDaEntriesAdapter = AddDaExpenseEntriesAdapter { position ->
            viewModel.removeDAExpense(position)
        }
        binding.rvDAEntries.layoutManager = LinearLayoutManager(this)
        binding.rvDAEntries.adapter = selectedDaEntriesAdapter
    }

    private fun setUpSelectedHotelEntriesRecycler() {
        selectedHotelEntriesAdapter = SelectedHotelEntriesAdapter { position ->
            viewModel.removeHotelExpense(position)
        }
        binding.rvHotelEntries.layoutManager = LinearLayoutManager(this)
        binding.rvHotelEntries.adapter = selectedHotelEntriesAdapter
    }

    private fun setUpSelectedOthersEntriesRecycler() {
        selectedOthersEntriesAdapter = SelectedOtherEntriesAdapter { position ->
            viewModel.removeOtherExpense(position)
        }
        binding.rvOtherEntries.layoutManager = LinearLayoutManager(this)
        binding.rvOtherEntries.adapter = selectedOthersEntriesAdapter
    }

    private fun setListeners() {
        binding.dateEt.setOnClickListener {
            showDatePicker()
        }

    }

    private fun showDatePicker() {
        DateUtils.onDateClickWithLimit(this, object : DateUtils.DateCallBack {
            override fun onDateSelected(date: String, formatDate: String) {
                binding.dateString = date
            }
        }, false)
    }

    override fun subscribeToEvents(vm: CreateClaimVM) {
        binding.vm = vm

        vm.selectedMultiSiteList.observe(this) { list ->
            selectedSitesAdapter.submitList(list)
        }

        vm.travelingEntriesList.observe(this) { list ->
            selectedTravelingEntriesAdapter.submitList(list)
        }

        vm.daEntriesList.observe(this) { list ->
            selectedDaEntriesAdapter.submitList(list)
        }

        vm.hotelEntriesList.observe(this) { list ->
            selectedHotelEntriesAdapter.submitList(list)
        }

        vm.othersEntriesList.observe(this) { list ->
            selectedOthersEntriesAdapter.submitList(list)
        }

        vm.clickEvents.observe(this) { event ->
            handleClickEvents(event)
        }
    }

    private fun handleClickEvents(event: CreateClaimClickEvents) {
        when (event) {
            CreateClaimClickEvents.ON_SINGLE_SITE_CLICK -> {
                binding.isMultiSite = false
                viewModel.clearSelectedMultiSites()
            }

            CreateClaimClickEvents.ON_MULTI_SITE_CLICK -> {
                binding.isMultiSite = true
                viewModel.clearSelectedSingleSite()
            }

            CreateClaimClickEvents.ON_LOCAL_CLAIM_CLICK -> {
                binding.isOutstationExpense = false
            }

            CreateClaimClickEvents.ON_OUTSTATION_CLAIM_CLICK -> {
                binding.isOutstationExpense = true
            }

            CreateClaimClickEvents.ON_ADD_MULTIPLE_SITE_CLICK -> {
                addSiteDetailsActivityResultLauncher.launch(
                    Intent(
                        this,
                        AddMutilSiteDetailsActivity::class.java
                    )
                )
            }

            CreateClaimClickEvents.ON_SINGLE_SITE_DROPDOWN_CLICK -> {

            }

            CreateClaimClickEvents.ON_ADD_TRAVELING_EXPENSE_CLICK -> {
                addTravelingDetailsActivityResultLauncher.launch(
                    Intent(
                        this,
                        AddTravelingDetailActivity::class.java
                    )
                )
            }

            CreateClaimClickEvents.ON_ADD_DAILY_ALLOWANCE_CLICK -> {
                showEnterDaBottomSheet()
            }

            CreateClaimClickEvents.ON_ADD_HOTEL_EXPENSE_CLICK -> {
                showEnterHotelBottomSheet()
            }

            CreateClaimClickEvents.ON_ADD_OTHERS_EXPENSE_CLICK -> {
                showEnterOthersBottomSheet()
            }

            CreateClaimClickEvents.ON_CANCEL_CLICK -> {
                finish()
            }
        }
    }

    private val addSiteDetailsActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val siteID = data?.getLongExtra(SharingKeys.SITE_ID, -1) ?: -1
            val purpose = data?.getStringExtra(SharingKeys.SITE_PURPOSE) ?: ""
            viewModel.addMultipleSite(MultipleSite(siteID, purpose))
        }
    }

    private val addTravelingDetailsActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val travelExpenseData: TravelExpense? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    data?.getParcelableExtra(
                        SharingKeys.TRAVELING_DETAILS,
                        TravelExpense::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    data?.getParcelableExtra(SharingKeys.TRAVELING_DETAILS)
                }
            if (travelExpenseData != null)
                viewModel.addTravelExpense(travelExpenseData)
        }
    }

    private fun showEnterDaBottomSheet() {
        var dialog = EnterDaBottomSheet(getString(R.string.add_da_entery)){ amount,path ->
            var daExpense = DAExpense(
                selectedDaEntriesAdapter?.itemCount?.plus(1).toString(),
                amount = amount,
                receiptAttachment = path
            )
            viewModel.addDAExpense(daExpense)
        }
        dialog.show(supportFragmentManager, "EnterDaBottomSheet")
    }

    private fun  showEnterHotelBottomSheet() {
        var dialog = EnterDaBottomSheet(getString(R.string.add_hotel_entry)){ amount,path ->
            var hotelExpense = HotelExpense(
                selectedHotelEntriesAdapter?.itemCount?.plus(1).toString(),
                amount = amount,
                receiptAttachment = path
            )
            viewModel.addHotelExpense(hotelExpense)
        }
        dialog.show(supportFragmentManager, "EnterHotelBottomSheet")
    }

    private fun showEnterOthersBottomSheet() {
        var dialog = EnterOthersBottomSheet{ category,amount,path ->
            var othersExpense = OtherExpense(
                selectedOthersEntriesAdapter?.itemCount?.plus(1).toString(),
                category = category,
                amount = amount,
                receiptAttachment = path
            )
            viewModel.addOtherExpense(othersExpense)
        }
        dialog.show(supportFragmentManager, "EnterOtherBottomSheet")
    }
}
