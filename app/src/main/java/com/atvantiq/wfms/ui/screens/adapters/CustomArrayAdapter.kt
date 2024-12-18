package com.atvantiq.wfms.ui.screens.adapters

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

class CustomArrayAdapter(
    context: Context,
    private val resource: Int,
    private val items: List<String>
) : ArrayAdapter<String>(context, resource, items) {

    private val filteredItems = mutableListOf<String>()

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
                    filteredItems.clear()
                    filteredItems.addAll(items) // Show all items for empty input
                } else {
                    val query = constraint.toString().lowercase()
                    filteredItems.clear()
                    filteredItems.addAll(items.filter { it.lowercase().contains(query) })
                }
                results.values = filteredItems
                results.count = filteredItems.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                notifyDataSetChanged()
            }
        }
    }

}
