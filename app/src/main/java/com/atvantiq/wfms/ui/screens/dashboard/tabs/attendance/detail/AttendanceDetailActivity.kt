package com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance.detail

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.ActivityAttendanceDetailBinding
import com.atvantiq.wfms.models.attendance.attendanceDetails.Record
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.AssignedTasksListAdapter
import com.atvantiq.wfms.ui.screens.attendance.AttendanceViewModel
import com.atvantiq.wfms.ui.screens.attendance.assignedTasks.AssignedTaskDetailActivity
import com.atvantiq.wfms.ui.screens.dashboard.tabs.attendance.AttendanceStatusVM
import com.atvantiq.wfms.utils.DateUtils
import com.atvantiq.wfms.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.HttpException


@AndroidEntryPoint
class AttendanceDetailActivity : BaseActivity<ActivityAttendanceDetailBinding, AttendanceViewModel>() {

    private var adapter: AssignedTasksListAdapter? = null

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_attendance_detail, AttendanceViewModel::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setToolbar()
        setWorkList()
        fetchAttendanceDetailFromBundle()
    }

    private fun fetchAttendanceDetailFromBundle() {
        val record: Record? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(SharingKeys.attendanceRecord, Record::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(SharingKeys.attendanceRecord) as? Record
        }
        setupUI(record)
    }

    private fun setupUI(record: Record?) {
        binding.item = record

        record?.checkin?.let { checkin ->
            if (checkin.latitude != null && checkin.logitude != null) {
                Utils.getAddressFromLatLong(this, checkin.latitude, checkin.logitude) {
                    binding.checkInAddressString = it
                }
            } else {
                binding.checkInAddressString = getString(R.string.address_not_found)
            }
        } ?: run {
            binding.checkInAddressString = getString(R.string.address_not_found)
        }

        record?.checkout?.let { checkout ->
            if (checkout.latitude != null && checkout.logitude != null) {
                Utils.getAddressFromLatLong(this, checkout.latitude, checkout.logitude) {
                    binding.checkOutAddressString = it
                }
            } else {
                binding.checkOutAddressString = getString(R.string.address_not_found)
            }
        } ?: run {
            binding.checkOutAddressString = getString(R.string.address_not_found)
        }

        val workDate = DateUtils.formatApiDateToYMD(record?.checkin?.time.orEmpty())
        workDate?.let { viewModel.workDetailsByDate(it) }
    }

    private fun setToolbar() = binding.attendanceDetailToolbar.apply {
        toolbarTitle.text = getString(R.string.details)
        toolbarBackButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setWorkList() {
        adapter = AssignedTasksListAdapter(
            true,
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
            onAcceptTask = { _, _ -> /* Handle accept task click if needed */ },
            onStartWork = { _, _ -> /* Handle start work click if needed */ },
            onEndWork = { _, _ -> /* Handle end work click if needed */ }
        )
        binding.workList.apply {
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            adapter = this@AttendanceDetailActivity.adapter
        }
    }

    override fun subscribeToEvents(vm: AttendanceViewModel) {
        vm.workDetailsByDateResponse.observe(this) { response ->
            when (response.status) {
                Status.SUCCESS -> {
                    dismissProgress()
                    val resp = response.response
                    when (resp?.code) {
                        200 -> {
                            if (resp.data.isEmpty()) {
                                binding.isNoDataAvailable = true
                                showToast(this, getString(R.string.no_work_details_found))
                            } else {
                                binding.isNoDataAvailable = false
                                adapter?.submitList(resp.data)
                            }
                        }
                        401 -> {
                            tokenExpiresAlert()
                            binding.isNoDataAvailable = true
                        }
                        else -> {
                            binding.isNoDataAvailable = true
                            alertDialogShow(
                                this,
                                getString(R.string.alert),
                                resp?.message ?: getString(R.string.something_went_wrong)
                            )
                        }
                    }
                }
                Status.ERROR -> {
                    dismissProgress()
                    binding.isNoDataAvailable = true
                    (response.throwable as? HttpException)?.let {
                        if (it.code() == 401) tokenExpiresAlert()
                    } ?: showToast(
                        this,
                        response.throwable?.message ?: getString(R.string.something_went_wrong)
                    )
                }
                Status.LOADING -> showProgress()
            }
        }
    }
}
