package com.atvantiq.wfms.ui.screens.reimbursement.createClaim.addTravelDetail

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.base.BaseBindingActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.ActivityAddTravelingDetailBinding

class AddTravelingDetailActivity : BaseActivity<ActivityAddTravelingDetailBinding,AddTravelDetailViewModel>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_add_traveling_detail, AddTravelDetailViewModel::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        handleToolbar()
    }

    private fun handleToolbar() {
        binding.addTrevelingToolbar.toolbarTitle.text = getString(R.string.add_travel_entry)
        binding.addTrevelingToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: AddTravelDetailViewModel) {
        binding.vm  = vm

        vm.clickEvents.observe(this) { event ->
            handleClickEvents(event)
        }
    }

    private fun handleClickEvents(event: AddTravelingClickEvents) {
        when (event) {
            AddTravelingClickEvents.SELECT_TRAVEL_MODE -> {

            }
            AddTravelingClickEvents.SELECT_TRAVELING_WITH -> {
                // Handle traveling with selection
            }
            AddTravelingClickEvents.ATTACHMENT_CLICK -> {
                // Handle attachment click
            }
            AddTravelingClickEvents.ON_DONE_CLICK -> {
                handleOnDoneClick()
            }
            AddTravelingClickEvents.ON_CANCEL_CLICK -> {
                finish()
            }
        }
    }

    private fun handleOnDoneClick() {
        val travelExpense = viewModel.createTravelDetail()
        val intent = Intent().apply {
            putExtra(SharingKeys.TRAVELING_DETAILS, travelExpense)
        }
        setResult(RESULT_OK, intent)
        finish()
    }
}