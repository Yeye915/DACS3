package com.example.dacs3

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val chatList: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val TYPE_USER = 1
    private val TYPE_AI = 2

    override fun getItemViewType(position: Int) = if (chatList[position].isUser) TYPE_USER else TYPE_AI

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_USER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_ai, parent, false)
            AIViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chat = chatList[position]
        if (holder is UserViewHolder) holder.bind(chat) else (holder as AIViewHolder).bind(chat)
    }

    override fun getItemCount() = chatList.size

    class UserViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        fun bind(chat: ChatMessage) { itemView.findViewById<TextView>(R.id.tvMessageUser).text = chat.message }
    }
    class AIViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        fun bind(chat: ChatMessage) { itemView.findViewById<TextView>(R.id.tvMessageAI).text = chat.message }
    }
}