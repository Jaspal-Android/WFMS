package com.atvantiq.wfms.ui.screens.intentory

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.atvantiq.wfms.R
import com.atvantiq.wfms.base.BaseActivity
import com.atvantiq.wfms.constants.SharingKeys
import com.atvantiq.wfms.databinding.ActivityMaterialUsageBinding
import com.atvantiq.wfms.models.inventory.UsedMaterial
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MaterialUsageActivity : BaseActivity<ActivityMaterialUsageBinding, MaterialUsageViewModel>() {

    private lateinit var adapter: MaterialAdapter
    private var searchDebounceJob: Job? = null
    private var lastVisibleRows: List<MaterialRow>? = null
    private val qtyFormatter = DecimalFormat("0.00")

    companion object {
        const val RESULT_SUBMITTED = RESULT_FIRST_USER + 1

        fun newIntent(
            ctx: Context,
            workId: Long,
            projectId: Long,
            statusId: Int,
            remarks: String,
            latitude: String,
            longitude: String
        ): Intent = Intent(ctx, MaterialUsageActivity::class.java).apply {
            putExtra(MaterialUsageViewModel.ARG_WORK_ID, workId)
            putExtra(MaterialUsageViewModel.ARG_PROJECT_ID, projectId)
            putExtra(MaterialUsageViewModel.ARG_STATUS_ID, statusId)
            putExtra(MaterialUsageViewModel.ARG_REMARKS, remarks)
            putExtra(MaterialUsageViewModel.ARG_LAT, latitude)
            putExtra(MaterialUsageViewModel.ARG_LNG, longitude)
        }
    }

    override val bindingActivity: ActivityBinding
        get() = ActivityBinding(R.layout.activity_material_usage, MaterialUsageViewModel::class.java)

    override fun onCreateActivity(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setToolbar()
        setupList()
        setupSearch()
        setupActions()
        observeState()
    }

    private fun setToolbar() {
        binding.materialUsageToolbar.toolbarTitle.text = getString(R.string.material_usage_title)
        binding.materialUsageToolbar.toolbarBackButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun subscribeToEvents(vm: MaterialUsageViewModel) {
        // no-op
    }

    private fun setupList() {
        adapter = MaterialAdapter(onQuantityChanged = viewModel::onQuantityChanged)
        binding.materialRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MaterialUsageActivity)
            setHasFixedSize(true)
            adapter = this@MaterialUsageActivity.adapter
            // Disable change animations — they fight the EditText focus
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                searchDebounceJob?.cancel()
                searchDebounceJob = lifecycleScope.launch {
                    delay(250)
                    viewModel.onSearchChanged(s?.toString().orEmpty())
                }
            }
        })
        binding.searchClearIcon.setOnClickListener { binding.searchEditText.setText("") }
    }

    private fun setupActions() {
        binding.tvClearAll.setOnClickListener { confirmClearAll() }
        binding.btnSubmit.setOnClickListener { confirmSubmit() }
        binding.btnRetry.setOnClickListener {
            viewModel.loadMaterials()
        }
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setTitle(R.string.material_clear_all_title)
            .setMessage(R.string.material_clear_all_msg)
            .setPositiveButton(R.string.material_clear) { d, _ ->
                viewModel.clearAllEntries()
                d.dismiss()
            }
            .setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }
            .show()
    }

    private fun confirmSubmit() {
        val s = viewModel.state.value
        if (s.hasAnyError) {
            Toast.makeText(this, R.string.material_fix_errors, Toast.LENGTH_SHORT).show()
            return
        }
        val msg = getString(R.string.material_confirm_submit_msg, s.addedCount, formatQty(s.totalQuantity))
        AlertDialog.Builder(this)
            .setTitle(R.string.material_confirm_submit_title)
            .setMessage(msg)
            .setPositiveButton(R.string.material_yes_complete) { d, _ ->
                viewModel.submit()
                d.dismiss()
            }
            .setNegativeButton(R.string.material_review) { d, _ -> d.dismiss() }
            .show()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: MaterialUsageUiState) {
        val isLoading = state.loading

        // Loading skeleton
        if (isLoading) showProgress() else dismissProgress()
        binding.materialRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE

        val hasError = state.errorMessage != null
        val hasRows = state.allRows.isNotEmpty()

        // Inline error (load failure with no rows)
        binding.errorContainer.visibility = if (!isLoading && hasError && !hasRows) View.VISIBLE else View.GONE
        binding.tvErrorMessage.text = state.errorMessage.orEmpty()

        // Empty (no materials assigned)
        binding.emptyContainer.visibility = if (!isLoading && !hasError && !hasRows) View.VISIBLE else View.GONE

        // Header counts
        val total = state.allRows.size
        val visible = state.visibleRows.size
        binding.tvMaterialCount.text = if (state.searchQuery.isBlank()) {
            getString(R.string.material_count_total, total)
        } else {
            getString(R.string.material_count_matched, visible, total)
        }

        binding.tvClearAll.visibility = if (state.addedCount > 0) View.VISIBLE else View.GONE
        binding.searchClearIcon.visibility = if (state.searchQuery.isNotEmpty()) View.VISIBLE else View.GONE

        // List
        if (state.visibleRows !== lastVisibleRows) {
            adapter.submitList(state.visibleRows)
            lastVisibleRows = state.visibleRows
        }

        // Sticky footer
        binding.tvAddedCount.text = getString(R.string.material_added_count, state.addedCount)
        binding.tvTotalQty.text = getString(R.string.material_total_qty, formatQty(state.totalQuantity))
        binding.btnSubmit.apply {
            isEnabled = !state.submitting && !state.hasAnyError
            alpha = if (isEnabled) 1f else 0.5f
            text = when {
                state.submitting -> getString(R.string.material_submitting)
                state.addedCount == 0 -> getString(R.string.material_submit_without)
                else -> getString(R.string.material_submit_completion)
            }
        }

        // Transient error after a failed submit (rows already loaded)
        if (hasError && hasRows && !state.submitting) {
            try {
                Toast.makeText(this, state.errorMessage, Toast.LENGTH_LONG).show()
            } finally {
                viewModel.consumeError()
            }
        }

        // Success — finish with result
        if (state.submitSuccess) {
            val resultIntent = Intent().apply {
                putExtra(SharingKeys.USED_MATERIALS, ArrayList(viewModel.buildUsedMaterials()))
            }
            setResult(RESULT_SUBMITTED, resultIntent)
            finish()
        }
    }

    private fun formatQty(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else qtyFormatter.format(value)

    override fun onDestroy() {
        searchDebounceJob?.cancel()
        binding.materialRecyclerView.adapter = null
        super.onDestroy()
    }
}
