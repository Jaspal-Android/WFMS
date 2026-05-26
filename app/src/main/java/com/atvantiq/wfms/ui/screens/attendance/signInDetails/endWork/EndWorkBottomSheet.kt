package com.atvantiq.wfms.ui.screens.attendance.signInDetails.endWork

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.BottomSheetEndWorkBinding
import com.atvantiq.wfms.models.StatusOption
import com.atvantiq.wfms.models.inventory.UsedMaterial
import com.atvantiq.wfms.ui.screens.adapters.StatusAdapter
import com.atvantiq.wfms.ui.screens.intentory.MaterialUsageActivity
import com.atvantiq.wfms.widgets.DividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class EndWorkBottomSheet : BottomSheetDialogFragment() {

    private var latitude: String? = null
    private var longitude: String? = null
    private var workId: Long = 0L
    private var projectId: Long = 0L

    var onSubmitDetails: ((statusId: Int, remarks: String) -> Unit)? = null
    var onMaterialFlowCompleted: ((statusId: Int, remarks: String, usedMaterials: List<UsedMaterial>) -> Unit)? = null

    lateinit var binding: BottomSheetEndWorkBinding
    private var selectedStatus: StatusOption? = null

    private lateinit var materialLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
        arguments?.let {
            latitude = it.getString(ARG_LATITUDE)
            longitude = it.getString(ARG_LONGITUDE)
            workId = it.getLong(ARG_WORK_ID)
            projectId = it.getLong(ARG_PROJECT_ID)
        }
        materialLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == MaterialUsageActivity.RESULT_SUBMITTED) {
                val statusId = selectedStatus?.code ?: return@registerForActivityResult
                val usedMaterials = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableArrayListExtra(SharingKeys.USED_MATERIALS, UsedMaterial::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableArrayListExtra<UsedMaterial>(SharingKeys.USED_MATERIALS)
                } ?: emptyList()
                onMaterialFlowCompleted?.invoke(statusId, binding.remarksEditText.text.toString(), usedMaterials)
                dismiss()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_sheet_end_work, container, false)
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
            val status = selectedStatus
            if (status == null) {
                Toast.makeText(requireContext(), getString(R.string.select_status), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val remarks = binding.remarksEditText.text.toString()

            // Status code 5 == COMPLETED, 6 == PENDING (existing mapping in initStatusList)
            if (status.code == STATUS_COMPLETED) {
                launchMaterialUsage(status.code, remarks)
            } else {
                onSubmitDetails?.invoke(status.code, remarks)
                dismiss()
            }
        }
        binding.btnCancel.setOnClickListener { dismiss() }
    }

    private fun launchMaterialUsage(statusId: Int, remarks: String) {
        val intent = MaterialUsageActivity.newIntent(
            ctx       = requireContext(),
            workId    = workId,
            projectId = projectId,
            statusId  = statusId,
            remarks   = remarks,
            latitude  = latitude ?: "",
            longitude = longitude ?: ""
        )
        materialLauncher.launch(intent)
    }

    private fun setLocationLatLon() {
        binding.locationString = "$latitude, $longitude"
    }

    private fun initStatusList() {
        val statusOptions = listOf(
            StatusOption(STATUS_COMPLETED, getString(R.string.completed)),
            StatusOption(STATUS_PENDING,   getString(R.string.pending))
        )
        binding.statusRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.statusRecyclerView.setHasFixedSize(true)
        binding.statusRecyclerView.addItemDecoration(
            DividerItemDecoration(requireContext(), R.drawable.custom_divider)
        )
        binding.statusRecyclerView.adapter = StatusAdapter(statusOptions) { selected ->
            selectedStatus = selected
        }
    }

    companion object {
        private const val ARG_LATITUDE = "latitude"
        private const val ARG_LONGITUDE = "longitude"
        private const val ARG_WORK_ID = "workId"
        private const val ARG_PROJECT_ID = "projectId"
        const val STATUS_COMPLETED = 5
        const val STATUS_PENDING   = 6

        fun newInstance(latitude: String, longitude: String, workId: Long, projectId: Long): EndWorkBottomSheet {
            val fragment = EndWorkBottomSheet()
            val args = Bundle()
            args.putString(ARG_LATITUDE, latitude)
            args.putString(ARG_LONGITUDE, longitude)
            args.putLong(ARG_WORK_ID, workId)
            args.putLong(ARG_PROJECT_ID, projectId)
            fragment.arguments = args
            return fragment
        }
    }
}