package com.atvantiq.wfms.ui.screens.reimbursement

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.FragmentReimbursementBinding
import com.atvantiq.wfms.models.reimbursement.allClaims.AllClaimsResponse
import com.atvantiq.wfms.models.reimbursement.allClaims.Record
import com.atvantiq.wfms.models.work.workAssigned.Site
import com.atvantiq.wfms.models.work.workAssigned.WorkAssignedResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.AllClaimsAdapter
import com.atvantiq.wfms.ui.screens.adapters.AssignedTasksListAdapter
import com.atvantiq.wfms.ui.screens.adapters.AttendanceOptionsAdapter
import com.atvantiq.wfms.ui.screens.attendance.AttendanceClickEvents
import com.atvantiq.wfms.ui.screens.attendance.addSignInActivity.AddSignInActivity
import com.atvantiq.wfms.ui.screens.attendance.myProgress.MyProgressActivity
import com.atvantiq.wfms.ui.screens.attendance.signInDetails.SignInDetailActivity
import com.atvantiq.wfms.ui.screens.reimbursement.claimApprovals.ClaimApprovalsActivity
import com.atvantiq.wfms.ui.screens.reimbursement.claimDetails.ClaimDetailActivity
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.CreateClaimActivity
import com.atvantiq.wfms.ui.screens.reimbursement.myClaims.MyClaimsActivity
import com.atvantiq.wfms.utils.Utils
import com.atvantiq.wfms.widgets.DividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException

@AndroidEntryPoint
class ReimbursementFragment : BaseFragment<FragmentReimbursementBinding, ReimbursementViewModel>() {
    private var adapter: AllClaimsAdapter? = null
    private var page: Int = 1
    private var pageSize: Int = 10
    private var isLoading = false
    private var isLastPage = false

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_reimbursement, ReimbursementViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun subscribeToEvents(vm: ReimbursementViewModel) {
        binding.vm = vm
        vm.clickEvents.observe(viewLifecycleOwner) { event ->
            if (!isLifeCycleResumed()) return@observe
            handleClickEvents(event)

        }

        vm.allClaimsResponse.observe(viewLifecycleOwner) { response ->
            if (!isLifeCycleStarted()) return@observe
            handleAllClaimsResponse(response)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        setUpAllClaimsList()
        swipeRefresh()
        page = 1
        isLastPage = false
        adapter?.submitList(emptyList()) // Clear adapter data
        getAllClaims() // Fetch data only on first creation
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
                response.response?.let {
                    if (it.code == 200) {
                        if (it?.data?.records == null) {
                            handleAllClaimsSuccess(emptyList())
                        }else{
                            handleAllClaimsSuccess(it.data?.records)
                        }
                    } else {
                        handleErrorResponse(it.code, it.message)
                    }
                }
            }
            Status.ERROR -> handleError(response.throwable)
            Status.LOADING -> showLoadingIndicator()
        }
    }

    private fun handleAllClaimsSuccess(records: List<Record>) {
        adapter?.removeLoadingFooter() // Always remove loading footer before updating list
        if (page == 1) {
            adapter?.submitList(emptyList()) // Clear adapter data on refresh
        }
        if (records.isEmpty()) {
            isLastPage = true
            if (page == 1) emptyDataLayout() else adapter?.removeLoadingFooter()
        } else {
            mainLayout()
            if (page == 1) {
                adapter?.submitList(records)
            } else {
                adapter?.addData(records)
            }
            if (records.size < pageSize) {
                isLastPage = true
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
        isLoading = false
        if (throwable is HttpException && throwable.code() == 401) {
            tokenExpiresAlert()
        } else {
            showToast(requireContext(), throwable?.message ?: getString(R.string.something_went_wrong))
        }
    }

    private fun showLoadingIndicator() {
        if (page == 1) showProgress() else {
            adapter?.removeLoadingFooter() // Remove any existing loading footer before adding
            adapter?.addLoadingFooter()
        }
    }

    private fun getAllClaims() {
        if (isLoading || isLastPage)
            return
        isLoading = true
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
                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                        ) {
                            page += 1
                            getAllClaims()
                        }
                    }
                }
            }
        })

        adapter = AllClaimsAdapter(onClaimClicked = { claim, position ->
            Utils.jumpActivityWithData(
                requireActivity(),
                ClaimDetailActivity::class.java,
                Bundle().apply {
                    putLong(SharingKeys.CLAIM_ID, claim.claimId ?: 0)
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
        isLoading = false
        adapter?.removeLoadingFooter() // Hide loading footer
        binding.isEmptyReimbursements = false
    }

    private fun emptyDataLayout() {
        isLoading = false
        adapter?.removeLoadingFooter() // Hide loading footer
        if (adapter?.count() ?: 0 <= 0) {
            binding.isEmptyReimbursements = true
        }
    }

    private fun swipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            startRefreshingData()
        }
    }

    private fun startRefreshingData() {
        page = 1
        isLastPage = false // Reset last page flag
        adapter?.removeLoadingFooter() // Remove loading footer on refresh
        adapter?.submitList(emptyList()) // Clear adapter data on refresh
        viewModel.getAllClaims(page, pageSize)
    }

    private fun stopRefreshingData() {
        if (binding.swipeRefreshLayout.isRefreshing) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private val createClaimLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {

            }
        }
}