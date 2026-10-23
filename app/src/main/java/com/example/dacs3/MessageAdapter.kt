package com.example.dacs3

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.dacs3.databinding.ItemMessageBinding
import com.google.firebase.auth.FirebaseAuth

class MessageAdapter(
    // Đã đồng bộ tên danh sách nhất quán
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

    // SỬA: Đổi kiểu đối số từ 'ViewHolder' thành 'MessageViewHolder' chuẩn xác
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        // SỬA: Đổi từ 'list' sang 'messageList' cho khớp biến truyền vào
        val message = messageList[position]
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        holder.binding.txtMessage.text = message.text

        // Hiển thị tên người gửi (Nhi nhớ kiểm tra xem Model Message của Nhi đã có thuộc tính senderName chưa nha)
        holder.binding.txtSenderName.text = message.senderName

        // Ép kiểu LayoutParams đồng bộ với LinearLayout chứa tin nhắn
        val params = holder.binding.layoutMessageContainer.layoutParams as LinearLayout.LayoutParams

        if (message.senderId == currentUid) {
            // 1. NẾU LÀ MÌNH GỬI: Căn phải + Khung màu Xanh nhạt
            params.gravity = android.view.Gravity.END
            holder.binding.txtSenderName.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = android.view.Gravity.END }

            holder.binding.txtMessage.setBackgroundResource(R.drawable.bg_message_sent)
        } else {
            // 2. NẾU NGƯỜI KHÁC GỬI: Căn trái + Khung màu Trắng trứng
            params.gravity = android.view.Gravity.START
            holder.binding.txtSenderName.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = android.view.Gravity.START }

            holder.binding.txtMessage.setBackgroundResource(R.drawable.bg_message_received)
        }

        holder.binding.layoutMessageContainer.layoutParams = params
    }

    override fun getItemCount(): Int {
        return messageList.size
    }
}