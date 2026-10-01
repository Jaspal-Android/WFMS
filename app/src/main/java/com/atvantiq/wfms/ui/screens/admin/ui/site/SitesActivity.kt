package com.atvantiq.wfms.ui.screens.admin.ui.site

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.base.PagedListUiState
import com.atvantiq.wfms.databinding.ActivitySitesBinding
import com.atvantiq.wfms.models.site.allSites.Site
import com.atvantiq.wfms.models.site.allSites.SitesListAllResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.AllSitesAdapter
import com.atvantiq.wfms.ui.screens.admin.ui.site.addSite.AddSiteActivity
import com.atvantiq.wfms.widgets.PaginationScrollListener

class SitesActivity : BaseActivity<ActivitySitesBinding, SitesVM>() {

    private var adapter: AllSitesAdapter? = null
    private var isProgressShown = false

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_sites, SitesVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSitesToolbar()
        setSitesList()
        swipeRefresh()
        // Shows the sites already loaded (e.g. after rotation) and refreshes them in the background.
        viewModel.sites.open()
    }

    private fun setSitesToolbar(){
        binding.toolbarTitle.text = getString(R.string.sites)
        binding.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: SitesVM) {
        binding.vm = vm

        vm.sites.state.observe(this) { state -> renderSites(state) }

        vm.sites.failure.observe(this) { failure ->
            if (!failure.consumeOnce()) return@observe
            handleSitesFailure(failure)
        }

        vm.clickEvents.observe(this) { event ->
            when (event) {
                SitesEventClicks.ON_ADD_STIE_CLICK -> {
                    val intent = Intent(this, AddSiteActivity::class.java)
                    createSiteLauncher.launch(intent)
                }
            }
        }
    }

    private fun renderSites(state: PagedListUiState<Site>) {
        showFirstPageProgress(state.isLoadingFirstPage)
        if (!state.isLoadingFirstPage && !state.isRefreshing) stopRefreshingData()
        adapter?.submitList(state.items)
        adapter?.showLoadingFooter(state.isLoadingMore)
        binding.isEmptySites = state.isEmpty
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

    private fun swipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.sites.refresh()
        }
    }

    private fun stopRefreshingData() {
        if (binding.swipeRefreshLayout.isRefreshing) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private val createSiteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.sites.refresh()
            }
        }

}
