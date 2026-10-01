package com.atvantiq.wfms.ui.screens.reimbursement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
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
import com.atvantiq.wfms.utils.PagedListState
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException

@AndroidEntryPoint
class ReimbursementFragment : BaseFragment<FragmentReimbursementBinding, ReimbursementViewModel>() {
    private var adapter: AllClaimsAdapter? = null
    private val pageSize: Int = 10
    private val paging = PagedListState(pageSize)

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_reimbursement, ReimbursementViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpAllClaimsList()
        swipeRefresh()
        loadFirstPage()
    }

    override fun subscribeToEvents(vm: ReimbursementViewModel) {
        binding.vm = vm
        vm.clickEvents.observe(viewLifecycleOwner) { event ->
            if (!isLifeCycleResumed()) return@observe
            handleClickEvents(event)

        }

        vm.allClaimsResponse.observe(viewLifecycleOwner) { response ->
            if (!isLifeCycleStarted()) return@observe
            // The ViewModel outlives the view (back stack, drawer re-entry), so a new observer is
            // handed the previous visit's last result. Applying it as this visit's first page
            // made the real page look unrequested and left the loading footer spinning.
            if (!response.consumeOnce()) return@observe
            handleAllClaimsResponse(response)
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

    private fun handleAllClaimsResponse(response: ApiState<AllClaimsResponse>) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                stopRefreshingData()
                val body = response.response
                if (body != null && body.code == 200) {
                    handleAllClaimsSuccess(body.data?.records ?: emptyList())
                } else {
                    // Rejected or empty: release the lock so the same page is retried on scroll.
                    paging.onRequestFailed()
                    adapter?.removeLoadingFooter()
                    if (body != null) handleErrorResponse(body.code, body.message)
                }
            }
            Status.ERROR -> handleError(response.throwable)
            Status.LOADING -> showLoadingIndicator()
        }
    }

    private fun handleAllClaimsSuccess(records: List<Record>) {
        // null: no page was awaited (e.g. a replayed old result), so there is nothing to apply.
        val isFirstPage = paging.onPageReceived(records.size) ?: return
        adapter?.removeLoadingFooter() // Always remove loading footer before updating list
        if (isFirstPage) {
            adapter?.submitList(emptyList()) // Clear adapter data on refresh
        }
        if (records.isEmpty()) {
            if (isFirstPage) emptyDataLayout() else adapter?.removeLoadingFooter()
        } else {
            mainLayout()
            if (isFirstPage) {
                adapter?.submitList(records)
            } else {
                adapter?.addData(records)
            }
        }
    }

    private fun handleErrorResponse(code: Int?, message: String?) {
        if (code == 401) tokenExpiresAlert() else alertDialogShow(requireContext(), getString(R.string.alert), message ?: getString(R.string.something_went_wrong))
    }

    private fun handleError(throwable: Throwable?) {
        dismissProgress()
        stopRefreshingData()
        adapter?.removeLoadingFooter()
        paging.onRequestFailed()
        if (throwable is HttpException && throwable.code() == 401) {
            tokenExpiresAlert()
        } else {
            showToast(requireContext(), throwable?.message ?: getString(R.string.something_went_wrong))
        }
    }

    private fun showLoadingIndicator() {
        when {
            paging.isLoadingFirstPage -> showProgress()
            // Only while a later page is actually awaited; otherwise nothing would remove it.
            paging.isLoading -> {
                adapter?.removeLoadingFooter() // Remove any existing loading footer before adding
                adapter?.addLoadingFooter()
            }
        }
    }

    private fun loadNextPage() {
        paging.startNextPage()?.let { fetchPage(it) }
    }

    // Start and refresh both come here. It supersedes any request still in flight, and the
    // ViewModel cancels that request, so a slow old answer can never overwrite this one.
    private fun loadFirstPage() {
        adapter?.removeLoadingFooter() // Remove loading footer on refresh
        adapter?.submitList(emptyList()) // Clear adapter data on refresh
        fetchPage(paging.restart())
    }

    private fun fetchPage(page: Int) {
        if (page != 1) {
            adapter?.addLoadingFooter() // Show loading footer only for next pages
        }
        viewModel.getAllClaims(page, pageSize)
    }

    private fun setUpAllClaimsList() {
        binding.rvReimbursements.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                if (dy > 0) {
                    if (!paging.isLoading && !paging.isLastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                        ) {
                            loadNextPage()
                        }
                    }
                }
            }
        })

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

    private fun mainLayout() {
        adapter?.removeLoadingFooter() // Hide loading footer
        binding.isEmptyReimbursements = false
    }

    private fun emptyDataLayout() {
        adapter?.removeLoadingFooter() // Hide loading footer
        if ((adapter?.count() ?: 0) <= 0) {
            binding.isEmptyReimbursements = true
        }
    }

    private fun swipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            startRefreshingData()
        }
    }

    private fun startRefreshingData() {
        loadFirstPage()
    }

    private fun stopRefreshingData() {
        if (binding.swipeRefreshLayout.isRefreshing) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private val claimDetailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startRefreshingData()
            }
        }

    private val createClaimLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
               startRefreshingData()
            }
        }
}
