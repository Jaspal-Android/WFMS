package com.atvantiq.wfms.ui.screens.intentory

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.atvantiq.wfms.base.BaseViewModel
import com.atvantiq.wfms.data.repository.work.WorkRepo
import com.atvantiq.wfms.models.inventory.InventoryData
import com.atvantiq.wfms.models.inventory.UsedMaterial
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MaterialUsageViewModel @Inject constructor(
    application: Application,
    private val workRepo: WorkRepo,
    private val savedState: SavedStateHandle
) : BaseViewModel(application) {

    companion object {
        private const val KEY_INPUTS = "material_inputs"
        private const val KEY_QUERY  = "material_search_query"
        const val ARG_WORK_ID    = "arg_work_id"
        const val ARG_PROJECT_ID = "arg_project_id"
        const val ARG_STATUS_ID  = "arg_status_id"
        const val ARG_REMARKS    = "arg_remarks"
        const val ARG_LAT        = "arg_lat"
        const val ARG_LNG        = "arg_lng"
    }

    private val projectId: Long = savedState[ARG_PROJECT_ID] ?: 0L

    private val _state = MutableStateFlow(MaterialUsageUiState())
    val state: StateFlow<MaterialUsageUiState> = _state.asStateFlow()

    init {
        loadMaterials()
    }

    fun loadMaterials() {
        executeStateRequest(
            state = _state,
            apiCall = { workRepo.inventoryByProject(projectId) },
            setLoading = { it.copy(loading = true, errorMessage = null) },
            onSuccess = { prev, items -> applyLoaded(prev, items.data) },
            onError = { prev, e ->
                prev.copy(loading = false, errorMessage = e.message ?: "Failed to load materials")
            }
        )
    }

    private fun applyLoaded(prev: MaterialUsageUiState, items: List<InventoryData>?): MaterialUsageUiState {
        val saved: Map<Long, String> = savedState.get<HashMap<Long, String>>(KEY_INPUTS).orEmpty()
        val savedQuery: String       = savedState[KEY_QUERY] ?: prev.searchQuery
        val rows = items?.map { item ->
            val raw = saved[item.id].orEmpty()
            MaterialRow(item = item, rawInput = raw, error = validate(raw, item))
        }
        return prev.copy(
            loading = false,
            allRows = rows ?: emptyList(),
            searchQuery = savedQuery,
            errorMessage = null
        )
    }


    fun onQuantityChanged(materialId: Long, raw: String) {
        val sanitized = sanitize(raw)
        _state.update { current ->
            val updatedRows = current.allRows.map { row ->
                if (row.item.id != materialId) row
                else row.copy(rawInput = sanitized, error = validate(sanitized, row.item))
            }
            current.copy(allRows = updatedRows)
        }
        persistInputs()
    }

    fun onSearchChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
        savedState[KEY_QUERY] = query
    }

    fun clearAllEntries() {
        _state.update { current ->
            current.copy(allRows = current.allRows.map { it.copy(rawInput = "", error = null) })
        }
        savedState[KEY_INPUTS] = HashMap<Long, String>()
    }

    fun buildUsedMaterials(): List<UsedMaterial> =
        _state.value.allRows
            .filter { it.isAdded }
            .mapNotNull { row -> row.parsedQty?.let { UsedMaterial(row.item.id, it) } }

    fun submit() {
        val snapshot = _state.value
        if (snapshot.hasAnyError || snapshot.submitting) return
        _state.update { it.copy(submitSuccess = true) }
    }

    fun consumeError() {
        _state.update { it.copy(errorMessage = null) }
    }

    // Helpers
    private fun sanitize(input: String): String {
        val trimmed = input.replace(Regex("[^0-9.]"), "")
        val firstDot = trimmed.indexOf('.')
        return if (firstDot == -1) trimmed
               else trimmed.take(firstDot + 1) + trimmed.substring(firstDot + 1).replace(".", "")
    }

    private fun validate(raw: String, item: InventoryData): ErrorType? {
        if (raw.isBlank() || raw == ".") return null
        val parsed = raw.toDoubleOrNull() ?: return ErrorType.INVALID
        return when {
            parsed < 0.0 -> ErrorType.NEGATIVE
            parsed > item.availableQuantity -> ErrorType.EXCEEDS_STOCK
            else -> null
        }
    }

    private fun persistInputs() {
        val map = HashMap<Long, String>()
        _state.value.allRows.forEach { if (it.rawInput.isNotBlank()) map[it.item.id] = it.rawInput }
        savedState[KEY_INPUTS] = map
    }
}
