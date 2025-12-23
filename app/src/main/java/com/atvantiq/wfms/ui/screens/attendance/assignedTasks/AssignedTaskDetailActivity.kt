package com.atvantiq.wfms.ui.screens.attendance.assignedTasks

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ActivityAssignedTaskDetailBinding
import com.atvantiq.wfms.models.work.assignedAll.WorkRecord
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.WorkTypeAdapter
import com.atvantiq.wfms.ui.screens.attendance.AttendanceViewModel
import com.atvantiq.wfms.ui.screens.attendance.signInDetails.endWork.EndWorkBottomSheet
import com.atvantiq.wfms.utils.DateUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException

@AndroidEntryPoint
class AssignedTaskDetailActivity :
    BaseActivity<ActivityAssignedTaskDetailBinding, AttendanceViewModel>() {

    private var itemPosition: Int = -1
    private var workId: Long? = null
    private var itemTypeAdapter: WorkTypeAdapter? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(
            R.layout.activity_assigned_task_detail,
            AttendanceViewModel::class.java
        )

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setToolbar()
        initListeners()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupWokTypeRecyclerView()
        setupSelectAllCheckbox()
        //fetchIntentData()
    }

    private fun setToolbar() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.tvBack.text = getString(R.string.details)
    }

    private fun initListeners() {
        binding.btnAccept.setOnClickListener {

        }
        binding.btnStartWork.setOnClickListener {

        }
        binding.btnEndWork.setOnClickListener {
            endWorkWithLocationPermissions(workId ?: -1, itemPosition)
        }
    }

    private fun fetchIntentData() {
        workId = intent.getLongExtra(SharingKeys.WORK_ID, -1)
        if(workId != null) {
            itemPosition = intent.getIntExtra(SharingKeys.WORK_POSITION, -1)
            viewModel.itemPosition.value = itemPosition
            viewModel.workById(workId!!)
        }
    }

    private fun setupWokTypeRecyclerView() {
        itemTypeAdapter = WorkTypeAdapter { allSelected ->
            binding.cbSelectAllWorkTypes.setOnCheckedChangeListener(null)
            binding.cbSelectAllWorkTypes.isChecked = allSelected
            binding.cbSelectAllWorkTypes.setOnCheckedChangeListener { _, isChecked ->
                itemTypeAdapter?.setAllSelected(isChecked)
            }
        }
        binding.rvWorkTypes.apply {
            layoutManager = LinearLayoutManager(this@AssignedTaskDetailActivity)
            adapter = itemTypeAdapter
        }
    }

    private fun setupSelectAllCheckbox() {
        binding.cbSelectAllWorkTypes.setOnCheckedChangeListener { _, isChecked ->
            itemTypeAdapter?.setAllSelected(isChecked)
        }
    }

    private fun setupUI(record: WorkRecord?) {
      /*  binding.tvProjectName.text = getString(R.string.project) + ": " + record?.project?.name
            ?: getString(R.string.not_available)
        binding.status = record?.status ?: ValConstants.OPEN
        binding.tvCircle.text = getString(R.string.circle) + ": " + record?.circle?.name
            ?: getString(R.string.not_available)
        binding.tvSite.text = record?.site?.name ?: getString(R.string.not_available)
        val firstType = record?.type?.firstOrNull()
        val typeName = firstType?.name ?: "N/A"
        val activityName = firstType?.activity?.firstOrNull()?.name ?: "N/A"
        binding.tvType.text = getString(R.string.type) + ": $typeName"
        binding.tvActivity.text = "$activityName"
        binding.tvDateTime.text = DateUtils.formatApiDateToTimeAndDate(record?.updatedAt)*/
    }

    override fun subscribeToEvents(vm: AttendanceViewModel) {

       /* vm.workByIdResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    dismissProgress()
                    if (response.response?.code == 200) {
                        showToast(this, response.response.message ?: getString(R.string.details))
                        setupUI(response.response.data)
                    } else if (response.response?.code == 401) {
                        tokenExpiresAlert()
                    } else {
                        alertDialogShow(
                            this,
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
                            this,
                            response.throwable.message ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.LOADING -> {
                    showProgress()
                }
            }
        }*/
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
        intent.data = android.net.Uri.fromParts("package", this.packageName, null)
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

    private fun getRequiredPermissions(): Array<String> {
        return buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }.toTypedArray()
    }

    private fun hasAllPermissions(permissions: Array<String>): Boolean =
        permissions.all {
            ContextCompat.checkSelfPermission(
               this,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun showPermissionRationale() {
        MaterialAlertDialogBuilder(this)
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
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_denied)
            .setMessage(R.string.permission_denied_permanently)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                openApplicationSettings()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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
                        }.show(this.supportFragmentManager, "END_WORK_BOTTOM_SHEET_TAG")
                    } else {
                        showToast(this, getString(R.string.location_not_found))
                    }
                }.addOnFailureListener {
                    showToast(this, getString(R.string.location_error))
                }
            }
        )
    }

}

