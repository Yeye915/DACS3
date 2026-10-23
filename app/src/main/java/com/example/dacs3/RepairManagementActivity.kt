package com.example.dacs3

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityRepairManagementBinding
import com.google.firebase.firestore.FirebaseFirestore

class RepairManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRepairManagementBinding
    private val db = FirebaseFirestore.getInstance()
    private val repairList = mutableListOf<RepairRequest>()
    private lateinit var adapter: RepairAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRepairManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        loadRepairRequests()
    }

    private fun setupRecyclerView() {
        adapter = RepairAdapter(repairList) { selectedRequest ->
            showUpdateStatusDialog(selectedRequest)
        }
        binding.rvRepairRequests.layoutManager = LinearLayoutManager(this)
        binding.rvRepairRequests.adapter = adapter
    }

    private fun loadRepairRequests() {
        db.collection("repairs")
            .addSnapshotListener { snapshots, e ->
                if (snapshots != null) {
                    repairList.clear()
                    for (doc in snapshots) {
                        val request = doc.toObject(RepairRequest::class.java)
                        request.id = doc.id
                        repairList.add(request)
                    }

                    if (repairList.isEmpty()) {
                        binding.tvEmptyRepair.visibility = View.VISIBLE
                    } else {
                        binding.tvEmptyRepair.visibility = View.GONE
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun showUpdateStatusDialog(request: RepairRequest) {
        val statuses = arrayOf("Chờ xử lý", "Đang sửa", "Đã xong")
        AlertDialog.Builder(this)
            .setTitle("Cập nhật trạng thái sự cố")
            .setItems(statuses) { _, which ->
                val newStatus = statuses[which]
                db.collection("repairs").document(request.id)
                    .update("status", newStatus)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đã chuyển thành: $newStatus", Toast.LENGTH_SHORT).show()
                    }
            }.show()
    }
}