package com.example.dacs3

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope // Dùng cái này thay cho MainScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityChatAiBinding // Import này rất quan trọng
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch

class ChatAIActivity : AppCompatActivity() {
    // Nếu dòng này vẫn đỏ, hãy xem bước 3 bên dưới
    private lateinit var binding: ActivityChatAiBinding
    private val chatList = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = "AIzaSyCgerrozztJPUc2GFSWctVods4f6HrnNFw" // Nhớ thay Key thật của Nhi vào đây
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Nếu dòng này đỏ, hãy đảm bảo tên file XML của Nhi là activity_chat_ai.xml
        binding = ActivityChatAiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Thiết lập RecyclerView
        chatAdapter = ChatAdapter(chatList)
        binding.rvChat.layoutManager = LinearLayoutManager(this)
        binding.rvChat.adapter = chatAdapter

        binding.btnSend.setOnClickListener {
            val prompt = binding.etMessage.text.toString().trim()
            if (prompt.isNotEmpty()) {
                // 1. Thêm tin nhắn của Nhi vào list
                chatList.add(ChatMessage(prompt, true))
                chatAdapter.notifyItemInserted(chatList.size - 1)
                binding.rvChat.scrollToPosition(chatList.size - 1)

                askGemini(prompt)
                binding.etMessage.text.clear()
            }
        }
    }

    private fun askGemini(prompt: String) {
        lifecycleScope.launch {
            try {
                val response = generativeModel.generateContent(prompt)
                val aiMsg = response.text ?: "Xin lỗi, mình không hiểu ý bạn."

                // XÓA cái AlertDialog cũ đi và thay bằng đoạn này:
                chatList.add(ChatMessage(aiMsg, false)) // false nghĩa là tin nhắn từ AI

                // Thông báo cho Adapter biết có tin nhắn mới để vẽ lên màn hình
                chatAdapter.notifyItemInserted(chatList.size - 1)

                // Tự động cuộn xuống dưới cùng để thấy tin nhắn mới nhất
                binding.rvChat.scrollToPosition(chatList.size - 1)

            } catch (e: Exception) {
                chatList.add(ChatMessage("Lỗi rồi Nhi ơi: ${e.message}", false))
                chatAdapter.notifyDataSetChanged()
            }
        }
    }


}