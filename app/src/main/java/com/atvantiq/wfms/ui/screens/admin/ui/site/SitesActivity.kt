package com.atvantiq.wfms.ui.screens.admin.ui.site

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.databinding.ActivitySitesBinding
import com.atvantiq.wfms.models.site.allSites.Site
import com.atvantiq.wfms.models.site.allSites.SitesListAllResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.AllSitesAdapter
import com.atvantiq.wfms.ui.screens.admin.ui.site.addSite.AddSiteActivity
import com.atvantiq.wfms.utils.PagedListState
import retrofit2.HttpException

class SitesActivity : BaseActivity<ActivitySitesBinding, SitesVM>() {

    private var adapter: AllSitesAdapter? = null
    private val pageSize: Int = 10
    private val paging = PagedListState(pageSize)

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
        loadFirstPage()
    }

    private fun setSitesToolbar(){
        binding.toolbarTitle.text = getString(R.string.sites)
        binding.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: SitesVM) {
        binding.vm = vm

        vm.allSitesResponse.observe(this) { response ->
            handleSiteAllResponse(response)
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

    private fun handleSiteAllResponse(response: ApiState<SitesListAllResponse>) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                stopRefreshingData()
                val body = response.response
                if (body != null && body.code == 200) {
                    handleSitesSuccess(body.data.sites)
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

    private fun handleSitesSuccess(records: List<Site>) {
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

    private fun handleErrorResponse(code: Int, message: String?) {
        if (code == 401) tokenExpiresAlert() else alertDialogShow(this, getString(R.string.alert), message ?: getString(R.string.something_went_wrong))
    }

    private fun handleError(throwable: Throwable?) {
        dismissProgress()
        stopRefreshingData()
        adapter?.removeLoadingFooter()
        paging.onRequestFailed()
        if (throwable is HttpException && throwable.code() == 401) {
            tokenExpiresAlert()
        } else {
            showToast(this, throwable?.message ?: getString(R.string.something_went_wrong))
        }
    }

    private fun showLoadingIndicator() {
        if (paging.isLoadingFirstPage) showProgress() else {
            adapter?.removeLoadingFooter() // Remove any existing loading footer before adding
            adapter?.addLoadingFooter()
        }
    }

    private fun loadNextPage() {
        paging.startNextPage()?.let { fetchPage(it) }
    }

    // Start, refresh and "site created" all come here. It supersedes any request still in flight,
    // and the ViewModel cancels that request, so a slow old answer can never overwrite this one.
    private fun loadFirstPage() {
        adapter?.removeLoadingFooter() // Remove loading footer on refresh
        adapter?.submitList(emptyList()) // Clear adapter data on refresh
        fetchPage(paging.restart())
    }

    private fun fetchPage(page: Int) {
        if (page != 1) {
            adapter?.addLoadingFooter() // Show loading footer only for next pages
        }
        viewModel.getAllSites(page, pageSize)
    }

    private fun setSitesList() {
        binding.rvSites.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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

        adapter = AllSitesAdapter ()
       /* binding.rvSites.addItemDecoration(
            DividerItemDecoration(
                this,
                R.drawable.custom_divider
            )
        )*/
        binding.rvSites.adapter = adapter
    }

    private fun mainLayout() {
        adapter?.removeLoadingFooter() // Hide loading footer
        binding.isEmptySites = false
    }

    private fun emptyDataLayout() {
        adapter?.removeLoadingFooter() // Hide loading footer
        if (adapter?.count() ?: 0 <= 0) {
            binding.isEmptySites = true
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

    private val createSiteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startRefreshingData()
            }
        }

}