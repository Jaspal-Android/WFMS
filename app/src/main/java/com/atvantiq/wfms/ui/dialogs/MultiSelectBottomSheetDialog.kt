package com.atvantiq.wfms.ui.dialogs

import android.app.Dialog
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
import com.atvantiq.wfms.widgets.BaseBottomSheet
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MultiSelectBottomSheetDialog<T>() : BaseBottomSheet() {

    // Items and callbacks are wired by the host through the secondary constructor. The no-arg
    // constructor lets the FragmentManager re-instantiate this sheet on restore (process death,
    // recreate() on a theme change) without an InstantiationException; a sheet that comes back
    // unwired dismisses itself instead of showing a dead list.
    private var items: List<T> = emptyList()
    private var bind: ((View, T, Boolean) -> Unit)? = null
    private var onSelectionChanged: ((Set<T>) -> Unit)? = null
    private var onSubmit: ((Set<T>) -> Unit)? = null
    private var filterCondition: ((T, String) -> Boolean)? = null
    private var title: String? = null

    private lateinit var binding: DialogMultiSelectBottomSheetBinding
    private val selectedItems = mutableSetOf<T>()
    private val filteredItems = mutableListOf<T>() // List to hold filtered items

    constructor(
        items: List<T>,
        preSelectedItems: Set<T>,
        bind: (View, T, Boolean) -> Unit,
        onSelectionChanged: (Set<T>) -> Unit,
        onSubmit: (Set<T>) -> Unit,
        filterCondition: (T, String) -> Boolean,
        title: String? = null
    ) : this() {
        this.items = items
        this.bind = bind
        this.onSelectionChanged = onSelectionChanged
        this.onSubmit = onSubmit
        this.filterCondition = filterCondition
        this.title = title
        selectedItems.addAll(preSelectedItems)
        filteredItems.addAll(items)
    }

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

        val bind = bind
        val onSelectionChanged = onSelectionChanged
        val onSubmit = onSubmit
        val filterCondition = filterCondition
        if (bind == null || onSelectionChanged == null || onSubmit == null || filterCondition == null) {
            dismissAllowingStateLoss()
            return
        }

        if (!title.isNullOrEmpty()) {
            binding.titleTextView.visibility = View.VISIBLE
            binding.titleTextView.text = title
        } else {
            binding.titleTextView.visibility = View.GONE
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
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
                // A recycled holder still carries the previous item's listener. Detach it before
                // touching the checkbox, otherwise binding this row toggles the previous item.
                checkBox.setOnCheckedChangeListener(null)
                bind(itemView, item, isSelected)
                checkBox.isChecked = isSelected
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    onCheckedChange(isChecked)
                }
            }
        }
    }
}
