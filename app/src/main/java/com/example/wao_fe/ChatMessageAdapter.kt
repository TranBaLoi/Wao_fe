package com.example.wao_fe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.wao_fe.network.models.ChatbotMessageItem

class ChatMessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<ChatbotMessageItem>()

    fun setMessages(items: List<ChatbotMessageItem>) {
        messages.clear()
        messages.addAll(items)
        notifyDataSetChanged()
    }

    fun addMessage(item: ChatbotMessageItem) {
        messages.add(item)
        notifyItemInserted(messages.size - 1)
    }

    fun clear() {
        messages.clear()
        notifyDataSetChanged()
    }

    fun setTypingVisible(visible: Boolean) {
        val index = messages.indexOfFirst { it.role.uppercase() == ROLE_TYPING }
        if (visible && index == -1) {
            messages.add(ChatbotMessageItem(id = -1L, role = ROLE_TYPING, content = "..."))
            notifyItemInserted(messages.size - 1)
        } else if (!visible && index != -1) {
            messages.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        val role = messages[position].role.uppercase()
        return when (role) {
            ROLE_USER -> VIEW_TYPE_USER
            ROLE_TYPING -> VIEW_TYPE_TYPING
            else -> VIEW_TYPE_AI
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val view = inflater.inflate(R.layout.item_chat_message_user, parent, false)
                UserViewHolder(view)
            }
            VIEW_TYPE_TYPING -> {
                val view = inflater.inflate(R.layout.item_chat_message_typing, parent, false)
                TypingViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_chat_message_ai, parent, false)
                AiViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = messages[position]
        val timeText = formatTime(item.createdAt)
        if (holder is UserViewHolder) {
            holder.message.text = item.content
            holder.time.text = timeText
        } else if (holder is AiViewHolder) {
            holder.message.text = item.content
            holder.time.text = timeText
        } else if (holder is TypingViewHolder) {
            holder.bind()
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is TypingViewHolder) {
            holder.stop()
        }
        super.onViewRecycled(holder)
    }

    private fun formatTime(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val timeIndex = raw.indexOf('T')
        if (timeIndex != -1 && raw.length >= timeIndex + 6) {
            return raw.substring(timeIndex + 1, timeIndex + 6)
        }
        return if (raw.length >= 5) raw.take(5) else raw
    }

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val message: TextView = view.findViewById(R.id.tvUserMessage)
        val time: TextView = view.findViewById(R.id.tvUserTime)
    }

    class AiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val message: TextView = view.findViewById(R.id.tvAiMessage)
        val time: TextView = view.findViewById(R.id.tvAiTime)
    }

    class TypingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val typingText: TextView = view.findViewById(R.id.tvTyping)
        private val frames = arrayOf(".", "..", "...")
        private var frameIndex = 0
        private val ticker = object : Runnable {
            override fun run() {
                typingText.text = frames[frameIndex % frames.size]
                frameIndex++
                typingText.postDelayed(this, 320)
            }
        }

        fun bind() {
            stop()
            frameIndex = 0
            typingText.text = frames[0]
            typingText.post(ticker)
        }

        fun stop() {
            typingText.removeCallbacks(ticker)
        }
    }

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
        private const val VIEW_TYPE_TYPING = 3

        private const val ROLE_USER = "USER"
        private const val ROLE_TYPING = "TYPING"
    }
}
