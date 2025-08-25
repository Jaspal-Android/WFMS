package com.atvantiq.wfms.ui.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.DialogMultiSelectBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MultiSelectBottomSheetDialog<T>(
    private val context: Context,
    private val items: List<T>,
    private val preSelectedItems: Set<T>,
    private val bind: (View, T, Boolean) -> Unit,
    private val onSelectionChanged: (Set<T>) -> Unit,
    private val onSubmit: (Set<T>) -> Unit,
    private val filterCondition: (T, String) -> Boolean ,
    private val title: String? = null// Filter condition for search
) : BottomSheetDialogFragment() {

    private lateinit var binding: DialogMultiSelectBottomSheetBinding
    private val selectedItems = mutableSetOf<T>().apply { addAll(preSelectedItems) } // Initialize with pre-selected items
    private var filteredItems = items.toMutableList() // List to hold filtered items

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogMultiSelectBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!title.isNullOrEmpty()) {
            binding.titleTextView.visibility = View.VISIBLE
            binding.titleTextView.text = title
        } else {
            binding.titleTextView.visibility = View.GONE
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    binding.searchBar.clearFocus() // Clear focus on scroll
                }
            }
        })
        val adapter = MultiSelectAdapter(filteredItems, bind, selectedItems, onSelectionChanged)
        binding.recyclerView.adapter = adapter

        // Add search functionality
        binding.searchBar.addTextChangedListener { text ->
            val query = text.toString()
            filteredItems.clear()
            filteredItems.addAll(items.filter { filterCondition(it, query) })
            adapter.notifyDataSetChanged()
        }

        binding.submitButton.setOnClickListener {
            onSubmit(selectedItems) // Return selected items on submit
            dismiss() // Close the dialog
        }
    }

    private class MultiSelectAdapter<T>(
        private val items: List<T>,
        private val bind: (View, T, Boolean) -> Unit,
        private val selectedItems: MutableSet<T>,
        private val onSelectionChanged: (Set<T>) -> Unit
    ) : RecyclerView.Adapter<MultiSelectAdapter.ViewHolder<T>>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<T> {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_multi_select, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder<T>, position: Int) {
            val item = items[position]
            val isSelected = selectedItems.contains(item)
            holder.bind(item, isSelected, bind) { isChecked ->
                if (isChecked) {
                    selectedItems.add(item)
                } else {
                    selectedItems.remove(item)
                }
                onSelectionChanged(selectedItems)
            }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)

            fun bind(
                item: T,
                isSelected: Boolean,
                bind: (View, T, Boolean) -> Unit,
                onCheckedChange: (Boolean) -> Unit
            ) {
                bind(itemView, item, isSelected)
                checkBox.isChecked = isSelected
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    onCheckedChange(isChecked)
                }
            }
        }
    }
}
