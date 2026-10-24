package com.example.dacs3

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dacs3.databinding.ActivityTenantHandoverBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TenantHandoverActivity : HouseActivity() {

    private lateinit var binding: ActivityTenantHandoverBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val handoverList = mutableListOf<HandoverRequest>()
    private lateinit var adapter: TenantHandoverAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTenantHandoverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupRecyclerView()
        loadTenantNameAndHandovers()
    }

    private fun setupRecyclerView() {
        adapter = TenantHandoverAdapter(handoverList) { selectedRequest ->
            confirmSignHandover(selectedRequest)
        }
        binding.rvTenantHandovers.layoutManager = LinearLayoutManager(this)
        binding.rvTenantHandovers.adapter = adapter
    }

    private fun loadTenantNameAndHandovers() {
        val uid = auth.currentUser?.uid ?: return

        // Bước 1: Lấy họ tên chuẩn của tài khoản Người thuê đang đăng nhập
        db.collection("Users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val myName = userDoc.getString("fullName") ?: ""

                // Bước 2: Lọc danh sách biên bản bàn giao được gửi cho chính họ
                db.collection("handovers")
                    .whereEqualTo("tenantName", myName)
                    .addSnapshotListener { snapshots, _ ->
                        if (snapshots != null) {
                            handoverList.clear()
                            for (doc in snapshots) {
                                val request = doc.toObject(HandoverRequest::class.java)
                                request.id = doc.id
                                handoverList.add(request)
                            }

                            if (handoverList.isEmpty()) {
                                binding.tvEmptyTenantHandover.visibility = View.VISIBLE
                            } else {
                                binding.tvEmptyTenantHandover.visibility = View.GONE
                            }
                            adapter.notifyDataSetChanged()
                        }
                    }
            }
    }

    private fun confirmSignHandover(request: HandoverRequest) {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận ký nhận phòng")
            .setMessage("Bằng việc đồng ý, bạn xác nhận đã kiểm tra kỹ phòng và đồng ý nhận bàn giao cơ sở vật chất từ chủ nhà.")
            .setPositiveButton("Đồng ý ký") { _, _ ->
                // Cập nhật trạng thái hoàn thành online lên Firestore
                db.collection("handovers").document(request.id)
                    .update("completed", true)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Bạn đã ký nhận phòng thành công!", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}