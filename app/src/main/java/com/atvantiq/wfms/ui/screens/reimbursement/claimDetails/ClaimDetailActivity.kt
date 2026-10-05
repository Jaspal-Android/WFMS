package com.atvantiq.wfms.ui.screens.reimbursement.claimDetails

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ActivityClaimDetailBinding
import com.atvantiq.wfms.models.reimbursement.delete.DeleteClaimResponse
import com.atvantiq.wfms.models.reimbursement.detail.ClaimData
import com.atvantiq.wfms.models.reimbursement.detail.ClaimDetailResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.reimbursement.ReimbursementViewModel
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.CreateClaimActivity
import com.atvantiq.wfms.utils.serverMessage
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

@AndroidEntryPoint
class ClaimDetailActivity : BaseActivity<ActivityClaimDetailBinding,ReimbursementViewModel>() {

    private val siteAdapter by lazy { SiteAdapter() }
    private var claimId: Long = INVALID_CLAIM_ID

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_claim_detail, ReimbursementViewModel::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        handleToolbar()
        setupSitesRecycler()
        setupActions()
        handleIntentData()
    }

    private val editClaimLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Claim changed: tell the list to reload, and show the updated claim here.
                setResult(RESULT_OK)
                viewModel.getClaimById(claimId)
            }
        }

    private fun setupActions() {
        binding.btnDeleteClaim.setOnClickListener { confirmDeleteClaim() }
        binding.btnEditClaim.setOnClickListener {
            editClaimLauncher.launch(
                Intent(this, CreateClaimActivity::class.java)
                    .putExtra(SharingKeys.EDIT_CLAIM_ID, claimId)
            )
        }
    }

    private fun confirmDeleteClaim() {
        alertDialogShow(
            this,
            getString(R.string.delete_claim),
            getString(R.string.delete_claim_confirmation),
            getString(R.string.delete),
            { dialog, _ ->
                dialog.dismiss()
                viewModel.deleteClaim(claimId)
            },
            { dialog, _ -> dialog.dismiss() }
        )
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
        claimId = intent.getLongExtra(SharingKeys.CLAIM_ID, INVALID_CLAIM_ID)
        if (claimId != INVALID_CLAIM_ID) {
            viewModel.getClaimById(claimId)
        } else {
            alertDialogShow(
                this,
                getString(R.string.alert),
                getString(R.string.something_went_wrong)
            )
        }
    }

    override fun subscribeToEvents(vm: ReimbursementViewModel) {
        binding.viewModel = vm
        vm.claimByIdResponse.observe(this) { response ->
            handleClaimByIdResponse(response)
        }
        vm.deleteClaimResponse.observe(this) { response ->
            handleDeleteClaimResponse(response)
        }
    }

    private fun handleDeleteClaimResponse(response: ApiState<DeleteClaimResponse>) {
        when (response.status) {
            Status.LOADING -> showProgress()
            Status.SUCCESS -> {
                dismissProgress()
                val code = response.response?.code
                if (code == ValConstants.SUCCESS_CODE) {
                    // Tells the claim list to reload; the claim no longer exists.
                    setResult(RESULT_OK)
                    alertDialogShow(
                        this,
                        getString(R.string.success),
                        response.response?.message ?: getString(R.string.claim_deleted_successfully),
                        okLister = DialogInterface.OnClickListener { _, _ -> finish() }
                    )
                } else {
                    handleRejectedResponse(code ?: 0, response.response?.message)
                }
            }
            Status.ERROR -> {
                dismissProgress()
                handleError(response.throwable)
            }
        }
    }

    private fun handleClaimByIdResponse(response: ApiState<ClaimDetailResponse>) {
        when (response.status) {
            Status.LOADING -> showProgress()
            Status.SUCCESS -> {
                dismissProgress()
                when (response.response?.code) {
                    ValConstants.SUCCESS_CODE -> {
                        val claim = response.response?.data
                        setDataOnUI(claim)
                    }
                    else -> {
                        handleRejectedResponse(response.response?.code ?: 0, response.response?.message)
                    }
                }
            }
            Status.ERROR -> {
                dismissProgress()
                handleError(response.throwable)
            }
        }
    }

    private fun handleError(throwable: Throwable?) {
        if (throwable is HttpException) {
            if (throwable.code() == ValConstants.UNAUTHORIZED_CODE) {
                tokenExpiresAlert()
            }else{
                alertDialogShow(
                    this,
                    getString(R.string.alert),
                    throwable.serverMessage() ?: getString(R.string.something_went_wrong)
                )
            }
        } else {
            showToast(this, throwable?.message ?: getString(R.string.something_went_wrong))
        }
    }

    private fun setDataOnUI(claim: ClaimData?) {
        binding.claim = claim
        siteAdapter.submitList(claim?.sites.orEmpty())
        updateActionBar(claim)
    }

    /**
     * Edit/Delete are offered on every loaded claim; the server is the source of truth and rejects a
     * claim that is already approved or disbursed. Client-side eligibility plugs in here.
     */
    private fun updateActionBar(claim: ClaimData?) {
        binding.claimActionBar.visibility = if (claim != null) View.VISIBLE else View.GONE
    }

    private companion object {
        const val INVALID_CLAIM_ID = -1L
    }
}
