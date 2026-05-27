package com.example.dacs3

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityChatBinding
import com.google.firebase.firestore.FirebaseFirestore

class ChatActivity : AppCompatActivity() {

    private lateinit var binding:
            ActivityChatBinding

    private lateinit var adapter:
            MessageAdapter

    private val messageList =
        mutableListOf<Message>()

    private lateinit var firestore:
            FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding =
            ActivityChatBinding.inflate(
                layoutInflater
            )

        setContentView(binding.root)

        firestore =
            FirebaseFirestore.getInstance()

        setupRecycler()

        loadMessages()

        binding.btnSend.setOnClickListener {

            sendMessage()
        }
    }

    private fun setupRecycler() {

        adapter =
            MessageAdapter(messageList)

        binding.recyclerMessage.layoutManager =
            LinearLayoutManager(this)

        binding.recyclerMessage.adapter =
            adapter
    }

    private fun sendMessage() {

        val text =
            binding.edtMessage.text
                .toString()

        if (text.isEmpty()) return

        val message = hashMapOf(

            "senderId" to "Guest",

            "message" to text,

            "time" to System.currentTimeMillis()
        )

        firestore.collection("messages")
            .add(message)

        binding.edtMessage.text.clear()
    }

    private fun loadMessages() {

        firestore.collection("messages")

            .orderBy("time")

            .addSnapshotListener { value, _ ->

                messageList.clear()

                value?.documents?.forEach {

                    val message =
                        it.toObject(
                            Message::class.java
                        )

                    if (message != null) {

                        messageList.add(message)
                    }
                }

                adapter.notifyDataSetChanged()
            }
    }
}