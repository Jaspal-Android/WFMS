package com.atvantiq.wfms.ui.screens.admin.ui.attendanceApproval

import android.view.View
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.isVisible
import androidx.databinding.Observable
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.ActivityAttendanceReviewBinding
import com.atvantiq.wfms.models.attendance.attendanceDetails.AttendanceRecord
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.AttendanceDecisionStatusAdapter
import dagger.hilt.android.AndroidEntryPoint
import com.atvantiq.wfms.utils.applySystemBarsAndImePadding

/**
 * Attendance Review (spec 6.3), opened from a row's Details: the record read-only, then the same
 * decision form as the sheet. A decision is handed back to the list, which updates the row.
 */
@AndroidEntryPoint
class AttendanceReviewActivity : BaseActivity<ActivityAttendanceReviewBinding, AttendanceApprovalVM>() {

    private var statusAdapter: AttendanceDecisionStatusAdapter? = null

    private val statusChanged = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            statusAdapter?.select(viewModel.decisionStatus.get())
        }
    }

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_attendance_review, AttendanceApprovalVM::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        findViewById<View>(R.id.main).applySystemBarsAndImePadding()
        binding.toolbar.toolbarTitle.text = getString(R.string.attendance_review)
        binding.toolbar.toolbarBackButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val record = viewModel.decisionRecord ?: recordFromIntent()
        if (record == null) {
            showToast(this, getString(R.string.attendance_id_missing))
            finish()
            return
        }
        if (viewModel.decisionRecord == null) viewModel.startDecision(record)
        statusAdapter = AttendanceDecisionStatusAdapter(onSelect = viewModel::selectStatus).also {
            binding.form.statusGrid.adapter = it
            it.select(viewModel.decisionStatus.get())
        }
        viewModel.decisionStatus.addOnPropertyChangedCallback(statusChanged)
        render(record)
    }

    /** Read-only record; the form only while the record still waits for this role. */
    private fun render(record: AttendanceRecord) {
        val canMark = viewModel.canMark(record)
        with(binding.recordCard) {
            this.record = record
            dateLabel = record.dayLabel
            timeRange = record.checkInOutLabel(this@AttendanceReviewActivity)
            this.canMark = false
            showDetails = false
            executePendingBindings()
        }
        binding.canMark = canMark
        binding.form.root.isVisible = canMark
        binding.notWaiting.root.isVisible = !canMark
    }

    override fun subscribeToEvents(vm: AttendanceApprovalVM) {
        binding.vm = vm
        vm.decisionError.observe(this) { error ->
            error ?: return@observe
            vm.decisionError.value = null
            showToast(this, getString(error))
        }
        vm.approveResponse.observe(this) { response ->
            if (!response.consumeOnce()) return@observe
            when (response.status) {
                Status.LOADING -> {
                    binding.btnSubmit.isEnabled = false
                    showProgress()
                }
                Status.SUCCESS -> {
                    dismissProgress()
                    binding.btnSubmit.isEnabled = true
                    val answer = response.response
                    if (answer?.success == true) {
                        showToast(this, answer.message ?: getString(R.string.attendance_decision_submitted))
                        returnDecision()
                    } else {
                        handleRejectedResponse(answer?.code, answer?.message)
                    }
                }
                Status.ERROR -> {
                    dismissProgress()
                    binding.btnSubmit.isEnabled = true
                    handleApiFailure(response.throwable)
                }
            }
        }
    }

    private fun returnDecision() {
        val record = viewModel.decisionRecord ?: return finish()
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(SharingKeys.ATTENDANCE_ID, record.id)
            putExtra(SharingKeys.ATTENDANCE_STATUS, record.status)
        })
        finish()
    }

    private fun recordFromIntent(): AttendanceRecord? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(SharingKeys.attendanceRecord, AttendanceRecord::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(SharingKeys.attendanceRecord) as? AttendanceRecord
        }

    override fun onDestroy() {
        viewModel.decisionStatus.removeOnPropertyChangedCallback(statusChanged)
        statusAdapter = null
        super.onDestroy()
    }
}
