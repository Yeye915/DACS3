package com.example.dacs3

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dacs3.databinding.ItemMessageBinding

class MessageAdapter(
    private val messageList: List<Message>
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(
        val binding: ItemMessageBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MessageViewHolder {

        val binding = ItemMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MessageViewHolder,
        position: Int
    ) {

        val message = messageList[position]

        holder.binding.txtMessage.text =
            message.message
    }

    override fun getItemCount(): Int {

        return messageList.size
    }
}