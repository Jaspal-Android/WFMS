package com.atvantiq.wfms.ui.screens.admin.ui.site

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.base.PagedListUiState
import com.atvantiq.wfms.databinding.FragmentSitesBinding
import com.atvantiq.wfms.models.site.allSites.Site
import com.atvantiq.wfms.models.site.allSites.SitesListAllResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.AllSitesAdapter
import com.atvantiq.wfms.ui.screens.admin.ui.site.addSite.AddSiteActivity
import com.atvantiq.wfms.widgets.PaginationScrollListener
import dagger.hilt.android.AndroidEntryPoint

/** Sites tab (admins with the `Site` permission): Add Site, then the active sites, paged. */
@AndroidEntryPoint
class SitesFragment : BaseFragment<FragmentSitesBinding, SitesVM>() {

    private var adapter: AllSitesAdapter? = null
    private var isProgressShown = false

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_sites, SitesVM::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setSitesList()
        binding.swipeRefreshLayout.setOnRefreshListener { viewModel.sites.refresh() }
        binding.errorState.onRetry = View.OnClickListener { viewModel.sites.refresh() }
        // Shows the sites already loaded (e.g. returning to the tab) and refreshes them in the background.
        viewModel.sites.open()
    }

    override fun subscribeToEvents(vm: SitesVM) {
        binding.vm = vm

        vm.sites.state.observe(viewLifecycleOwner) { state -> renderSites(state) }

        vm.sites.failure.observe(viewLifecycleOwner) { failure ->
            if (!failure.consumeOnce()) return@observe
            handleSitesFailure(failure)
        }

        vm.clickEvents.observe(viewLifecycleOwner) { event ->
            when (event) {
                SitesEventClicks.ON_ADD_STIE_CLICK ->
                    createSiteLauncher.launch(Intent(requireContext(), AddSiteActivity::class.java))
            }
        }
    }

    private fun renderSites(state: PagedListUiState<Site>) {
        showFirstPageProgress(state.isLoadingFirstPage)
        if (!state.isLoadingFirstPage && !state.isRefreshing) stopRefreshingData()
        adapter?.submitList(state.items, state.changedPosition)
        adapter?.showLoadingFooter(state.isLoadingMore)
        binding.isEmptySites = state.isEmpty
        binding.errorState.root.isVisible = state.isFirstPageFailed
    }

    private fun showFirstPageProgress(show: Boolean) {
        if (show == isProgressShown) return
        isProgressShown = show
        if (show) showProgress() else dismissProgress()
    }

    private fun handleSitesFailure(failure: ApiState<SitesListAllResponse>) {
        if (failure.status == Status.SUCCESS) {
            // The server rejected the page.
            failure.response?.let { handleRejectedResponse(it.code, it.message) }
        } else {
            handleApiFailure(failure.throwable)
        }
    }

    private fun setSitesList() {
        binding.rvSites.addOnScrollListener(PaginationScrollListener { viewModel.sites.loadNextPage() })
        adapter = AllSitesAdapter()
        binding.rvSites.adapter = adapter
    }

    private fun stopRefreshingData() {
        if (binding.swipeRefreshLayout.isRefreshing) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        // A progress dialog still up when the tab is left would outlive this view.
        if (isProgressShown) dismissProgress()
        isProgressShown = false
        adapter = null
        super.onDestroyView()
    }

    private val createSiteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.sites.refresh()
            }
        }
}
