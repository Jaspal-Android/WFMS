package com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance.detail

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.ActivityWorkDetailsByDateBinding
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.AssignedTasksListAdapter
import com.atvantiq.wfms.ui.screens.attendance.AttendanceViewModel
import com.atvantiq.wfms.ui.screens.attendance.assignedTasks.AssignedTaskDetailActivity
import com.atvantiq.wfms.utils.Utils
import retrofit2.HttpException

class WorkDetailsByDateActivity : BaseActivity<ActivityWorkDetailsByDateBinding, AttendanceViewModel>() {

    private var adapter: AssignedTasksListAdapter? = null


    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(
            R.layout.activity_work_details_by_date,
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
        setWorkList()
        fetchWorkDetailsByDate()
    }

    private fun setToolbar() {
        binding.workDetailsByDateToolbar.toolbarTitle.text = getString(R.string.details)
        binding.workDetailsByDateToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun fetchWorkDetailsByDate() {
        val date = intent.getStringExtra(SharingKeys.workDate) ?: return
        viewModel.workDetailsByDate(date)
    }

    private fun setWorkList(){
        adapter = AssignedTasksListAdapter(true,
            onViewAssignedTask = { assignedTask, position ->
                Utils.jumpActivityWithData(
                    this,
                    AssignedTaskDetailActivity::class.java,
                    Bundle().apply {
                        putInt(SharingKeys.WORK_POSITION, position)
                        putLong(SharingKeys.WORK_ID, assignedTask.id)
                    }
                )
            },
            onAcceptTask = { assignedTask, position ->
                // Handle accept task click
            },
            onStartWork = { assignedTask, position ->
                // Handle start work click
            },
            onEndWork = { assignedTask, position ->
                // Handle end work click
            }
        )
        binding.workList.adapter = adapter
    }

    override fun subscribeToEvents(vm: AttendanceViewModel) {
        vm.workDetailsByDateResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    dismissProgress()
                    if (response.response?.code == 200) {
                        if (response.response.data.isEmpty()) {
                            binding.isNoDataAvailable = true
                            showToast(
                                this,
                                getString(R.string.no_work_details_found)
                            )
                        } else {
                            binding.isNoDataAvailable = false
                            adapter?.submitList(response.response.data)
                        }
                    } else if (response.response?.code == 401) {
                        tokenExpiresAlert()
                        binding.isNoDataAvailable = true
                    } else {
                        binding.isNoDataAvailable = true
                        alertDialogShow(
                            this,
                            getString(R.string.alert),
                            response.response?.message
                                ?: getString(R.string.something_went_wrong)
                        )
                    }
                }

                Status.ERROR -> {
                    dismissProgress()
                    binding.isNoDataAvailable = true
                    val throwable = response.throwable
                    if (throwable is HttpException) {
                        if (throwable.code() == 401) {
                            tokenExpiresAlert()
                        }
                    } else {
                        showToast(
                            this,
                            response.throwable?.message ?: getString(R.string.something_went_wrong)
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