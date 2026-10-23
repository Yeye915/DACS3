package com.example.dacs3

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityRentedRoomsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RentedRoomsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRentedRoomsBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val rentedList = mutableListOf<HandoverRequest>()
    private lateinit var adapter: RentedRoomsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRentedRoomsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        loadMyRentedRooms()
    }

    private fun setupRecyclerView() {
        adapter = RentedRoomsAdapter(rentedList)
        binding.rvRentedRooms.layoutManager = LinearLayoutManager(this)
        binding.rvRentedRooms.adapter = adapter
    }

    private fun loadMyRentedRooms() {
        val uid = auth.currentUser?.uid ?: return

        // 1. Lấy tên chính chủ người thuê
        db.collection("Users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val myName = userDoc.getString("fullName") ?: ""

                // 2. Tìm kiếm trong handovers những phòng có tên của họ
                db.collection("handovers")
                    .whereEqualTo("tenantName", myName)
                    .addSnapshotListener { snapshots, _ ->
                        if (snapshots != null) {
                            rentedList.clear()
                            for (doc in snapshots) {
                                val room = doc.toObject(HandoverRequest::class.java)
                                room.id = doc.id
                                rentedList.add(room)
                            }

                            if (rentedList.isEmpty()) {
                                binding.tvEmptyRented.visibility = View.VISIBLE
                            } else {
                                binding.tvEmptyRented.visibility = View.GONE
                            }
                            adapter.notifyDataSetChanged()
                        }
                    }
            }
    }
}