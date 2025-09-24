package com.atvantiq.wfms.ui.screens.attendance

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.FragmentAttendanceBinding
import com.atvantiq.wfms.models.attendance.checkInStatus.CheckInStatusResponse
import com.atvantiq.wfms.models.work.acceptWork.AcceptWorkResponse
import com.atvantiq.wfms.models.work.assignedAll.WorkAssignedAllResponse
import com.atvantiq.wfms.models.work.assignedAll.WorkRecord
import com.atvantiq.wfms.models.work.endWork.EndWorkResponse
import com.atvantiq.wfms.models.work.startWork.StartWorkResponse
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

    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_attendance, AttendanceViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpWorkAssignmentList()
        swipeRefresh()
        page = 1
        isLastPage = false
        adapter?.submitList(emptyList()) // Clear adapter data
        getWorkAssignedAll() // Fetch data only on first creation
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvAssignedTasks.adapter = null // Avoid memory leaks
        viewModel.stopTracking() // Stop tracking when the fragment is destroyed
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

    private fun handleWorkAssignedResponse(response: ApiState<WorkAssignedAllResponse>) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                stopRefreshingData()
                response.response?.let {
                    if (it.code == 200) {
                        handleWorkAssignedSuccess(it.data.records)
                    } else {
                        handleErrorResponse(it.code, it.message)
                    }
                }
            }
            Status.ERROR -> handleError(response.throwable)
            Status.LOADING -> showLoadingIndicator()
        }
    }

    private fun handleWorkAssignedSuccess(records: List<WorkRecord>) {
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
        response: ApiState<AcceptWorkResponse>,
        successMessage: Int,
        status: String
    ) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                response.response?.let {
                    if (it.code == 200) {
                        showToast(requireContext(), it.message ?: getString(successMessage))
                        adapter?.setUpdateStatus(viewModel.itemPosition.value ?: -1, status)
                        viewModel.itemPosition.value = -1
                    } else {
                        handleErrorResponse(it.code, it.message)
                    }
                }
            }
            Status.ERROR -> handleError(response.throwable)
            Status.LOADING -> showProgress()
        }
    }

    private fun handleStartWorkResponse(
        response: ApiState<StartWorkResponse>,
        successMessage: Int,
        status: String
    ) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                response.response?.let {
                    if (it.code == 200) {
                        showToast(requireContext(), it.message ?: getString(successMessage))
                        adapter?.setUpdateStatus(viewModel.itemPosition.value ?: -1, status)
                        viewModel.itemPosition.value = -1
                    } else {
                        handleErrorResponse(it.code, it.message)
                    }
                }
            }
            Status.ERROR -> handleError(response.throwable)
            Status.LOADING -> showProgress()
        }
    }

    private fun handleWorkEndResponse(response: ApiState<EndWorkResponse>) {
        when (response.status) {
            Status.SUCCESS -> {
                dismissProgress()
                response.response?.let {
                    if (it.code == 200) {
                        showToast(requireContext(), it.message ?: getString(R.string.work_ended))
                        adapter?.setUpdateStatus(
                            viewModel.itemPosition.value ?: -1,
                            it.data?.status ?: ValConstants.COMPLETED
                        )
                        viewModel.itemPosition.value = -1
                    } else {
                        handleErrorResponse(it.code, it.message)
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
        if (isLoading || isLastPage)
            return
        isLoading = true
        if (page != 1) {
            adapter?.addLoadingFooter() // Show loading footer only for next pages
        }
        viewModel.getWorkAssignedAll(page, pageSize)
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
                Utils.jumpActivityWithData(
                    requireContext(),
                    AssignedTaskDetailActivity::class.java,
                    Bundle().apply {
                        putInt(SharingKeys.WORK_POSITION, position)
                        putLong(SharingKeys.WORK_ID, assignedTask.id)
                    }
                )
            },
            onAcceptTask = { assignedTask, position ->
                viewModel.workAccept(assignedTask.id, position)
            },
            onStartWork = { assignedTask, position ->
                checkAttendanceStatus(assignedTask.id, position)
            },
            onEndWork = { assignedTask, position ->
                endWorkWithLocationPermissions(assignedTask.id, position)
            }
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
        page = 1
        isLastPage = false // Reset last page flag
        adapter?.removeLoadingFooter() // Remove loading footer on refresh
        adapter?.submitList(emptyList()) // Clear adapter data on refresh
        viewModel.getWorkAssignedAll(page, pageSize)
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
                        StartWorkBottomSheet(latitude, longitude) { imagePath ->
                            viewModel.workStart(workId.toString(), latitude, longitude, imagePath, position)
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

    @SuppressLint("MissingPermission")
    private fun endWorkWithLocationPermissions(workId: Long, position: Int) {
        handleLocationPermissions(
            onPermissionsGranted = {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val latitude = location.latitude.toString()
                        val longitude = location.longitude.toString()
                        EndWorkBottomSheet(latitude, longitude) { statusId, remarks ->
                            viewModel.workEnd(workId, latitude.toDouble(), longitude.toDouble(), statusId, remarks, position)
                        }.show(requireActivity().supportFragmentManager, "END_WORK_BOTTOM_SHEET_TAG")
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

}
