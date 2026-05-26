package com.atvantiq.wfms.ui.screens.intentory

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.ItemMaterialBinding
import com.google.android.material.color.MaterialColors

class MaterialAdapter(
    private val onQuantityChanged: (Long, String) -> Unit
) : ListAdapter<MaterialRow, MaterialAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MaterialRow>() {
            override fun areItemsTheSame(o: MaterialRow, n: MaterialRow): Boolean = o.item.id == n.item.id
            override fun areContentsTheSame(o: MaterialRow, n: MaterialRow): Boolean = o == n
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMaterialBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding, onQuantityChanged)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(
        private val binding: ItemMaterialBinding,
        private val onQuantityChanged: (Long, String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentRow: MaterialRow? = null

        private val colorError = MaterialColors.getColor(itemView, R.attr.wfmsColorError)
        private val colorPrimary = MaterialColors.getColor(itemView, R.attr.wfmsColorPrimary)
        private val colorOutline = MaterialColors.getColor(itemView, R.attr.wfmsColorOutline)

        private val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val row = currentRow ?: return
                val text = s?.toString().orEmpty()
                if (text == row.rawInput) return  // no-op, prevents loop on rebind
                onQuantityChanged(row.item.id, text)
            }
        }

        init {
            binding.etQuantity.apply {
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                minHeight = 0   // override the 48dp default so the row stays compact
                addTextChangedListener(watcher)
            }
        }

        fun bind(row: MaterialRow) {
            currentRow = row

            // ── Text content ──
            binding.tvMaterialName.text = row.item.name
            binding.tvUnitLabel.text    = row.item.unit
            binding.tvAvailable.text    = itemView.context.getString(
                R.string.material_available_qty,
                formatQty(row.item.availableQuantity),
                row.item.unit
            )

            // ── Quantity field (set silently so the watcher doesn't fire) ──
            binding.etQuantity.removeTextChangedListener(watcher)
            if (binding.etQuantity.text?.toString() != row.rawInput) {
                binding.etQuantity.setText(row.rawInput)
                binding.etQuantity.setSelection(row.rawInput.length)
            }
            binding.etQuantity.isEnabled = !row.isOutOfStock
            binding.etQuantity.addTextChangedListener(watcher)

            // ── Out-of-stock pill / available text swap ──
            binding.tvOutOfStock.visibility = if (row.isOutOfStock) View.VISIBLE else View.GONE
            binding.tvAvailable.visibility  = if (row.isOutOfStock) View.GONE else View.VISIBLE
            binding.cardRoot.alpha          = if (row.isOutOfStock) 0.55f else 1f

            // ── Error message ──
            binding.tvErrorMessage.visibility = if (row.error != null) View.VISIBLE else View.GONE
            binding.tvErrorMessage.text = when (row.error) {
                ErrorType.EXCEEDS_STOCK -> itemView.context.getString(
                    R.string.material_err_exceeds, formatQty(row.item.availableQuantity), row.item.unit)
                ErrorType.NEGATIVE      -> itemView.context.getString(R.string.material_err_negative)
                ErrorType.INVALID       -> itemView.context.getString(R.string.material_err_invalid)
                null                    -> ""
            }

            // ── Visual states: ALWAYS set on the same view (cardRoot) for every branch
            // to avoid the recycling stale-background bug.
            val strokeColor = when {
                row.error != null -> colorError
                row.isAdded       -> colorPrimary
                else              -> colorOutline
            }
            val bgRes = when {
                row.isAdded && row.error == null -> R.drawable.bg_material_card_added
                else                              -> R.drawable.bg_material_card
            }
            binding.cardRoot.setBackgroundResource(bgRes)
            binding.qtyInputContainer.setBackgroundResource(
                if (row.error != null) R.drawable.error_bg_outline
                else if (binding.etQuantity.isFocused) R.drawable.bg_qty_input_focused
                else R.drawable.bg_qty_input
            )
        }

        private fun formatQty(value: Double): String =
            if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)
    }
}
