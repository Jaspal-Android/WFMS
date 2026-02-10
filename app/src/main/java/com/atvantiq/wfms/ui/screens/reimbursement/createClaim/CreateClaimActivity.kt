package com.atvantiq.wfms.ui.screens.reimbursement.createClaim

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.AppListData
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ActivityCreateClaimBinding
import com.atvantiq.wfms.models.allProjects.AllProjectsResponse
import com.atvantiq.wfms.models.allProjects.Circle
import com.atvantiq.wfms.models.allProjects.Project
import com.atvantiq.wfms.models.reimbursement.DAExpense
import com.atvantiq.wfms.models.reimbursement.HotelExpense
import com.atvantiq.wfms.models.reimbursement.OtherExpense
import com.atvantiq.wfms.models.reimbursement.TravelExpense
import com.atvantiq.wfms.models.reimbursement.create.CreateClaimResponse
import com.atvantiq.wfms.models.site.SiteData
import com.atvantiq.wfms.models.site.SiteListByProjectResponse
import com.atvantiq.wfms.models.workSiteByDate.Site
import com.atvantiq.wfms.models.workSiteByDate.WorkSiteByDateResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.dialogs.MultiSelectBottomSheetDialog
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.adapters.AddDaExpenseEntriesAdapter
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.adapters.SelectedHotelEntriesAdapter
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.adapters.SelectedOtherEntriesAdapter
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.adapters.SelectedSitesAdapter
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.adapters.SelectedTravelingEntriesAdapter
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.addTravelDetail.AddTravelingDetailActivity
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.dialogs.EnterDaBottomSheet
import com.atvantiq.wfms.ui.screens.reimbursement.createClaim.dialogs.EnterOthersBottomSheet
import com.atvantiq.wfms.utils.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException
import java.util.Locale

