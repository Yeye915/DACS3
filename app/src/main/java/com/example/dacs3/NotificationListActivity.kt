package com.example.dacs3

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityNotificationListBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationListActivity : HouseActivity() {

    private lateinit var binding: ActivityNotificationListBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val notiList = mutableListOf<NotificationItem>()
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        loadMyNotifications()
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(notiList)
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
    }

    private fun loadMyNotifications() {
        val currentUid = auth.currentUser?.uid ?: return

        // Quét collection "notifications" lọc theo userUid và sắp xếp thông báo mới nhất lên đầu
        db.collection("notifications")
            .whereEqualTo("userUid", currentUid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    notiList.clear()
                    for (doc in snapshots) {
                        val item = doc.toObject(NotificationItem::class.java)
                        item.id = doc.id
                        notiList.add(item)
                    }

                    if (notiList.isEmpty()) {
                        binding.tvEmptyNoti.visibility = View.VISIBLE
                    } else {
                        binding.tvEmptyNoti.visibility = View.GONE
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }
}