// PATH: app/src/main/java/com/example/minlish/DeckAdapter.kt
package com.example.minlish

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeckAdapter(
    private val deckList: MutableList<DeckItem>
) : RecyclerView.Adapter<DeckAdapter.DeckViewHolder>() {

    private var onItemClick: ((DeckItem) -> Unit)? = null
    private var onItemLongClick: ((DeckItem) -> Unit)? = null

    fun setOnItemClickListener(listener: (DeckItem) -> Unit) { onItemClick = listener }
    fun setOnItemLongClickListener(listener: (DeckItem) -> Unit) { onItemLongClick = listener }

    inner class DeckViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDeckName: TextView        = itemView.findViewById(R.id.tvDeckName)
        val tvDeckDescription: TextView = itemView.findViewById(R.id.tvDeckDescription)
        val tvWordCount: TextView       = itemView.findViewById(R.id.tvWordCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeckViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deck, parent, false)
        return DeckViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeckViewHolder, position: Int) {
        val item = deckList[position]
        holder.tvDeckName.text        = item.name
        holder.tvDeckDescription.text = item.description
        holder.tvWordCount.text       = "${item.wordCount} từ"

        holder.itemView.setOnClickListener      { onItemClick?.invoke(item) }
        holder.itemView.setOnLongClickListener  { onItemLongClick?.invoke(item); true }
    }

    override fun getItemCount(): Int = deckList.size
}

data class DeckItem(
    val id: Int,
    val name: String,
    val description: String,
    val wordCount: Int
)