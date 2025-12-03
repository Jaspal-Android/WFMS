package com.atvantiq.wfms.ui.screens.dashboard

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.BottomSheetAttendanceRemarksBinding
import com.atvantiq.wfms.databinding.BottomSheetEndWorkBinding
import com.atvantiq.wfms.databinding.BottomSheetStartWorkBinding
import com.atvantiq.wfms.models.StatusOption
import com.atvantiq.wfms.ui.screens.adapters.StatusAdapter
import com.atvantiq.wfms.utils.files.PickMediaHelper
import com.atvantiq.wfms.widgets.DividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AttendanceRemarksBottomSheet(var onSubmitDetails: (remarkds: String) -> Unit
) : BottomSheetDialogFragment() {

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
            onSubmitDetails(binding.remarksEditText.text.toString())
            dismiss()
        }
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }
}