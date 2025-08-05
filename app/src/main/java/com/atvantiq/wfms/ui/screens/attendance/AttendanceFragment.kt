package com.atvantiq.wfms.ui.screens.attendance

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseFragment
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.FragmentAttendanceBinding
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

    private val TAG = "AttendanceFragment"
    private var adapter: AssignedTasksListAdapter? = null
    private var page: Int = 1
    private var pageSize: Int = 10
    private var isLoading = false
    private var isLastPage = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    override val fragmentBinding: FragmentBinding
        get() = FragmentBinding(R.layout.fragment_attendance, AttendanceViewModel::class.java)

    override fun onCreateViewFragment(savedInstanceState: Bundle?) {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpWorkAssignmentList()
        //getWorkAssignedAll() // Ensure this is called here
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    override fun subscribeToEvents(vm: AttendanceViewModel) {
        binding.vm = vm
        binding.lifecycleOwner = this

        vm.clickEvents.observe(viewLifecycleOwner) { event ->
            when (event) {
                AttendanceClickEvents.ON_SIGN_IN_CLICK -> {
                    Utils.jumpActivity(requireContext(), AddSignInActivity::class.java)
                    //checkPermissionsAndStart()
                }

                AttendanceClickEvents.ON_MY_PROGRESS_CLICK -> {
                    Utils.jumpActivity(requireContext(), MyProgressActivity::class.java)
                    //viewModel.stopTracking()
                }

                AttendanceClickEvents.ON_SIGN_IN_DETAILS_CLICK -> {
                    Utils.jumpActivity(requireContext(), SignInDetailActivity::class.java)
                }
            }
        }

        vm.workAssignedAllResponse.observe(viewLifecycleOwner) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    dismissProgress()
                    stopRefreshingData()
                    if (response.response?.code == 200) {
                        if (response.response.data.records.isEmpty()) {
                            isLastPage = true
                            if (page == 1) {
                                emptyDataLayout()
                            } else {
                                adapter?.removeLoadingFooter() // Hide loading footer
                                isLoading = false
                            }
                        } else {
                            mainLayout()
                            if (page == 1) {
                                adapter?.submitList(response.response.data.records)
                                page = 2
                            } else {
                                adapter?.addData(response.response.data.records)
                                page += 1
                            }
                        }
                    } else if (response.response?.code == 401) {
                        tokenExpiresAlert()
                    } else {
                        alertDialogShow(
                            requireContext(),
                            getString(R.string.alert),
                            response.response?.message
                                ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.ERROR -> {
                    dismissProgress()
                    stopRefreshingData()
                    adapter?.removeLoadingFooter() // Hide loading footer
                    isLoading = false
                    if (page == 1) {
                        emptyDataLayout()
                    }
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == 401) {
                            tokenExpiresAlert()
                        }
                    } else {
                        showToast(
                            requireContext(),
                            response.throwable?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    if (page == 1) {
                        showProgress()
                    } else {
                        adapter?.removeLoadingFooter() // Show loading footer
                    }
                }
            }


        }

        vm.workAcceptResponse.observe(viewLifecycleOwner) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    dismissProgress()
                    if (response.response?.code == 200) {
                        showToast(
                            requireContext(),
                            response.response.message ?: getString(R.string.work_accepted)
                        )
                        // Optionally refresh the list or update UI
                        adapter?.setUpdateStatus(vm.itemPosition.value ?: -1, ValConstants.ACCEPTED)
                        viewModel.itemPosition.value = -1 // Reset item position after handling
                    } else if (response.response?.code == 401) {
                        tokenExpiresAlert()
                    } else {
                        alertDialogShow(
                            requireContext(),
                            getString(R.string.alert),
                            response.response?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.ERROR -> {
                    dismissProgress()
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == 401) {
                            tokenExpiresAlert()
                        }
                    } else {
                        showToast(
                            requireContext(),
                            response.throwable?.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    showProgress()
                }
            }
        }

         vm.workStartResponse.observe(viewLifecycleOwner) { response ->
             when (response.status) {
                 Status.SUCCESS -> {
                     dismissProgress()
                     if (response.response?.code == 200) {
                         showToast(requireContext(), response.response.message ?: getString(R.string.work_started))
                         // Optionally refresh the list or update UI
                         adapter?.setUpdateStatus(vm.itemPosition.value ?: -1, ValConstants.WIP)
                         viewModel.itemPosition.value = -1 // Reset item position after handling
                     } else if (response.response?.code == 401) {
                         tokenExpiresAlert()
                     } else {
                         alertDialogShow(
                             requireContext(),
                             getString(R.string.alert),
                             response.response?.message ?: getString(R.string.something_went_wrong)
                         )
                     }
                 }

                 Status.ERROR -> {
                     dismissProgress()
                     if ((response.throwable as HttpException).code() == 401) {
                         tokenExpiresAlert()
                     } else {
                         showToast(
                             requireContext(),
                             response.throwable.message ?: getString(R.string.something_went_wrong)
                         )
                     }
                 }

                 Status.LOADING -> {
                     showProgress()
                 }
             }
         }


         vm.workEndResponse.observe(viewLifecycleOwner) { response ->
             when (response.status) {
                 Status.SUCCESS -> {
                     dismissProgress()
                     if (response.response?.code == 200) {
                         showToast(requireContext(), response.response.message ?: getString(R.string.work_ended))
                         adapter?.setUpdateStatus(vm.itemPosition.value ?: -1, response.response?.data?.status ?: ValConstants.COMPLETED)
                         viewModel.itemPosition.value = -1 // Reset item position after handling
                     } else if (response.response?.code == 401) {
                         tokenExpiresAlert()
                     } else {
                         alertDialogShow(
                             requireContext(),
                             getString(R.string.alert),
                             response.response?.message ?: getString(R.string.something_went_wrong)
                         )
                     }
                 }

                 Status.ERROR -> {
                     dismissProgress()
                     if ((response.throwable as HttpException).code() == 401) {
                         tokenExpiresAlert()
                     } else {
                         showToast(
                             requireContext(),
                             response.throwable.message ?: getString(R.string.something_went_wrong)
                         )
                     }
                 }

                 Status.LOADING -> {
                     showProgress()
                 }
             }
         }

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
                            getWorkAssignedAll()
                        }
                    }
                }
            }
        })

        adapter = AssignedTasksListAdapter(
            onViewAssignedTask = { assignedTask, position ->
                Utils.jumpActivityWithData(
                    requireContext(),
                    AssignedTaskDetailActivity::class.java,
                    Bundle().apply {
                        putInt(SharingKeys.WORK_POSITION, position)
                        putInt(SharingKeys.WORK_ID, assignedTask.id)
                    }
                )
            },
            onAcceptTask = { assignedTask, position ->
                viewModel.workAccept(assignedTask.id, position)
            },
            onStartWork = { assignedTask, position ->
                startWorkWithLocationPermissions(assignedTask.id, position)
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
        swipeRefresh()
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

    @SuppressLint("MissingPermission")
    private fun startWorkWithLocationPermissions(workId: Int, position: Int) {
        val permissions = getRequiredPermissions()
        when {
            hasAllPermissions(permissions) -> {
                //find the location and start work
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    if(it != null) {
                        val latitude = it.latitude.toString()
                        val longitude = it.longitude.toString()
                        var startWorkBottomSheet = StartWorkBottomSheet(latitude, longitude) { imagePath ->
                            viewModel.workStart(workId.toString(), latitude, longitude, imagePath, position)
                        }
                        startWorkBottomSheet.show(requireActivity().supportFragmentManager, "StartWorkBottomSheet")
                    } else {
                        showToast(requireContext(), getString(R.string.location_not_found))
                    }
                }.addOnFailureListener { exception ->
                    showToast(requireContext(), getString(R.string.location_error))
                }
            }

            permissions.any { shouldShowRequestPermissionRationale(it) } -> {
                showPermissionRationale()
            }

            else -> {
                permissionLauncher.launch(permissions)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun endWorkWithLocationPermissions(workId: Int, position: Int) {
        val permissions = getRequiredPermissions()
        when {
            hasAllPermissions(permissions) -> {
                //find the location and start work
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    if(it != null) {
                        val latitude = it.latitude
                        val longitude = it.longitude
                        var endWorkBottomSheet = EndWorkBottomSheet(latitude.toString(), longitude.toString()) { statusId, remarks ->
                            viewModel.workEnd(workId, latitude, longitude, statusId, remarks, position)
                        }
                        endWorkBottomSheet.show(requireActivity().supportFragmentManager, "EndWorkBottomSheet")
                    } else {
                        showToast(requireContext(), getString(R.string.location_not_found))
                    }
                }.addOnFailureListener { exception ->
                    showToast(requireContext(), getString(R.string.location_error))
                }
            }

            permissions.any { shouldShowRequestPermissionRationale(it) } -> {
                showPermissionRationale()
            }

            else -> {
                permissionLauncher.launch(permissions)
            }
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        return buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                 add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
             }
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                 add(Manifest.permission.POST_NOTIFICATIONS)
             }*/
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

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop tracking when the fragment is destroyed
        viewModel.stopTracking()
    }

}