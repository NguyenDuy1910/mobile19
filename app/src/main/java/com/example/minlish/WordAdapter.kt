package com.example.minlish

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WordAdapter(
    private val wordList: MutableList<Vocabulary>
) : RecyclerView.Adapter<WordAdapter.WordViewHolder>() {

    private var onItemClick: ((Vocabulary) -> Unit)? = null

    fun setOnItemClickListener(
        listener: (Vocabulary) -> Unit
    ) {
        onItemClick = listener
    }

    class WordViewHolder(itemView: View)
        : RecyclerView.ViewHolder(itemView) {

        val tvWord: TextView =
            itemView.findViewById(R.id.tvWord)

        val tvMeaning: TextView =
            itemView.findViewById(R.id.tvMeaning)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): WordViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_vocabulary,
                parent,
                false
            )

        return WordViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: WordViewHolder,
        position: Int
    ) {

        val vocabulary = wordList[position]

        holder.tvWord.text = vocabulary.word

        holder.tvMeaning.text = vocabulary.meaning

        holder.itemView.setOnClickListener {

            onItemClick?.invoke(vocabulary)
        }
    }

    override fun getItemCount(): Int {

        return wordList.size
    }
}