package com.example.wao_fe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.wao_fe.network.models.ChatbotConversationSummary

class ConversationAdapter(
    private val onClick: (Long) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    private val items = mutableListOf<ChatbotConversationSummary>()

    fun setItems(list: List<ChatbotConversationSummary>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.time.text = formatTime(item.updatedAt)
        holder.itemView.setOnClickListener { onClick(item.id) }
    }

    private fun formatTime(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val timeIndex = raw.indexOf('T')
        if (timeIndex != -1 && raw.length >= timeIndex + 6) {
            return raw.substring(timeIndex + 1, timeIndex + 6)
        }
        return if (raw.length >= 10) raw.take(10) else raw
    }

    class ConversationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvConversationTitle)
        val time: TextView = view.findViewById(R.id.tvConversationTime)
    }
}
