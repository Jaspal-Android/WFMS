package com.atvantiq.wfms.ui.screens.attendance.assignedTasks

import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.constants.ValConstants
import com.atvantiq.wfms.databinding.ActivityAssignedTaskDetailBinding
import com.atvantiq.wfms.models.work.assignedAll.WorkRecord
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.attendance.AttendanceViewModel
import com.atvantiq.wfms.utils.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException

@AndroidEntryPoint
class AssignedTaskDetailActivity :
    BaseActivity<ActivityAssignedTaskDetailBinding, AttendanceViewModel>() {

    private var itemPosition: Int = -1
    private var workId: Long? = null

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
        fetchIntentData()
    }

    private fun setToolbar() {
        binding.assignedTaskDetailToolbar.toolbarTitle.text = getString(R.string.details)
        binding.assignedTaskDetailToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
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

    private fun setupUI(record: WorkRecord?) {
        binding.tvProjectName.text = getString(R.string.project) + ": " + record?.project?.name
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
        binding.tvDateTime.text = DateUtils.formatApiDateToTimeAndDate(record?.updatedAt)
    }

    override fun subscribeToEvents(vm: AttendanceViewModel) {

        vm.workByIdResponse.observe(this) { response ->
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
        }
    }

}