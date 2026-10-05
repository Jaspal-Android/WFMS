package com.atvantiq.wfms.ui.screens.attendance

import com.atvantiq.wfms.utils.permissions.LocationPermissionDelegate
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.base.PagedListUiState
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.FragmentAttendanceBinding
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.models.work.workAssigned.Site
import com.atvantiq.wfms.models.work.workAssigned.WorkAssignedResponse
import com.atvantiq.wfms.models.work.workDetail.WorkDetailResponse
import com.atvantiq.wfms.network.ApiState
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.AssignedTasksListAdapter
import com.atvantiq.wfms.ui.screens.attendance.addSignInActivity.AddSignInActivity
import com.atvantiq.wfms.ui.screens.attendance.assignedTasks.AssignedTaskDetailActivity
import com.atvantiq.wfms.ui.screens.attendance.signInDetails.startWork.StartWorkBottomSheet
import com.atvantiq.wfms.utils.navigateToTab
import com.atvantiq.wfms.widgets.DividerItemDecoration
import com.atvantiq.wfms.widgets.PaginationScrollListener
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AttendanceFragment : BaseFragment<FragmentAttendanceBinding, AttendanceViewModel>() {

    private var adapter: AssignedTasksListAdapter? = null
    private var isProgressShown = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val searchHandler = Handler(Looper.getMainLooper())
    private val searchDebounce = Runnable { viewModel.workList.reload() }

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_attendance, AttendanceViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpWorkAssignmentList()
        setupSearch()
        setupFilterChips()
        swipeRefresh()
        // Shows the work already loaded (if any) and refreshes it in the background.
        viewModel.workList.open()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchHandler.removeCallbacks(searchDebounce)
        binding.rvAssignedTasks.adapter = null // Avoid memory leaks
    }

    override fun subscribeToEvents(vm: AttendanceViewModel) {
        binding.vm = vm

        vm.clickEvents.observe(viewLifecycleOwner) { event ->
            handleClickEvents(event)
        }
        vm.workList.state.observe(viewLifecycleOwner) { state -> renderWorkList(state) }

        vm.workList.failure.observe(viewLifecycleOwner) { failure ->
            if (!failure.consumeOnce()) return@observe
            handleWorkListFailure(failure)
        }

        vm.workAcceptResponse.observe(viewLifecycleOwner) { response ->
            if (!response.consumeOnce()) return@observe
            handleAcceptWorkResponse(response, R.string.work_accepted, ValConstants.ACCEPTED)
        }

        vm.workStartResponse.observe(viewLifecycleOwner) { response ->
            if (!response.consumeOnce()) return@observe
            handleStartWorkResponse(response, R.string.work_started, ValConstants.WIP)
        }

        vm.workEndResponse.observe(viewLifecycleOwner) { response ->
            if (!response.consumeOnce()) return@observe
            handleWorkEndResponse(response)
        }

        vm.attendanceCheckInStatusResponse.observe(viewLifecycleOwner) { response ->
            if (!response.consumeOnce()) return@observe
            handleAttendanceCheckInResponse(response)
        }
    }

    private fun handleClickEvents(event: AttendanceClickEvents) {
        when (event) {
            AttendanceClickEvents.ON_SIGN_IN_CLICK -> {
                val intent = Intent(requireContext(), AddSignInActivity::class.java)
                assignTaskLauncher.launch(intent)
            }
        }
    }

    private fun renderWorkList(state: PagedListUiState<Site>) {
        showFirstPageProgress(state.isLoadingFirstPage)
        if (!state.isLoadingFirstPage && !state.isRefreshing) stopRefreshingData()
        adapter?.submitList(state.items, state.changedPosition)
        adapter?.showLoadingFooter(state.isLoadingMore)
        binding.isEmptyAssignedTasks = state.isEmpty
    }

    private fun showFirstPageProgress(show: Boolean) {
        if (show == isProgressShown) return
        isProgressShown = show
        if (show) showProgress() else dismissProgress()
    }

    private fun handleWorkListFailure(failure: ApiState<WorkAssignedResponse>) {
        if (failure.status == Status.SUCCESS) {
            // The server rejected the page.
            failure.response?.let { handleRejectedResponse(it.code, it.message) }
        } else {
            handleApiFailure(failure.throwable)
        }
    }

    private fun handleAcceptWorkResponse(
        response: ApiState<WorkDetailResponse>,
        successMessage: Int,
        status: String
    ) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                response.response?.let {
                    if (it.code == ValConstants.SUCCESS_CODE) {
                        showToast(requireContext(), it.message ?: getString(successMessage))
                        //adapter?.setUpdateStatus(viewModel.itemPosition.value ?: -1, status)
                        viewModel.itemPosition.value = -1
                    } else {
                        //handleRejectedResponse(it.code, it.message)
                    }
                }
            }
            Status.ERROR -> handleError(response.throwable)
            Status.LOADING -> showProgress()
        }
    }

    private fun handleStartWorkResponse(
        response: ApiState<WorkDetailResponse>,
        successMessage: Int,
        status: String
    ) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                response.response?.let {
                    if (it.code == ValConstants.SUCCESS_CODE) {
                        showToast(requireContext(), it.message ?: getString(successMessage))
                        //adapter?.setUpdateStatus(viewModel.itemPosition.value ?: -1, status)
                        viewModel.itemPosition.value = -1
                    } else {
                        handleRejectedResponse(it.code, it.message)
                    }
                }
            }
            Status.ERROR -> handleError(response.throwable)
            Status.LOADING -> showProgress()
        }
    }

    private fun handleWorkEndResponse(response: ApiState<WorkDetailResponse>) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                response.response?.let {
                    if (it.code == ValConstants.SUCCESS_CODE) {
                        showToast(requireContext(), it.message ?: getString(R.string.work_ended))
                        /*adapter?.setUpdateStatus(
                            viewModel.itemPosition.value ?: -1,
                            it.data?.status ?: ValConstants.COMPLETED
                        )*/
                        viewModel.itemPosition.value = -1
                    } else {
                        handleRejectedResponse(it.code, it.message)
                    }
                }
            }
            Status.ERROR -> handleError(response.throwable)
            Status.LOADING -> showProgress()
        }
    }

    private fun handleAttendanceCheckInResponse(response: ApiState<CheckInStatusResponse>) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                response.response?.let {
                    if (it.code == ValConstants.SUCCESS_CODE && it.data?.checkedIn == true) {
                        startWorkWithLocationPermissions(viewModel.currentWorkId ?: -1, viewModel.itemPosition.value ?: -1)
                    } else {
                        alertDialogShow(requireContext(), getString(R.string.alert), getString(R.string.please_check_in_first), okLister = DialogInterface.OnClickListener { dialog, _ ->
                            dialog.dismiss()
                            navigateToDashboard()
                        })
                    }
                }
            }
            Status.ERROR -> handleError(response.throwable)
            Status.LOADING -> showProgress()
        }
    }
    private fun navigateToDashboard() {
        findNavController().navigateToTab(R.id.nav_dashboard)
    }


    private fun handleError(throwable: Throwable?) {
        dismissProgress()
        handleApiFailure(throwable)
    }

    private fun checkAttendanceStatus(id: Long, position: Int) {
        viewModel.currentWorkId = id
        viewModel.itemPosition.value = position // Set the current item position
        viewModel.checkInStatusAttendance()
    }

    private fun setupSearch() {
        // A recreated view shows the kept list, so show the query it was filtered by.
        binding.searchBar.etSearch.setText(viewModel.searchQuery)
        binding.searchBar.ivClearSearch.visibility = if (viewModel.searchQuery.isNotBlank()) View.VISIBLE else View.GONE
        binding.searchBar.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                binding.searchBar.ivClearSearch.visibility = if (query.isNotBlank()) View.VISIBLE else View.GONE
                // The text the view restores after recreation is the query already applied; only a
                // real change reloads (a reload clears the kept list).
                if (query == viewModel.searchQuery) return
                viewModel.searchQuery = query
                searchHandler.removeCallbacks(searchDebounce)
                searchHandler.postDelayed(searchDebounce, SEARCH_DEBOUNCE_MS)
            }
        })
        binding.searchBar.ivClearSearch.setOnClickListener {
            binding.searchBar.etSearch.setText("")
        }
    }

    private fun setupFilterChips() {
        val chips = mapOf(
            binding.chipAll to WorkFilter.ALL,
            binding.chipPending to WorkFilter.PENDING,
            binding.chipActive to WorkFilter.ACTIVE,
            binding.chipCompleted to WorkFilter.COMPLETED
        )
        chips.forEach { (chip, filter) ->
            chip.setOnClickListener {
                if (viewModel.activeFilter == filter) return@setOnClickListener
                viewModel.activeFilter = filter
                updateChipStyles(chips, filter)
                viewModel.workList.reload()
            }
        }
        updateChipStyles(chips, viewModel.activeFilter)
    }

    private fun updateChipStyles(chips: Map<TextView, WorkFilter>, selected: WorkFilter) {
        val primaryColor = com.google.android.material.color.MaterialColors.getColor(
            requireView(), com.atvantiq.wfms.R.attr.wfmsColorPrimary
        )
        val onSurfaceVariantColor = com.google.android.material.color.MaterialColors.getColor(
            requireView(), com.atvantiq.wfms.R.attr.wfmsColorOnSurfaceVariant
        )
        chips.forEach { (chip, filter) ->
            if (filter == selected) {
                chip.setBackgroundResource(R.drawable.bg_filter_chip_selected)
                chip.setTextColor(resources.getColor(android.R.color.white, requireContext().theme))
            } else {
                chip.setBackgroundResource(R.drawable.bg_filter_chip_unselected)
                chip.setTextColor(onSurfaceVariantColor)
            }
        }
    }

    private fun setUpWorkAssignmentList() {
        binding.rvAssignedTasks.addOnScrollListener(PaginationScrollListener { viewModel.workList.loadNextPage() })

        adapter = AssignedTasksListAdapter(false,
            onViewAssignedTask = { assignedTask, position ->
                launchAssignedTaskDetail(position, assignedTask)
            },
            /*onAcceptTask = { assignedTask, position ->
                viewModel.workAccept(assignedTask.id, position)
            },
            onStartWork = { assignedTask, position ->
                checkAttendanceStatus(assignedTask.id, position)
               *//* val intent = Intent(requireContext(), LocationTrackingService::class.java)
                intent.action = "com.atvantiq.wfms.ACTION_START_WORK"
                intent.putExtra("WORK_ID", "0002")
                ContextCompat.startForegroundService(requireContext(), intent)*//*
            },
            onEndWork = { assignedTask, position ->
                endWorkWithLocationPermissions(assignedTask.id, position)
            }*/
        )
        binding.rvAssignedTasks.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                R.drawable.custom_divider
            )
        )
        binding.rvAssignedTasks.adapter = adapter
    }

    private fun swipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.workList.refresh()
        }
    }

    private fun stopRefreshingData() {
        if (binding.swipeRefreshLayout.isRefreshing) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    /** Start Work needs location: asked for (with the disclosure) and run once it is granted. */
    private val locationPermission = LocationPermissionDelegate(
        this, { requireContext() }, ::shouldShowRequestPermissionRationale,
        LocationPermissionDelegate.Config(
            LocationPermissionDelegate.Disclosure(R.string.location_permission_needed, R.string.start_end_work_location_permission_msg),
            retry = LocationPermissionDelegate.Retry.REQUEST_AGAIN
        )
    )

    @SuppressLint("MissingPermission")
    private fun startWorkWithLocationPermissions(workId: Long, position: Int) {
        locationPermission.request(
            action = {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    whenAttached { context ->
                        if (location != null) {
                            val latitude = location.latitude.toString()
                            val longitude = location.longitude.toString()
                            StartWorkBottomSheet.newInstance(latitude, longitude).apply {
                                onImageSelected = { imagePath ->
                                    viewModel.workStart(workId.toString(), latitude, longitude, imagePath, position)
                                }
                            }.show(parentFragmentManager, "START_WORK_BOTTOM_SHEET_TAG")
                        } else {
                            showToast(context, getString(R.string.location_not_found))
                        }
                    }
                }.addOnFailureListener {
                    whenAttached { showToast(it, getString(R.string.location_error)) }
                }
            }
        )
    }

    private val assignTaskLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.workList.refresh()
            }
        }

    private val assignedTaskDetailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val position = result.data?.getIntExtra(SharingKeys.WORK_POSITION, -1) ?: -1
                val updatedStatus = result.data?.getIntExtra(SharingKeys.UPDATED_STATUS,-1) ?: -1
                if (position != -1 && updatedStatus != -1) {
                    viewModel.workList.updateItem(position) { it.status?.code = updatedStatus }
                }
            }

            if(result.resultCode == ValConstants.RESULT_MARK_ATTENDANCE){
                navigateToDashboard()
            }
        }

    private fun launchAssignedTaskDetail(position: Int, assignedTask: Site) {
        val intent = Intent(requireContext(), AssignedTaskDetailActivity::class.java).apply {
            putExtra(SharingKeys.WORK_POSITION, position)
            putExtra(SharingKeys.WORK_ID, assignedTask.workSiteId)
        }
        assignedTaskDetailLauncher.launch(intent)
    }

    private companion object {
        /** Wait for typing to pause before searching. */
        const val SEARCH_DEBOUNCE_MS = 500L
    }
}
