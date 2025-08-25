import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewGenericAdapter<T>(
    private val items: List<T>,
    private val layoutResId: Int,
    private val bind: (View, T) -> Unit,
    private val onItemClick: (T) -> Unit
) : RecyclerView.Adapter<RecyclerViewGenericAdapter.GenericViewHolder<T>>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericViewHolder<T> {
        val view = LayoutInflater.from(parent.context).inflate(layoutResId, parent, false)
        return GenericViewHolder(view, bind, onItemClick)
    }

    override fun onBindViewHolder(holder: GenericViewHolder<T>, position: Int) {
        holder.bindItem(items[position])
    }

    override fun getItemCount(): Int = items.size

    class GenericViewHolder<T>(
        private val view: View,
        private val bind: (View, T) -> Unit,
        private val onItemClick: (T) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        fun bindItem(item: T) {
            bind(view, item)
            view.setOnClickListener { onItemClick(item) }
        }
    }
}