@AndroidEntryPoint
class CreateClaimActivity : BaseActivity<ActivityCreateClaimBinding, CreateClaimVM>() {

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_create_claim, CreateClaimVM::class.java)

    private lateinit var selectedSitesAdapter: SelectedSitesAdapter
    private lateinit var selectedTravelingEntriesAdapter: SelectedTravelingEntriesAdapter
    private lateinit var selectedDaEntriesAdapter: AddDaExpenseEntriesAdapter
    private lateinit var selectedHotelEntriesAdapter: SelectedHotelEntriesAdapter
    private lateinit var selectedOthersEntriesAdapter: SelectedOtherEntriesAdapter

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setUpToolbar()
        setUpSelectedSitesRecycler()
        setUpSelectedTravelingEntriesRecycler()
        setUpSelectedDAEntriesRecycler()
        setUpSelectedHotelEntriesRecycler()
        setUpSelectedOthersEntriesRecycler()
    }

    private fun setUpToolbar() {
        binding.createReimbursementToolbar.toolbarTitle.text = getString(R.string.create_claim)
        binding.createReimbursementToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setUpSelectedSitesRecycler() {
        selectedSitesAdapter = SelectedSitesAdapter { site ->
            viewModel.removeMultipleSite(site)
        }
        binding.rvSelectedSites.layoutManager = LinearLayoutManager(this)
        binding.rvSelectedSites.adapter = selectedSitesAdapter
    }

    private fun setUpSelectedTravelingEntriesRecycler() {
        selectedTravelingEntriesAdapter = SelectedTravelingEntriesAdapter { position ->
            viewModel.removeTravelExpense(position)
        }
        binding.rvTravelEntries.layoutManager = LinearLayoutManager(this)
        binding.rvTravelEntries.adapter = selectedTravelingEntriesAdapter
    }

    private fun setUpSelectedDAEntriesRecycler() {
        selectedDaEntriesAdapter = AddDaExpenseEntriesAdapter { position ->
            viewModel.removeDAExpense(position)
        }
        binding.rvDAEntries.layoutManager = LinearLayoutManager(this)
        binding.rvDAEntries.adapter = selectedDaEntriesAdapter
    }

    private fun setUpSelectedHotelEntriesRecycler() {
        selectedHotelEntriesAdapter = SelectedHotelEntriesAdapter { position ->
            viewModel.removeHotelExpense(position)
        }
        binding.rvHotelEntries.layoutManager = LinearLayoutManager(this)
        binding.rvHotelEntries.adapter = selectedHotelEntriesAdapter
    }

    private fun setUpSelectedOthersEntriesRecycler() {
        selectedOthersEntriesAdapter = SelectedOtherEntriesAdapter { position ->
            viewModel.removeOtherExpense(position)
        }
        binding.rvOtherEntries.layoutManager = LinearLayoutManager(this)
        binding.rvOtherEntries.adapter = selectedOthersEntriesAdapter
    }

    private fun showDatePicker() {
        DateUtils.onDateClickWithLimit(this, object : DateUtils.DateCallBack {
            override fun onDateSelected(date: String, formatDate: String) {
                binding.dateEt.error = null
                viewModel.date.set(date)
                viewModel.getWorkSitesByDate(date)
            }
        }, false)
    }

    override fun subscribeToEvents(vm: CreateClaimVM) {
        binding.vm = vm

        vm.selectedSitesIdList.observe(this) { list ->
            selectedSitesAdapter.submitList(list)
        }

        vm.travelingEntriesList.observe(this) { list ->
            selectedTravelingEntriesAdapter.submitList(list)
        }

        vm.daEntriesList.observe(this) { list ->
            selectedDaEntriesAdapter.submitList(list)
        }

        vm.hotelEntriesList.observe(this) { list ->
            selectedHotelEntriesAdapter.submitList(list)
        }

        vm.othersEntriesList.observe(this) { list ->
            selectedOthersEntriesAdapter.submitList(list)
        }

        vm.clickEvents.observe(this) { event ->
            handleClickEvents(event)
        }

        vm.errorEvents.observe(this) { event ->
            errorHandler(event)
        }

        vm.workSiteByDateResponse.observe(this) { response ->
            handleWorkBySiteResponse(response)
        }

        vm.allProjectsResponse.observe(this) { response ->
            handleAllProjectsResponse(response)
        }

        vm.siteListByProjectResponse.observe(this) { response ->
            handleSiteByProjectResponse(response)
        }
        vm.createClaimResponse.observe(this) { response ->
            handleCreateClaimResponse(response)
        }
    }

    private fun handleWorkBySiteResponse(response: ApiState<WorkSiteByDateResponse>) {
        when (response.status) {
            Status.LOADING -> showProgress()
            Status.SUCCESS -> {
                dismissProgress()
                when (response.response?.code) {
                    200 -> {
                        val sites = response.response?.data?.sites ?: emptyList()
                        viewModel.singleSites = sites
                    }

                    else -> {
                        handleErrorResponse(
                            response.response?.code ?: 0,
                            response.response?.message
                        )
                    }
                }
            }

            Status.ERROR -> {
                dismissProgress()
                handleError(response.throwable)
            }
        }
    }


    private fun handleAllProjectsResponse(response: ApiState<AllProjectsResponse>) {
        when (response.status) {
            Status.LOADING -> showProgress()
            Status.SUCCESS -> {
                dismissProgress()
                when (response.response?.code) {
                    200 -> {
                        val projects = response.response?.data?.projects ?: emptyList()
                        viewModel.projects = projects
                        showAllProjectsSelectionDialog(projects)
                    }

                    else -> {
                        handleErrorResponse(
                            response.response?.code ?: 0,
                            response.response?.message
                        )
                    }
                }
            }

            Status.ERROR -> {
                dismissProgress()
                handleError(response.throwable)
            }
        }
    }

    private fun handleSiteByProjectResponse(response: ApiState<SiteListByProjectResponse>) {
        when (response.status) {
            Status.LOADING -> showProgress()
            Status.SUCCESS -> {
                dismissProgress()
                when (response.response?.code) {
                    200 -> {
                        val sites = response.response?.data ?: emptyList()
                        viewModel.multiSites = sites
                    }

                    else -> {
                        handleErrorResponse(
                            response.response?.code ?: 0,
                            response.response?.message
                        )
                    }
                }
            }

            Status.ERROR -> {
                dismissProgress()
                handleError(response.throwable)
            }
        }
    }

    private fun handleCreateClaimResponse(response: ApiState<CreateClaimResponse>) {
        when (response.status) {
            Status.LOADING -> showProgress()
            Status.SUCCESS -> {
                dismissProgress()
                when (response.response?.code) {
                    ValConstants.SUCCESS_CREATION_CODE -> {
                        setResult(RESULT_OK)
                        alertDialogShow(
                            this,
                            getString(R.string.success),
                            response.response?.message
                                ?: getString(R.string.claim_submitted_successfully),
                            okLister = DialogInterface.OnClickListener { _, _ ->
                                finish()
                            }
                        )

                    }

                    else -> {
                        handleErrorResponse(
                            response.response?.code ?: 0,
                            response.response?.message
                        )
                    }
                }
            }

            Status.ERROR -> {
                dismissProgress()
                handleError(response.throwable)
            }
        }
    }

    private fun errorHandler(event: CreateClaimErrorHandler) {
        when (event) {
            CreateClaimErrorHandler.EMPTY_DATE -> {
                binding.dateEt.error = getString(R.string.please_select_date)
                showToast(this, getString(R.string.please_select_date))
            }

            CreateClaimErrorHandler.EMPTY_SELECTED_SITE -> {
                binding.siteEt.error = getString(R.string.please_select_site)
                showToast(this, getString(R.string.please_select_site))
            }

            CreateClaimErrorHandler.EMPTY_PROJECT -> {
                binding.projectSelectEt.error = getString(R.string.please_select_project)
                showToast(this, getString(R.string.please_select_project))
            }

            CreateClaimErrorHandler.EMPTY_CIRCLE -> {
                binding.circleEt.error = getString(R.string.please_select_circle)
                showToast(this, getString(R.string.please_select_circle))
            }

            CreateClaimErrorHandler.EMPTY_EXPENSES -> showToast(
                this,
                getString(R.string.please_add_at_least_one_expense_entry)
            )

            CreateClaimErrorHandler.EMPTY_PURPOSE -> {
                binding.purposeEt.error = getString(R.string.please_select_purpose)
                showToast(this, getString(R.string.please_select_purpose))
            }

            CreateClaimErrorHandler.EMPTY_MUTLI_SITES -> showToast(
                this,
                getString(R.string.please_select_at_least_one_site)
            )
        }
    }

    private fun handleClickEvents(event: CreateClaimClickEvents) {
        when (event) {
            CreateClaimClickEvents.ON_DATE_PICKER_CLICK -> {
                showDatePicker()
            }

            CreateClaimClickEvents.ON_SINGLE_SITE_CLICK -> {
                viewModel.isMultiSite.set(false)
                viewModel.clearSelectedMultiSites()
                binding.projectSelectEt.setText("")
                binding.circleEt.setText("")
            }

            CreateClaimClickEvents.ON_MULTI_SITE_CLICK -> {
                viewModel.isMultiSite.set(true)
                viewModel.clearSelectedSingleSite()
                binding.siteEt.setText("")
                binding.projectEt.setText("")
                binding.circleEt.setText("")
            }

            CreateClaimClickEvents.ON_LOCAL_CLAIM_CLICK -> {
                viewModel.isOutstationExpense.set(false)
                viewModel.clearTravelingEntries()
                viewModel.clearHotelEntries()
            }

            CreateClaimClickEvents.ON_OUTSTATION_CLAIM_CLICK -> {
                viewModel.isOutstationExpense.set(true)
                viewModel.clearTravelingEntries()
            }

            CreateClaimClickEvents.ON_ADD_MULTIPLE_SITE_CLICK -> {
                showSitesSelectionDialog(viewModel.multiSites)
            }

            CreateClaimClickEvents.ON_SINGLE_SITE_DROPDOWN_CLICK -> {
                if (viewModel.singleSites.isEmpty()) {
                    alertDialogShow(
                        this,
                        getString(R.string.alert),
                        getString(R.string.no_sites_available),
                    )
                } else {
                    showSingleSiteSelectionDialog(viewModel.singleSites)
                }
            }

            CreateClaimClickEvents.ON_ADD_TRAVELING_EXPENSE_CLICK -> {
                val lastEntry = viewModel.travelingEntriesList.value?.lastOrNull()
                addTravelingDetailsActivityResultLauncher.launch(
                    Intent(this, AddTravelingDetailActivity::class.java).apply {
                        putExtra(
                            SharingKeys.IS_OUTSTATION_CLAIM,
                            viewModel.isOutstationExpense.get()
                        )
                        putExtra(SharingKeys.EXTRA_CIRCLE_CODE, viewModel.selectedCircleCode)
                        putExtra(SharingKeys.EXTRA_DEFAULT_FROM, lastEntry?.to)
                    }
                )
            }

            CreateClaimClickEvents.ON_ADD_DAILY_ALLOWANCE_CLICK -> {
                showEnterDaBottomSheet()
            }

            CreateClaimClickEvents.ON_ADD_HOTEL_EXPENSE_CLICK -> {
                showEnterHotelBottomSheet()
            }

            CreateClaimClickEvents.ON_ADD_OTHERS_EXPENSE_CLICK -> {
                showEnterOthersBottomSheet()
            }

            CreateClaimClickEvents.ON_CANCEL_CLICK -> {
                finish()
            }

            CreateClaimClickEvents.ON_PURPOSE_CLICK -> {
                showPurposeSelectionDialog(AppListData.purposes)
            }

            CreateClaimClickEvents.ON_PROJECT_DROPDOWN_CLICK -> {
                if (viewModel.projects.isEmpty()) {
                    viewModel.getAllProjects()
                } else {
                    showAllProjectsSelectionDialog(viewModel.projects)
                }
            }

            CreateClaimClickEvents.ON_CIRCLE_DROPDOWN_CLICK -> {
                showProjectCirclesSelectionDialog(viewModel.circles)
            }

            else -> { /* ...existing code... */
            }
        }
    }

    private fun handleErrorResponse(code: Int, message: String?) {
        if (code == 401) tokenExpiresAlert() else alertDialogShow(
            this,
            getString(R.string.alert),
            message ?: getString(R.string.something_went_wrong)
        )
    }

    private fun handleError(throwable: Throwable?) {
        if (throwable is HttpException) {
            if (throwable.code() == 401) {
                tokenExpiresAlert()
            } else {
                showToast(this, throwable.message())
            }
        } else {
            showToast(this, throwable?.message ?: getString(R.string.something_went_wrong))
        }
    }

    private val addSiteDetailsActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val siteID = data?.getLongExtra(SharingKeys.SITE_ID, -1) ?: -1
            val purpose = data?.getStringExtra(SharingKeys.SITE_PURPOSE) ?: ""
            //viewModel.addMultipleSite(MultipleSite(siteID, purpose))
        }
    }

    private val addTravelingDetailsActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data: Intent? = result.data
            val travelExpenseData: TravelExpense? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    data?.getParcelableExtra(
                        SharingKeys.TRAVELING_DETAILS,
                        TravelExpense::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    data?.getParcelableExtra(SharingKeys.TRAVELING_DETAILS)
                }
            if (travelExpenseData != null)
                viewModel.addTravelExpense(travelExpenseData)
        }
    }

    private fun showEnterDaBottomSheet() {
        var dialog = EnterDaBottomSheet(getString(R.string.add_da_entery)) { amount, path ->
            var daExpense = DAExpense(
                selectedDaEntriesAdapter?.itemCount?.plus(1).toString(),
                amount = amount,
                receiptAttachments = if (path.isNullOrEmpty()) emptyList() else listOf(path)
            )
            viewModel.addDAExpense(daExpense)
        }
        dialog.show(supportFragmentManager, "EnterDaBottomSheet")
    }

    private fun showEnterHotelBottomSheet() {
        var dialog = EnterDaBottomSheet(getString(R.string.add_hotel_entry)) { amount, path ->
            var hotelExpense = HotelExpense(
                selectedHotelEntriesAdapter?.itemCount?.plus(1).toString(),
                amount = amount,
                receiptAttachments = if (path.isNullOrEmpty()) emptyList() else listOf(path)
            )
            viewModel.addHotelExpense(hotelExpense)
        }
        dialog.show(supportFragmentManager, "EnterHotelBottomSheet")
    }

    private fun showEnterOthersBottomSheet() {
        var dialog = EnterOthersBottomSheet { category, amount, path ->
            var othersExpense = OtherExpense(
                selectedOthersEntriesAdapter?.itemCount?.plus(1).toString(),
                category = category,
                amount = amount,
                receiptAttachments = if (path.isNullOrEmpty()) emptyList() else listOf(path)
            )
            viewModel.addOtherExpense(othersExpense)
        }
        dialog.show(supportFragmentManager, "EnterOtherBottomSheet")
    }

    private fun showPurposeSelectionDialog(purposeList: List<String>) {
        showSelectionDialog(
            items = purposeList,
            title = getString(R.string.select_purpose),
            layoutResId = R.layout.item_generic_adapter,
            bind = { view, purpose ->
                view.findViewById<TextView>(R.id.text1).text = purpose
            },
            onItemSelected = {
                binding.purposeEt.error = null
                viewModel.purpose.set(it)
            },
            filterCondition = { purpose, query ->
                purpose.lowercase(Locale.getDefault())
                    .contains(query.lowercase(Locale.getDefault()))
            },
            emptyMessage = getString(R.string.no_data_available),
            retryAction = { },
            tag = "PurposeSelectionDialog"
        )
    }

    private fun showSingleSiteSelectionDialog(siteList: List<Site>) {
        showSelectionDialog(
            items = siteList,
            title = getString(R.string.select_site),
            layoutResId = R.layout.item_generic_adapter,
            bind = { view, site ->
                view.findViewById<TextView>(R.id.text1).text = site.siteName
            },
            onItemSelected = {
                binding.siteEt.error = null
                viewModel.onSingleSiteSelected(it)
                binding.siteEt.setText(it.siteName)
                binding.projectEt.setText(it.projectName)
                binding.circleEt.setText(it.circleName)

            },
            filterCondition = { site, query ->
                site.siteName?.lowercase(Locale.getDefault())
                    ?.contains(query.lowercase(Locale.getDefault()))
                    ?: false
            },
            emptyMessage = getString(R.string.no_data_available),
            retryAction = { },
            tag = "SiteSelectionDialog"
        )
    }

    private fun showSitesSelectionDialog(sites: List<SiteData>) {
        if (sites.isEmpty()) {
            alertDialogShow(
                this,
                getString(R.string.alert),
                getString(R.string.no_types_available),
                getString(R.string.retry),
                okLister = DialogInterface.OnClickListener { _, _ ->
                    viewModel.getSiteListByProject(viewModel.selectedProjectId ?: 0L)
                },
            )
        } else {
            val preSelectedSites = viewModel.selectedSitesIdList?.value?.mapNotNull { site ->
                sites.find { it == site }
            }?.toSet() ?: emptySet()

            val dialog = MultiSelectBottomSheetDialog(
                context = this,
                items = sites,
                preSelectedItems = preSelectedSites,
                bind = { view, site, isSelected ->
                    view.findViewById<TextView>(R.id.textView).text = site.name
                    view.findViewById<CheckBox>(R.id.checkBox).isChecked = isSelected
                },
                onSelectionChanged = { selectedSites ->
                    updateSelectedSites(selectedSites)
                },
                onSubmit = { selectedSites ->
                    updateSelectedSites(selectedSites)
                },
                filterCondition = { site, query ->
                    site?.name?.lowercase(Locale.getDefault())
                        ?.contains(query.lowercase(Locale.getDefault())) ?: false
                },
                title = getString(R.string.select_site)
            )
            dialog.show(supportFragmentManager, "SiteSelectionDialog")
        }
    }

    private fun showAllProjectsSelectionDialog(projectList: List<Project>) {
        showSelectionDialog(
            items = projectList,
            title = getString(R.string.select_project),
            layoutResId = R.layout.item_generic_adapter,
            bind = { view, project ->
                view.findViewById<TextView>(R.id.text1).text = project.name
            },
            onItemSelected = {
                binding.projectSelectEt.error = null
                binding.projectSelectEt.setText(it.name)
                viewModel.onProjectSelected(it)
                viewModel.getSiteListByProject(it.id ?: 0L)
            },
            filterCondition = { project, query ->
                project.name?.lowercase(Locale.getDefault())
                    ?.contains(query.lowercase(Locale.getDefault()))
                    ?: false
            },
            emptyMessage = getString(R.string.no_data_available),
            retryAction = { },
            tag = "ProjectSelectionDialog"
        )
    }

    private fun showProjectCirclesSelectionDialog(circleList: List<Circle>) {
        showSelectionDialog(
            items = circleList,
            title = getString(R.string.select_circle),
            layoutResId = R.layout.item_generic_adapter,
            bind = { view, circle ->
                view.findViewById<TextView>(R.id.text1).text = circle.name
            },
            onItemSelected = {
                binding.circleEt.error = null
                binding.circleEt.setText(it.name)
                viewModel.onCircleSelected(it)
            },
            filterCondition = { circle, query ->
                circle.name?.lowercase(Locale.getDefault())
                    ?.contains(query.lowercase(Locale.getDefault()))
                    ?: false
            },
            emptyMessage = getString(R.string.no_data_available),
            retryAction = { },
            tag = "CircleSelectionDialog"
        )
    }

    private fun updateSelectedSites(selectedSites: Set<SiteData>) {
        viewModel.addMultipleSite(selectedSites.toList())
    }
}
