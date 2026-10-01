package com.atvantiq.wfms.ui.screens.admin.ui.attendanceApproval

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.fragment.app.activityViewModels
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.BottomSheetAttendanceDecisionBinding
import com.atvantiq.wfms.network.Status
import com.atvantiq.wfms.ui.screens.adapters.AttendanceDecisionStatusAdapter
import com.atvantiq.wfms.widgets.BaseBottomSheet
import dagger.hilt.android.AndroidEntryPoint

/**
 * Attendance Decision (spec 6.2), opened from a row's "Mark attendance". The decision lives in the
 * list's ViewModel, so the sheet survives being recreated; it can't be closed while submitting. The
 * host shows the result and closes it.
 */
@AndroidEntryPoint
class AttendanceDecisionBottomSheet : BaseBottomSheet() {

    private val viewModel: AttendanceApprovalVM by activityViewModels()
    private lateinit var binding: BottomSheetAttendanceDecisionBinding
    private var statusAdapter: AttendanceDecisionStatusAdapter? = null

    private val statusChanged = object : Observable.OnPropertyChangedCallback() {
        override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
            statusAdapter?.select(viewModel.decisionStatus.get())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_attendance_decision, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.vm = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (viewModel.decisionRecord == null) {
            dismissAllowingStateLoss()
            return
        }
        statusAdapter = AttendanceDecisionStatusAdapter(onSelect = viewModel::selectStatus).also {
            binding.form.statusGrid.adapter = it
            it.select(viewModel.decisionStatus.get())
        }
        viewModel.decisionStatus.addOnPropertyChangedCallback(statusChanged)
        binding.btnClose.setOnClickListener { if (!viewModel.isSubmitting) dismiss() }

        viewModel.decisionError.observe(viewLifecycleOwner) { error ->
            error ?: return@observe
            viewModel.decisionError.value = null
            showToast(requireContext(), getString(error))
        }
        // The host shows the result and closes the sheet; here only the sending state matters.
        viewModel.approveResponse.observe(viewLifecycleOwner) { response ->
            val sending = response.status == Status.LOADING
            isCancelable = !sending
            binding.btnSubmit.isEnabled = !sending
        }
    }

    override fun onDestroyView() {
        viewModel.decisionStatus.removeOnPropertyChangedCallback(statusChanged)
        statusAdapter = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "AttendanceDecisionBottomSheet"
    }
}
