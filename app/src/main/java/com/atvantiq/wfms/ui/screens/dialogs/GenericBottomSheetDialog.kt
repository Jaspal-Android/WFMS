import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atvantiq.wfms.R
import com.atvantiq.wfms.databinding.DialogGenericBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class GenericBottomSheetDialog<T>(
    private val context: Context,
    private val items: List<T>,
    private val layoutResId: Int,
    private val bind: (View, T) -> Unit,
    private val onItemSelected: (T) -> Unit,
    private val filterCondition: (T, String) -> Boolean,
    private val title: String? = null // Added title parameter
) : BottomSheetDialogFragment() {

    private lateinit var binding: DialogGenericBottomSheetBinding
    private lateinit var adapter: RecyclerViewGenericAdapter<T>
    private var filteredItems: MutableList<T> = items.toMutableList()

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
            filteredItems,
            layoutResId,
            bind
        ) { selectedItem ->
            onItemSelected(selectedItem)
            dismiss()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
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
                context,
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
        filteredItems.addAll(items.filter { filterCondition(it, query) })
        adapter.notifyDataSetChanged()
    }
}
