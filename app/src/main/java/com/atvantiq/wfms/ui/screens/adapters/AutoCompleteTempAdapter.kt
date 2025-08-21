import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable

class AutoCompleteTempAdapter<T>(
    context: Context,
    private val resource: Int,
    val items: List<T>
) : ArrayAdapter<T>(context, resource, items), Filterable {

    private var filteredItems: List<T> = items

    override fun getCount(): Int = filteredItems.size

    override fun getItem(position: Int): T? = filteredItems[position]

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                if (constraint.isNullOrEmpty()) {
                    results.values = items
                    results.count = items.size
                } else {
                    val query = constraint.toString().lowercase()
                    val filteredList = items.filter {
                        it.toString().lowercase().contains(query)
                    }
                    results.values = filteredList
                    results.count = filteredList.size
                }
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = results?.values as? List<T> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}