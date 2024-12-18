package com.atvantiq.wfms.ui.screens.adapters

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.Filterable

class CustomAutoCompleteAdapter(
    context: Context,
    private val resource: Int,
    private val items: List<String>
) : ArrayAdapter<String>(context, resource, items), Filterable {

    private var filteredItems: List<String> = items

    override fun getCount(): Int {
        return filteredItems.size
    }

    override fun getItem(position: Int): String? {
        return filteredItems[position]
    }

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
                        it.lowercase().contains(query)
                    }
                    results.values = filteredList
                    results.count = filteredList.size
                }
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = results?.values as? List<String> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}
