package com.example.dacs3

import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dacs3.databinding.ItemMessageBinding

class MessageAdapter(private val messages: List<Message>) : RecyclerView.Adapter<MessageAdapter.Holder>() {
    class Holder(val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup,type: Int)=Holder(
        ItemMessageBinding.inflate(LayoutInflater.from(parent.context),parent,false))
    override fun onBindViewHolder(holder: Holder,position: Int) {
        val message=messages[position]
        val own=message.senderId==Backend.auth.currentUser?.uid
        holder.binding.apply {
            txtSenderName.text=message.senderName
            txtMessage.text=message.text
            // The row belongs to RecyclerView. Align its children instead of casting its LayoutParams.
            layoutMessageContainer.gravity=if(own) Gravity.END else Gravity.START
            txtMessage.setBackgroundResource(if(own) R.drawable.bg_message_sent else R.drawable.bg_message_received)
        }
    }
    override fun getItemCount()=messages.size
}
