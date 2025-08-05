package com.atvantiq.wfms.ui.screens.attendance.signInDetails.endWork

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
import com.atvantiq.wfms.databinding.BottomSheetEndWorkBinding
import com.atvantiq.wfms.databinding.BottomSheetStartWorkBinding
import com.atvantiq.wfms.models.StatusOption
import com.atvantiq.wfms.ui.screens.adapters.StatusAdapter
import com.atvantiq.wfms.utils.files.PickMediaHelper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EndWorkBottomSheet(
    var latitude: String,
    var longitude: String,
    var onSubmitDetails: (statusId: Int, remarkds: String) -> Unit
) : BottomSheetDialogFragment() {

    lateinit var binding: BottomSheetEndWorkBinding
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
            DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_end_work, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setLocationLatLon()
        initListeners()
        initStatusList()
    }
    private fun initListeners() {
        binding.btnDone.setOnClickListener {
            if(selectedStatus == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.select_status),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            } else {
                selectedStatus?.code?.let { it1 -> onSubmitDetails(it1, binding.remarksEditText.text.toString()) }
                dismiss()
            }
        }
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun setLocationLatLon() {
        binding.locationString = "$latitude $longitude"
    }

    private fun initStatusList() {
        val statusOptions = listOf(
            StatusOption(4, "ACCESS ISSUE"),
            StatusOption(5, "COMPLETED"),
            StatusOption(6, "REVISIT"),
            StatusOption(7, "REJECTED")
        )
        binding.statusRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.statusRecyclerView.adapter = StatusAdapter(statusOptions) { selected ->
            selectedStatus = selected
        }
    }

}