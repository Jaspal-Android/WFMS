package com.atvantiq.wfms.ui.screens.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.BottomSheetAttendanceRemarksBinding
import com.atvantiq.wfms.models.StatusOption
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AttendanceRemarksBottomSheet : BottomSheetDialogFragment() {

    // Callback is wired by the host after construction. Keeping a no-arg constructor
    // lets the FragmentManager re-instantiate this sheet on process-death/config-change
    // restore without an InstantiationException (mirrors EndWorkBottomSheet).
    var onSubmitDetails: ((remarks: String) -> Unit)? = null

    lateinit var binding: BottomSheetAttendanceRemarksBinding
    private var selectedStatus: StatusOption? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_attendance_remarks, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initListeners()
    }

    private fun initListeners() {
        binding.btnDone.setOnClickListener {
            onSubmitDetails?.invoke(binding.remarksEditText.text.toString())
            dismiss()
        }
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        fun newInstance(): AttendanceRemarksBottomSheet = AttendanceRemarksBottomSheet()
    }
}
