package com.atvantiq.wfms.ui.screens.dialogs


import RecyclerViewGenericAdapter
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.DialogGenericBottomSheetBinding
import com.atvantiq.wfms.utils.dismissRestoredWithoutCallbacks
import com.atvantiq.wfms.widgets.BaseBottomSheet


class SimpleBottomSheetDialog<T>() : BaseBottomSheet() {

    // Items and callbacks are wired by the host through the secondary constructor. The no-arg
    // constructor lets the FragmentManager re-instantiate this sheet on restore (process death,
    // recreate() on a theme change) without an InstantiationException; a sheet that comes back
    // unwired dismisses itself instead of showing a dead list.
    private var items: List<T> = emptyList()
    private var layoutResId: Int = 0
    private var bind: ((View, T) -> Unit)? = null
    private var onItemSelected: ((T) -> Unit)? = null
    private var title: String? = null

    private lateinit var binding: DialogGenericBottomSheetBinding
    private lateinit var adapter: RecyclerViewGenericAdapter<T>

    constructor(
        items: List<T>,
        layoutResId: Int,
        bind: (View, T) -> Unit,
        onItemSelected: (T) -> Unit,
        title: String? = null
    ) : this() {
        this.items = items
        this.layoutResId = layoutResId
        this.bind = bind
        this.onItemSelected = onItemSelected
        this.title = title
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
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

        val bind = bind
        val onItemSelected = onItemSelected
        if (bind == null || onItemSelected == null) {
            dismissRestoredWithoutCallbacks()
            return
        }

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

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
        binding.recyclerView.adapter = adapter

        // Hide search bar for simple version
        binding.searchBar.visibility = View.GONE
    }
}
