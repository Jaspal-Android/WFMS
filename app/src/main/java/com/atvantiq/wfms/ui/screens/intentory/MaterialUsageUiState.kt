package com.atvantiq.wfms.ui.screens.intentory

import com.atvantiq.wfms.models.inventory.InventoryData


data class MaterialRow(
    val item: InventoryData,
    val rawInput: String = "",
    val error: ErrorType? = null
) {
    val isOutOfStock: Boolean get() = item.availableQuantity <= 0.0

    /** Parsed quantity, null when input is blank or unparsable. */
    val parsedQty: Double? get() = rawInput.toDoubleOrNull()

    val isAdded: Boolean
        get() = error == null && (parsedQty ?: 0.0) > 0.0
}

enum class ErrorType { NEGATIVE, EXCEEDS_STOCK, INVALID }

/**
 * Single immutable snapshot of the screen state. Exposed by the ViewModel as
 * a [kotlinx.coroutines.flow.StateFlow] and rendered by the Fragment.
 */
data class MaterialUsageUiState(
    val loading: Boolean = true,
    val errorMessage: String? = null,
    val allRows: List<MaterialRow> = emptyList(),
    val searchQuery: String = "",
    val submitting: Boolean = false,
    val submitSuccess: Boolean = false
) {
    /** Filtered, search-applied view of [allRows]. Lazy by design. */
    val visibleRows: List<MaterialRow>
        get() {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) return allRows
            return allRows.filter {
                it.item.name.lowercase().contains(q) ||
                    it.item.unit.lowercase().contains(q)
            }
        }

    val addedCount: Int get() = allRows.count { it.isAdded }
    val totalQuantity: Double
        get() = allRows.sumOf { if (it.isAdded) it.parsedQty ?: 0.0 else 0.0 }

    val hasAnyError: Boolean get() = allRows.any { it.error != null }
}
