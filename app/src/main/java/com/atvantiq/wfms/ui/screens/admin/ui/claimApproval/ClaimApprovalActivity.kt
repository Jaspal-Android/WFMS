package com.atvantiq.wfms.ui.screens.admin.ui.claimApproval

import android.view.View
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.base.PagedListUiState
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.ActivityClaimApprovalBinding
import com.atvantiq.wfms.models.reimbursement.review.ClaimReviewListResponse
import com.atvantiq.wfms.models.reimbursement.review.ClaimReviewRecord
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.ClaimReviewAdapter
import com.atvantiq.wfms.widgets.PaginationScrollListener
import dagger.hilt.android.AndroidEntryPoint
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

/**
 * Claims Approval (spec 8.1): the claims to review. Search runs on the keyboard's Search key;
 * a claim approved on the detail screen updates its row in place.
 */
@AndroidEntryPoint
class ClaimApprovalActivity : BaseActivity<ActivityClaimApprovalBinding, ClaimApprovalVM>() {

    private var adapter: ClaimReviewAdapter? = null
    private var isProgressShown = false

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_claim_approval, ClaimApprovalVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        binding.toolbar.toolbarTitle.text = getString(R.string.claims_approval)
        binding.toolbar.toolbarBackButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        setUpSearch()
        setUpList()
        binding.swipeRefreshLayout.setOnRefreshListener { viewModel.claims.refresh() }
        viewModel.claims.open()
    }

    private fun setUpSearch() {
        with(binding.searchBar) {
            etSearch.setText(viewModel.appliedSearch)
            ivClearSearch.isVisible = viewModel.appliedSearch.isNotEmpty()
            etSearch.doAfterTextChanged { ivClearSearch.isVisible = !it.isNullOrEmpty() }
            etSearch.setOnEditorActionListener { view, actionId, _ ->
                if (actionId != EditorInfo.IME_ACTION_SEARCH) return@setOnEditorActionListener false
                hideSoftKeyboard(this@ClaimApprovalActivity)
                viewModel.search(view.text.toString())
                true
            }
            ivClearSearch.setOnClickListener {
                etSearch.setText("")
                viewModel.search("")
            }
        }
    }

    override fun subscribeToEvents(vm: ClaimApprovalVM) {
        vm.claimCount.observe(this) { count ->
            binding.countLabel = count?.let { resources.getQuantityString(R.plurals.claims_count, it, it) }
        }
        vm.claims.state.observe(this) { state -> renderClaims(state) }
        vm.claims.failure.observe(this) { failure ->
            if (!failure.consumeOnce()) return@observe
            handleFailure(failure)
        }
    }

    private fun setUpList() {
        adapter = ClaimReviewAdapter(onReview = ::openClaim)
        binding.rvClaims.adapter = adapter
        binding.rvClaims.addOnScrollListener(PaginationScrollListener { viewModel.claims.loadNextPage() })
    }

    private fun renderClaims(state: PagedListUiState<ClaimReviewRecord>) {
        showFirstPageProgress(state.isLoadingFirstPage)
        if (!state.isLoadingFirstPage && !state.isRefreshing) binding.swipeRefreshLayout.isRefreshing = false
        adapter?.submitList(state.items)
        adapter?.showLoadingFooter(state.isLoadingMore)
        binding.emptyState.root.isVisible = state.isEmpty
    }

    private fun showFirstPageProgress(show: Boolean) {
        if (show == isProgressShown) return
        isProgressShown = show
        if (show) showProgress() else dismissProgress()
    }

    private fun handleFailure(failure: ApiState<ClaimReviewListResponse>) {
        if (failure.status == Status.SUCCESS) {
            failure.response?.let { handleRejectedResponse(it.code, it.message) }
        } else {
            handleApiFailure(failure.throwable)
        }
    }

    private fun openClaim(claim: ClaimReviewRecord) {
        val claimId = claim.claimId ?: return showToast(this, getString(R.string.claim_id_missing))
        detailLauncher.launch(Intent(this, ClaimApprovalDetailActivity::class.java).apply {
            putExtra(SharingKeys.CLAIM_ID, claimId)
        })
    }

    /** A claim approved on the detail screen updates its row here. */
    private val detailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data ?: return@registerForActivityResult
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val claimId = data.getLongExtra(SharingKeys.CLAIM_ID, NO_ID)
            val approvedTotal = data.getDoubleExtra(SharingKeys.APPROVED_TOTAL, 0.0)
            if (claimId != NO_ID) viewModel.applyApproval(claimId, approvedTotal)
        }

    private companion object {
        const val NO_ID = -1L
    }
}
