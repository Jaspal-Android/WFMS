package com.atvantiq.wfms.ui.screens.reimbursement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.base.PagedListUiState
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.FragmentReimbursementBinding
import com.atvantiq.wfms.models.reimbursement.allClaims.AllClaimsResponse
import com.atvantiq.wfms.models.reimbursement.allClaims.Record
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.AllClaimsAdapter
import com.atvantiq.wfms.ui.screens.reimbursement.claimDetails.ClaimDetailActivity
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.CreateClaimActivity
import com.atvantiq.wfms.widgets.DividerItemDecoration
import com.atvantiq.wfms.widgets.PaginationScrollListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReimbursementFragment : BaseFragment<FragmentReimbursementBinding, ReimbursementViewModel>() {
    private var adapter: AllClaimsAdapter? = null
    private var isProgressShown = false

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_reimbursement, ReimbursementViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpAllClaimsList()
        swipeRefresh()
        // Shows the claims already loaded (if any) and refreshes them in the background.
        viewModel.claims.open()
    }

    override fun subscribeToEvents(vm: ReimbursementViewModel) {
        binding.vm = vm
        vm.clickEvents.observe(viewLifecycleOwner) { event ->
            handleClickEvents(event)
        }

        vm.claims.state.observe(viewLifecycleOwner) { state -> renderClaims(state) }

        vm.claims.failure.observe(viewLifecycleOwner) { failure ->
            if (!failure.consumeOnce()) return@observe
            handleClaimsFailure(failure)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Intentionally empty: avoid attaching adapters / triggering loads during restore.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvReimbursements.adapter = null // Avoid memory leaks
    }

    private fun handleClickEvents(event: ReimbursementClickEvents) {
        when (event) {
            ReimbursementClickEvents.ON_CLICK_CREATE_CLAIM -> {
                val intent = Intent(requireContext(), CreateClaimActivity::class.java)
                createClaimLauncher.launch(intent)
            }
        }
    }

    private fun renderClaims(state: PagedListUiState<Record>) {
        showFirstPageProgress(state.isLoadingFirstPage)
        if (!state.isLoadingFirstPage && !state.isRefreshing) stopRefreshingData()
        adapter?.submitList(state.items)
        adapter?.showLoadingFooter(state.isLoadingMore)
        binding.isEmptyReimbursements = state.isEmpty
    }

    private fun showFirstPageProgress(show: Boolean) {
        if (show == isProgressShown) return
        isProgressShown = show
        if (show) showProgress() else dismissProgress()
    }

    private fun handleClaimsFailure(failure: ApiState<AllClaimsResponse>) {
        if (failure.status == Status.SUCCESS) {
            // The server rejected the page.
            failure.response?.let { handleRejectedResponse(it.code, it.message) }
        } else {
            handleApiFailure(failure.throwable)
        }
    }

    private fun setUpAllClaimsList() {
        binding.rvReimbursements.addOnScrollListener(PaginationScrollListener { viewModel.claims.loadNextPage() })

        adapter = AllClaimsAdapter(onClaimClicked = { claim, _ ->
             claimDetailLauncher.launch(
                Intent(requireContext(), ClaimDetailActivity::class.java).apply {
                    putExtra(SharingKeys.CLAIM_ID, claim.claimId ?: 0L)
                }
            )
        })
        binding.rvReimbursements.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                R.drawable.custom_divider
            )
        )
        binding.rvReimbursements.adapter = adapter
    }

    private fun swipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.claims.refresh()
        }
    }

    private fun stopRefreshingData() {
        if (binding.swipeRefreshLayout.isRefreshing) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private val claimDetailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.claims.refresh()
            }
        }

    private val createClaimLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.claims.refresh()
            }
        }
}
