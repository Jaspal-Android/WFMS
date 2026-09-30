import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.DialogGenericBottomSheetBinding
import com.atvantiq.wfms.widgets.BaseBottomSheet
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class GenericBottomSheetDialog<T>() : BaseBottomSheet() {

    // Items and callbacks are wired by the host through the secondary constructor. The no-arg
    // constructor lets the FragmentManager re-instantiate this sheet on restore (process death,
    // recreate() on a theme change) without an InstantiationException; a sheet that comes back
    // unwired dismisses itself instead of showing a dead list.
    private var items: List<T> = emptyList()
    private var layoutResId: Int = 0
    private var bind: ((View, T) -> Unit)? = null
    private var onItemSelected: ((T) -> Unit)? = null
    private var filterCondition: ((T, String) -> Boolean)? = null
    private var title: String? = null

    private lateinit var binding: DialogGenericBottomSheetBinding
    private lateinit var adapter: RecyclerViewGenericAdapter<T>
    private val filteredItems: MutableList<T> = mutableListOf()

    constructor(
        items: List<T>,
        layoutResId: Int,
        bind: (View, T) -> Unit,
        onItemSelected: (T) -> Unit,
        filterCondition: (T, String) -> Boolean,
        title: String? = null
    ) : this() {
        this.items = items
        this.layoutResId = layoutResId
        this.bind = bind
        this.onItemSelected = onItemSelected
        this.filterCondition = filterCondition
        this.title = title
        filteredItems.addAll(items)
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
        if (bind == null || onItemSelected == null || filterCondition == null) {
            dismissAllowingStateLoss()
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
            filteredItems,
            layoutResId,
            bind
        ) { selectedItem ->
            onItemSelected(selectedItem)
            dismiss()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    binding.searchBar.clearFocus()
                }
            }
        })
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.addItemDecoration(
            androidx.recyclerview.widget.DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )
        binding.recyclerView.adapter = adapter

        setupSearchBar()
    }

    private fun setupSearchBar() {
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterList(query: String) {
        filteredItems.clear()
        val condition = filterCondition ?: return
        filteredItems.addAll(items.filter { condition(it, query) })
        adapter.notifyDataSetChanged()
    }
}