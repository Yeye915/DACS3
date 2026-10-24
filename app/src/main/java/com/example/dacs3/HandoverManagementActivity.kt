package com.example.dacs3

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityHandoverManagementBinding
import com.google.firebase.firestore.FirebaseFirestore

class HandoverManagementActivity : HouseActivity() {

    private lateinit var binding: ActivityHandoverManagementBinding
    private val db = FirebaseFirestore.getInstance()
    private val handoverList = mutableListOf<HandoverRequest>()
    private lateinit var adapter: HandoverAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHandoverManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        loadHandoverRequests()
    }

    private fun setupRecyclerView() {
        adapter = HandoverAdapter(handoverList) { selectedRequest ->
            showUpdateStatusDialog(selectedRequest)
        }
        binding.rvHandoverRequests.layoutManager = LinearLayoutManager(this)
        binding.rvHandoverRequests.adapter = adapter
    }

    private fun loadHandoverRequests() {
        db.collection("handovers")
            .addSnapshotListener { snapshots, e ->
                if (snapshots != null) {
                    handoverList.clear()
                    for (doc in snapshots) {
                        val request = doc.toObject(HandoverRequest::class.java)
                        request.id = doc.id
                        handoverList.add(request)
                    }

                    if (handoverList.isEmpty()) {
                        binding.tvEmptyHandover.visibility = View.VISIBLE
                    } else {
                        binding.tvEmptyHandover.visibility = View.GONE
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun showUpdateStatusDialog(request: HandoverRequest) {
        val options = arrayOf("Xác nhận đã bàn giao thành công", "Hủy trạng thái hoàn thành")
        AlertDialog.Builder(this)
            .setTitle("Cập nhật tiến độ biên bản")
            .setItems(options) { _, which ->
                // Giá trị logic: true nếu chọn dòng 0 (Thành công), false nếu chọn dòng 1 (Hủy)
                val isDone: Boolean = (which == 0)

                if (request.id.isNotEmpty()) {
                    db.collection("handovers").document(request.id)
                        .update("completed", isDone)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Đã cập nhật trạng thái thành công!", Toast.LENGTH_SHORT).show()
                        }


                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Lỗi cập nhật Firestore: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                } else {
                    Toast.makeText(this, "Lỗi: ID biên bản rỗng, không thể xác nhận!", Toast.LENGTH_SHORT).show()
                }

            }.show()
    }
}