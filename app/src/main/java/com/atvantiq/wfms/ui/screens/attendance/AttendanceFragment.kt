package com.atvantiq.wfms.ui.screens.attendance

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
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
import com.atvantiq.wfms.ui.screens.attendance.myProgress.MyProgressActivity
import com.atvantiq.wfms.ui.screens.attendance.signInDetails.SignInDetailActivity
import com.atvantiq.wfms.ui.screens.attendance.signInDetails.endWork.EndWorkBottomSheet
import com.atvantiq.wfms.ui.screens.attendance.signInDetails.startWork.StartWorkBottomSheet
import com.atvantiq.wfms.utils.Utils
import com.atvantiq.wfms.widgets.DividerItemDecoration
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException

@AndroidEntryPoint
class AttendanceFragment : BaseFragment<FragmentAttendanceBinding, AttendanceViewModel>() {

    private var adapter: AssignedTasksListAdapter? = null
    private var page: Int = 1
    private var pageSize: Int = 10
    private var isLoading = false
    private var isLastPage = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val searchHandler = Handler(Looper.getMainLooper())
    private val searchDebounce = Runnable { resetAndFetch() }

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
        page = 1
        isLastPage = false
        adapter?.submitList(emptyList())
        getWorkAssignedAll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchHandler.removeCallbacks(searchDebounce)
        binding.rvAssignedTasks.adapter = null // Avoid memory leaks
    }

    override fun subscribeToEvents(vm: AttendanceViewModel) {
        binding.vm = vm

        vm.clickEvents.observe(viewLifecycleOwner) { event ->
            if (!isLifeCycleResumed()) return@observe
            handleClickEvents(event)

        }
        vm.workAssignedAllResponse.observe(viewLifecycleOwner) { response ->
            if (!isLifeCycleStarted()) return@observe
            handleWorkAssignedResponse(response)
        }

        vm.workAcceptResponse.observe(viewLifecycleOwner) { response ->
            if (!isLifeCycleResumed()) return@observe
            handleAcceptWorkResponse(response, R.string.work_accepted, ValConstants.ACCEPTED)
        }

        vm.workStartResponse.observe(viewLifecycleOwner) { response ->
            if (!isLifeCycleResumed()) return@observe
            handleStartWorkResponse(response, R.string.work_started, ValConstants.WIP)
        }

        vm.workEndResponse.observe(viewLifecycleOwner) { response ->
            if (!isLifeCycleResumed()) return@observe
            handleWorkEndResponse(response)
        }

        vm.attendanceCheckInStatusResponse.observe(viewLifecycleOwner) { response ->
            if (!isLifeCycleResumed()) return@observe
            handleAttendanceCheckInResponse(response)
        }
    }

    private fun handleClickEvents(event: AttendanceClickEvents) {
        when (event) {
            AttendanceClickEvents.ON_SIGN_IN_CLICK -> {
                val intent = Intent(requireContext(), AddSignInActivity::class.java)
                assignTaskLauncher.launch(intent)
            }
            AttendanceClickEvents.ON_MY_PROGRESS_CLICK -> {
                Utils.jumpActivity(requireContext(), MyProgressActivity::class.java)
            }
            AttendanceClickEvents.ON_SIGN_IN_DETAILS_CLICK -> {
                Utils.jumpActivity(requireContext(), SignInDetailActivity::class.java)
            }
        }
    }

    private fun handleWorkAssignedResponse(response: ApiState<WorkAssignedResponse>) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                stopRefreshingData()
                response.response?.let {
                    if (it.code == 200) {
                        handleWorkAssignedSuccess(it.data.results)
                    } else {
                        handleErrorResponse(it.code, it.message)
                    }
                }
            }
            Status.ERROR -> handleError(response.throwable)
            Status.LOADING -> showLoadingIndicator()
        }
    }

    private fun handleWorkAssignedSuccess(records: List<Site>) {
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

    private fun handleAcceptWorkResponse(
        response: ApiState<WorkDetailResponse>,
        successMessage: Int,
        status: String
    ) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                response.response?.let {
                    if (it.code == 200) {
                        showToast(requireContext(), it.message ?: getString(successMessage))
                        //adapter?.setUpdateStatus(viewModel.itemPosition.value ?: -1, status)
                        viewModel.itemPosition.value = -1
                    } else {
                        //handleErrorResponse(it.code, it.message)
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
                    if (it.code == 200) {
                        showToast(requireContext(), it.message ?: getString(successMessage))
                        //adapter?.setUpdateStatus(viewModel.itemPosition.value ?: -1, status)
                        viewModel.itemPosition.value = -1
                    } else {
                        it.code?.let { it1 -> handleErrorResponse(it1, it.message) }
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
                    if (it.code == 200) {
                        showToast(requireContext(), it.message ?: getString(R.string.work_ended))
                        /*adapter?.setUpdateStatus(
                            viewModel.itemPosition.value ?: -1,
                            it.data?.status ?: ValConstants.COMPLETED
                        )*/
                        viewModel.itemPosition.value = -1
                    } else {
                        it.code?.let { it1 -> handleErrorResponse(it1, it.message) }
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
                    if (it.code == 200 && it.data?.checkedIn == true) {
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
        val navController = requireActivity().findNavController(R.id.nav_host_fragment_content_dashboard)
        navController.navigate(R.id.nav_dashboard)
    }


    private fun handleErrorResponse(code: Int, message: String?) {
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

    private fun checkAttendanceStatus(id: Long, position: Int) {
        viewModel.currentWorkId = id
        viewModel.itemPosition.value = position // Set the current item position
        viewModel.checkInStatusAttendance()
    }

    private fun getWorkAssignedAll() {
        if (isLoading || isLastPage) return
        isLoading = true
        if (page != 1) adapter?.addLoadingFooter()
        viewModel.getWorkAssignedAll(page, pageSize)
    }

    private fun resetAndFetch() {
        page = 1
        isLastPage = false
        isLoading = false
        adapter?.removeLoadingFooter()
        adapter?.submitList(emptyList())
        getWorkAssignedAll()
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString() ?: ""
                viewModel.searchQuery = query
                binding.ivClearSearch.visibility = if (query.isNotBlank()) View.VISIBLE else View.GONE
                searchHandler.removeCallbacks(searchDebounce)
                searchHandler.postDelayed(searchDebounce, 500L)
            }
        })
        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.setText("")
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
                resetAndFetch()
            }
        }
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
        binding.rvAssignedTasks.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
                            getWorkAssignedAll()
                        }
                    }
                }
            }
        })

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

    private fun mainLayout() {
        isLoading = false
        adapter?.removeLoadingFooter() // Hide loading footer
        binding.isEmptyAssignedTasks = false
    }

    private fun emptyDataLayout() {
        isLoading = false
        adapter?.removeLoadingFooter() // Hide loading footer
        if (adapter?.count() ?: 0 <= 0) {
            binding.isEmptyAssignedTasks = true
        }
    }

    private fun swipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            startRefreshingData()
        }
    }

    private fun startRefreshingData() {
        resetAndFetch()
    }

    private fun stopRefreshingData() {
        if (binding.swipeRefreshLayout.isRefreshing) {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.all { it.value } -> {

            }

            !permissions.any { shouldShowRequestPermissionRationale(it.key) } -> {
                showPermissionDeniedPermanently()
            }

            else -> {
                showPermissionRationale()
            }
        }
    }

    private fun openApplicationSettings() {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = android.net.Uri.fromParts("package", requireContext().packageName, null)
        startActivity(intent)
    }

    private fun handleLocationPermissions(
        onPermissionsGranted: () -> Unit,
        onPermissionsDenied: () -> Unit = { showPermissionRationale() },
        onPermissionsDeniedPermanently: () -> Unit = { showPermissionDeniedPermanently() }
    ) {
        val permissions = getRequiredPermissions()
        when {
            hasAllPermissions(permissions) -> onPermissionsGranted()
            permissions.any { shouldShowRequestPermissionRationale(it) } -> onPermissionsDenied()
            else -> permissionLauncher.launch(permissions)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startWorkWithLocationPermissions(workId: Long, position: Int) {
        handleLocationPermissions(
            onPermissionsGranted = {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val latitude = location.latitude.toString()
                        val longitude = location.longitude.toString()
                        StartWorkBottomSheet.newInstance(latitude, longitude).apply {
                            onImageSelected = { imagePath ->
                                viewModel.workStart(workId.toString(), latitude, longitude, imagePath, position)
                            }
                        }.show(requireActivity().supportFragmentManager, "START_WORK_BOTTOM_SHEET_TAG")
                    } else {
                        showToast(requireContext(), getString(R.string.location_not_found))
                    }
                }.addOnFailureListener {
                    showToast(requireContext(), getString(R.string.location_error))
                }
            }
        )
    }

    private fun getRequiredPermissions(): Array<String> {
        return buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }.toTypedArray()
    }

    private fun hasAllPermissions(permissions: Array<String>): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(
                requireContext(),
                it
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun showPermissionRationale() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.permission_required)
            .setMessage(R.string.location_permission_rationale)
            .setPositiveButton(R.string.retry) { _, _ ->
                // Launch permission request after showing rationale
                permissionLauncher.launch(getRequiredPermissions())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPermissionDeniedPermanently() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.permission_denied)
            .setMessage(R.string.permission_denied_permanently)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                openApplicationSettings()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private val assignTaskLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startRefreshingData()
            }
        }

    private val assignedTaskDetailLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val position = result.data?.getIntExtra(SharingKeys.WORK_POSITION, -1) ?: -1
                val updatedStatus = result.data?.getIntExtra(SharingKeys.UPDATED_STATUS,-1) ?: -1
                if (position != -1 && updatedStatus != null) {
                    adapter?.setUpdateStatus(position, updatedStatus)
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

}
