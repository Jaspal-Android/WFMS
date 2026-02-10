package com.atvantiq.wfms.ui.screens.reimbursement.claimDetails

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.ActivityClaimDetailBinding
import com.atvantiq.wfms.models.reimbursement.detail.ClaimData
import com.atvantiq.wfms.models.reimbursement.detail.ClaimDetailResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.reimbursement.ReimbursementViewModel
import retrofit2.HttpException

class ClaimDetailActivity : BaseActivity<ActivityClaimDetailBinding,ReimbursementViewModel>() {

    private val siteAdapter by lazy { SiteAdapter() }

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_claim_detail, ReimbursementViewModel::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        handleToolbar()
        setupSitesRecycler()
        handleIntentData()
    }

    private fun setupSitesRecycler() {
        binding.recyclerViewSites.apply {
            layoutManager = LinearLayoutManager(this@ClaimDetailActivity)
            adapter = siteAdapter
            setHasFixedSize(false)
        }
    }

    private fun handleToolbar(){
        binding.claimDetailsToolbar.toolbarTitle.text = getString(R.string.claim_details)
        binding.claimDetailsToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun handleIntentData() {
        val claimId = intent.getLongExtra(SharingKeys.CLAIM_ID, -1L)
        if (claimId != -1L) {
            viewModel.getClaimById(claimId)
        }
    }

    override fun subscribeToEvents(vm: ReimbursementViewModel) {
        binding.viewModel = vm
        vm.claimByIdResponse.observe(this) { response ->
            handleClaimByIdResponse(response)
        }
    }

    private fun handleClaimByIdResponse(response: ApiState<ClaimDetailResponse>) {
        when (response.status) {
            Status.LOADING -> showProgress()
            Status.SUCCESS -> {
                dismissProgress()
                when (response.response?.code) {
                    200 -> {
                        val claim = response.response?.data
                        setDataOnUI(claim)
                    }
                    else -> {
                        handleErrorResponse(response.response?.code ?: 0, response.response?.message)
                    }
                }
            }
            Status.ERROR -> {
                dismissProgress()
                handleError(response.throwable)
            }
        }
    }

    private fun handleErrorResponse(code: Int, message: String?) {
        if (code == 401) tokenExpiresAlert() else alertDialogShow(this, getString(R.string.alert), message ?: getString(R.string.something_went_wrong))
    }

    private fun handleError(throwable: Throwable?) {
        if (throwable is HttpException) {
            if (throwable.code() == 401) {
                tokenExpiresAlert()
            }else{
                showToast(this, throwable.message())
            }
        } else {
            showToast(this, throwable?.message ?: getString(R.string.something_went_wrong))
        }
    }

    private fun setDataOnUI(claim: ClaimData?) {
        binding.claim = claim
        siteAdapter.submitList(claim?.sites.orEmpty())
    }
}
