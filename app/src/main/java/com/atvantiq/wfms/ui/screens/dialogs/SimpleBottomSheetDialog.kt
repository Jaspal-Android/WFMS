package com.atvantiq.wfms.ui.screens.dialogs


import RecyclerViewGenericAdapter
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.DialogGenericBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class SimpleBottomSheetDialog<T>(
    private val context: Context,
    private val items: List<T>,
    private val layoutResId: Int,
    private val bind: (View, T) -> Unit,
    private val onItemSelected: (T) -> Unit,
    private val title: String? = null
) : BottomSheetDialogFragment() {

    private lateinit var binding: DialogGenericBottomSheetBinding
    private lateinit var adapter: RecyclerViewGenericAdapter<T>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(context, R.style.AppBottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogGenericBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set the title if provided
        if (!title.isNullOrEmpty()) {
            binding.titleTextView.visibility = View.VISIBLE
            binding.titleTextView.text = title
        } else {
            binding.titleTextView.visibility = View.GONE
        }

        adapter = RecyclerViewGenericAdapter(
            items,
            layoutResId,
            bind
        ) { selectedItem ->
            onItemSelected(selectedItem)
            dismiss()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )
        binding.recyclerView.adapter = adapter

        // Hide search bar for simple version
        binding.searchBar.visibility = View.GONE
    }
}
